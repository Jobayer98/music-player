package dev.playground.musicplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import dev.playground.musicplayer.LibraryTab
import dev.playground.musicplayer.PlayerUiState
import dev.playground.musicplayer.PlayerViewModel
import dev.playground.musicplayer.Song
import dev.playground.musicplayer.SortMode

@Composable
fun MusicApp(
    state: PlayerUiState,
    hasPermission: Boolean,
    onGrant: () -> Unit,
    vm: PlayerViewModel,
) {
    var showPlayer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    BackHandler(enabled = showPlayer) { showPlayer = false }
    BackHandler(enabled = showQueue) { showQueue = false }
    BackHandler(enabled = !showPlayer && state.group != null) { vm.setGroup(null) }

    Surface(Modifier.fillMaxSize(), color = Color.Transparent, contentColor = Color.White) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                when {
                    !hasPermission -> PermissionPrompt(onGrant)
                    state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = Amber) }
                    else -> Library(state, vm, Modifier.weight(1f))
                }
                state.current?.let { song ->
                    MiniPlayer(song, state, vm, onOpen = { showPlayer = true }, onQueue = { showQueue = true })
                }
            }
            AnimatedVisibility(
                visible = showPlayer && state.current != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                state.current?.let {
                    NowPlaying(it, state, vm, onClose = { showPlayer = false }, onQueue = { showQueue = true })
                }
            }
            AnimatedVisibility(
                visible = showQueue && state.current != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                QueueScreen(state, vm, onClose = { showQueue = false })
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.MusicNote, null, Modifier.size(64.dp), tint = Amber)
        Spacer(Modifier.height(16.dp))
        Text("Play your music", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Allow access to audio files on your phone. Nothing leaves your device.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant, colors = ButtonDefaults.buttonColors(containerColor = Amber)) { Text("Allow access") }
    }
}

