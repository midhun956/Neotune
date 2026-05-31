package com.example.neotune

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import android.net.Uri
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val ctx get() = getApplication<Application>()

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

    // --- Navigation triggers (set by ViewModel, consumed by MainActivity) ---
    val navigateToArtistDetail = mutableStateOf(false)
    val navigateToAlbumDetail = mutableStateOf(false)
    val songToAddToPlaylist = mutableStateOf<SongResult?>(null)
    val showAddToPlaylistSheet = mutableStateOf(false)
    private var isNavigatingToArtist = false
    private var isNavigatingToAlbum = false

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

    val queue = mutableStateOf<List<SongResult>>(emptyList())
    private val _queue
        get() = queue
    val recommendedVideoIds = mutableSetOf<String>()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                playNext()
            }
        }
    }

    private var _exoPlayer: Player? = null
    var exoPlayer: Player?
        get() = _exoPlayer
        set(value) {
            _exoPlayer?.removeListener(playerListener)
            _exoPlayer = value
            _exoPlayer?.addListener(playerListener)
        }

    // --- User Library State ---
    private val _likedSongs = mutableStateOf<List<SongResult>>(emptyList())
    val likedSongs: State<List<SongResult>> = _likedSongs
    private val _playlists = mutableStateOf<Map<String, List<SongResult>>>(emptyMap())
    val playlists: State<Map<String, List<SongResult>>> = _playlists
    private val _playlistSongCounts = mutableStateOf<Map<String, Int>>(emptyMap())
    val playlistSongCounts: State<Map<String, Int>> = _playlistSongCounts
    val isLooping = mutableStateOf(false)
    var loopMode = mutableIntStateOf(Player.REPEAT_MODE_OFF)
    val playbackHistory = mutableStateOf<List<SongResult>>(emptyList())
    val nowPlayingBackgroundStyle = mutableStateOf("gradient")
    val appThemeStyle = mutableStateOf("amoled")
    val amoledAccentColor = mutableStateOf("purple")
    val backendIp = mutableStateOf("")
    val isCheckingConnection = mutableStateOf(false)
    val connectionStatus = mutableStateOf<Boolean?>(null)

    // --- Offline Caching and Downloading States ---
    val downloadedSongs = mutableStateOf<Map<String, OfflineTrack>>(emptyMap())
    val activeDownloads = mutableStateOf<Set<String>>(emptySet())
    val cacheStorageLimit = mutableStateOf("1.0 GB")
    val currentCacheSize = mutableStateOf(0L)
    private val downloadJobs = mutableMapOf<String, Job>()
    private val downloadSemaphore = kotlinx.coroutines.sync.Semaphore(3)

    // --- Recently Played ---
    private val _recentlyPlayed = mutableStateOf<List<SongResult>>(emptyList())
    val recentlyPlayed: State<List<SongResult>> = _recentlyPlayed

    // --- Trending ---
    private val _trendingSongs = mutableStateOf<List<SongResult>>(emptyList())
    val trendingSongs: State<List<SongResult>> = _trendingSongs
    private val _isLoadingTrending = mutableStateOf(false)
    val isLoadingTrending: State<Boolean> = _isLoadingTrending

    // --- Quick Picks ---
    private val _quickPicksSongs = mutableStateOf<List<SongResult>>(emptyList())
    val quickPicksSongs: State<List<SongResult>> = _quickPicksSongs
    private val _isLoadingQuickPicks = mutableStateOf(false)
    val isLoadingQuickPicks: State<Boolean> = _isLoadingQuickPicks

    init {
        // Load persisted data and dynamically upgrade all saved thumbnails to high-definition
        _likedSongs.value = LocalStorage.loadLikedSongs(ctx).map { 
            it.copy(thumbnailUrl = getHighQualityThumbnailUrl(it.thumbnailUrl)) 
        }
        
        _playlists.value = LocalStorage.loadPlaylists(ctx).mapValues { entry ->
            entry.value.map { it.copy(thumbnailUrl = getHighQualityThumbnailUrl(it.thumbnailUrl)) }
        }
        
        _recentlyPlayed.value = LocalStorage.loadRecentlyPlayed(ctx).map { 
            it.copy(thumbnailUrl = getHighQualityThumbnailUrl(it.thumbnailUrl)) 
        }
        
        updatePlaylistSongCounts()
        loadTrending()
        loadQuickPicks()
        nowPlayingBackgroundStyle.value = LocalStorage.loadNPBackgroundStyle(ctx)
        appThemeStyle.value = LocalStorage.loadAppThemeStyle(ctx)
        amoledAccentColor.value = LocalStorage.loadAmoledAccent(ctx)
        
        val storedIp = LocalStorage.loadBackendIp(ctx)
        backendIp.value = storedIp
        Config.BACKEND_BASE_URL = formatBackendUrl(storedIp)

        cacheStorageLimit.value = LocalStorage.loadCacheLimit(ctx)
        refreshCacheStats()
    }

    private fun formatBackendUrl(input: String): String {
        var cleaned = input.trim()
        if (cleaned.isBlank()) {
            return ""
        }
        if (cleaned.startsWith("http://")) {
            cleaned = cleaned.removePrefix("http://")
        } else if (cleaned.startsWith("https://")) {
            cleaned = cleaned.removePrefix("https://")
        }
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.removeSuffix("/")
        }
        if (cleaned.isBlank()) {
            return ""
        }
        return if (cleaned.contains(":")) {
            "http://$cleaned"
        } else {
            "http://$cleaned:8000"
        }
    }

    fun setNowPlayingBackgroundStyle(style: String) {
        nowPlayingBackgroundStyle.value = style
        LocalStorage.saveNPBackgroundStyle(ctx, style)
    }

    fun setAppThemeStyle(style: String) {
        appThemeStyle.value = style
        LocalStorage.saveAppThemeStyle(ctx, style)
    }

    fun setAmoledAccent(accent: String) {
        amoledAccentColor.value = accent
        LocalStorage.saveAmoledAccent(ctx, accent)
    }

    fun setBackendIp(ip: String) {
        val trimmed = ip.trim()
        backendIp.value = trimmed
        LocalStorage.saveBackendIp(ctx, trimmed)
        Config.BACKEND_BASE_URL = formatBackendUrl(trimmed)
        connectionStatus.value = null
        loadTrending()
        loadQuickPicks()
    }

    fun checkConnection(ip: String) {
        viewModelScope.launch {
            isCheckingConnection.value = true
            connectionStatus.value = null
            val url = formatBackendUrl(ip)
            connectionStatus.value = YouTubeMusicApi.ping(url)
            isCheckingConnection.value = false
        }
    }

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
                val results = parseSearchResults(response, filter)
                _searchResults.value = results
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun broadcastLikeState(song: SongResult?) {
        if (song == null) return
        val isLiked = isLiked(song)
        val intent = android.content.Intent("com.example.neotune.ACTION_UPDATE_LIKE").apply {
            setPackage(ctx.packageName)
            putExtra("is_liked", isLiked)
        }
        ctx.sendBroadcast(intent)
    }

    fun selectSong(song: SongResult, clearQueue: Boolean = true, addToHistory: Boolean = true) {
        val prev = _selectedSong.value
        val historyList = playbackHistory.value.toMutableList()

        if (addToHistory && prev != null && prev.videoId != song.videoId) {
            historyList.removeAll { it.videoId == prev.videoId }
            historyList.add(0, prev)
        }
        // Ensure the newly selected song is not in the history stack
        historyList.removeAll { it.videoId == song.videoId }
        playbackHistory.value = historyList

        _selectedSong.value = song
        broadcastLikeState(song)
        if (clearQueue) {
            recommendedVideoIds.clear()
            _queue.value = listOf(song)
            fetchAndQueueRecommendations(song.videoId)
        }
        playSelectedSong(song.videoId)
        
        // Update last played timestamp in offline registry for LRP
        viewModelScope.launch {
            DownloadManager.updateLastPlayed(ctx, song.videoId)
            refreshCacheStats()
        }

        // Track recently played (deduplicate, max 30)
        val current = _recentlyPlayed.value.toMutableList()
        current.removeAll { it.videoId == song.videoId }
        current.add(0, song)
        if (current.size > 30) current.subList(30, current.size).clear()
        _recentlyPlayed.value = current
        LocalStorage.saveRecentlyPlayed(ctx, current)
    }

    fun playNext() {
        val currentSong = selectedSong.value ?: return
        val queueList = queue.value

        if (queueList.isNotEmpty()) {
            val nextSong = queueList.first()
            _queue.value = queueList.drop(1)
            selectSong(nextSong, clearQueue = false)

            if (_queue.value.size < 5) {
                fetchAndQueueRecommendations(nextSong.videoId)
            }
        } else {
            fetchAndQueueRecommendations(currentSong.videoId)
        }
    }

    fun moveQueueSong(fromIndex: Int, toIndex: Int) {
        val currentQueue = queue.value.toMutableList()
        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices) {
            val movedItem = currentQueue.removeAt(fromIndex)
            currentQueue.add(toIndex, movedItem)
            queue.value = currentQueue
        }
    }

    fun playPrevious() {
        val player = exoPlayer
        if (player != null && player.currentPosition > 3000) {
            player.seekTo(0)
            return
        }

        val historyList = playbackHistory.value.toMutableList()
        if (historyList.isNotEmpty()) {
            val prevSong = historyList.removeAt(0)
            playbackHistory.value = historyList

            // Put current song to front of queue
            val current = _selectedSong.value
            if (current != null) {
                val qList = _queue.value.toMutableList()
                qList.removeAll { it.videoId == current.videoId }
                qList.add(0, current)
                _queue.value = qList
            }

            // Play previous song without adding current song back to history again
            selectSong(prevSong, clearQueue = false, addToHistory = false)
        } else {
            exoPlayer?.seekTo(0)
        }
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
        LocalStorage.saveLikedSongs(ctx, currentLiked)
        if (selectedSong.value?.videoId == song.videoId) {
            broadcastLikeState(song)
        }
    }

    fun createPlaylist(name: String) {
        val currentPlaylists = _playlists.value.toMutableMap()
        if (!currentPlaylists.containsKey(name)) {
            currentPlaylists[name] = emptyList()
            _playlists.value = currentPlaylists
            updatePlaylistSongCounts()
            LocalStorage.savePlaylists(ctx, currentPlaylists)
        }
    }

    fun deletePlaylist(name: String) {
        val currentPlaylists = _playlists.value.toMutableMap()
        currentPlaylists.remove(name)
        _playlists.value = currentPlaylists
        updatePlaylistSongCounts()
        LocalStorage.savePlaylists(ctx, currentPlaylists)
    }

    fun addSongToPlaylist(playlistName: String, song: SongResult) {
        val currentPlaylists = _playlists.value.toMutableMap()
        val currentSongs = currentPlaylists[playlistName]?.toMutableList() ?: mutableListOf()
        if (!currentSongs.any { it.videoId == song.videoId }) {
            currentSongs.add(song)
            currentPlaylists[playlistName] = currentSongs
            _playlists.value = currentPlaylists
            updatePlaylistSongCounts()
            LocalStorage.savePlaylists(ctx, currentPlaylists)
        }
    }

    fun removeSongFromPlaylist(playlistName: String, song: SongResult) {
        val currentPlaylists = _playlists.value.toMutableMap()
        val currentSongs = currentPlaylists[playlistName]?.toMutableList() ?: return
        currentSongs.removeAll { it.videoId == song.videoId }
        currentPlaylists[playlistName] = currentSongs
        _playlists.value = currentPlaylists
        updatePlaylistSongCounts()
        LocalStorage.savePlaylists(ctx, currentPlaylists)
    }

    fun renamePlaylist(oldName: String, newName: String) {
        val currentPlaylists = _playlists.value.toMutableMap()
        if (currentPlaylists.containsKey(oldName) && newName.isNotBlank() && oldName != newName) {
            val songs = currentPlaylists.remove(oldName) ?: emptyList()
            currentPlaylists[newName] = songs
            _playlists.value = currentPlaylists
            updatePlaylistSongCounts()
            LocalStorage.savePlaylists(ctx, currentPlaylists)
        }
    }

    fun playPlaylist(playlistName: String, shuffle: Boolean = false) {
        val songs = _playlists.value[playlistName] ?: return
        if (songs.isEmpty()) return

        val listToPlay = if (shuffle) songs.shuffled() else songs
        val firstSong = listToPlay.first()
        selectSong(firstSong, clearQueue = true)

        if (listToPlay.size > 1) {
            _queue.value = listToPlay.drop(1)
        }
    }

    fun playPlaylistSong(playlistName: String, clickedSong: SongResult) {
        val songs = _playlists.value[playlistName] ?: return
        val startIndex = songs.indexOfFirst { it.videoId == clickedSong.videoId }
        if (startIndex == -1) return

        selectSong(clickedSong, clearQueue = true)
        if (startIndex + 1 < songs.size) {
            _queue.value = songs.drop(startIndex + 1)
        }
    }

    fun addToQueue(song: SongResult) {
        val currentQueue = _queue.value.toMutableList()
        currentQueue.removeAll { it.videoId == song.videoId }
        recommendedVideoIds.remove(song.videoId)

        // Find the first index of a recommended song to insert before it
        val firstRecIndex = currentQueue.indexOfFirst { recommendedVideoIds.contains(it.videoId) }
        if (firstRecIndex != -1) {
            currentQueue.add(firstRecIndex, song)
        } else {
            currentQueue.add(song)
        }
        _queue.value = currentQueue
    }

    fun playNext(song: SongResult) {
        val currentQueue = _queue.value.toMutableList()
        currentQueue.removeAll { it.videoId == song.videoId }
        recommendedVideoIds.remove(song.videoId)
        currentQueue.add(0, song)
        _queue.value = currentQueue
    }

    fun removeFromQueue(song: SongResult) {
        val currentQueue = _queue.value.toMutableList()
        currentQueue.removeAll { it.videoId == song.videoId }
        recommendedVideoIds.remove(song.videoId)
        _queue.value = currentQueue
    }

    fun movePlaylistSong(playlistName: String, fromIndex: Int, toIndex: Int) {
        val currentPlaylists = _playlists.value.toMutableMap()
        val songs = currentPlaylists[playlistName]?.toMutableList() ?: return
        if (fromIndex in songs.indices && toIndex in songs.indices) {
            val s = songs.removeAt(fromIndex)
            songs.add(toIndex, s)
            currentPlaylists[playlistName] = songs
            _playlists.value = currentPlaylists
            LocalStorage.savePlaylists(ctx, currentPlaylists)
        }
    }

    fun playSong(song: SongResult) {
        selectSong(song)
    }

    fun playAlbumSong(albumDetails: AlbumDetails, startIndex: Int) {
        if (startIndex !in albumDetails.tracks.indices) return

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

    fun searchAndNavigateToArtist(artistName: String) {
        if (isNavigatingToArtist || _isLoadingArtistDetails.value) return
        isNavigatingToArtist = true
        viewModelScope.launch {
            try {
                // Parse primary artist out of multi-artist string
                val primaryArtist = artistName.split(Regex("[,&]|\\b(feat|ft)\\b", RegexOption.IGNORE_CASE))
                        .firstOrNull()?.trim() ?: artistName

                val response = YouTubeMusicApi.search(primaryArtist, "Artists")
                val results = parseSearchResults(response, "artists")
                val artist = results.filterIsInstance<ArtistResult>().firstOrNull { it.browseId != null }
                if (artist?.browseId != null) {
                    loadArtistDetails(artist.browseId)
                    navigateToArtistDetail.value = true
                }
            } catch (e: Exception) { /* silently ignore */ }
            finally {
                isNavigatingToArtist = false
            }
        }
    }

    fun searchAndNavigateToAlbum(albumName: String) {
        if (isNavigatingToAlbum || _isLoadingAlbumDetails.value) return
        isNavigatingToAlbum = true
        viewModelScope.launch {
            try {
                val response = YouTubeMusicApi.search(albumName, "Albums")
                val results = parseSearchResults(response, "albums")
                val album = results.filterIsInstance<AlbumResult>().firstOrNull { it.browseId != null }
                if (album?.browseId != null) {
                    loadAlbumDetails(album.browseId)
                    navigateToAlbumDetail.value = true
                }
            } catch (e: Exception) { /* silently ignore */ }
            finally {
                isNavigatingToAlbum = false
            }
        }
    }

    fun clearArtistNavigation() { navigateToArtistDetail.value = false }
    fun clearAlbumNavigation() { navigateToAlbumDetail.value = false }

    fun loadTrending() {
        viewModelScope.launch {
            _isLoadingTrending.value = true
            try {
                val response = YouTubeMusicApi.getTrending()
                if (response.isNotBlank()) {
                    _trendingSongs.value = parseSongResultsOnly(response)
                }
            } catch (e: Exception) {
                // Silently fail — trending is optional
            } finally {
                _isLoadingTrending.value = false
            }
        }
    }

    fun loadQuickPicks() {
        viewModelScope.launch {
            _isLoadingQuickPicks.value = true
            try {
                // Use the last recently played song as the seed, else general recommended
                val seedId = _recentlyPlayed.value.firstOrNull()?.videoId
                val response = YouTubeMusicApi.getRecommended(seedId)
                if (response.isNotBlank()) {
                    _quickPicksSongs.value = parseSongResultsOnly(response)
                }
            } catch (e: Exception) {
                // Silently fail
            } finally {
                _isLoadingQuickPicks.value = false
            }
        }
    }

    private fun playSelectedSong(videoId: String) {
        val song = selectedSong.value
        viewModelScope.launch {
            try {
                val localFile = File(ctx.filesDir, "offline_cache/$videoId")
                if (localFile.exists()) {
                    // Play cached file directly
                    val metadataBuilder = MediaMetadata.Builder()
                    if (song != null) {
                        metadataBuilder.setTitle(song.title)
                        metadataBuilder.setArtist(song.artist ?: "Unknown Artist")
                        metadataBuilder.setArtworkUri(Uri.parse(song.thumbnailUrl ?: ""))
                    }
                    val mediaItem = MediaItem.Builder()
                            .setUri(Uri.fromFile(localFile))
                            .setMediaMetadata(metadataBuilder.build())
                            .build()
                    exoPlayer?.apply {
                        stop()
                        setMediaItem(mediaItem)
                        prepare()
                        play()
                    }
                    _streamUrl.value = Uri.fromFile(localFile).toString()
                } else {
                    val audioUrl = YouTubeMusicApi.getBackendAudioUrl(videoId)
                    audioUrl?.let {
                        val metadataBuilder = MediaMetadata.Builder()
                        if (song != null) {
                            metadataBuilder.setTitle(song.title)
                            metadataBuilder.setArtist(song.artist ?: "Unknown Artist")
                            metadataBuilder.setArtworkUri(Uri.parse(song.thumbnailUrl ?: ""))
                        }
                        val mediaItem = MediaItem.Builder()
                                .setUri(it)
                                .setMediaMetadata(metadataBuilder.build())
                                .build()
                        exoPlayer?.apply {
                            stop()
                            setMediaItem(mediaItem)
                            prepare()
                            play()
                        }
                        _streamUrl.value = it
                    }
                            ?: run { _error.value = "Unable to get audio stream." }
                }
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
                        recommendedVideoIds.addAll(newSongsToAdd.map { it.videoId })
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

    private fun getHighQualityThumbnailUrl(url: String?): String? {
        if (url == null) return null
        
        // 1. Google/YouTube Music format: replace =wXXX-hXXX with =w544-h544
        if (url.contains("=w") && url.contains("-h")) {
            val regex = "=w\\d+-h\\d+".toRegex()
            if (regex.containsMatchIn(url)) {
                return url.replace(regex, "=w544-h544")
            }
        }
        
        // 2. YouTube standard video thumbnail: upgrade default/sddefault to hqdefault
        if (url.contains("i.ytimg.com/vi/")) {
            if (url.endsWith("/default.jpg")) {
                return url.replace("/default.jpg", "/hqdefault.jpg")
            }
            if (url.endsWith("/sddefault.jpg")) {
                return url.replace("/sddefault.jpg", "/hqdefault.jpg")
            }
        }
        
        return url
    }

    private fun getThumbnailUrl(json: JsonElement): String? {
        val thumbnailNode = json.jsonObject["thumbnails"] ?: json.jsonObject["thumbnail"]
        val rawUrl = thumbnailNode
                ?.jsonArray
                ?.lastOrNull()
                ?.jsonObject
                ?.get("url")
                ?.jsonPrimitive
                ?.content
        return getHighQualityThumbnailUrl(rawUrl)
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
            val artistName = root["name"]?.jsonPrimitive?.content ?: return null

            ArtistDetails(
                    name = artistName,
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

    // --- Offline Cache Management Actions ---
    fun refreshCacheStats() {
        viewModelScope.launch {
            downloadedSongs.value = DownloadManager.loadRegistry(ctx)
            currentCacheSize.value = DownloadManager.getTotalCacheSize(ctx)
        }
    }

    fun downloadSong(song: SongResult) {
        val videoId = song.videoId
        if (activeDownloads.value.contains(videoId) || downloadedSongs.value.containsKey(videoId)) return

        activeDownloads.value = activeDownloads.value + videoId
        val job = viewModelScope.launch {
            try {
                downloadSemaphore.acquire()
                try {
                    DownloadManager.downloadSong(ctx, song, cacheStorageLimit.value)
                } finally {
                    downloadSemaphore.release()
                }
            } finally {
                activeDownloads.value = activeDownloads.value - videoId
                downloadJobs.remove(videoId)
                refreshCacheStats()
            }
        }
        downloadJobs[videoId] = job
    }

    fun cancelDownload(videoId: String) {
        downloadJobs[videoId]?.cancel()
        downloadJobs.remove(videoId)
        activeDownloads.value = activeDownloads.value - videoId
        viewModelScope.launch {
            DownloadManager.deleteDownload(ctx, videoId)
            refreshCacheStats()
        }
    }

    fun downloadPlaylist(playlistName: String) {
        val songs = playlists.value[playlistName] ?: return
        songs.forEach { song ->
            downloadSong(song)
        }
    }

    fun removeDownload(videoId: String) {
        cancelDownload(videoId)
        viewModelScope.launch {
            DownloadManager.deleteDownload(ctx, videoId)
            refreshCacheStats()
        }
    }

    fun setCacheLimit(limit: String) {
        cacheStorageLimit.value = limit
        LocalStorage.saveCacheLimit(ctx, limit)
        viewModelScope.launch {
            DownloadManager.checkAndEvictCache(ctx, 0L, limit)
            refreshCacheStats()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            DownloadManager.clearCache(ctx)
            refreshCacheStats()
        }
    }
}

