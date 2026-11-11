package com.fahim.powerampclone

import com.fahim.powerampclone.playback.EqualizerController
import com.fahim.powerampclone.playback.InMemoryDspSettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdvancedDspPipelineTest {

    private lateinit var store: InMemoryDspSettingsStore
    private lateinit var controller: EqualizerController

    @Before
    fun setUp() {
        store = InMemoryDspSettingsStore()
        controller = EqualizerController(store)
    }

    @Test
    fun `bass boost toggles and clamps strength correctly`() {
        assertFalse(controller.state.value.bassBoostEnabled)
        assertEquals(0, controller.state.value.bassBoostStrength)

        controller.setBassBoostEnabled(true)
        assertTrue(controller.state.value.bassBoostEnabled)

        controller.setBassBoostStrength(650)
        assertEquals(650, controller.state.value.bassBoostStrength)

        // Upper clamp boundary
        controller.setBassBoostStrength(1500)
        assertEquals(1000, controller.state.value.bassBoostStrength)

        // Lower clamp boundary
        controller.setBassBoostStrength(-50)
        assertEquals(0, controller.state.value.bassBoostStrength)
    }

    @Test
    fun `virtualizer toggles and clamps strength correctly`() {
        assertFalse(controller.state.value.virtualizerEnabled)
        assertEquals(0, controller.state.value.virtualizerStrength)

        controller.setVirtualizerEnabled(true)
        assertTrue(controller.state.value.virtualizerEnabled)

        controller.setVirtualizerStrength(800)
        assertEquals(800, controller.state.value.virtualizerStrength)

        // Boundary test
        controller.setVirtualizerStrength(2000)
        assertEquals(1000, controller.state.value.virtualizerStrength)
    }

    @Test
    fun `preset reverb cycles known acoustic spaces`() {
        assertEquals("None", controller.state.value.reverbPreset)

        controller.setReverbPreset("Medium Hall")
        assertEquals("Medium Hall", controller.state.value.reverbPreset)

        controller.setReverbPreset("Plate")
        assertEquals("Plate", controller.state.value.reverbPreset)

        controller.setReverbPreset("Small Room")
        assertEquals("Small Room", controller.state.value.reverbPreset)
    }

    @Test
    fun `bass and treble tone controls scale frequency bands within boundaries`() {
        controller.setPreset("Flat")
        controller.setBassTone(6f)
        assertEquals(6f, controller.state.value.bassToneDb, 0.01f)

        // Low bands should be elevated
        val gains = controller.state.value.bandGains
        assertTrue(gains[0] > 0f)
        assertTrue(gains[1] > 0f)
        assertTrue(gains[2] > 0f)

        controller.setTrebleTone(-8f)
        assertEquals(-8f, controller.state.value.trebleToneDb, 0.01f)

        // High bands should be attenuated
        val updatedGains = controller.state.value.bandGains
        assertTrue(updatedGains[7] < 0f)
        assertTrue(updatedGains[8] < 0f)
        assertTrue(updatedGains[9] < 0f)
    }

    @Test
    fun `custom preset creation, selection, and deletion work correctly`() {
        controller.setBandGain(0, 4f)
        controller.setBandGain(1, 3f)
        controller.saveCustomPreset("My Audiophile Curve")

        val state = controller.state.value
        assertEquals("My Audiophile Curve", state.preset)
        assertTrue(state.customPresets.containsKey("My Audiophile Curve"))
        assertEquals(4f, state.customPresets["My Audiophile Curve"]!![0], 0.01f)

        // Switch to Flat
        controller.setPreset("Flat")
        assertEquals("Flat", controller.state.value.preset)

        // Switch back to custom preset
        controller.setPreset("My Audiophile Curve")
        assertEquals("My Audiophile Curve", controller.state.value.preset)
        assertEquals(4f, controller.state.value.bandGains[0], 0.01f)

        // Delete custom preset
        controller.deleteCustomPreset("My Audiophile Curve")
        assertFalse(controller.state.value.customPresets.containsKey("My Audiophile Curve"))
        assertEquals("Flat", controller.state.value.preset)
    }

    @Test
    fun `dsp settings store persists and restores state across controller instances`() {
        controller.setPreset("Rock")
        controller.setBassBoostEnabled(true)
        controller.setBassBoostStrength(700)
        controller.setVirtualizerEnabled(true)
        controller.setVirtualizerStrength(400)
        controller.setReverbPreset("Large Hall")

        // Create new controller with same store
        val newController = EqualizerController(store)
        val restored = newController.state.value

        assertEquals("Rock", restored.preset)
        assertTrue(restored.bassBoostEnabled)
        assertEquals(700, restored.bassBoostStrength)
        assertTrue(restored.virtualizerEnabled)
        assertEquals(400, restored.virtualizerStrength)
        assertEquals("Large Hall", restored.reverbPreset)
    }

    @Test
    fun `effects lifecycle attaches and detaches safely in simulation environment`() {
        // Session 0 or invalid session should not attach
        controller.attachAudioSession(0)
        assertFalse(controller.state.value.isHardwareAttached)

        // Detach and release should be safe to call repeatedly
        controller.detach()
        assertFalse(controller.state.value.isHardwareAttached)

        controller.release()
        assertFalse(controller.state.value.isHardwareAttached)
    }
}
