package com.rionaci.pujikord.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Music2
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rionaci.pujikord.R
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.rionaci.pujikord.data.ChordTransposer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.PI
import com.rionaci.pujikord.data.CustomIntroRepository
import com.rionaci.pujikord.data.FavoritesRepository
import com.rionaci.pujikord.data.LineType
import com.rionaci.pujikord.data.SettingsRepository
import com.rionaci.pujikord.data.SongLine
import com.rionaci.pujikord.data.SongSummary
import com.rionaci.pujikord.data.albumImageUrl
import com.rionaci.pujikord.data.displayTitle
import com.rionaci.pujikord.data.favKey
import com.rionaci.pujikord.data.parseSongBody
import com.rionaci.pujikord.ui.components.ChordDiagramCard
import com.rionaci.pujikord.ui.components.ErrorState
import com.rionaci.pujikord.ui.components.LoadingState
import com.rionaci.pujikord.viewmodel.DetailUiState
import com.rionaci.pujikord.viewmodel.SongDetailViewModel
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
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

    val shareSong = (viewModel.uiState as? DetailUiState.Success)?.song

    val scrollState = rememberScrollState()
    var controlsVisible by remember { mutableStateOf(true) }

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
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
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
                        imageVector = Lucide.ChevronLeft,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

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
                        imageVector = Lucide.Star,
                        contentDescription = "Favorit",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                var shareMenuOpen by remember { mutableStateOf(false) }
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = shareSong != null,
                            ) { shareMenuOpen = true },
                    ) {
                        Icon(
                            imageVector = Lucide.Share2,
                            contentDescription = "Bagikan chord",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = shareMenuOpen,
                        onDismissRequest = { shareMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Bagikan teks") },
                            onClick = {
                                shareMenuOpen = false
                                val song = shareSong ?: return@DropdownMenuItem
                                val shareText = buildString {
                                    appendLine(song.displayTitle())
                                    appendLine(song.penyanyi.ifBlank { "Tidak diketahui" })
                                    appendLine("Key: " + ChordTransposer.transposeChordToken(song.base_key, viewModel.transpose))
                                    appendLine()
                                    parseSongBody(song.isi_chord).forEach { line ->
                                        appendLine(
                                            if (line.type == LineType.CHORD) {
                                                ChordTransposer.transposeLine(line.text, viewModel.transpose)
                                            } else {
                                                line.text
                                            }
                                        )
                                    }
                                }
                                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                }
                                runCatching {
                                    context.startActivity(android.content.Intent.createChooser(send, "Bagikan chord"))
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Ekspor PDF") },
                            onClick = {
                                shareMenuOpen = false
                                val song = shareSong ?: return@DropdownMenuItem
                                runCatching {
                                    shareSongAsPdf(context, song, viewModel.transpose)
                                }
                            },
                        )
                    }
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
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    controlsVisible = !controlsVisible
                                }
                            }
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
                                        model = albumImageUrl(summary.album_image),
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
                                    Lucide.Play,
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
                                YouTubePlayerViewCompose(
                                    videoId = videoId,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(10.dp)),
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
                                    imageVector = if (introEditing) Lucide.Minus else Lucide.Plus,
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
                                            imageVector = Lucide.ChevronDown,
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

                        Spacer(modifier = Modifier.height(24.dp))
                        AndroidView(
                            factory = { ctx ->
                                com.google.android.gms.ads.AdView(ctx).apply {
                                    setAdSize(com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE)
                                    adUnitId = "ca-app-pub-1727373702562428/5150931424"
                                    loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
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
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    TransposeKeyBar(
                        baseRoot = baseRoot,
                        currentKey = currentKey,
                        onSelectKey = { targetKey ->
                            viewModel.applyTranspose(
                                ChordTransposer.stepsBetween(baseRoot, targetKey),
                            )
                        },
                        autoScroll = viewModel.autoScroll,
                        scrollSpeed = viewModel.scrollSpeed,
                        onToggleAutoScroll = { viewModel.toggleAutoScroll() },
                        onSelectSpeed = { viewModel.selectScrollSpeed(it) },
                        onSpeedUp = { viewModel.speedUp() },
                        onSpeedDown = { viewModel.speedDown() },
                        onFontUp = { viewModel.fontUp() },
                        onFontDown = { viewModel.fontDown() },
                        modifier = Modifier.fillMaxWidth(),
                    )
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
private fun YouTubePlayerViewCompose(videoId: String, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
                enableAutomaticInitialization = false
                lifecycleOwner.lifecycle.addObserver(this)
                val listener = object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId, 0f)
                    }
                }
                val options = IFramePlayerOptions.Builder(ctx)
                    .controls(1)
                    .autoplay(1)
                    .build()
                initialize(listener, true, options)
            }
        },
        onRelease = { playerView ->
            lifecycleOwner.lifecycle.removeObserver(playerView)
            playerView.release()
        },
    )
}

