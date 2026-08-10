package com.example.utils

/**
 * Traduce errores técnicos (HTTP, red, excepciones de Retrofit/Moshi/Supabase) a
 * mensajes en español entendibles por el usuario final. Punto único de traducción:
 * ningún otro lugar de la app debe mostrar `e.message` / `exceptionOrNull()?.message`
 * directamente en la UI - todos pasan por acá primero.
 *
 * El mensaje técnico original nunca se pierde: quien llame a [humanize] puede seguir
 * logueando `throwable` completo con Log.e/println para debug, esta función solo
 * decide qué ve el usuario.
 */
object ErrorMessages {

    fun humanize(throwable: Throwable?): String {
        if (throwable == null) return "Ocurrió un problema inesperado. Inténtalo nuevamente."
        val msg = throwable.message ?: ""

        return when {
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("Failed to connect", ignoreCase = true) ||
            msg.contains("ConnectException", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("No address associated", ignoreCase = true) ->
                "No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo nuevamente."

            msg.contains("400") -> "Los datos enviados no son válidos. Revísalos e inténtalo de nuevo."

            msg.contains("401") || msg.contains("Invalid login credentials", ignoreCase = true) ->
                "El número o la contraseña no son correctos."

            msg.contains("403") -> "No tienes permiso para realizar esta acción."

            msg.contains("404") -> "No se encontró la información solicitada."

            msg.contains("409") || msg.contains("already registered", ignoreCase = true) || msg.contains("duplicate", ignoreCase = true) ->
                "Ese número de teléfono ya está registrado."

            msg.contains("422") -> "Los datos enviados no son válidos. Revísalos e inténtalo de nuevo."

            msg.contains("429") -> "Demasiados intentos. Espera un momento e inténtalo de nuevo."

            msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504") ->
                "Ocurrió un problema en el servidor. Inténtalo nuevamente en unos minutos."

            msg.contains("sesión", ignoreCase = true) && msg.contains("expir", ignoreCase = true) ->
                "Tu sesión ha expirado. Inicia sesión nuevamente."

            else -> "Ocurrió un problema inesperado. Inténtalo nuevamente."
        }
    }
}
