package com.ondy.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ondy.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    /**
     * DataStore throws [IOException] when the underlying file cannot be read
     * (for example during a crash mid-write). Falling back to empty preferences
     * keeps the app usable instead of terminating the collector.
     */
    private val preferences: Flow<Preferences> = dataStore.data.catch { throwable ->
        if (throwable is IOException) emit(emptyPreferences()) else throw throwable
    }

    val hasSeenWelcome: Flow<Boolean> = preferences.map { preferences ->
        preferences[PreferencesKeys.HAS_SEEN_WELCOME] ?: false
    }

    val runInForeground: Flow<Boolean> = preferences.map { preferences ->
        preferences[PreferencesKeys.RUN_IN_FOREGROUND] ?: true
    }

    val themeMode: Flow<ThemeMode> = preferences.map { preferences ->
        ThemeMode.fromStorage(preferences[PreferencesKeys.THEME_MODE])
    }
    
    suspend fun setHasSeenWelcome(hasSeen: Boolean) {
        // Preference writes must never crash the UI thread; the worst case is
        // that the welcome screen shows again on the next launch.
        runCatching {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.HAS_SEEN_WELCOME] = hasSeen
            }
        }
    }

    suspend fun setRunInForeground(enabled: Boolean) {
        runCatching {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.RUN_IN_FOREGROUND] = enabled
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        runCatching {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME_MODE] = mode.name
            }
        }
    }
    
    private object PreferencesKeys {
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        val RUN_IN_FOREGROUND = booleanPreferencesKey("run_in_foreground")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
