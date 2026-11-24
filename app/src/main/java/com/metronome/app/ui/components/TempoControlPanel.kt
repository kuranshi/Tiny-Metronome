package com.metronome.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.ui.theme.StudioColors
import kotlin.math.roundToInt

@Composable
fun TempoControlPanel(
    bpm: Int,
    tempoMarking: String,
    nativeSampleRate: Int,
    onBpmChange: (Int) -> Unit,
    onAdjustBpm: (Int) -> Unit,
    onTapTempo: () -> Unit,
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
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top metadata row: Italian tempo mark + Hardware sample-clock badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tempoMarking,
                    color = StudioColors.AccentMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioColors.SurfaceElevated
                ) {
                    Text(
                        text = "${nativeSampleRate / 1000}kHz 采样时钟",
                        color = StudioColors.PolyStrong,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Large BPM readout
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = bpm.toString(),
                    color = StudioColors.TextPrimary,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 68.sp
                )
                Text(
                    text = "BPM",
                    color = StudioColors.TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Smooth BPM Slider
            Slider(
                value = bpm.toFloat(),
                onValueChange = { onBpmChange(it.roundToInt()) },
                valueRange = MetronomeConfig.MIN_BPM.toFloat()..MetronomeConfig.MAX_BPM.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = StudioColors.AccentStrong,
                    activeTrackColor = StudioColors.AccentStrong,
                    inactiveTrackColor = StudioColors.SurfaceHighlight
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Fine tuning buttons + TAP Tempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BpmStepButton("-5", onClick = { onAdjustBpm(-5) }, modifier = Modifier.weight(1f))
                BpmStepButton("-1", onClick = { onAdjustBpm(-1) }, modifier = Modifier.weight(1f))

                Surface(
                    modifier = Modifier
                        .weight(1.5f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onTapTempo),
                    shape = RoundedCornerShape(14.dp),
                    color = StudioColors.AccentStrong.copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, StudioColors.AccentStrong)
                ) {
                    Text(
                        text = "TAP 测速",
                        color = StudioColors.AccentStrong,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                BpmStepButton("+1", onClick = { onAdjustBpm(1) }, modifier = Modifier.weight(1f))
                BpmStepButton("+5", onClick = { onAdjustBpm(5) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BpmStepButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = StudioColors.SurfaceElevated,
        border = BorderStroke(1.dp, StudioColors.Outline.copy(alpha = 0.6f))
    ) {
        Text(
            text = label,
            color = StudioColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}
