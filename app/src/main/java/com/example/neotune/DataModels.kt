package com.example.neotune

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

sealed interface SearchResultItem

@Serializable
data class SongResult(
        val title: String,
        val artist: String?,
        val thumbnailUrl: String?,
        val videoId: String,
        val album: String?,
        val duration: String? = null,
) : SearchResultItem

@Serializable
data class ArtistResult(
        val name: String,
        val thumbnailUrl: String?,
        val browseId: String?,
) : SearchResultItem

@Serializable
data class AlbumResult(
        val title: String,
        val artists: String?,
        val year: String?,
        val thumbnailUrl: String?,
        val browseId: String?,
) : SearchResultItem

@Serializable
data class PlaylistResult(
        val name: String,
        val author: String?,
        val songCount: String?,
        val thumbnailUrl: String?,
        val browseId: String?,
) : SearchResultItem

data class Playlist(
        val name: String,
        val icon: ImageVector? = null,
        val isLiked: Boolean = false,
        val songCount: Int = 0,
        val isUserCreated: Boolean = false
)

data class ArtistDetails(
        val name: String,
        val description: String?,
        val thumbnailUrl: String?,
        val topSongs: List<SongResult> = emptyList(),
        val albums: List<AlbumResult> = emptyList()
)

data class AlbumDetails(
        val title: String,
        val description: String?,
        val thumbnailUrl: String?,
        val year: String?,
        val artist: String?,
        val tracks: List<AlbumTrack> = emptyList()
)

data class AlbumTrack(
        val title: String,
        val videoId: String,
        val duration: String?,
        val artists: String?
)

// Utility function
fun formatTime(milliseconds: Long): String {
        if (milliseconds <= 0) return "0:00"
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes >= 60) {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                String.format("%d:%02d:%02d", hours, remainingMinutes, seconds)
        } else {
                String.format("%d:%02d", minutes, seconds)
        }
}
