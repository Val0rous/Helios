package com.ephemeris.helios.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.*

object SolarEphemeris {

    // Target Altitudes (in degrees)
    const val ALT_SUNRISE_SUNSET = -0.833
    const val ALT_CIVIL_TWILIGHT = -6.0
    const val ALT_NAUTICAL_TWILIGHT = -12.0
    const val ALT_ASTRONOMICAL_TWILIGHT = -18.0
    const val ALT_BLUE_HOUR_LOWER = -6.0 // Matches Civil Twilight
    const val ALT_BLUE_HOUR_UPPER = -4.0
    const val ALT_GOLDEN_HOUR_LOWER = -4.0
    const val ALT_GOLDEN_HOUR_UPPER = 6.0
    const val ALT_PLUTO_TIME = -2.20

    data class SolarPosition(
        val altitude: Double, // degrees
        val azimuth: Double   // degrees
    )

    data class DailyEvents(
        val solarNoon: Double,
        val solarNoonAzimuth: Double,
        val sunrise: Double?,
        val sunriseAzimuth: Double?,
        val sunset: Double?,
        val sunsetAzimuth: Double?,
        val dawnCivil: Double?,
        val duskCivil: Double?,
        val dawnNautical: Double?,
        val duskNautical: Double?,
        val dawnAstronomical: Double?,
        val duskAstronomical: Double?
    )

    /**
     * Calculates all daily events (sunrise, sunset, twilights) in decimal hours, using iterative refinement for continuous accuracy.
     */
    fun calculateDailyEvents(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        tzOffsetHours: Double
    ): DailyEvents {
        val latRad = Math.toRadians(latitude)
        val dayOfYear = date.dayOfYear

        // 1. Calculate Solar Noon continuously (it drifts slightly from 12:00)
        // We do a quick 2-step iteration to find exact solar noon
        var exactSolarNoon = 12.0 - (longitude / 15.0) + tzOffsetHours
        for (i in 1..2) {
            val gamma = calculateGamma(dayOfYear, exactSolarNoon)
            val eot = calculateEquationOfTime(gamma)
            exactSolarNoon = 12.0 - (longitude / 15.0) - (eot / 60.0) + tzOffsetHours
        }

        // 2. The Iterative Engine for finding specific altitude times
        fun findPreciseEventTimes(targetAltDeg: Double): Pair<Double, Double>? {
            val targetAltRad = Math.toRadians(targetAltDeg)

            // Start with rough guesses: 6 AM for morning, 6 PM for evening
            var dawnEstimate = 6.0
            var duskEstimate = 18.0

            // Iterate 3 times for sub-second convergence
            for (i in 1..3) {
                // Refine Dawn
                val dawnGamma = calculateGamma(dayOfYear, dawnEstimate)
                val dawnDeclination = calculateDeclination(dawnGamma)
                val dawnEot = calculateEquationOfTime(dawnGamma)

                val dawnCosOmega = (sin(targetAltRad) - sin(latRad) * sin(dawnDeclination)) / (cos(latRad) * cos(dawnDeclination))
                if (dawnCosOmega < -1.0 || dawnCosOmega > 1.0) return null // Polar day/night

                val dawnOmegaDeg = Math.toDegrees(acos(dawnCosOmega))
                val dawnSolarNoonAtThatMoment = 12.0 - (longitude / 15.0) - (dawnEot / 60.0) + tzOffsetHours
                dawnEstimate = dawnSolarNoonAtThatMoment - (dawnOmegaDeg / 15.0)

                // Refine Dusk
                val duskGamma = calculateGamma(dayOfYear, duskEstimate)
                val duskDeclination = calculateDeclination(duskGamma)
                val duskEot = calculateEquationOfTime(duskGamma)

                val duskCosOmega = (sin(targetAltRad) - sin(latRad) * sin(duskDeclination)) / (cos(latRad) * cos(duskDeclination))
                val duskOmegaDeg = Math.toDegrees(acos(duskCosOmega))
                val duskSolarNoonAtThatMoment = 12.0 - (longitude / 15.0) - (duskEot / 60.0) + tzOffsetHours
                duskEstimate = duskSolarNoonAtThatMoment + (duskOmegaDeg / 15.0)
            }

            return Pair(dawnEstimate, duskEstimate)
        }

//        // Old helper to find hour angle for a specific altitude
//        fun getEventOffsets(targetAltDeg: Double): Pair<Double, Double>? {
//            val targetAltRad = Math.toRadians(targetAltDeg)
//            val cosOmega = (sin(targetAltRad) - sin(latRad) * sin(declination)) / (cos(latRad) * cos(declination))
//
//            // Check if the sun actually reaches this altitude (Polar day/night check)
//            if (cosOmega < -1.0 || cosOmega > 1.0) return null
//
//            val omegaDeg = Math.toDegrees(acos(cosOmega))
//            val offsetHours = omegaDeg / 15.0
//            return Pair(exactSolarNoon - offsetHours, exactSolarNoon + offsetHours)
//        }

        val sun = findPreciseEventTimes(ALT_SUNRISE_SUNSET)
        val civil = findPreciseEventTimes(ALT_CIVIL_TWILIGHT)
        val nautical = findPreciseEventTimes(ALT_NAUTICAL_TWILIGHT)
        val astro = findPreciseEventTimes(ALT_ASTRONOMICAL_TWILIGHT)

        return DailyEvents(
            solarNoon = exactSolarNoon,
            sunrise = sun?.first, sunset = sun?.second,
            dawnCivil = civil?.first, duskCivil = civil?.second,
            dawnNautical = nautical?.first, duskNautical = nautical?.second,
            dawnAstronomical = astro?.first, duskAstronomical = astro?.second
        )
    }

