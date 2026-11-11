package com.fahim.powerampclone.data.lyrics

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage interface for user-configured lyrics synchronization offsets (in milliseconds).
 */
interface LyricOffsetStore {
    /** Returns the offset in milliseconds for the given track (positive = delayed, negative = advanced). Default 0L. */
    fun getOffset(trackId: Long): Long

    /** Persists the offset in milliseconds for the given track. */
    fun setOffset(trackId: Long, offsetMs: Long)

    /** Clears the offset for the given track. */
    fun clearOffset(trackId: Long)
}

/**
 * Production SharedPreferences-backed storage for lyric offsets.
 */
class SharedPreferencesLyricOffsetStore(context: Context) : LyricOffsetStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "miora_lyric_offsets"
        private const val KEY_PREFIX = "offset_"
    }

    override fun getOffset(trackId: Long): Long {
        return try {
            prefs.getLong(KEY_PREFIX + trackId, 0L)
        } catch (_: Exception) {
            0L
        }
    }

    override fun setOffset(trackId: Long, offsetMs: Long) {
        try {
            prefs.edit().putLong(KEY_PREFIX + trackId, offsetMs).apply()
        } catch (_: Exception) {}
    }

    override fun clearOffset(trackId: Long) {
        try {
            prefs.edit().remove(KEY_PREFIX + trackId).apply()
        } catch (_: Exception) {}
    }
}

/**
 * Fast in-memory implementation for unit testing and headless environments.
 */
class InMemoryLyricOffsetStore : LyricOffsetStore {
    private val offsets = ConcurrentHashMap<Long, Long>()

    override fun getOffset(trackId: Long): Long = offsets[trackId] ?: 0L

    override fun setOffset(trackId: Long, offsetMs: Long) {
        if (offsetMs == 0L) {
            offsets.remove(trackId)
        } else {
            offsets[trackId] = offsetMs
        }
    }

    override fun clearOffset(trackId: Long) {
        offsets.remove(trackId)
    }
}
