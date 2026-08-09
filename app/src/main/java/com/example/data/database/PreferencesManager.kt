package com.example.data.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "barberia_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        private val USER_PHONE = stringPreferencesKey("user_phone")
        private val USER_ROLE = stringPreferencesKey("user_role") // 'client', 'admin'
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_FULL_NAME = stringPreferencesKey("user_full_name")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val LAST_SYNC = longPreferencesKey("last_sync")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_DARK_MODE] ?: false
    }

    val userPhone: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_PHONE] ?: ""
    }

    val userRole: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_ROLE] ?: ""
    }

    val userId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_ID] ?: ""
    }

    val userFullName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_FULL_NAME] ?: ""
    }

    val authToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AUTH_TOKEN] ?: ""
    }

    val refreshToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[REFRESH_TOKEN] ?: ""
    }

    val lastSync: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_SYNC] ?: 0L
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_DARK_MODE] = enabled
        }
    }

    suspend fun saveUserSession(
        id: String,
        phone: String,
        role: String,
        fullName: String,
        token: String,
        refreshToken: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = id
            prefs[USER_PHONE] = phone
            prefs[USER_ROLE] = role
            prefs[USER_FULL_NAME] = fullName
            prefs[AUTH_TOKEN] = token
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    /** Updates only the tokens for the current session, e.g. after a session restore/refresh. */
    suspend fun updateTokens(token: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun updateUserPhone(newPhone: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_PHONE] = newPhone
        }
    }

    suspend fun updateUserFullName(newName: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_FULL_NAME] = newName
        }
    }

    suspend fun setLastSync(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SYNC] = timestamp
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_ID)
            prefs.remove(USER_PHONE)
            prefs.remove(USER_ROLE)
            prefs.remove(USER_FULL_NAME)
            prefs.remove(AUTH_TOKEN)
            prefs.remove(REFRESH_TOKEN)
        }
    }
}
