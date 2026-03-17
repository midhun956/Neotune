package com.example.neotune.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.neotune.AlbumResult
import com.example.neotune.ArtistResult
import com.example.neotune.PlaylistResult
import com.example.neotune.SearchViewModel
import com.example.neotune.SongResult

@Composable
fun HorizontalSongItem(
        song: SongResult,
        onSongClick: (SongResult) -> Unit,
        viewModel: SearchViewModel
) {
        Column(
                modifier =
                        Modifier.width(140.dp)
                                .clickable { onSongClick(song) }
                                .padding(bottom = 8.dp)
        ) {
                if (song.thumbnailUrl != null) {
                        Image(
                                painter = rememberAsyncImagePainter(model = song.thumbnailUrl),
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier =
                                        Modifier.size(140.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                )
                        )
                } else {
                        Box(
                                modifier =
                                        Modifier.size(140.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = "Song",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                        song.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                )
                if (song.artist != null) {
                        Text(
                                song.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                        )
                }
        }
}

@Composable
fun HorizontalAlbumItem(album: AlbumResult, onAlbumClick: (AlbumResult) -> Unit) {
        Column(
                modifier =
                        Modifier.width(140.dp)
                                .clickable { onAlbumClick(album) }
                                .padding(bottom = 8.dp)
        ) {
                if (album.thumbnailUrl != null) {
                        Image(
                                painter = rememberAsyncImagePainter(model = album.thumbnailUrl),
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier =
                                        Modifier.size(140.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                )
                        )
                } else {
                        Box(
                                modifier =
                                        Modifier.size(140.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        Icons.Default.Album,
                                        contentDescription = "Album",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                        album.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                )
                if (album.year != null) {
                        Text(
                                album.year,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                        )
                }
        }
}

@Composable
fun SongRowItem(song: SongResult, onSongClick: (SongResult) -> Unit, viewModel: SearchViewModel) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                        Modifier.fillMaxWidth()
                                .clickable { onSongClick(song) }
                                .padding(vertical = 8.dp)
        ) {
                if (song.thumbnailUrl != null) {
                        Image(
                                painter = rememberAsyncImagePainter(model = song.thumbnailUrl),
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier =
                                        Modifier.size(56.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                )
                        )
                } else {
                        Box(
                                modifier =
                                        Modifier.size(56.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = "Song",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                        Text(
                                song.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                                song.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
                if (song.duration != null) {
                        Text(
                                song.duration,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 8.dp)
                        )
                }
                IconButton(onClick = { viewModel.addToQueue(song) }) {
                        Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                "Add to queue",
                                tint = Color.Gray
                        )
                }
        }
}

@Composable
fun AlbumRowItem(album: AlbumResult, onClick: () -> Unit) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                        Modifier.fillMaxWidth()
                                .clickable(onClick = onClick)
                                .padding(vertical = 8.dp)
        ) {
                if (album.thumbnailUrl != null) {
                        Image(
                                painter = rememberAsyncImagePainter(model = album.thumbnailUrl),
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier =
                                        Modifier.size(56.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                )
                        )
                } else {
                        Box(
                                modifier =
                                        Modifier.size(56.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        Icons.Default.Album,
                                        contentDescription = "Album",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                        Text(
                                album.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                                "Album • ${album.artists ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
        }
}

@Composable
fun PlaylistRowItem(playlist: PlaylistResult) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                        Modifier.fillMaxWidth()
                                .clickable { /* TODO: Handle playlist click */}
                                .padding(vertical = 8.dp)
        ) {
                if (playlist.thumbnailUrl != null) {
                        Image(
                                painter = rememberAsyncImagePainter(model = playlist.thumbnailUrl),
                                contentDescription = playlist.name,
                                contentScale = ContentScale.Crop,
                                modifier =
                                        Modifier.size(56.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                )
                        )
                } else {
                        Box(
                                modifier =
                                        Modifier.size(56.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        Icons.AutoMirrored.Filled.PlaylistPlay,
                                        contentDescription = "Playlist",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                        Text(
                                playlist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                                "Playlist • ${playlist.author ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
        }
}

@Composable
fun ArtistRowItem(artist: ArtistResult, onClick: () -> Unit) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                        Modifier.fillMaxWidth()
                                .clickable(onClick = onClick)
                                .padding(vertical = 8.dp)
        ) {
                if (artist.thumbnailUrl != null) {
                        Image(
                                painter = rememberAsyncImagePainter(model = artist.thumbnailUrl),
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier =
                                        Modifier.size(56.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                )
                        )
                } else {
                        Box(
                                modifier =
                                        Modifier.size(56.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Artist",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                        Text(
                                artist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                                "Artist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                        )
                }
        }
}
