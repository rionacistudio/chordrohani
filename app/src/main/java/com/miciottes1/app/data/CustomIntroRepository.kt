package com.miciottes1.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.introStore by preferencesDataStore(name = "custom_intros")

class CustomIntroRepository(private val context: Context) {

    private fun keyFor(songKey: String) = stringPreferencesKey("intro_$songKey")

    fun introFlow(songKey: String): Flow<String> =
        context.introStore.data.map { prefs ->
            prefs[keyFor(songKey)] ?: ""
        }

    suspend fun save(songKey: String, text: String) {
        context.introStore.edit { prefs ->
            val k = keyFor(songKey)
            if (text.isBlank()) prefs.remove(k) else prefs[k] = text
        }
    }
}
