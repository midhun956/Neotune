package com.example.neotune.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
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
import com.example.neotune.SongResult

@Composable
fun LikedSongsScreen(
        likedSongs: List<SongResult>,
        onBack: () -> Unit,
        onSongClick: (SongResult) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(16.dp))
            Text(
                    "Liked Songs",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(likedSongs) { song ->
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable { onSongClick(song) }
                                        .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    if (song.thumbnailUrl != null) {
                        Image(
                                painter = rememberAsyncImagePainter(song.thumbnailUrl),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                                modifier =
                                        Modifier.size(56.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Gray),
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                song.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                        Text(
                                song.artist ?: "Unknown Artist",
                                style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.Gray
                                        ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Text(
                            song.duration ?: "0:00",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }

                if (song != likedSongs.last()) {
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}
