package com.miciottes1.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.miciottes1.app.data.ServiceListRepository
import com.miciottes1.app.data.SongSummary
import com.miciottes1.app.data.displayTitle
import com.miciottes1.app.data.favKey
import com.miciottes1.app.ui.components.EmptyState
import com.miciottes1.app.ui.components.ErrorState
import com.miciottes1.app.ui.components.LoadingState
import com.miciottes1.app.ui.theme.Dark
import com.miciottes1.app.ui.theme.Green
import com.miciottes1.app.ui.theme.LightGray
import com.miciottes1.app.viewmodel.ListUiState
import com.miciottes1.app.viewmodel.SongListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceListScreen(
    viewModel: SongListViewModel,
    onSongClick: (SongSummary) -> Unit,
) {
    val context = LocalContext.current
    val serviceIds by ServiceListRepository.listFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var showPicker by remember { mutableStateOf(false) }
    var pickerQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "List Pelayanan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Lagu untuk ibadah hari Minggu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (serviceIds.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                            .clickable { scope.launch { ServiceListRepository.clear() } },
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Kosongkan list",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            when (val state = viewModel.uiState) {
                is ListUiState.Loading -> {
                    if (serviceIds.isEmpty()) {
                        LoadingState("Memuat list\u2026")
                    } else {
                        // List sudah ada di DataStore, tapi song DB belum ke-load
                        ServiceListContent(
                            serviceIds = serviceIds,
                            songs = emptyList(),
                            ServiceListRepository = ServiceListRepository,
                            scope = scope,
                            density = density,
                            onSongClick = {},
                        )
                    }
                }
                is ListUiState.Error -> {
                    if (serviceIds.isEmpty()) {
                        ErrorState(state.message) { viewModel.load() }
                    } else {
                        ServiceListContent(
                            serviceIds = serviceIds,
                            songs = emptyList(),
                            ServiceListRepository = ServiceListRepository,
                            scope = scope,
                            density = density,
                            onSongClick = {},
                        )
                    }
                }
                is ListUiState.Success -> {
                    ServiceListContent(
                        serviceIds = serviceIds,
                        songs = state.songs,
                        ServiceListRepository = ServiceListRepository,
                        scope = scope,
                        density = density,
                        onSongClick = onSongClick,
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showPicker = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Lagu")
        }
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Pilih Lagu Pelayanan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                OutlinedTextField(
                    value = pickerQuery,
                    onValueChange = { pickerQuery = it },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (pickerQuery.isNotEmpty()) {
                            IconButton(onClick = { pickerQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )

                val state = viewModel.uiState
                if (state is ListUiState.Success) {
                    val filtered = if (pickerQuery.isBlank()) state.songs
                    else state.songs.filter {
                        it.judul.contains(pickerQuery, ignoreCase = true) ||
                            it.penyanyi.contains(pickerQuery, ignoreCase = true)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp, end = 20.dp, bottom = 32.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = filtered,
                            key = { _, s -> s.favKey() },
                        ) { _, song ->
                            val added = song.favKey() in serviceIds
                            PickerRow(
                                song = song,
                                added = added,
                                onToggle = {
                                    scope.launch { ServiceListRepository.toggle(song.favKey()) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceListContent(
    serviceIds: List<String>,
    songs: List<SongSummary>,
    ServiceListRepository: ServiceListRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    density: androidx.compose.ui.unit.Density,
    onSongClick: (SongSummary) -> Unit,
) {
    val byKey = remember(songs) { songs.associateBy { it.favKey() } }
    val listSongs = serviceIds.mapNotNull { byKey[it] }

    if (serviceIds.isEmpty()) {
        EmptyState(
            title = "List masih kosong",
            subtitle = "Ketuk tombol + untuk menyusun lagu pelayanan hari Minggu",
        )
        return
    }

    if (listSongs.isEmpty()) {
        // IDs ada di DataStore tapi song DB belum ke-load — tampilkan loading placeholder
        LoadingState("Memuat lagu\u2026")
        return
    }

    val localList = remember { mutableStateListOf<SongSummary>() }
    var isDragging by remember { mutableStateOf(false) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(listSongs, isDragging) {
        if (!isDragging) {
            localList.clear()
            localList.addAll(listSongs)
        }
    }

    val lazyListState = rememberLazyListState()
    val itemHeightPx = with(density) { 76.dp.toPx() }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(
            items = localList,
            key = { _, s -> s.favKey() },
        ) { index, song ->
            val key = song.favKey()
            val isDragged = draggedKey == key
            val elevation by animateDpAsState(
                targetValue = if (isDragged) 10.dp else 1.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "elev",
            )
            val scale by animateFloatAsState(
                targetValue = if (isDragged) 1.04f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "scale",
            )

            ServiceSongCard(
                order = index + 1,
                song = song,
                isDragged = isDragged,
                elevation = elevation,
                scale = scale,
                dragOffsetY = if (isDragged) dragOffsetY else 0f,
                onClick = {
                    if (!isDragging) onSongClick(song)
                },
                onRemove = {
                    scope.launch { ServiceListRepository.remove(key) }
                },
                onDragStart = {
                    isDragging = true
                    draggedKey = key
                    dragOffsetY = 0f
                },
                onDrag = { deltaY ->
                    dragOffsetY += deltaY
                    val from = localList.indexOfFirst { it.favKey() == draggedKey }
                    if (from < 0) return@ServiceSongCard

                    val targetOffset = (dragOffsetY / itemHeightPx).toInt()
                    val to = (from + targetOffset).coerceIn(0, localList.lastIndex)
                    if (to != from) {
                        val item = localList.removeAt(from)
                        localList.add(to, item)
                        dragOffsetY -= (to - from) * itemHeightPx
                    }
                },
                onDragEnd = {
                    val finalOrder = localList.map { it.favKey() }
                    isDragging = false
                    draggedKey = null
                    dragOffsetY = 0f
                    scope.launch { ServiceListRepository.setOrder(finalOrder) }
                },
                modifier = Modifier.animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
            )
        }
    }
}

@Composable
private fun ServiceSongCard(
    order: Int,
    song: SongSummary,
    isDragged: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    scale: Float,
    dragOffsetY: Float,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isDragged) 8.dp else 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragged) 10f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = dragOffsetY
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                        )
                    },
            ) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Geser",
                    tint = if (isDragged) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Green),
            ) {
                Text(
                    text = "$order",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = song.displayTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${song.penyanyi.ifBlank { "Tidak diketahui" }} \u2022 Nada ${song.base_key}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Dark)
                    .clickable(onClick = onRemove),
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Hapus dari list",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    song: SongSummary,
    added: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.displayTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.penyanyi.ifBlank { "Tidak diketahui" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(0.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                    .background(if (added) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
            ) {
                Icon(
                    imageVector = if (added) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (added) "Sudah di list" else "Tambah ke list",
                    tint = if (added) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
