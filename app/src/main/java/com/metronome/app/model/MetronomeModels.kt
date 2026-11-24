package com.metronome.app.model

enum class AccentLevel(val label: String, val shortLabel: String) {
    ACCENT("强拍", "强"),
    MEDIUM("次强", "次"),
    NORMAL("普通", "弱"),
    MUTE("静音", "休");

    fun next(): AccentLevel = when (this) {
        ACCENT -> MEDIUM
        MEDIUM -> NORMAL
        NORMAL -> MUTE
        MUTE -> ACCENT
    }
}

enum class RhythmMode(val displayName: String) {
    STANDARD("标准拍号"),
    COMPOUND("复合分组"),
    POLYRHYTHM("多节奏")
}

enum class SubdivisionType(
    val displayName: String,
    val shortSymbol: String,
    val offsets: List<Double>
) {
    QUARTER("四分音符", "♩", listOf(0.0)),
    EIGHTH("八分音符", "♫", listOf(0.0, 0.5)),
    TRIPLET("三连音", "♪³", listOf(0.0, 1.0 / 3.0, 2.0 / 3.0)),
    SIXTEENTH("十六分音符", "♬", listOf(0.0, 0.25, 0.5, 0.75)),
    DOTTED_EIGHTH("附点八分", "♪.♬", listOf(0.0, 0.75)),
    SYNCOPATED("十六分切分", "♬_♬", listOf(0.0, 0.25, 0.75)),
    SWING("爵士摇摆", "Swing", listOf(0.0, 2.0 / 3.0))
}

enum class SoundKit(val displayName: String, val description: String) {
    WOODBLOCK("木鱼", "温暖清晰，穿透力佳"),
    MECHANICAL("机械节拍器", "经典传统木质机械哒声"),
    DIGITAL("电子滴答", "精准干脆的高频正弦脉冲"),
    RIMSHOT("军鼓边击", "清亮干练，适合副声部对比")
}

enum class VoiceTrack {
    PRIMARY,
    SECONDARY
}

