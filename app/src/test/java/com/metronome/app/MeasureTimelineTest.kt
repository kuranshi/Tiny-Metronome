package com.metronome.app

import com.metronome.app.engine.MeasureTimeline
import com.metronome.app.model.AccentLevel
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.RhythmMode
import com.metronome.app.model.SubdivisionType
import com.metronome.app.model.VoiceTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasureTimelineTest {

    @Test
    fun standardMeterWithCustomBackbeatAccents() {
        val config = MetronomeConfig(
            bpm = 120,
            mode = RhythmMode.STANDARD,
            beatsPerBar = 4,
            beatUnit = 4,
            subdivision = SubdivisionType.QUARTER,
            beatAccents = listOf(
                AccentLevel.MUTE,
                AccentLevel.ACCENT,
                AccentLevel.MEDIUM,
                AccentLevel.ACCENT
            )
        )
        val plan = MeasureTimeline.buildPlan(config)
        assertEquals(4.0, plan.barDurationBeats, 1e-9)
        assertEquals(4, plan.events.size)

        assertEquals(0.0, plan.events[0].phase, 1e-9)
        assertEquals(AccentLevel.MUTE, plan.events[0].accent)
        assertEquals(0.25, plan.events[1].phase, 1e-9)
        assertEquals(AccentLevel.ACCENT, plan.events[1].accent)
        assertEquals(0.5, plan.events[2].phase, 1e-9)
        assertEquals(AccentLevel.MEDIUM, plan.events[2].accent)
        assertEquals(0.75, plan.events[3].phase, 1e-9)
        assertEquals(AccentLevel.ACCENT, plan.events[3].accent)
    }

    @Test
    fun standardMeterTripletAndSwingPhases() {
        val tripletPlan = MeasureTimeline.buildPlan(
            MetronomeConfig(
                mode = RhythmMode.STANDARD,
                beatsPerBar = 3,
                subdivision = SubdivisionType.TRIPLET
            )
        )
        assertEquals(9, tripletPlan.events.size)
        // Beat 1, 2nd triplet should be at (1 + 1/3) / 3 = 4/9
        assertEquals(4.0 / 9.0, tripletPlan.events[4].phase, 1e-9)
        assertTrue(tripletPlan.events[4].isSubdivision)

        val swingPlan = MeasureTimeline.buildPlan(
            MetronomeConfig(
                mode = RhythmMode.STANDARD,
                beatsPerBar = 4,
                subdivision = SubdivisionType.SWING
            )
        )
        assertEquals(8, swingPlan.events.size)
        // Beat 0 swing upbeat should be at (0 + 2/3) / 4 = 1/6
        assertEquals(1.0 / 6.0, swingPlan.events[1].phase, 1e-9)
    }

    @Test
    fun compoundSevenEightThreeTwoTwoGrouping() {
        val groups = listOf(3, 2, 2)
        val config = MetronomeConfig(
            mode = RhythmMode.COMPOUND,
            compoundGroups = groups,
            compoundUnit = 8,
            compoundAccents = MetronomeConfig.defaultCompoundAccents(groups)
        )
        val plan = MeasureTimeline.buildPlan(config)
        assertEquals(7.0, plan.barDurationBeats, 1e-9)
        assertEquals(7, plan.events.size)

        // Default compound accents: group heads 0=ACCENT, 3=MEDIUM, 5=MEDIUM
        assertEquals(AccentLevel.ACCENT, plan.events[0].accent)
        assertEquals(AccentLevel.NORMAL, plan.events[1].accent)
        assertEquals(AccentLevel.NORMAL, plan.events[2].accent)
        assertEquals(AccentLevel.MEDIUM, plan.events[3].accent)
        assertEquals(AccentLevel.NORMAL, plan.events[4].accent)
        assertEquals(AccentLevel.MEDIUM, plan.events[5].accent)
        assertEquals(AccentLevel.NORMAL, plan.events[6].accent)
        assertEquals(5.0 / 7.0, plan.events[5].phase, 1e-9)
    }

    @Test
    fun polyrhythmFourAgainstThreeAlignment() {
        val config = MetronomeConfig(
            mode = RhythmMode.POLYRHYTHM,
            polyPrimaryBeats = 4,
            polySecondaryBeats = 3,
            polyPrimaryAccents = listOf(
                AccentLevel.ACCENT,
                AccentLevel.NORMAL,
                AccentLevel.MEDIUM,
                AccentLevel.NORMAL
            ),
            polySecondaryAccents = listOf(
                AccentLevel.ACCENT,
                AccentLevel.MUTE,
                AccentLevel.NORMAL
            )
        )
        val plan = MeasureTimeline.buildPlan(config)
        assertEquals(4.0, plan.barDurationBeats, 1e-9)
        assertEquals(7, plan.events.size)

        // Both voices coincide at phase 0.0 (PRIMARY first, SECONDARY second)
        assertEquals(0.0, plan.events[0].phase, 1e-9)
        assertEquals(VoiceTrack.PRIMARY, plan.events[0].voiceTrack)
        assertEquals(0.0, plan.events[1].phase, 1e-9)
        assertEquals(VoiceTrack.SECONDARY, plan.events[1].voiceTrack)

        // Strict non-decreasing phase order
        for (i in 1 until plan.events.size) {
            assertTrue(plan.events[i].phase >= plan.events[i - 1].phase)
        }
        // Check secondary beat 1 (1/3) carries MUTE
        val secBeat1 = plan.events.first { it.voiceTrack == VoiceTrack.SECONDARY && it.beatIndex == 1 }
        assertEquals(1.0 / 3.0, secBeat1.phase, 1e-9)
        assertEquals(AccentLevel.MUTE, secBeat1.accent)
    }
}
