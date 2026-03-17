package com.example.neotune.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.neotune.SearchViewModel
import com.example.neotune.SongResult
import com.example.neotune.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayerSheet(
        song: SongResult,
        isPlaying: Boolean,
        playbackPosition: Long,
        duration: Long,
        onPlayPause: () -> Unit,
        onSeek: (Long) -> Unit,
        onClose: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
        isLiked: Boolean,
        onLike: () -> Unit,
        isLooping: Boolean,
        onToggleLoop: () -> Unit,
        onAddToPlaylist: (String) -> Unit,
        onCreatePlaylist: (String) -> Unit,
        viewModel: SearchViewModel
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showQueue by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close")
            }
            Spacer(Modifier.weight(1f))
            val highResUrl = song.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+-"), "=w1080-h1080-")

            if (highResUrl != null) {
                Image(
                        painter = rememberAsyncImagePainter(highResUrl),
                        contentDescription = null,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(100.dp)
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                    song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
            )
            Text(
                    song.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))

            Slider(
                    value = if (duration > 0) playbackPosition.toFloat() else 0f,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                    modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                        formatTime(playbackPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                )
                Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                val likeIcon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
                val likeIconTint = if (isLiked) MaterialTheme.colorScheme.primary else Color.Gray
                val playPauseIcon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                val playPauseDescription = if (isPlaying) "Pause" else "Play"
                val loopIconTint = if (isLooping) MaterialTheme.colorScheme.primary else Color.Gray

                IconButton(onClick = onLike) {
                    Icon(imageVector = likeIcon, contentDescription = "Like", tint = likeIconTint)
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(
                        onClick = onPlayPause,
                        modifier =
                                Modifier.size(64.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                            imageVector = playPauseIcon,
                            contentDescription = playPauseDescription,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onToggleLoop) {
                    Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Loop",
                            tint = loopIconTint
                    )
                }
            }
            Spacer(Modifier.weight(0.5f))
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                    if (showMenu) {
                        DropdownMenu(expanded = true, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                    text = { Text("Add to queue") },
                                    onClick = {
                                        viewModel.addToQueue(song)
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null)
                                    }
                            )
                            DropdownMenuItem(
                                    text = { Text("Add to playlist") },
                                    onClick = {
                                        showMenu = false
                                        showPlaylistDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null)
                                    }
                            )
                        }
                    }
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Show Queue")
                }
            }
        }
    }

    if (showQueue) {
        ModalBottomSheet(
                onDismissRequest = { showQueue = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                )

                val queue = viewModel.queue.value
                if (queue.isEmpty()) {
                    Text(
                            text = "No songs in queue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 32.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        itemsIndexed(queue) { index, queuedSong ->
                            SongRowItem(
                                    song = queuedSong,
                                    onSongClick = {
                                        // Jump to this song in the queue
                                        viewModel.playSong(queuedSong)
                                        // Remove this song and all previous songs from the queue
                                        viewModel.queue.value = queue.drop(index + 1)
                                        showQueue = false
                                    },
                                    viewModel = viewModel
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showPlaylistDialog) {
        AlertDialog(
                onDismissRequest = { showPlaylistDialog = false },
                title = { Text("Add to playlist") },
                text = {
                    Column {
                        TextButton(
                                onClick = {
                                    showPlaylistDialog = false
                                    showCreatePlaylistDialog = true
                                }
                        ) { Text("Create new playlist") }
                        viewModel.playlists.value.keys.forEach { playlistName ->
                            TextButton(
                                    onClick = {
                                        onAddToPlaylist(playlistName)
                                        showPlaylistDialog = false
                                    }
                            ) { Text(playlistName) }
                        }
                    }
                },
                confirmButton = {}
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text("Create New Playlist") },
                text = {
                    OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Playlist Name") }
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    onCreatePlaylist(newPlaylistName)
                                    showCreatePlaylistDialog = false
                                    newPlaylistName = ""
                                }
                            }
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
                }
        )
    }
}
