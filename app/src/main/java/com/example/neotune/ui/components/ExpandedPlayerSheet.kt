package com.example.neotune.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.neotune.AddToPlaylistSheet
import com.example.neotune.SearchViewModel
import com.example.neotune.SongResult
import com.example.neotune.formatTime
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import com.example.neotune.SongOptionsSheet

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

    val context = LocalContext.current
    val backgroundStyle = viewModel.nowPlayingBackgroundStyle.value
    val highResUrl = remember(song.thumbnailUrl) {
        song.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+-"), "=w1080-h1080-")
    }

    var targetColorTop by remember { mutableStateOf(Color(0xFF2C2C2C)) }
    var targetColorBottom by remember { mutableStateOf(Color(0xFF0F0F0F)) }

    val animatedColorTop by animateColorAsState(
        targetValue = targetColorTop,
        animationSpec = tween(durationMillis = 1000),
        label = "ColorTop"
    )
    val animatedColorBottom by animateColorAsState(
        targetValue = targetColorBottom,
        animationSpec = tween(durationMillis = 1000),
        label = "ColorBottom"
    )

    LaunchedEffect(song.videoId) {
        val url = highResUrl
        if (url != null) {
            try {
                val result = withContext(Dispatchers.IO) {
                    val loader = context.imageLoader
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)
                        .build()
                    loader.execute(request)
                }
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.IO) {
                            Palette.from(bitmap).generate()
                        }
                        
                        // Extract a vibrant or dominant swatch to use for a rich top color
                        val swatch = palette.vibrantSwatch 
                            ?: palette.darkVibrantSwatch 
                            ?: palette.dominantSwatch
                        
                        val topColorInt = swatch?.rgb ?: 0xFF2C2C2C.toInt()
                        val bottomColorInt = palette.getDarkMutedColor(
                            palette.getDarkVibrantColor(0xFF0F0F0F.toInt())
                        )
                        
                        val rawTop = Color(topColorInt)
                        val rawBottom = Color(bottomColorInt)
                        
                        // Ensure color pop by limiting minimum intensity and scaling nicely (highly accurate and vibrant matching)
                        val blendedTop = Color(
                            red = (rawTop.red * 0.8f).coerceIn(0.08f, 0.95f),
                            green = (rawTop.green * 0.8f).coerceIn(0.08f, 0.95f),
                            blue = (rawTop.blue * 0.8f).coerceIn(0.08f, 0.95f),
                            alpha = 1f
                        )
                        val blendedBottom = Color(
                            red = (rawBottom.red * 0.25f).coerceIn(0.02f, 0.18f),
                            green = (rawBottom.green * 0.25f).coerceIn(0.02f, 0.18f),
                            blue = (rawBottom.blue * 0.25f).coerceIn(0.02f, 0.18f),
                            alpha = 1f
                        )
                        
                        targetColorTop = blendedTop
                        targetColorBottom = blendedBottom
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            targetColorTop = Color(0xFF2C2C2C)
            targetColorBottom = Color(0xFF0F0F0F)
        }
    }

    val backgroundModifier = if (backgroundStyle == "gradient") {
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(animatedColorTop, animatedColorBottom)
                )
            )
    } else {
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    }

    val contentColor = Color.White
    val secondaryContentColor = Color.LightGray

    Surface(
        modifier = backgroundModifier,
        color = Color.Transparent
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
            ) {
            // Centered Premium Drag Handle Pill (clickable to collapse)
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .clickable { onClose() }
            )
            Spacer(Modifier.weight(1f))
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
                    text = song.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = secondaryContentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        song.artist?.let {
                            viewModel.searchAndNavigateToArtist(it)
                            onClose()
                        }
                    }
            )
            Spacer(Modifier.weight(1f))

            Slider(
                    value = if (duration > 0) playbackPosition.toFloat() else 0f,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        activeTrackColor = contentColor,
                        inactiveTrackColor = contentColor.copy(alpha = 0.24f),
                        thumbColor = contentColor
                    )
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                        formatTime(playbackPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryContentColor
                )
                Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryContentColor
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                val likeIcon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
                val likeIconTint = if (isLiked) MaterialTheme.colorScheme.primary else secondaryContentColor
                val playPauseIcon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                val playPauseDescription = if (isPlaying) "Pause" else "Play"
                val loopIconTint = if (isLooping) MaterialTheme.colorScheme.primary else secondaryContentColor

                IconButton(onClick = onLike) {
                    Icon(imageVector = likeIcon, contentDescription = "Like", tint = likeIconTint)
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(36.dp),
                            tint = contentColor
                    )
                }
                IconButton(
                        onClick = onPlayPause,
                        modifier =
                                Modifier.size(64.dp)
                                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                            imageVector = playPauseIcon,
                            contentDescription = playPauseDescription,
                            tint = Color(0xFF1C1B1F),
                            modifier = Modifier.size(42.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(36.dp),
                            tint = contentColor
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
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Show Queue")
                }
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
                val queue = viewModel.queue.value
                val history = viewModel.playbackHistory.value
                var draggedIndex by remember { mutableStateOf<Int?>(null) }
                var dragOffset by remember { mutableStateOf(0f) }
                val density = LocalDensity.current

                Text(
                        text = "Queue & History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                )

                if (queue.isEmpty() && history.isEmpty()) {
                    Text(
                            text = "No songs in queue or history",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 32.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        if (history.isNotEmpty()) {
                            item {
                                Text(
                                        text = "History",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            itemsIndexed(history) { index, historySong ->
                                SongRowItem(
                                        song = historySong,
                                        onSongClick = {
                                            viewModel.selectSong(historySong, clearQueue = false)
                                            showQueue = false
                                        },
                                        viewModel = viewModel
                                )
                            }
                        }

                        if (queue.isNotEmpty()) {
                            item {
                                Text(
                                        text = "Up Next",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            itemsIndexed(queue) { index, queuedSong ->
                                QueueSongRow(
                                        song = queuedSong,
                                        index = index,
                                        isDragged = draggedIndex == index,
                                        dragOffset = dragOffset,
                                        onSongClick = {
                                            // Jump to this song in the queue
                                            viewModel.playSong(queuedSong)
                                            // Remove this song and all previous songs from the queue
                                            viewModel.queue.value = queue.drop(index + 1)
                                            showQueue = false
                                        },
                                        viewModel = viewModel,
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffset = 0f
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { delta ->
                                            val currentDragged = draggedIndex
                                            if (currentDragged != null) {
                                                dragOffset += delta
                                                val itemHeightPx = with(density) { 72.dp.toPx() }
                                                
                                                if (dragOffset > itemHeightPx && currentDragged < queue.size - 1) {
                                                    viewModel.moveQueueSong(currentDragged, currentDragged + 1)
                                                    draggedIndex = currentDragged + 1
                                                    dragOffset -= itemHeightPx
                                                } else if (dragOffset < -itemHeightPx && currentDragged > 0) {
                                                    viewModel.moveQueueSong(currentDragged, currentDragged - 1)
                                                    draggedIndex = currentDragged - 1
                                                    dragOffset += itemHeightPx
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // 3-Dot Options Bottom Sheet
    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                // Song Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (song.thumbnailUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(song.thumbnailUrl),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                // Option: Add to playlist
                OptionItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    title = "Add to playlist",
                    subtitle = "Add this song to your playlists",
                    onClick = {
                        showMenu = false
                        showPlaylistDialog = true
                    }
                )

                // Option: Download / Caching
                val videoId = song.videoId
                val isDownloaded = viewModel.downloadedSongs.value.containsKey(videoId)
                val isDownloading = viewModel.activeDownloads.value.contains(videoId)

                if (isDownloading) {
                    OptionItem(
                        icon = Icons.Filled.Cancel,
                        title = "Stop downloading",
                        subtitle = "Cancel background downloading",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            viewModel.cancelDownload(videoId)
                            showMenu = false
                        }
                    )
                } else {
                    OptionItem(
                        icon = if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                        title = if (isDownloaded) "Remove download" else "Download song",
                        subtitle = if (isDownloaded) "Delete cached audio from device" else "Save track for offline playback",
                        tint = if (isDownloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            if (isDownloaded) {
                                viewModel.removeDownload(videoId)
                            } else {
                                viewModel.downloadSong(song)
                            }
                            showMenu = false
                        }
                    )
                }
            }
        }
    }

    // Add to Playlist bottom sheet
    if (showPlaylistDialog) {
        AddToPlaylistSheet(
                playlists = viewModel.playlists.value,
                onPlaylistSelected = { playlistName ->
                    onAddToPlaylist(playlistName)
                    showPlaylistDialog = false
                },
                onCreatePlaylist = {
                    showCreatePlaylistDialog = true
                },
                onDismiss = { showPlaylistDialog = false }
        )
    }

    // Create Playlist dialog (opened from the sheet)
    if (showCreatePlaylistDialog) {
        AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text("Create New Playlist") },
                text = {
                    OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = tint
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QueueSongRow(
    song: SongResult,
    index: Int,
    isDragged: Boolean,
    dragOffset: Float,
    onSongClick: () -> Unit,
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit
) {
    var showOptionsSheet by remember { mutableStateOf(false) }
    
    // Scale and elevation animation for high premium aesthetics
    val scale by animateFloatAsState(targetValue = if (isDragged) 1.05f else 1.0f, label = "scale")
    val elevation = if (isDragged) 8.dp else 0.dp
    
    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = if (isDragged) dragOffset else 0f
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isDragged) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                } else {
                    Color.Transparent
                }
            )
            .shadow(elevation, shape = RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onSongClick,
                onLongClick = { showOptionsSheet = true }
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail
            if (song.thumbnailUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = song.thumbnailUrl),
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
            
            // Details
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    song.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            if (song.duration != null) {
                Text(
                    song.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Reorder Handle matching visual cues of playlist detail screen
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
                    showOptionsSheet = false
                },
                onAddToQueue = {
                    viewModel.addToQueue(song)
                    showOptionsSheet = false
                },
                onLike = {
                    viewModel.toggleLike(song)
                    showOptionsSheet = false
                },
                onViewArtist = {
                    song.artist?.let { viewModel.searchAndNavigateToArtist(it) }
                    showOptionsSheet = false
                },
                onViewAlbum = {
                    song.album?.let { viewModel.searchAndNavigateToAlbum(it) }
                    showOptionsSheet = false
                },
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
                    showOptionsSheet = false
                }
        )
    }
}

