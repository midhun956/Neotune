package com.example.neotune

import android.content.Context
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class OfflineTrack(
    val song: SongResult,
    val downloadedAt: Long,
    val lastPlayedAt: Long,
    val fileSize: Long
)

object DownloadManager {
    private const val REGISTRY_FILE = "offline_registry.json"
    private const val CACHE_DIR = "offline_cache"
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000L
            connectTimeoutMillis = 15000L
            socketTimeoutMillis = 60000L
        }
    }

    // Load registry from private storage
    suspend fun loadRegistry(context: Context): Map<String, OfflineTrack> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(context.filesDir, REGISTRY_FILE)
            if (!file.exists()) return@withLock emptyMap()
            try {
                val content = file.readText()
                json.decodeFromString<Map<String, OfflineTrack>>(content)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
    }

    // Save registry to private storage
    private suspend fun saveRegistry(context: Context, registry: Map<String, OfflineTrack>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(context.filesDir, REGISTRY_FILE)
            try {
                val content = json.encodeToString(registry)
                // Use a temporary file for atomic write safety
                val tmpFile = File(context.filesDir, "$REGISTRY_FILE.tmp")
                tmpFile.writeText(content)
                tmpFile.renameTo(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Convert string limit to bytes
    fun getCacheLimitBytes(limitStr: String): Long {
        return when (limitStr) {
            "500 MB" -> 500 * 1024 * 1024L
            "1.0 GB" -> 1024 * 1024 * 1024L
            "2.0 GB" -> 2 * 1024 * 1024 * 1024L
            "5.0 GB" -> 5 * 1024 * 1024 * 1024L
            "Unlimited" -> Long.MAX_VALUE
            else -> 1024 * 1024 * 1024L // Default to 1.0 GB
        }
    }

    // Get total cache size on disk
    suspend fun getTotalCacheSize(context: Context): Long = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, CACHE_DIR)
        if (!cacheDir.exists()) return@withContext 0L
        cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    // Update last played timestamp for a song (LRP)
    suspend fun updateLastPlayed(context: Context, videoId: String) {
        val registry = loadRegistry(context).toMutableMap()
        val track = registry[videoId]
        if (track != null) {
            registry[videoId] = track.copy(lastPlayedAt = System.currentTimeMillis())
            saveRegistry(context, registry)
        }
    }

    // Perform LRP eviction
    suspend fun checkAndEvictCache(context: Context, upcomingSize: Long, limitStr: String) = withContext(Dispatchers.IO) {
        val limitBytes = getCacheLimitBytes(limitStr)
        if (limitBytes == Long.MAX_VALUE) return@withContext

        val cacheDir = File(context.filesDir, CACHE_DIR)
        if (!cacheDir.exists()) cacheDir.mkdirs()

        var currentSize = getTotalCacheSize(context)
        if (currentSize + upcomingSize <= limitBytes) return@withContext

        // Load registry and sort by last played (Least Recently Played first)
        val registry = loadRegistry(context).toMutableMap()
        val sortedTracks = registry.values.sortedBy { it.lastPlayedAt }

        for (track in sortedTracks) {
            if (currentSize + upcomingSize <= limitBytes) break

            val songFile = File(cacheDir, track.song.videoId)
            val fileSize = songFile.length()
            if (songFile.exists() && songFile.delete()) {
                currentSize -= fileSize
                registry.remove(track.song.videoId)
            }
        }
        saveRegistry(context, registry)
    }

    // Download a song
    suspend fun downloadSong(context: Context, song: SongResult, limitStr: String): Boolean = withContext(Dispatchers.IO) {
        var connection: java.net.HttpURLConnection? = null
        var tempFile: File? = null
        try {
            val videoId = song.videoId
            val url = YouTubeMusicApi.getBackendAudioUrl(videoId) ?: return@withContext false

            val cacheDir = File(context.filesDir, CACHE_DIR)
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // Download chunk-by-chunk to a temporary file for atomic write safety
            tempFile = File(cacheDir, "$videoId.download")
            if (tempFile.exists()) tempFile.delete()

            val urlObj = java.net.URL(url)
            connection = urlObj.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Neotune/1.0")
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.connect()

            if (connection.responseCode !in 200..299) {
                tempFile.delete()
                return@withContext false
            }

            connection.inputStream.buffered(65536).use { inputStream ->
                tempFile.outputStream().buffered(65536).use { outputStream ->
                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            }

            val incomingSize = tempFile.length()
            if (incomingSize <= 0) {
                tempFile.delete()
                return@withContext false
            }

            // Run cache eviction first if saving this track would exceed storage limits
            checkAndEvictCache(context, incomingSize, limitStr)

            // Finalize target file atomically
            val targetFile = File(cacheDir, videoId)
            if (targetFile.exists()) targetFile.delete()

            if (tempFile.renameTo(targetFile)) {
                // Register track in registry
                val registry = loadRegistry(context).toMutableMap()
                registry[videoId] = OfflineTrack(
                    song = song,
                    downloadedAt = System.currentTimeMillis(),
                    lastPlayedAt = System.currentTimeMillis(),
                    fileSize = incomingSize
                )
                saveRegistry(context, registry)
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile?.delete()
            false
        } finally {
            connection?.disconnect()
        }
    }

    // Delete a download
    suspend fun deleteDownload(context: Context, videoId: String) = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, CACHE_DIR)
        val songFile = File(cacheDir, videoId)
        if (songFile.exists()) {
            songFile.delete()
        }
        val registry = loadRegistry(context).toMutableMap()
        registry.remove(videoId)
        saveRegistry(context, registry)
    }

    // Clear all downloads
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, CACHE_DIR)
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
        saveRegistry(context, emptyMap())
    }
}
