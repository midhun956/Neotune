package com.example.neotune.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.neotune.SearchViewModel
import com.example.neotune.SongOptionsSheet
import com.example.neotune.SongResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        viewModel: SearchViewModel,
        onSongClick: (SongResult) -> Unit,
        onSettingsClick: () -> Unit
) {
    val quickPicks = viewModel.quickPicksSongs.value
    val isLoadingQuickPicks = viewModel.isLoadingQuickPicks.value
    val recentlyPlayed = viewModel.recentlyPlayed.value
    val trending = viewModel.trendingSongs.value
    val isLoadingTrending = viewModel.isLoadingTrending.value

    val isRefreshing = isLoadingTrending || isLoadingQuickPicks

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            viewModel.loadTrending()
            viewModel.loadQuickPicks()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
        ) {
        // --- Greeting Header ---
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(
                                        Brush.verticalGradient(
                                                listOf(
                                                        MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.15f
                                                        ),
                                                        Color.Transparent
                                                )
                                        )
                                )
                                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                    "Neotune",
                    style =
                            MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                            ),
                    modifier = Modifier.align(Alignment.CenterStart)
            )
            IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ── 1. QUICK PICKS ──────────────────────────────────────────────
        SectionHeader(title = "Quick Picks")
        HomeCarousel(
                songs = quickPicks,
                isLoading = isLoadingQuickPicks,
                emptyMessage = "Start playing songs to get personalised picks",
                onSongClick = onSongClick,
                viewModel = viewModel
        )
        Spacer(Modifier.height(24.dp))

        // ── 2. KEEP LISTENING ───────────────────────────────────────────
        if (recentlyPlayed.isNotEmpty()) {
            SectionHeader(title = "Keep Listening")
            HomeCarousel(
                    songs = recentlyPlayed.take(20),
                    isLoading = false,
                    emptyMessage = "",
                    onSongClick = onSongClick,
                    viewModel = viewModel
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── 3. TRENDING NOW ─────────────────────────────────────────────
        SectionHeader(title = "Trending Now")
        HomeCarousel(
                songs = trending,
                isLoading = isLoadingTrending,
                emptyMessage = "Couldn't load trending — make sure backend is running",
                onSongClick = onSongClick,
                viewModel = viewModel
        )

        Spacer(Modifier.height(120.dp)) // room for mini player
    }
}
}

@Composable
private fun SectionHeader(title: String) {
    Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
    )
}

@Composable
private fun HomeCarousel(
        songs: List<SongResult>,
        isLoading: Boolean,
        emptyMessage: String,
        onSongClick: (SongResult) -> Unit,
        viewModel: SearchViewModel
) {
    when {
        isLoading -> {
            Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
        songs.isEmpty() -> {
            if (emptyMessage.isNotEmpty()) {
                Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            emptyMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
        else -> {
            LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(songs) { song ->
                    HomeCard(song = song, onClick = { onSongClick(song) }, viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeCard(song: SongResult, onClick: () -> Unit, viewModel: SearchViewModel) {
    var showOptionsSheet by remember { mutableStateOf(false) }
    Column(
            modifier = Modifier.width(150.dp)
                    .combinedClickable(
                            onClick = onClick,
                            onLongClick = { showOptionsSheet = true }
                    )
                    .padding(bottom = 4.dp)
    ) {
        Box(
                modifier =
                        Modifier.size(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (song.thumbnailUrl != null) {
                Image(
                        painter = rememberAsyncImagePainter(song.thumbnailUrl),
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
                song.title,
                style =
                        MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                        ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
        )
        if (song.artist != null) {
            Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
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
                isDownloaded = isDownloaded,
                isDownloading = isDownloading,
                onDownloadToggle = {
                    if (isDownloaded) {
                        viewModel.removeDownload(song.videoId)
                    } else {
                        viewModel.downloadSong(song)
                    }
                }
        )
    }
}
