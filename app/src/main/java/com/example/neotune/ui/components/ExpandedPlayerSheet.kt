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
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close")
            }
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
                    song.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = secondaryContentColor,
                    textAlign = TextAlign.Center
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
