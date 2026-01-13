package com.mehchow.letyoucook.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mehchow.letyoucook.data.model.AuthResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val EMAIL = stringPreferencesKey("email")

        private val USERNAME = stringPreferencesKey("username")
    }

    // React-like "Global State" as a Flow
    val accessToken: Flow<String?> = dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = dataStore.data.map { it[REFRESH_TOKEN] }
    val email: Flow<String?> = dataStore.data.map { it[EMAIL] }

    val username: Flow<String?> = dataStore.data.map { it[USERNAME] }

    suspend fun saveAuthData(response: AuthResponse) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = response.accessToken
            prefs[REFRESH_TOKEN] = response.refreshToken
            prefs[USERNAME] = response.username
        }
    }

    suspend fun clearAuthData() {
        dataStore.edit { it.clear() }
    }
}