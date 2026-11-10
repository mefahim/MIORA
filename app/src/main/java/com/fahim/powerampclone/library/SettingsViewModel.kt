package com.fahim.powerampclone.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.powerampclone.data.SettingsStore
import com.fahim.powerampclone.design.AccentChoice
import com.fahim.powerampclone.design.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accent: AccentChoice = AccentChoice.DYNAMIC,
    val autoAddPlaylists: Boolean = true,
    val crossfade: String = "Off",
    val playbackMode: String = "Normal",
)

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        store.themeMode, store.dynamicColor, store.accent,
        store.autoAddPlaylists, store.crossfade, store.playbackMode,
    ) { theme, dynamic, accent, autoAdd, crossfade, mode ->
        SettingsUiState(theme, dynamic, accent, autoAdd, crossfade, mode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { store.setDynamicColor(enabled) }
    fun setAccent(choice: AccentChoice) = viewModelScope.launch { store.setAccent(choice) }
    fun setAutoAddPlaylists(enabled: Boolean) = viewModelScope.launch { store.setAutoAddPlaylists(enabled) }
    fun setCrossfade(value: String) = viewModelScope.launch { store.setCrossfade(value) }
    fun setPlaybackMode(value: String) = viewModelScope.launch { store.setPlaybackMode(value) }
}
