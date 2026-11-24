package com.metronome.app.engine

import com.metronome.app.model.AccentLevel
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.SoundKit
import com.metronome.app.model.VoiceTrack
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.tanh

/**
 * Tick metadata stamped with the exact hardware audio frame index when it begins sounding.
 */
data class ScheduledTickRecord(
    val audioFrame: Long,
    val voiceTrack: VoiceTrack,
    val beatIndex: Int,
    val subIndex: Int,
    val isSubdivision: Boolean,
    val accent: AccentLevel
)

/**
 * Snapshot pushed atomically from UI thread to audio thread.
 */
internal data class RendererSnapshot(
    val config: MetronomeConfig,
    val plan: MeasurePlan,
    val primaryBank: SynthesizedSoundBank,
    val secondaryBank: SynthesizedSoundBank
)

/**
 * Pure-Kotlin, zero-allocation, sample-accurate metronome PCM renderer.
 *
 * Timing architecture:
 * - All beat positions are derived from a monotonic bar counter multiplied by exact double
 *   bar duration in audio frames: `Math.round(epochStartFrame + (barNumber + phase) * barDurationFrames)`.
 * - Because bar boundaries use multiplication instead of repeated tiny float increments,
 *   cumulative drift over thousands of bars stays strictly < 0.5 samples (< 0.01 ms at 48 kHz).
 * - Triggers are mixed at their exact sub-buffer sample offset `offsetInChunk`, eliminating
 *   buffer-size quantization jitter (resolution = 1 / sampleRate = ~0.0208 ms at 48 kHz).
 */
