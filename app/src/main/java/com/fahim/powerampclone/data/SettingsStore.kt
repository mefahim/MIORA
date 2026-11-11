package com.fahim.powerampclone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.powerampclone.design.AccentChoice
import com.fahim.powerampclone.design.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persisted user preferences (DataStore). Theme/accent changes apply immediately. */
class SettingsStore(private val context: Context) {
    companion object {
        private val THEME = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val ACCENT = stringPreferencesKey("accent")
        private val AUTO_ADD_PLAYLISTS = booleanPreferencesKey("auto_add_playlists")
        private val CROSSFADE = stringPreferencesKey("crossfade")
        private val PLAYBACK_MODE = stringPreferencesKey("playback_mode")
        private val COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[THEME] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM)
    }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLOR] ?: true }
    val accent: Flow<AccentChoice> = context.dataStore.data.map {
        runCatching { AccentChoice.valueOf(it[ACCENT] ?: AccentChoice.DYNAMIC.name) }.getOrDefault(AccentChoice.DYNAMIC)
    }
    val autoAddPlaylists: Flow<Boolean> = context.dataStore.data.map { it[AUTO_ADD_PLAYLISTS] ?: true }
    val crossfade: Flow<String> = context.dataStore.data.map { it[CROSSFADE] ?: "Off" }
    val playbackMode: Flow<String> = context.dataStore.data.map { it[PLAYBACK_MODE] ?: "Normal" }
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { it[COMPLETED_ONBOARDING] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    suspend fun setAccent(choice: AccentChoice) = context.dataStore.edit { it[ACCENT] = choice.name }
    suspend fun setAutoAddPlaylists(enabled: Boolean) = context.dataStore.edit { it[AUTO_ADD_PLAYLISTS] = enabled }
    suspend fun setCrossfade(value: String) = context.dataStore.edit { it[CROSSFADE] = value }
    suspend fun setPlaybackMode(value: String) = context.dataStore.edit { it[PLAYBACK_MODE] = value }
    suspend fun setCompletedOnboarding(completed: Boolean) = context.dataStore.edit { it[COMPLETED_ONBOARDING] = completed }
}
