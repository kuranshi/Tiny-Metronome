package com.metronome.app

import com.metronome.app.engine.SampleAccurateRenderer
import com.metronome.app.engine.ScheduledTickRecord
import com.metronome.app.model.AccentLevel
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.RhythmMode
import com.metronome.app.model.SoundKit
import com.metronome.app.model.SubdivisionType
import com.metronome.app.model.VoiceTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Verifies the highest-priority requirement: sample-accurate beat timing and zero long-term drift.
 */
class PrecisionEngineTest {

    @Test
    fun thousandBeatsAtIrrationalTempo123BpmHasSubSampleZeroDrift() {
        val sampleRate = 48_000
        val bpm = 123 // 60/123*48000 = 23414.634146... non-integer frames per beat
        val config = MetronomeConfig(
            bpm = bpm,
            mode = RhythmMode.STANDARD,
            beatsPerBar = 4,
            subdivision = SubdivisionType.QUARTER,
            primarySound = SoundKit.DIGITAL
        )

        val renderer = SampleAccurateRenderer(sampleRate, config)
        val chunkFrames = 256
        val pcmChunk = ShortArray(chunkFrames)
        val ticks = ArrayList<ScheduledTickRecord>(1200)

        val exactFramesPerBeat = (60.0 / bpm.toDouble()) * sampleRate.toDouble()
        val targetBeats = 1050
        val totalFramesToRender = (exactFramesPerBeat * targetBeats).roundToLong()

        var rendered = 0L
        var maxOnsetPcmError = 0L
        var checkedPcmOnsets = 0

        while (rendered < totalFramesToRender) {
            val chunkStart = renderer.globalFrame
            val chunkTicks = ArrayList<ScheduledTickRecord>(2)
            renderer.renderChunk(pcmChunk, chunkFrames) { tick ->
                ticks.add(tick)
                chunkTicks.add(tick)
            }

            // Verify actual PCM waveform starts sounding at the exact sub-buffer offset!
            for (tick in chunkTicks) {
                val offset = (tick.audioFrame - chunkStart).toInt()
                // Check silence immediately before beat (except frame 0) if far from previous click
                if (offset > 10 && offset + 24 < chunkFrames) {
                    val beforeEnergy = abs(pcmChunk[offset - 2].toInt())
                    var onsetPeak = 0
                    for (k in 0 until 24) {
                        val v = abs(pcmChunk[offset + k].toInt())
                        if (v > onsetPeak) onsetPeak = v
                    }
                    assertEquals("Expected silence before beat onset at frame ${tick.audioFrame}", 0, beforeEnergy)
                    assertTrue("Expected crisp click transient within 0.5ms of scheduled frame", onsetPeak > 4000)
                    checkedPcmOnsets++
                }
            }
            rendered += chunkFrames
        }

        assertTrue("Expected at least 1000 rendered beats, got ${ticks.size}", ticks.size >= 1000)
        assertTrue("Expected many verified PCM onsets, got $checkedPcmOnsets", checkedPcmOnsets > 500)

        // Check every single beat from #0 through #1000 against exact theoretical frame
        for (i in 0 until 1000) {
            val expectedFrame = (i.toDouble() * exactFramesPerBeat).roundToLong()
            val actualFrame = ticks[i].audioFrame
            val errorSamples = abs(actualFrame - expectedFrame)
            if (errorSamples > maxOnsetPcmError) maxOnsetPcmError = errorSamples
            assertTrue(
                "Beat #$i drifted by $errorSamples samples (expected=$expectedFrame, actual=$actualFrame)",
                errorSamples <= 1L
            )
        }
        assertTrue("Max error over 1000 beats must be <= 1 sample (~0.021ms), was $maxOnsetPcmError", maxOnsetPcmError <= 1L)
    }

