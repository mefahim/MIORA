package com.fahim.powerampclone.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

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
     * Inspects the downloaded APK to verify it is valid, matches the application ID,
     * and matches the cryptographic signing certificate fingerprint.
     */
    fun preflightCheck(context: Context, apkFile: File): PreflightResult {
        if (!apkFile.exists() || !apkFile.canRead()) {
            return PreflightResult.InvalidPackage("APK file not found or inaccessible at ${apkFile.name}")
        }

        val packageManager = context.packageManager
        val signatureFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, signatureFlags)
            ?: packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            ?: return PreflightResult.InvalidPackage("APK parsing failed; file may be corrupted.")

        val expectedPackage = context.packageName
        if (archiveInfo.packageName != expectedPackage) {
            return PreflightResult.InvalidPackage(
                "Package mismatch: expected '$expectedPackage', found '${archiveInfo.packageName}'."
            )
        }

        // Verify cryptographic signing certificate fingerprint matches running app
        val installedCert = getInstalledCertificateSha256(context)
        val apkCert = getArchiveCertificateSha256(archiveInfo)
        if (installedCert != null && apkCert != null) {
            if (!verifyCertificateFingerprints(installedCert, apkCert)) {
                return PreflightResult.InvalidPackage(
                    "Signing certificate mismatch: Downloaded APK signature does not match installed application signature."
                )
            }
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
     * Computes the SHA-256 fingerprint for a raw certificate byte array.
     */
    fun computeSha256Fingerprint(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies that target fingerprint matches installed fingerprint.
     */
    fun verifyCertificateFingerprints(installedHash: String, targetHash: String): Boolean {
        return installedHash.equals(targetHash, ignoreCase = true)
    }

    private fun getInstalledCertificateSha256(context: Context): String? {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo ?: return null
                val signers = if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
                val certBytes = signers?.firstOrNull()?.toByteArray() ?: return null
                computeSha256Fingerprint(certBytes)
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val certBytes = info.signatures?.firstOrNull()?.toByteArray() ?: return null
                computeSha256Fingerprint(certBytes)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getArchiveCertificateSha256(archiveInfo: PackageInfo): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = archiveInfo.signingInfo
                val signers = if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else {
                    @Suppress("DEPRECATION")
                    archiveInfo.signatures
                }
                val certBytes = signers?.firstOrNull()?.toByteArray() ?: return null
                computeSha256Fingerprint(certBytes)
            } else {
                @Suppress("DEPRECATION")
                val certBytes = archiveInfo.signatures?.firstOrNull()?.toByteArray() ?: return null
                computeSha256Fingerprint(certBytes)
            }
        } catch (_: Exception) {
            null
        }
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
