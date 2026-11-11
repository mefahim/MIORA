package com.fahim.powerampclone.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Observable UI state for the Equalizer and Advanced DSP audio effects subsystem.
 */
data class EqualizerState(
    val enabled: Boolean = true,
    val preset: String = "Flat",
    val bandGains: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val frequencies: List<String> = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"),
    val isHardwareAttached: Boolean = false,
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0, // 0..1000
    val reverbPreset: String = "None", // "None", "Small Room", "Medium Room", "Large Room", "Medium Hall", "Large Hall", "Plate"
    val bassToneDb: Float = 0f, // -12f..+12f
    val trebleToneDb: Float = 0f, // -12f..+12f
    val isBassBoostSupported: Boolean = false,
    val isVirtualizerSupported: Boolean = false,
    val isReverbSupported: Boolean = false,
    val customPresets: Map<String, List<Float>> = emptyMap(),
)

/**
 * Comprehensive controller managing hardware Equalizer, BassBoost, Virtualizer,
 * and PresetReverb audio effects attached to active audio sessions.
 * Gracefully operates in virtual/simulation mode when native audiofx effects are unavailable.
 */
class EqualizerController(
    private val settingsStore: DspSettingsStore? = null,
) {

    companion object {
        val PRESETS: Map<String, List<Float>> = mapOf(
            "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Rock" to listOf(4f, 3f, 1f, 0f, -1f, 0f, 1f, 3f, 4f, 5f),
            "Pop" to listOf(-1f, 0f, 2f, 3f, 3f, 2f, 0f, -1f, -1f, 0f),
            "Jazz" to listOf(3f, 2f, 1f, 0f, -1f, 0f, 1f, 2f, 3f, 3f),
            "Classical" to listOf(4f, 3f, 2f, 0f, -1f, -1f, 0f, 2f, 3f, 4f),
            "Bass Boost" to listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Vocal" to listOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f),
            "Club" to listOf(3f, 4f, 2f, 0f, 0f, 0f, 2f, 3f, 4f, 3f),
            "Acoustic" to listOf(3f, 2f, 0f, 1f, 2f, 2f, 3f, 2f, 2f, 1f),
        )

        val REVERB_PRESETS = listOf(
            "None",
            "Small Room",
            "Medium Room",
            "Large Room",
            "Medium Hall",
            "Large Hall",
            "Plate",
        )

        const val MIN_GAIN_DB = -12f
        const val MAX_GAIN_DB = 12f
        const val MAX_EFFECT_STRENGTH = 1000
    }

    private var hardwareEqualizer: Equalizer? = null
    private var hardwareBassBoost: BassBoost? = null
    private var hardwareVirtualizer: Virtualizer? = null
    private var hardwareReverb: PresetReverb? = null
    private var currentSessionId: Int = 0

    private val _state = MutableStateFlow(
        settingsStore?.loadDspSettings() ?: EqualizerState()
    )
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    init {
        settingsStore?.let { store ->
            val customs = store.getCustomPresets()
            if (customs.isNotEmpty()) {
                _state.update { it.copy(customPresets = customs) }
            }
        }
    }

    @Synchronized
    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        currentSessionId = audioSessionId

        detach()

        // 1. Equalizer
        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.enabled
            }
            hardwareEqualizer = eq
            _state.update { it.copy(isHardwareAttached = true) }
            applyCurrentBandsToHardware()
        } catch (_: Throwable) {
            hardwareEqualizer = null
            _state.update { it.copy(isHardwareAttached = false) }
        }

        // 2. BassBoost
        try {
            val bb = BassBoost(0, audioSessionId).apply {
                enabled = _state.value.bassBoostEnabled
                if (strengthSupported) {
                    setStrength(_state.value.bassBoostStrength.toShort())
                }
            }
            hardwareBassBoost = bb
            _state.update { it.copy(isBassBoostSupported = true) }
        } catch (_: Throwable) {
            hardwareBassBoost = null
            _state.update { it.copy(isBassBoostSupported = false) }
        }

        // 3. Virtualizer
        try {
            val virt = Virtualizer(0, audioSessionId).apply {
                enabled = _state.value.virtualizerEnabled
                if (strengthSupported) {
                    setStrength(_state.value.virtualizerStrength.toShort())
                }
            }
            hardwareVirtualizer = virt
            _state.update { it.copy(isVirtualizerSupported = true) }
        } catch (_: Throwable) {
            hardwareVirtualizer = null
            _state.update { it.copy(isVirtualizerSupported = false) }
        }

        // 4. PresetReverb
        try {
            val rev = PresetReverb(0, audioSessionId).apply {
                val p = mapReverbNameToPreset(_state.value.reverbPreset)
                preset = p
                enabled = p != PresetReverb.PRESET_NONE
            }
            hardwareReverb = rev
            _state.update { it.copy(isReverbSupported = true) }
        } catch (_: Throwable) {
            hardwareReverb = null
            _state.update { it.copy(isReverbSupported = false) }
        }
    }

    @Synchronized
    fun detach() {
        try { hardwareEqualizer?.release() } catch (_: Throwable) {}
        try { hardwareBassBoost?.release() } catch (_: Throwable) {}
        try { hardwareVirtualizer?.release() } catch (_: Throwable) {}
        try { hardwareReverb?.release() } catch (_: Throwable) {}

        hardwareEqualizer = null
        hardwareBassBoost = null
        hardwareVirtualizer = null
        hardwareReverb = null

        _state.update {
            it.copy(
                isHardwareAttached = false,
                isBassBoostSupported = false,
                isVirtualizerSupported = false,
                isReverbSupported = false,
            )
        }
    }

    @Synchronized
    fun release() {
        detach()
        currentSessionId = 0
    }

    // --- Equalizer Controls ---

    @Synchronized
    fun setBandGain(bandIndex: Int, gainDb: Float) {
        val clamped = gainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        val currentGains = _state.value.bandGains.toMutableList()
        if (bandIndex in currentGains.indices) {
            currentGains[bandIndex] = clamped
            _state.update {
                it.copy(bandGains = currentGains, preset = "Custom")
            }
            applyBandToHardware(bandIndex, clamped)
            persistSettings()
        }
    }

    @Synchronized
    fun setPreset(presetName: String) {
        val gains = PRESETS[presetName] ?: _state.value.customPresets[presetName] ?: return
        _state.update {
            it.copy(preset = presetName, bandGains = gains)
        }
        applyCurrentBandsToHardware()
        persistSettings()
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        try {
            hardwareEqualizer?.enabled = enabled
        } catch (_: Throwable) {}
        persistSettings()
    }

    @Synchronized
    fun reset() {
        setPreset("Flat")
        setBassBoostEnabled(false)
        setBassBoostStrength(0)
        setVirtualizerEnabled(false)
        setVirtualizerStrength(0)
        setReverbPreset("None")
        setBassTone(0f)
        setTrebleTone(0f)
    }

    // --- Advanced DSP Controls ---

    @Synchronized
    fun setBassBoostEnabled(enabled: Boolean) {
        _state.update { it.copy(bassBoostEnabled = enabled) }
        try {
            hardwareBassBoost?.enabled = enabled
        } catch (_: Throwable) {}
        persistSettings()
    }

    @Synchronized
    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, MAX_EFFECT_STRENGTH)
        _state.update { it.copy(bassBoostStrength = clamped) }
        try {
            if (hardwareBassBoost?.strengthSupported == true) {
                hardwareBassBoost?.setStrength(clamped.toShort())
            }
        } catch (_: Throwable) {}
        persistSettings()
    }

    @Synchronized
    fun setVirtualizerEnabled(enabled: Boolean) {
        _state.update { it.copy(virtualizerEnabled = enabled) }
        try {
            hardwareVirtualizer?.enabled = enabled
        } catch (_: Throwable) {}
        persistSettings()
    }

    @Synchronized
    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, MAX_EFFECT_STRENGTH)
        _state.update { it.copy(virtualizerStrength = clamped) }
        try {
            if (hardwareVirtualizer?.strengthSupported == true) {
                hardwareVirtualizer?.setStrength(clamped.toShort())
            }
        } catch (_: Throwable) {}
        persistSettings()
    }

    @Synchronized
    fun setReverbPreset(presetName: String) {
        _state.update { it.copy(reverbPreset = presetName) }
        try {
            val p = mapReverbNameToPreset(presetName)
            hardwareReverb?.preset = p
            hardwareReverb?.enabled = (p != PresetReverb.PRESET_NONE)
        } catch (_: Throwable) {}
        persistSettings()
    }

    @Synchronized
    fun setBassTone(gainDb: Float) {
        val clamped = gainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        _state.update { it.copy(bassToneDb = clamped) }
        // Scale the low frequency bands (31Hz, 62Hz, 125Hz) proportional to bass tone
        val currentGains = _state.value.bandGains.toMutableList()
        currentGains[0] = (currentGains[0] * 0.5f + clamped * 0.5f).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        currentGains[1] = (currentGains[1] * 0.5f + clamped * 0.4f).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        currentGains[2] = (currentGains[2] * 0.5f + clamped * 0.3f).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        _state.update { it.copy(bandGains = currentGains) }
        applyCurrentBandsToHardware()
        persistSettings()
    }

    @Synchronized
    fun setTrebleTone(gainDb: Float) {
        val clamped = gainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        _state.update { it.copy(trebleToneDb = clamped) }
        // Scale the high frequency bands (4kHz, 8kHz, 16kHz) proportional to treble tone
        val currentGains = _state.value.bandGains.toMutableList()
        currentGains[7] = (currentGains[7] * 0.5f + clamped * 0.3f).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        currentGains[8] = (currentGains[8] * 0.5f + clamped * 0.4f).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        currentGains[9] = (currentGains[9] * 0.5f + clamped * 0.5f).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        _state.update { it.copy(bandGains = currentGains) }
        applyCurrentBandsToHardware()
        persistSettings()
    }

    // --- Custom Preset Management ---

    @Synchronized
    fun saveCustomPreset(name: String) {
        val currentGains = _state.value.bandGains
        val updatedCustoms = _state.value.customPresets + (name to currentGains)
        _state.update { it.copy(customPresets = updatedCustoms, preset = name) }
        settingsStore?.saveCustomPreset(name, currentGains)
        persistSettings()
    }

    @Synchronized
    fun deleteCustomPreset(name: String) {
        val updatedCustoms = _state.value.customPresets - name
        val nextPreset = if (_state.value.preset == name) "Flat" else _state.value.preset
        _state.update { it.copy(customPresets = updatedCustoms, preset = nextPreset) }
        settingsStore?.deleteCustomPreset(name)
        persistSettings()
    }

    // --- Internal Hardware Helpers ---

    private fun applyBandToHardware(bandIndex: Int, gainDb: Float) {
        val eq = hardwareEqualizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            if (numBands > 0) {
                val targetBand = (bandIndex * numBands / 10).coerceIn(0, numBands - 1).toShort()
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                val millibels = (gainDb * 100).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                eq.setBandLevel(targetBand, millibels)
            }
        } catch (_: Throwable) {}
    }

    private fun applyCurrentBandsToHardware() {
        val gains = _state.value.bandGains
        gains.forEachIndexed { index, gain ->
            applyBandToHardware(index, gain)
        }
    }

    private fun mapReverbNameToPreset(name: String): Short {
        return when (name) {
            "Small Room" -> PresetReverb.PRESET_SMALLROOM
            "Medium Room" -> PresetReverb.PRESET_MEDIUMROOM
            "Large Room" -> PresetReverb.PRESET_LARGEROOM
            "Medium Hall" -> PresetReverb.PRESET_MEDIUMHALL
            "Large Hall" -> PresetReverb.PRESET_LARGEHALL
            "Plate" -> PresetReverb.PRESET_PLATE
            else -> PresetReverb.PRESET_NONE
        }
    }

    private fun persistSettings() {
        try {
            settingsStore?.saveDspSettings(_state.value)
        } catch (_: Throwable) {}
    }
}
