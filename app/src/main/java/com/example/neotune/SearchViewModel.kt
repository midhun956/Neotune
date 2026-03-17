package com.example.neotune

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SearchViewModel : ViewModel() {
    // --- Data for Search Screen ---
    private val _searchResults = mutableStateOf<List<SearchResultItem>>(emptyList())
    val searchResults: State<List<SearchResultItem>> = _searchResults
    private val _selectedFilter = mutableStateOf("All")
    val selectedFilter: State<String> = _selectedFilter
    val filters = listOf("All", "Songs", "Artists", "Albums", "Playlists")

    // --- Data for Artist Detail Screen ---
    private val _artistDetails = mutableStateOf<ArtistDetails?>(null)
    val artistDetails: State<ArtistDetails?> = _artistDetails
    private val _isLoadingArtistDetails = mutableStateOf(false)
    val isLoadingArtistDetails: State<Boolean> = _isLoadingArtistDetails

    // --- Data for Album Detail Screen ---
    private val _albumDetails = mutableStateOf<AlbumDetails?>(null)
    val albumDetails: State<AlbumDetails?> = _albumDetails
    private val _isLoadingAlbumDetails = mutableStateOf(false)
    val isLoadingAlbumDetails: State<Boolean> = _isLoadingAlbumDetails

    // --- General State ---
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error
    private var searchJob: Job? = null

    // --- Player State ---
    private val _selectedSong = mutableStateOf<SongResult?>(null)
    val selectedSong: State<SongResult?> = _selectedSong
    private val _streamUrl = mutableStateOf<String?>(null)
    val streamUrl: State<String?> = _streamUrl
    // --- Playback and Queue ---
    private val _currentSong = mutableStateOf<SongResult?>(null)
    val currentSong: State<SongResult?> = _currentSong

    // Changing queue to MutableState so we can update it directly from UI components if needed
    val queue = mutableStateOf<List<SongResult>>(emptyList())
    private val _queue
        get() = queue
    var exoPlayer: ExoPlayer? = null

    // --- User Library State ---
    private val _likedSongs = mutableStateOf<List<SongResult>>(emptyList())
    val likedSongs: State<List<SongResult>> = _likedSongs
    private val _playlists = mutableStateOf<Map<String, List<SongResult>>>(emptyMap())
    val playlists: State<Map<String, List<SongResult>>> = _playlists
    private val _playlistSongCounts = mutableStateOf<Map<String, Int>>(emptyMap())
    val playlistSongCounts: State<Map<String, Int>> = _playlistSongCounts
    val isLooping = mutableStateOf(false)
    var loopMode = mutableIntStateOf(Player.REPEAT_MODE_OFF)

    fun clearError() {
        _error.value = null
    }

    fun onQueryChange(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob =
                viewModelScope.launch {
                    delay(300L)
                    search(query, _selectedFilter.value)
                }
    }

    fun onFilterChange(filter: String, currentQuery: String) {
        _selectedFilter.value = filter
        if (currentQuery.isNotBlank()) {
            search(currentQuery, filter)
        }
    }

    fun search(query: String, filter: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = YouTubeMusicApi.search(query, filter)
                val results = parseSearchResults(response, filter) // Pass filter to parser
                _searchResults.value = results
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectSong(song: SongResult, clearQueue: Boolean = true) {
        _selectedSong.value = song
        if (clearQueue) {
            _queue.value = listOf(song)
            fetchAndQueueRecommendations(song.videoId)
        }
        playSelectedSong(song.videoId)
    }

    fun playNext() {
        val currentSong = selectedSong.value ?: return
        val queueList = queue.value

        // If the queue is not empty, play the first item in the upcoming queue
        if (queueList.isNotEmpty()) {
            val nextSong = queueList.first()
            _queue.value = queueList.drop(1)
            selectSong(nextSong, clearQueue = false)

            // If the queue is running low, fetch more recommendations based on the new song
            if (_queue.value.size < 5) {
                fetchAndQueueRecommendations(nextSong.videoId)
            }
        } else {
            // If the queue is empty, fetch recommendations based on the current song
            fetchAndQueueRecommendations(currentSong.videoId)
        }
    }

    fun playPrevious() {
        // Since we now dequeue items when playing next, we don't have a history yet.
        // For now, restarting the current song if possible.
        exoPlayer?.seekTo(0)
    }

    fun toggleLoop() {
        exoPlayer?.let { player ->
            player.repeatMode =
                    if (player.repeatMode == Player.REPEAT_MODE_ALL) {
                        Player.REPEAT_MODE_OFF
                    } else {
                        Player.REPEAT_MODE_ALL
                    }
            isLooping.value = player.repeatMode == Player.REPEAT_MODE_ALL
        }
    }

    fun isLiked(song: SongResult): Boolean {
        return likedSongs.value.any { it.videoId == song.videoId }
    }

    fun toggleLike(song: SongResult) {
        val currentLiked = _likedSongs.value.toMutableList()
        if (isLiked(song)) {
            currentLiked.removeAll { it.videoId == song.videoId }
        } else {
            currentLiked.add(song)
        }
        _likedSongs.value = currentLiked
    }

    fun createPlaylist(name: String) {
        val currentPlaylists = _playlists.value.toMutableMap()
        if (!currentPlaylists.containsKey(name)) {
            currentPlaylists[name] = emptyList()
            _playlists.value = currentPlaylists
            updatePlaylistSongCounts()
        }
    }

    fun deletePlaylist(name: String) {
        val currentPlaylists = _playlists.value.toMutableMap()
        currentPlaylists.remove(name)
        _playlists.value = currentPlaylists
        updatePlaylistSongCounts()
    }

    fun addSongToPlaylist(playlistName: String, song: SongResult) {
        val currentPlaylists = _playlists.value.toMutableMap()
        val currentSongs = currentPlaylists[playlistName]?.toMutableList() ?: mutableListOf()
        if (!currentSongs.any { it.videoId == song.videoId }) {
            currentSongs.add(song)
            currentPlaylists[playlistName] = currentSongs
            _playlists.value = currentPlaylists
            updatePlaylistSongCounts()
        }
    }

    fun addToQueue(song: SongResult) {
        val currentQueue = _queue.value.toMutableList()
        if (!currentQueue.any { it.videoId == song.videoId }) {
            currentQueue.add(song)
            _queue.value = currentQueue
        }
    }

    fun removeFromQueue(song: SongResult) {
        val currentQueue = _queue.value.toMutableList()
        currentQueue.removeAll { it.videoId == song.videoId }
        _queue.value = currentQueue
    }

    fun playSong(song: SongResult) {
        selectSong(song)
    }

    fun playAlbumSong(albumDetails: AlbumDetails, startIndex: Int) {
        if (startIndex !in albumDetails.tracks.indices) return

        // 1. Play the clicked song
        val clickedTrack = albumDetails.tracks[startIndex]
        val song =
                SongResult(
                        title = clickedTrack.title,
                        artist = clickedTrack.artists ?: albumDetails.artist,
                        thumbnailUrl = albumDetails.thumbnailUrl,
                        videoId = clickedTrack.videoId,
                        album = albumDetails.title,
                        duration = clickedTrack.duration
                )
        selectSong(song, clearQueue = false)

        // 2. Queue up the remaining songs
        val remainingTracks =
                albumDetails.tracks.drop(startIndex + 1).map { track ->
                    SongResult(
                            title = track.title,
                            artist = track.artists ?: albumDetails.artist,
                            thumbnailUrl = albumDetails.thumbnailUrl,
                            videoId = track.videoId,
                            album = albumDetails.title,
                            duration = track.duration
                    )
                }
        _queue.value = remainingTracks
    }

    fun clearQueue() {
        _queue.value = emptyList()
    }

    fun loadArtistDetails(browseId: String) {
        viewModelScope.launch {
            _isLoadingArtistDetails.value = true
            _artistDetails.value = null
            try {
                val response = YouTubeMusicApi.getArtist(browseId)
                _artistDetails.value = parseArtistDetails(response)
            } catch (e: Exception) {
                _error.value = "Failed to load artist details."
            } finally {
                _isLoadingArtistDetails.value = false
            }
        }
    }

    private fun playSelectedSong(videoId: String) {
        viewModelScope.launch {
            try {
                val audioUrl = YouTubeMusicApi.getBackendAudioUrl(videoId)
                audioUrl?.let {
                    val mediaItem = MediaItem.fromUri(it)
                    exoPlayer?.apply {
                        stop()
                        setMediaItem(mediaItem)
                        prepare()
                        play()
                    }
                    _streamUrl.value = it
                }
                        ?: run { _error.value = "Unable to get audio stream." }
            } catch (e: Exception) {
                _error.value = "Failed to get audio stream: ${e.message}"
            }
        }
    }

    private fun updatePlaylistSongCounts() {
        _playlistSongCounts.value = _playlists.value.mapValues { it.value.size }
    }

    private fun fetchAndQueueRecommendations(videoId: String) {
        viewModelScope.launch {
            try {
                val json = YouTubeMusicApi.getRecommended(videoId)
                if (json.isBlank()) return@launch

                val recommendedSongs = parseSongResultsOnly(json)
                if (recommendedSongs.isNotEmpty()) {
                    val currentQueue = _queue.value
                    val newSongsToAdd =
                            recommendedSongs.filter { newSong ->
                                currentQueue.none { it.videoId == newSong.videoId }
                            }
                    if (newSongsToAdd.isNotEmpty()) {
                        _queue.value = currentQueue + newSongsToAdd
                    }
                }
            } catch (e: Exception) {
                println("Error fetching recommendations: ${e.message}")
            }
        }
    }

    private fun parseSearchResults(json: String, filter: String): List<SearchResultItem> {
        return try {
            Json.parseToJsonElement(json).jsonArray.mapNotNull { element ->
                when (filter.lowercase()) {
                    "songs" -> parseSongFromJson(element.jsonObject)
                    "artists" -> parseArtistFromJson(element.jsonObject)
                    "albums" -> parseAlbumFromJson(element.jsonObject)
                    "playlists" -> parsePlaylistFromJson(element.jsonObject)
                    "all" -> guessResultType(element.jsonObject)
                    else -> null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun guessResultType(json: JsonElement): SearchResultItem? {
        val category = json.jsonObject["category"]?.jsonPrimitive?.content?.lowercase()
        return when {
            category == "songs" -> parseSongFromJson(json.jsonObject)
            category == "artists" -> parseArtistFromJson(json.jsonObject)
            category == "albums" -> parseAlbumFromJson(json.jsonObject)
            category == "playlists" -> parsePlaylistFromJson(json.jsonObject)
            json.jsonObject.containsKey("videoId") -> parseSongFromJson(json.jsonObject)
            else -> null
        }
    }

    private fun getThumbnailUrl(json: JsonElement): String? {
        val thumbnailNode = json.jsonObject["thumbnails"] ?: json.jsonObject["thumbnail"]
        return thumbnailNode
                ?.jsonArray
                ?.lastOrNull()
                ?.jsonObject
                ?.get("url")
                ?.jsonPrimitive
                ?.content
    }

    private fun parseSongFromJson(json: JsonElement): SongResult? {
        val obj = json.jsonObject
        return SongResult(
                title = obj["title"]?.jsonPrimitive?.content ?: return null,
                videoId = obj["videoId"]?.jsonPrimitive?.content ?: return null,
                artist =
                        obj["artists"]?.jsonArray?.joinToString(", ") {
                            it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                        },
                album = obj["album"]?.jsonObject?.get("name")?.jsonPrimitive?.content,
                duration = obj["duration"]?.jsonPrimitive?.content
                                ?: obj["length"]?.jsonPrimitive?.content,
                thumbnailUrl = getThumbnailUrl(json)
        )
    }

    private fun parseArtistFromJson(json: JsonElement): ArtistResult? {
        val obj = json.jsonObject
        return ArtistResult(
                name = obj["artist"]?.jsonPrimitive?.content
                                ?: obj["name"]?.jsonPrimitive?.content ?: return null,
                browseId = obj["browseId"]?.jsonPrimitive?.content,
                thumbnailUrl = getThumbnailUrl(json)
        )
    }

    private fun parseAlbumFromJson(json: JsonElement): AlbumResult? {
        val obj = json.jsonObject
        // The array of artists could be directly in 'artists'
        val artistsString =
                obj["artists"]?.jsonArray?.joinToString(", ") {
                    it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                }
        val yearString = obj["year"]?.jsonPrimitive?.content ?: obj["type"]?.jsonPrimitive?.content
        return AlbumResult(
                title = obj["title"]?.jsonPrimitive?.content ?: return null,
                artists = artistsString,
                year = yearString,
                browseId = obj["browseId"]?.jsonPrimitive?.content,
                thumbnailUrl = getThumbnailUrl(json)
        )
    }

    private fun parsePlaylistFromJson(json: JsonElement): PlaylistResult? {
        val obj = json.jsonObject
        // The author could be a string or we might need to extract it
        val author =
                obj["author"]?.jsonPrimitive?.content
                        ?: obj["author"]
                                ?.jsonArray
                                ?.firstOrNull()
                                ?.jsonObject
                                ?.get("name")
                                ?.jsonPrimitive
                                ?.content
        return PlaylistResult(
                name = obj["title"]?.jsonPrimitive?.content ?: return null,
                author = author,
                songCount = obj["itemCount"]?.jsonPrimitive?.content,
                browseId = obj["browseId"]?.jsonPrimitive?.content,
                thumbnailUrl = getThumbnailUrl(json)
        )
    }

    private fun parseArtistDetails(json: String): ArtistDetails? {
        return try {
            val root = Json.parseToJsonElement(json).jsonObject
            // --- MODIFICATION START ---
            // REASON: This is the fix for the build error. We first extract the name into a
            // variable. If the name is null (missing from the JSON), we return null immediately,
            // preventing the app from trying to create an ArtistDetails object with invalid data.
            val artistName = root["name"]?.jsonPrimitive?.content ?: return null

            ArtistDetails(
                    name = artistName,
                    // --- MODIFICATION END ---
                    description = root["description"]?.jsonPrimitive?.content,
                    thumbnailUrl = getThumbnailUrl(root),
                    topSongs =
                            root["songs"]?.jsonObject?.get("results")?.jsonArray?.mapNotNull {
                                parseSongFromJson(it)
                            }
                                    ?: emptyList(),
                    albums =
                            root["albums"]?.jsonObject?.get("results")?.jsonArray?.mapNotNull {
                                parseAlbumFromJson(it)
                            }
                                    ?: emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSongResultsOnly(json: String): List<SongResult> {
        return try {
            Json.parseToJsonElement(json).jsonArray.mapNotNull { parseSongFromJson(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadAlbumDetails(browseId: String) {
        viewModelScope.launch {
            _isLoadingAlbumDetails.value = true
            _albumDetails.value = null
            try {
                val response = YouTubeMusicApi.getAlbum(browseId)
                _albumDetails.value = parseAlbumDetails(response)
            } catch (e: Exception) {
                _error.value = "Failed to load album details: ${e.message}"
            } finally {
                _isLoadingAlbumDetails.value = false
            }
        }
    }

    private fun parseAlbumDetails(json: String): AlbumDetails? {
        return try {
            val root = Json.parseToJsonElement(json).jsonObject
            val albumTitle = root["title"]?.jsonPrimitive?.content ?: return null

            val tracksList =
                    root["tracks"]?.jsonArray?.mapNotNull { trackElement ->
                        val trObj = trackElement.jsonObject
                        AlbumTrack(
                                title = trObj["title"]?.jsonPrimitive?.content
                                                ?: return@mapNotNull null,
                                videoId = trObj["videoId"]?.jsonPrimitive?.content
                                                ?: return@mapNotNull null,
                                duration = trObj["duration"]?.jsonPrimitive?.content,
                                artists =
                                        trObj["artists"]?.let { el ->
                                            if (el is JsonArray) {
                                                el.joinToString(", ") {
                                                    it.jsonObject["name"]?.jsonPrimitive?.content
                                                            ?: ""
                                                }
                                            } else {
                                                el.jsonPrimitive.content
                                            }
                                        }
                        )
                    }
                            ?: emptyList()

            AlbumDetails(
                    title = albumTitle,
                    description = root["description"]?.jsonPrimitive?.content,
                    thumbnailUrl = getThumbnailUrl(root),
                    year = root["year"]?.jsonPrimitive?.content,
                    artist =
                            root["artists"]?.jsonArray?.joinToString(", ") {
                                it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                            },
                    tracks = tracksList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
