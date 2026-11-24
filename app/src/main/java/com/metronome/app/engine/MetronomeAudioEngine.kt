package com.metronome.app.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.Process
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.SoundKit
import com.metronome.app.model.AccentLevel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Low-latency hardware-clocked Android AudioTrack engine wrapping [SampleAccurateRenderer].
 * Synchronizes visual UI ticks with actual AudioTrack playback head frames.
 */
class MetronomeAudioEngine(
    context: Context,
    initialConfig: MetronomeConfig,
    private val onVisualTick: (ScheduledTickRecord) -> Unit,
    private val onAudioFocusLost: () -> Unit
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Query hardware native output sample rate (typically 48000 Hz) to bypass software resampling jitter
    val nativeSampleRate: Int = run {
        val rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        if (rate in 22050..192000) rate else 48000
    }

    private val renderer = SampleAccurateRenderer(nativeSampleRate, initialConfig)
    private val isRunning = AtomicBoolean(false)
    private var audioThread: Thread? = null

    @Volatile
    private var latestConfig: MetronomeConfig = initialConfig.sanitized()

    private val pendingVisualTicks = ConcurrentLinkedQueue<ScheduledTickRecord>()

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        ) {
            stop()
            onAudioFocusLost()
        }
    }

    private val focusRequest: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(audioAttributes)
        .setOnAudioFocusChangeListener(focusChangeListener)
        .build()

    fun updateConfig(config: MetronomeConfig) {
        val safe = config.sanitized()
        latestConfig = safe
        renderer.updateConfig(safe)
    }

    fun start() {
        if (!isRunning.compareAndSet(false, true)) return

        audioManager.requestAudioFocus(focusRequest)
        pendingVisualTicks.clear()
        renderer.reset(latestConfig)

        val thread = Thread(::audioLoop, "MetronomeAudioThread").apply {
            isDaemon = true
        }
        audioThread = thread
        thread.start()
    }

    fun stop() {
        if (!isRunning.compareAndSet(true, false)) return
        audioThread?.let { t ->
            try {
                t.join(400)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        audioThread = null
        pendingVisualTicks.clear()
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    /**
     * Plays a short one-shot sound preview when the user selects a SoundKit in settings.
     */
    fun previewSound(kit: SoundKit, accent: AccentLevel = AccentLevel.ACCENT) {
        Thread({
            val bank = SoundSynthesizer.buildBank(kit, nativeSampleRate)
            val wave = bank.waveformFor(accent, isSubdivision = false)
            if (wave.isEmpty()) return@Thread

            val pcm = ShortArray(wave.size) { idx ->
                (wave[idx] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            }
            val minBytes = AudioTrack.getMinBufferSize(
                nativeSampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(pcm.size * 2)

            val track = buildAudioTrack(minBytes)
            try {
                track.play()
                track.write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING)
                // Write a brief silence tail so the buffer flushes cleanly
                val tail = ShortArray(nativeSampleRate / 25)
                track.write(tail, 0, tail.size, AudioTrack.WRITE_BLOCKING)
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }, "MetronomePreviewThread").start()
    }

    fun release() {
        stop()
    }

    private fun buildAudioTrack(bufferSizeBytes: Int): AudioTrack {
        val format = AudioFormat.Builder()
            .setSampleRate(nativeSampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        return AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private fun audioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val minBufBytes = AudioTrack.getMinBufferSize(
            nativeSampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(1024)

        // 2x minBufferSize keeps latency low (~15-25ms) while protecting against underruns under UI load
        val trackBufferBytes = (minBufBytes * 2).coerceAtLeast(2048)
        val chunkFrames = 256 // ~5.33ms at 48 kHz
        val pcmChunk = ShortArray(chunkFrames)

        val track = buildAudioTrack(trackBufferBytes)
        val timestamp = AudioTimestamp()

        // Track 32-bit unsigned playbackHeadPosition wrap-around safely for multi-hour sessions
        var lastRawHead = 0L
        var wrappedHeadOffset = 0L

        try {
            // Pre-fill 2 chunks so beat 1 fires immediately on track.play() with zero startup underrun
            repeat(2) {
                renderer.renderChunk(pcmChunk, chunkFrames) { tick ->
                    pendingVisualTicks.add(tick)
                }
                track.write(pcmChunk, 0, chunkFrames, AudioTrack.WRITE_BLOCKING)
            }

            track.play()

            while (isRunning.get()) {
                renderer.renderChunk(pcmChunk, chunkFrames) { tick ->
                    pendingVisualTicks.add(tick)
                }

                val written = track.write(pcmChunk, 0, chunkFrames, AudioTrack.WRITE_BLOCKING)
                if (written < 0) {
                    break
                }

                // Compute hardware playback frame for tight audio-visual synchronization
                val currentPlaybackFrame = readPlaybackFrame(track, timestamp, lastRawHead, wrappedHeadOffset) { raw, offset ->
                    lastRawHead = raw
                    wrappedHeadOffset = offset
                }

                // Dispatch all ticks whose audio frame has reached (or is within 3ms of) the speaker output
                val leadFrames = (nativeSampleRate * 0.003).toLong()
                while (true) {
                    val head = pendingVisualTicks.peek() ?: break
                    if (head.audioFrame <= currentPlaybackFrame + leadFrames) {
                        pendingVisualTicks.poll()
                        onVisualTick(head)
                    } else {
                        break
                    }
                }
            }
        } finally {
            runCatching {
                track.pause()
                track.flush()
                track.stop()
            }
            track.release()
        }
    }

    private inline fun readPlaybackFrame(
        track: AudioTrack,
        timestamp: AudioTimestamp,
        lastRawHead: Long,
        wrappedOffset: Long,
        updateWrap: (Long, Long) -> Unit
    ): Long {
        if (track.getTimestamp(timestamp) && timestamp.framePosition >= 0L) {
            val deltaNs = System.nanoTime() - timestamp.nanoTime
            if (deltaNs in 0L..200_000_000L) {
                val extrapolatedFrames = (deltaNs * nativeSampleRate.toDouble() / 1_000_000_000.0).toLong()
                return timestamp.framePosition + extrapolatedFrames
            }
        }
        val rawUnsigned = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        var offset = wrappedOffset
        if (rawUnsigned < lastRawHead) {
            offset += (1L shl 32)
        }
        updateWrap(rawUnsigned, offset)
        return offset + rawUnsigned
    }
}
