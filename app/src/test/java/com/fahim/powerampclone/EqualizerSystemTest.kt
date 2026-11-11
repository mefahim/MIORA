package com.fahim.powerampclone

import com.fahim.powerampclone.playback.EqualizerController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EqualizerSystemTest {

    private lateinit var controller: EqualizerController

    @Before
    fun setUp() {
        controller = EqualizerController()
    }

    @Test
    fun initialState_hasFlatPresetAndZeroGains() {
        val state = controller.state.value
        assertEquals("Flat", state.preset)
        assertTrue(state.enabled)
        assertEquals(10, state.bandGains.size)
        assertEquals(10, state.frequencies.size)
        assertTrue(state.bandGains.all { it == 0f })
    }

    @Test
    fun setPreset_appliesKnownPresetGains() {
        controller.setPreset("Rock")
        val rockState = controller.state.value
        assertEquals("Rock", rockState.preset)
        assertEquals(listOf(4f, 3f, 1f, 0f, -1f, 0f, 1f, 3f, 4f, 5f), rockState.bandGains)

        controller.setPreset("Classical")
        val classicalState = controller.state.value
        assertEquals("Classical", classicalState.preset)
        assertEquals(listOf(4f, 3f, 2f, 0f, -1f, -1f, 0f, 2f, 3f, 4f), classicalState.bandGains)
    }

    @Test
    fun setBandGain_updatesGainAndSwitchesToCustom() {
        controller.setPreset("Flat")
        controller.setBandGain(0, 5.5f)

        val state = controller.state.value
        assertEquals("Custom", state.preset)
        assertEquals(5.5f, state.bandGains[0], 0.01f)
        assertEquals(0f, state.bandGains[1], 0.01f)
    }

    @Test
    fun setBandGain_clampsToMinAndMaxBoundaries() {
        controller.setBandGain(0, 25f)
        assertEquals(EqualizerController.MAX_GAIN_DB, controller.state.value.bandGains[0], 0.01f)

        controller.setBandGain(1, -30f)
        assertEquals(EqualizerController.MIN_GAIN_DB, controller.state.value.bandGains[1], 0.01f)
    }

    @Test
    fun reset_restoresFlatPreset() {
        controller.setPreset("Jazz")
        controller.setBandGain(3, 4.0f)
        assertEquals("Custom", controller.state.value.preset)

        controller.reset()
        val resetState = controller.state.value
        assertEquals("Flat", resetState.preset)
        assertTrue(resetState.bandGains.all { it == 0f })
    }

    @Test
    fun setEnabled_togglesEqualizerState() {
        assertTrue(controller.state.value.enabled)
        controller.setEnabled(false)
        assertFalse(controller.state.value.enabled)
        controller.setEnabled(true)
        assertTrue(controller.state.value.enabled)
    }

    @Test
    fun attachAudioSession_gracefullyHandlesSimulationEnvironment() {
        // Calling with session 0 or invalid session in JVM headless test environment
        controller.attachAudioSession(0)
        assertFalse(controller.state.value.isHardwareAttached)

        // Detach should be safe to call repeatedly
        controller.detach()
        assertFalse(controller.state.value.isHardwareAttached)
    }
}
