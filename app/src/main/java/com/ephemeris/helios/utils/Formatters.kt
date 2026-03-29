package com.ephemeris.helios.utils

import android.content.Context
import android.text.format.DateFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.roundToInt

// Format hours according to brevity rules
fun formatHour(hour: Int, isShortFormat: Boolean = false, context: Context): String {
    val is24Hour = DateFormat.is24HourFormat(context)
    val amPmStrings = DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings

    if (is24Hour) return hour.toString()
    val adjustedHour = if (hour == 12) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour >= 12) amPmStrings[1] else amPmStrings[0]
    // Regex removes 'm', 'M', spaces, and periods (e.g., "a.m." -> "a", " AM" -> "A")
    val suffix = if (isShortFormat) amPm.replace(Regex("[mM\\s.]"), "") else " $amPm"
    return "$adjustedHour$suffix"
}

fun formatNumber(number: Double): String {
    val numberFormatter = NumberFormat.getInstance()
    return numberFormatter.format(number)
}

fun Double.round(decimals: Int = 1): Double {
    return BigDecimal(this).setScale(decimals, RoundingMode.HALF_UP).toDouble()
}

fun Double.roundToSignificant(figures: Int = 3): Double {
    if (this == 0.0) return 0.0
    val magnitude = ceil(log10(abs(this)))
    val decimals = (figures - magnitude.toInt()).coerceIn(0, 4)
    return BigDecimal(this).setScale(decimals, RoundingMode.HALF_UP).toDouble()
}

fun Double.printRounded(decimals: Int = 2): String {
    if (this == 0.0) return "0"
    return BigDecimal(this)
        .setScale(decimals, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}

fun timeFormat(time: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
    return time.format(formatter)
        .replace("\u202F", " ")
}

fun Double.formatDuration(showSeconds: Boolean = false): String {
    // Catch the polar extremes directly
    if (this <= 0.0) return "0h"
    if (this >= 24.0) return "24h"

    // Convert to total seconds to avoid floating point modulo errors
    val totalSeconds = (this * 3600.0).roundToInt()

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    if (hours == 0) {
        return if (showSeconds) {
            String.format("%2dm %2ds", minutes, seconds)
        } else {
            val roundedMinutes = if (seconds >= 30) minutes + 1 else minutes
            String.format("%2dm", roundedMinutes)
        }
    }

    // %d formats without a leading zero. %02d forces two digits (leading zero if needed).
    return if (showSeconds) {
        String.format("%dh %2dm %2ds", hours, minutes, seconds)
    } else {
        val roundedMinutes = if (seconds >= 30) minutes + 1 else minutes
        String.format("%dh %2dm", hours, roundedMinutes)
    }
}

fun getDuration(startTime: Double, endTime: Double): Double {
    return (endTime - startTime + 24.0) % 24.0
}

/**
 * Converts decimal hours to "HH:mm" string format.
 */
fun Double?.formatDecimalHours(): String {
    if (this == null) return "--:--" // Sun never reaches the target angle

    var hoursNormalized = this % 24.0
    if (hoursNormalized < 0) hoursNormalized += 24.0

    val hours = hoursNormalized.toInt()
    val minutes = ((hoursNormalized - hours) * 60).roundToInt()

    // Handle rounding edge case where minutes become 60
    val finalHours = if (minutes == 60) (hours + 1) % 24 else hours
    val finalMinutes = if (minutes == 60) 0 else minutes

    val time = LocalTime.of(finalHours, finalMinutes)
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return time.format(formatter)
        .replace("\u202F", " ")
//        return String.format("%02d:%02d", finalHours, finalMinutes)
}