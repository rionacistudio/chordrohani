package com.miciottes1.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.miciottes1.app.R
import com.miciottes1.app.data.FavoritesRepository
import com.miciottes1.app.data.SettingsRepository
import com.miciottes1.app.data.SongSummary
import com.miciottes1.app.data.displayTitle
import com.miciottes1.app.data.favKey
import com.miciottes1.app.ui.components.EmptyState
import com.miciottes1.app.ui.components.ErrorState
import com.miciottes1.app.ui.components.LoadingState
import com.miciottes1.app.ui.components.SongCard
import com.miciottes1.app.ui.theme.Dark
import com.miciottes1.app.ui.theme.Green
import com.miciottes1.app.ui.theme.JetBrainsMono
import com.miciottes1.app.ui.theme.LightGray
import com.miciottes1.app.viewmodel.ListUiState
import com.miciottes1.app.viewmodel.SongListViewModel
import kotlinx.coroutines.launch

// ── Palet referensi ──────────────────────────────────────────────────
private val AccentGreen = Color(0xFF0A8A5A)

private data class Palette(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val sub: Color,
)

@Composable
private fun referencePalette(isDark: Boolean) =
    if (isDark) Palette(
        bg = Color(0xFF111111),
        card = Color(0xFF1A1A1A),
        ink = Color.White,
        sub = Color(0xFF9A9A9A),
    ) else Palette(
        bg = Color(0xFFE7E7E7),
        card = Color(0xFF111111),
        ink = Color(0xFF111111),
        sub = Color(0xFF8A8A8A),
    )

