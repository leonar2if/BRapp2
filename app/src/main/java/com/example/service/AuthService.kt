package com.example.service

import com.example.data.models.Profile
import com.example.utils.Validators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthService {
    private val api = SupabaseClient.api

    // NOTA (auditoría / sección 3 del prompt maestro): este servicio ya NO traduce
    // errores a texto humano localmente. Antes tenía su propia getErrorMessage()
    // duplicando lógica; ahora deja pasar el mensaje técnico original (para logs
    // de desarrollador) y la traducción a lenguaje humano se hace en un único
    // lugar: com.example.utils.ErrorTranslator, justo antes de mostrarse en UI
    // (AuthViewModel). Esto evita tener dos implementaciones paralelas de
    // traducción de errores.
    private fun getErrorMessage(e: Throwable): String {
        return e.message ?: "Error desconocido"
    }

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

    /**
     * Cambia la contraseña del usuario autenticado actual (sección 3.1). Usa el
     * endpoint estándar de GoTrue PUT /auth/v1/user con el token de sesión actual
     * (SupabaseClient.currentAuthToken, ya agregado por el interceptor). Si no hay
     * sesión activa (token nulo), falla explícitamente en vez de simular éxito.
     */
    suspend fun changePassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (SupabaseClient.currentAuthToken.isNullOrBlank()) {
            return@withContext Result.failure(Exception("No hay una sesión activa. Vuelve a iniciar sesión."))
        }
        if (newPassword.length < 6) {
            return@withContext Result.failure(Exception("La contraseña debe tener al menos 6 caracteres."))
        }
        try {
            api.updateAuthUser(mapOf("password" to newPassword))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error cambiando contraseña: ${getErrorMessage(e)}"))
        }
    }

    suspend fun updateBirthday(userId: String, birthday: String?): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            val updated = api.updateProfile("eq.$userId", mapOf("birthday" to birthday))
            if (updated.isNotEmpty()) Result.success(updated.first())
            else Result.failure(Exception("Error al guardar el cumpleaños"))
        } catch (e: Exception) {
            Result.failure(Exception("Error guardando cumpleaños: ${getErrorMessage(e)}"))
        }
    }

    /**
     * Suma 1 al contador de visitas del cliente (sección de contadores). Lectura
     * + escritura simple: el volumen de un solo admin tocando esto secuencialmente
     * no justifica una RPC atómica extra.
     */
    suspend fun incrementVisitCount(phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = api.getProfileByPhone("eq.$phone").firstOrNull() ?: return@withContext Result.success(Unit)
            api.updateProfile("eq.${profile.id}", mapOf("visit_count" to (profile.visitCount + 1)))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementNoShowCount(phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = api.getProfileByPhone("eq.$phone").firstOrNull() ?: return@withContext Result.success(Unit)
            api.updateProfile("eq.${profile.id}", mapOf("no_show_count" to (profile.noShowCount + 1)))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Listado completo de clientes para el directorio del admin (sección de directorio). */
    suspend fun getAllClients(): Result<List<Profile>> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.getAllClientProfiles("eq.client"))
        } catch (e: Exception) {
            Result.failure(Exception("Error cargando clientes: ${getErrorMessage(e)}"))
        }
    }

    /** Perfil de un cliente por teléfono, para mostrar sus contadores en la galería. */
    suspend fun getProfileByPhone(phone: String): Profile? = withContext(Dispatchers.IO) {
        try {
            api.getProfileByPhone("eq.$phone").firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}