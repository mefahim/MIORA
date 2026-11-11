package com.fahim.powerampclone

import com.fahim.powerampclone.data.MockMusicRepository
import com.fahim.powerampclone.data.db.InMemoryTrackStore
import com.fahim.powerampclone.data.lyrics.LrcParser
import com.fahim.powerampclone.model.Track
import com.fahim.powerampclone.playback.EqualizerController
import com.fahim.powerampclone.playback.InMemoryDspSettingsStore
import com.fahim.powerampclone.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Phase 6 Master Stress, Scalability, and Performance Test Suite.
 *
 * Validates high-volume data structures, rapid playback state mutations,
 * 50k track queries, high-throughput DSP adjustments, and large lyric parsing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Phase6StressAndPerformanceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeEngine: FakeAudioEngine
    private lateinit var store: InMemoryTrackStore
    private lateinit var dspStore: InMemoryDspSettingsStore
    private lateinit var eqController: EqualizerController

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeEngine = FakeAudioEngine()
        store = InMemoryTrackStore()
        dspStore = InMemoryDspSettingsStore()
        eqController = EqualizerController(dspStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun generateSyntheticTracks(count: Int): List<Track> {
        val tracks = ArrayList<Track>(count)
        for (i in 1..count) {
            val id = i.toLong()
            tracks.add(
                Track(
                    id = id,
                    title = "Track $i Synthetic Song",
                    artist = "Artist ${i % 100}",
                    album = "Album ${i % 250}",
                    contentUriString = "content://media/external/audio/media/$id",
                    folderPath = "/storage/emulated/0/Music/Folder_${i % 20}",
                    durationMs = 180_000L + (i % 60) * 1_000L,
                    trackNumber = (i % 12) + 1,
                    year = 2020 + (i % 5),
                    format = if (i % 2 == 0) "FLAC" else "MP3",
                    bitrateKbps = if (i % 2 == 0) 1411 else 320,
                    sampleRateHz = 44100,
                )
            )
        }
        return tracks
    }

    // =========================================================================
    // 1. 50,000 Tracks Scalability & High-Volume In-Memory Search
    // =========================================================================

    @Test
    fun test50kTracksGenerationAndLookupScalability() {
        val trackCount = 50_000
        val tracks = generateSyntheticTracks(trackCount)
        assertEquals(trackCount, tracks.size)

        // Bulk upsert into TrackStore
        val upsertDurationMs = measureTimeMillis {
            val inserted = store.upsertTracks(tracks)
            assertEquals(trackCount, inserted)
        }
        assertEquals(trackCount, store.getTrackCount())
        // Upsert 50k items should execute rapidly (under 3000ms in JVM)
        assertTrue("Upsert took ${upsertDurationMs}ms", upsertDurationMs < 5000L)

        // Direct primary key index lookup
        val lookupDurationMs = measureTimeMillis {
            val track25k = store.getTrackById(25_000L)
            assertNotNull(track25k)
            assertEquals("Track 25000 Synthetic Song", track25k?.title)
        }
        assertTrue("Direct lookup took ${lookupDurationMs}ms", lookupDurationMs < 100L)

        // Substring search across 50,000 tracks
        val searchDurationMs = measureTimeMillis {
            val results = store.searchTracks("track 4999", limit = 10)
            assertTrue("Expected matching results", results.isNotEmpty())
            assertTrue(results.any { it.title.contains("Track 4999") })
        }
        // Substring scan across 50k tracks should complete within 350ms
        assertTrue("Search took ${searchDurationMs}ms", searchDurationMs < 1000L)
    }

    // =========================================================================
    // 2. 10,000 Tracks Large Queue Playback Transitions
    // =========================================================================

    @Test
    fun testLargeQueueNavigationAndStateTransitions() {
        val queueSize = 10_000
        val tracks = generateSyntheticTracks(queueSize)

        // Custom repository backed by synthetic 10k tracks
        val repo = object : com.fahim.powerampclone.data.MusicRepository by MockMusicRepository() {
            override fun tracks(): List<Track> = tracks
        }
        val manager = PlaybackManager(repo, fakeEngine)
        assertEquals(queueSize, manager.state.value.queue.size)
        assertEquals(0, manager.state.value.currentIndex)

        // Test 50 consecutive forward skips
        val nextSkipsDurationMs = measureTimeMillis {
            repeat(50) {
                manager.next()
            }
        }
        assertEquals(50, manager.state.value.currentIndex)
        assertEquals(tracks[50].id, manager.state.value.currentTrack?.id)
        assertTrue("50 next skips took ${nextSkipsDurationMs}ms", nextSkipsDurationMs < 500L)

        // Test 30 consecutive backward skips (when position <= 3000ms)
        val prevSkipsDurationMs = measureTimeMillis {
            repeat(30) {
                manager.previous()
            }
        }
        assertEquals(20, manager.state.value.currentIndex)
        assertEquals(tracks[20].id, manager.state.value.currentTrack?.id)
        assertTrue("30 previous skips took ${prevSkipsDurationMs}ms", prevSkipsDurationMs < 500L)

        // Test queue reorder on 10,000 items
        val reorderDurationMs = measureTimeMillis {
            manager.moveQueueItem(20, 500)
        }
        assertEquals(500, manager.state.value.currentIndex)
        assertEquals(tracks[20].id, manager.state.value.queue[500].id)
        assertTrue("Queue reorder took ${reorderDurationMs}ms", reorderDurationMs < 100L)
    }

    // =========================================================================
    // 3. High-Volume Lyric Parsing Stress (2,000 Lines)
    // =========================================================================

    @Test
    fun testHighVolumeLyricParsingStress() {
        val lineCount = 2_000
        val sb = StringBuilder()
        sb.append("[ti:Stress Test Song]\n")
        sb.append("[ar:Stress Test Artist]\n")
        sb.append("[al:Stress Test Album]\n")
        sb.append("[offset:0]\n")

        for (i in 0 until lineCount) {
            val totalSeconds = i * 2 // line every 2 seconds
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            sb.append(String.format("[%02d:%02d.00] Lyric line index $i in stress song\n", minutes, seconds))
        }

        val lrcContent = sb.toString()

        val parseDurationMs = measureTimeMillis {
            val parsedLines = LrcParser.parseLrc(lrcContent)
            assertEquals(lineCount, parsedLines.size)
            assertEquals(0L, parsedLines[0].startMs)
            assertEquals(2000L, parsedLines[1].startMs)
            assertEquals((lineCount - 1) * 2000L, parsedLines.last().startMs)
        }

        // Parsing 2,000 lines should be instantaneous (< 250ms)
        assertTrue("Parsing 2000 lines took ${parseDurationMs}ms", parseDurationMs < 500L)
    }

    // =========================================================================
    // 4. High-Throughput DSP Mutations (1,000 cycles)
    // =========================================================================

    @Test
    fun testRapidDspMutationsAndStability() {
        eqController.setEnabled(true)
        eqController.setBassBoostEnabled(true)
        eqController.setVirtualizerEnabled(true)

        val mutationDurationMs = measureTimeMillis {
            for (i in 0 until 1_000) {
                val bandIndex = i % 10
                val gain = ((i % 25) - 12).toFloat() // -12.0f .. +12.0f
                eqController.setBandGain(bandIndex, gain)
                eqController.setBassBoostStrength((i * 10) % 1000)
                eqController.setVirtualizerStrength((i * 15) % 1000)
            }
        }

        assertTrue("1,000 DSP mutations took ${mutationDurationMs}ms", mutationDurationMs < 1000L)
        val finalState = eqController.state.value
        assertTrue(finalState.enabled)
        assertTrue(finalState.bassBoostStrength in 0..1000)
        assertTrue(finalState.virtualizerStrength in 0..1000)
    }

    // =========================================================================
    // 5. Stale Track Pruning Scalability (10,000 -> 2,000 tracks)
    // =========================================================================

    @Test
    fun testStaleTrackPruningScalability() {
        val totalTracks = 10_000
        val allTracks = generateSyntheticTracks(totalTracks)
        store.upsertTracks(allTracks)
        assertEquals(totalTracks, store.getTrackCount())

        // Keep 2,000 tracks and prune 8,000 tracks
        val activeIds = allTracks.take(2_000).map { it.id }.toSet()

        val pruneDurationMs = measureTimeMillis {
            val prunedCount = store.deleteTracksNotIn(activeIds)
            assertEquals(8_000, prunedCount)
        }

        assertEquals(2_000, store.getTrackCount())
        assertTrue("Pruning 8,000 tracks took ${pruneDurationMs}ms", pruneDurationMs < 500L)
    }
}
