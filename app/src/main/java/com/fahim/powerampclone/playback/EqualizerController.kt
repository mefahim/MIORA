package com.fahim.powerampclone.playback

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Observable UI state for the Equalizer subsystem.
 */
data class EqualizerState(
    val enabled: Boolean = true,
    val preset: String = "Flat",
    val bandGains: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val frequencies: List<String> = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"),
    val isHardwareAttached: Boolean = false,
)

/**
 * Controller managing hardware audio equalization via Android's Equalizer audio effect.
 * Gracefully operates in virtual/simulation mode when native audiofx effects are unavailable
 * (such as in headless JVM unit test environments or restricted devices).
 */
class EqualizerController {

    companion object {
        val PRESETS: Map<String, List<Float>> = mapOf(
            "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Rock" to listOf(4f, 3f, 1f, 0f, -1f, 0f, 1f, 3f, 4f, 5f),
            "Pop" to listOf(-1f, 0f, 2f, 3f, 3f, 2f, 0f, -1f, -1f, 0f),
            "Jazz" to listOf(3f, 2f, 1f, 0f, -1f, 0f, 1f, 2f, 3f, 3f),
            "Classical" to listOf(4f, 3f, 2f, 0f, -1f, -1f, 0f, 2f, 3f, 4f),
            "Bass Boost" to listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
            "Vocal" to listOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f),
        )

        const val MIN_GAIN_DB = -12f
        const val MAX_GAIN_DB = 12f
    }

    private var hardwareEqualizer: Equalizer? = null
    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    @Synchronized
    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        try {
            hardwareEqualizer?.release()
            hardwareEqualizer = null

            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.enabled
            }
            hardwareEqualizer = eq
            _state.update { it.copy(isHardwareAttached = true) }
            applyCurrentBandsToHardware()
        } catch (_: Throwable) {
            // Catches RuntimeException / UnsatisfiedLinkError on emulator / test JVMs without AudioFlinger
            hardwareEqualizer = null
            _state.update { it.copy(isHardwareAttached = false) }
        }
    }

    @Synchronized
    fun detach() {
        try {
            hardwareEqualizer?.release()
        } catch (_: Throwable) {}
        hardwareEqualizer = null
        _state.update { it.copy(isHardwareAttached = false) }
    }

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
        }
    }

    @Synchronized
    fun setPreset(presetName: String) {
        val gains = PRESETS[presetName] ?: return
        _state.update {
            it.copy(preset = presetName, bandGains = gains)
        }
        applyCurrentBandsToHardware()
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
        try {
            hardwareEqualizer?.enabled = enabled
        } catch (_: Throwable) {}
    }

    @Synchronized
    fun reset() {
        setPreset("Flat")
    }

    private fun applyBandToHardware(bandIndex: Int, gainDb: Float) {
        val eq = hardwareEqualizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            if (numBands > 0) {
                // Map the 10 UI logical bands to the device's physical bands
                val targetBand = (bandIndex * numBands / 10).coerceIn(0, numBands - 1).toShort()
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                // 1 dB = 100 millibels
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
}
