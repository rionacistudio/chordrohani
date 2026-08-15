package com.miciottes1.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miciottes1.app.data.Song
import com.miciottes1.app.data.SongRepository
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Error(val message: String) : DetailUiState
    data class Success(val song: Song) : DetailUiState
}

class SongDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SongRepository(app)

    var uiState by mutableStateOf<DetailUiState>(DetailUiState.Loading)
        private set

    var transpose by mutableIntStateOf(0)
        private set

    var fontSize by mutableFloatStateOf(15f)
        private set

    var autoScroll by mutableStateOf(false)
        private set

    var scrollSpeed by mutableIntStateOf(2)
        private set

    private var loadedFor: String? = null
    private var defaultsApplied = false

    /** Terapkan preferensi default dari Pengaturan (sekali per pembukaan lagu). */
    fun applyDefaults(defaultFontSize: Float, defaultSpeed: Int) {
        if (defaultsApplied) return
        defaultsApplied = true
        fontSize = defaultFontSize
        scrollSpeed = defaultSpeed
    }

    fun load(judul: String, penyanyi: String) {
        val key = "$judul||$penyanyi"
        if (loadedFor == key && uiState is DetailUiState.Success) return
        loadedFor = key
        uiState = DetailUiState.Loading
        viewModelScope.launch {
            uiState = try {
                // Chord dapat dibaca dari Room, tetapi metadata media selalu dicoba dari API.
                val cached = repo.getDetail(judul, penyanyi)
                val fresh = runCatching { repo.fetchDetailFromApi(judul, penyanyi) }.getOrNull()
                val song = fresh ?: if (cached != null && cached.isi_chord.isNotEmpty()) {
                    Song(
                        judul = cached.judul,
                        penyanyi = cached.penyanyi,
                        base_key = cached.base_key,
                        isi_chord = cached.isi_chord,
                        lastmod = cached.lastmod,
                        language = cached.language,
                    )
                } else null
                if (song != null) DetailUiState.Success(song)
                else DetailUiState.Error("Lagu tidak ditemukan")
            } catch (e: Exception) {
                DetailUiState.Error(e.message ?: "Gagal memuat lagu")
            }
        }
    }

    fun retry(judul: String, penyanyi: String) {
        loadedFor = null
        load(judul, penyanyi)
    }

    fun transposeUp() { if (transpose < 11) transpose += 1 }
    fun transposeDown() { if (transpose > -11) transpose -= 1 }
    fun resetTranspose() { transpose = 0 }
    fun applyTranspose(steps: Int) { transpose = steps.coerceIn(-11, 11) }

    fun fontUp() { if (fontSize < 26f) fontSize += 1f }
    fun fontDown() { if (fontSize > 11f) fontSize -= 1f }

    fun toggleAutoScroll() { autoScroll = !autoScroll }
    fun stopAutoScroll() { autoScroll = false }
    fun speedUp() { if (scrollSpeed < 5) scrollSpeed += 1 }
    fun speedDown() { if (scrollSpeed > 1) scrollSpeed -= 1 }
}
