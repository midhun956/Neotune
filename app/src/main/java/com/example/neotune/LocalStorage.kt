package com.example.neotune

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object LocalStorage {
    private const val PREFS_NAME = "neotune_prefs"
    private const val KEY_LIKED_SONGS = "liked_songs"
    private const val KEY_PLAYLISTS = "playlists"
    private const val KEY_RECENTLY_PLAYED = "recently_played"
    private const val KEY_NP_BACKGROUND_STYLE = "np_background_style"

    private val json = Json { ignoreUnknownKeys = true }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Liked Songs ---
    fun saveLikedSongs(context: Context, songs: List<SongResult>) {
        val encoded = json.encodeToString(ListSerializer(SongResult.serializer()), songs)
        prefs(context).edit().putString(KEY_LIKED_SONGS, encoded).apply()
    }

    fun loadLikedSongs(context: Context): List<SongResult> {
        val encoded = prefs(context).getString(KEY_LIKED_SONGS, null) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(SongResult.serializer()), encoded)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Playlists ---
    fun savePlaylists(context: Context, playlists: Map<String, List<SongResult>>) {
        val encoded = json.encodeToString(
            MapSerializer(String.serializer(), ListSerializer(SongResult.serializer())),
            playlists
        )
        prefs(context).edit().putString(KEY_PLAYLISTS, encoded).apply()
    }

    fun loadPlaylists(context: Context): Map<String, List<SongResult>> {
        val encoded = prefs(context).getString(KEY_PLAYLISTS, null) ?: return emptyMap()
        return try {
            json.decodeFromString(
                MapSerializer(String.serializer(), ListSerializer(SongResult.serializer())),
                encoded
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // --- Recently Played ---
    fun saveRecentlyPlayed(context: Context, songs: List<SongResult>) {
        val encoded = json.encodeToString(ListSerializer(SongResult.serializer()), songs)
        prefs(context).edit().putString(KEY_RECENTLY_PLAYED, encoded).apply()
    }

    fun loadRecentlyPlayed(context: Context): List<SongResult> {
        val encoded = prefs(context).getString(KEY_RECENTLY_PLAYED, null) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(SongResult.serializer()), encoded)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Now Playing Background Style ---
    fun saveNPBackgroundStyle(context: Context, style: String) {
        prefs(context).edit().putString(KEY_NP_BACKGROUND_STYLE, style).apply()
    }

    fun loadNPBackgroundStyle(context: Context): String {
        return prefs(context).getString(KEY_NP_BACKGROUND_STYLE, "gradient") ?: "gradient"
    }

    // --- App Theme Style ---
    fun saveAppThemeStyle(context: Context, style: String) {
        prefs(context).edit().putString("app_theme_style", style).apply()
    }

    fun loadAppThemeStyle(context: Context): String {
        return prefs(context).getString("app_theme_style", "amoled") ?: "amoled"
    }

    // --- AMOLED Accent Color ---
    fun saveAmoledAccent(context: Context, accent: String) {
        prefs(context).edit().putString("amoled_accent_color", accent).apply()
    }

    fun loadAmoledAccent(context: Context): String {
        return prefs(context).getString("amoled_accent_color", "purple") ?: "purple"
    }

    // --- Backend Server IP Address ---
    fun saveBackendIp(context: Context, ip: String) {
        prefs(context).edit().putString("backend_ip", ip).apply()
    }

    fun loadBackendIp(context: Context): String {
        return prefs(context).getString("backend_ip", "") ?: ""
    }

    // --- Cache Storage Limit ---
    fun saveCacheLimit(context: Context, limit: String) {
        prefs(context).edit().putString("cache_storage_limit", limit).apply()
    }

    fun loadCacheLimit(context: Context): String {
        return prefs(context).getString("cache_storage_limit", "1.0 GB") ?: "1.0 GB"
    }
}
