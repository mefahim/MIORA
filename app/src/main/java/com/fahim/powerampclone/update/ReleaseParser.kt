package com.fahim.powerampclone.update

/**
 * Parser for GitHub Releases API responses.
 */
object ReleaseParser {

    private val SHA256_BODY_REGEX = Regex(
        """(?:SHA-?256|sha256|checksum)[\s:=]+([a-fA-F0-9]{64})""",
        RegexOption.IGNORE_CASE
    )

    fun parseReleases(jsonString: String): List<ReleaseInfo> {
        val root = try {
            SimpleJsonParser.parse(jsonString)
        } catch (_: Exception) {
            return emptyList()
        }

        val items = when (root) {
            is JsonElement.Arr -> root.list
            is JsonElement.Obj -> listOf(root)
            else -> emptyList()
        }

        return items.mapNotNull { parseSingleRelease(it) }
    }

    fun parseSingleRelease(element: JsonElement): ReleaseInfo? {
        val obj = element.asObject() ?: return null
        val tagName = obj["tag_name"]?.asString() ?: return null
        val name = obj["name"]?.asString() ?: tagName
        val prerelease = obj["prerelease"]?.asBoolean() ?: false
        val body = obj["body"]?.asString() ?: ""
        val publishedAt = obj["published_at"]?.asString() ?: ""

        val rawAssets = obj["assets"]?.asArray()?.list ?: emptyList()
        val assets = rawAssets.mapNotNull { assetEl ->
            val assetObj = assetEl.asObject() ?: return@mapNotNull null
            val assetName = assetObj["name"]?.asString() ?: return@mapNotNull null
            val downloadUrl = assetObj["browser_download_url"]?.asString() ?: return@mapNotNull null
            val size = assetObj["size"]?.asLong() ?: 0L
            val contentType = assetObj["content_type"]?.asString() ?: "application/vnd.android.package-archive"
            ReleaseAsset(
                name = assetName,
                downloadUrl = downloadUrl,
                sizeBytes = size,
                contentType = contentType
            )
        }

        // Look for the distribution APK asset
        val apkAsset = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: return null
        val sha256Asset = assets.firstOrNull {
            it.name.endsWith(".sha256", ignoreCase = true) || it.name.endsWith(".sha256sum", ignoreCase = true)
        }

        // Extract expected SHA-256 hash from markdown release body if available
        val bodyHash = SHA256_BODY_REGEX.find(body)?.groupValues?.getOrNull(1)

        return ReleaseInfo(
            tagName = tagName,
            versionName = name,
            isPrerelease = prerelease,
            changelog = body,
            publishedAt = publishedAt,
            apkAsset = apkAsset,
            sha256Asset = sha256Asset,
            expectedSha256 = bodyHash
        )
    }
}
