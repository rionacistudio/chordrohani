package com.miciottes1.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object SupabaseRepository {

    private const val BASE_URL = "https://gyyzutfqhkvkdtdlgtzo.supabase.co/rest/v1/tb_chord"
    private const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd5eXp1dGZxaGt2a2R0ZGxndHpvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU2NzU5MzQsImV4cCI6MjEwMTI1MTkzNH0.Xzzm-mgyzyjY5dp-BsKtQ5mPBQEoNVIxKkZmYckwPBg"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun request(url: okhttp3.HttpUrl): Request =
        Request.Builder()
            .url(url)
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer $ANON_KEY")
            .get()
            .build()

    // ponytail: fetch all or delta by lastmod; paginated 1000/batch
    suspend fun fetchSongs(lastmodAfter: String? = null): List<SongSummary> = withContext(Dispatchers.IO) {
        val allSongs = mutableListOf<SongSummary>()
        var offset = 0
        val limit = 1000

        while (true) {
            val urlBuilder = BASE_URL.toHttpUrl().newBuilder()
                .addQueryParameter("select", "judul,penyanyi,base_key,album,album_image,lastmod,language")
                .addQueryParameter("offset", offset.toString())
                .addQueryParameter("limit", limit.toString())

            if (!lastmodAfter.isNullOrEmpty()) {
                urlBuilder.addQueryParameter("lastmod", "gt.$lastmodAfter")
                urlBuilder.addQueryParameter("order", "lastmod.asc")
            }

            client.newCall(request(urlBuilder.build())).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val body = resp.body?.string() ?: throw IOException("Respons kosong")
                val batch = json.decodeFromString<List<SongSummary>>(body)
                allSongs.addAll(batch)

                if (batch.size < limit) break
                offset += limit
            }
        }
        allSongs
    }

    suspend fun fetchDetail(judul: String, penyanyi: String): Song? = withContext(Dispatchers.IO) {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter(
                "select",
                "judul,penyanyi,base_key,isi_chord,lastmod,language,youtube_url",
            )
            .addQueryParameter("judul", "eq.$judul")
            .addQueryParameter("penyanyi", "eq.$penyanyi")
            .addQueryParameter("limit", "1")
            .build()
        client.newCall(request(url)).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Respons kosong")
            json.decodeFromString<List<Song>>(body).firstOrNull()
        }
    }

    suspend fun searchLyrics(query: String): List<SongSummary> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().replace(Regex("[,()*%]"), " ")
        if (cleanQuery.length < 3) return@withContext emptyList()

        val url = BASE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("select", "judul,penyanyi,base_key,album,album_image,lastmod,language")
            .addQueryParameter("isi_chord", "ilike.*$cleanQuery*")
            .addQueryParameter("limit", "200")
            .build()
        client.newCall(request(url)).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Respons kosong")
            json.decodeFromString<List<SongSummary>>(body)
        }
    }
}
