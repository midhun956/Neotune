package com.example.neotune.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.neotune.SearchViewModel
import com.example.neotune.SongResult

@Composable
fun PlaylistDetailScreen(
        playlistName: String,
        songs: List<SongResult>,
        onBack: () -> Unit,
        onSongClick: (SongResult) -> Unit,
        onRemoveSong: (SongResult) -> Unit,
        viewModel: SearchViewModel
) {
    Column(
            modifier =
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // --- Header ---
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                        playlistName,
                        style =
                                MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Text(
                        "${songs.size} song${if (songs.size != 1) "s" else ""}",
                        style =
                                MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                )
            }
        }

        // --- Play All Button ---
        if (songs.isNotEmpty()) {
            Button(
                    onClick = { onSongClick(songs.first()) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Play All")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Song List ---
        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                            "No songs yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                            "Add songs from Search or player",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                itemsIndexed(songs) { _, song ->
                    PlaylistSongRow(
                            song = song,
                            onClick = { onSongClick(song) },
                            onRemove = { onRemoveSong(song) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun PlaylistSongRow(
        song: SongResult,
        onClick: () -> Unit,
        onRemove: () -> Unit
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable { onClick() }
                            .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        if (song.thumbnailUrl != null) {
            Image(
                    painter = rememberAsyncImagePainter(song.thumbnailUrl),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(
                    modifier =
                            Modifier.size(52.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            Text(
                    song.artist ?: "Unknown Artist",
                    style =
                            MaterialTheme.typography.bodySmall.copy(
                                    color =
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }

        if (song.duration != null) {
            Text(
                    song.duration,
                    style =
                            MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray
                            ),
                    modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove from playlist",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
