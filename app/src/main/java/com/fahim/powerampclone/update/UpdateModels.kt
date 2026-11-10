package com.fahim.powerampclone.update

import java.io.File

/**
 * Representation of a semantic version (SemVer 2.0.0 compatible).
 * Handles major.minor.patch[-prerelease].
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: String? = null,
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)

        // When version numbers are identical:
        // A release version (no prerelease) has higher precedence than a pre-release version.
        // e.g. 0.1.0 > 0.1.0-beta.1
        val pre1 = prerelease
        val pre2 = other.prerelease
        if (pre1 == null && pre2 == null) return 0
        if (pre1 == null) return 1
        if (pre2 == null) return -1

        // Both have prereleases: compare dot-separated identifiers
        val p1 = pre1.split(".")
        val p2 = pre2.split(".")
        val maxLen = maxOf(p1.size, p2.size)

        for (i in 0 until maxLen) {
            val token1 = p1.getOrNull(i)
            val token2 = p2.getOrNull(i)

            if (token1 == null) return -1
            if (token2 == null) return 1

            val num1 = token1.toIntOrNull()
            val num2 = token2.toIntOrNull()

            val comp = when {
                num1 != null && num2 != null -> num1.compareTo(num2)
                num1 != null && num2 == null -> -1 // numeric identifiers have lower precedence than text
                num1 == null && num2 != null -> 1
                else -> token1.compareTo(token2)
            }
            if (comp != 0) return comp
        }

        return 0
    }

    override fun toString(): String {
        return buildString {
            append("$major.$minor.$patch")
            if (!prerelease.isNullOrEmpty()) {
                append("-$prerelease")
            }
        }
    }

    companion object {
        private val SEMVER_REGEX = Regex(
            """^[vV]?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+([0-9A-Za-z.-]+))?$"""
        )

        /**
         * Parses a version string into a [SemanticVersion], returning null if malformed.
         */
        fun parse(version: String): SemanticVersion? {
            val trimmed = version.trim()
            val match = SEMVER_REGEX.matchEntire(trimmed) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            val patch = match.groupValues[3].toIntOrNull() ?: return null
            val prerelease = match.groupValues[4].takeIf { it.isNotEmpty() }
            return SemanticVersion(major, minor, patch, prerelease)
        }
    }
}

/**
 * Distribution update channels supported by MIORA.
 */
enum class UpdateChannel(val displayName: String, val description: String) {
    STABLE("Stable", "Only verified stable releases"),
    BETA("Beta (Pre-release)", "Includes latest features and public betas")
}

/**
 * Asset attached to a GitHub release.
 */
data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val contentType: String = "application/vnd.android.package-archive"
)

/**
 * Complete metadata for a published GitHub release.
 */
data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val isPrerelease: Boolean,
    val changelog: String,
    val publishedAt: String,
    val apkAsset: ReleaseAsset,
    val sha256Asset: ReleaseAsset? = null,
    val expectedSha256: String? = null
)

/**
 * Observable UI and operational state of the in-app update subsystem.
 */
sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class UpdateAvailable(val release: ReleaseInfo, val channel: UpdateChannel) : UpdateState()
    data class UpToDate(val currentVersion: String, val checkedAt: Long) : UpdateState()
    data class Downloading(
        val release: ReleaseInfo,
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateState()
    data class ReadyToInstall(
        val release: ReleaseInfo,
        val apkFile: File,
        val checksumVerified: Boolean
    ) : UpdateState()
    data class Error(val message: String, val canRetry: Boolean = true) : UpdateState()
}
