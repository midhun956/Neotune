package com.example.neotune.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.neotune.AlbumTrack
import com.example.neotune.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(viewModel: SearchViewModel, onBack: () -> Unit) {
    if (viewModel.isLoadingAlbumDetails.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val details = viewModel.albumDetails.value

    if (details == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Failed to load album details", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text(details.title, maxLines = 1) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                )
                )
            }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val highResUrl =
                            details.thumbnailUrl?.replace(Regex("=w\\d+-h\\d+-"), "=w1080-h1080-")

                    if (!highResUrl.isNullOrEmpty()) {
                        Image(
                                painter = rememberAsyncImagePainter(highResUrl),
                                contentDescription = details.title,
                                modifier = Modifier.size(250.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                                modifier =
                                        Modifier.size(250.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                contentAlignment = Alignment.Center
                        ) { Text("No Cover Art", style = MaterialTheme.typography.bodyLarge) }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                            text = details.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            text = "${details.artist ?: "Unknown Artist"} • ${details.year ?: ""}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            itemsIndexed(details.tracks) { index, track ->
                AlbumTrackRowItem(track = track, albumImageUrl = details.thumbnailUrl) { _ ->
                    viewModel.playAlbumSong(details, index)
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Miniplayer padding
            }
        }
    }
}

@Composable
fun AlbumTrackRowItem(track: AlbumTrack, albumImageUrl: String?, onClick: (AlbumTrack) -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable { onClick(track) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        if (!albumImageUrl.isNullOrEmpty()) {
            Image(
                    painter = rememberAsyncImagePainter(albumImageUrl),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
            )
            if (!track.artists.isNullOrBlank()) {
                Text(
                        text = track.artists,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                )
            }
        }

        if (track.duration != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                    text = track.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
            )
        }
    }
}
