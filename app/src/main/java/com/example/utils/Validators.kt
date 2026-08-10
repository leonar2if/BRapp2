package com.example.utils

object Validators {
    // Prefijo de país fijo mostrado en la UI (punto 13). El usuario solo escribe el
    // número local, que debe empezar con 5 (móviles en Cuba).
    const val COUNTRY_CODE = "+53"
    private const val LOCAL_PHONE_LENGTH = 8

    fun isValidLocalPhone(localPhone: String): Boolean {
        val clean = cleanPhoneNumber(localPhone)
        return clean.length == LOCAL_PHONE_LENGTH && clean.all { it.isDigit() } && clean.startsWith("5")
    }

    fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        return cleanPhone.length >= 7 && cleanPhone.all { it.isDigit() || it == '+' }
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    fun cleanPhoneNumber(phone: String): String {
        return phone.trim().replace(" ", "").replace("-", "").removePrefix(COUNTRY_CODE)
    }
}
