package com.example.neotune.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.neotune.AlbumResult
import com.example.neotune.ArtistResult
import com.example.neotune.PlaylistResult
import com.example.neotune.SearchResultItem
import com.example.neotune.SearchViewModel
import com.example.neotune.SongResult
import com.example.neotune.ui.components.AlbumRowItem
import com.example.neotune.ui.components.ArtistRowItem
import com.example.neotune.ui.components.PlaylistRowItem
import com.example.neotune.ui.components.SongRowItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeMusicSearchScreen(
        searchResults: List<SearchResultItem>,
        isLoading: Boolean,
        error: String?,
        onSongClick: (SongResult) -> Unit,
        onArtistClick: (ArtistResult) -> Unit,
        onAlbumClick: (AlbumResult) -> Unit,
        viewModel: SearchViewModel
) {
    var query by remember { mutableStateOf("") }
    val selectedFilter by viewModel.selectedFilter

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onQueryChange(it)
                },
                placeholder = { Text("Search songs, artists, albums") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                                onClick = {
                                    query = ""
                                    viewModel.onQueryChange("")
                                }
                        ) { Icon(Icons.Default.Clear, contentDescription = "Clear") }
                    }
                },
                singleLine = true,
                shape = CircleShape
        )

        LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.filters) { filter ->
                FilterChip(
                        selected = filter == selectedFilter,
                        onClick = { viewModel.onFilterChange(filter, query) },
                        label = { Text(filter) },
                        leadingIcon = {
                            if (filter == selectedFilter) Icon(Icons.Filled.Done, null)
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else if (error != null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                        error,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(searchResults) { item ->
                    when (item) {
                        is SongResult -> SongRowItem(item, onSongClick, viewModel)
                        is ArtistResult -> ArtistRowItem(item, onClick = { onArtistClick(item) })
                        is AlbumResult -> AlbumRowItem(item, onClick = { onAlbumClick(item) })
                        is PlaylistResult -> PlaylistRowItem(item)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
