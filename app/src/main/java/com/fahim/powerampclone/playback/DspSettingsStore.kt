package com.fahim.powerampclone.playback

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Contract for persisting and restoring equalizer and advanced DSP audio effect configurations.
 */
interface DspSettingsStore {
    fun saveDspSettings(state: EqualizerState)
    fun loadDspSettings(): EqualizerState?
    fun saveCustomPreset(name: String, bandGains: List<Float>)
    fun getCustomPresets(): Map<String, List<Float>>
    fun deleteCustomPreset(name: String)
}

/**
 * Production SharedPreferences-backed DSP storage.
 */
class SharedPreferencesDspSettingsStore(context: Context) : DspSettingsStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "miora_dsp_settings"
        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_PRESET = "eq_preset"
        private const val KEY_BAND_PREFIX = "eq_band_"
        private const val KEY_BASS_BOOST_ENABLED = "dsp_bass_boost_enabled"
        private const val KEY_BASS_BOOST_STRENGTH = "dsp_bass_boost_strength"
        private const val KEY_VIRTUALIZER_ENABLED = "dsp_virtualizer_enabled"
        private const val KEY_VIRTUALIZER_STRENGTH = "dsp_virtualizer_strength"
        private const val KEY_REVERB_PRESET = "dsp_reverb_preset"
        private const val KEY_BASS_TONE = "dsp_bass_tone"
        private const val KEY_TREBLE_TONE = "dsp_treble_tone"
        private const val KEY_CUSTOM_PRESETS_PREFIX = "custom_preset_"
    }

    override fun saveDspSettings(state: EqualizerState) {
        try {
            val editor = prefs.edit()
            editor.putBoolean(KEY_ENABLED, state.enabled)
            editor.putString(KEY_PRESET, state.preset)
            state.bandGains.forEachIndexed { index, gain ->
                editor.putFloat(KEY_BAND_PREFIX + index, gain)
            }
            editor.putBoolean(KEY_BASS_BOOST_ENABLED, state.bassBoostEnabled)
            editor.putInt(KEY_BASS_BOOST_STRENGTH, state.bassBoostStrength)
            editor.putBoolean(KEY_VIRTUALIZER_ENABLED, state.virtualizerEnabled)
            editor.putInt(KEY_VIRTUALIZER_STRENGTH, state.virtualizerStrength)
            editor.putString(KEY_REVERB_PRESET, state.reverbPreset)
            editor.putFloat(KEY_BASS_TONE, state.bassToneDb)
            editor.putFloat(KEY_TREBLE_TONE, state.trebleToneDb)
            editor.apply()
        } catch (_: Exception) {}
    }

    override fun loadDspSettings(): EqualizerState? {
        return try {
            if (!prefs.contains(KEY_ENABLED)) return null
            val enabled = prefs.getBoolean(KEY_ENABLED, true)
            val preset = prefs.getString(KEY_PRESET, "Flat") ?: "Flat"
            val gains = (0 until 10).map { i ->
                prefs.getFloat(KEY_BAND_PREFIX + i, 0f)
            }
            val bbEnabled = prefs.getBoolean(KEY_BASS_BOOST_ENABLED, false)
            val bbStrength = prefs.getInt(KEY_BASS_BOOST_STRENGTH, 0)
            val virtEnabled = prefs.getBoolean(KEY_VIRTUALIZER_ENABLED, false)
            val virtStrength = prefs.getInt(KEY_VIRTUALIZER_STRENGTH, 0)
            val reverb = prefs.getString(KEY_REVERB_PRESET, "None") ?: "None"
            val bassTone = prefs.getFloat(KEY_BASS_TONE, 0f)
            val trebleTone = prefs.getFloat(KEY_TREBLE_TONE, 0f)

            EqualizerState(
                enabled = enabled,
                preset = preset,
                bandGains = gains,
                bassBoostEnabled = bbEnabled,
                bassBoostStrength = bbStrength,
                virtualizerEnabled = virtEnabled,
                virtualizerStrength = virtStrength,
                reverbPreset = reverb,
                bassToneDb = bassTone,
                trebleToneDb = trebleTone,
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun saveCustomPreset(name: String, bandGains: List<Float>) {
        try {
            val encoded = bandGains.joinToString(",") { it.toString() }
            prefs.edit().putString(KEY_CUSTOM_PRESETS_PREFIX + name, encoded).apply()
        } catch (_: Exception) {}
    }

    override fun getCustomPresets(): Map<String, List<Float>> {
        val results = mutableMapOf<String, List<Float>>()
        try {
            for ((key, value) in prefs.all) {
                if (key.startsWith(KEY_CUSTOM_PRESETS_PREFIX) && value is String) {
                    val name = key.removePrefix(KEY_CUSTOM_PRESETS_PREFIX)
                    val gains = value.split(",").mapNotNull { it.toFloatOrNull() }
                    if (gains.size == 10) {
                        results[name] = gains
                    }
                }
            }
        } catch (_: Exception) {}
        return results
    }

    override fun deleteCustomPreset(name: String) {
        try {
            prefs.edit().remove(KEY_CUSTOM_PRESETS_PREFIX + name).apply()
        } catch (_: Exception) {}
    }
}

/**
 * Fast in-memory implementation for unit testing and headless environments.
 */
class InMemoryDspSettingsStore : DspSettingsStore {
    private var savedState: EqualizerState? = null
    private val customPresets = ConcurrentHashMap<String, List<Float>>()

    override fun saveDspSettings(state: EqualizerState) {
        savedState = state
    }

    override fun loadDspSettings(): EqualizerState? = savedState

    override fun saveCustomPreset(name: String, bandGains: List<Float>) {
        customPresets[name] = bandGains
    }

    override fun getCustomPresets(): Map<String, List<Float>> = customPresets.toMap()

    override fun deleteCustomPreset(name: String) {
        customPresets.remove(name)
    }
}
