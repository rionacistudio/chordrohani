package com.miciottes1.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favStore by preferencesDataStore(name = "favorites")

class FavoritesRepository(private val context: Context) {

    private val favKey = stringSetPreferencesKey("fav_songs")

    val favoritesFlow: Flow<Set<String>> = context.favStore.data.map { prefs ->
        prefs[favKey] ?: emptySet()
    }

    suspend fun toggle(id: String) {
        context.favStore.edit { prefs ->
            val current = prefs[favKey] ?: emptySet()
            prefs[favKey] = if (id in current) current - id else current + id
        }
    }
}
