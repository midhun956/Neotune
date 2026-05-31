package com.example.neotune.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.neotune.SearchViewModel
import com.example.neotune.SongOptionsSheet
import com.example.neotune.SongResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
        playlistName: String,
        songs: List<SongResult>,
        onBack: () -> Unit,
        onSongClick: (SongResult) -> Unit,
        onRemoveSong: (SongResult) -> Unit,
        onPlaylistRenamed: (String) -> Unit,
        viewModel: SearchViewModel,
        isPlaying: Boolean,
        onPlayPauseToggle: () -> Unit
) {
    val context = LocalContext.current
    var showMoreSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(true) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    val currentSongState = viewModel.selectedSong
    val isCurrentPlaylistPlaying = currentSongState.value != null && songs.any { it.videoId == currentSongState.value?.videoId }

    Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // --- Static Header ---
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = {
                Toast.makeText(context, "Search inside playlist coming soon!", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        // --- Scrollable content ---
        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Playlist Cover Grid
            item {
                Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                ) {
                    PlaylistCover(
                            songs = songs,
                            modifier = Modifier.size(260.dp),
                            onEditClick = { showRenameDialog = true }
                    )
                }
            }

            // 2. Title & Metadata
            item {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                            text = playlistName,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                            text = "${songs.size} song${if (songs.size != 1) "s" else ""} • ${formatTotalDuration(songs)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                    )
                }
            }

            // 3. Playback Controls Row (Shuffle, Play/Pause, More)
            item {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle button
                    IconButton(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    viewModel.playPlaylist(playlistName, shuffle = true)
                                } else {
                                    Toast.makeText(context, "Playlist is empty!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Play/Pause button
                    IconButton(
                            onClick = {
                                if (songs.isNotEmpty()) {
                                    if (isCurrentPlaylistPlaying) {
                                        onPlayPauseToggle()
                                    } else {
                                        viewModel.playPlaylist(playlistName, shuffle = false)
                                    }
                                } else {
                                    Toast.makeText(context, "Playlist is empty!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                                imageVector = if (isCurrentPlaylistPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isCurrentPlaylistPlaying && isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // 3 dots button
                    IconButton(
                            onClick = { showMoreSheet = true },
                            modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 4. Custom Order Header Row
            item {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = "Custom order",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                    IconButton(
                            onClick = { isLocked = !isLocked },
                            modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = if (isLocked) "Unlock to rearrange" else "Lock order",
                                tint = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 5. Song List Rows
            if (songs.isEmpty()) {
                item {
                    Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                    text = "No songs yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(songs) { index, song ->
                    val isDragging = draggedIndex == index
                    PlaylistSongRow(
                            song = song,
                            onClick = { viewModel.playPlaylistSong(playlistName, song) },
                            onRemove = { onRemoveSong(song) },
                            viewModel = viewModel,
                            isLocked = isLocked,
                            isDragging = isDragging,
                            dragOffset = dragOffset,
                            onDragStart = { draggedIndex = index },
                            onDragEnd = {
                                draggedIndex = null
                                dragOffset = 0f
                            },
                            onDrag = { dragAmountY ->
                                dragOffset += dragAmountY
                                val rowHeightPx = 200f
                                val currentDragged = draggedIndex
                                if (currentDragged != null) {
                                    if (dragOffset > rowHeightPx / 2f && currentDragged < songs.size - 1) {
                                        viewModel.movePlaylistSong(playlistName, currentDragged, currentDragged + 1)
                                        draggedIndex = currentDragged + 1
                                        dragOffset -= rowHeightPx
                                    } else if (dragOffset < -rowHeightPx / 2f && currentDragged > 0) {
                                        viewModel.movePlaylistSong(playlistName, currentDragged, currentDragged - 1)
                                        draggedIndex = currentDragged - 1
                                        dragOffset += rowHeightPx
                                    }
                                }
                            },
                            modifier = Modifier.animateItem()
                    )
                    HorizontalDivider(
                            modifier = Modifier.padding(start = 80.dp, end = 24.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }

    // --- Options Modal Bottom Sheet ---
    if (showMoreSheet) {
        ModalBottomSheet(
                onDismissRequest = { showMoreSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                OptionItem(
                        icon = Icons.Filled.Edit,
                        title = "Edit",
                        subtitle = "Edit playlist name",
                        onClick = {
                            showMoreSheet = false
                            showRenameDialog = true
                        }
                )
                OptionItem(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        title = "Add to queue",
                        subtitle = "Add to the end of the queue",
                        onClick = {
                            showMoreSheet = false
                            songs.forEach { viewModel.addToQueue(it) }
                            Toast.makeText(context, "Added ${songs.size} songs to queue!", Toast.LENGTH_SHORT).show()
                        }
                )
                OptionItem(
                        icon = Icons.Filled.Download,
                        title = "Download playlist",
                        subtitle = "Download all songs for offline playback",
                        onClick = {
                            showMoreSheet = false
                            viewModel.downloadPlaylist(playlistName)
                            Toast.makeText(context, "Downloading playlist in background...", Toast.LENGTH_SHORT).show()
                        }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                OptionItem(
                        icon = Icons.Filled.Delete,
                        title = "Delete",
                        subtitle = "Delete this playlist",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            showMoreSheet = false
                            showDeleteDialog = true
                        }
                )
            }
        }
    }

    // --- Rename Dialog ---
    if (showRenameDialog) {
        var tempName by remember { mutableStateOf(playlistName) }
        AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Playlist") },
                text = {
                    OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                if (tempName.isNotBlank()) {
                                    viewModel.renamePlaylist(playlistName, tempName)
                                    onPlaylistRenamed(tempName)
                                    showRenameDialog = false
                                }
                            }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                }
        )
    }

    // --- Delete Confirmation Dialog ---
    if (showDeleteDialog) {
        AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Playlist") },
                text = { Text("Are you sure you want to delete the playlist \"$playlistName\"? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.deletePlaylist(playlistName)
                                showDeleteDialog = false
                                onBack()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
        )
    }
}

@Composable
private fun PlaylistCover(
        songs: List<SongResult>,
        modifier: Modifier = Modifier,
        onEditClick: (() -> Unit)? = null
) {
    Box(
            modifier = modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (songs.isEmpty()) {
            Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else if (songs.size < 4) {
            val firstSong = songs.first()
            if (firstSong.thumbnailUrl != null) {
                Image(
                        painter = rememberAsyncImagePainter(firstSong.thumbnailUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                )
            } else {
                Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                ) {
                    Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            // 2x2 grid of the first 4 songs
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Image(
                                painter = rememberAsyncImagePainter(songs[0].thumbnailUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Image(
                                painter = rememberAsyncImagePainter(songs[1].thumbnailUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )
                    }
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Image(
                                painter = rememberAsyncImagePainter(songs[2].thumbnailUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Image(
                                painter = rememberAsyncImagePainter(songs[3].thumbnailUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Edit button overlay at bottom right
        if (onEditClick != null) {
            Box(
                    modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit playlist name",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistSongRow(
        song: SongResult,
        onClick: () -> Unit,
        onRemove: () -> Unit,
        viewModel: SearchViewModel,
        isLocked: Boolean = true,
        isDragging: Boolean = false,
        dragOffset: Float = 0f,
        onDragStart: () -> Unit = {},
        onDragEnd: () -> Unit = {},
        onDrag: (Float) -> Unit = {},
        modifier: Modifier = Modifier
) {
    var showOptionsSheet by remember { mutableStateOf(false) }

    Row(
            modifier = modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else 0f
                        shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                        scaleX = if (isDragging) 1.02f else 1.0f
                        scaleY = if (isDragging) 1.02f else 1.0f
                    }
                    .zIndex(if (isDragging) 1f else 0f)
                    .combinedClickable(
                            onClick = { onClick() },
                            onLongClick = { showOptionsSheet = true }
                    )
                    .padding(vertical = 10.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        if (song.thumbnailUrl != null) {
            Image(
                    painter = rememberAsyncImagePainter(song.thumbnailUrl),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
            )
        } else {
            Box(
                    modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
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

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                    song.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }

        val isDownloaded = viewModel.downloadedSongs.value.containsKey(song.videoId)
        val isDownloading = viewModel.activeDownloads.value.contains(song.videoId)

        if (isDownloading) {
            CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
        } else if (isDownloaded) {
            Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
        }

        if (song.duration != null) {
            Text(
                    song.duration,
                    style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        if (!isLocked) {
            Box(
                    modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                        onDragStart = { onDragStart() },
                                        onDragEnd = { onDragEnd() },
                                        onDragCancel = { onDragEnd() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDrag(dragAmount.y)
                                        }
                                )
                            },
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Filled.Reorder,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                )
            }
        } else {
            IconButton(onClick = { showOptionsSheet = true }) {
                Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Song options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }

    if (showOptionsSheet) {
        val isDownloaded = viewModel.downloadedSongs.value.containsKey(song.videoId)
        val isDownloading = viewModel.activeDownloads.value.contains(song.videoId)
        SongOptionsSheet(
                song = song,
                isLiked = viewModel.isLiked(song),
                onDismiss = { showOptionsSheet = false },
                onPlayNext = { viewModel.playNext(song) },
                onAddToPlaylist = {
                    viewModel.songToAddToPlaylist.value = song
                    viewModel.showAddToPlaylistSheet.value = true
                },
                onAddToQueue = { viewModel.addToQueue(song) },
                onLike = { viewModel.toggleLike(song) },
                onViewArtist = {
                    song.artist?.let { viewModel.searchAndNavigateToArtist(it) }
                },
                onViewAlbum = {
                    song.album?.let { viewModel.searchAndNavigateToAlbum(it) }
                },
                onRemoveFromPlaylist = onRemove,
                isDownloaded = isDownloaded,
                isDownloading = isDownloading,
                onDownloadToggle = {
                    if (isDownloading) {
                        viewModel.cancelDownload(song.videoId)
                    } else if (isDownloaded) {
                        viewModel.removeDownload(song.videoId)
                    } else {
                        viewModel.downloadSong(song)
                    }
                }
        )
    }
}

@Composable
private fun OptionItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit,
        tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(vertical = 12.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = tint
            )
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            )
        }
    }
}

private fun formatTotalDuration(songs: List<SongResult>): String {
    var totalSeconds = 0
    for (song in songs) {
        val parts = song.duration?.split(":") ?: continue
        if (parts.size == 2) {
            val mins = parts[0].toIntOrNull() ?: 0
            val secs = parts[1].toIntOrNull() ?: 0
            totalSeconds += mins * 60 + secs
        } else if (parts.size == 3) {
            val hrs = parts[0].toIntOrNull() ?: 0
            val mins = parts[1].toIntOrNull() ?: 0
            val secs = parts[2].toIntOrNull() ?: 0
            totalSeconds += hrs * 3600 + mins * 60 + secs
        }
    }
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}
