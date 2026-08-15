package com.miciottes1.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.miciottes1.app.R
import com.miciottes1.app.data.ChordTransposer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.PI
import com.miciottes1.app.data.CustomIntroRepository
import com.miciottes1.app.data.FavoritesRepository
import com.miciottes1.app.data.LineType
import com.miciottes1.app.data.SettingsRepository
import com.miciottes1.app.data.SongLine
import com.miciottes1.app.data.SongSummary
import com.miciottes1.app.data.displayTitle
import com.miciottes1.app.data.favKey
import com.miciottes1.app.data.parseSongBody
import com.miciottes1.app.ui.components.ChordDiagramCard
import com.miciottes1.app.ui.components.ErrorState
import com.miciottes1.app.ui.components.LoadingState
import com.miciottes1.app.viewmodel.DetailUiState
import com.miciottes1.app.viewmodel.SongDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SongDetailScreen(
    summary: SongSummary,
    onBack: () -> Unit,
    viewModel: SongDetailViewModel = viewModel(),
) {
    LaunchedEffect(summary) { viewModel.load(summary.judul, summary.penyanyi) }

    val context = LocalContext.current
    val favRepo = remember { FavoritesRepository(context) }
    val settingsRepo = remember { SettingsRepository(context) }
    val introRepo = remember { CustomIntroRepository(context) }

    // Terapkan ukuran font & kecepatan scroll default dari Pengaturan
    LaunchedEffect(Unit) {
        val (size, speed) = settingsRepo.chordDefaultsFlow.first()
        viewModel.applyDefaults(size, speed)
    }
    val favorites by favRepo.favoritesFlow.collectAsState(initial = emptySet())
    val isFavorite = summary.favKey() in favorites
    val scope = rememberCoroutineScope()

    var toolsVisible by remember { mutableStateOf(false) }
    // Panah kiri saat tertutup, panah kanan (panel terbuka dari kanan) saat tools terbuka
    val arrowRotation by animateFloatAsState(targetValue = if (toolsVisible) 180f else 0f, label = "arrow")

    val scrollState = rememberScrollState()

    // Autoscroll engine
    LaunchedEffect(viewModel.autoScroll, viewModel.scrollSpeed) {
        while (viewModel.autoScroll) {
            if (scrollState.value >= scrollState.maxValue) {
                viewModel.stopAutoScroll()
                break
            }
            scrollState.scrollBy(viewModel.scrollSpeed * 0.6f)
            delay(16L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---------- Top bar ----------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            ) {
                AnimatedVisibility(
                    visible = !toolsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onBack() },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                AnimatedVisibility(
                    visible = !toolsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { scope.launch { favRepo.toggle(summary.favKey()) } },
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Favorit",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { toolsVisible = !toolsVisible },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Tools",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowRotation),
                    )
                }
            }

            // ---------- Content ----------
            when (val state = viewModel.uiState) {
                is DetailUiState.Loading -> LoadingState("Memuat chord\u2026")
                is DetailUiState.Error -> ErrorState(state.message) {
                    viewModel.retry(summary.judul, summary.penyanyi)
                }
                is DetailUiState.Success -> {
                    val song = state.song
                    val lines = remember(song.isi_chord) { parseSongBody(song.isi_chord) }
                    val currentKey = ChordTransposer.transposeChordToken(song.base_key, viewModel.transpose)
                    val youtubeVideoId = remember(song.youtube_url) { extractYoutubeVideoId(song.youtube_url) }
                    var playerVisible by remember(song.judul, song.penyanyi) { mutableStateOf(false) }
                    var selectedTokenId by remember(song.isi_chord) { mutableStateOf<String?>(null) }
                    val usedChords = remember(lines, viewModel.transpose) {
                        lines.filter { it.type == LineType.CHORD }
                            .flatMap { it.text.trim().split(Regex("[\\s|¦‖]+")) }
                            .map { it.trim('(', ')', '[', ']', '{', '}', '.', ',', '-') }
                            .filter { it.isNotEmpty() && ChordTransposer.rootNote(it) != null }
                            .map { ChordTransposer.transposeChordToken(it, viewModel.transpose) }
                            .distinct()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp),
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.displayTitle(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 32.sp,
                                )
                                Text(
                                    text = song.penyanyi.ifBlank { "Tidak diketahui" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 12.dp),
                                ) {
                                    Text(
                                        text = "Nada Dasar: $currentKey",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (viewModel.transpose != 0) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.padding(start = 8.dp),
                                        ) {
                                            Text(
                                                text = if (viewModel.transpose > 0) "+${viewModel.transpose}" else "${viewModel.transpose}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(108.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                            ) {
                                if (summary.album_image.isNotBlank()) {
                                    AsyncImage(
                                        model = summary.album_image.replace(
                                            "/assets/img/albums/",
                                            "/album-image/",
                                        ),
                                        contentDescription = "Album ${summary.album}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_music),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(44.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            onClick = {
                                if (youtubeVideoId != null) playerVisible = !playerVisible
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (youtubeVideoId != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (youtubeVideoId != null) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (youtubeVideoId != null) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = when {
                                        youtubeVideoId == null -> "Musik Belum Tersedia"
                                        playerVisible -> "Tutup Pemutar"
                                        else -> "Putar Musik"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (youtubeVideoId != null) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = playerVisible && youtubeVideoId != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            youtubeVideoId?.let { videoId ->
                                val html = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width,initial-scale=1">
                                        <style>
                                            *{margin:0;padding:0;box-sizing:border-box;}
                                            html,body{width:100%;height:100%;background:#000;}
                                            iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:none;}
                                        </style>
                                    </head>
                                    <body>
                                        <iframe
                                            src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0&modestbranding=1"
                                            frameborder="0"
                                            allow="accelerometer;autoplay;clipboard-write;encrypted-media;gyroscope;picture-in-picture;web-share"
                                            allowfullscreen
                                        ></iframe>
                                    </body>
                                    </html>
                                """.trimIndent()
                                AndroidView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(10.dp)),
                                    factory = { ctx ->
                                        android.webkit.WebView(ctx).apply {
                                            setBackgroundColor(android.graphics.Color.BLACK)
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.mediaPlaybackRequiresUserGesture = false
                                            webViewClient = android.webkit.WebViewClient()
                                            loadDataWithBaseURL(
                                                "https://chordku.app/",
                                                html,
                                                "text/html",
                                                "UTF-8",
                                                null,
                                            )
                                        }
                                    },
                                    onRelease = { it.destroy() },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ---------- Daftar diagram chord yang dipakai ----------
                        if (usedChords.isNotEmpty()) {
                            Text(
                                text = "Chords",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                usedChords.forEach { chord ->
                                    ChordDiagramCard(chord = chord)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // ---------- Custom Intro (DataStore per lagu; hide ≠ hapus) ----------
                        val songKey = remember(song.judul, song.penyanyi) {
                            "${song.judul}||${song.penyanyi}"
                        }
                        val savedIntro by introRepo.introFlow(songKey).collectAsState(initial = "")
                        var introDraft by remember(songKey) { mutableStateOf("") }
                        var introLoaded by remember(songKey) { mutableStateOf(false) }
                        // Editor expand; isi tetap ada di DataStore sampai user hapus
                        var introEditing by remember(songKey) { mutableStateOf(false) }
                        LaunchedEffect(songKey, savedIntro) {
                            if (!introLoaded) {
                                introDraft = savedIntro
                                introLoaded = true
                            } else if (savedIntro != introDraft && !introEditing) {
                                // Sync dari storage jika diubah di luar sesi edit
                                introDraft = savedIntro
                            }
                        }
                        val fieldValue = remember(introDraft, viewModel.transpose) {
                            ChordTransposer.transposeLine(introDraft, viewModel.transpose)
                        }
                        val hasIntro = introDraft.isNotBlank()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                        ) {
                            Text(
                                text = "Custom Intro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { introEditing = !introEditing },
                            ) {
                                Icon(
                                    imageVector = if (introEditing) Icons.Default.Remove else Icons.Default.Add,
                                    contentDescription = if (introEditing) "Tutup editor" else "Edit intro",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            if (hasIntro && !introEditing) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "Hapus",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) {
                                            introDraft = ""
                                            scope.launch { introRepo.save(songKey, "") }
                                        }
                                        .padding(horizontal = 4.dp),
                                )
                            }
                        }

                        // Isi selalu ditampilkan (ikut transpose) sampai dihapus user
                        if (hasIntro && !introEditing) {
                            Text(
                                text = fieldValue,
                                fontFamily = FontFamily.Monospace,
                                fontSize = viewModel.fontSize.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                            )
                        }

                        AnimatedVisibility(
                            visible = introEditing,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column {
                                OutlinedTextField(
                                    value = fieldValue,
                                    onValueChange = { newText ->
                                        val base = ChordTransposer.transposeLine(
                                            newText,
                                            -viewModel.transpose,
                                        )
                                        introDraft = base
                                        scope.launch { introRepo.save(songKey, base) }
                                    },
                                    placeholder = {
                                        Text(
                                            text = "Contoh: G  D  Em  C",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = viewModel.fontSize.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    ),
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (hasIntro) {
                                    Text(
                                        text = "Hapus intro",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) {
                                                introDraft = ""
                                                introEditing = false
                                                scope.launch { introRepo.save(songKey, "") }
                                            },
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        val blocks = remember(lines) { groupLines(lines) }
                        val sections = remember(blocks) { groupIntoSections(blocks) }
                        var collapsedSections by remember(song.isi_chord) {
                            mutableStateOf(setOf<String>())
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            sections.forEachIndexed { sectionIndex, section ->
                                val sectionKey = "${sectionIndex}_${section.title}"
                                val isCollapsed = sectionKey in collapsedSections

                                if (section.title != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) {
                                                collapsedSections =
                                                    if (isCollapsed) collapsedSections - sectionKey
                                                    else collapsedSections + sectionKey
                                            }
                                            .padding(top = 14.dp, bottom = 6.dp),
                                    ) {
                                        Text(
                                            text = section.title.uppercase(),
                                            fontSize = (viewModel.fontSize - 2f).sp,
                                            letterSpacing = 1.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isCollapsed) "Tampilkan" else "Sembunyikan",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .rotate(if (isCollapsed) -90f else 0f),
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = !isCollapsed || section.title == null,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    Column {
                                        section.blocks.forEachIndexed { blockIndex, block ->
                                            val lineId = "s$sectionIndex-b$blockIndex"
                                            when (block) {
                                                is LineBlock.Blank ->
                                                    Spacer(modifier = Modifier.height((viewModel.fontSize * 0.6f).dp))
                                                is LineBlock.Header -> Unit
                                                is LineBlock.ChordLyric -> InteractiveChordLyricPair(
                                                    chordLine = block.chord,
                                                    lyricLine = block.lyric,
                                                    transpose = viewModel.transpose,
                                                    fontSize = viewModel.fontSize,
                                                    lineId = lineId,
                                                    selectedTokenId = selectedTokenId,
                                                    onSelect = { selectedTokenId = it },
                                                )
                                                is LineBlock.PlainLyric -> Text(
                                                    text = block.text,
                                                    fontSize = viewModel.fontSize.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(bottom = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(160.dp))
                    }
                }
            }
        }

        // ---------- Bar transpose nada dasar (bawah layar) ----------
        val successState = viewModel.uiState as? DetailUiState.Success
        if (successState != null) {
            val baseRoot = remember(successState.song.base_key) {
                ChordTransposer.rootNote(successState.song.base_key).orEmpty()
            }
            val currentKey = ChordTransposer.transposeChordToken(
                successState.song.base_key,
                viewModel.transpose,
            )
            if (baseRoot.isNotEmpty()) {
                TransposeKeyBar(
                    baseRoot = baseRoot,
                    currentKey = currentKey,
                    onSelectKey = { targetKey ->
                        viewModel.applyTranspose(
                            ChordTransposer.stepsBetween(baseRoot, targetKey),
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        }

        // ---------- Floating tools panel horizontal (muncul di kiri tombol tools) ----------
        AnimatedVisibility(
            visible = toolsVisible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 58.dp),
        ) {
            ToolsPanel(
                viewModel = viewModel,
                onPlusWhole = { viewModel.applyTranspose(viewModel.transpose + 2) },
                onPlusHalf = { viewModel.applyTranspose(viewModel.transpose + 1) },
                onMinusHalf = { viewModel.applyTranspose(viewModel.transpose - 1) },
                onMinusWhole = { viewModel.applyTranspose(viewModel.transpose - 2) },
            )
        }

        // ---------- Kontrol kecepatan autoscroll (kanan bawah, di atas bar transpose) ----------
        AnimatedVisibility(
            visible = viewModel.autoScroll,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 72.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(6.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                            .clickable { viewModel.speedUp() },
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Percepat",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = "${viewModel.scrollSpeed}x",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                            .clickable { viewModel.speedDown() },
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Perlambat",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun extractYoutubeVideoId(url: String): String? {
    val value = url.trim()
    if (value.isEmpty()) return null
    val match = Regex(
        "(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/))([A-Za-z0-9_-]{11})",
        RegexOption.IGNORE_CASE,
    ).find(value)
    return match?.groupValues?.getOrNull(1)
        ?: value.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
}

@Composable
private fun TransposeKeyBar(
    baseRoot: String,
    currentKey: String,
    onSelectKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentRoot = remember(currentKey) {
        ChordTransposer.rootNote(currentKey) ?: currentKey
    }
    val keys = ChordTransposer.DISPLAY_KEYS
    val chipSize = 38.dp
    val scrollState = rememberScrollState()

    LaunchedEffect(currentRoot) {
        val idx = keys.indexOf(currentRoot)
        if (idx >= 0) {
            scrollState.animateScrollTo((idx * 46).coerceAtLeast(0))
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF111111),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                keys.forEach { key ->
                    val selected = key == currentRoot
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(chipSize)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else Color(0xFF111111),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelectKey(key) },
                    ) {
                        Text(
                            text = key,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FineButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
            ) { onClick() },
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
            maxLines = 1,
        )
    }
}

@Composable
private fun ToolsPanel(
    viewModel: SongDetailViewModel,
    onPlusWhole: () -> Unit,
    onPlusHalf: () -> Unit,
    onMinusHalf: () -> Unit,
    onMinusWhole: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToolSquareButton(
            label = "A+",
            onClick = { viewModel.fontUp() },
        )
        ToolSquareButton(
            label = "A\u2212",
            onClick = { viewModel.fontDown() },
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                .clickable { viewModel.toggleAutoScroll() },
        ) {
            Icon(
                imageVector = if (viewModel.autoScroll) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Autoscroll",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }

        ToolSquareButton(label = "+1", onClick = onPlusWhole)
        ToolSquareButton(label = "+½", onClick = onPlusHalf)
        ToolSquareButton(label = "-½", onClick = onMinusHalf)
        ToolSquareButton(label = "-1", onClick = onMinusWhole)
    }
}

private data class ChordLyricChunk(
    val chord: String,
    val lyric: String,
    val chordTokenIndex: Int = -1
)

private sealed interface LineBlock {
    data class ChordLyric(val chord: String, val lyric: String) : LineBlock
    data class Header(val text: String) : LineBlock
    data class PlainLyric(val text: String) : LineBlock
    data object Blank : LineBlock
}

private data class SongSection(
    val title: String?,
    val blocks: List<LineBlock>,
)

/** Kelompokkan block per section (HEADER → isi sampai HEADER berikutnya). */
private fun groupIntoSections(blocks: List<LineBlock>): List<SongSection> {
    val sections = mutableListOf<SongSection>()
    var title: String? = null
    var content = mutableListOf<LineBlock>()

    fun flush() {
        if (title != null || content.isNotEmpty()) {
            sections.add(SongSection(title, content.toList()))
            content = mutableListOf()
        }
    }

    for (block in blocks) {
        when (block) {
            is LineBlock.Header -> {
                flush()
                title = block.text
            }
            else -> content.add(block)
        }
    }
    flush()
    return sections
}

private fun groupLines(lines: List<SongLine>): List<LineBlock> {
    val blocks = mutableListOf<LineBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when (line.type) {
            LineType.BLANK -> blocks.add(LineBlock.Blank)
            LineType.HEADER -> blocks.add(LineBlock.Header(line.text))
            LineType.CHORD -> {
                val next = lines.getOrNull(i + 1)
                if (next != null && next.type == LineType.LYRIC) {
                    blocks.add(LineBlock.ChordLyric(line.text, next.text))
                    i++
                } else {
                    blocks.add(LineBlock.ChordLyric(line.text, ""))
                }
            }
            LineType.LYRIC -> blocks.add(LineBlock.PlainLyric(line.text))
        }
        i++
    }
    return blocks
}

private fun processChordLyricPair(chordLine: String, lyricLine: String): List<ChordLyricChunk> {
    val cleanChord = chordLine.trimEnd()
    val cleanLyric = lyricLine.trimEnd()
    if (cleanChord.isEmpty() && cleanLyric.isEmpty()) return emptyList()

    val maxLen = maxOf(cleanChord.length, cleanLyric.length)
    val cLine = cleanChord.padEnd(maxLen, ' ')
    val lLine = cleanLyric.padEnd(maxLen, ' ')

    val chordRegex = Regex("\\S+")
    val matches = chordRegex.findAll(cleanChord).toList()

    val forbiddenSplits = mutableSetOf<Int>()
    for (m in matches) {
        for (i in m.range.first + 1..m.range.last) {
            forbiddenSplits.add(i)
        }
    }

    val splitPoints = mutableSetOf(0, maxLen)

    for (i in 1 until maxLen) {
        if (lLine[i - 1] == ' ' && i !in forbiddenSplits) splitPoints.add(i)
    }

    // Selalu split di awal tiap chord token agar 1 chunk = 1 chord (transpose & klik akurat)
    for (m in matches) {
        splitPoints.add(m.range.first)
    }

    val sortedSplits = splitPoints.sorted()
    val chunks = mutableListOf<ChordLyricChunk>()
    var tokenCounter = 0

    for (i in 0 until sortedSplits.size - 1) {
        val start = sortedSplits[i]
        val end = sortedSplits[i + 1]

        // Simpan substring asli (termasuk spasi) agar posisi alignment tidak bergeser
        val rawChord = cLine.substring(start, end)
        val chordTrimmed = rawChord.trimEnd()
        val pureToken = Regex("\\S+").find(chordTrimmed)?.value.orEmpty()
        val tIndex = if (pureToken.isNotEmpty()) tokenCounter++ else -1
        val lyricChunk = lLine.substring(start, end)

        chunks.add(ChordLyricChunk(chordTrimmed, lyricChunk, tIndex))
    }
    return chunks
}

// ponytail: max 30 karakter per baris, split di batas chunk (sudah align dengan chord)
private fun groupChunksByWidth(chunks: List<ChordLyricChunk>, maxWidth: Int): List<List<ChordLyricChunk>> {
    if (chunks.isEmpty()) return emptyList()
    val groups = mutableListOf<MutableList<ChordLyricChunk>>()
    var current = mutableListOf<ChordLyricChunk>()
    var width = 0
    for (chunk in chunks) {
        val w = chunk.lyric.length
        if (current.isNotEmpty() && width + w > maxWidth) {
            groups.add(current)
            current = mutableListOf(chunk)
            width = w
        } else {
            current.add(chunk)
            width += w
        }
    }
    if (current.isNotEmpty()) groups.add(current)
    return groups
}

/**
 * Baris chord interaktif: setiap chord bisa diklik dan menampilkan
 * tooltip berisi foto diagram chord tepat di atas chord yang diklik.
 * Maksimal 30 karakter per baris, word-wrap tanpa memotong chord.
 */
@Composable
private fun InteractiveChordLyricPair(
    chordLine: String,
    lyricLine: String,
    transpose: Int,
    fontSize: Float,
    lineId: String,
    selectedTokenId: String?,
    onSelect: (String?) -> Unit,
) {
    val chunks = remember(chordLine, lyricLine) {
        processChordLyricPair(chordLine, lyricLine)
    }
    val hasLyric = lyricLine.isNotBlank()
    val groups = remember(chunks) { groupChunksByWidth(chunks, 30) }

    Column {
        groups.forEach { group ->
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth(),
            ) {
                group.forEach { chunk ->
                    Column(modifier = Modifier.padding(bottom = 2.dp)) {
                        if (chunk.chord.isNotEmpty()) {
                            val tokenId = "$lineId-token-${chunk.chordTokenIndex}"
                            // transposeLine: tiap token \S+ di-transpose (chunk bisa berisi >1 chord)
                            val transposed = remember(chunk.chord, transpose) {
                                ChordTransposer.transposeLine(chunk.chord, transpose)
                            }
                            val displayed = remember(transposed) {
                                transposed.replace(' ', '\u00A0')
                            }
                            val primaryToken = remember(transposed) {
                                Regex("\\S+").find(transposed)?.value.orEmpty()
                            }
                            val isSelected = selectedTokenId == tokenId

                            Box {
                                if (isSelected && primaryToken.isNotEmpty()) {
                                    ChordTooltip(
                                        chord = primaryToken,
                                        onDismiss = { onSelect(null) },
                                    )
                                }
                                Text(
                                    text = displayed,
                                    fontSize = fontSize.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                    softWrap = false,
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                            RoundedCornerShape(4.dp),
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { onSelect(if (isSelected) null else tokenId) },
                                )
                            }
                        } else {
                            Text(
                                text = " ",
                                fontSize = fontSize.sp,
                                fontFamily = FontFamily.Monospace,
                                softWrap = false,
                            )
                        }

                        if (hasLyric && chunk.lyric.isNotEmpty()) {
                            Text(
                                text = chunk.lyric.replace(' ', '\u00A0'),
                                fontSize = fontSize.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                softWrap = false,
                                lineHeight = (fontSize * 1.5f).sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Popup diagram chord yang muncul tepat di atas chord yang diklik. */
@Composable
private fun ChordTooltip(chord: String, onDismiss: () -> Unit) {
    val positionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = (anchorBounds.left + anchorBounds.width / 2 - popupContentSize.width / 2)
                    .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val above = anchorBounds.top - popupContentSize.height - 8
                val y = if (above >= 0) above else anchorBounds.bottom + 8
                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        ChordDiagramCard(chord = chord)
    }
}

@Composable
private fun ToolSquareButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ToolCircleIconText(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .background(bg, CircleShape)
            .clickable { onClick() },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = fg,
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