@Composable
private fun TransposeKeyBar(
    baseRoot: String,
    currentKey: String,
    onSelectKey: (String) -> Unit,
    autoScroll: Boolean,
    scrollSpeed: Int,
    onToggleAutoScroll: () -> Unit,
    onSelectSpeed: (Int) -> Unit,
    onSpeedUp: () -> Unit,
    onSpeedDown: () -> Unit,
    onFontUp: () -> Unit,
    onFontDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentRoot = remember(currentKey) {
        ChordTransposer.rootNote(currentKey) ?: currentKey
    }
    val keys = ChordTransposer.DISPLAY_KEYS
    val chipSize = 48.dp
    val scrollState = rememberScrollState()
    var keyPickerVisible by remember { mutableStateOf(false) }
    var speedPickerVisible by remember { mutableStateOf(false) }
    val accent = Color(0xFFF3FF83)

    LaunchedEffect(currentRoot, keyPickerVisible) {
        if (keyPickerVisible) {
            val idx = keys.indexOf(currentRoot)
            if (idx >= 0) {
                scrollState.animateScrollTo((idx * 46).coerceAtLeast(0))
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF111111),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                keyPickerVisible = !keyPickerVisible
                                speedPickerVisible = false
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Lucide.Music2,
                            contentDescription = "Pilih key",
                            tint = if (keyPickerVisible) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "KEY $currentRoot",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (keyPickerVisible) FontWeight.Bold else FontWeight.Normal,
                            color = if (keyPickerVisible) Color.White else Color.White.copy(alpha = 0.5f),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                keyPickerVisible = false
                                if (autoScroll) {
                                    onToggleAutoScroll()
                                    speedPickerVisible = false
                                } else {
                                    speedPickerVisible = !speedPickerVisible
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = if (autoScroll) Lucide.Pause else Lucide.Play,
                            contentDescription = if (autoScroll) "Pause" else "Play",
                            tint = if (autoScroll || speedPickerVisible) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(if (autoScroll) 26.dp else 20.dp),
                        )
                        if (!autoScroll) {
                            Text(
                                text = "PLAY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (speedPickerVisible) FontWeight.Bold else FontWeight.Normal,
                                color = if (speedPickerVisible) Color.White else Color.White.copy(alpha = 0.5f),
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("−1" to { onSelectKey(nextKey(currentRoot, keys, -2)) },
                                   "+1" to { onSelectKey(nextKey(currentRoot, keys, +2)) })
                                .forEach { (label, action) ->
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = action,
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f),
                                        )
                                    }
                                }
                        }
                        Text(
                            text = "TRANSPOSE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("A−" to onFontDown, "A+" to onFontUp)
                                .forEach { (label, action) ->
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = action,
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f),
                                        )
                                    }
                                }
                        }
                        Text(
                            text = "RESIZE TEXT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
            }


            AnimatedVisibility(visible = keyPickerVisible) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF111111),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        keys.forEach { key ->
                            val selected = key == currentRoot
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(width = 42.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        color = if (selected) Color.White
                                        else Color.White.copy(alpha = 0.12f),
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        onSelectKey(key)
                                        keyPickerVisible = false
                                    },
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF111111)
                                    else Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = speedPickerVisible) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF111111),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        (1..5).forEach { speed ->
                            val selected = speed == scrollSpeed
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        color = if (selected) Color.White
                                        else Color.White.copy(alpha = 0.12f),
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        onSelectSpeed(speed)
                                        if (!autoScroll) onToggleAutoScroll()
                                        speedPickerVisible = false
                                    },
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF111111)
                                    else Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillControlButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun nextKey(current: String, keys: List<String>, step: Int): String {
    val idx = keys.indexOf(current)
    if (idx < 0) return current
    val next = (idx + step + keys.size) % keys.size
    return keys[next]
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

/**
 * Pisahkan baris chord+lirik menjadi segmen visual dengan alignment karakter 1:1.
 * Setiap segmen menyimpan substring chord dan lirik dengan panjang identik,
 * sehingga spasi, pipe, dan notasi tidak pernah hilang atau tergeser.
 */
private fun processChordLyricPair(chordLine: String, lyricLine: String): List<ChordLyricChunk> {
    val cleanChord = chordLine.trimEnd()
    val cleanLyric = lyricLine.trimEnd()
    if (cleanChord.isEmpty() && cleanLyric.isEmpty()) return emptyList()

    val maxLen = maxOf(cleanChord.length, cleanLyric.length)
    if (maxLen == 0) return emptyList()
    val cLine = cleanChord.padEnd(maxLen, ' ')
    val lLine = cleanLyric.padEnd(maxLen, ' ')

    val chordMatches = Regex("\\S+").findAll(cLine).toList()

    val chordLocked = BooleanArray(maxLen)
    for (m in chordMatches) {
        var idx = m.range.first + 1
        while (idx <= m.range.last && idx < maxLen) {
            chordLocked[idx] = true
            idx++
        }
    }

    val lyricLocked = BooleanArray(maxLen)
    var inWord = false
    for (idx in 0 until maxLen) {
        if (lLine[idx] != ' ') {
            if (inWord) lyricLocked[idx] = true
            inWord = true
        } else {
            inWord = false
        }
    }

    val splits = sortedSetOf(0, maxLen)
    for (idx in 1 until maxLen) {
        if (chordLocked[idx] || lyricLocked[idx]) continue
        val wordStart = lLine[idx - 1] == ' ' && lLine[idx] != ' '
        val chordStart = cLine[idx - 1] == ' ' && cLine[idx] != ' '
        val pipeStart = cLine[idx] in "|¦‖" || lLine[idx] in "|¦‖"
        if (wordStart || chordStart || pipeStart) splits.add(idx)
    }

    val points = splits.toList()
    val chunks = mutableListOf<ChordLyricChunk>()
    var tokenCursor = 0

    for (i in 0 until points.size - 1) {
        val start = points[i]
        val end = points[i + 1]
        val chordPart = cLine.substring(start, end)
        val lyricPart = lLine.substring(start, end)
        val hasChord = chordPart.any { !it.isWhitespace() }
        val tIndex = if (hasChord) tokenCursor++ else -1
        chunks.add(
            ChordLyricChunk(
                chord = chordPart,
                lyric = lyricPart,
                chordTokenIndex = tIndex,
            )
        )
    }

    return chunks
}

/**
 * Baris chord interaktif.
 * - Segment dirender berurutan sehingga spasi asli tetap utuh.
 * - Wrap otomatis mengikuti lebar layar lewat FlowRow.
 * - Setiap token chord dapat diklik untuk menampilkan diagram.
 */
@OptIn(ExperimentalLayoutApi::class)
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

    FlowRow(
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        chunks.forEach { chunk ->
            Column {
                if (chunk.chord.isNotEmpty()) {
                    ChordSegmentRow(
                        chordPart = ChordTransposer.transposeLine(chunk.chord, transpose),
                        lineId = lineId,
                        tokenIndex = chunk.chordTokenIndex,
                        fontSize = fontSize,
                        selectedTokenId = selectedTokenId,
                        onSelect = onSelect,
                    )
                } else if (hasLyric) {
                    Text(
                        text = " ",
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        softWrap = false,
                        lineHeight = (fontSize * 1.0f).sp,
                    )
                }

                if (hasLyric && chunk.lyric.isNotEmpty()) {
                    Text(
                        text = chunk.lyric.replace(' ', '\u00A0'),
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                        lineHeight = (fontSize * 1.1f).sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChordSegmentRow(
    chordPart: String,
    lineId: String,
    tokenIndex: Int,
    fontSize: Float,
    selectedTokenId: String?,
    onSelect: (String?) -> Unit,
) {
    val tokenId = "$lineId-token-$tokenIndex"
    val isSelected = tokenIndex >= 0 && selectedTokenId == tokenId

    Row(verticalAlignment = Alignment.Top) {
        var index = 0
        while (index < chordPart.length) {
            val char = chordPart[index]
            if (char.isWhitespace()) {
                var end = index
                while (end < chordPart.length && chordPart[end].isWhitespace()) end++
                Text(
                    text = chordPart.substring(index, end).replace(' ', '\u00A0'),
                    fontSize = fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false,
                    lineHeight = (fontSize * 1.0f).sp,
                )
                index = end
                continue
            }
            var end = index
            while (end < chordPart.length && !chordPart[end].isWhitespace()) end++
            val token = chordPart.substring(index, end)
            val isChord = ChordTransposer.rootNote(token) != null
            if (isChord && tokenIndex >= 0) {
                Box {
                    if (isSelected) {
                        ChordTooltip(chord = token.trim('(', ')', '[', ']'), onDismiss = { onSelect(null) })
                    }
                    Text(
                        text = token,
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                        lineHeight = (fontSize * 1.0f).sp,
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
                    text = token,
                    fontSize = fontSize.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = false,
                    lineHeight = (fontSize * 1.0f).sp,
                )
            }
            index = end
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

private fun shareSongAsPdf(context: Context, song: com.rionaci.pujikord.data.Song, transpose: Int) {
    val currentKey = ChordTransposer.transposeChordToken(song.base_key, transpose)
    val lines = parseSongBody(song.isi_chord)

    val plainLines = buildList {
        add(song.displayTitle())
        add(song.penyanyi.ifBlank { "Tidak diketahui" })
        add("Nada Dasar: $currentKey")
        add("")
        lines.forEach { line ->
            if (line.type == LineType.CHORD) {
                add(ChordTransposer.transposeLine(line.text, transpose))
            } else if (line.text.isNotBlank()) {
                add(line.text)
            } else {
                add("")
            }
        }
    }

    val pageWidth = 595
    val pageHeight = 842
    val marginLeft = 40f
    val marginTop = 48f
    val lineHeight = 16f
    val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = 18f
        isAntiAlias = true
    }
    val headerPaint = Paint().apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textSize = 12f
        isAntiAlias = true
    }
    val chordPaint = Paint().apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = 12f
        isAntiAlias = true
    }
    val lyricPaint = Paint().apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textSize = 12f
        isAntiAlias = true
    }

    val document = PdfDocument()
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = marginTop

    fun newPage() {
        document.finishPage(page)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = marginTop
    }

    plainLines.forEachIndexed { index, text ->
        val paint = when {
            index == 0 -> titlePaint
            index <= 2 -> headerPaint
            else -> if (index % 2 == 0) lyricPaint else lyricPaint
        }
        val isChordLine = index > 3 && runCatching {
            lineTypeOf(lines, index - 4)
        }.getOrDefault(false)
        if (y > pageHeight - marginTop) newPage()
        canvas.drawText(text, marginLeft, y, if (isChordLine) chordPaint else paint)
        y += lineHeight
    }

    document.finishPage(page)

    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safeName = song.judul.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifBlank { "chord" }
    val file = File(dir, "$safeName.pdf")
    file.outputStream().use { document.writeTo(it) }
    document.close()

    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Ekspor chord ke PDF"))
}

private fun lineTypeOf(lines: List<SongLine>, index: Int): Boolean =
    lines.getOrNull(index)?.type == LineType.CHORD
