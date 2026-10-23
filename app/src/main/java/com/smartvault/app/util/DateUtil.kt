package com.smartvault.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExpiryUrgency {
    EXPIRED,
    CRITICAL_30_DAYS,
    WARNING_60_DAYS,
    NOTICE_90_DAYS,
    SAFE,
    UNKNOWN
}

object DateUtil {
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val displayDateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        return displayDateFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return displayDateTimeFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return displayTimeFormat.format(Date(timestamp))
    }

    fun parseExpiry(expiryStr: String?): Calendar? {
        if (expiryStr.isNullOrBlank()) return null
        val trimmed = expiryStr.trim().replace(" ", "")

        // Format 1: MM/YY
        if (trimmed.matches(Regex("""^\d{1,2}/\d{2}$"""))) {
            val parts = trimmed.split("/")
            val month = parts[0].toIntOrNull() ?: return null
            val year2Digit = parts[1].toIntOrNull() ?: return null
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, 2000 + year2Digit)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            return cal
        }

        // Format 2: YYYY-MM-DD
        if (trimmed.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdf.parse(trimmed) ?: return null
            val cal = Calendar.getInstance()
            cal.time = d
            return cal
        }

        // Format 3: MM/YYYY
        if (trimmed.matches(Regex("""^\d{1,2}/\d{4}$"""))) {
            val parts = trimmed.split("/")
            val month = parts[0].toIntOrNull() ?: return null
            val year = parts[1].toIntOrNull() ?: return null
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            return cal
        }

        return null
    }

    fun checkExpiryUrgency(expiryStr: String?): ExpiryUrgency {
        val cal = parseExpiry(expiryStr) ?: return ExpiryUrgency.UNKNOWN
        val now = Calendar.getInstance()
        val diffMs = cal.timeInMillis - now.timeInMillis
        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays < 0 -> ExpiryUrgency.EXPIRED
            diffDays <= 30 -> ExpiryUrgency.CRITICAL_30_DAYS
            diffDays <= 60 -> ExpiryUrgency.WARNING_60_DAYS
            diffDays <= 90 -> ExpiryUrgency.NOTICE_90_DAYS
            else -> ExpiryUrgency.SAFE
        }
    }
}
