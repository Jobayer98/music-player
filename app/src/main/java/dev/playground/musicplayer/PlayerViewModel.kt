package dev.playground.musicplayer

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortMode(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    RECENT("Recently added"),
}

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    FAVORITES("Favorites"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
}

data class PlayerUiState(
    val songs: List<Song> = emptyList(),
    val loading: Boolean = true,
    val query: String = "",
    val currentId: Long? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val favorites: Set<Long> = emptySet(),
    val tab: LibraryTab = LibraryTab.SONGS,
    /** Album or artist name opened from the Albums / Artists tab. */
    val group: String? = null,
    val sort: SortMode = SortMode.TITLE,
    val sleepRemainingMs: Long = 0,
    /** Song ids in playback-queue order and the index currently playing. */
    val queue: List<Long> = emptyList(),
    val queueIndex: Int = 0,
) {
    val queueSongs: List<Song>
        get() {
            val byId = songs.associateBy { it.id }
            return queue.mapNotNull { byId[it] }
        }

    val visibleSongs: List<Song>
        get() {
            var list = songs
            if (tab == LibraryTab.FAVORITES) list = list.filter { it.id in favorites }
            if (group != null) list = list.filter {
                if (tab == LibraryTab.ALBUMS) it.album == group else it.artist == group
            }
            if (query.isNotBlank()) list = list.filter {
                it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
            }
            return when (sort) {
                SortMode.TITLE -> list
                SortMode.ARTIST -> list.sortedBy { it.artist.lowercase() }
                SortMode.RECENT -> list.sortedByDescending { it.dateAdded }
            }
        }
    val current: Song? get() = songs.firstOrNull { it.id == currentId }
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var sleepJob: Job? = null
    private val prefs = app.getSharedPreferences("music_prefs", android.content.Context.MODE_PRIVATE)

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncFromPlayer(player)
    }

    init {
        val favs = prefs.getStringSet("favorites", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        val sort = SortMode.entries.firstOrNull { it.name == prefs.getString("sort", null) } ?: SortMode.TITLE
        _state.update { it.copy(favorites = favs, sort = sort) }

        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            controller = future.get().also {
                it.addListener(listener)
                syncFromPlayer(it)
            }
        }, MoreExecutors.directExecutor())
    }

    fun loadSongs() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val songs = withContext(Dispatchers.IO) { MusicRepository.load(getApplication()) }
            _state.update { it.copy(songs = songs, loading = false) }
            controller?.let(::syncFromPlayer)
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun setTab(tab: LibraryTab) = _state.update { it.copy(tab = tab, group = null) }

    fun setGroup(name: String?) = _state.update { it.copy(group = name) }

    fun playAll() {
        val c = controller ?: return
        val list = _state.value.visibleSongs
        if (list.isEmpty()) return
        c.shuffleModeEnabled = false
        c.setMediaItems(list.map(::toMediaItem), 0, 0)
        c.prepare()
        c.play()
    }

    fun setSort(mode: SortMode) {
        prefs.edit().putString("sort", mode.name).apply()
        _state.update { it.copy(sort = mode) }
    }

    fun toggleFavorite(id: Long) {
        val updated = _state.value.favorites.let { if (id in it) it - id else it + id }
        prefs.edit().putStringSet("favorites", updated.map { it.toString() }.toSet()).apply()
        _state.update { it.copy(favorites = updated) }
    }

    /** Pauses playback after [minutes]; 0 cancels the timer. */
    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _state.update { it.copy(sleepRemainingMs = 0) }
            return
        }
        val endsAt = android.os.SystemClock.elapsedRealtime() + minutes * 60_000L
        sleepJob = viewModelScope.launch {
            while (isActive) {
                val left = endsAt - android.os.SystemClock.elapsedRealtime()
                if (left <= 0) break
                _state.update { it.copy(sleepRemainingMs = left) }
                delay(1000)
            }
            controller?.pause()
            _state.update { it.copy(sleepRemainingMs = 0) }
        }
    }

    fun play(song: Song) {
        val c = controller ?: return
        val list = _state.value.visibleSongs
        val index = list.indexOfFirst { it.id == song.id }
        if (index < 0) return
        c.setMediaItems(list.map(::toMediaItem), index, 0)
        c.prepare()
        c.play()
    }

    fun shuffleAll() {
        val c = controller ?: return
        val list = _state.value.visibleSongs
        if (list.isEmpty()) return
        c.shuffleModeEnabled = true
        c.setMediaItems(list.map(::toMediaItem), list.indices.random(), 0)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNext() }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPrevious()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _state.update { it.copy(positionMs = ms) }
    }

    /** Jumps to the song at [index] in the current queue. */
    fun playQueueIndex(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0)
        c.play()
    }

    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
    }

    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun skipBy(deltaMs: Long) {
        val c = controller ?: return
        seekTo((c.currentPosition + deltaMs).coerceIn(0, c.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    private fun syncFromPlayer(p: Player) {
        _state.update {
            it.copy(
                currentId = p.currentMediaItem?.mediaId?.toLongOrNull(),
                isPlaying = p.isPlaying,
                positionMs = p.currentPosition.coerceAtLeast(0),
                durationMs = p.duration.takeIf { d -> d > 0 } ?: (it.current?.durationMs ?: 0),
                shuffle = p.shuffleModeEnabled,
                repeatMode = p.repeatMode,
                queue = List(p.mediaItemCount) { i -> p.getMediaItemAt(i).mediaId.toLongOrNull() ?: -1L },
                queueIndex = p.currentMediaItemIndex,
            )
        }
        if (p.isPlaying) startTicker(p) else ticker?.cancel()
    }

    private fun startTicker(p: Player) {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(positionMs = p.currentPosition.coerceAtLeast(0)) }
                delay(500)
            }
        }
    }

    private fun toMediaItem(s: Song) = MediaItem.Builder()
        .setMediaId(s.id.toString())
        .setUri(s.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(s.title)
                .setArtist(s.artist)
                .setAlbumTitle(s.album)
                .setArtworkUri(s.artUri)
                .build(),
        )
        .build()

    override fun onCleared() {
        ticker?.cancel()
        sleepJob?.cancel()
        controller?.run {
            removeListener(listener)
            release()
        }
        super.onCleared()
    }
}
