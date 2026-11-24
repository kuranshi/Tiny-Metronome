package com.metronome.app.data

import android.content.Context
import com.metronome.app.model.AccentLevel
import com.metronome.app.model.MetronomeConfig
import com.metronome.app.model.MetronomePreset
import com.metronome.app.model.RhythmMode
import com.metronome.app.model.SoundKit
import com.metronome.app.model.SubdivisionType
import org.json.JSONArray
import org.json.JSONObject

class PresetRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadLastConfig(): MetronomeConfig {
        val raw = prefs.getString(KEY_LAST_CONFIG, null) ?: return MetronomeConfig()
        return runCatching {
            configFromJson(JSONObject(raw)).sanitized()
        }.getOrDefault(MetronomeConfig())
    }

    fun saveLastConfig(config: MetronomeConfig) {
        prefs.edit()
            .putString(KEY_LAST_CONFIG, configToJson(config.sanitized()).toString())
            .apply()
    }

    fun loadUserPresets(): List<MetronomePreset> {
        val raw = prefs.getString(KEY_USER_PRESETS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        MetronomePreset(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            config = configFromJson(obj.getJSONObject("config")).sanitized(),
                            isBuiltIn = false
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveUserPresets(presets: List<MetronomePreset>) {
        val array = JSONArray()
        presets.filter { !it.isBuiltIn }.forEach { preset ->
            val obj = JSONObject().apply {
                put("id", preset.id)
                put("name", preset.name)
                put("description", preset.description)
                put("config", configToJson(preset.config.sanitized()))
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_USER_PRESETS, array.toString()).apply()
    }

    private fun configToJson(c: MetronomeConfig): JSONObject = JSONObject().apply {
        put("bpm", c.bpm)
        put("mode", c.mode.name)
        put("beatsPerBar", c.beatsPerBar)
        put("beatUnit", c.beatUnit)
        put("subdivision", c.subdivision.name)
        put("beatAccents", accentsToJson(c.beatAccents))
        put("subdivisionAccent", c.subdivisionAccent.name)
        put("compoundGroups", JSONArray(c.compoundGroups))
        put("compoundUnit", c.compoundUnit)
        put("compoundAccents", accentsToJson(c.compoundAccents))
        put("polyPrimaryBeats", c.polyPrimaryBeats)
        put("polySecondaryBeats", c.polySecondaryBeats)
        put("polyPrimaryAccents", accentsToJson(c.polyPrimaryAccents))
        put("polySecondaryAccents", accentsToJson(c.polySecondaryAccents))
        put("primarySound", c.primarySound.name)
        put("secondarySound", c.secondarySound.name)
        put("keepScreenOn", c.keepScreenOn)
    }

    private fun configFromJson(obj: JSONObject): MetronomeConfig {
        val groupsArr = obj.optJSONArray("compoundGroups")
        val compoundGroups = if (groupsArr != null && groupsArr.length() > 0) {
            List(groupsArr.length()) { idx -> groupsArr.optInt(idx, 2) }
        } else {
            listOf(3, 2, 2)
        }

        return MetronomeConfig(
            bpm = obj.optInt("bpm", 120),
            mode = enumOrDefault(obj.optString("mode"), RhythmMode.STANDARD),
            beatsPerBar = obj.optInt("beatsPerBar", 4),
            beatUnit = obj.optInt("beatUnit", 4),
            subdivision = enumOrDefault(obj.optString("subdivision"), SubdivisionType.QUARTER),
            beatAccents = accentsFromJson(obj.optJSONArray("beatAccents"), obj.optInt("beatsPerBar", 4)),
            subdivisionAccent = enumOrDefault(obj.optString("subdivisionAccent"), AccentLevel.NORMAL),
            compoundGroups = compoundGroups,
            compoundUnit = obj.optInt("compoundUnit", 8),
            compoundAccents = accentsFromJson(
                obj.optJSONArray("compoundAccents"),
                compoundGroups.sum(),
                fallback = MetronomeConfig.defaultCompoundAccents(compoundGroups)
            ),
            polyPrimaryBeats = obj.optInt("polyPrimaryBeats", 3),
            polySecondaryBeats = obj.optInt("polySecondaryBeats", 2),
            polyPrimaryAccents = accentsFromJson(obj.optJSONArray("polyPrimaryAccents"), obj.optInt("polyPrimaryBeats", 3)),
            polySecondaryAccents = accentsFromJson(obj.optJSONArray("polySecondaryAccents"), obj.optInt("polySecondaryBeats", 2)),
            primarySound = enumOrDefault(obj.optString("primarySound"), SoundKit.WOODBLOCK),
            secondarySound = enumOrDefault(obj.optString("secondarySound"), SoundKit.RIMSHOT),
            keepScreenOn = obj.optBoolean("keepScreenOn", true)
        )
    }

    private fun accentsToJson(list: List<AccentLevel>): JSONArray =
        JSONArray().apply { list.forEach { put(it.name) } }

    private fun accentsFromJson(
        arr: JSONArray?,
        expectedSize: Int,
        fallback: List<AccentLevel>? = null
    ): List<AccentLevel> {
        if (arr == null || arr.length() == 0) {
            return fallback ?: MetronomeConfig.defaultAccents(expectedSize)
        }
        return List(arr.length()) { idx ->
            enumOrDefault(arr.optString(idx), AccentLevel.NORMAL)
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T {
        if (name.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }

    companion object {
        private const val PREFS_NAME = "precision_metronome_prefs"
        private const val KEY_LAST_CONFIG = "last_config_v1"
        private const val KEY_USER_PRESETS = "user_presets_v1"
    }
}
