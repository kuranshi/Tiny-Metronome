package com.metronome.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.SoundKit
import com.metronome.app.ui.theme.StudioColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSettingsSheet(
    config: MetronomeConfig,
    onDismiss: () -> Unit,
    onSelectPrimarySound: (SoundKit) -> Unit,
    onSelectSecondarySound: (SoundKit) -> Unit,
    onPreviewSound: (SoundKit) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "音色与练琴设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StudioColors.TextPrimary
            )

            Text(
                text = "主声部音色（点击可试听）",
                color = StudioColors.AccentStrong,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            SoundKitList(
                selected = config.primarySound,
                accentColor = StudioColors.AccentStrong,
                onSelect = onSelectPrimarySound,
                onPreview = onPreviewSound
            )

            Text(
                text = "副声部音色（多节奏 Polyrhythm 专用）",
                color = StudioColors.PolyStrong,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            SoundKitList(
                selected = config.secondarySound,
                accentColor = StudioColors.PolyStrong,
                onSelect = onSelectSecondarySound,
                onPreview = onPreviewSound
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StudioColors.SurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "打拍时保持屏幕常亮",
                            color = StudioColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "练琴时防止自动息屏，随时看清拍位闪烁",
                            color = StudioColors.TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.keepScreenOn,
                        onCheckedChange = onToggleKeepScreenOn,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioColors.Background,
                            checkedTrackColor = StudioColors.AccentStrong
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SoundKitList(
    selected: SoundKit,
    accentColor: Color,
    onSelect: (SoundKit) -> Unit,
    onPreview: (SoundKit) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SoundKit.entries.forEach { kit ->
            val isSelected = kit == selected
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(kit) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.16f) else StudioColors.SurfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) accentColor else StudioColors.Outline.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = kit.displayName,
                            color = if (isSelected) accentColor else StudioColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = kit.description,
                            color = StudioColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { onPreview(kit) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "试听",
                            tint = if (isSelected) accentColor else StudioColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
