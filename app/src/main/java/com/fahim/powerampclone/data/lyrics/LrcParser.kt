package com.fahim.powerampclone.data.lyrics

import com.fahim.powerampclone.model.LyricLine
import java.io.File
import java.util.Locale
import java.util.regex.Pattern

/**
 * Robust, high-performance Synchronized LRC Lyrics Parser.
 *
 * Supports:
 * - Standard [mm:ss.xx] (hundredths of a second)
 * - Extended [mm:ss.xxx] (milliseconds)
 * - Basic [mm:ss]
 * - Multi-timestamp lines (e.g. [00:12.00][00:45.00] Chorus text)
 * - Playback offset tags ([offset:+500] or [offset:-300])
 * - Serialization between List<LyricLine> and LRC string
 * - Sibling .lrc file discovery on storage
 */
object LrcParser {

    private val TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val OFFSET_PATTERN = Pattern.compile("\\[offset:\\s*([+-]?\\d+)]", Pattern.CASE_INSENSITIVE)

    /**
     * Parses an LRC-formatted string into a chronologically ordered list of [LyricLine]s.
     */
    fun parseLrc(content: String?): List<LyricLine> {
        if (content.isNullOrBlank()) return emptyList()

        var globalOffsetMs = 0L
        val lines = content.lineSequence()
        val parsedEntries = mutableListOf<LyricLine>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            // 1. Check for offset tag
            val offsetMatcher = OFFSET_PATTERN.matcher(line)
            if (offsetMatcher.find()) {
                globalOffsetMs = offsetMatcher.group(1)?.toLongOrNull() ?: 0L
                continue
            }

            // 2. Extract all timestamps on this line
            val tsMatcher = TIMESTAMP_PATTERN.matcher(line)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (tsMatcher.find()) {
                val minutes = tsMatcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = tsMatcher.group(2)?.toLongOrNull() ?: 0L
                val fractionalStr = tsMatcher.group(3)

                val msFraction = when {
                    fractionalStr == null -> 0L
                    fractionalStr.length == 1 -> (fractionalStr.toLongOrNull() ?: 0L) * 100L
                    fractionalStr.length == 2 -> (fractionalStr.toLongOrNull() ?: 0L) * 10L
                    else -> fractionalStr.take(3).toLongOrNull() ?: 0L
                }

                val totalMs = (minutes * 60_000L) + (seconds * 1_000L) + msFraction
                timestamps.add(totalMs)
                lastMatchEnd = tsMatcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val lyricText = line.substring(lastMatchEnd).trim()
                for (ts in timestamps) {
                    val adjusted = (ts + globalOffsetMs).coerceAtLeast(0L)
                    parsedEntries.add(LyricLine(adjusted, lyricText))
                }
            }
        }

        return parsedEntries.sortedBy { it.startMs }
    }

    /**
     * Serializes a list of [LyricLine]s into standard LRC text format.
     */
    fun formatLrc(lyrics: List<LyricLine>): String {
        if (lyrics.isEmpty()) return ""
        val sb = StringBuilder()
        for (line in lyrics) {
            val totalSeconds = line.startMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val centiseconds = (line.startMs % 1000) / 10
            sb.append(String.format(Locale.US, "[%02d:%02d.%02d]%s\n", minutes, seconds, centiseconds, line.text))
        }
        return sb.toString().trimEnd()
    }

    /**
     * Looks for a companion `.lrc` file beside an audio file path.
     * E.g., for `/music/track.flac`, looks for `/music/track.lrc`.
     */
    fun findSiblingLrcFile(audioFilePath: String?): File? {
        if (audioFilePath.isNullOrBlank()) return null
        return try {
            val audioFile = File(audioFilePath)
            if (!audioFile.exists()) return null
            val baseName = audioFile.nameWithoutExtension
            val parent = audioFile.parentFile ?: return null
            val lrcFile = File(parent, "$baseName.lrc")
            if (lrcFile.exists() && lrcFile.isFile && lrcFile.canRead()) {
                lrcFile
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
