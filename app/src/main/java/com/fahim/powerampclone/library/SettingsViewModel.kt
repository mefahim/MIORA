package com.fahim.powerampclone.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.powerampclone.data.SettingsStore
import com.fahim.powerampclone.design.AccentChoice
import com.fahim.powerampclone.design.ThemeMode
import com.fahim.powerampclone.update.ReleaseInfo
import com.fahim.powerampclone.update.UpdateChannel
import com.fahim.powerampclone.update.UpdateInstaller
import com.fahim.powerampclone.update.UpdateManager
import com.fahim.powerampclone.update.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accent: AccentChoice = AccentChoice.DYNAMIC,
    val autoAddPlaylists: Boolean = true,
    val crossfade: String = "Off",
    val playbackMode: String = "Normal",
)

class SettingsViewModel(
    private val store: SettingsStore,
    val updateManager: UpdateManager? = null
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        store.themeMode, store.dynamicColor, store.accent,
        store.autoAddPlaylists, store.crossfade, store.playbackMode,
    ) { args: Array<Any?> ->
        SettingsUiState(
            themeMode = args[0] as ThemeMode,
            dynamicColor = args[1] as Boolean,
            accent = args[2] as AccentChoice,
            autoAddPlaylists = args[3] as Boolean,
            crossfade = args[4] as String,
            playbackMode = args[5] as String,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    val updateState: StateFlow<UpdateState> = updateManager?.state ?: MutableStateFlow(UpdateState.Idle)
    val updateChannel: StateFlow<UpdateChannel> = updateManager?.selectedChannel ?: MutableStateFlow(UpdateChannel.BETA)
    val lastCheckedTimestamp: StateFlow<Long> = updateManager?.lastCheckedTimestamp ?: MutableStateFlow(0L)

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { store.setDynamicColor(enabled) }
    fun setAccent(choice: AccentChoice) = viewModelScope.launch { store.setAccent(choice) }
    fun setAutoAddPlaylists(enabled: Boolean) = viewModelScope.launch { store.setAutoAddPlaylists(enabled) }
    fun setCrossfade(value: String) = viewModelScope.launch { store.setCrossfade(value) }
    fun setPlaybackMode(value: String) = viewModelScope.launch { store.setPlaybackMode(value) }

    fun setUpdateChannel(channel: UpdateChannel) {
        updateManager?.setChannel(channel)
    }

    fun checkForUpdates() {
        updateManager?.checkForUpdates(silentIfUpToDate = false)
    }

    fun startDownload(release: ReleaseInfo) {
        updateManager?.startDownload(release)
    }

    fun cancelDownload() {
        updateManager?.cancelDownload()
    }

    fun dismissUpdate() {
        updateManager?.dismiss()
    }

    fun installApk(apkFile: File): UpdateInstaller.PreflightResult? {
        return updateManager?.installApk(apkFile)
    }
}
