package com.example.neotune

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*

object YouTubeMusicApi {
    private val client =
            HttpClient(CIO) {
                install(ContentNegotiation) { json() }
                install(HttpTimeout) {
                    requestTimeoutMillis = Config.REQUEST_TIMEOUT
                    connectTimeoutMillis = Config.CONNECT_TIMEOUT
                    socketTimeoutMillis = Config.SOCKET_TIMEOUT
                }
            }

    suspend fun search(query: String, scope: String?, maxRetries: Int = 3): String {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                val response: HttpResponse =
                        client.get("${Config.BACKEND_BASE_URL}/search") {
                            parameter("query", query)
                            parameter("limit", Config.DEFAULT_SEARCH_LIMIT)
                            // Add the scope/filter parameter if it's provided and not "All"
                            if (scope != null && scope.lowercase() != "all") {
                                parameter("scope", scope.lowercase())
                            }
                        }
                val responseText = response.bodyAsText()
                println("Search response: $responseText") // Debug log
                return responseText
            } catch (e: Exception) {
                lastException = e
                println(
                        "Search error (attempt ${attempt + 1}/$maxRetries): ${e.message}"
                ) // Debug log

                if (attempt < maxRetries - 1) {
                    delay(1000L * (attempt + 1)) // Exponential backoff
                }
            }
        }

        throw lastException ?: Exception("Failed to search after $maxRetries attempts")
    }

    suspend fun getArtist(browseId: String, maxRetries: Int = 3): String {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val response: HttpResponse =
                        client.get("${Config.BACKEND_BASE_URL}/artist") {
                            parameter("browseId", browseId)
                        }
                return response.bodyAsText()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(1000L * (attempt + 1))
                }
            }
        }
        throw lastException ?: Exception("Failed to get artist details after $maxRetries attempts")
    }

    suspend fun getStreamUrl(videoId: String, maxRetries: Int = 3): String {
        // Use the backend audio URL instead of YouTube Music API
        val audioUrl = getBackendAudioUrl(videoId)
        return audioUrl ?: throw Exception("Failed to get audio URL")
    }

    // Get streaming audio URL from FastAPI backend
    suspend fun getBackendAudioUrl(videoId: String, maxRetries: Int = 3): String? {
        // The backend streams audio directly, so we return the streaming endpoint URL
        return "${Config.BACKEND_BASE_URL}/yt_audio?video_id=$videoId"
    }

    suspend fun getRecommended(videoId: String? = null, maxRetries: Int = 3): String {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                val response: HttpResponse =
                        if (videoId != null) {
                            client.get("${Config.BACKEND_BASE_URL}/recommended") {
                                parameter("video_id", videoId)
                                parameter("limit", Config.DEFAULT_RECOMMENDED_LIMIT)
                            }
                        } else {
                            client.get("${Config.BACKEND_BASE_URL}/recommended") {
                                parameter("limit", Config.DEFAULT_RECOMMENDED_LIMIT)
                            }
                        }
                return response.bodyAsText()
            } catch (e: Exception) {
                lastException = e
                println(
                        "Backend recommended error (attempt ${attempt + 1}/$maxRetries): ${e.message}"
                ) // Debug log

                if (attempt < maxRetries - 1) {
                    delay(1000L * (attempt + 1)) // Exponential backoff
                }
            }
        }

        throw lastException ?: Exception("Failed to get recommended after $maxRetries attempts")
    }

    suspend fun getTrending(maxRetries: Int = 3): String {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                val response: HttpResponse =
                        client.get("${Config.BACKEND_BASE_URL}/trending") {
                            parameter("limit", Config.DEFAULT_TRENDING_LIMIT)
                        }
                return response.bodyAsText()
            } catch (e: Exception) {
                lastException = e
                println(
                        "Backend trending error (attempt ${attempt + 1}/$maxRetries): ${e.message}"
                ) // Debug log

                if (attempt < maxRetries - 1) {
                    delay(1000L * (attempt + 1)) // Exponential backoff
                }
            }
        }

        throw lastException ?: Exception("Failed to get trending after $maxRetries attempts")
    }

    suspend fun getRelatedSongsJson(videoId: String): String {
        // Use the backend recommended endpoint
        return getRecommended(videoId)
    }

    suspend fun getNext(videoId: String, maxRetries: Int = 3): String? {
        // Use the backend recommended endpoint
        return getRecommended(videoId, maxRetries)
    }

    suspend fun getAlbum(browseId: String, maxRetries: Int = 3): String {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val response: HttpResponse =
                        client.get("${Config.BACKEND_BASE_URL}/album/$browseId")
                return response.bodyAsText()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(1000L * (attempt + 1))
                }
            }
        }
        throw lastException ?: Exception("Failed to get album details after $maxRetries attempts")
    }
}
