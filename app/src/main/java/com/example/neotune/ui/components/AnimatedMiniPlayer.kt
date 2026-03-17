package com.example.neotune.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
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
        onNext: () -> Unit,
        onBoundsChange: (IntSize) -> Unit,
        playbackPosition: Long,
        duration: Long,
) {
    Column {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(onClick = onClick)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .onGloballyPositioned { onBoundsChange(it.size) },
                verticalAlignment = Alignment.CenterVertically
        ) {
            if (song.thumbnailUrl != null) {
                Image(
                        painter = rememberAsyncImagePainter(song.thumbnailUrl),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
                )
            } else {
                Box(
                        modifier =
                                Modifier.size(48.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                        song.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            }
            val playPauseIcon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
            val playPauseDescription = if (isPlaying) "Pause" else "Play"
            IconButton(onClick = onPlayPause) {
                Icon(imageVector = playPauseIcon, contentDescription = playPauseDescription)
            }
            IconButton(onClick = onNext) {
                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next")
            }
        }
        if (duration > 0) {
            LinearProgressIndicator(
                    progress = { playbackPosition / duration.toFloat() },
                    modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
