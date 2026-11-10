package com.fahim.powerampclone

import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.model.RepeatMode
import com.fahim.powerampclone.player.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `initial queue contains all sample tracks`() {
        val vm = PlayerViewModel(MockMusicRepository())
        assertEquals(20, vm.state.value.queue.size)
        assertEquals("Sky Dreams", vm.state.value.currentTrack?.title)
        assertFalse(vm.state.value.isPlaying)
    }

    @Test fun `playTrack selects track and starts playing`() {
        val vm = PlayerViewModel(MockMusicRepository())
        val track = MockMusicRepository().tracks()[4]
        vm.playTrack(track)
        assertEquals("Feel the Beat", vm.state.value.currentTrack?.title)
        assertTrue(vm.state.value.isPlaying)
        assertEquals(0L, vm.state.value.positionMs)
    }

    @Test fun `togglePlayPause flips state`() {
        val vm = PlayerViewModel(MockMusicRepository())
        vm.togglePlayPause()
        assertTrue(vm.state.value.isPlaying)
        vm.togglePlayPause()
        assertFalse(vm.state.value.isPlaying)
    }

    @Test fun `next and previous move through queue`() {
        val vm = PlayerViewModel(MockMusicRepository())
        vm.next()
        assertEquals("Better Tomorrow", vm.state.value.currentTrack?.title)
        vm.previous()
        assertEquals("Sky Dreams", vm.state.value.currentTrack?.title)
    }

    @Test fun `seek clamps to track duration`() {
        val vm = PlayerViewModel(MockMusicRepository())
        vm.seekTo(999_999_999L)
        assertEquals(232_000L, vm.state.value.positionMs)
        vm.seekTo(-50L)
        assertEquals(0L, vm.state.value.positionMs)
    }

    @Test fun `repeat cycles OFF ALL ONE OFF`() {
        val vm = PlayerViewModel(MockMusicRepository())
        assertEquals(RepeatMode.OFF, vm.state.value.repeat)
        vm.cycleRepeat(); assertEquals(RepeatMode.ALL, vm.state.value.repeat)
        vm.cycleRepeat(); assertEquals(RepeatMode.ONE, vm.state.value.repeat)
        vm.cycleRepeat(); assertEquals(RepeatMode.OFF, vm.state.value.repeat)
    }

    @Test fun `favourite toggles`() {
        val vm = PlayerViewModel(MockMusicRepository())
        assertFalse(2L in vm.state.value.favorites)
        vm.toggleFavorite(2L)
        assertTrue(2L in vm.state.value.favorites)
        vm.toggleFavorite(2L)
        assertFalse(2L in vm.state.value.favorites)
    }

    @Test fun `queue remove and clear keep current track`() {
        val vm = PlayerViewModel(MockMusicRepository())
        val size = vm.state.value.queue.size
        vm.removeQueueItem(5)
        assertEquals(size - 1, vm.state.value.queue.size)
        vm.clearQueue()
        assertEquals(1, vm.state.value.queue.size)
        assertEquals("Sky Dreams", vm.state.value.currentTrack?.title)
    }

    @Test fun `moveQueueItem reorders without losing current track`() {
        val vm = PlayerViewModel(MockMusicRepository())
        vm.moveQueueItem(1, 3)
        assertEquals("Sky Dreams", vm.state.value.currentTrack?.title)
        assertEquals("Better Tomorrow", vm.state.value.queue[3].title)
    }
}
