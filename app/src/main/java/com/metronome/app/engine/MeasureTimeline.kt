package com.metronome.app.engine

import com.metronome.app.model.AccentLevel
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.RhythmMode
import com.metronome.app.model.VoiceTrack

/**
 * Single rhythmic trigger inside a normalized bar [0.0, 1.0).
 */
data class ScheduledBeatEvent(
    val phase: Double,
    val voiceTrack: VoiceTrack,
    val beatIndex: Int,
    val subIndex: Int,
    val isSubdivision: Boolean,
    val accent: AccentLevel
)

/**
 * Immutable rhythmic plan for one full bar.
 * [structureKey] uniquely identifies the metric grid (excluding accents/sounds/bpm)
 * so the audio engine knows when it can hot-swap accents without touching bar phase.
 */
data class MeasurePlan(
    val barDurationBeats: Double,
    val structureKey: String,
    val events: List<ScheduledBeatEvent>
)

object MeasureTimeline {

    fun buildPlan(rawConfig: MetronomeConfig): MeasurePlan {
        val config = rawConfig.sanitized()
        return when (config.mode) {
            RhythmMode.STANDARD -> buildStandardPlan(config)
            RhythmMode.COMPOUND -> buildCompoundPlan(config)
            RhythmMode.POLYRHYTHM -> buildPolyrhythmPlan(config)
        }
    }

    private fun buildStandardPlan(config: MetronomeConfig): MeasurePlan {
        val beats = config.beatsPerBar
        val offsets = config.subdivision.offsets
        val events = ArrayList<ScheduledBeatEvent>(beats * offsets.size)

        for (beatIdx in 0 until beats) {
            val mainAccent = config.beatAccents[beatIdx]
            offsets.forEachIndexed { subIdx, offset ->
                val isSub = subIdx > 0
                val phase = (beatIdx.toDouble() + offset) / beats.toDouble()
                val accent = if (isSub) config.subdivisionAccent else mainAccent
                events.add(
                    ScheduledBeatEvent(
                        phase = phase,
                        voiceTrack = VoiceTrack.PRIMARY,
                        beatIndex = beatIdx,
                        subIndex = subIdx,
                        isSubdivision = isSub,
                        accent = accent
                    )
                )
            }
        }

        return MeasurePlan(
            barDurationBeats = beats.toDouble(),
            structureKey = "STD:$beats:${config.beatUnit}:${config.subdivision.name}",
            events = events
        )
    }

    private fun buildCompoundPlan(config: MetronomeConfig): MeasurePlan {
        val totalUnits = config.compoundGroups.sum()
        val events = ArrayList<ScheduledBeatEvent>(totalUnits)

        for (unitIdx in 0 until totalUnits) {
            events.add(
                ScheduledBeatEvent(
                    phase = unitIdx.toDouble() / totalUnits.toDouble(),
                    voiceTrack = VoiceTrack.PRIMARY,
                    beatIndex = unitIdx,
                    subIndex = 0,
                    isSubdivision = false,
                    accent = config.compoundAccents[unitIdx]
                )
            )
        }

        return MeasurePlan(
            barDurationBeats = totalUnits.toDouble(),
            structureKey = "CMP:${config.compoundGroups.joinToString("+")}:${config.compoundUnit}",
            events = events
        )
    }

    private fun buildPolyrhythmPlan(config: MetronomeConfig): MeasurePlan {
        val p = config.polyPrimaryBeats
        val s = config.polySecondaryBeats
        val events = ArrayList<ScheduledBeatEvent>(p + s)

        for (i in 0 until p) {
            events.add(
                ScheduledBeatEvent(
                    phase = i.toDouble() / p.toDouble(),
                    voiceTrack = VoiceTrack.PRIMARY,
                    beatIndex = i,
                    subIndex = 0,
                    isSubdivision = false,
                    accent = config.polyPrimaryAccents[i]
                )
            )
        }

        for (j in 0 until s) {
            events.add(
                ScheduledBeatEvent(
                    phase = j.toDouble() / s.toDouble(),
                    voiceTrack = VoiceTrack.SECONDARY,
                    beatIndex = j,
                    subIndex = 0,
                    isSubdivision = false,
                    accent = config.polySecondaryAccents[j]
                )
            )
        }

        // Sort strictly by phase; when primary and secondary coincide (e.g. phase 0.0), PRIMARY comes first.
        events.sortWith(compareBy<ScheduledBeatEvent> { it.phase }.thenBy { it.voiceTrack.ordinal })

        return MeasurePlan(
            barDurationBeats = p.toDouble(),
            structureKey = "POLY:$p:$s",
            events = events
        )
    }
}
