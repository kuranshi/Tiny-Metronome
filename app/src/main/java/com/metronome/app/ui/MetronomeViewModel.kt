package com.metronome.app.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import com.metronome.app.data.PresetRepository
import com.metronome.app.engine.MetronomeAudioEngine
import com.metronome.app.engine.ScheduledTickRecord
import com.metronome.app.engine.TapTempoCalculator
import com.metronome.app.model.AccentLevel
import com.metronome.app.model.DefaultPresets
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.MetronomePreset
import com.metronome.app.model.RhythmMode
import com.metronome.app.model.SoundKit
import com.metronome.app.model.SubdivisionType
import com.metronome.app.model.VoiceTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class MetronomeUiState(
    val config: MetronomeConfig = MetronomeConfig(),
    val isPlaying: Boolean = false,
    val activePrimaryBeat: Int = -1,
    val activePrimarySub: Int = 0,
    val activeSecondaryBeat: Int = -1,
    val tickPulseSeq: Long = 0L,
    val nativeSampleRate: Int = 48000,
    val builtInPresets: List<MetronomePreset> = DefaultPresets.builtInPresets,
    val userPresets: List<MetronomePreset> = emptyList(),
    val tapFlashSeq: Long = 0L
)

class MetronomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PresetRepository(application)
    private val tapCalculator = TapTempoCalculator()

    private val initialConfig = repository.loadLastConfig()

    private val _uiState = MutableStateFlow(
        MetronomeUiState(
            config = initialConfig,
            userPresets = repository.loadUserPresets()
        )
    )
    val uiState: StateFlow<MetronomeUiState> = _uiState.asStateFlow()

    private val audioEngine = MetronomeAudioEngine(
        context = application,
        initialConfig = initialConfig,
        onVisualTick = ::handleVisualTick,
        onAudioFocusLost = ::handleAudioFocusLost
    )

    init {
        _uiState.update { it.copy(nativeSampleRate = audioEngine.nativeSampleRate) }
    }

    private fun handleVisualTick(tick: ScheduledTickRecord) {
        _uiState.update { state ->
            if (!state.isPlaying) return@update state
            when (tick.voiceTrack) {
                VoiceTrack.PRIMARY -> state.copy(
                    activePrimaryBeat = tick.beatIndex,
                    activePrimarySub = tick.subIndex,
                    tickPulseSeq = state.tickPulseSeq + 1L
                )
                VoiceTrack.SECONDARY -> state.copy(
                    activeSecondaryBeat = tick.beatIndex,
                    tickPulseSeq = state.tickPulseSeq + 1L
                )
            }
        }
    }

    private fun handleAudioFocusLost() {
        _uiState.update {
            it.copy(
                isPlaying = false,
                activePrimaryBeat = -1,
                activePrimarySub = 0,
                activeSecondaryBeat = -1
            )
        }
    }

    private fun updateConfig(transform: (MetronomeConfig) -> MetronomeConfig) {
        val next = transform(_uiState.value.config).sanitized()
        _uiState.update { it.copy(config = next) }
        audioEngine.updateConfig(next)
        repository.saveLastConfig(next)
    }

    fun togglePlay() {
        val currentlyPlaying = _uiState.value.isPlaying
        if (currentlyPlaying) {
            stopPlayback()
        } else {
            _uiState.update {
                it.copy(
                    isPlaying = true,
                    activePrimaryBeat = -1,
                    activePrimarySub = 0,
                    activeSecondaryBeat = -1
                )
            }
            audioEngine.start()
        }
    }

    fun stopPlayback() {
        audioEngine.stop()
        _uiState.update {
            it.copy(
                isPlaying = false,
                activePrimaryBeat = -1,
                activePrimarySub = 0,
                activeSecondaryBeat = -1
            )
        }
    }

    fun setBpm(bpm: Int) {
        updateConfig { it.copy(bpm = bpm.coerceIn(MetronomeConfig.MIN_BPM, MetronomeConfig.MAX_BPM)) }
    }

    fun adjustBpm(delta: Int) {
        setBpm(_uiState.value.config.bpm + delta)
    }

    fun onTapTempo() {
        val detected = tapCalculator.recordTap(SystemClock.elapsedRealtime())
        _uiState.update { it.copy(tapFlashSeq = it.tapFlashSeq + 1L) }
        if (detected != null) {
            setBpm(detected)
        }
    }

    fun setRhythmMode(mode: RhythmMode) {
        updateConfig { it.copy(mode = mode) }
    }

    fun setTimeSignature(beatsPerBar: Int, beatUnit: Int) {
        val safeBeats = beatsPerBar.coerceIn(1, MetronomeConfig.MAX_BEATS)
        updateConfig { current ->
            val resized = MetronomeConfig.resizeAccents(current.beatAccents, safeBeats)
            current.copy(
                mode = RhythmMode.STANDARD,
                beatsPerBar = safeBeats,
                beatUnit = beatUnit,
                beatAccents = resized
            )
        }
    }

    fun setSubdivision(subdivision: SubdivisionType) {
        updateConfig { it.copy(mode = RhythmMode.STANDARD, subdivision = subdivision) }
    }

    fun setSubdivisionAccent(level: AccentLevel) {
        updateConfig { it.copy(subdivisionAccent = level) }
    }

    fun setCompoundMeter(groups: List<Int>, unit: Int) {
        val cleaned = groups.map { it.coerceIn(1, 5) }.ifEmpty { listOf(3, 2) }
        updateConfig {
            it.copy(
                mode = RhythmMode.COMPOUND,
                compoundGroups = cleaned,
                compoundUnit = unit,
                compoundAccents = MetronomeConfig.defaultCompoundAccents(cleaned)
            )
        }
    }

    fun setPolyrhythm(primaryBeats: Int, secondaryBeats: Int) {
        val p = primaryBeats.coerceIn(2, MetronomeConfig.MAX_POLY_BEATS)
        val s = secondaryBeats.coerceIn(2, MetronomeConfig.MAX_POLY_BEATS)
        updateConfig { current ->
            current.copy(
                mode = RhythmMode.POLYRHYTHM,
                polyPrimaryBeats = p,
                polySecondaryBeats = s,
                polyPrimaryAccents = MetronomeConfig.resizeAccents(current.polyPrimaryAccents, p),
                polySecondaryAccents = MetronomeConfig.resizeAccents(current.polySecondaryAccents, s)
            )
        }
    }

    fun cyclePrimaryBeatAccent(index: Int) {
        updateConfig { c ->
            when (c.mode) {
                RhythmMode.STANDARD -> {
                    if (index !in c.beatAccents.indices) return@updateConfig c
                    val updated = c.beatAccents.toMutableList()
                    updated[index] = updated[index].next()
                    c.copy(beatAccents = updated)
                }
                RhythmMode.COMPOUND -> {
                    if (index !in c.compoundAccents.indices) return@updateConfig c
                    val updated = c.compoundAccents.toMutableList()
                    updated[index] = updated[index].next()
                    c.copy(compoundAccents = updated)
                }
                RhythmMode.POLYRHYTHM -> {
                    if (index !in c.polyPrimaryAccents.indices) return@updateConfig c
                    val updated = c.polyPrimaryAccents.toMutableList()
                    updated[index] = updated[index].next()
                    c.copy(polyPrimaryAccents = updated)
                }
            }
        }
    }

    fun cyclePolySecondaryAccent(index: Int) {
        updateConfig { c ->
            if (index !in c.polySecondaryAccents.indices) return@updateConfig c
            val updated = c.polySecondaryAccents.toMutableList()
            updated[index] = updated[index].next()
            c.copy(polySecondaryAccents = updated)
        }
    }

    fun applyQuickAccentPattern(pattern: QuickAccentPattern) {
        updateConfig { c ->
            when (c.mode) {
                RhythmMode.STANDARD -> {
                    val count = c.beatsPerBar
                    val accents = buildQuickAccents(count, pattern)
                    c.copy(beatAccents = accents)
                }
                RhythmMode.COMPOUND -> {
                    val accents = when (pattern) {
                        QuickAccentPattern.FIRST_STRONG -> MetronomeConfig.defaultCompoundAccents(c.compoundGroups)
                        else -> buildQuickAccents(c.compoundGroups.sum(), pattern)
                    }
                    c.copy(compoundAccents = accents)
                }
                RhythmMode.POLYRHYTHM -> {
                    c.copy(
                        polyPrimaryAccents = buildQuickAccents(c.polyPrimaryBeats, pattern),
                        polySecondaryAccents = buildQuickAccents(c.polySecondaryBeats, pattern)
                    )
                }
            }
        }
    }

    private fun buildQuickAccents(count: Int, pattern: QuickAccentPattern): List<AccentLevel> =
        when (pattern) {
            QuickAccentPattern.FIRST_STRONG -> MetronomeConfig.defaultAccents(count)
            QuickAccentPattern.BACKBEAT -> List(count) { idx ->
                if (idx % 2 == 1) AccentLevel.ACCENT else AccentLevel.NORMAL
            }
            QuickAccentPattern.ALL_EQUAL -> List(count) { AccentLevel.NORMAL }
            QuickAccentPattern.ODD_STRONG -> List(count) { idx ->
                if (idx % 2 == 0) AccentLevel.ACCENT else AccentLevel.NORMAL
            }
        }

    fun setPrimarySound(kit: SoundKit) {
        updateConfig { it.copy(primarySound = kit) }
        if (!_uiState.value.isPlaying) {
            audioEngine.previewSound(kit, AccentLevel.ACCENT)
        }
    }

    fun setSecondarySound(kit: SoundKit) {
        updateConfig { it.copy(secondarySound = kit) }
        if (!_uiState.value.isPlaying) {
            audioEngine.previewSound(kit, AccentLevel.MEDIUM)
        }
    }

    fun previewSound(kit: SoundKit) {
        audioEngine.previewSound(kit, AccentLevel.ACCENT)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        updateConfig { it.copy(keepScreenOn = enabled) }
    }

    fun saveUserPreset(name: String, description: String = "") {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val currentConfig = _uiState.value.config
        val desc = description.trim().ifEmpty {
            "${currentConfig.summaryBadge()} · ${currentConfig.bpm} BPM"
        }
        val newPreset = MetronomePreset(
            id = "user_${UUID.randomUUID()}",
            name = trimmed,
            description = desc,
            config = currentConfig,
            isBuiltIn = false
        )
        val updated = listOf(newPreset) + _uiState.value.userPresets
        _uiState.update { it.copy(userPresets = updated) }
        repository.saveUserPresets(updated)
    }

    fun loadPreset(preset: MetronomePreset) {
        updateConfig { preset.config }
    }

    fun deleteUserPreset(id: String) {
        val updated = _uiState.value.userPresets.filterNot { it.id == id }
        _uiState.update { it.copy(userPresets = updated) }
        repository.saveUserPresets(updated)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}

enum class QuickAccentPattern(val label: String) {
    FIRST_STRONG("默认重音"),
    BACKBEAT("反拍 2&4"),
    ODD_STRONG("单数强拍"),
    ALL_EQUAL("全部均等")
}