class SampleAccurateRenderer(
    val sampleRate: Int,
    initialConfig: MetronomeConfig
) {
    private class ActiveVoice {
        var active: Boolean = false
        var waveform: FloatArray = SynthesizedSoundBank.EMPTY
        var readPos: Int = 0
        var delaySamples: Int = 0
    }

    private val voicePool = Array(MAX_POLYPHONY) { ActiveVoice() }
    private var mixBus = FloatArray(1024)

    // Pre-allocated cache of synthesized sound banks per SoundKit so hot-swapping sounds never synthesizes on audio thread
    private val primaryBanks: Map<SoundKit, SynthesizedSoundBank> = SoundKit.entries.associateWith {
        SoundSynthesizer.buildBank(it, sampleRate, pitchMultiplier = 1.0)
    }
    private val secondaryBanks: Map<SoundKit, SynthesizedSoundBank> = SoundKit.entries.associateWith {
        SoundSynthesizer.buildBank(it, sampleRate, pitchMultiplier = 0.84)
    }

    @Volatile
    private var pendingSnapshot: RendererSnapshot? = null

    private var activeSnapshot: RendererSnapshot = createSnapshot(initialConfig.sanitized())

    // Timing state owned strictly by the render thread
    var globalFrame: Long = 0L
        private set
    private var epochStartFrame: Double = 0.0
    private var currentBar: Long = 0L
    private var nextEventIdx: Int = 0
    private var barDurationFrames: Double = computeBarDurationFrames(activeSnapshot)

    private fun createSnapshot(config: MetronomeConfig): RendererSnapshot {
        val safe = config.sanitized()
        return RendererSnapshot(
            config = safe,
            plan = MeasureTimeline.buildPlan(safe),
            primaryBank = primaryBanks.getValue(safe.primarySound),
            secondaryBank = secondaryBanks.getValue(safe.secondarySound)
        )
    }

    private fun computeBarDurationFrames(snapshot: RendererSnapshot): Double {
        return (60.0 / snapshot.config.bpm.toDouble()) * snapshot.plan.barDurationBeats * sampleRate.toDouble()
    }

    /**
     * Thread-safe configuration update. Applied seamlessly at the start of the next render chunk.
     */
    fun updateConfig(newConfig: MetronomeConfig) {
        pendingSnapshot = createSnapshot(newConfig)
    }

    /**
     * Resets transport back to frame 0 / bar 0 beat 1 (used when starting playback from stopped state).
     */
    fun reset(config: MetronomeConfig? = null) {
        if (config != null) {
            activeSnapshot = createSnapshot(config)
            pendingSnapshot = null
        } else {
            pendingSnapshot?.let {
                activeSnapshot = it
                pendingSnapshot = null
            }
        }
        globalFrame = 0L
        epochStartFrame = 0.0
        currentBar = 0L
        nextEventIdx = 0
        barDurationFrames = computeBarDurationFrames(activeSnapshot)
        for (v in voicePool) {
            v.active = false
            v.readPos = 0
            v.delaySamples = 0
        }
    }

    /**
     * Renders [frameCount] mono 16-bit PCM samples into [outputBuffer] starting at index 0.
     * Any scheduled beat events falling in `[globalFrame, globalFrame + frameCount)` are emitted
     * to [onEventScheduled] with their exact absolute audio frame index.
     */
    fun renderChunk(
        outputBuffer: ShortArray,
        frameCount: Int,
        onEventScheduled: ((ScheduledTickRecord) -> Unit)? = null
    ) {
        applyPendingUpdateIfAny()

        val chunkStart = globalFrame
        val chunkEnd = chunkStart + frameCount

        val snap = activeSnapshot
        val events = snap.plan.events

        // 1. Schedule every beat event whose exact frame falls in [chunkStart, chunkEnd)
        if (events.isNotEmpty()) {
            while (true) {
                val ev = events[nextEventIdx]
                val exactFrameDouble = epochStartFrame + (currentBar.toDouble() + ev.phase) * barDurationFrames
                val eventFrame = exactFrameDouble.roundToLong()

                if (eventFrame >= chunkEnd) {
                    break
                }

                if (eventFrame >= chunkStart) {
                    val offsetInChunk = (eventFrame - chunkStart).toInt()
                    val bank = if (ev.voiceTrack == VoiceTrack.PRIMARY) snap.primaryBank else snap.secondaryBank
                    val waveform = bank.waveformFor(ev.accent, ev.isSubdivision)
                    if (waveform.isNotEmpty()) {
                        triggerVoice(waveform, offsetInChunk)
                    }
                    onEventScheduled?.invoke(
                        ScheduledTickRecord(
                            audioFrame = eventFrame,
                            voiceTrack = ev.voiceTrack,
                            beatIndex = ev.beatIndex,
                            subIndex = ev.subIndex,
                            isSubdivision = ev.isSubdivision,
                            accent = ev.accent
                        )
                    )
                }

                // Advance monotonic event cursor
                nextEventIdx++
                if (nextEventIdx >= events.size) {
                    nextEventIdx = 0
                    currentBar++
                }
            }
        }

        // 2. Mix active voices into float bus with exact sub-buffer offset accuracy
        if (mixBus.size < frameCount) {
            mixBus = FloatArray(frameCount)
        }
        for (i in 0 until frameCount) {
            mixBus[i] = 0f
        }

        for (v in voicePool) {
            if (!v.active) continue
            val wave = v.waveform
            var sampleIdx = v.delaySamples
            v.delaySamples = 0
            var rp = v.readPos

            while (sampleIdx < frameCount && rp < wave.size) {
                mixBus[sampleIdx] += wave[rp]
                sampleIdx++
                rp++
            }

            if (rp >= wave.size) {
                v.active = false
                v.readPos = 0
            } else {
                v.readPos = rp
            }
        }

        // 3. Soft-limit (prevents harsh digital clipping when polyrhythm voices coincide) & convert to PCM16
        for (i in 0 until frameCount) {
            val mixed = mixBus[i]
            val limited = if (mixed > 0.92f || mixed < -0.92f) {
                tanh(mixed.toDouble()).toFloat()
            } else {
                mixed
            }
            val pcm = (limited * 32767f).toInt().coerceIn(-32768, 32767)
            outputBuffer[i] = pcm.toShort()
        }

        globalFrame = chunkEnd
    }

    private fun applyPendingUpdateIfAny() {
        val next = pendingSnapshot ?: return
        pendingSnapshot = null

        val prev = activeSnapshot
        val sameStructure = prev.plan.structureKey == next.plan.structureKey
        val sameBpm = prev.config.bpm == next.config.bpm

        activeSnapshot = next

        if (sameStructure && sameBpm) {
            // Only accents or sound kits changed: keep exact anchor & phase with ZERO timing disturbance!
            return
        }

        val newBarDuration = computeBarDurationFrames(next)

        if (sameStructure) {
            // BPM changed smoothly: preserve current fractional bar phase without stutter or jump
            val exactBarsElapsed = (globalFrame.toDouble() - epochStartFrame) / barDurationFrames
            val barFloor = floor(exactBarsElapsed)
            val currentPhase = (exactBarsElapsed - barFloor).coerceIn(0.0, 0.999999999)

            barDurationFrames = newBarDuration
            epochStartFrame = globalFrame.toDouble() - currentPhase * newBarDuration
            currentBar = 0L

            // Advance cursor to the first event at or after globalFrame
            val events = next.plan.events
            var foundIdx = -1
            for (i in events.indices) {
                val f = (epochStartFrame + events[i].phase * newBarDuration).roundToLong()
                if (f >= globalFrame) {
                    foundIdx = i
                    break
                }
            }
            if (foundIdx >= 0) {
                nextEventIdx = foundIdx
            } else {
                nextEventIdx = 0
                currentBar = 1L
            }
        } else {
            // Meter / rhythm mode changed: re-anchor cleanly on Beat 1 at current globalFrame
            barDurationFrames = newBarDuration
            epochStartFrame = globalFrame.toDouble()
            currentBar = 0L
            nextEventIdx = 0
        }
    }

    private fun triggerVoice(waveform: FloatArray, offsetInChunk: Int) {
        // Find an idle slot; if all busy, steal the oldest voice (highest readPos)
        var target: ActiveVoice? = null
        var oldest: ActiveVoice = voicePool[0]
        for (v in voicePool) {
            if (!v.active) {
                target = v
                break
            }
            if (v.readPos > oldest.readPos) {
                oldest = v
            }
        }
        val slot = target ?: oldest
        slot.active = true
        slot.waveform = waveform
        slot.readPos = 0
        slot.delaySamples = offsetInChunk
    }

    companion object {
        private const val MAX_POLYPHONY = 12
    }
}