data class MetronomeConfig(
    val bpm: Int = 120,
    val mode: RhythmMode = RhythmMode.STANDARD,
    // Standard meter fields
    val beatsPerBar: Int = 4,
    val beatUnit: Int = 4,
    val subdivision: SubdivisionType = SubdivisionType.QUARTER,
    val beatAccents: List<AccentLevel> = defaultAccents(4),
    val subdivisionAccent: AccentLevel = AccentLevel.NORMAL,
    // Compound / Additive meter fields (e.g. 7/8 = 3+2+2)
    val compoundGroups: List<Int> = listOf(3, 2, 2),
    val compoundUnit: Int = 8,
    val compoundAccents: List<AccentLevel> = defaultCompoundAccents(listOf(3, 2, 2)),
    // Polyrhythm fields (e.g. 3 against 2)
    val polyPrimaryBeats: Int = 3,
    val polySecondaryBeats: Int = 2,
    val polyPrimaryAccents: List<AccentLevel> = defaultAccents(3),
    val polySecondaryAccents: List<AccentLevel> = defaultAccents(2),
    // Audio & display preferences
    val primarySound: SoundKit = SoundKit.WOODBLOCK,
    val secondarySound: SoundKit = SoundKit.RIMSHOT,
    val keepScreenOn: Boolean = true
) {
    fun sanitized(): MetronomeConfig {
        val safeBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
        val safeBeats = beatsPerBar.coerceIn(1, MAX_BEATS)
        val safeUnit = if (beatUnit in VALID_BEAT_UNITS) beatUnit else 4
        val safeBeatAccents = resizeAccents(beatAccents, safeBeats)

        val safeGroups = compoundGroups
            .map { it.coerceIn(1, 5) }
            .ifEmpty { listOf(3, 2, 2) }
            .let { list ->
                val result = mutableListOf<Int>()
                var sum = 0
                for (g in list) {
                    if (sum + g <= MAX_BEATS) {
                        result.add(g)
                        sum += g
                    }
                }
                if (result.isEmpty()) listOf(3, 2) else result
            }
        val safeCompoundTotal = safeGroups.sum().coerceAtLeast(2)
        val safeCompoundUnit = if (compoundUnit in listOf(4, 8, 16)) compoundUnit else 8
        val safeCompoundAccents = resizeAccents(
            compoundAccents,
            safeCompoundTotal,
            fallback = defaultCompoundAccents(safeGroups)
        )

        val safePolyP = polyPrimaryBeats.coerceIn(2, MAX_POLY_BEATS)
        val safePolyS = polySecondaryBeats.coerceIn(2, MAX_POLY_BEATS)
        val safePolyPAccents = resizeAccents(polyPrimaryAccents, safePolyP)
        val safePolySAccents = resizeAccents(polySecondaryAccents, safePolyS)

        return copy(
            bpm = safeBpm,
            beatsPerBar = safeBeats,
            beatUnit = safeUnit,
            beatAccents = safeBeatAccents,
            compoundGroups = safeGroups,
            compoundUnit = safeCompoundUnit,
            compoundAccents = safeCompoundAccents,
            polyPrimaryBeats = safePolyP,
            polySecondaryBeats = safePolyS,
            polyPrimaryAccents = safePolyPAccents,
            polySecondaryAccents = safePolySAccents
        )
    }

    fun summaryBadge(): String = when (mode) {
        RhythmMode.STANDARD -> "$beatsPerBar/$beatUnit · ${subdivision.displayName}"
        RhythmMode.COMPOUND -> "${compoundGroups.sum()}/$compoundUnit (${compoundGroups.joinToString("+")})"
        RhythmMode.POLYRHYTHM -> "多节奏 $polyPrimaryBeats : $polySecondaryBeats"
    }

    fun tempoMarking(): String = when {
        bpm < 40 -> "Grave · 极缓板"
        bpm < 60 -> "Largo · 广板"
        bpm < 72 -> "Adagio · 柔板"
        bpm < 92 -> "Andante · 行板"
        bpm < 112 -> "Moderato · 中板"
        bpm < 126 -> "Allegretto · 小快板"
        bpm < 156 -> "Allegro · 快板"
        bpm < 176 -> "Vivace · 活泼"
        bpm < 200 -> "Presto · 急板"
        else -> "Prestissimo · 最急板"
    }

    companion object {
        const val MIN_BPM = 20
        const val MAX_BPM = 300
        const val MAX_BEATS = 16
        const val MAX_POLY_BEATS = 12
        val VALID_BEAT_UNITS = listOf(2, 4, 8, 16)

        fun defaultAccents(count: Int): List<AccentLevel> =
            List(count.coerceIn(1, MAX_BEATS)) { idx ->
                if (idx == 0) AccentLevel.ACCENT else AccentLevel.NORMAL
            }

        fun defaultCompoundAccents(groups: List<Int>): List<AccentLevel> {
            val result = mutableListOf<AccentLevel>()
            groups.forEachIndexed { groupIdx, groupSize ->
                repeat(groupSize) { idxInGroup ->
                    val level = when {
                        groupIdx == 0 && idxInGroup == 0 -> AccentLevel.ACCENT
                        idxInGroup == 0 -> AccentLevel.MEDIUM
                        else -> AccentLevel.NORMAL
                    }
                    result.add(level)
                }
            }
            return result
        }

        fun resizeAccents(
            current: List<AccentLevel>,
            targetSize: Int,
            fallback: List<AccentLevel>? = null
        ): List<AccentLevel> {
            if (current.size == targetSize) return current
            return List(targetSize) { idx ->
                current.getOrNull(idx)
                    ?: fallback?.getOrNull(idx)
                    ?: if (idx == 0) AccentLevel.ACCENT else AccentLevel.NORMAL
            }
        }
    }
}

data class MetronomePreset(
    val id: String,
    val name: String,
    val description: String,
    val config: MetronomeConfig,
    val isBuiltIn: Boolean = false
)
