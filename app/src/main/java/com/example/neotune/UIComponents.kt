package com.example.neotune

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun YouTubeMusicSeekBar(
        value: Float,
        onValueChange: (Float) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
        steps: Int = 0,
        onValueChangeFinished: (() -> Unit)? = null,
        bufferedProgress: Float = 0f,
        isBuffering: Boolean = false,
        duration: Long = 0L
) {
        var containerWidth by remember { mutableStateOf(0f) }

        val animatedValue by
                animateFloatAsState(
                        targetValue = value,
                        animationSpec = spring(), // A simple spring animation is fine
                        label = "seekBarValue"
                )

        val trackHeight = 4.dp
        val thumbSize = 16.dp

        Box(
                modifier =
                        modifier.height(32.dp)
                                .onSizeChanged { size -> containerWidth = size.width.toFloat() }
                                .pointerInput(Unit) {
                                        detectDragGestures(
                                                onDragStart = { offset ->
                                                        // This fires on first touch (for taps)
                                                        if (containerWidth > 0) {
                                                                val newValue =
                                                                        (offset.x / containerWidth)
                                                                                .coerceIn(0f, 1f)
                                                                onValueChange(newValue)
                                                        }
                                                },
                                                onDragEnd = {
                                                        // Fires when you lift your finger
                                                        onValueChangeFinished?.invoke()
                                                },
                                                onDrag = { change, dragAmount ->
                                                        // This fires as you move your finger
                                                        change.consume()
                                                        if (containerWidth > 0) {
                                                                // The key fix: Use the finger's
                                                                // absolute position
                                                                val newValue =
                                                                        (change.position.x /
                                                                                        containerWidth)
                                                                                .coerceIn(0f, 1f)
                                                                onValueChange(newValue)
                                                        }
                                                }
                                        )
                                }
                                .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                                if (containerWidth > 0) {
                                                        val newValue =
                                                                (offset.x / containerWidth)
                                                                        .coerceIn(0f, 1f)
                                                        onValueChange(newValue)
                                                        onValueChangeFinished?.invoke()
                                                }
                                        }
                                }
        ) {
                // Background track
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(trackHeight)
                                        .background(
                                                color = Color.Gray.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(trackHeight / 2)
                                        )
                                        .align(Alignment.CenterStart)
                )

                // Progress track
                Box(
                        modifier =
                                Modifier.fillMaxWidth(animatedValue)
                                        .height(trackHeight)
                                        .background(
                                                brush =
                                                        Brush.horizontalGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color(0xFF3B3BFF),
                                                                                Color(0xFF6B6BFF)
                                                                        )
                                                        ),
                                                shape = RoundedCornerShape(trackHeight / 2)
                                        )
                                        .align(Alignment.CenterStart)
                )

                // Thumb
                Box(
                        modifier =
                                Modifier.size(thumbSize)
                                        .offset {
                                                val thumbOffset =
                                                        (containerWidth * animatedValue -
                                                                        (thumbSize.toPx() / 2f))
                                                                .coerceIn(
                                                                        0f,
                                                                        containerWidth -
                                                                                thumbSize.toPx()
                                                                )
                                                IntOffset(thumbOffset.roundToInt(), 0)
                                        }
                                        .background(color = Color.White, shape = CircleShape)
                                        .shadow(
                                                elevation = 4.dp,
                                                shape = CircleShape,
                                                spotColor = Color(0xFF3B3BFF).copy(alpha = 0.3f)
                                        )
                                        .align(Alignment.CenterStart)
                ) {
                        // Inner progress indicator
                        Box(
                                modifier =
                                        Modifier.size(10.dp)
                                                .background(
                                                        color = Color(0xFF3B3BFF),
                                                        shape = CircleShape
                                                )
                                                .align(Alignment.Center)
                        )
                }
        }
}

@Composable
fun MiniPlayerSeekBar(value: Float, modifier: Modifier = Modifier) {
        val animatedValue by
                animateFloatAsState(
                        targetValue = value,
                        animationSpec = spring(), // A simple spring animation for smooth progress
                        label = "miniSeekBarValue"
                )

        val trackHeight = 2.dp

        Box(
                modifier =
                        modifier.height(trackHeight) // Removed the extra height for gestures
                                .fillMaxWidth() // The box should just fill the width
        ) {
                // Background track
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(trackHeight)
                                        .background(
                                                color = Color.Gray,
                                                shape = RoundedCornerShape(trackHeight / 2)
                                        )
                )

                // Progress track
                Box(
                        modifier =
                                Modifier.fillMaxWidth(animatedValue)
                                        .height(trackHeight)
                                        .background(
                                                color = Color(0xFF3B3BFF),
                                                shape = RoundedCornerShape(trackHeight / 2)
                                        )
                )
        }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(
        playlist: Playlist,
        songs: List<SongResult> = emptyList(),
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
) {
        Card(
                modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.82f)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                                onClick = onClick,
                                onLongClick = { onLongClick?.invoke() }
                        ),
                colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                ) {
                        // Top Cover Image / Grid
                        Box(
                                modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                                if (playlist.icon != null) {
                                        // Liked Songs or fallback icon card
                                        Box(
                                                modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                                Brush.verticalGradient(
                                                                        colors = listOf(
                                                                                MaterialTheme.colorScheme.primaryContainer,
                                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                                        )
                                                                )
                                                        ),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Icon(
                                                        imageVector = playlist.icon,
                                                        contentDescription = playlist.name,
                                                        tint = if (playlist.isLiked) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(44.dp)
                                                )
                                        }
                                } else {
                                        // 2x2 cover collage
                                        LibraryPlaylistCover(songs = songs)
                                }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Title
                        Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )

                        // Track count
                        Text(
                                text = "${playlist.songCount} song${if (playlist.songCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        )
                }
        }
}

