package com.miciottes1.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode") // system | light | dark
    private val fontKey = floatPreferencesKey("default_font_size")
    private val speedKey = intPreferencesKey("default_scroll_speed")

    val themeModeFlow: Flow<String> = context.settingsStore.data.map { prefs ->
        prefs[themeKey] ?: "system"
    }

    val fontSizeFlow: Flow<Float> = context.settingsStore.data.map { prefs ->
        prefs[fontKey] ?: 15f
    }

    val scrollSpeedFlow: Flow<Int> = context.settingsStore.data.map { prefs ->
        prefs[speedKey] ?: 2
    }

    val chordDefaultsFlow: Flow<Pair<Float, Int>> = context.settingsStore.data.map { prefs ->
        Pair(prefs[fontKey] ?: 15f, prefs[speedKey] ?: 2)
    }

    suspend fun setThemeMode(mode: String) {
        context.settingsStore.edit { it[themeKey] = mode }
    }

    suspend fun setFontSize(size: Float) {
        context.settingsStore.edit { it[fontKey] = size }
    }

    suspend fun setScrollSpeed(speed: Int) {
        context.settingsStore.edit { it[speedKey] = speed }
    }
}
