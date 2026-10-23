package com.smartvault.app.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val decimalFormat = DecimalFormat("#,##0.00")

    fun format(amount: Double, currency: String = "PKR", isPrivacyMode: Boolean = false): String {
        if (isPrivacyMode) {
            return "$currency •••••"
        }
        val formattedNumber = decimalFormat.format(amount)
        return "$currency $formattedNumber"
    }

    fun maskCardNumber(cardNumber: String?, isPrivacyMode: Boolean = false): String {
        if (cardNumber.isNullOrBlank()) return "No Number"
        if (isPrivacyMode) return "•••• •••• •••• ••••"
        val clean = cardNumber.replace(" ", "")
        return if (clean.length >= 4) {
            val last4 = clean.takeLast(4)
            "•••• •••• •••• $last4"
        } else {
            "••••"
        }
    }
}
