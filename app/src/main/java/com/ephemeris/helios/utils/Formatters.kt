package com.ephemeris.helios.utils

import android.content.Context
import android.icu.text.TimeZoneNames
import android.icu.util.TimeZone as IcuTimeZone
import android.text.format.DateFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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
    val totalSeconds = (abs(this) * 3600.0).roundToInt()
    val degrees = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d°%02d′%02d″ %s", degrees, minutes, seconds, direction)
}

// TODO: add formatTime, formatAzimuth and formatElevation so rounding and symbols are encapsulated

fun Double.formatShortLatitude(
    isDecimal: Boolean = true,
    locale: Locale = Locale.getDefault(),
): String {
    val direction = if (this >= 0) "N" else "S"
    val absValue = abs(this)
    return if (isDecimal) {
        String.format(locale, "%.2f°%s", absValue, direction)
    } else {
        val totalMinutes = (absValue * 60.0).roundToInt()
        val degrees = totalMinutes / 60
        val minutes = totalMinutes % 60
        String.format(locale, "%d°%02d′%s", degrees, minutes, direction)
    }
}

fun Double.formatLongitude(): String {
    val direction = if (this >= 0) "E" else "W"
    val totalSeconds = (abs(this) * 3600.0).roundToInt()
    val degrees = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d°%02d′%02d″ %s", degrees, minutes, seconds, direction)
}

fun Double.formatShortLongitude(
    isDecimal: Boolean = true,
    locale: Locale = Locale.getDefault(),
): String {
    val direction = if (this >= 0) "E" else "W"
    val absValue = abs(this)
    return if (isDecimal) {
        String.format(locale, "%.2f°%s", absValue, direction)
    } else {
        val totalMinutes = (absValue * 60.0).roundToInt()
        val degrees = totalMinutes / 60
        val minutes = totalMinutes % 60
        String.format(locale, "%d°%02d′%s", degrees, minutes, direction)
    }
}

data class DmsCoordinate(
    val degrees: Int,
    val minutes: Int,
    val seconds: Double,
    val isPositive: Boolean,
)

fun Double.toDmsCoordinate(secondsDecimals: Int = 2): DmsCoordinate {
    val isPositive = this >= 0.0
    val scale = 10.0.pow(secondsDecimals)
    val totalScaledSeconds = (abs(this) * 3600.0 * scale).roundToLong()
    val scaledPerMinute = (60.0 * scale).roundToLong()
    val scaledPerDegree = (3600.0 * scale).roundToLong()
    val degrees = (totalScaledSeconds / scaledPerDegree).toInt()
    val remainingAfterDegrees = totalScaledSeconds % scaledPerDegree
    val minutes = (remainingAfterDegrees / scaledPerMinute).toInt()
    val seconds = (remainingAfterDegrees % scaledPerMinute) / scale
    return DmsCoordinate(
        degrees = degrees,
        minutes = minutes,
        seconds = seconds,
        isPositive = isPositive,
    )
}

