package com.ephemeris.helios.utils

import android.content.Context
import android.text.format.DateFormat
import java.text.DateFormatSymbols
import java.util.Locale

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