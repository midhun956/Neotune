package com.example.neotune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.neotune.ui.components.AnimatedMiniPlayer
import com.example.neotune.ui.components.ExpandedPlayerSheet
import com.example.neotune.ui.screens.AlbumDetailScreen
import com.example.neotune.ui.screens.ArtistDetailScreen
import com.example.neotune.ui.screens.HomeScreen
import com.example.neotune.ui.screens.LibraryScreen
import com.example.neotune.ui.screens.LikedSongsScreen
import com.example.neotune.ui.screens.YouTubeMusicSearchScreen
import com.example.neotune.ui.theme.NeotuneTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NeotuneTheme { MainScreen() } }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel = viewModel<SearchViewModel>()
    var showSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showLikedSongs by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<String?>(null) }
    var showArtistDetail by remember { mutableStateOf(false) }
    var showAlbumDetail by remember { mutableStateOf(false) }

    val playbackPosition = remember { mutableLongStateOf(0L) }
    val duration = remember { mutableLongStateOf(0L) }
    val isPlaying = remember { mutableStateOf(false) }

    // Hardware back button routing
    BackHandler(enabled = showSheet) { showSheet = false }
    BackHandler(enabled = !showSheet && showAlbumDetail) { showAlbumDetail = false }
    BackHandler(enabled = !showSheet && !showAlbumDetail && showArtistDetail) {
        showArtistDetail = false
    }
    BackHandler(enabled = !showSheet && !showAlbumDetail && !showArtistDetail && showLikedSongs) {
        showLikedSongs = false
    }
    BackHandler(
            enabled =
                    !showSheet &&
                            !showAlbumDetail &&
                            !showArtistDetail &&
                            !showLikedSongs &&
                            selectedTab != 0
    ) { selectedTab = 0 }

    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    viewModel.exoPlayer = exoPlayer

    var isTransitioning by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.streamUrl.value) {
        viewModel.streamUrl.value?.let { url ->
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            isPlaying.value = exoPlayer.isPlaying
            if (isPlaying.value) {
                playbackPosition.longValue = exoPlayer.currentPosition
                duration.longValue = exoPlayer.duration
            }
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    DisposableEffect(exoPlayer) {
        val listener =
                object : Player.Listener {
                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            viewModel.playNext()
                        }
                    }
                }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    Scaffold(
            bottomBar = {
                Column {
                    AnimatedVisibility(
                            visible = !showSheet && viewModel.selectedSong.value != null,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        AnimatedMiniPlayer(
                                song = viewModel.selectedSong.value!!,
                                isPlaying = isPlaying.value,
                                onPlayPause = {
                                    if (isPlaying.value) exoPlayer.pause() else exoPlayer.play()
                                },
                                onClick = {
                                    isTransitioning = true
                                    showSheet = true
                                },
                                onNext = { viewModel.playNext() },
                                onBoundsChange = { /* Unused for now */},
                                playbackPosition = playbackPosition.longValue,
                                duration = duration.longValue
                        )
                    }
                    if (!showSheet && !showArtistDetail && !showAlbumDetail) {
                        NavigationBar {
                            NavigationBarItem(
                                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 }
                            )
                            NavigationBarItem(
                                    icon = {
                                        Icon(Icons.Filled.Search, contentDescription = "Search")
                                    },
                                    label = { Text("Search") },
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 }
                            )
                            NavigationBarItem(
                                    icon = {
                                        Icon(
                                                Icons.Filled.LibraryMusic,
                                                contentDescription = "Library"
                                        )
                                    },
                                    label = { Text("Library") },
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 }
                            )
                        }
                    }
                }
            }
    )       { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedVisibility(
                    visible = showAlbumDetail,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
            ) { AlbumDetailScreen(viewModel = viewModel, onBack = { showAlbumDetail = false }) }

            AnimatedVisibility(
                    visible = showArtistDetail && !showAlbumDetail,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
            ) {
                ArtistDetailScreen(
                        viewModel = viewModel,
                        onBack = { showArtistDetail = false },
                        onSongClick = { song ->
                            viewModel.selectSong(song)
                            isTransitioning = true
                            showSheet = true
                        },
                        onAlbumClick = { album ->
                            album.browseId?.let {
                                viewModel.loadAlbumDetails(it)
                                showAlbumDetail = true
                            }
                        }
                )
            }

            AnimatedVisibility(
                    visible = !showAlbumDetail && !showArtistDetail,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> HomeScreen()
                    1 ->
                            YouTubeMusicSearchScreen(
                                    searchResults = viewModel.searchResults.value,
                                    isLoading = viewModel.isLoading.value,
                                    error = viewModel.error.value,
                                    onSongClick = { song ->
                                        viewModel.selectSong(song)
                                        isTransitioning = true
                                        showSheet = true
                                    },
                                    onArtistClick = { artist ->
                                        artist.browseId?.let {
                                            viewModel.loadArtistDetails(it)
                                            showArtistDetail = true
                                        }
                                    },
                                    onAlbumClick = { album ->
                                        album.browseId?.let {
                                            viewModel.loadAlbumDetails(it)
                                            showAlbumDetail = true
                                        }
                                    },
                                    viewModel = viewModel
                            )
                    2 -> {
                        if (showLikedSongs) {
                            LikedSongsScreen(
                                    likedSongs = viewModel.likedSongs.value,
                                    onBack = { showLikedSongs = false },
                                    onSongClick = { song ->
                                        viewModel.selectSong(song)
                                        isTransitioning = true
                                        showSheet = true
                                    }
                            )
                        } else {
                            LibraryScreen(
                                    likedSongsCount = viewModel.likedSongs.value.size,
                                    playlists = viewModel.playlists.value,
                                    playlistSongCounts = viewModel.playlistSongCounts.value,
                                    onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                                    onPlaylistClick = { playlistName ->
                                        // TODO: Add screen / state for navigating into custom
                                        // playlists
                                    },
                                    onPlaylistLongClick = { playlistName ->
                                        playlistToDelete = playlistName
                                        showDeletePlaylistDialog = true
                                    },
                                    onLikedSongsClick = { showLikedSongs = true }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                    visible = showSheet && viewModel.selectedSong.value != null,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
            ) {
                var dragOffset by remember { mutableStateOf(0f) }
                var isDragging by remember { mutableStateOf(false) }
                val animatedOffset by
                        animateFloatAsState(
                                targetValue = if (isDragging) dragOffset else 0f,
                                animationSpec =
                                        if (isDragging) tween(0)
                                        else spring(stiffness = Spring.StiffnessLow),
                                label = "dragOffset"
                        )

                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .offset { IntOffset(0, animatedOffset.roundToInt()) }
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                    onDragStart = { isDragging = true },
                                                    onDragEnd = {
                                                        isDragging = false
                                                        if (dragOffset > 400f) showSheet = false
                                                        dragOffset = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffset =
                                                                (dragOffset + dragAmount.y)
                                                                        .coerceAtLeast(0f)
                                                    }
                                            )
                                        }
                ) {
                    key(viewModel.selectedSong.value!!.videoId) {
                        ExpandedPlayerSheet(
                                song = viewModel.selectedSong.value!!,
                                isPlaying = isPlaying.value,
                                playbackPosition = playbackPosition.longValue,
                                duration = duration.longValue,
                                onPlayPause = {
                                    if (isPlaying.value) exoPlayer.pause() else exoPlayer.play()
                                },
                                onSeek = { position: Long -> exoPlayer.seekTo(position) },
                                onClose = { showSheet = false },
                                onNext = { viewModel.playNext() },
                                onPrevious = { viewModel.playPrevious() },
                                isLiked = viewModel.isLiked(viewModel.selectedSong.value!!),
                                onLike = { viewModel.toggleLike(viewModel.selectedSong.value!!) },
                                isLooping = viewModel.isLooping.value,
                                onToggleLoop = { viewModel.toggleLoop() },
                                onAddToPlaylist = { playlistName: String ->
                                    viewModel.addSongToPlaylist(
                                            playlistName,
                                            viewModel.selectedSong.value!!
                                    )
                                },
                                onCreatePlaylist = { name: String ->
                                    viewModel.createPlaylist(name)
                                },
                                viewModel = viewModel
                        )
                    }
                }

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
                                                viewModel.createPlaylist(newPlaylistName)
                                                showCreatePlaylistDialog = false
                                                newPlaylistName = ""
                                            }
                                        }
                                ) { Text("Create") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                    )
                }

                if (showDeletePlaylistDialog && playlistToDelete != null) {
                    AlertDialog(
                            onDismissRequest = { showDeletePlaylistDialog = false },
                            title = { Text("Delete Playlist") },
                            text = {
                                Text("Are you sure you want to delete '${playlistToDelete}'?")
                            },
                            confirmButton = {
                                TextButton(
                                        onClick = {
                                            viewModel.deletePlaylist(playlistToDelete!!)
                                            showDeletePlaylistDialog = false
                                            playlistToDelete = null
                                        }
                                ) { Text("Delete") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeletePlaylistDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                    )
                }
            }
        }
    }
}
