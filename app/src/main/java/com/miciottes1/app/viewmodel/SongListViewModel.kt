package com.miciottes1.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miciottes1.app.data.SongRepository
import com.miciottes1.app.data.SongSummary
import com.miciottes1.app.data.favKey
import com.miciottes1.app.data.toSummary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface ListUiState {
    data object Loading : ListUiState
    data class Error(val message: String) : ListUiState
    data class Success(
        val songs: List<SongSummary>,
        val newest: List<SongSummary> = emptyList(),
        val popular: List<SongSummary> = emptyList(),
        val languages: List<String> = emptyList(),
        val newestPerLanguage: Map<String, List<SongSummary>> = emptyMap(),
    ) : ListUiState
}

class SongListViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SongRepository(app)

    var uiState by mutableStateOf<ListUiState>(ListUiState.Loading)
        private set

    var query by mutableStateOf("")
        private set

    var lyricSearchResults by mutableStateOf<List<SongSummary>>(emptyList())
        private set

    var isSearchingLyrics by mutableStateOf(false)
        private set

    private var searchJob: Job? = null

    var selectedLanguage by mutableStateOf("")

    var isRefreshing by mutableStateOf(false)
        private set

    var selectedSong by mutableStateOf<SongSummary?>(null)
        private set

    var selectedAlbum by mutableStateOf("")
        private set

    var selectedAlbumImage by mutableStateOf("")
        private set

    init {
        // Observe Room — UI update otomatis saat DB berubah
        viewModelScope.launch {
            repo.songsFlow.collectLatest { entities ->
                if (entities.isEmpty()) {
                    if (uiState !is ListUiState.Error) uiState = ListUiState.Loading
                } else {
                    uiState = buildSuccessState(entities.map { it.toSummary() })
                }
            }
        }
        // Sync di background (delta atau full)
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            isRefreshing = true
            try {
                val result = repo.sync()
                android.util.Log.d("SongListVM", "Sync OK: fetched=${result.fetched} total=${result.total}")
            } catch (e: Exception) {
                android.util.Log.e("SongListVM", "Sync FAILED", e)
                if (repo.count() == 0) {
                    uiState = ListUiState.Error("Gagal memuat data. Periksa koneksi internet.")
                }
            }
            isRefreshing = false
        }
    }

    fun fullResync() {
        viewModelScope.launch {
            isRefreshing = true
            try {
                repo.fullResync()
            } catch (_: Exception) {
                if (repo.count() == 0) {
                    uiState = ListUiState.Error("Gagal memuat data. Periksa koneksi internet.")
                }
            }
            isRefreshing = false
        }
    }

    // Legacy alias
    fun load() = sync()

    fun updateQuery(value: String) {
        query = value
        searchJob?.cancel()
        if (value.trim().length < 3) {
            lyricSearchResults = emptyList()
            isSearchingLyrics = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            isSearchingLyrics = true
            lyricSearchResults = try {
                repo.searchLyrics(value)
            } catch (e: Exception) {
                android.util.Log.e("SongListVM", "Lyric search failed", e)
                emptyList()
            }
            isSearchingLyrics = false
        }
    }

    private fun buildSuccessState(raw: List<SongSummary>): ListUiState.Success {
        val distinct = raw.distinctBy { it.favKey() }
        val alphabetical = distinct.sortedBy { it.judul.lowercase() }
        val newest = distinct.takeLast(10).reversed()
        val popular = alphabetical.shuffled(Random(7)).take(10)

        val langCounts = distinct.filter { it.language.isNotBlank() }
            .groupBy { it.language }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .map { it.key }
        val languages = listOf("") + langCounts

        val newestPerLanguage = langCounts.associateWith { lang ->
            distinct.filter { it.language == lang }
                .sortedByDescending { it.lastmod }
                .take(3)
        }

        return ListUiState.Success(alphabetical, newest, popular, languages, newestPerLanguage)
    }

    fun select(song: SongSummary) {
        selectedSong = song
    }

    fun selectAlbum(name: String, image: String) {
        selectedAlbum = name
        selectedAlbumImage = image
    }
}