@Composable
fun HomeScreen(
    viewModel: SongListViewModel,
    onSongClick: (SongSummary) -> Unit,
    onAlbumClick: (String, String) -> Unit,
    onChordDictionary: () -> Unit,
    onRequestChord: () -> Unit,
    onDonation: () -> Unit,
) {
    val context = LocalContext.current
    val favRepo = remember { FavoritesRepository(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val favorites by favRepo.favoritesFlow.collectAsState(initial = emptySet())
    val themeMode by settingsRepo.themeModeFlow.collectAsState(initial = "system")
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val pal = referencePalette(isDark)
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pal.bg)
            .blur(if (menuOpen) 10.dp else 0.dp),
    ) {
        // ═══════════ 2. TOP HEADER ═══════════
        TopHeader(
            pal = pal,
            isRefreshing = viewModel.isRefreshing,
            onMenu = { menuOpen = true },
            onSearch = { showSearch = !showSearch },
            onBell = { scope.launch { settingsRepo.setThemeMode(if (isDark) "light" else "dark") } },
            isDark = isDark,
        )

        // Search field (toggle dari tombol search)
        if (showSearch || viewModel.query.isNotBlank()) {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::updateQuery,
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = pal.sub, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (viewModel.query.isNotEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, pal.ink, RoundedCornerShape(10.dp))
                                .clickable { viewModel.updateQuery("") },
                        ) {
                            Icon(Icons.Default.Close, "Hapus", tint = pal.ink, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                placeholder = { Text("Cari lagu...", color = pal.sub, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = pal.ink,
                    unfocusedBorderColor = pal.ink,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        when (val state = viewModel.uiState) {
            is ListUiState.Loading -> LoadingState("Memuat chord…")
            is ListUiState.Error -> ErrorState(state.message) { viewModel.load() }
            is ListUiState.Success -> {
                if (viewModel.query.isNotBlank()) {
                    val localMatches = state.songs.filter {
                        val matchesQuery = it.judul.contains(viewModel.query, ignoreCase = true) ||
                            it.penyanyi.contains(viewModel.query, ignoreCase = true)
                        val matchesLang = viewModel.selectedLanguage.isBlank() ||
                            it.language == viewModel.selectedLanguage
                        matchesQuery && matchesLang
                    }
                    val lyricMatches = viewModel.lyricSearchResults.filter {
                        viewModel.selectedLanguage.isBlank() || it.language == viewModel.selectedLanguage
                    }
                    val filtered = (localMatches + lyricMatches).distinctBy { it.favKey() }
                    if (filtered.isEmpty()) {
                        EmptyState("Tidak ditemukan", "Coba kata kunci lain")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 100.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filtered, key = { it.favKey() }) { song ->
                                SongCard(
                                    song = song,
                                    isFavorite = song.favKey() in favorites,
                                    onClick = { onSongClick(song) },
                                    onToggleFavorite = { scope.launch { favRepo.toggle(song.favKey()) } },
                                )
                            }
                        }
                    }
                } else {
                    val albumData = remember(state.songs) {
                        val map = LinkedHashMap<String, Pair<String, Int>>()
                        state.songs.forEach { s ->
                            if (s.album.isNotBlank()) {
                                val existing = map[s.album]
                                map[s.album] = (s.album_image to ((existing?.second ?: 0) + 1))
                            }
                        }
                        map.entries.sortedByDescending { it.value.second }
                            .take(10)
                            .map { Triple(it.key, it.value.first, it.value.second) }
                    }

                    val langLabel = mapOf(
                        "" to "Semua",
                        "ind" to "Indonesia",
                        "eng" to "English",
                        "rus" to "Russian",
                        "zho" to "Chinese",
                        "jpn" to "Japanese",
                        "kor" to "Korean",
                        "spa" to "Spanish",
                        "por" to "Portuguese",
                        "fra" to "French",
                        "deu" to "German",
                        "ita" to "Italian",
                        "tha" to "Thai",
                        "vie" to "Vietnamese",
                        "msa" to "Malay",
                        "tl" to "Filipino",
                        "hin" to "Hindi",
                        "ara" to "Arabic",
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // Language filter chips
                        if (state.languages.size > 1) {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(state.languages) { lang ->
                                        val selected = viewModel.selectedLanguage == lang
                                        val label = langLabel[lang] ?: lang.uppercase()
                                        val count = if (lang.isBlank()) state.songs.size
                                            else state.songs.count { it.language == lang }
                                        Surface(
                                            onClick = { viewModel.selectedLanguage = if (selected) "" else lang },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (selected) pal.card else Color.Transparent,
                                            border = BorderStroke(1.dp, if (selected) AccentGreen else pal.ink),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selected) Color.White else pal.ink,
                                                )
                                                Text(
                                                    text = " ($count)",
                                                    fontSize = 12.sp,
                                                    color = if (selected) Color.White.copy(alpha = 0.72f) else pal.sub,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Filtered songs when language selected
                        if (viewModel.selectedLanguage.isNotBlank()) {
                            val filteredSongs = state.songs.filter { it.language == viewModel.selectedLanguage }
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                SectionHeader(
                                    title = langLabel[viewModel.selectedLanguage] ?: viewModel.selectedLanguage.uppercase(),
                                    pal = pal,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            items(filteredSongs, key = { it.favKey() }) { song ->
                                RecentlyListenedItem(
                                    song = song,
                                    isFavorite = song.favKey() in favorites,
                                    pal = pal,
                                    onClick = { onSongClick(song) },
                                    onToggleFavorite = { scope.launch { favRepo.toggle(song.favKey()) } },
                                )
                            }
                        } else {
                            // ═══════════ 3. ALBUM POPULER ═══════════
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                                SectionHeader(
                                    title = "Album Populer",
                                    pal = pal,
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(albumData.size) { i ->
                                        val (name, image, count) = albumData[i]
                                        AlbumCard(
                                            name = name,
                                            image = image,
                                            count = count,
                                            pal = pal,
                                            onClick = { onAlbumClick(name, image) },
                                        )
                                    }
                                }
                            }

                            // ═══════════ 4. SECTION HEADER ═══════════
                            item {
                                Spacer(modifier = Modifier.height(26.dp))
                                SectionHeader(
                                    title = "Chord Populer",
                                    pal = pal,
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // ═══════════ 5. FEATURED CARDS ═══════════
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(state.popular, key = { it.favKey() }) { song ->
                                        FeaturedMusicCard(
                                            song = song,
                                            pal = pal,
                                            onClick = { onSongClick(song) },
                                        )
                                    }
                                }
                            }

                            // ═══════════ 6. CHORD TERBARU INDONESIA & INGGRIS ═══════════
                            listOf("ind", "eng").forEach { lang ->
                                val songs = state.newestPerLanguage[lang].orEmpty()
                                if (songs.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(28.dp))
                                        SectionHeader(
                                            title = "Chord Terbaru • ${langLabel[lang] ?: lang.uppercase()}",
                                            pal = pal,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    items(songs, key = { it.favKey() }) { song ->
                                        RecentlyListenedItem(
                                            song = song,
                                            isFavorite = song.favKey() in favorites,
                                            pal = pal,
                                            onClick = { onSongClick(song) },
                                            onToggleFavorite = { scope.launch { favRepo.toggle(song.favKey()) } },
                                        )
                                    }
                                }
                            }

                            // ═══════════ 7. PILIHAN CEPAT ═══════════
                            item {
                                Spacer(modifier = Modifier.height(28.dp))
                                Text(
                                    text = "Pilihan Cepat",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JetBrainsMono,
                                    color = pal.ink,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            item {
                                val quickPickPages = remember(state.songs) {
                                    state.songs.shuffled().take(15).chunked(5)
                                }
                                val pagerState = rememberPagerState(
                                    pageCount = { quickPickPages.size },
                                )

                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(330.dp),
                                ) { page ->
                                    Column {
                                        quickPickPages[page].forEach { song ->
                                            QuickPickItem(
                                                song = song,
                                                isFavorite = song.favKey() in favorites,
                                                pal = pal,
                                                onClick = { onSongClick(song) },
                                                onToggleFavorite = {
                                                    scope.launch { favRepo.toggle(song.favKey()) }
                                                },
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 4.dp),
                                ) {
                                    repeat(quickPickPages.size) { index ->
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .size(if (pagerState.currentPage == index) 18.dp else 6.dp, 6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    if (pagerState.currentPage == index) AccentGreen
                                                    else pal.sub.copy(alpha = 0.45f),
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (menuOpen) {
        HomeMenuSheet(
            isDark = isDark,
            onDismiss = { menuOpen = false },
            onChordDictionary = {
                menuOpen = false
                onChordDictionary()
            },
            onRequestChord = {
                menuOpen = false
                onRequestChord()
            },
            onDonation = {
                menuOpen = false
                onDonation()
            },
        )
    }
}

// ═══════════ 2. TOP HEADER ═══════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopHeader(
    pal: Palette,
    isRefreshing: Boolean,
    isDark: Boolean,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onBell: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
    ) {
        // Hamburger menu — sama style dengan search / dark mode
        HeaderSquareButton(pal = pal, onClick = onMenu) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Menu",
                tint = pal.ink,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Tombol search — rounded square, border hitam tipis
        HeaderSquareButton(pal = pal, onClick = onSearch) {
            Icon(Icons.Default.Search, "Cari", tint = pal.ink, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Tombol dark mode — rounded square, border hitam tipis
        HeaderSquareButton(pal = pal, onClick = onBell) {
            Icon(
                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Mode gelap",
                tint = pal.ink,
                modifier = Modifier.size(18.dp),
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeMenuSheet(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onChordDictionary: () -> Unit,
    onRequestChord: () -> Unit,
    onDonation: () -> Unit,
) {
    val panelColor = if (isDark) Dark else LightGray
    val detailColor = if (isDark) LightGray else Dark
    val decorationColor = if (isDark) LightGray else Green
    ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = Modifier.padding(horizontal = 38.dp, vertical = 20.dp),
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.Transparent,
            contentColor = detailColor,
            dragHandle = null,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(374.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(panelColor)
                    .border(1.dp, LightGray, RoundedCornerShape(18.dp))
                    .padding(bottom = 18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 24.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(detailColor),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BottomMenuButton(
                            label = "KAMUS CHORD",
                            isDark = isDark,
                            onClick = onChordDictionary,
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        BottomMenuButton(
                            label = "REQUEST CHORD GITAR",
                            isDark = isDark,
                            onClick = onRequestChord,
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        BottomMenuButton(
                            label = "DONASI",
                            isDark = isDark,
                            onClick = onDonation,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                ) {
                    Text("◆", fontSize = 9.sp, color = decorationColor)
                    Text("CHORDKU", fontSize = 7.sp, fontFamily = JetBrainsMono, color = decorationColor)
                    Text("◆  ◆  ◆", fontSize = 9.sp, color = decorationColor)
                    Text("ROHANI", fontSize = 7.sp, fontFamily = JetBrainsMono, color = decorationColor)
                    Text("◆", fontSize = 9.sp, color = decorationColor)
                }
            }
        }
}

@Composable
private fun BottomMenuButton(
    label: String,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color.Transparent else Green,
        contentColor = Color.White,
        border = if (isDark) BorderStroke(1.dp, LightGray) else null,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMono,
            color = if (isDark) LightGray else Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun HeaderSquareButton(
    pal: Palette,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, pal.ink, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        content()
    }
}

// ═══════════ 3. ALBUM CARD ═══════════
@Composable
private fun AlbumCard(
    name: String,
    image: String,
    count: Int,
    pal: Palette,
    onClick: () -> Unit,
) {
    val cardInk = Color(0xFFE7E7E7)
    val cardSub = Color(0xFFAAAAAA)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = pal.card,
        border = BorderStroke(1.dp, pal.ink),
        modifier = Modifier.width(132.dp),
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .background(pal.card),
                ) {
                    if (image.isNotBlank()) {
                        AsyncImage(
                            model = image.replace("/assets/img/albums/", "/album-image/"),
                            contentDescription = name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_music),
                            contentDescription = null,
                            tint = cardInk.copy(alpha = 0.35f),
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.Center),
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 12.dp)) {
                    Text(
                        text = name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$count lagu",
                        fontSize = 11.sp,
                        color = cardSub,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ═══════════ 4. SECTION HEADER ═══════════
@Composable
private fun SectionHeader(
    title: String,
    pal: Palette,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    showRefresh: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMono,
            color = pal.ink,
        )
        if (showRefresh) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AccentGreen)
                    .clickable(onClick = onRefresh),
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ═══════════ 5. FEATURED CARD ═══════════
@Composable
private fun FeaturedMusicCard(
    song: SongSummary,
    pal: Palette,
    onClick: () -> Unit,
) {
    val cardInk = Color(0xFFE7E7E7)
    val cardSub = Color(0xFFAAAAAA)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = pal.card,
        border = BorderStroke(1.dp, pal.ink),
        modifier = Modifier.width(132.dp),
    ) {
        Box {
            Column {
                // Artwork area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .background(pal.card),
                ) {
                    if (song.album_image.isNotBlank()) {
                        AsyncImage(
                            model = song.album_image.replace("/assets/img/albums/", "/album-image/"),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_music),
                            contentDescription = null,
                            tint = cardInk.copy(alpha = 0.35f),
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.Center),
                        )
                    }
                }
                // Title + artist
                Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 12.dp)) {
                    Text(
                        text = song.displayTitle(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.penyanyi.ifBlank { "Tidak diketahui" },
                        fontSize = 11.sp,
                        color = cardSub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPickItem(
    song: SongSummary,
    isFavorite: Boolean,
    pal: Palette,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentGreen),
        ) {
            if (song.album_image.isNotBlank()) {
                AsyncImage(
                    model = song.album_image.replace("/assets/img/albums/", "/album-image/"),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_music),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = song.displayTitle(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.penyanyi.ifBlank { "Tidak diketahui" },
                fontSize = 12.sp,
                color = pal.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { menuOpen = true },
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = pal.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit") },
                    onClick = {
                        menuOpen = false
                        onToggleFavorite()
                    },
                )
            }
        }
    }
}

// ═══════════ 6. RECENTLY LISTENED ITEM ═══════════
@Composable
private fun RecentlyListenedItem(
    song: SongSummary,
    isFavorite: Boolean,
    pal: Palette,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        // Artwork album 48dp
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary),
        ) {
            if (song.album_image.isNotBlank()) {
                AsyncImage(
                    model = song.album_image.replace("/assets/img/albums/", "/album-image/"),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_music),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayTitle(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Check hijau emerald
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = song.penyanyi.ifBlank { "Tidak diketahui" },
                    fontSize = 13.sp,
                    color = pal.sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Three-dot menu
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { menuOpen = true },
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = pal.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit") },
                    onClick = {
                        menuOpen = false
                        onToggleFavorite()
                    },
                )
            }
        }
    }
}
