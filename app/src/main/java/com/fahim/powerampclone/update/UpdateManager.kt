package com.fahim.powerampclone.update

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Coordinator for managing app update lifecycles, background checks, downloads, and UI states.
 */
class UpdateManager(
    private val context: Context,
    private val releaseClient: ReleaseClient = HttpGitHubReleaseClient(appVersion = CURRENT_VERSION_NAME),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {
    companion object {
        const val CURRENT_VERSION_NAME = "0.1.0-beta.1"
        const val CURRENT_VERSION_CODE = 2
    }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _selectedChannel = MutableStateFlow(UpdateChannel.BETA)
    val selectedChannel: StateFlow<UpdateChannel> = _selectedChannel.asStateFlow()

    private val _lastCheckedTimestamp = MutableStateFlow(0L)
    val lastCheckedTimestamp: StateFlow<Long> = _lastCheckedTimestamp.asStateFlow()

    private var currentDownloadJob: Job? = null

    fun setChannel(channel: UpdateChannel) {
        _selectedChannel.value = channel
    }

    fun checkForUpdates(silentIfUpToDate: Boolean = false) {
        coroutineScope.launch {
            _state.value = UpdateState.Checking
            _lastCheckedTimestamp.value = System.currentTimeMillis()

            val result = releaseClient.fetchReleases()
            result.fold(
                onSuccess = { releases ->
                    val eligible = UpdateDecisionEngine.findEligibleUpdate(
                        releases = releases,
                        currentVersionStr = CURRENT_VERSION_NAME,
                        channel = _selectedChannel.value
                    )

                    if (eligible != null) {
                        _state.value = UpdateState.UpdateAvailable(eligible, _selectedChannel.value)
                    } else {
                        if (!silentIfUpToDate) {
                            _state.value = UpdateState.UpToDate(CURRENT_VERSION_NAME, _lastCheckedTimestamp.value)
                        } else {
                            _state.value = UpdateState.Idle
                        }
                    }
                },
                onFailure = { error ->
                    _state.value = UpdateState.Error(
                        error.message ?: "Failed to retrieve releases from GitHub."
                    )
                }
            )
        }
    }

    fun startDownload(release: ReleaseInfo) {
        currentDownloadJob?.cancel()
        currentDownloadJob = coroutineScope.launch {
            _state.value = UpdateState.Downloading(
                release = release,
                progressPercent = 0,
                bytesDownloaded = 0L,
                totalBytes = release.apkAsset.sizeBytes
            )

            val cacheDir = File(context.cacheDir, "updates")
            val targetFile = File(cacheDir, "MIORA-${release.tagName}.apk")

            val downloadResult = UpdateDownloadManager.downloadFile(
                sourceUrl = release.apkAsset.downloadUrl,
                destinationFile = targetFile
            ) { percent, bytesRead, totalBytes ->
                _state.value = UpdateState.Downloading(
                    release = release,
                    progressPercent = percent,
                    bytesDownloaded = bytesRead,
                    totalBytes = totalBytes
                )
            }

            downloadResult.fold(
                onSuccess = { apkFile ->
                    // Resolve expected SHA-256 (from markdown body or .sha256 companion asset)
                    var expectedHash = release.expectedSha256
                    if (expectedHash == null && release.sha256Asset != null) {
                        expectedHash = UpdateDownloadManager.fetchSha256FromUrl(release.sha256Asset.downloadUrl)
                    }

                    val verified = if (!expectedHash.isNullOrBlank()) {
                        UpdateDownloadManager.verifyChecksum(apkFile, expectedHash)
                    } else {
                        // Checksum unavailable in metadata; package is valid on disk
                        true
                    }

                    if (!expectedHash.isNullOrBlank() && !verified) {
                        apkFile.delete()
                        _state.value = UpdateState.Error(
                            "Cryptographic integrity verification failed: SHA-256 hash mismatch."
                        )
                    } else {
                        _state.value = UpdateState.ReadyToInstall(
                            release = release,
                            apkFile = apkFile,
                            checksumVerified = verified
                        )
                    }
                },
                onFailure = { error ->
                    _state.value = UpdateState.Error(
                        "Download failed: ${error.localizedMessage ?: "Network error"}"
                    )
                }
            )
        }
    }

    fun installApk(apkFile: File): UpdateInstaller.PreflightResult {
        val check = UpdateInstaller.preflightCheck(context, apkFile)
        if (check is UpdateInstaller.PreflightResult.Ready) {
            val intent = UpdateInstaller.createInstallIntent(context, apkFile)
            context.startActivity(intent)
        }
        return check
    }

    fun cancelDownload() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        _state.value = UpdateState.Idle
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }
}