@Composable
private fun Library(state: PlayerUiState, vm: PlayerViewModel, modifier: Modifier) {
    var searching by remember { mutableStateOf(false) }
    val songs = state.visibleSongs
    val showGroups = (state.tab == LibraryTab.ALBUMS || state.tab == LibraryTab.ARTISTS) && state.group == null

    Column(modifier.statusBarsPadding()) {
        TopBar(
            state = state,
            vm = vm,
            searching = searching,
            onSearching = {
                searching = it
                if (!it) vm.setQuery("")
            },
        )
        TabRow(state.tab, vm::setTab)

        if (showGroups) {
            GroupList(state, vm)
            return@Column
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.group != null) {
                IconButton({ vm.setGroup(null) }) { Icon(Icons.Default.ArrowBack, "Back") }
                Text(
                    state.group,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text("${songs.size} Songs", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            SleepTimerButton(state.sleepRemainingMs, vm::setSleepTimer)
            SortButton(state.sort, vm::setSort)
        }

        if (state.songs.isEmpty()) {
            EmptyMessage("No music found on this device.")
            return@Column
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PillButton("Shuffle", Icons.Default.Shuffle, Modifier.weight(1f), vm::shuffleAll)
            PillButton("Play", Icons.Default.PlayArrow, Modifier.weight(1f), vm::playAll)
        }

        if (songs.isEmpty()) {
            EmptyMessage(
                if (state.tab == LibraryTab.FAVORITES && state.query.isBlank()) "Tap ⋮ on a song to add it to favorites."
                else "No results.",
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                items(songs, key = { it.id }) { song ->
                    SongRow(
                        song,
                        current = song.id == state.currentId,
                        playing = song.id == state.currentId && state.isPlaying,
                        favorite = song.id in state.favorites,
                        onFavorite = { vm.toggleFavorite(song.id) },
                    ) { vm.play(song) }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    state: PlayerUiState,
    vm: PlayerViewModel,
    searching: Boolean,
    onSearching: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (searching) {
            TextField(
                value = state.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search songs, artists, albums") },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x33FFFFFF),
                    unfocusedContainerColor = Color(0x33FFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Amber,
                ),
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            IconButton({ onSearching(false) }) { Icon(Icons.Default.Close, "Close search") }
        } else {
            Icon(Icons.Default.MusicNote, null, Modifier.padding(start = 12.dp).size(28.dp), tint = Amber)
            Text(
                "Music Player",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
            IconButton({ onSearching(true) }) { Icon(Icons.Default.Search, "Search", Modifier.size(28.dp)) }
        }
    }
}

@Composable
private fun TabRow(selected: LibraryTab, onSelect: (LibraryTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        LibraryTab.entries.forEach { tab ->
            val active = tab == selected
            Column(Modifier.clickable(onClick = { onSelect(tab) }).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    tab.label,
                    fontSize = if (active) 26.sp else 19.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) Color.White else Color.White.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.width(28.dp).height(3.dp).clip(CircleShape)
                        .background(if (active) Color.White else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun PillButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF141432)),
    ) {
        Icon(icon, null, Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GroupList(state: PlayerUiState, vm: PlayerViewModel) {
    val byAlbum = state.tab == LibraryTab.ALBUMS
    val groups = state.songs
        .filter { state.query.isBlank() || it.title.contains(state.query, true) || it.artist.contains(state.query, true) || it.album.contains(state.query, true) }
        .groupBy { if (byAlbum) it.album.ifBlank { "Unknown album" } else it.artist }
        .toList()
        .sortedBy { it.first.lowercase() }
    if (groups.isEmpty()) {
        EmptyMessage("Nothing here yet.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        items(groups, key = { it.first }) { (name, list) ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.setGroup(if (byAlbum) list.first().album else name) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Art(list.first(), Modifier.size(56.dp).clip(if (byAlbum) RoundedCornerShape(12.dp) else CircleShape))
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 17.sp)
                    Text(
                        "${list.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SortButton(current: SortMode, onSelect: (SortMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort") }
        DropdownMenu(open, { open = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label, color = if (mode == current) Amber else MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSelect(mode); open = false },
                )
            }
        }
    }
}

@Composable
private fun SleepTimerButton(remainingMs: Long, onSet: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val active = remainingMs > 0
    Box {
        Row(
            Modifier.clip(CircleShape).clickable { open = true }.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Alarm, "Sleep timer", tint = if (active) Amber else Color.White)
            if (active) {
                Spacer(Modifier.size(4.dp))
                Text(formatTime(remainingMs), style = MaterialTheme.typography.labelMedium, color = Amber)
            }
        }
        DropdownMenu(open, { open = false }) {
            listOf(15, 30, 45, 60).forEach { m ->
                DropdownMenuItem(text = { Text("$m minutes") }, onClick = { onSet(m); open = false })
            }
            if (active) DropdownMenuItem(text = { Text("Turn off") }, onClick = { onSet(0); open = false })
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    current: Boolean,
    playing: Boolean,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)), Alignment.Center) {
            Art(song, Modifier.fillMaxSize())
            if (current) {
                Box(Modifier.fillMaxSize().background(Color(0x99000000)), Alignment.Center) {
                    Icon(if (playing) Icons.Default.Equalizer else Icons.Default.Pause, null, tint = Amber)
                }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 17.sp,
                color = if (current) Amber else Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${song.artist} - ${song.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (favorite) Icon(Icons.Default.Favorite, "Favorite", Modifier.size(18.dp), tint = Amber)
        Box {
            IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (favorite) "Remove from favorites" else "Add to favorites") },
                    leadingIcon = { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null) },
                    onClick = { onFavorite(); menu = false },
                )
            }
        }
    }
}

@Composable
private fun Art(song: Song, modifier: Modifier) {
    SubcomposeAsyncImage(
        model = song.artUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { ArtPlaceholder() },
        error = { ArtPlaceholder() },
    )
}

@Composable
private fun ArtPlaceholder() {
    Box(Modifier.fillMaxSize().background(Color(0x40FFFFFF)), Alignment.Center) {
        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun MiniPlayer(song: Song, state: PlayerUiState, vm: PlayerViewModel, onOpen: () -> Unit, onQueue: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color(0x40000000)).navigationBarsPadding(),
    ) {
        LinearProgressIndicator(
            progress = { if (state.durationMs > 0) state.positionMs / state.durationMs.toFloat() else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = Amber,
            trackColor = Color(0x33FFFFFF),
        )
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFF111111)).padding(5.dp)) {
                Art(song, Modifier.fillMaxSize().clip(CircleShape))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${song.artist} - ${song.album}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                vm::togglePlay,
                Modifier.size(48.dp).border(BorderStroke(2.dp, Color.White), CircleShape),
            ) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isPlaying) "Pause" else "Play")
            }
            IconButton(onQueue) { Icon(Icons.Default.QueueMusic, "Queue", Modifier.size(30.dp)) }
        }
    }
}

