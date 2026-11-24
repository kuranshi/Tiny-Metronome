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
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.MetronomePreset
import com.metronome.app.ui.theme.StudioColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSheet(
    currentConfig: MetronomeConfig,
    userPresets: List<MetronomePreset>,
    builtInPresets: List<MetronomePreset>,
    onDismiss: () -> Unit,
    onSavePreset: (String) -> Unit,
    onLoadPreset: (MetronomePreset) -> Unit,
    onDeletePreset: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newPresetName by remember { mutableStateOf("") }

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
                text = "节拍预设方案",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StudioColors.TextPrimary
            )

            // Save current config as preset
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StudioColors.SurfaceElevated,
                border = BorderStroke(1.dp, StudioColors.Outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "保存当前配置：${currentConfig.summaryBadge()} · ${currentConfig.bpm} BPM",
                        color = StudioColors.AccentMedium,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPresetName,
                            onValueChange = { newPresetName = it },
                            placeholder = {
                                Text("输入预设名称，如：吉他切分练习", fontSize = 13.sp)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StudioColors.AccentStrong,
                                unfocusedBorderColor = StudioColors.Outline,
                                focusedTextColor = StudioColors.TextPrimary,
                                unfocusedTextColor = StudioColors.TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newPresetName.isNotBlank()) {
                                    onSavePreset(newPresetName)
                                    newPresetName = ""
                                }
                            },
                            enabled = newPresetName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.AccentStrong,
                                contentColor = StudioColors.Background
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdd,
                                contentDescription = "保存"
                            )
                        }
                    }
                }
            }

            if (userPresets.isNotEmpty()) {
                Text(
                    text = "我的自定义预设 (${userPresets.size})",
                    color = StudioColors.PolyStrong,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    userPresets.forEach { preset ->
                        PresetCard(
                            preset = preset,
                            onLoad = {
                                onLoadPreset(preset)
                                onDismiss()
                            },
                            onDelete = { onDeletePreset(preset.id) }
                        )
                    }
                }
            }

            Text(
                text = "内置专业预设",
                color = StudioColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                builtInPresets.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        onLoad = {
                            onLoadPreset(preset)
                            onDismiss()
                        },
                        onDelete = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PresetCard(
    preset: MetronomePreset,
    onLoad: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onLoad),
        shape = RoundedCornerShape(14.dp),
        color = StudioColors.SurfaceElevated,
        border = BorderStroke(1.dp, StudioColors.Outline.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = preset.name,
                        color = StudioColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StudioColors.SurfaceHighlight
                    ) {
                        Text(
                            text = "${preset.config.bpm} BPM",
                            color = StudioColors.AccentStrong,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = preset.description,
                    color = StudioColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "删除预设",
                        tint = StudioColors.TextMuted
                    )
                }
            }
        }
    }
}
