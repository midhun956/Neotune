package com.example.neotune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.neotune.Playlist
import com.example.neotune.PlaylistCard
import com.example.neotune.SongResult

@Composable
fun LibraryScreen(
        likedSongsCount: Int,
        playlists: Map<String, List<SongResult>>,
        playlistSongCounts: Map<String, Int>,
        onCreatePlaylist: (String) -> Unit,
        onPlaylistClick: (String) -> Unit,
        onPlaylistLongClick: (String) -> Unit,
        onLikedSongsClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                        onClick = { showDialog = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) { Icon(Icons.Filled.Add, contentDescription = "New Playlist") }
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { paddingValues ->
        Column(
                Modifier.fillMaxSize()
                        .padding(paddingValues)
                        .padding(WindowInsets.systemBars.asPaddingValues())
                        .padding(16.dp)
        ) {
            Text("Library", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dedicated Liked Songs card
                item {
                    PlaylistCard(
                            playlist =
                                    Playlist(
                                            "Liked Songs",
                                            icon = Icons.Filled.Favorite,
                                            isLiked = true,
                                            songCount = likedSongsCount
                                    ),
                            onClick = { onLikedSongsClick() }
                    )
                }

                // User playlists
                items(playlists.keys.toList()) { playlistName ->
                    PlaylistCard(
                            playlist =
                                    Playlist(
                                            name = playlistName,
                                            songCount = playlistSongCounts[playlistName] ?: 0
                                    ),
                            onClick = { onPlaylistClick(playlistName) },
                            onLongClick = { onPlaylistLongClick(playlistName) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
                onDismissRequest = { showDialog = false },
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
                                    showDialog = false
                                    newPlaylistName = ""
                                }
                            }
                    ) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}
