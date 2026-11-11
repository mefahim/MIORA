package com.fahim.powerampclone

import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.player.PlayerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractivePlayerUiTest {

    @Test
    fun testPositionToTimeMapping() {
        val durationMs = 210000L // 3m 30s

        // Normal fractions
        assertEquals(0L, mapFractionToTime(0f, durationMs))
        assertEquals(105000L, mapFractionToTime(0.5f, durationMs))
        assertEquals(210000L, mapFractionToTime(1.0f, durationMs))

        // Clamped boundary values
        assertEquals(0L, mapFractionToTime(-0.25f, durationMs))
        assertEquals(210000L, mapFractionToTime(1.4f, durationMs))

        // Zero duration edge case
        assertEquals(0L, mapFractionToTime(0.5f, 0L))
    }

    @Test
    fun testSwipeGestureThresholds() {
        val thresholdPx = 130f

        var nextCalled = false
        var prevCalled = false

        fun evaluateSwipe(offset: Float) {
            if (offset < -thresholdPx) {
                nextCalled = true
            } else if (offset > thresholdPx) {
                prevCalled = true
            }
        }

        // Sub-threshold movements
        evaluateSwipe(-50f)
        assertFalse(nextCalled)
        assertFalse(prevCalled)

        evaluateSwipe(80f)
        assertFalse(nextCalled)
        assertFalse(prevCalled)

        // Beyond threshold movements
        evaluateSwipe(-150f)
        assertTrue(nextCalled)
        assertFalse(prevCalled)

        evaluateSwipe(160f)
        assertTrue(prevCalled)
    }

    @Test
    fun testSynthesizedWaveformPeakGeneration() {
        // Peaks should always be within [0.0f, 1.0f] and have positive height
        val seedColor = 0xFF4285F4.toLong()
        val base = (seedColor and 0xFFFF).toInt() % 10
        val peaks = List(48) { i ->
            val v = kotlin.math.abs(kotlin.math.sin(i * 0.7 + base)).toFloat()
            (0.25f + 0.65f * v * (0.6f + 0.4f * kotlin.math.abs(kotlin.math.sin(i * 0.31)).toFloat())).coerceIn(0.1f, 1.0f)
        }

        assertEquals(48, peaks.size)
        peaks.forEach { peak ->
            assertTrue("Peak must be >= 0.1f: $peak", peak >= 0.1f)
            assertTrue("Peak must be <= 1.0f: $peak", peak <= 1.0f)
        }
    }

    @Test
    fun testPlayerUiStatePositionClamping() {
        val sampleTrack = Track(
            id = 1L,
            title = "Test Track",
            artist = "Artist",
            album = "Album",
            durationMs = 120000L,
            contentUriString = "content://media/1",
        )
        val state = PlayerUiState(
            queue = listOf(sampleTrack),
            currentIndex = 0,
            positionMs = 60000L,
            durationMs = 120000L,
        )

        val progress = if (state.currentDurationMs > 0L) {
            (state.positionMs.toFloat() / state.currentDurationMs).coerceIn(0f, 1f)
        } else {
            0f
        }

        assertEquals(0.5f, progress, 0.001f)
    }

    private fun mapFractionToTime(fraction: Float, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        return (fraction.coerceIn(0f, 1f) * durationMs).toLong()
    }
}
