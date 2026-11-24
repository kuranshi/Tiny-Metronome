package com.metronome.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metronome.app.model.AccentLevel
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.RhythmMode
import com.metronome.app.ui.QuickAccentPattern
import com.metronome.app.ui.theme.StudioColors

@Composable
fun BeatGridVisualizer(
    config: MetronomeConfig,
    isPlaying: Boolean,
    activePrimaryBeat: Int,
    activePrimarySub: Int,
    activeSecondaryBeat: Int,
    onTapPrimaryBeat: (Int) -> Unit,
    onTapSecondaryBeat: (Int) -> Unit,
    onApplyQuickPattern: (QuickAccentPattern) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = StudioColors.Surface,
        border = BorderStroke(1.dp, StudioColors.Outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Quick Accent Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "拍位重音编排",
                    color = StudioColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "点击单拍切换 强/次/弱/休",
                    color = StudioColors.TextMuted,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAccentPattern.entries.forEach { pattern ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StudioColors.SurfaceElevated,
                        border = BorderStroke(1.dp, StudioColors.Outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onApplyQuickPattern(pattern) }
                    ) {
                        Text(
                            text = pattern.label,
                            color = StudioColors.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Main Beat Matrix based on RhythmMode
            when (config.mode) {
                RhythmMode.STANDARD -> {
                    BeatBarsMatrix(
                        accents = config.beatAccents,
                        activeIndex = if (isPlaying) activePrimaryBeat else -1,
                        isSecondaryVoice = false,
                        onTapBeat = onTapPrimaryBeat
                    )
                    // Subdivision pulse dots
                    if (config.subdivision.offsets.size > 1) {
                        SubdivisionIndicatorRow(
                            subCount = config.subdivision.offsets.size,
                            activeSub = if (isPlaying && activePrimaryBeat >= 0) activePrimarySub else -1,
                            label = "${config.subdivision.displayName} (${config.subdivision.shortSymbol})"
                        )
                    }
                }

                RhythmMode.COMPOUND -> {
                    CompoundGroupsMatrix(
                        groups = config.compoundGroups,
                        accents = config.compoundAccents,
                        activeIndex = if (isPlaying) activePrimaryBeat else -1,
                        onTapBeat = onTapPrimaryBeat
                    )
                }

                RhythmMode.POLYRHYTHM -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TrackHeaderLabel(
                            title = "主声部 (${config.polyPrimaryBeats} 拍)",
                            subtitle = config.primarySound.displayName,
                            dotColor = StudioColors.AccentStrong
                        )
                        BeatBarsMatrix(
                            accents = config.polyPrimaryAccents,
                            activeIndex = if (isPlaying) activePrimaryBeat else -1,
                            isSecondaryVoice = false,
                            onTapBeat = onTapPrimaryBeat
                        )

                        TrackHeaderLabel(
                            title = "副声部 (${config.polySecondaryBeats} 拍)",
                            subtitle = config.secondarySound.displayName,
                            dotColor = StudioColors.PolyStrong
                        )
                        BeatBarsMatrix(
                            accents = config.polySecondaryAccents,
                            activeIndex = if (isPlaying) activeSecondaryBeat else -1,
                            isSecondaryVoice = true,
                            onTapBeat = onTapSecondaryBeat
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackHeaderLabel(
    title: String,
    subtitle: String,
    dotColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = title,
            color = StudioColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "· $subtitle",
            color = StudioColors.TextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun BeatBarsMatrix(
    accents: List<AccentLevel>,
    activeIndex: Int,
    isSecondaryVoice: Boolean,
    onTapBeat: (Int) -> Unit
) {
    val maxPerRow = if (accents.size > 8) (accents.size + 1) / 2 else accents.size
    val rows = accents.indices.chunked(maxPerRow.coerceAtLeast(1))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowIndices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowIndices.forEach { beatIdx ->
                    BeatColumnTile(
                        beatNumber = beatIdx + 1,
                        accent = accents[beatIdx],
                        isActive = beatIdx == activeIndex,
                        isSecondaryVoice = isSecondaryVoice,
                        onClick = { onTapBeat(beatIdx) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompoundGroupsMatrix(
    groups: List<Int>,
    accents: List<AccentLevel>,
    activeIndex: Int,
    onTapBeat: (Int) -> Unit
) {
    // Build index ranges for each group (e.g. [3, 2, 2] -> [0..2], [3..4], [5..6])
    val groupRanges = buildList {
        var cursor = 0
        groups.forEach { size ->
            add(cursor until (cursor + size).coerceAtMost(accents.size))
            cursor += size
        }
    }

    val total = accents.size
    if (total <= 8) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupRanges.forEach { range ->
                val count = range.count().coerceAtLeast(1)
                CompoundGroupBox(
                    groupSize = count,
                    range = range,
                    accents = accents,
                    activeIndex = activeIndex,
                    onTapBeat = onTapBeat,
                    modifier = Modifier.weight(count.toFloat())
                )
            }
        }
    } else {
        // Split groups across two rows when total beats > 8 so buttons remain spacious
        val splitPoint = (groupRanges.size + 1) / 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(groupRanges.take(splitPoint), groupRanges.drop(splitPoint)).forEach { rowGroups ->
                if (rowGroups.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowGroups.forEach { range ->
                            val count = range.count().coerceAtLeast(1)
                            CompoundGroupBox(
                                groupSize = count,
                                range = range,
                                accents = accents,
                                activeIndex = activeIndex,
                                onTapBeat = onTapBeat,
                                modifier = Modifier.weight(count.toFloat())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompoundGroupBox(
    groupSize: Int,
    range: IntRange,
    accents: List<AccentLevel>,
    activeIndex: Int,
    onTapBeat: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(StudioColors.SurfaceElevated.copy(alpha = 0.65f))
            .border(1.dp, StudioColors.Outline.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "$groupSize 拍组",
            color = StudioColors.AccentMedium,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            range.forEach { beatIdx ->
                BeatColumnTile(
                    beatNumber = beatIdx + 1,
                    accent = accents.getOrElse(beatIdx) { AccentLevel.NORMAL },
                    isActive = beatIdx == activeIndex,
                    isSecondaryVoice = false,
                    onClick = { onTapBeat(beatIdx) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BeatColumnTile(
    beatNumber: Int,
    accent: AccentLevel,
    isActive: Boolean,
    isSecondaryVoice: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = accentColorFor(accent, isSecondaryVoice)
    val bgColor by animateColorAsState(
        targetValue = if (isActive) {
            if (accent == AccentLevel.MUTE) StudioColors.SurfaceHighlight else activeColor.copy(alpha = 0.28f)
        } else {
            StudioColors.SurfaceElevated
        },
        animationSpec = tween(durationMillis = 65),
        label = "beatBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> if (accent == AccentLevel.MUTE) StudioColors.TextSecondary else activeColor
            accent == AccentLevel.ACCENT -> activeColor.copy(alpha = 0.55f)
            else -> StudioColors.Outline.copy(alpha = 0.35f)
        },
        animationSpec = tween(durationMillis = 65),
        label = "beatBorder"
    )

    val filledSegments = when (accent) {
        AccentLevel.ACCENT -> 3
        AccentLevel.MEDIUM -> 2
        AccentLevel.NORMAL -> 1
        AccentLevel.MUTE -> 0
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 3 stacked LED intensity segments (top = Accent, mid = Medium, bottom = Normal)
        for (segment in 3 downTo 1) {
            val isLit = segment <= filledSegments
            val segColor = when {
                !isLit -> StudioColors.Background.copy(alpha = 0.65f)
                isActive -> activeColor
                else -> activeColor.copy(alpha = 0.85f)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(segColor)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = beatNumber.toString(),
            color = if (isActive) StudioColors.TextPrimary else StudioColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isActive || accent == AccentLevel.ACCENT) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = accent.shortLabel,
            color = if (accent == AccentLevel.MUTE) StudioColors.TextMuted else activeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubdivisionIndicatorRow(
    subCount: Int,
    activeSub: Int,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioColors.SurfaceElevated.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "当前细分：$label",
            color = StudioColors.TextSecondary,
            fontSize = 11.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (idx in 0 until subCount) {
                val active = idx == activeSub
                Box(
                    modifier = Modifier
                        .size(if (idx == 0) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                active && idx == 0 -> StudioColors.AccentStrong
                                active -> StudioColors.AccentMedium
                                idx == 0 -> StudioColors.Outline
                                else -> StudioColors.Outline.copy(alpha = 0.45f)
                            }
                        )
                )
            }
        }
    }
}

private fun accentColorFor(accent: AccentLevel, isSecondaryVoice: Boolean): Color {
    if (accent == AccentLevel.MUTE) return StudioColors.AccentMute
    return if (isSecondaryVoice) {
        when (accent) {
            AccentLevel.ACCENT -> StudioColors.PolyStrong
            AccentLevel.MEDIUM -> StudioColors.PolyMedium
            AccentLevel.NORMAL -> StudioColors.PolyNormal
            AccentLevel.MUTE -> StudioColors.AccentMute
        }
    } else {
        when (accent) {
            AccentLevel.ACCENT -> StudioColors.AccentStrong
            AccentLevel.MEDIUM -> StudioColors.AccentMedium
            AccentLevel.NORMAL -> StudioColors.AccentNormal
            AccentLevel.MUTE -> StudioColors.AccentMute
        }
    }
}
