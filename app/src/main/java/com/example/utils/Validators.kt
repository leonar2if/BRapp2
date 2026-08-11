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
     * Normaliza un número LOCAL (8 dígitos, empieza en 5) a formato completo
     * "53XXXXXXXX" para guardarlo/usarlo igual que el resto del sistema ya
     * usa el teléfono (login construye "$cleanPhone@barberia.cu"). Mantiene
     * compatibilidad con datos existentes: si ya viene con 53 delante o con
     * +, se limpia igual.
     */
    fun cleanPhoneNumber(phone: String): String {
        val digitsOnly = phone.trim().filter { it.isDigit() }
        return when {
            digitsOnly.startsWith("53") && digitsOnly.length == 10 -> digitsOnly
            digitsOnly.length == 8 && digitsOnly.startsWith("5") -> "53$digitsOnly"
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