@Composable
private fun QueueScreen(state: PlayerUiState, vm: PlayerViewModel, onClose: () -> Unit) {
    // Local copy of the queue so rows follow the finger instantly while the player catches up.
    val order = remember { mutableStateListOf<Song>() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val rowHeight = 68.dp
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }

    LaunchedEffect(state.queue, draggingId) {
        if (draggingId == null) {
            if (order.isNotEmpty()) delay(250)
            order.clear()
            order.addAll(state.queueSongs)
        }
    }

    Box(Modifier.fillMaxSize().background(Gradient)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClose) { Icon(Icons.Default.ArrowBack, "Back") }
                Text(
                    "Queue",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(
                    "${order.size} songs",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Text(
                "Drag the handle to reorder. Tap a song to play it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(order, key = { _, s -> s.id }) { index, song ->
                    val dragging = draggingId == song.id
                    val current = song.id == state.currentId
                    Row(
                        Modifier.fillMaxWidth().height(rowHeight)
                            .then(if (dragging) Modifier else Modifier.animateItem())
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                            .background(if (dragging) Color(0xFF2B1B5A) else Color.Transparent)
                            .clickable { vm.playQueueIndex(index) }
                            .padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Art(song, Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(
                                song.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 16.sp,
                                color = if (current) Amber else Color.White,
                            )
                            Text(
                                song.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (current) Icon(Icons.Default.Equalizer, "Now playing", tint = Amber)
                        IconButton({
                            val i = order.indexOfFirst { it.id == song.id }
                            if (i >= 0) {
                                order.removeAt(i)
                                vm.removeQueueItem(i)
                            }
                        }) {
                            Icon(Icons.Default.Close, "Remove from queue", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            Icons.Default.DragHandle,
                            "Reorder",
                            Modifier.size(48.dp).padding(12.dp).pointerInput(song.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingId = song.id
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        val from = order.indexOfFirst { it.id == song.id }
                                        if (dragOffset > rowPx / 2 && from in 0 until order.lastIndex) {
                                            order.add(from + 1, order.removeAt(from))
                                            vm.moveQueueItem(from, from + 1)
                                            dragOffset -= rowPx
                                        } else if (dragOffset < -rowPx / 2 && from > 0) {
                                            order.add(from - 1, order.removeAt(from))
                                            vm.moveQueueItem(from, from - 1)
                                            dragOffset += rowPx
                                        }
                                    },
                                    onDragEnd = {
                                        draggingId = null
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragOffset = 0f
                                    },
                                )
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlaying(song: Song, state: PlayerUiState, vm: PlayerViewModel, onClose: () -> Unit, onQueue: () -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val duration = state.durationMs.coerceAtLeast(1)
    val shown = if (dragging) dragValue else state.positionMs.toFloat()
    val fav = song.id in state.favorites

    Box(Modifier.fillMaxSize().background(NowPlayingGradient)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClose) { Icon(Icons.Default.KeyboardArrowDown, "Close", Modifier.size(32.dp)) }
                Spacer(Modifier.weight(1f))
                Text("Song", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onQueue) { Icon(Icons.Default.QueueMusic, "Queue", Modifier.size(28.dp)) }
            }
            Spacer(Modifier.weight(1f))
            Art(song, Modifier.fillMaxWidth(0.82f).aspectRatio(1f).clip(RoundedCornerShape(6.dp)))
            Spacer(Modifier.height(32.dp))
            Text(
                song.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${song.artist} - ${song.album}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton({ vm.toggleFavorite(song.id) }) {
                    Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", Modifier.size(28.dp))
                }
                SleepTimerButton(state.sleepRemainingMs, vm::setSleepTimer)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ vm.skipBy(-10_000) }) { Icon(Icons.Default.Replay10, "Back 10 seconds", Modifier.size(30.dp)) }
                ThinSeekBar(
                    fraction = (shown / duration).coerceIn(0f, 1f),
                    onSeek = { dragging = true; dragValue = it * duration },
                    onSeekFinished = { vm.seekTo(dragValue.toLong()); dragging = false },
                    modifier = Modifier.weight(1f),
                )
                IconButton({ vm.skipBy(10_000) }) { Icon(Icons.Default.Forward10, "Forward 10 seconds", Modifier.size(30.dp)) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 48.dp)) {
                Text(formatTime(shown.toLong()), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(vm::toggleShuffle) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (state.shuffle) Amber else Color.White)
                }
                IconButton(vm::previous, Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(38.dp)) }
                FilledIconButton(
                    vm::togglePlay,
                    Modifier.size(76.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color(0xFF141432)),
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                        Modifier.size(42.dp),
                    )
                }
                IconButton(vm::next, Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(38.dp)) }
                IconButton(vm::cycleRepeat) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Repeat",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) Amber else Color.White,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

/** A 3dp seek line with a small round thumb; tap or drag anywhere on it to seek. */
@Composable
private fun ThinSeekBar(
    fraction: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var widthPx by remember { mutableStateOf(1f) }
    fun at(x: Float) = (x / widthPx).coerceIn(0f, 1f)
    Box(
        modifier
            .height(32.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { onSeek(at(it.x)); onSeekFinished() }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { onSeek(at(it.x)) },
                    onHorizontalDrag = { change, _ -> change.consume(); onSeek(at(change.position.x)) },
                    onDragEnd = onSeekFinished,
                    onDragCancel = onSeekFinished,
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color(0x40FFFFFF)))
        Box(Modifier.fillMaxWidth(fraction).height(3.dp).clip(CircleShape).background(Amber))
        val thumb = 12.dp
        val density = LocalDensity.current
        Box(
            Modifier
                .graphicsLayer {
                    translationX = fraction * widthPx - with(density) { thumb.toPx() } / 2
                }
                .size(thumb)
                .clip(CircleShape)
                .background(Amber),
        )
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
