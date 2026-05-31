package com.example.neotune.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neotune.Playlist
import com.example.neotune.PlaylistCard
import com.example.neotune.SearchViewModel
import com.example.neotune.SongResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
        onPlaylistClick: (String) -> Unit,
        onLikedSongsClick: () -> Unit,
        onDownloadsClick: () -> Unit,
        viewModel: SearchViewModel
) {
    val likedSongsCount = viewModel.likedSongs.value.size
    val playlists = viewModel.playlists.value
    val playlistSongCounts = viewModel.playlistSongCounts.value

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var newPlaylistName by remember { mutableStateOf("") }
    var activePlaylistForSheet by remember { mutableStateOf<String?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }

    Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) { Icon(Icons.Filled.Add, contentDescription = "New Playlist") }
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { paddingValues ->
        Column(
                Modifier.fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                    "Library",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(24.dp))

            LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
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
                            songs = viewModel.likedSongs.value,
                            onClick = { onLikedSongsClick() }
                    )
                }

                // Dedicated Downloads card
                item {
                    PlaylistCard(
                            playlist =
                                    Playlist(
                                            "Downloads",
                                            icon = Icons.Filled.Download,
                                            isLiked = false,
                                            songCount = viewModel.downloadedSongs.value.size
                                    ),
                            songs = viewModel.downloadedSongs.value.values.map { it.song },
                            onClick = { onDownloadsClick() }
                    )
                }

                // User playlists
                items(playlists.keys.toList()) { playlistName ->
                    val songs = playlists[playlistName] ?: emptyList()
                    PlaylistCard(
                            playlist =
                                    Playlist(
                                            name = playlistName,
                                            songCount = playlistSongCounts[playlistName] ?: 0
                                    ),
                            songs = songs,
                            onClick = { onPlaylistClick(playlistName) },
                            onLongClick = {
                                activePlaylistForSheet = playlistName
                                showOptionsSheet = true
                            }
                    )
                }
            }
        }
    }

    // --- Options Modal Bottom Sheet on Long Press ---
    if (showOptionsSheet && activePlaylistForSheet != null) {
        val playlistName = activePlaylistForSheet!!
        ModalBottomSheet(
                onDismissRequest = { showOptionsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text(
                        text = playlistName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                        modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOptionsSheet = false
                                    showRenameDialog = true
                                }
                                .padding(vertical = 12.dp, horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(16.dp))
                    Text("Rename Playlist", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }

                Row(
                        modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOptionsSheet = false
                                    showDeleteDialog = true
                                }
                                .padding(vertical = 12.dp, horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Text("Delete Playlist", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // --- Create Playlist Dialog ---
    if (showCreateDialog) {
        AlertDialog(
                onDismissRequest = { showCreateDialog = false },
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
                                    showCreateDialog = false
                                    newPlaylistName = ""
                                }
                            }
                    ) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    // --- Rename Playlist Dialog ---
    if (showRenameDialog && activePlaylistForSheet != null) {
        val playlistName = activePlaylistForSheet!!
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
                                    showRenameDialog = false
                                }
                            }
                    ) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }

    // --- Delete Playlist Dialog ---
    if (showDeleteDialog && activePlaylistForSheet != null) {
        val playlistName = activePlaylistForSheet!!
        AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Playlist") },
                text = { Text("Are you sure you want to delete the playlist \"$playlistName\"? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.deletePlaylist(playlistName)
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}
