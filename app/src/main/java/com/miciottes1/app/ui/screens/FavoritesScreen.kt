package com.miciottes1.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miciottes1.app.data.FavoritesRepository
import com.miciottes1.app.data.SongSummary
import com.miciottes1.app.data.favKey
import com.miciottes1.app.ui.components.EmptyState
import com.miciottes1.app.ui.components.ErrorState
import com.miciottes1.app.ui.components.LoadingState
import com.miciottes1.app.ui.components.SongCard
import com.miciottes1.app.ui.theme.Dark
import com.miciottes1.app.ui.theme.Green
import com.miciottes1.app.ui.theme.LightGray
import com.miciottes1.app.viewmodel.ListUiState
import com.miciottes1.app.viewmodel.SongListViewModel
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    viewModel: SongListViewModel,
    onSongClick: (SongSummary) -> Unit,
) {
    val context = LocalContext.current
    val favRepo = remember { FavoritesRepository(context) }
    val favorites by favRepo.favoritesFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
            Text(
                text = "Favorit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Lagu yang kamu tandai bintang",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (val state = viewModel.uiState) {
            is ListUiState.Loading -> LoadingState("Memuat favorit\u2026")
            is ListUiState.Error -> ErrorState(state.message) { viewModel.load() }
            is ListUiState.Success -> {
                val favSongs = state.songs.filter { it.favKey() in favorites }
                if (favSongs.isEmpty()) {
                    EmptyState(
                        title = "Belum ada favorit",
                        subtitle = "Ketuk ikon bintang pada lagu untuk menyimpannya di sini",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(favSongs, key = { it.favKey() }) { song ->
                            SongCard(
                                song = song,
                                isFavorite = true,
                                onClick = { onSongClick(song) },
                                onToggleFavorite = { scope.launch { favRepo.toggle(song.favKey()) } },
                            )
                        }
                    }
                }
            }
        }
    }
}
