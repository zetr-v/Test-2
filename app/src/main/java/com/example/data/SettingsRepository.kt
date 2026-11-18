package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.StudioThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "webp_studio_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val COMPRESSION_MODE = stringPreferencesKey("compression_mode")
        val QUALITY = intPreferencesKey("quality")
        val RESIZE_PRESET = stringPreferencesKey("resize_preset")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<ConversionSettings> = context.dataStore.data.map { prefs ->
        val mode = prefs[Keys.COMPRESSION_MODE]
            ?.let { runCatching { CompressionMode.valueOf(it) }.getOrNull() }
            ?: CompressionMode.LOSSY
        val quality = (prefs[Keys.QUALITY] ?: 85).coerceIn(1, 100)
        val resize = prefs[Keys.RESIZE_PRESET]
            ?.let { runCatching { ResizePreset.valueOf(it) }.getOrNull() }
            ?: ResizePreset.ORIGINAL

        ConversionSettings(
            mode = mode,
            quality = quality,
            resizePreset = resize,
        )
    }

    val themeModeFlow: Flow<StudioThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]
            ?.let { runCatching { StudioThemeMode.valueOf(it) }.getOrNull() }
            ?: StudioThemeMode.DARK
    }

    suspend fun updateSettings(settings: ConversionSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COMPRESSION_MODE] = settings.mode.name
            prefs[Keys.QUALITY] = settings.quality.coerceIn(1, 100)
            prefs[Keys.RESIZE_PRESET] = settings.resizePreset.name
        }
    }

    suspend fun updateThemeMode(themeMode: StudioThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }
}
