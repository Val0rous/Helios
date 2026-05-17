package com.ephemeris.helios.utils

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.graphics.Paint
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
import kotlin.math.roundToInt

// Format hours according to brevity rules
fun formatHour(hour: Int, isShortFormat: Boolean = false, context: Context): String {
    val is24Hour = DateFormat.is24HourFormat(context)
    val amPmStrings = DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings

    if (is24Hour) return if (hour == 24) "24+" else (hour % 24).toString()
    val adjustedHour = if (hour % 12 == 0) 12 else hour % 12
    val isPm = (hour % 24) in 12..23
    val amPm = if (isPm) amPmStrings[1] else amPmStrings[0]
    // Regex removes 'm', 'M', spaces, and periods (e.g., "a.m." -> "a", " AM" -> "A")
    val suffix = if (isShortFormat) amPm.replace(Regex("[mM\\s.]"), "") else " $amPm"
    val plusIndicator = if (hour == 24) "+" else ""
    return "$adjustedHour$suffix$plusIndicator"
}

fun formatNumber(number: Double): String {
    val numberFormatter = NumberFormat.getInstance()
    return numberFormatter.format(number)
}

fun Double.formatLatitude(): String {
    val direction = if (this >= 0) "N" else "S"
    val absValue = abs(this)
    val degrees = absValue.toInt()
    val minutes = ((absValue - degrees) * 60).toInt()
    val seconds = ((absValue - degrees - minutes / 60.0) * 3600.0).roundToInt()
    return String.format(Locale.getDefault(), "%d°%02d′%02d″ %s", degrees, minutes, seconds, direction)
}

// TODO: add formatTime, formatAzimuth and formatElevation so rounding and symbols are encapsulated

fun Double.formatShortLatitude(isDecimal: Boolean = true): String {
    val direction = if (this >= 0) "N" else "S"
    val absValue = abs(this)
    val degrees = absValue.toInt()
    val minutes = ((absValue - degrees) * 60).toInt()
    return if (isDecimal) {
        "${absValue.round(2)}°$direction"
    } else {
        "$degrees°$minutes′$direction"
    }
//    val formattedLat = String.format(
//                    LocalLocale.current.platformLocale,
//                    "%.2f",
//                    coordinates.latitude
//                ) + if (coordinates.latitude < 0) "S" else "N"
}

fun Double.formatLongitude(): String {
    val direction = if (this >= 0) "E" else "W"
    val absValue = abs(this)
    val degrees = absValue.toInt()
    val minutes = ((absValue - degrees) * 60).toInt()
    val seconds = ((absValue - degrees - minutes / 60.0) * 3600.0).roundToInt()
    return String.format(Locale.getDefault(), "%d°%02d′%02d″ %s", degrees, minutes, seconds, direction)
}

fun Double.formatShortLongitude(isDecimal: Boolean = true): String {
    val direction = if (this >= 0) "E" else "W"
    val absValue = abs(this)
    val degrees = absValue.toInt()
    val minutes = ((absValue - degrees) * 60).toInt()
    return if (isDecimal) {
        "${absValue.round(2)}°$direction"
    } else {
        "$degrees°$minutes′$direction"
    }
    //                val formattedLon = String.format(
//                    LocalLocale.current.platformLocale,
//                    "%.2f",
//                    coordinates.longitude
//                ) + if (coordinates.longitude < 0) "W" else "E"
}

fun Double.round(decimals: Int = 1): Double {
    if (this.isNaN()) return Double.NaN
    return BigDecimal(this).setScale(decimals, RoundingMode.HALF_UP).toDouble()
}

fun Double?.round(decimals: Int = 1): Double? {
    if (this == null || this.isNaN()) return null
    return BigDecimal(this).setScale(decimals, RoundingMode.HALF_UP).toDouble()
}

fun Double.roundToSignificant(): Double {
    if (this.isNaN()) return Double.NaN
    if (this == 0.0) return 0.0
    val absValue = abs(this)
    val decimals = when {
        absValue < 1.0 -> 3
        absValue < 10.0 -> 2
        absValue < 100.0 -> 1
        else -> 0
    }
    return BigDecimal(this.toString()).setScale(decimals, RoundingMode.HALF_UP).toDouble()
}

fun Double.printSignificant(): String {
    if (this == 0.0) return "0"
    val rounded = this.roundToSignificant()
    if (rounded.isNaN()) return ""
    if (rounded == 0.0) return "0"
    val absValue = abs(rounded)
    val decimals = when {
        absValue < 1.0 -> 3
        absValue < 10.0 -> 2
        absValue < 100.0 -> 1
        else -> 0
    }
    return String.format(Locale.getDefault(), "%,.${decimals}f", rounded)
}

fun Double.printRounded(decimals: Int = 2, stripTrailingZeros: Boolean = true): String {
    if (this == 0.0) return "0"
    val result = BigDecimal(this.toString()).setScale(decimals, RoundingMode.HALF_UP)
    if (result.signum() == 0) return "0"
    return if (stripTrailingZeros) {
        result.stripTrailingZeros().toPlainString()
    } else {
        result.toPlainString()
    }
}

fun timeFormat(time: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
    return time.format(formatter)
        .replace("\u202F", " ")
}

fun Double.formatDuration(showSeconds: Boolean = false): String {
    if (this.isNaN()) return ""

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
            String.format("%dm %ds", minutes, seconds)
        } else {
            val roundedMinutes = if (seconds >= 30) minutes + 1 else minutes
            String.format("%dm", roundedMinutes)
        }
    }

    // %d formats without a leading zero. %02d forces two digits (leading zero if needed).
    return if (showSeconds) {
        String.format("%dh %dm %ds", hours, minutes, seconds)
    } else {
        val roundedMinutes = if (seconds >= 30) minutes + 1 else minutes
        String.format("%dh %dm", hours, roundedMinutes)
    }
}

fun getDuration(startTime: Double, endTime: Double): Double {
    return (endTime - startTime + 24.0) % 24.0
}

/**
 * Converts decimal hours to "HH:mm" string format.
 */
fun Double?.formatDecimalHours(): String {
    if (this == null || this.isNaN()) return "--:--" // Sun never reaches the target angle

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

fun Double.formatDirection(): String {
    if (this.isNaN()) return ""

    val normalized = (this % 360 + 360) % 360
    val directions = arrayOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    )

    val index = (((normalized + 11.25) % 360) / 22.5).toInt()
    return directions[index]
}