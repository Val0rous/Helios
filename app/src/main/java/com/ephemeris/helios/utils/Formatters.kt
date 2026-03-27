package com.ephemeris.helios.utils

import android.content.Context
import android.text.format.DateFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10

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
    val decimals = (figures - magnitude.toInt()).coerceAtLeast(0)
    return BigDecimal(this).setScale(decimals, RoundingMode.HALF_UP).toDouble()
}

fun timeFormat(time: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
    return time.format(formatter)
        .replace("\u202F", " ")
}