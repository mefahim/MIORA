package com.fahim.powerampclone.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Validates downloaded APK packages and launches the native Android package installer.
 */
object UpdateInstaller {

    sealed class PreflightResult {
        data object Ready : PreflightResult()
        data class PermissionRequired(val settingsIntent: Intent) : PreflightResult()
        data class InvalidPackage(val reason: String) : PreflightResult()
    }

    /**
     * Inspects the downloaded APK to verify it is valid and matches the application ID.
     */
    fun preflightCheck(context: Context, apkFile: File): PreflightResult {
        if (!apkFile.exists() || !apkFile.canRead()) {
            return PreflightResult.InvalidPackage("APK file not found or inaccessible at ${apkFile.name}")
        }

        val packageManager = context.packageManager
        val archiveInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            ?: return PreflightResult.InvalidPackage("APK parsing failed; file may be corrupted.")

        val expectedPackage = context.packageName
        if (archiveInfo.packageName != expectedPackage) {
            return PreflightResult.InvalidPackage(
                "Package mismatch: expected '$expectedPackage', found '${archiveInfo.packageName}'."
            )
        }

        // On Android 8.0 (API 26) and above, check for unknown sources permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return PreflightResult.PermissionRequired(intent)
            }
        }

        return PreflightResult.Ready
    }

    /**
     * Builds the system Intent to launch Android's package installer for the given APK.
     */
    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
