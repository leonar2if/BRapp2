package com.example.utils

/** Formatea un monto con su moneda: "100 MN" o "100 USD", sin el signo €. */
object PriceFormatter {
    fun format(amount: Double, currency: String): String {
        val amountText = if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            amount.toString()
        }
        val curr = if (currency == "USD") "USD" else "MN"
        return "$amountText $curr"
    }
}
