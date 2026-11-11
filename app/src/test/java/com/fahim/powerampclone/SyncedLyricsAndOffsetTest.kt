package com.fahim.powerampclone

import com.fahim.powerampclone.data.lyrics.InMemoryLyricOffsetStore
import com.fahim.powerampclone.data.lyrics.LrcParser
import com.fahim.powerampclone.model.LyricLine
import com.fahim.powerampclone.model.Track
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SyncedLyricsAndOffsetTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `in memory lyric offset store correctly manages track offsets`() {
        val store = InMemoryLyricOffsetStore()
        assertEquals(0L, store.getOffset(101L))

        store.setOffset(101L, 350L)
        assertEquals(350L, store.getOffset(101L))
        assertEquals(0L, store.getOffset(102L))

        store.setOffset(102L, -200L)
        assertEquals(-200L, store.getOffset(102L))

        store.clearOffset(101L)
        assertEquals(0L, store.getOffset(101L))
    }

    @Test
    fun `player view model adjusts and resets lyric offset per track`() {
        val vm = PlayerViewModel()
        val track1 = Track(id = 1L, title = "Song A", artist = "Artist", album = "Album")
        val track2 = Track(id = 2L, title = "Song B", artist = "Artist", album = "Album")
        vm.playTrack(track1, listOf(track1, track2))

        assertEquals(0L, vm.lyricOffsetMs.value)

        vm.adjustLyricOffset(50L)
        assertEquals(50L, vm.lyricOffsetMs.value)

        vm.adjustLyricOffset(500L)
        assertEquals(550L, vm.lyricOffsetMs.value)

        vm.adjustLyricOffset(-100L)
        assertEquals(450L, vm.lyricOffsetMs.value)

        // Switch to track 2 -> offset should be 0L for track 2
        vm.playTrack(track2)
        assertEquals(0L, vm.lyricOffsetMs.value)

        // Switch back to track 1 -> offset 450L should be restored
        vm.playTrack(track1)
        assertEquals(450L, vm.lyricOffsetMs.value)

        // Reset offset on track 1
        vm.resetLyricOffset()
        assertEquals(0L, vm.lyricOffsetMs.value)
    }

    @Test
    fun `lrc parser resolves embedded lyrics over external search`() {
        val embeddedLines = listOf(LyricLine(1000L, "Hello World"))
        val track = Track(id = 10L, title = "Test", artist = "Artist", album = "Album", lyrics = embeddedLines)

        val resolved = LrcParser.resolveLyricsForTrack(null, track)
        assertEquals(1, resolved.size)
        assertEquals("Hello World", resolved[0].text)
        assertEquals(1000L, resolved[0].startMs)
    }

    @Test
    fun `lrc parser resolves companion lrc file from disk when audio file exists`() {
        val tempDir = File.createTempFile("miora_test_", "").apply {
            delete()
            mkdirs()
        }
        try {
            val audioFile = File(tempDir, "Echoes.mp3").apply { writeBytes(ByteArray(10)) }
            val lrcFile = File(tempDir, "Echoes.lrc").apply {
                writeText(
                    """
                    [00:01.00]First line of echoes
                    [00:04.50]Second line fades in
                    """.trimIndent()
                )
            }

            val track = Track(
                id = 99L,
                title = "Echoes",
                artist = "Pink Floyd",
                album = "Meddle",
                contentUriString = "file://${audioFile.absolutePath}",
                folderPath = tempDir.absolutePath,
            )

            val resolved = LrcParser.resolveLyricsForTrack(null, track)
            assertEquals(2, resolved.size)
            assertEquals("First line of echoes", resolved[0].text)
            assertEquals(1000L, resolved[0].startMs)
            assertEquals("Second line fades in", resolved[1].text)
            assertEquals(4500L, resolved[1].startMs)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `lyrics line synchronization accounts for positive and negative offsets`() {
        val lyrics = listOf(
            LyricLine(2000L, "Line 1"),
            LyricLine(5000L, "Line 2"),
            LyricLine(8000L, "Line 3"),
        )

        // Without offset at position 4000ms:
        val pos = 4000L
        val activeWithoutOffset = lyrics.indexOfLast { pos >= it.startMs }
        assertEquals(0, activeWithoutOffset) // Line 1 is active

        // With +1500ms offset (effective position = 5500ms):
        val posWithLead = pos + 1500L
        val activeWithLead = lyrics.indexOfLast { posWithLead >= it.startMs }
        assertEquals(1, activeWithLead) // Line 2 is now active

        // With -2500ms offset (effective position = 1500ms):
        val posWithLag = pos - 2500L
        val activeWithLag = lyrics.indexOfLast { posWithLag >= it.startMs }
        assertEquals(-1, activeWithLag) // Before Line 1
    }

    @Test
    fun `missing and malformed companion lrc files are handled safely`() {
        val missingTrack = Track(
            id = 55L,
            title = "Ghost",
            artist = "Nobody",
            album = "Nowhere",
            contentUriString = "file:///non/existent/path/song.mp3",
            folderPath = "/non/existent/path",
        )
        val resolved = LrcParser.resolveLyricsForTrack(null, missingTrack)
        assertTrue(resolved.isEmpty())

        // Malformed content
        val malformed = LrcParser.parseLrc("Random text without any timestamp brackets\n[not_a_time] Just words")
        assertTrue(malformed.isEmpty())
    }
}
