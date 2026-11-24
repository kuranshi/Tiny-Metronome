package com.metronome.app.model

object DefaultPresets {
    val builtInPresets: List<MetronomePreset> = listOf(
        MetronomePreset(
            id = "builtin_4_4_pop",
            name = "4/4 流行基础",
            description = "经典 4/4 首拍强音，适合日常基本功练习",
            isBuiltIn = true,
            config = MetronomeConfig(
                bpm = 120,
                mode = RhythmMode.STANDARD,
                beatsPerBar = 4,
                beatUnit = 4,
                subdivision = SubdivisionType.QUARTER,
                beatAccents = listOf(
                    AccentLevel.ACCENT,
                    AccentLevel.NORMAL,
                    AccentLevel.MEDIUM,
                    AccentLevel.NORMAL
                )
            )
        ),
        MetronomePreset(
            id = "builtin_4_4_backbeat",
            name = "4/4 反拍强调 (2&4)",
            description = "强调第 2、4 反拍，培养律动与爵士/放克律动感",
            isBuiltIn = true,
            config = MetronomeConfig(
                bpm = 108,
                mode = RhythmMode.STANDARD,
                beatsPerBar = 4,
                beatUnit = 4,
                subdivision = SubdivisionType.EIGHTH,
                beatAccents = listOf(
                    AccentLevel.NORMAL,
                    AccentLevel.ACCENT,
                    AccentLevel.NORMAL,
                    AccentLevel.ACCENT
                ),
                subdivisionAccent = AccentLevel.NORMAL,
                primarySound = SoundKit.RIMSHOT
            )
        ),
        MetronomePreset(
            id = "builtin_3_4_waltz",
            name = "3/4 华尔兹",
            description = "强-弱-弱三拍子律动",
            isBuiltIn = true,
            config = MetronomeConfig(
                bpm = 132,
                mode = RhythmMode.STANDARD,
                beatsPerBar = 3,
                beatUnit = 4,
                subdivision = SubdivisionType.QUARTER,
                beatAccents = listOf(
                    AccentLevel.ACCENT,
                    AccentLevel.NORMAL,
                    AccentLevel.NORMAL
                ),
                primarySound = SoundKit.MECHANICAL
            )
        ),
        MetronomePreset(
            id = "builtin_6_8_compound",
            name = "6/8 复合摇曳 (3+3)",
            description = "第 1 拍强、第 4 拍次强的典型六八拍",
            isBuiltIn = true,
            config = MetronomeConfig(
                bpm = 150,
                mode = RhythmMode.COMPOUND,
                compoundGroups = listOf(3, 3),
                compoundUnit = 8,
                compoundAccents = MetronomeConfig.defaultCompoundAccents(listOf(3, 3))
            )
        ),
        MetronomePreset(
            id = "builtin_5_4_takefive",
            name = "5/4 经典五拍 (3+2)",
            description = "Take Five 风格 3+2 分组，第 1 拍强、第 4 拍次强",
            isBuiltIn = true,
            config = MetronomeConfig(
                bpm = 164,
                mode = RhythmMode.COMPOUND,
                compoundGroups = listOf(3, 2),
                compoundUnit = 4,
                compoundAccents = MetronomeConfig.defaultCompoundAccents(listOf(3, 2))
            )
        ),
        MetronomePreset(
            id = "builtin_7_8_balkan",
            name = "7/8 巴尔干 (3+2+2)",
            description = "不对称奇数拍子 3+2+2 分组强调",
            isBuiltIn = true,
            config = MetronomeConfig(
                bpm = 148,
                mode = RhythmMode.COMPOUND,
                compoundGroups = listOf(3, 2, 2),
                compoundUnit = 8,
                compoundAccents = MetronomeConfig.defaultCompoundAccents(listOf(3, 2, 2))
            )
        ),
        MetronomePreset(
            id = "builtin_jazz_swing",
            name = "4/4 爵士摇摆 Swing",
            description = "三连音摇摆切分 + 2、4 反拍重音",
            isBuiltIn = true,
            config = MetronomeConfig(
                bpm = 130,
                mode = RhythmMode.STANDARD,
                beatsPerBar = 4,
                beatUnit = 4,
                subdivision = SubdivisionType.SWING,
                beatAccents = listOf(
                    AccentLevel.MEDIUM,
                    AccentLevel.ACCENT,
                    AccentLevel.MEDIUM,
                    AccentLevel.ACCENT
                ),
                subdivisionAccent = AccentLevel.NORMAL,
                primarySound = SoundKit.WOODBLOCK
            )
        ),
        MetronomePreset(
            id = "builtin_poly_3_2",
            name = "3 : 2 多节奏",
            description = "经典三对二跨节奏训练，双音色区分主副声部",
            isBuiltIn = true,
            config = MetronomeConfig(
                bpm = 108,
                mode = RhythmMode.POLYRHYTHM,
                polyPrimaryBeats = 3,
                polySecondaryBeats = 2,
                polyPrimaryAccents = MetronomeConfig.defaultAccents(3),
                polySecondaryAccents = MetronomeConfig.defaultAccents(2),
                primarySound = SoundKit.WOODBLOCK,
                secondarySound = SoundKit.RIMSHOT
            )
        ),
        MetronomePreset(
            id = "builtin_poly_4_3",
            name = "4 : 3 多节奏",
            description = "四对三进阶复节奏，钢琴/鼓手常用专项练习",
            isBuiltIn = true,
            config = MetronomeConfig(
                bpm = 96,
                mode = RhythmMode.POLYRHYTHM,
                polyPrimaryBeats = 4,
                polySecondaryBeats = 3,
                polyPrimaryAccents = MetronomeConfig.defaultAccents(4),
                polySecondaryAccents = MetronomeConfig.defaultAccents(3),
                primarySound = SoundKit.DIGITAL,
                secondarySound = SoundKit.WOODBLOCK
            )
        )
    )
}
