package com.fahim.powerampclone.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads update APK assets and performs cryptographic SHA-256 integrity verification.
 */
object UpdateDownloadManager {

    private val HASH_PATTERN = Regex("""([a-fA-F0-9]{64})""")

    /**
     * Downloads an APK to the destination file, emitting periodic progress updates.
     */
    suspend fun downloadFile(
        sourceUrl: String,
        destinationFile: File,
        onProgress: (percent: Int, bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            destinationFile.parentFile?.mkdirs()
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val url = URL(sourceUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MIORA-Android-Updater")
            }

            val responseCode = connection.responseCode
            // Handle redirects if needed
            val effectiveConnection = if (responseCode in 301..308) {
                val redirectUrl = connection.getHeaderField("Location")
                connection.disconnect()
                (URL(redirectUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "MIORA-Android-Updater")
                }
            } else {
                connection
            }

            val totalBytes = effectiveConnection.contentLengthLong
            val inputStream = effectiveConnection.inputStream
            val outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(16 * 1024)
            var bytesReadTotal = 0L
            var bytesRead: Int
            var lastReportedPercent = -1

            outputStream.use { out ->
                inputStream.use { inp ->
                    while (inp.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        bytesReadTotal += bytesRead

                        val percent = if (totalBytes > 0) {
                            ((bytesReadTotal * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }

                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            onProgress(percent, bytesReadTotal, totalBytes)
                        }
                    }
                }
            }
            effectiveConnection.disconnect()
            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads an optional companion .sha256 checksum file and extracts the 64-char hex string.
     */
    suspend fun fetchSha256FromUrl(checksumUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(checksumUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MIORA-Android-Updater")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val text = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                connection.disconnect()
                HASH_PATTERN.find(text)?.groupValues?.getOrNull(1)
            } else {
                connection.disconnect()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculates the SHA-256 hash of a local file.
     */
    fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        file.inputStream().use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val bytes = digest.digest()
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies that the given file matches the expected SHA-256 hex string.
     */
    fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        if (!file.exists() || !file.canRead()) return false
        val computed = calculateFileSha256(file)
        return computed.equals(expectedSha256.trim(), ignoreCase = true)
    }
}
