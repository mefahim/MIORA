package com.fahim.powerampclone

import com.fahim.powerampclone.data.lyrics.LrcParser
import com.fahim.powerampclone.model.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parse empty or blank content returns empty list`() {
        assertTrue(LrcParser.parseLrc(null).isEmpty())
        assertTrue(LrcParser.parseLrc("").isEmpty())
        assertTrue(LrcParser.parseLrc("   \n\t  \n").isEmpty())
    }

    @Test
    fun `parse standard two-digit hundredths timestamp`() {
        val lrc = """
            [00:12.50]Hello world
            [01:05.25]Second line of lyrics
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(2, lines.size)
        assertEquals(12_500L, lines[0].startMs)
        assertEquals("Hello world", lines[0].text)
        assertEquals(65_250L, lines[1].startMs)
        assertEquals("Second line of lyrics", lines[1].text)
    }

    @Test
    fun `parse three-digit milliseconds timestamp`() {
        val lrc = """
            [00:05.123]Precise timing
            [02:10.999]Ending note
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(2, lines.size)
        assertEquals(5_123L, lines[0].startMs)
        assertEquals("Precise timing", lines[0].text)
        assertEquals(130_999L, lines[1].startMs)
        assertEquals("Ending note", lines[1].text)
    }

    @Test
    fun `parse multiple timestamps per line produces multiple sorted lines`() {
        val lrc = """
            [00:10.00][00:30.00]Chorus line repeated
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(2, lines.size)
        assertEquals(10_000L, lines[0].startMs)
        assertEquals("Chorus line repeated", lines[0].text)
        assertEquals(30_000L, lines[1].startMs)
        assertEquals("Chorus line repeated", lines[1].text)
    }

    @Test
    fun `parse global playback offset shifts timestamps accurately`() {
        val lrc = """
            [offset:+500]
            [00:01.00]Delayed word
        """.trimIndent()

        val lines = LrcParser.parseLrc(lrc)
        assertEquals(1, lines.size)
        assertEquals(1_500L, lines[0].startMs)
        assertEquals("Delayed word", lines[0].text)
    }

    @Test
    fun `format and parse roundtrip retains lyric sequence`() {
        val original = listOf(
            LyricLine(1_200L, "First line"),
            LyricLine(4_500L, "Second line"),
            LyricLine(12_000L, "Third line"),
        )

        val formatted = LrcParser.formatLrc(original)
        val parsedBack = LrcParser.parseLrc(formatted)

        assertEquals(original.size, parsedBack.size)
        for (i in original.indices) {
            assertEquals(original[i].text, parsedBack[i].text)
            // Millisecond precision to within 10ms due to centisecond formatting
            assertTrue(Math.abs(original[i].startMs - parsedBack[i].startMs) <= 10L)
        }
    }
}
