package com.example.neotune.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.neotune.AlbumResult
import com.example.neotune.SearchViewModel
import com.example.neotune.SongResult
import com.example.neotune.ui.components.HorizontalAlbumItem
import com.example.neotune.ui.components.HorizontalSongItem

@Composable
fun ArtistDetailScreen(
        viewModel: SearchViewModel,
        onBack: () -> Unit,
        onSongClick: (SongResult) -> Unit,
        onAlbumClick: (AlbumResult) -> Unit
) {
        val artistDetails by viewModel.artistDetails
        val isLoading by viewModel.isLoadingArtistDetails

        LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
                // Header Section with Image and Gradient
                item {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                                if (artistDetails?.thumbnailUrl != null) {
                                        Image(
                                                painter =
                                                        rememberAsyncImagePainter(
                                                                model = artistDetails?.thumbnailUrl
                                                        ),
                                                contentDescription = artistDetails?.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                        )
                                }
                                // Gradient overlay for text readability
                                Box(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .background(
                                                                Brush.verticalGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        Color.Transparent,
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .background
                                                                                ),
                                                                        startY = 400f
                                                                )
                                                        )
                                )
                                // Back button
                                IconButton(
                                        onClick = onBack,
                                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                ) {
                                        Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = Color.White
                                        )
                                }
                                // Artist Name
                                Text(
                                        text = artistDetails?.name ?: "Loading...",
                                        style =
                                                MaterialTheme.typography.headlineLarge.copy(
                                                        fontWeight = FontWeight.Bold
                                                ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier =
                                                Modifier.align(Alignment.BottomStart).padding(16.dp)
                                )
                        }
                }

                // Top Songs Section
                if (isLoading) {
                        item {
                                Box(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator() }
                        }
                } else if (artistDetails?.topSongs?.isNotEmpty() == true) {
                        item {
                                Text(
                                        "Top Songs",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier =
                                                Modifier.padding(
                                                        start = 16.dp,
                                                        top = 16.dp,
                                                        end = 16.dp,
                                                        bottom = 8.dp
                                                )
                                )
                        }
                        item {
                                LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                        items(artistDetails?.topSongs ?: emptyList()) { song ->
                                                HorizontalSongItem(
                                                        song = song,
                                                        onSongClick = onSongClick,
                                                        viewModel = viewModel
                                                )
                                        }
                                }
                        }
                }

                // Albums Section
                if (!isLoading && artistDetails?.albums?.isNotEmpty() == true) {
                        item {
                                Text(
                                        "Albums",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier =
                                                Modifier.padding(
                                                        start = 16.dp,
                                                        top = 16.dp,
                                                        end = 16.dp,
                                                        bottom = 8.dp
                                                )
                                )
                        }
                        item {
                                LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                        items(artistDetails?.albums ?: emptyList()) { album ->
                                                HorizontalAlbumItem(
                                                        album = album,
                                                        onAlbumClick = onAlbumClick
                                                )
                                        }
                                }
                        }
                }

                // Description Section (Moved to Bottom)
                if (!artistDetails?.description.isNullOrBlank()) {
                        item {
                                HorizontalDivider(
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 16.dp
                                                )
                                )
                                Text(
                                        text = "About",
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier =
                                                Modifier.padding(
                                                        start = 16.dp,
                                                        end = 16.dp,
                                                        bottom = 8.dp
                                                )
                                )
                                Text(
                                        text = artistDetails?.description ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier =
                                                Modifier.padding(
                                                        start = 16.dp,
                                                        end = 16.dp,
                                                        bottom = 32.dp
                                                )
                                )
                        }
                }
        }
}
