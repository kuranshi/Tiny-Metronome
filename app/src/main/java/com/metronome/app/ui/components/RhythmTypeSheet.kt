package com.metronome.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metronome.app.model.AccentLevel
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.RhythmMode
import com.metronome.app.model.SubdivisionType
import com.metronome.app.ui.theme.StudioColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RhythmTypeSheet(
    config: MetronomeConfig,
    onDismiss: () -> Unit,
    onSelectMode: (RhythmMode) -> Unit,
    onSetTimeSignature: (Int, Int) -> Unit,
    onSetSubdivision: (SubdivisionType) -> Unit,
    onSetSubdivisionAccent: (AccentLevel) -> Unit,
    onSetCompoundMeter: (List<Int>, Int) -> Unit,
    onSetPolyrhythm: (Int, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioColors.Surface,
        contentColor = StudioColors.TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "自定义节拍类型",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StudioColors.TextPrimary
            )

            // 3 Mode Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RhythmMode.entries.forEach { mode ->
                    val selected = config.mode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectMode(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) StudioColors.AccentStrong else StudioColors.SurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (selected) StudioColors.AccentStrong else StudioColors.Outline.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (selected) StudioColors.Background else StudioColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            when (config.mode) {
                RhythmMode.STANDARD -> StandardMeterEditor(
                    config = config,
                    onSetTimeSignature = onSetTimeSignature,
                    onSetSubdivision = onSetSubdivision,
                    onSetSubdivisionAccent = onSetSubdivisionAccent
                )

                RhythmMode.COMPOUND -> CompoundMeterEditor(
                    config = config,
                    onSetCompoundMeter = onSetCompoundMeter
                )

                RhythmMode.POLYRHYTHM -> PolyrhythmEditor(
                    config = config,
                    onSetPolyrhythm = onSetPolyrhythm
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StandardMeterEditor(
    config: MetronomeConfig,
    onSetTimeSignature: (Int, Int) -> Unit,
    onSetSubdivision: (SubdivisionType) -> Unit,
    onSetSubdivisionAccent: (AccentLevel) -> Unit
) {
    val quickMeters = listOf(2 to 4, 3 to 4, 4 to 4, 5 to 4, 6 to 8, 7 to 8, 9 to 8, 12 to 8)

    SectionTitle("常用拍号快捷切换")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickMeters.forEach { (num, den) ->
            val selected = config.beatsPerBar == num && config.beatUnit == den
            SelectableChip(
                label = "$num/$den",
                selected = selected,
                onClick = { onSetTimeSignature(num, den) }
            )
        }
    }

    SectionTitle("自由拍号 (每小节 1~16 拍 / 分母 2,4,8,16)")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepperCard(
            title = "每小节拍数",
            valueText = "${config.beatsPerBar}",
            onDecrement = { if (config.beatsPerBar > 1) onSetTimeSignature(config.beatsPerBar - 1, config.beatUnit) },
            onIncrement = { if (config.beatsPerBar < MetronomeConfig.MAX_BEATS) onSetTimeSignature(config.beatsPerBar + 1, config.beatUnit) },
            modifier = Modifier.weight(1f)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "拍号分母", color = StudioColors.TextSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetronomeConfig.VALID_BEAT_UNITS.forEach { unit ->
                    val selected = config.beatUnit == unit
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSetTimeSignature(config.beatsPerBar, unit) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) StudioColors.AccentMedium else StudioColors.SurfaceElevated
                    ) {
                        Text(
                            text = "$unit",
                            color = if (selected) StudioColors.Background else StudioColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }

    SectionTitle("节奏细分型 (Subdivision)")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SubdivisionType.entries.forEach { sub ->
            val selected = config.subdivision == sub
            SelectableChip(
                label = "${sub.shortSymbol}  ${sub.displayName}",
                selected = selected,
                onClick = { onSetSubdivision(sub) }
            )
        }
    }

    if (config.subdivision.offsets.size > 1) {
        SectionTitle("细分音强度")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccentLevel.entries.forEach { level ->
                val selected = config.subdivisionAccent == level
                SelectableChip(
                    label = level.label,
                    selected = selected,
                    onClick = { onSetSubdivisionAccent(level) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompoundMeterEditor(
    config: MetronomeConfig,
    onSetCompoundMeter: (List<Int>, Int) -> Unit
) {
    val quickCompounds = listOf(
        Triple("5/8 (3+2)", listOf(3, 2), 8),
        Triple("5/8 (2+3)", listOf(2, 3), 8),
        Triple("6/8 (3+3)", listOf(3, 3), 8),
        Triple("7/8 (3+2+2)", listOf(3, 2, 2), 8),
        Triple("7/8 (2+2+3)", listOf(2, 2, 3), 8),
        Triple("7/8 (2+3+2)", listOf(2, 3, 2), 8),
        Triple("8/8 (3+3+2)", listOf(3, 3, 2), 8),
        Triple("9/8 (2+2+2+3)", listOf(2, 2, 2, 3), 8),
        Triple("11/8 (3+3+3+2)", listOf(3, 3, 3, 2), 8)
    )

    SectionTitle("经典不对称 / 加法拍子预设")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickCompounds.forEach { (label, groups, unit) ->
            val selected = config.compoundGroups == groups && config.compoundUnit == unit
            SelectableChip(
                label = label,
                selected = selected,
                onClick = { onSetCompoundMeter(groups, unit) }
            )
        }
    }

    val currentSum = config.compoundGroups.sum()
    SectionTitle("自定义分组构建（当前：${currentSum}/${config.compoundUnit} = ${config.compoundGroups.joinToString("+")}）")

    // Current groups chips (tap to remove if > 1 group)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        config.compoundGroups.forEachIndexed { idx, groupSize ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioColors.AccentMedium.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, StudioColors.AccentMedium),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        if (config.compoundGroups.size > 1) {
                            val next = config.compoundGroups.toMutableList().apply { removeAt(idx) }
                            if (next.sum() >= 2) {
                                onSetCompoundMeter(next, config.compoundUnit)
                            }
                        }
                    }
            ) {
                Text(
                    text = "组${idx + 1}: $groupSize 拍  ×",
                    color = StudioColors.AccentMedium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }

    Text(
        text = "点击上方分组可移除；点击下方按钮追加新分组（总拍数上限 16 拍）：",
        color = StudioColors.TextMuted,
        fontSize = 11.sp
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(1, 2, 3, 4, 5).forEach { addSize ->
            val canAdd = currentSum + addSize <= MetronomeConfig.MAX_BEATS
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = canAdd) {
                        onSetCompoundMeter(config.compoundGroups + addSize, config.compoundUnit)
                    },
                shape = RoundedCornerShape(10.dp),
                color = if (canAdd) StudioColors.SurfaceElevated else StudioColors.Background,
                border = BorderStroke(
                    1.dp,
                    if (canAdd) StudioColors.AccentStrong.copy(alpha = 0.7f) else StudioColors.Outline.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "+$addSize",
                    color = if (canAdd) StudioColors.TextPrimary else StudioColors.TextMuted,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }

    SectionTitle("记谱分母单位")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(4, 8, 16).forEach { u ->
            SelectableChip(
                label = "/$u 拍",
                selected = config.compoundUnit == u,
                onClick = { onSetCompoundMeter(config.compoundGroups, u) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PolyrhythmEditor(
    config: MetronomeConfig,
    onSetPolyrhythm: (Int, Int) -> Unit
) {
    val quickPolys = listOf(3 to 2, 4 to 3, 5 to 3, 5 to 4, 7 to 4, 5 to 2, 7 to 3)

    SectionTitle("常见多节奏快捷组合")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickPolys.forEach { (p, s) ->
            val selected = config.polyPrimaryBeats == p && config.polySecondaryBeats == s
            SelectableChip(
                label = "$p 对 $s",
                selected = selected,
                onClick = { onSetPolyrhythm(p, s) }
            )
        }
    }

    SectionTitle("自定义双声部拍数 (2 ~ 12)")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepperCard(
            title = "主声部 (BPM基准)",
            valueText = "${config.polyPrimaryBeats}",
            onDecrement = { if (config.polyPrimaryBeats > 2) onSetPolyrhythm(config.polyPrimaryBeats - 1, config.polySecondaryBeats) },
            onIncrement = { if (config.polyPrimaryBeats < MetronomeConfig.MAX_POLY_BEATS) onSetPolyrhythm(config.polyPrimaryBeats + 1, config.polySecondaryBeats) },
            modifier = Modifier.weight(1f)
        )
        StepperCard(
            title = "副声部 (同周期对齐)",
            valueText = "${config.polySecondaryBeats}",
            onDecrement = { if (config.polySecondaryBeats > 2) onSetPolyrhythm(config.polyPrimaryBeats, config.polySecondaryBeats - 1) },
            onIncrement = { if (config.polySecondaryBeats < MetronomeConfig.MAX_POLY_BEATS) onSetPolyrhythm(config.polyPrimaryBeats, config.polySecondaryBeats + 1) },
            modifier = Modifier.weight(1f)
        )
    }

    Text(
        text = "说明：主声部按当前 BPM 稳定走拍，副声部在完全相同的小节周期内等分切分，两者在第 1 拍采样级精准对齐。",
        color = StudioColors.TextMuted,
        fontSize = 11.sp
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = StudioColors.TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) StudioColors.AccentStrong.copy(alpha = 0.22f) else StudioColors.SurfaceElevated,
        border = BorderStroke(
            1.dp,
            if (selected) StudioColors.AccentStrong else StudioColors.Outline.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = label,
            color = if (selected) StudioColors.AccentStrong else StudioColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StepperCard(
    title: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = title, color = StudioColors.TextSecondary, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDecrement),
                shape = RoundedCornerShape(10.dp),
                color = StudioColors.SurfaceElevated
            ) {
                Text(
                    text = "−",
                    color = StudioColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Text(
                text = valueText,
                color = StudioColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onIncrement),
                shape = RoundedCornerShape(10.dp),
                color = StudioColors.SurfaceElevated
            ) {
                Text(
                    text = "+",
                    color = StudioColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
