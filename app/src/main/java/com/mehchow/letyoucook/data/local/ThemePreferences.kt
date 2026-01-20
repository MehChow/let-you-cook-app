package com.mehchow.letyoucook.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages theme preference persistence using DataStore.
 * Similar to AsyncStorage in React Native.
 */
@Singleton
class ThemePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    /**
     * Flow that emits the current theme preference.
     * null = follow system default, true = dark, false = light
     */
    val isDarkTheme: Flow<Boolean?> = dataStore.data.map { preferences ->
        preferences[IS_DARK_THEME]
    }

    /**
     * Save theme preference.
     */
    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_DARK_THEME] = isDark
        }
    }

    /**
     * Clear theme preference (revert to system default).
     */
    suspend fun clearThemePreference() {
        dataStore.edit { preferences ->
            preferences.remove(IS_DARK_THEME)
        }
    }
}
