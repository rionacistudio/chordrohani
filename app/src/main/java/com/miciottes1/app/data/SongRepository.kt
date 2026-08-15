package com.miciottes1.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bridge: Room = cache utama (offline), Supabase = delta sync.
 */
class SongRepository(context: Context) {

    private val dao = ChordRoomDb.get(context).chordDao()

    val songsFlow = dao.getAllSongsFlow()

    suspend fun count(): Int = withContext(Dispatchers.IO) { dao.count() }

    suspend fun getLastSync(): String? = withContext(Dispatchers.IO) { dao.getLastSync() }

    suspend fun getAll(): List<SongEntity> = withContext(Dispatchers.IO) { dao.getAllSongs() }

    suspend fun searchLyrics(query: String): List<SongSummary> =
        SupabaseRepository.searchLyrics(query)

    suspend fun getDetail(judul: String, penyanyi: String): SongEntity? =
        withContext(Dispatchers.IO) { dao.getDetail(judul, penyanyi) }

    suspend fun fetchDetailFromApi(judul: String, penyanyi: String): Song? {
        val song = SupabaseRepository.fetchDetail(judul, penyanyi) ?: return null
        val existing = withContext(Dispatchers.IO) { dao.getDetail(judul, penyanyi) }
        val entity = SongEntity(
            judul = song.judul,
            penyanyi = song.penyanyi,
            base_key = song.base_key,
            album = existing?.album ?: "",
            album_image = existing?.album_image ?: "",
            lastmod = song.lastmod,
            isi_chord = song.isi_chord,
            language = song.language,
        )
        withContext(Dispatchers.IO) { dao.upsertAll(listOf(entity)) }
        return song
    }

    // ponytail: delta sync by lastmod; full fetch if DB empty or lastmod blank
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        val lastSync = dao.getLastSync()
        val effectiveLastSync = if (lastSync.isNullOrEmpty()) null else lastSync
        val songs = SupabaseRepository.fetchSongs(lastmodAfter = effectiveLastSync)
        android.util.Log.d("SongRepo", "Fetched ${songs.size} songs from API")
        if (songs.isNotEmpty()) {
            // Upsert dalam batch 500 untuk hindari SQLite too many variables
            songs.chunked(500).forEach { batch ->
                dao.upsertAll(batch.map { it.toEntity() })
            }
        }
        val total = dao.count()
        android.util.Log.d("SongRepo", "DB total after sync: $total")
        SyncResult(fetched = songs.size, total = total)
    }

    suspend fun fullResync(): SyncResult = withContext(Dispatchers.IO) {
        dao.clear()
        val songs = SupabaseRepository.fetchSongs(lastmodAfter = null)
        dao.upsertAll(songs.map { it.toEntity() })
        SyncResult(fetched = songs.size, total = dao.count())
    }
}

data class SyncResult(val fetched: Int, val total: Int)

fun SongSummary.toEntity() = SongEntity(
    judul = judul,
    penyanyi = penyanyi,
    base_key = base_key,
    album = album,
    album_image = album_image,
    lastmod = lastmod,
    isi_chord = "",
    language = language,
)

fun SongEntity.toSummary() = SongSummary(
    judul = judul,
    penyanyi = penyanyi,
    base_key = base_key,
    album = album,
    album_image = album_image,
    lastmod = lastmod,
    language = language,
)
