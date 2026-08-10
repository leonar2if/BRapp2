package com.example.service

import com.example.data.models.Profile
import com.example.utils.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthService {
    private val api = SupabaseClient.api

    private fun getErrorMessage(e: Throwable): String = com.example.utils.ErrorMessages.humanize(e)

    suspend fun login(phone: String, pass: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = Validators.cleanPhoneNumber(phone)
            val email = "$cleanPhone@barberia.cu"

            val existingProfiles = try {
                api.getProfileByPhone("eq.$cleanPhone")
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Error buscando perfil por teléfono: ${getErrorMessage(e)}")
                )
            }

            val authRes = try {
                api.login(
                    mapOf(
                        "email" to email,
                        "password" to pass
                    )
                )
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Error en login Supabase: ${getErrorMessage(e)}")
                )
            }

            if (authRes.access_token == null || authRes.user == null) {
                return@withContext Result.failure(
                    Exception(
                        authRes.error_description
                            ?: authRes.error
                            ?: "Supabase no devolvió una sesión válida"
                    )
                )
            }

            SupabaseClient.currentAuthToken = authRes.access_token
            SupabaseClient.currentRefreshToken = authRes.refresh_token

            val profile = try {
                api.getProfileById("eq.${authRes.user.id}").firstOrNull()
            } catch (e: Exception) {
                null
            }

            return@withContext Result.success(
                profile ?: existingProfiles.firstOrNull() ?: Profile(
                    id = authRes.user.id,
                    phone = cleanPhone,
                    fullName = "Cliente",
                    role = "client"
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Login falló: ${getErrorMessage(e)}"))
        }
    }

    suspend fun register(phone: String, fullName: String, pass: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = Validators.cleanPhoneNumber(phone)

            val existing = try {
                api.getProfileByPhone("eq.$cleanPhone")
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Error comprobando si el teléfono existe: ${getErrorMessage(e)}")
                )
            }

            if (existing.isNotEmpty()) {
                return@withContext Result.failure(
                    Exception("Este número de teléfono ya está registrado.")
                )
            }

            val email = "$cleanPhone@barberia.cu"
            var userId = UUID.randomUUID().toString()

            val authRes = try {
                api.signup(
                    mapOf(
                        "email" to email,
                        "password" to pass,
                        "data" to mapOf(
                            "phone" to cleanPhone,
                            "full_name" to fullName,
                            "role" to "client"
                        )
                    )
                )
            } catch (e: Exception) {
                null
            }

            if (authRes?.user != null) {
                userId = authRes.user.id
            }

            if (authRes?.access_token != null) {
                SupabaseClient.currentAuthToken = authRes.access_token
                SupabaseClient.currentRefreshToken = authRes.refresh_token
            }

            val newProfile = Profile(
                id = userId,
                phone = cleanPhone,
                fullName = fullName,
                role = "client"
            )

            val created = try {
                api.createProfile(newProfile)
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Error creando perfil: ${getErrorMessage(e)}")
                )
            }

            Result.success(created.firstOrNull() ?: newProfile)
        } catch (e: Exception) {
            Result.failure(Exception("Registro falló: ${getErrorMessage(e)}"))
        }
    }

    /**
     * Restores a previously persisted session by exchanging the stored refresh token
     * for a new access token. Must be called on app start before any authenticated
     * request is made, since SupabaseClient.currentAuthToken only lives in memory.
     * Returns the new (accessToken, refreshToken) pair on success.
     */
    suspend fun restoreSession(refreshToken: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        if (refreshToken.isBlank()) {
            return@withContext Result.failure(Exception("No hay sesión guardada"))
        }
        try {
            val authRes = api.refreshToken(mapOf("refresh_token" to refreshToken))
            val newAccessToken = authRes.access_token
            val newRefreshToken = authRes.refresh_token
            if (newAccessToken == null || newRefreshToken == null) {
                return@withContext Result.failure(
                    Exception(authRes.error_description ?: authRes.error ?: "Sesión expirada")
                )
            }
            SupabaseClient.currentAuthToken = newAccessToken
            SupabaseClient.currentRefreshToken = newRefreshToken
            Result.success(newAccessToken to newRefreshToken)
        } catch (e: Exception) {
            Result.failure(Exception("Error restaurando sesión: ${getErrorMessage(e)}"))
        }
    }

    suspend fun updatePhone(userId: String, newPhone: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = Validators.cleanPhoneNumber(newPhone)
            val updated = api.updateProfile("eq.$userId", mapOf("phone" to cleanPhone))

            if (updated.isNotEmpty()) {
                Result.success(updated.first())
            } else {
                Result.failure(Exception("Error al actualizar teléfono"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error actualizando teléfono: ${getErrorMessage(e)}"))
        }
    }
}