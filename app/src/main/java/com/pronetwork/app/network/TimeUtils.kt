package com.pronetwork.app.network

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Utility to convert UTC timestamps from server to local device time.
 */
object TimeUtils {

    private val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val localDateTimeFormat = SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    private val localTimeOnlyFormat = SimpleDateFormat("HH:mm", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    private val localDateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    /**
     * Convert UTC timestamp string to local date+time string.
     * Input:  "2026-02-24T15:30:00" (UTC)
     * Output: "2026-02-24  18:30" (Local +03)
     */
    fun utcToLocal(utcString: String?): String {
        if (utcString.isNullOrBlank()) return "—"
        return try {
            // Handle both formats: with and without fractional seconds
            val cleanUtc = utcString.substringBefore(".").substringBefore("Z")
            val date = utcFormat.parse(cleanUtc) ?: return utcString
            localDateTimeFormat.format(date)
        } catch (e: Exception) {
            utcString.take(16).replace("T", "  ")
        }
    }

    /**
     * Convert UTC timestamp to local time only (HH:mm).
     */
    fun utcToLocalTimeOnly(utcString: String?): String {
        if (utcString.isNullOrBlank()) return "—"
        return try {
            val cleanUtc = utcString.substringBefore(".").substringBefore("Z")
            val date = utcFormat.parse(cleanUtc) ?: return utcString
            localTimeOnlyFormat.format(date)
        } catch (e: Exception) {
            utcString
        }
    }

    /**
     * Convert UTC timestamp to local date only (yyyy-MM-dd).
     */
    fun utcToLocalDateOnly(utcString: String?): String {
        if (utcString.isNullOrBlank()) return "—"
        return try {
            val cleanUtc = utcString.substringBefore(".").substringBefore("Z")
            val date = utcFormat.parse(cleanUtc) ?: return utcString
            localDateOnlyFormat.format(date)
        } catch (e: Exception) {
            utcString
        }
    }
}
