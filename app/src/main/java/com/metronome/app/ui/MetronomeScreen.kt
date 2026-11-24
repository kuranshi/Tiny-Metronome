package com.metronome.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metronome.app.ui.components.BeatGridVisualizer
import com.metronome.app.ui.components.PresetSheet
import com.metronome.app.ui.components.RhythmTypeSheet
import com.metronome.app.ui.components.SoundSettingsSheet
import com.metronome.app.ui.components.TempoControlPanel
import com.metronome.app.ui.theme.StudioColors

@Composable
fun MetronomeScreen(
    viewModel: MetronomeViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showRhythmSheet by remember { mutableStateOf(false) }
    var showSoundSheet by remember { mutableStateOf(false) }
    var showPresetSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = StudioColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioColors.Background)
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Studio Header Bar
            TopActionHeader(
                rhythmSummary = state.config.summaryBadge(),
                soundLabel = state.config.primarySound.displayName,
                onOpenRhythm = { showRhythmSheet = true },
                onOpenSound = { showSoundSheet = true },
                onOpenPresets = { showPresetSheet = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Responsive Body: Android 16 Large Screen / Landscape (>= 600dp) uses side-by-side layout
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isExpandedWidth = maxWidth >= 600.dp

                if (isExpandedWidth) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        BeatGridVisualizer(
                            config = state.config,
                            isPlaying = state.isPlaying,
                            activePrimaryBeat = state.activePrimaryBeat,
                            activePrimarySub = state.activePrimarySub,
                            activeSecondaryBeat = state.activeSecondaryBeat,
                            onTapPrimaryBeat = viewModel::cyclePrimaryBeatAccent,
                            onTapSecondaryBeat = viewModel::cyclePolySecondaryAccent,
                            onApplyQuickPattern = viewModel::applyQuickAccentPattern,
                            modifier = Modifier.weight(1.15f)
                        )
                        TempoControlPanel(
                            bpm = state.config.bpm,
                            tempoMarking = state.config.tempoMarking(),
                            nativeSampleRate = state.nativeSampleRate,
                            onBpmChange = viewModel::setBpm,
                            onAdjustBpm = viewModel::adjustBpm,
                            onTapTempo = viewModel::onTapTempo,
                            modifier = Modifier.weight(0.85f)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BeatGridVisualizer(
                            config = state.config,
                            isPlaying = state.isPlaying,
                            activePrimaryBeat = state.activePrimaryBeat,
                            activePrimarySub = state.activePrimarySub,
                            activeSecondaryBeat = state.activeSecondaryBeat,
                            onTapPrimaryBeat = viewModel::cyclePrimaryBeatAccent,
                            onTapSecondaryBeat = viewModel::cyclePolySecondaryAccent,
                            onApplyQuickPattern = viewModel::applyQuickAccentPattern
                        )
                        TempoControlPanel(
                            bpm = state.config.bpm,
                            tempoMarking = state.config.tempoMarking(),
                            nativeSampleRate = state.nativeSampleRate,
                            onBpmChange = viewModel::setBpm,
                            onAdjustBpm = viewModel::adjustBpm,
                            onTapTempo = viewModel::onTapTempo
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Primary Transport Bar
            TransportBottomBar(
                isPlaying = state.isPlaying,
                onTogglePlay = viewModel::togglePlay,
                onOpenRhythm = { showRhythmSheet = true },
                onOpenSound = { showSoundSheet = true }
            )
        }
    }

    if (showRhythmSheet) {
        RhythmTypeSheet(
            config = state.config,
            onDismiss = { showRhythmSheet = false },
            onSelectMode = viewModel::setRhythmMode,
            onSetTimeSignature = viewModel::setTimeSignature,
            onSetSubdivision = viewModel::setSubdivision,
            onSetSubdivisionAccent = viewModel::setSubdivisionAccent,
            onSetCompoundMeter = viewModel::setCompoundMeter,
            onSetPolyrhythm = viewModel::setPolyrhythm
        )
    }

    if (showSoundSheet) {
        SoundSettingsSheet(
            config = state.config,
            onDismiss = { showSoundSheet = false },
            onSelectPrimarySound = viewModel::setPrimarySound,
            onSelectSecondarySound = viewModel::setSecondarySound,
            onPreviewSound = viewModel::previewSound,
            onToggleKeepScreenOn = viewModel::setKeepScreenOn
        )
    }

    if (showPresetSheet) {
        PresetSheet(
            currentConfig = state.config,
            userPresets = state.userPresets,
            builtInPresets = state.builtInPresets,
            onDismiss = { showPresetSheet = false },
            onSavePreset = { name -> viewModel.saveUserPreset(name) },
            onLoadPreset = viewModel::loadPreset,
            onDeletePreset = viewModel::deleteUserPreset
        )
    }
}

@Composable
private fun TopActionHeader(
    rhythmSummary: String,
    soundLabel: String,
    onOpenRhythm: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenPresets: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clickable Rhythm Summary Pill
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenRhythm),
            shape = RoundedCornerShape(14.dp),
            color = StudioColors.Surface,
            border = BorderStroke(1.dp, StudioColors.AccentStrong.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = "节拍类型",
                    tint = StudioColors.AccentStrong,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = rhythmSummary,
                    color = StudioColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Sound Kit quick button
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenSound),
            shape = RoundedCornerShape(14.dp),
            color = StudioColors.Surface,
            border = BorderStroke(1.dp, StudioColors.Outline.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = "音色",
                    tint = StudioColors.PolyStrong,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = soundLabel,
                    color = StudioColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Presets button
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenPresets),
            shape = RoundedCornerShape(14.dp),
            color = StudioColors.Surface,
            border = BorderStroke(1.dp, StudioColors.Outline.copy(alpha = 0.6f))
        ) {
            Icon(
                imageVector = Icons.Rounded.Bookmarks,
                contentDescription = "预设库",
                tint = StudioColors.AccentMedium,
                modifier = Modifier.padding(10.dp).size(18.dp)
            )
        }
    }
}

@Composable
private fun TransportBottomBar(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onOpenRhythm: () -> Unit,
    onOpenSound: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenRhythm),
            shape = RoundedCornerShape(16.dp),
            color = StudioColors.SurfaceElevated,
            border = BorderStroke(1.dp, StudioColors.Outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "节拍设置",
                    tint = StudioColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "节拍类型",
                    color = StudioColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Large Central Play/Stop Button
        Surface(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(onClick = onTogglePlay),
            shape = CircleShape,
            color = if (isPlaying) StudioColors.PlayActive else StudioColors.PlayIdle,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "停止" else "开始",
                    tint = StudioColors.Background,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenSound),
            shape = RoundedCornerShape(16.dp),
            color = StudioColors.SurfaceElevated,
            border = BorderStroke(1.dp, StudioColors.Outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = "音色设置",
                    tint = StudioColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "音色设置",
                    color = StudioColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