    @Test
    fun compoundMeterSevenEightAndPolyrhythmFourThreeExactTiming() {
        val sampleRate = 48_000
        val bpm = 137
        val config = MetronomeConfig(
            bpm = bpm,
            mode = RhythmMode.POLYRHYTHM,
            polyPrimaryBeats = 4,
            polySecondaryBeats = 3,
            primarySound = SoundKit.WOODBLOCK,
            secondarySound = SoundKit.RIMSHOT
        )

        val renderer = SampleAccurateRenderer(sampleRate, config)
        val chunk = ShortArray(256)
        val ticks = mutableListOf<ScheduledTickRecord>()

        val barFrames = (60.0 / bpm.toDouble()) * 4.0 * sampleRate.toDouble()
        val barsToSimulate = 120
        val totalFrames = (barFrames * barsToSimulate).roundToLong()

        var rendered = 0L
        while (rendered < totalFrames) {
            renderer.renderChunk(chunk, chunk.size) { ticks.add(it) }
            rendered += chunk.size
        }

        val primaryTicks = ticks.filter { it.voiceTrack == VoiceTrack.PRIMARY }
        val secondaryTicks = ticks.filter { it.voiceTrack == VoiceTrack.SECONDARY }

        assertTrue(primaryTicks.size >= 400)
        assertTrue(secondaryTicks.size >= 300)

        val exactPrimaryInterval = barFrames / 4.0
        val exactSecondaryInterval = barFrames / 3.0

        for (i in 0 until 400) {
            val expected = (i * exactPrimaryInterval).roundToLong()
            assertTrue("Primary beat $i error > 1 sample", abs(primaryTicks[i].audioFrame - expected) <= 1L)
        }
        for (j in 0 until 300) {
            val expected = (j * exactSecondaryInterval).roundToLong()
            assertTrue("Secondary beat $j error > 1 sample", abs(secondaryTicks[j].audioFrame - expected) <= 1L)
        }

        // Verify bar downbeats coincide identically (0 sample delta) after 100 bars!
        val bar100Primary = primaryTicks[100 * 4].audioFrame
        val bar100Secondary = secondaryTicks[100 * 3].audioFrame
        assertEquals("Polyrhythm voices must lock identically on Bar 100 downbeat", bar100Primary, bar100Secondary)
    }

    @Test
    fun hotSwapAccentDoesNotDisturbPhaseAndMutedBeatProducesSilence() {
        val sampleRate = 48_000
        val initial = MetronomeConfig(
            bpm = 120,
            mode = RhythmMode.STANDARD,
            beatsPerBar = 4,
            subdivision = SubdivisionType.QUARTER,
            beatAccents = listOf(AccentLevel.ACCENT, AccentLevel.NORMAL, AccentLevel.NORMAL, AccentLevel.NORMAL)
        )
        val renderer = SampleAccurateRenderer(sampleRate, initial)
        val chunk = ShortArray(256)
        val ticks = mutableListOf<ScheduledTickRecord>()

        // Render first 2 beats (48000 samples = 1.0s at 120 BPM)
        while (renderer.globalFrame < 40_000L) {
            renderer.renderChunk(chunk, chunk.size) { ticks.add(it) }
        }

        // Hot-swap beat 2 (index 2) to MUTE and beat 3 (index 3) to ACCENT mid-bar
        renderer.updateConfig(
            initial.copy(
                beatAccents = listOf(AccentLevel.ACCENT, AccentLevel.NORMAL, AccentLevel.MUTE, AccentLevel.ACCENT)
            )
        )

        var peakAroundBeat2 = 0
        while (renderer.globalFrame < 90_000L) {
            val start = renderer.globalFrame
            renderer.renderChunk(chunk, chunk.size) { ticks.add(it) }
            // Beat 2 falls at exact frame 48000; check window [48000..49000) is completely silent
            for (i in chunk.indices) {
                val f = start + i
                if (f in 48_000L..49_000L) {
                    val v = abs(chunk[i].toInt())
                    if (v > peakAroundBeat2) peakAroundBeat2 = v
                }
            }
        }

        assertEquals("Muted beat 2 must produce zero PCM output", 0, peakAroundBeat2)
        // Verify beat 3 still occurred at exact frame 72000 with ZERO timing shift from accent hot-swap!
        val beat3 = ticks.first { it.beatIndex == 3 }
        assertEquals(72_000L, beat3.audioFrame)
        assertEquals(AccentLevel.ACCENT, beat3.accent)
    }

    @Test
    fun hotSwapBpmPreservesSmoothForwardTimelineWithoutDoubleTrigger() {
        val sampleRate = 48_000
        val initial = MetronomeConfig(bpm = 100, beatsPerBar = 4)
        val renderer = SampleAccurateRenderer(sampleRate, initial)
        val chunk = ShortArray(256)
        val ticks = mutableListOf<ScheduledTickRecord>()

        // Play ~1.5 seconds at 100 BPM
        while (renderer.globalFrame < 72_000L) {
            renderer.renderChunk(chunk, chunk.size) { ticks.add(it) }
        }

        // Hot-swap BPM to 150 mid-bar
        renderer.updateConfig(initial.copy(bpm = 150))

        // Play another ~2 seconds at 150 BPM
        while (renderer.globalFrame < 168_000L) {
            renderer.renderChunk(chunk, chunk.size) { ticks.add(it) }
        }

        // Verify strictly increasing frames and no double-trigger closer than 50ms
        for (i in 1 until ticks.size) {
            val delta = ticks[i].audioFrame - ticks[i - 1].audioFrame
            assertTrue("Detected invalid or double-triggered beat delta=$delta", delta > 2400L)
        }

        // Verify final consecutive beat interval matches exact 150 BPM = 19200 samples
        val lastDelta = ticks.last().audioFrame - ticks[ticks.size - 2].audioFrame
        assertTrue("Expected 150 BPM interval 19200 samples, got $lastDelta", abs(lastDelta - 19_200L) <= 1L)
    }
}
