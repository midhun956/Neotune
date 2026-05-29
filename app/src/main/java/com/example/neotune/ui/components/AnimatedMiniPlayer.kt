package com.example.neotune.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.neotune.SongResult

@Composable
fun AnimatedMiniPlayer(
        song: SongResult,
        isPlaying: Boolean,
        onPlayPause: () -> Unit,
        onClick: () -> Unit,
        isLiked: Boolean,
        onLike: () -> Unit,
        onAddToPlaylist: () -> Unit,
        onBoundsChange: (IntSize) -> Unit,
        playbackPosition: Long,
        duration: Long,
) {
    // Modern Floating Card Style Mini Player (inspired by Metrolist & YouTube Music)
    Card(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable(onClick = onClick)
                            .onGloballyPositioned { onBoundsChange(it.size) },
            shape = RoundedCornerShape(16.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                if (song.thumbnailUrl != null) {
                    Image(
                            painter = rememberAsyncImagePainter(song.thumbnailUrl),
                            contentDescription = null,
                            modifier =
                                    Modifier.size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                            modifier =
                                    Modifier.size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                    Text(
                            song.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                }
                
                val playPauseIcon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                val playPauseDescription = if (isPlaying) "Pause" else "Play"
                
                IconButton(
                        onClick = onPlayPause,
                        colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                        )
                ) {
                    Icon(
                            imageVector = playPauseIcon,
                            contentDescription = playPauseDescription,
                            modifier = Modifier.size(28.dp)
                    )
                }

                // Add to Playlist Button
                IconButton(
                        onClick = onAddToPlaylist,
                        colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                ) {
                    Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            modifier = Modifier.size(26.dp)
                    )
                }

                // Like Button
                val likeIcon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
                val likeColor = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                IconButton(
                        onClick = onLike,
                        colors = IconButtonDefaults.iconButtonColors(
                                contentColor = likeColor
                        )
                ) {
                    Icon(
                            imageVector = likeIcon,
                            contentDescription = "Like",
                            modifier = Modifier.size(24.dp)
                    )
                }
            }
            if (duration > 0) {
                LinearProgressIndicator(
                        progress = { playbackPosition / duration.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }
        }
    }
}
