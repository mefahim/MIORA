package com.fahim.powerampclone

import com.fahim.powerampclone.update.ReleaseAsset
import com.fahim.powerampclone.update.ReleaseInfo
import com.fahim.powerampclone.update.ReleaseParser
import com.fahim.powerampclone.update.SemanticVersion
import com.fahim.powerampclone.update.UpdateChannel
import com.fahim.powerampclone.update.UpdateDecisionEngine
import com.fahim.powerampclone.update.UpdateDownloadManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateSystemTest {

    @Test
    fun semVer_parsesValidVersions() {
        val v1 = SemanticVersion.parse("0.1.0")
        assertNotNull(v1)
        assertEquals(0, v1!!.major)
        assertEquals(1, v1.minor)
        assertEquals(0, v1.patch)
        assertNull(v1.prerelease)

        val v2 = SemanticVersion.parse("v0.1.0-beta.1")
        assertNotNull(v2)
        assertEquals(0, v2!!.major)
        assertEquals(1, v2.minor)
        assertEquals(0, v2.patch)
        assertEquals("beta.1", v2.prerelease)

        val v3 = SemanticVersion.parse("V2.14.3-rc.2+20261110")
        assertNotNull(v3)
        assertEquals(2, v3!!.major)
        assertEquals(14, v3.minor)
        assertEquals(3, v3.patch)
        assertEquals("rc.2", v3.prerelease)
    }

    @Test
    fun semVer_rejectsMalformedStrings() {
        assertNull(SemanticVersion.parse("invalid"))
        assertNull(SemanticVersion.parse(""))
        assertNull(SemanticVersion.parse("1.2"))
        assertNull(SemanticVersion.parse("1.x.3"))
    }

    @Test
    fun semVer_correctPrecedenceOrdering() {
        val vBeta1 = SemanticVersion.parse("0.1.0-beta.1")!!
        val vBeta2 = SemanticVersion.parse("0.1.0-beta.2")!!
        val vStable = SemanticVersion.parse("0.1.0")!!
        val vNextMinor = SemanticVersion.parse("0.2.0-beta.1")!!
        val vNextMajor = SemanticVersion.parse("1.0.0")!!

        assertTrue(vBeta1 < vBeta2)
        assertTrue(vBeta2 < vStable)
        assertTrue(vStable < vNextMinor)
        assertTrue(vNextMinor < vNextMajor)

        // Equal versions
        val vBeta1Clone = SemanticVersion.parse("v0.1.0-beta.1")!!
        assertEquals(0, vBeta1.compareTo(vBeta1Clone))
    }

    @Test
    fun releaseParser_parsesGitHubApiResponse() {
        val json = """
        [
          {
            "tag_name": "v0.1.0-beta.1",
            "name": "MIORA v0.1.0-beta.1 — Initial Public Beta",
            "prerelease": true,
            "body": "First public test build.\nSHA-256: 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            "published_at": "2026-11-10T17:00:00Z",
            "assets": [
              {
                "name": "MIORA-v0.1.0-beta.1.apk",
                "browser_download_url": "https://github.com/mefahim/MIORA/releases/download/v0.1.0-beta.1/MIORA-v0.1.0-beta.1.apk",
                "size": 18147215,
                "content_type": "application/vnd.android.package-archive"
              },
              {
                "name": "MIORA-v0.1.0-beta.1.apk.sha256",
                "browser_download_url": "https://github.com/mefahim/MIORA/releases/download/v0.1.0-beta.1/MIORA-v0.1.0-beta.1.apk.sha256",
                "size": 65,
                "content_type": "text/plain"
              }
            ]
          }
        ]
        """.trimIndent()

        val releases = ReleaseParser.parseReleases(json)
        assertEquals(1, releases.size)

        val release = releases[0]
        assertEquals("v0.1.0-beta.1", release.tagName)
        assertTrue(release.isPrerelease)
        assertEquals("MIORA-v0.1.0-beta.1.apk", release.apkAsset.name)
        assertEquals(18147215L, release.apkAsset.sizeBytes)
        assertNotNull(release.sha256Asset)
        assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", release.expectedSha256)
    }

    @Test
    fun updateDecisionEngine_filtersByChannel() {
        val releases = listOf(
            createFakeRelease("v0.1.0-beta.1", isPrerelease = true),
            createFakeRelease("v0.1.0", isPrerelease = false),
            createFakeRelease("v0.2.0-beta.1", isPrerelease = true),
        )

        // For stable channel from 0.1.0-beta.1: should select v0.1.0 (ignores 0.2.0-beta.1)
        val stableUpdate = UpdateDecisionEngine.findEligibleUpdate(
            releases = releases,
            currentVersionStr = "0.1.0-beta.1",
            channel = UpdateChannel.STABLE
        )
        assertNotNull(stableUpdate)
        assertEquals("v0.1.0", stableUpdate!!.tagName)

        // For beta channel from 0.1.0-beta.1: should select highest available, which is v0.2.0-beta.1
        val betaUpdate = UpdateDecisionEngine.findEligibleUpdate(
            releases = releases,
            currentVersionStr = "0.1.0-beta.1",
            channel = UpdateChannel.BETA
        )
        assertNotNull(betaUpdate)
        assertEquals("v0.2.0-beta.1", betaUpdate!!.tagName)
    }

    @Test
    fun updateDecisionEngine_preventsDowngradeAndDuplicate() {
        val releases = listOf(
            createFakeRelease("v0.1.0-beta.1", isPrerelease = true),
            createFakeRelease("v0.0.9", isPrerelease = false),
        )

        // Current version is v0.1.0-beta.1; no candidate is newer
        val result = UpdateDecisionEngine.findEligibleUpdate(
            releases = releases,
            currentVersionStr = "0.1.0-beta.1",
            channel = UpdateChannel.BETA
        )
        assertNull(result)

        // Same version check
        assertFalse(UpdateDecisionEngine.isStrictlyNewer("0.1.0-beta.1", "0.1.0-beta.1"))
        assertFalse(UpdateDecisionEngine.isStrictlyNewer("0.1.0", "0.1.0-beta.1"))
        assertTrue(UpdateDecisionEngine.isStrictlyNewer("0.1.0-beta.1", "0.1.0"))
    }

    @Test
    fun updateDownloadManager_verifiesChecksumCorrectly() {
        val tempFile = File.createTempFile("miora_test_", ".apk")
        try {
            val content = "MIORA_DETERMINISTIC_TEST_BINARY_DATA_2026".toByteArray(Charsets.UTF_8)
            tempFile.writeBytes(content)

            val computedHash = UpdateDownloadManager.calculateFileSha256(tempFile)
            assertEquals(64, computedHash.length)

            assertTrue(UpdateDownloadManager.verifyChecksum(tempFile, computedHash))
            assertTrue(UpdateDownloadManager.verifyChecksum(tempFile, computedHash.uppercase()))
            assertFalse(UpdateDownloadManager.verifyChecksum(tempFile, "0000000000000000000000000000000000000000000000000000000000000000"))
        } finally {
            tempFile.delete()
        }
    }

    private fun createFakeRelease(tagName: String, isPrerelease: Boolean): ReleaseInfo {
        return ReleaseInfo(
            tagName = tagName,
            versionName = "MIORA $tagName",
            isPrerelease = isPrerelease,
            changelog = "Test changelog for $tagName",
            publishedAt = "2026-11-10T17:00:00Z",
            apkAsset = ReleaseAsset(
                name = "MIORA-$tagName.apk",
                downloadUrl = "https://example.com/MIORA-$tagName.apk",
                sizeBytes = 10_000_000L
            )
        )
    }
}
