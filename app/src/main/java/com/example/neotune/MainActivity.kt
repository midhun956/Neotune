package com.example.neotune

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.neotune.ui.components.AnimatedMiniPlayer
import com.example.neotune.ui.components.ExpandedPlayerSheet
import com.example.neotune.ui.screens.AlbumDetailScreen
import com.example.neotune.ui.screens.ArtistDetailScreen
import com.example.neotune.ui.screens.HomeScreen
import com.example.neotune.ui.screens.SettingsScreen
import com.example.neotune.ui.screens.LibraryScreen
import com.example.neotune.ui.screens.LikedSongsScreen
import com.example.neotune.ui.screens.PlaylistDetailScreen
import com.example.neotune.ui.screens.YouTubeMusicSearchScreen
import com.example.neotune.ui.theme.NeotuneTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        setContent {
            val viewModel = viewModel<SearchViewModel>()
            val themeStyle = viewModel.appThemeStyle.value
            val amoledAccent = viewModel.amoledAccentColor.value
            NeotuneTheme(themeStyle = themeStyle, amoledAccent = amoledAccent) {
                MainScreen(controllerFuture, viewModel)
            }
        }
    }

    override fun onDestroy() {
        MediaController.releaseFuture(controllerFuture)
        super.onDestroy()
    }
}

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(controllerFuture: ListenableFuture<MediaController>, viewModel: SearchViewModel) {

    // Connect MediaController → ViewModel once it's ready
    var controller by remember { mutableStateOf<MediaController?>(null) }
    LaunchedEffect(controllerFuture) {
        controllerFuture.addListener(
                {
                    val mc = controllerFuture.get()
                    controller = mc
                    viewModel.exoPlayer = mc
                },
                MoreExecutors.directExecutor()
        )
    }

    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.example.neotune.ACTION_NEXT" -> viewModel.playNext()
                    "com.example.neotune.ACTION_PREVIOUS" -> viewModel.playPrevious()
                    "com.example.neotune.ACTION_LIKE" -> {
                        viewModel.selectedSong.value?.let {
                            viewModel.toggleLike(it)
                        }
                    }
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction("com.example.neotune.ACTION_NEXT")
            addAction("com.example.neotune.ACTION_PREVIOUS")
            addAction("com.example.neotune.ACTION_LIKE")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    var showSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showLikedSongs by remember { mutableStateOf(false) }
    var showPlaylistDetail by remember { mutableStateOf(false) }
    var selectedPlaylistName by remember { mutableStateOf<String?>(null) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<String?>(null) }
    var showArtistDetail by remember { mutableStateOf(false) }
    var showAlbumDetail by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val playbackPosition = remember { mutableLongStateOf(0L) }
    val duration = remember { mutableLongStateOf(0L) }
    val isPlaying = remember { mutableStateOf(false) }

    var showMiniPlaylistDialog by remember { mutableStateOf(false) }
    var showMiniCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newMiniPlaylistName by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val horizontalOffset = remember { Animatable(0f) }

    var parentHeightPx by remember { mutableStateOf(0f) }
    var isSheetActive by remember { mutableStateOf(false) }
    val sheetOffset = remember { Animatable(10000f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(showSheet) {
        if (showSheet) {
            isSheetActive = true
        }
    }

    LaunchedEffect(showSheet, parentHeightPx) {
        if (parentHeightPx > 0f) {
            if (showSheet) {
                if (sheetOffset.value > parentHeightPx) {
                    sheetOffset.snapTo(parentHeightPx)
                }
                sheetOffset.animateTo(
                        targetValue = 0f,
                        animationSpec =
                                spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                )
                )
            } else {
                sheetOffset.animateTo(
                        targetValue = parentHeightPx,
                        animationSpec =
                                spring(
                                        dampingRatio = 1.0f,
                                        stiffness = Spring.StiffnessMediumLow
                                )
                )
                isSheetActive = false
            }
        }
    }

    // Back button priority chain
    BackHandler(enabled = showSheet) { showSheet = false }
    BackHandler(enabled = !showSheet && showSettings) { showSettings = false }
    BackHandler(enabled = !showSheet && showAlbumDetail) { showAlbumDetail = false }
    BackHandler(enabled = !showSheet && !showAlbumDetail && showArtistDetail) {
        showArtistDetail = false
    }
    BackHandler(
            enabled = !showSheet && !showAlbumDetail && !showArtistDetail && showPlaylistDetail
    ) {
        showPlaylistDetail = false
        selectedPlaylistName = null
    }
    BackHandler(
            enabled =
                    !showSheet &&
                            !showAlbumDetail &&
                            !showArtistDetail &&
                            !showPlaylistDetail &&
                            showLikedSongs
    ) { showLikedSongs = false }
    BackHandler(
            enabled =
                    !showSheet &&
                            !showAlbumDetail &&
                            !showArtistDetail &&
                            !showPlaylistDetail &&
                            !showLikedSongs &&
                            selectedTab != 0
    ) { selectedTab = 0 }

    // Poll playback state
    LaunchedEffect(controller) {
        while (true) {
            delay(200)
            val c = controller ?: continue
            isPlaying.value = c.isPlaying
            if (c.isPlaying || c.playbackState == Player.STATE_READY) {
                playbackPosition.longValue = c.currentPosition
                duration.longValue = c.duration.coerceAtLeast(0L)
            }
        }
    }

    // Auto-advance to next on song end
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.playNext()
                }
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    Box(
            modifier =
                    Modifier.fillMaxSize().onSizeChanged { size ->
                        parentHeightPx = size.height.toFloat()
                    }
    ) {
        Scaffold(
                bottomBar = {
                    if (!showSettings) {
                        Column {
                            val coroutineScope = rememberCoroutineScope()
                        AnimatedVisibility(
                                visible = viewModel.selectedSong.value != null,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                        ) {
                            Box(
                                    modifier =
                                            Modifier.offset { IntOffset(horizontalOffset.value.roundToInt(), 0) }
                                                    .draggable(
                                                    orientation = Orientation.Vertical,
                                                    state =
                                                            rememberDraggableState { delta ->
                                                                coroutineScope.launch {
                                                                    val newOffset =
                                                                            (sheetOffset.value + delta)
                                                                                    .coerceIn(
                                                                                            0f,
                                                                                            parentHeightPx
                                                                                    )
                                                                    sheetOffset.snapTo(newOffset)
                                                                }
                                                            },
                                                    onDragStarted = {
                                                        isDragging = true
                                                        isSheetActive = true
                                                        coroutineScope.launch {
                                                            sheetOffset.snapTo(parentHeightPx)
                                                        }
                                                    },
                                                    onDragStopped = { velocity ->
                                                        isDragging = false
                                                        coroutineScope.launch {
                                                            if (sheetOffset.value <
                                                                            parentHeightPx - 50f ||
                                                                            velocity < -500f
                                                            ) {
                                                                showSheet = true
                                                            } else {
                                                                showSheet = false
                                                                sheetOffset.animateTo(parentHeightPx)
                                                                isSheetActive = false
                                                            }
                                                        }
                                                    }
                                            ).draggable(
                                                    orientation = Orientation.Horizontal,
                                                    state =
                                                            rememberDraggableState { delta ->
                                                                coroutineScope.launch {
                                                                    horizontalOffset.snapTo(horizontalOffset.value + delta)
                                                                }
                                                            },
                                                    onDragStarted = {
                                                        coroutineScope.launch {
                                                            horizontalOffset.snapTo(0f)
                                                        }
                                                    },
                                                    onDragStopped = { velocity ->
                                                        coroutineScope.launch {
                                                            if (horizontalOffset.value < -150f ||
                                                                            velocity < -500f
                                                            ) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                viewModel.playNext()
                                                            } else if (horizontalOffset.value > 150f ||
                                                                            velocity > 500f
                                                            ) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                viewModel.playPrevious()
                                                            }
                                                            horizontalOffset.animateTo(
                                                                    targetValue = 0f,
                                                                    animationSpec =
                                                                            spring(
                                                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                                    stiffness = Spring.StiffnessMediumLow
                                                                            )
                                                            )
                                                        }
                                                    }
                                            )
                            ) {
                                AnimatedMiniPlayer(
                                        song = viewModel.selectedSong.value!!,
                                        isPlaying = isPlaying.value,
                                        onPlayPause = {
                                            if (isPlaying.value) controller?.pause()
                                            else controller?.play()
                                        },
                                        onClick = { showSheet = true },
                                        isLiked = viewModel.isLiked(viewModel.selectedSong.value!!),
                                        onLike = { viewModel.toggleLike(viewModel.selectedSong.value!!) },
                                        onAddToPlaylist = { showMiniPlaylistDialog = true },
                                        onBoundsChange = {},
                                        playbackPosition = playbackPosition.longValue,
                                        duration = duration.longValue
                                )
                            }
                        }
                        if (!showArtistDetail && !showAlbumDetail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.background
                            ) {
                                NavigationBarItem(
                                        icon = {
                                            Icon(Icons.Filled.Home, contentDescription = "Home")
                                        },
                                        label = { Text("Home") },
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 }
                                )
                                NavigationBarItem(
                                        icon = {
                                            Icon(
                                                    Icons.Filled.Search,
                                                    contentDescription = "Search"
                                            )
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
                }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Album Detail
            AnimatedVisibility(
                    visible = showAlbumDetail,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
            ) { AlbumDetailScreen(viewModel = viewModel, onBack = { showAlbumDetail = false }) }

            // Artist Detail
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

            // Main tab content
            AnimatedVisibility(
                    visible = !showAlbumDetail && !showArtistDetail,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 ->
                            HomeScreen(
                                    viewModel = viewModel,
                                    onSongClick = { song ->
                                        viewModel.selectSong(song)
                                        showSheet = true
                                    },
                                    onSettingsClick = { showSettings = true }
                            )
                    1 ->
                            YouTubeMusicSearchScreen(
                                    searchResults = viewModel.searchResults.value,
                                    isLoading = viewModel.isLoading.value,
                                    error = viewModel.error.value,
                                    onSongClick = { song ->
                                        viewModel.selectSong(song)
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
                        when {
                            showPlaylistDetail && selectedPlaylistName != null -> {
                                val songs =
                                        viewModel.playlists.value[selectedPlaylistName]
                                                ?: emptyList()
                                PlaylistDetailScreen(
                                        playlistName = selectedPlaylistName!!,
                                        songs = songs,
                                        onBack = {
                                            showPlaylistDetail = false
                                            selectedPlaylistName = null
                                        },
                                        onSongClick = { song ->
                                            viewModel.selectSong(song)
                                            showSheet = true
                                        },
                                        onRemoveSong = { song ->
                                            viewModel.removeSongFromPlaylist(
                                                    selectedPlaylistName!!,
                                                    song
                                            )
                                        },
                                        viewModel = viewModel
                                )
                            }
                            showLikedSongs -> {
                                LikedSongsScreen(
                                        likedSongs = viewModel.likedSongs.value,
                                        onBack = { showLikedSongs = false },
                                        onSongClick = { song ->
                                            viewModel.selectSong(song)
                                            showSheet = true
                                        }
                                )
                            }
                            else -> {
                                LibraryScreen(
                                        likedSongsCount = viewModel.likedSongs.value.size,
                                        playlists = viewModel.playlists.value,
                                        playlistSongCounts = viewModel.playlistSongCounts.value,
                                        onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                                        onPlaylistClick = { playlistName ->
                                            selectedPlaylistName = playlistName
                                            showPlaylistDetail = true
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
            }
        }

        // Delete Playlist Dialog
        if (showDeletePlaylistDialog && playlistToDelete != null) {
            AlertDialog(
                    onDismissRequest = { showDeletePlaylistDialog = false },
                    title = { Text("Delete Playlist") },
                    text = { Text("Are you sure you want to delete '$playlistToDelete'?") },
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

        // Mini Playlist Selection Dialog
        if (showMiniPlaylistDialog && viewModel.selectedSong.value != null) {
            AlertDialog(
                    onDismissRequest = { showMiniPlaylistDialog = false },
                    title = { Text("Add to playlist") },
                    text = {
                        Column {
                            TextButton(
                                    onClick = {
                                        showMiniPlaylistDialog = false
                                        showMiniCreatePlaylistDialog = true
                                    }
                            ) { Text("Create new playlist") }
                            viewModel.playlists.value.keys.forEach { playlistName ->
                                TextButton(
                                        onClick = {
                                            viewModel.addSongToPlaylist(
                                                    playlistName,
                                                    viewModel.selectedSong.value!!
                                            )
                                            showMiniPlaylistDialog = false
                                        }
                                ) { Text(playlistName) }
                            }
                        }
                    },
                    confirmButton = {}
            )
        }

        if (showMiniCreatePlaylistDialog) {
            AlertDialog(
                    onDismissRequest = { showMiniCreatePlaylistDialog = false },
                    title = { Text("Create New Playlist") },
                    text = {
                        OutlinedTextField(
                                value = newMiniPlaylistName,
                                onValueChange = { newMiniPlaylistName = it },
                                label = { Text("Playlist Name") }
                        )
                    },
                    confirmButton = {
                        TextButton(
                                onClick = {
                                    if (newMiniPlaylistName.isNotBlank()) {
                                        viewModel.createPlaylist(newMiniPlaylistName)
                                        showMiniCreatePlaylistDialog = false
                                        newMiniPlaylistName = ""
                                    }
                                }
                        ) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMiniCreatePlaylistDialog = false }) { Text("Cancel") }
                    }
            )
        }

        // Settings Screen Overlay
        AnimatedVisibility(
                visible = showSettings,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
        ) {
            SettingsScreen(
                    viewModel = viewModel,
                    onBack = { showSettings = false }
            )
        }
    }

    // Expanded Player Sheet
    if (isSheetActive && viewModel.selectedSong.value != null) {
        val scrimAlpha = (1f - (sheetOffset.value / parentHeightPx.coerceAtLeast(1f))).coerceIn(0f, 1f) * 0.6f
        val coroutineScope = rememberCoroutineScope()

        // Scrim background
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(Color.Black.copy(alpha = scrimAlpha))
                                .pointerInput(Unit) {
                                    detectTapGestures { showSheet = false }
                                }
        )

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .offset { IntOffset(0, sheetOffset.value.roundToInt()) }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                            onDragStart = { isDragging = true },
                                            onDragEnd = {
                                                isDragging = false
                                                coroutineScope.launch {
                                                    if (sheetOffset.value > 150f) {
                                                        showSheet = false
                                                    } else {
                                                        sheetOffset.animateTo(
                                                                targetValue = 0f,
                                                                animationSpec =
                                                                        spring(
                                                                                dampingRatio =
                                                                                        0.85f,
                                                                                stiffness =
                                                                                        Spring
                                                                                                .StiffnessMediumLow
                                                                        )
                                                        )
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                                coroutineScope.launch {
                                                    sheetOffset.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec =
                                                                    spring(
                                                                            dampingRatio =
                                                                                    0.85f,
                                                                            stiffness =
                                                                                    Spring
                                                                                            .StiffnessMediumLow
                                                                    )
                                                    )
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newOffset =
                                                        (sheetOffset.value + dragAmount.y)
                                                                .coerceAtLeast(0f)
                                                coroutineScope.launch {
                                                    sheetOffset.snapTo(newOffset)
                                                }
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
                            if (isPlaying.value) controller?.pause()
                            else controller?.play()
                        },
                        onSeek = { position: Long -> controller?.seekTo(position) },
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
                        onCreatePlaylist = { name: String -> viewModel.createPlaylist(name) },
                        viewModel = viewModel
                )
            }
        }
    }
}
}
