package com.miciottes1.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.serviceStore by preferencesDataStore(name = "service_list")

/**
 * Menyimpan daftar lagu (berurutan) untuk dimainkan saat pelayanan hari Minggu.
 * Singleton — hanya boleh ada satu instance per app.
 */
object ServiceListRepository {

    private lateinit var appContext: Context
    private val listKey = stringPreferencesKey("sunday_service_songs")
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val store get() = appContext.serviceStore

    val listFlow: Flow<List<String>> get() = store.data.map { prefs ->
        prefs[listKey]?.let { raw ->
            runCatching { json.decodeFromString<List<String>>(raw) }.getOrElse { emptyList() }
        } ?: emptyList()
    }

    private fun decode(raw: String?): List<String> =
        raw?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrElse { emptyList() } }
            ?: emptyList()

    suspend fun toggle(id: String) {
        store.edit { prefs ->
            val current = decode(prefs[listKey])
            val updated = if (id in current) current - id else current + id
            prefs[listKey] = json.encodeToString(updated)
        }
    }

    suspend fun remove(id: String) {
        store.edit { prefs ->
            val current = decode(prefs[listKey])
            prefs[listKey] = json.encodeToString(current - id)
        }
    }

    suspend fun clear() {
        store.edit { prefs ->
            prefs[listKey] = json.encodeToString(emptyList<String>())
        }
    }

    suspend fun reorder(fromIndex: Int, toIndex: Int) {
        store.edit { prefs ->
            val current = decode(prefs[listKey]).toMutableList()
            if (fromIndex in current.indices && toIndex in current.indices) {
                val item = current.removeAt(fromIndex)
                current.add(toIndex, item)
                prefs[listKey] = json.encodeToString(current)
            }
        }
    }

    suspend fun setOrder(ids: List<String>) {
        store.edit { prefs ->
            prefs[listKey] = json.encodeToString(ids)
        }
    }
}