fun dmsToDecimal(
    degrees: Int,
    minutes: Int,
    seconds: Double,
    isPositive: Boolean,
): Double {
    val magnitude = degrees + minutes / 60.0 + seconds / 3600.0
    return if (isPositive) magnitude else -magnitude
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
    if (totalSeconds <= 0) return "0h"
    if (totalSeconds >= 24 * 3600) return "24h"

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

/**
 * Formats an azimuth angle in degrees with one decimal place and an optional 16-point
 * compass direction (e.g., `"180.0° S"`, `"62.3° ENE"`, `"0.0° N"`).
 *
 * Normalizes any input angle into `[0.0, 360.0)` and wraps values rounding to `360.0°`
 * cleanly back to `0.0° N`.
 */
fun Double?.formatAzimuth(
    locale: Locale = Locale.getDefault(),
    includeDirection: Boolean = true,
): String? {
    if (this == null || this.isNaN()) return null
    val normalized = ((this % 360.0) + 360.0) % 360.0
    val roundedTenths = (normalized * 10.0).roundToInt() % 3600
    val wrappedDeg = roundedTenths / 10.0
    val degText = String.format(locale, "%.1f°", wrappedDeg)
    if (!includeDirection) return degText
    val cardinal = wrappedDeg.formatDirection()
    return if (cardinal.isNotEmpty()) "$degText $cardinal" else degText
}

/**
 * Formats a signed coordinate in decimal degrees with 5 fractional digits (~1.1 m precision)
 * and a proper minus sign (e.g. `"41.90278°"`, `"−34.60372°"`).
 */
fun Double.formatDecimalDegrees(
    decimals: Int = 5,
    locale: Locale = Locale.getDefault(),
): String {
    val rounded = BigDecimal.valueOf(this).setScale(decimals, RoundingMode.HALF_UP)
    val sign = if (rounded.signum() < 0) "\u2212" else ""
    val formatted = String.format(locale, "%.${decimals}f", rounded.abs().toDouble())
    return "$sign$formatted°"
}

/**
 * Formats a [ZoneOffset] in concise UTC notation (never GMT), e.g., `"UTC+2"`, `"UTC−3"`,
 * `"UTC+5:30"`, or `"UTC+0"`.
 */
fun ZoneOffset.formatUtcOffset(): String {
    val total = totalSeconds
    if (total == 0) return "UTC+0"
    val sign = if (total > 0) "+" else "\u2212"
    val absSeconds = abs(total)
    val hours = absSeconds / 3600
    val minutes = (absSeconds % 3600) / 60
    val seconds = absSeconds % 60
    return when {
        seconds != 0 -> String.format(Locale.US, "UTC%s%d:%02d:%02d", sign, hours, minutes, seconds)
        minutes != 0 -> String.format(Locale.US, "UTC%s%d:%02d", sign, hours, minutes)
        else -> "UTC$sign$hours"
    }
}

data class FormattedTimeZone(
    val utcOffset: String,
    val abbreviation: String?,
    val fullName: String,
    val cityName: String?,
    val isDst: Boolean,
)

private val AbbreviationLookupLocales: List<Locale> = listOf(
    Locale.US,
    Locale.UK,
    Locale.forLanguageTag("en-AU"),
    Locale.forLanguageTag("en-NZ"),
    Locale.forLanguageTag("en-IN"),
    Locale.forLanguageTag("en-CA"),
    Locale.forLanguageTag("en-ZA"),
    Locale.JAPAN,
    Locale.KOREA,
)

private fun isValidTimeZoneAbbreviation(candidate: String?): Boolean {
    if (candidate.isNullOrBlank() || candidate.length !in 2..5) return false
    return candidate.all { it in 'A'..'Z' }
}

private fun isFixedOffsetZoneId(id: String): Boolean {
    if ('/' !in id) return true
    return id.startsWith("Etc/", ignoreCase = true)
}

/**
 * Formats a [ZonedDateTime]'s time zone for display:
 * - [FormattedTimeZone.utcOffset]: concise UTC offset (`"UTC+2"`, `"UTC−3"`, `"UTC+5:30"`, `"UTC+0"`).
 * - [FormattedTimeZone.abbreviation]: authoritative CLDR short abbreviation distinguishing Standard
 *   vs Daylight/Summer time (e.g. `"CET"` vs `"CEST"`, `"EST"` vs `"EDT"`, `"GMT"` vs `"BST"`), or
 *   `null` when the zone has no standard abbreviation (e.g. Argentina, Brazil, China).
 * - [FormattedTimeZone.cityName]: localized exemplar city name via ICU (e.g. `"Rome"`,
 *   `"Buenos Aires"`, `"Córdoba"`, `"Indianapolis"`), cleanly handling multi-segment IANA IDs like
 *   `America/Argentina/Buenos_Aires`.
 * - [FormattedTimeZone.fullName]: localized full time zone name with any `"GMT±..."` fallback
 *   normalized to UTC notation.
 */
fun ZonedDateTime.formatTimeZone(locale: Locale = Locale.getDefault()): FormattedTimeZone {
    val instant = toInstant()
    val epochMillis = instant.toEpochMilli()
    val isDst = zone.rules.isDaylightSavings(instant)
    val utcOffset = offset.formatUtcOffset()

    val rawId = zone.id
    val canonicalId = runCatching { IcuTimeZone.getCanonicalID(rawId) }.getOrNull() ?: rawId
    val isPureUtc = canonicalId.equals("UTC", ignoreCase = true) ||
        canonicalId.equals("Etc/UTC", ignoreCase = true) ||
        canonicalId.equals("Etc/UCT", ignoreCase = true) ||
        (zone is ZoneOffset && offset.totalSeconds == 0)

    if (isPureUtc) {
        return FormattedTimeZone(
            utcOffset = "UTC+0",
            abbreviation = "UTC",
            fullName = "Coordinated Universal Time",
            cityName = null,
            isDst = false,
        )
    }

    if (zone is ZoneOffset) {
        return FormattedTimeZone(
            utcOffset = utcOffset,
            abbreviation = null,
            fullName = "Fixed Offset",
            cityName = null,
            isDst = false,
        )
    }

    // 1. Authoritative short abbreviation via ICU TimeZoneNames (properly separates STD vs DST)
    val shortType = if (isDst) {
        TimeZoneNames.NameType.SHORT_DAYLIGHT
    } else {
        TimeZoneNames.NameType.SHORT_STANDARD
    }
    val abbreviation = sequenceOf(locale)
        .plus(AbbreviationLookupLocales.asSequence())
        .distinct()
        .mapNotNull { lookupLocale ->
            runCatching {
                TimeZoneNames.getInstance(lookupLocale)
                    .getDisplayName(canonicalId, shortType, epochMillis)
            }.getOrNull()
        }
        .firstOrNull(::isValidTimeZoneAbbreviation)

    // 2. Exemplar city name (e.g. "Europe/Rome" -> "Rome", "America/Argentina/Buenos_Aires" -> "Buenos Aires")
    val cityName = if (isFixedOffsetZoneId(canonicalId)) {
        null
    } else {
        val icuCity = runCatching {
            TimeZoneNames.getInstance(locale).getExemplarLocationName(canonicalId)
        }.getOrNull()?.takeIf { it.isNotBlank() && !it.contains("Unknown", ignoreCase = true) }

        icuCity ?: canonicalId.substringAfterLast('/').replace('_', ' ').takeIf { it.isNotBlank() }
    }

    // 3. Full localized name, never leaking "GMT+..." offset fallbacks
    val rawFullName = format(DateTimeFormatter.ofPattern("zzzz", locale))
    val fullName = when {
        rawFullName.startsWith("GMT") && rawFullName.length > 3 &&
            (rawFullName[3] == '+' || rawFullName[3] == '-' || rawFullName[3] == '\u2212') -> {
            "Fixed Offset"
        }
        rawFullName.equals("GMT", ignoreCase = true) && offset.totalSeconds == 0 &&
            canonicalId.contains("UTC", ignoreCase = true) -> {
            "Coordinated Universal Time"
        }
        else -> rawFullName
    }

    return FormattedTimeZone(
        utcOffset = utcOffset,
        abbreviation = abbreviation,
        fullName = fullName,
        cityName = cityName,
        isDst = isDst,
    )
}