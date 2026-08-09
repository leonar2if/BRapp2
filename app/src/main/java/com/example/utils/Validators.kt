package com.example.utils

object Validators {
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
        return phone.trim().replace(" ", "").replace("-", "")
    }
}
