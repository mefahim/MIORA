package com.fahim.powerampclone.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Interface for fetching GitHub release metadata.
 */
interface ReleaseClient {
    suspend fun fetchReleases(): Result<List<ReleaseInfo>>
}

/**
 * Standard implementation querying GitHub REST API.
 */
class HttpGitHubReleaseClient(
    private val owner: String = "mefahim",
    private val repo: String = "MIORA",
    private val appVersion: String = "0.1.0"
) : ReleaseClient {

    override suspend fun fetchReleases(): Result<List<ReleaseInfo>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "MIORA-Android/$appVersion")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                connection.disconnect()

                val releases = ReleaseParser.parseReleases(response)
                Result.success(releases)
            } else if (responseCode == 403 || responseCode == 429) {
                connection.disconnect()
                Result.failure(Exception("GitHub API rate limit exceeded. Please try again later."))
            } else {
                connection.disconnect()
                Result.failure(Exception("GitHub API returned HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Pure business logic for checking, version comparison, and channel filtering.
 */
object UpdateDecisionEngine {

    /**
     * Determines the latest eligible update based on current version, channel, and release list.
     * Prevents downgrades and duplicate updates.
     */
    fun findEligibleUpdate(
        releases: List<ReleaseInfo>,
        currentVersionStr: String,
        channel: UpdateChannel
    ): ReleaseInfo? {
        val currentVersion = SemanticVersion.parse(currentVersionStr) ?: return null

        val eligibleReleases = releases.filter { release ->
            when (channel) {
                UpdateChannel.STABLE -> !release.isPrerelease
                UpdateChannel.BETA -> true // beta channel receives both beta and stable
            }
        }

        val candidatesWithVersion = eligibleReleases.mapNotNull { release ->
            val version = SemanticVersion.parse(release.tagName) ?: return@mapNotNull null
            if (version > currentVersion) {
                version to release
            } else {
                null
            }
        }

        // Return the highest version among candidates
        return candidatesWithVersion
            .maxByOrNull { it.first }
            ?.second
    }

    /**
     * Verifies whether targetVersion is strictly newer than currentVersion.
     * Rejects downgrades or same-version updates.
     */
    fun isStrictlyNewer(currentVersionStr: String, targetVersionStr: String): Boolean {
        val current = SemanticVersion.parse(currentVersionStr) ?: return false
        val target = SemanticVersion.parse(targetVersionStr) ?: return false
        return target > current
    }
}
