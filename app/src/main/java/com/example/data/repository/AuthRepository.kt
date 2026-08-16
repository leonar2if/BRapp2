package com.example.data.repository

import android.content.Context
import com.example.data.database.PreferencesManager
import com.example.data.models.Profile
import com.example.service.AuthService
import com.example.service.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepository(context: Context) {
    private val authService = AuthService()
    private val prefs = PreferencesManager(context)

    val isDarkMode: Flow<Boolean> = prefs.isDarkMode
    val userPhone: Flow<String> = prefs.userPhone
    val userRole: Flow<String> = prefs.userRole
    val userId: Flow<String> = prefs.userId
    val userFullName: Flow<String> = prefs.userFullName
    val authToken: Flow<String> = prefs.authToken

    suspend fun login(phone: String, pass: String): Result<Profile> {
        val result = authService.login(phone, pass)
        val profile = result.getOrNull()
        if (result.isSuccess && profile != null) {
            prefs.saveUserSession(
                id = profile.id,
                phone = profile.phone,
                role = profile.role,
                fullName = profile.fullName,
                token = SupabaseClient.currentAuthToken ?: "",
                refreshToken = SupabaseClient.currentRefreshToken ?: ""
            )
        }
        return result
    }

    suspend fun register(phone: String, fullName: String, pass: String): Result<Profile> {
        val result = authService.register(phone, fullName, pass)
        val profile = result.getOrNull()
        if (result.isSuccess && profile != null) {
            prefs.saveUserSession(
                id = profile.id,
                phone = profile.phone,
                role = profile.role,
                fullName = profile.fullName,
                token = SupabaseClient.currentAuthToken ?: "",
                refreshToken = SupabaseClient.currentRefreshToken ?: ""
            )
        }
        return result
    }

    /**
     * Restores the in-memory Supabase auth token from the persisted refresh token.
     * Must be called on app start (before any authenticated call) for previously
     * logged-in users, since SupabaseClient.currentAuthToken does not survive process death.
     * Returns true if the session is valid and was restored, false if there was no
     * session to restore or it could not be renewed (in which case the local session
     * is cleared and the user is sent back to login).
     */
    suspend fun restoreSession(): Boolean {
        val storedRefreshToken = prefs.refreshToken.first()
        if (storedRefreshToken.isBlank()) return false

        val result = authService.restoreSession(storedRefreshToken)
        val tokens = result.getOrNull()
        return if (result.isSuccess && tokens != null) {
            prefs.updateTokens(token = tokens.first, refreshToken = tokens.second)
            true
        } else {
            // Refresh token is invalid/expired: don't leave the app in a half-logged-in state.
            logout()
            false
        }
    }

    suspend fun updatePhone(newPhone: String): Result<Profile> {
        val currentId = userId.first()
        val res = authService.updatePhone(currentId, newPhone)
        if (res.isSuccess) {
            prefs.updateUserPhone(newPhone)
        }
        return res
    }

    suspend fun changePassword(newPassword: String): Result<Unit> {
        return authService.changePassword(newPassword)
    }

    /** Perfil completo del usuario actual (para leer el cumpleaños guardado). */
    suspend fun getCurrentProfile(): Profile? {
        val id = userId.first()
        if (id.isEmpty()) return null
        return SupabaseClient.api.getProfileById("eq.$id").firstOrNull()
    }

    suspend fun updateBirthday(birthday: String?): Result<Profile> {
        val id = userId.first()
        return authService.updateBirthday(id, birthday)
    }

    suspend fun setDarkMode(enabled: Boolean) {
        prefs.setDarkMode(enabled)
    }

    suspend fun logout() {
        prefs.clearSession()
        SupabaseClient.currentAuthToken = null
        SupabaseClient.currentRefreshToken = null
    }
}
