package com.example.utils

/**
 * Capa centralizada de traducción de errores técnicos -> mensajes humanos.
 *
 * Objetivo (ver PROMPT MAESTRO, sección 3):
 * - El usuario NUNCA debe ver stack traces, JSON crudo, códigos HTTP,
 *   mensajes de Supabase/Retrofit/PostgREST, UUIDs, nombres de tablas, etc.
 * - Los desarrolladores pueden seguir viendo el error técnico completo en
 *   Logcat (BuildConfig.DEBUG ya controla eso en SupabaseClient/HttpLoggingInterceptor).
 * - Toda la app debe pasar sus mensajes de error por aquí en vez de mostrar
 *   e.message directamente, para no duplicar lógica de traducción.
 *
 * Uso: ErrorTranslator.toHumanMessage(throwable) o
 *      ErrorTranslator.toHumanMessage(rawMessage)
 */
object ErrorTranslator {

    fun toHumanMessage(e: Throwable?): String {
        return toHumanMessage(e?.message)
    }

    fun toHumanMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return GENERIC_ERROR
        val msg = raw.lowercase()

        return when {
            // Conexión / red
            msg.contains("unable to resolve host") ||
            msg.contains("failed to connect") ||
            msg.contains("timeout") ||
            msg.contains("timed out") ||
            msg.contains("no address associated") ||
            msg.contains("network is unreachable") ->
                "No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo nuevamente."

            // Credenciales / login
            msg.contains("invalid login credentials") ||
            msg.contains("invalid_grant") ||
            msg.contains("401") ->
                "El número o la contraseña no son correctos."

            msg.contains("email not confirmed") ->
                "Tu cuenta aún no está confirmada. Contacta con el administrador."

            // Sesión
            msg.contains("refresh_token") ||
            msg.contains("jwt expired") ||
            msg.contains("token is expired") ||
            msg.contains("sesión") && msg.contains("expirad") ->
                "Tu sesión ha expirado. Inicia sesión nuevamente."

            // Permisos
            msg.contains("403") ||
            msg.contains("row-level security") ||
            msg.contains("permission denied") ->
                "No tienes permiso para realizar esta acción."

            // Registro duplicado
            msg.contains("ya está registrado") ||
            msg.contains("duplicate key") ||
            msg.contains("already registered") ||
            msg.contains("user already registered") ->
                "Ese número de teléfono ya está registrado."

            // No encontrado
            msg.contains("404") ->
                "No se encontró la información solicitada."

            // Servidor
            msg.contains("500") ||
            msg.contains("502") ||
            msg.contains("503") ||
            msg.contains("internal server error") ->
                "Ocurrió un problema en el servidor. Inténtalo nuevamente en unos minutos."

            // Reglas de negocio de la app (ya vienen en español y son seguras
            // de mostrar tal cual: no contienen datos técnicos).
            msg.contains("ya tienes") ||
            msg.contains("reserva") && (msg.contains("activa") || msg.contains("anexada")) ||
            msg.contains("completa el nombre") ||
            msg.contains("datos de reserva incompletos") ||
            msg.contains("contraseñas no coinciden") ||
            msg.contains("número de teléfono") ->
                raw

            else -> GENERIC_ERROR
        }
    }

    private const val GENERIC_ERROR = "Ocurrió un problema inesperado. Inténtalo nuevamente."
}