    // --- Extracted continuous math helpers for clean code ---

    private fun calculateGamma(dayOfYear: Int, hour: Double): Double {
        return (2.0 * PI / 365.0) * (dayOfYear - 1.0 + (hour - 12.0) / 24.0)
    }

    private fun calculateEquationOfTime(gamma: Double): Double {
        return 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma)
                - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
    }

    private fun calculateDeclination(gamma: Double): Double {
        return 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
    }

    /**
     * Calculates the current position of the Sun.
     */
    fun calculatePosition(
        time: ZonedDateTime,
        latitude: Double,
        longitude: Double
    ): SolarPosition {
        val latRad = Math.toRadians(latitude)
        val dayOfYear = time.dayOfYear
        val hour = time.hour + time.minute / 60.0 + time.second / 3600.0
        val tzOffset = time.offset.totalSeconds / 3600.0

        // Fractional Year
        val gamma = (2.0 * PI / 365.0) * (dayOfYear - 1.0 + (hour - 12.0) / 24.0)

        // Equation of Time (minutes)
        val eot = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma)
                - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))

        // Solar Declination (radians)
        val declination = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma)- 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

        // True Solar Time & Hour Angle
        val tst = (hour * 60.0) + eot + (4.0 * longitude) - (60.0 * tzOffset)
        val hourAngleDeg = (tst / 4.0) - 180.0
        val hourAngleRad = Math.toRadians(hourAngleDeg)

        // Altitude
        val sinAlt = sin(latRad) * sin(declination) + cos(latRad) * cos(declination) * cos(hourAngleRad)
        val altitudeRad = asin(sinAlt)

        // Azimuth
        val cosAz = (sin(altitudeRad) * sin(latRad) - sin(declination)) / (cos(altitudeRad) * cos(latRad))
        val rawAzimuthDeg = Math.toDegrees(acos(cosAz.coerceIn(-1.0, 1.0)))

        val azimuthDeg = if (hourAngleDeg > 0) 360.0 - rawAzimuthDeg else rawAzimuthDeg

        // Return rounded to 2 decimal places as requested
        return SolarPosition(
            altitude = round(Math.toDegrees(altitudeRad) * 100) / 100,
            azimuth = round(azimuthDeg * 100) / 100
        )
    }

    /**
     * Calculates the exact azimuth (compass direction) of the sun at a specific decimal hour.
     * Useful for finding the azimuth of sunrise, sunset, or any calculated event.
     */
    fun getAzimuthAtHour(
        date: LocalDate,
        decimalHour: Double,
        latitude: Double,
        longitude: Double,
        tzOffsetHours: Double
    ): Double {
        val latRad = Math.toRadians(latitude)
        val dayOfYear = date.dayOfYear

        // 1. Calculate continuous orbital variables for this exact moment
        val gamma = calculateGamma(dayOfYear, decimalHour)
        val declination = calculateDeclination(gamma)
        val eot = calculateEquationOfTime(gamma)

        // 2. Calculate True Solar Time (TST) and Hour Angle
        val tst = (decimalHour * 60.0) + eot + (4.0 * longitude) - (60.0 * tzOffsetHours)
        val hourAngleDeg = (tst / 4.0) - 180.0
        val hourAngleRad = Math.toRadians(hourAngleDeg)

        // 3. Calculate Altitude (required for the Azimuth formula)
        val sinAlt = sin(latRad) * sin(declination) + cos(latRad) * cos(declination) * cos(hourAngleRad)
        val altitudeRad = asin(sinAlt)

        // 4. Calculate raw Azimuth
        val cosAz = (sin(altitudeRad) * sin(latRad) - sin(declination)) / (cos(altitudeRad) * cos(latRad))

        // Coerce is necessary to prevent NaN errors from tiny floating point rounding overflows
        val rawAzimuthDeg = Math.toDegrees(acos(cosAz.coerceIn(-1.0, 1.0)))

        // 5. Adjust for morning vs. afternoon
        val azimuthDeg = if (hourAngleDeg > 0) 360.0 - rawAzimuthDeg else rawAzimuthDeg

        // Return rounded to 2 decimal places
        return round(azimuthDeg * 100) / 100.0
    }

    /**
     * Converts decimal hours to "HH:mm" string format.
     */
    fun formatDecimalHours(decimalHours: Double?): String {
        if (decimalHours == null) return "--:--" // Sun never reaches the target angle

        var hoursNormalized = decimalHours % 24.0
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
}