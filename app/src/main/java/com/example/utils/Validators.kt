package com.example.utils

object Validators {
    // Prefijo obligatorio para todos los números telefónicos de la app (Cuba).
    // La interfaz siempre muestra "+53" fijo y el usuario solo introduce el
    // número local (que debe empezar por 5). Ver secciones 13 y sección 2 del
    // prompt maestro.
    const val COUNTRY_CODE = "+53"

    /**
     * Valida el número LOCAL introducido por el usuario (sin el +53), tal
     * como llega desde el campo de texto: debe tener 8 dígitos y empezar por 5.
     * Los operadores móviles cubanos siempre usan 8 dígitos empezando en 5.
     */
    fun isValidLocalPhone(localPhone: String): Boolean {
        val digitsOnly = localPhone.trim().filter { it.isDigit() }
        return digitsOnly.length == 8 && digitsOnly.startsWith("5")
    }

    /**
     * Valida un número ya en cualquier formato (con o sin +53 delante),
     * usado por pantallas legadas que aún no separan prefijo/local
     * (por ejemplo AdminSettingsScreen -> teléfono del gestor).
     * Acepta dígitos que, quitando el código de país si está presente,
     * empiecen por 5 y tengan 8 dígitos.
     */
    fun isValidPhone(phone: String): Boolean {
        val digitsOnly = phone.trim().filter { it.isDigit() }
        val local = when {
            digitsOnly.startsWith("53") && digitsOnly.length == 10 -> digitsOnly.removePrefix("53")
            else -> digitsOnly
        }
        return isValidLocalPhone(local)
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    /**
     * Normaliza un número a formato LOCAL (8 dígitos, sin +53/53 delante) para
     * guardarlo en Supabase y para construir el email de auth. Antes se
     * guardaba con "53" antepuesto ("53XXXXXXXX"); ya no: el prefijo del país
     * es solo un dato de UI, nunca debe persistirse.
     */
    fun cleanPhoneNumber(phone: String): String {
        val digitsOnly = phone.trim().filter { it.isDigit() }
        return when {
            digitsOnly.startsWith("53") && digitsOnly.length == 10 -> digitsOnly.removePrefix("53")
            else -> digitsOnly
        }
    }

    /**
     * Extrae solo la parte local (8 dígitos) de un teléfono ya normalizado
     * o crudo, para mostrarlo en un campo "+53 | 5XX XXXXX" sin duplicar el
     * prefijo. Uso principal: precargar el campo de teléfono en pantallas de
     * edición (Ajustes) a partir del valor guardado.
     */
    fun toLocalDisplay(phone: String): String {
        val digitsOnly = phone.trim().filter { it.isDigit() }
        return when {
            digitsOnly.startsWith("53") && digitsOnly.length == 10 -> digitsOnly.removePrefix("53")
            else -> digitsOnly
        }
    }
}
