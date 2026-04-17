package com.ondy.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    val hasSeenWelcome: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_SEEN_WELCOME] ?: false
    }
    
    suspend fun setHasSeenWelcome(hasSeen: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_WELCOME] = hasSeen
        }
    }
    
    private object PreferencesKeys {
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
    }
}
