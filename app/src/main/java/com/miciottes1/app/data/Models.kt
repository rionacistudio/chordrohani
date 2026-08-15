package com.miciottes1.app.data

import kotlinx.serialization.Serializable

@Serializable
data class SongSummary(
    val judul: String = "",
    val penyanyi: String = "",
    val base_key: String = "",
    val album: String = "",
    val album_image: String = "",
    val lastmod: String = "",
    val language: String = "",
)

@Serializable
data class Song(
    val judul: String = "",
    val penyanyi: String = "",
    val base_key: String = "",
    val isi_chord: String = "",
    val lastmod: String = "",
    val language: String = "",
    val youtube_url: String = "",
)

fun SongSummary.favKey(): String = "$judul||$penyanyi"

/** Bersihkan judul tampilan: hapus prefix "Chord" dan "(artis)" di akhir. */
fun cleanDisplayTitle(raw: String): String {
    var t = raw.trim()
    t = t.replace(Regex("(?i)^chord\\s+"), "")
    t = t.replace(Regex("\\s*\\([^)]*\\)\\s*$"), "")
    return t.trim().ifBlank { raw.trim() }
}

fun SongSummary.displayTitle(): String = cleanDisplayTitle(judul)
fun Song.displayTitle(): String = cleanDisplayTitle(judul)