@Composable
private fun LibraryPlaylistCover(songs: List<SongResult>) {
        Box(modifier = Modifier.fillMaxSize()) {
                if (songs.isEmpty()) {
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
        playlists: Map<String, List<SongResult>>,
        onPlaylistSelected: (String) -> Unit,
        onCreatePlaylist: () -> Unit,
        onDismiss: () -> Unit
) {
    ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // Header
        Text(
                text = "Add to playlist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)
        )

        // "New Playlist" creation row
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCreatePlaylist(); onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                    text = "New playlist",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (playlists.isNotEmpty()) {
            HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        }

        // Playlist list
        LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(playlists.keys.toList()) { playlistName ->
                val songs = playlists[playlistName] ?: emptyList()
                Row(
                        modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaylistSelected(playlistName); onDismiss() }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini album art collage
                    Box(
                            modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (songs.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                        imageVector = Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(24.dp)
                                )
                            }
                        } else if (songs.size < 4) {
                            val url = songs.first().thumbnailUrl
                            if (url != null) {
                                Image(
                                        painter = rememberAsyncImagePainter(url),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        Image(painter = rememberAsyncImagePainter(songs[0].thumbnailUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    }
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        Image(painter = rememberAsyncImagePainter(songs[1].thumbnailUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    }
                                }
                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        Image(painter = rememberAsyncImagePainter(songs[2].thumbnailUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    }
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        Image(painter = rememberAsyncImagePainter(songs[3].thumbnailUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = playlistName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                        Text(
                                text = "${songs.size} song${if (songs.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
        song: SongResult,
        isLiked: Boolean,
        onDismiss: () -> Unit,
        onPlayNext: () -> Unit,
        onAddToPlaylist: () -> Unit,
        onAddToQueue: () -> Unit,
        onLike: () -> Unit,
        onViewArtist: () -> Unit,
        onViewAlbum: () -> Unit,
        onRemoveFromPlaylist: (() -> Unit)? = null,
        isDownloaded: Boolean = false,
        isDownloading: Boolean = false,
        onDownloadToggle: () -> Unit = {}
) {
    ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // ── Song header row ──────────────────────────────────────────────────
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (song.thumbnailUrl != null) {
                    Image(
                            painter = rememberAsyncImagePainter(song.thumbnailUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.Center).size(28.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                if (song.artist != null) {
                    Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = { onLike(); onDismiss() }) {
                Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        // ── Quick action cards row ─────────────────────────────────────────
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                    icon = Icons.Filled.SkipNext,
                    label = "Play next",
                    modifier = Modifier.weight(1f),
                    onClick = { onPlayNext(); onDismiss() }
            )
            QuickActionCard(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    label = "Add to playlist",
                    modifier = Modifier.weight(1f),
                    onClick = { onAddToPlaylist(); onDismiss() }
            )
            QuickActionCard(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = "Add to queue",
                    modifier = Modifier.weight(1f),
                    onClick = { onAddToQueue(); onDismiss() }
            )
        }

        HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        // ── List option rows ───────────────────────────────────────────────
        SongOptionRow(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = "Add to queue",
                subtitle = "Add to the end of the queue",
                onClick = { onAddToQueue(); onDismiss() }
        )
        SongOptionRow(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                title = if (isLiked) "Remove from liked" else "Save to liked songs",
                subtitle = if (isLiked) "Remove this song from your liked songs" else "Add this song to your liked songs",
                tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface,
                onClick = { onLike(); onDismiss() }
        )
        if (isDownloading) {
            SongOptionRow(
                    icon = Icons.Filled.Cancel,
                    title = "Stop downloading",
                    subtitle = "Cancel background downloading",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { onDownloadToggle(); onDismiss() }
            )
        } else {
            SongOptionRow(
                    icon = if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                    title = if (isDownloaded) "Remove download" else "Download song",
                    subtitle = if (isDownloaded) "Delete cached audio from device" else "Save track for offline playback",
                    tint = if (isDownloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    onClick = { onDownloadToggle(); onDismiss() }
            )
        }
        if (!song.artist.isNullOrBlank()) {
            SongOptionRow(
                    icon = Icons.Filled.Person,
                    title = "View artist",
                    subtitle = song.artist,
                    onClick = { onViewArtist(); onDismiss() }
            )
        }
        if (!song.album.isNullOrBlank()) {
            SongOptionRow(
                    icon = Icons.Filled.Album,
                    title = "View album",
                    subtitle = song.album,
                    onClick = { onViewAlbum(); onDismiss() }
            )
        }
        onRemoveFromPlaylist?.let { removeCallback ->
            SongOptionRow(
                    icon = Icons.Filled.Delete,
                    title = "Remove from playlist",
                    subtitle = "Remove this song from the playlist",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { removeCallback(); onDismiss() }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuickActionCard(
        icon: ImageVector,
        label: String,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
) {
    Surface(
            modifier = modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onClick() },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(14.dp)
    ) {
        Column(
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SongOptionRow(
        icon: ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit,
        tint: Color = Color.Unspecified
) {
    val iconColor = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
    Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(18.dp))
        Column {
            Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = iconColor
            )
            Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }
    }
}
