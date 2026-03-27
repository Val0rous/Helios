package com.ephemeris.helios.utils

import java.time.LocalDate
import java.time.LocalDateTime
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
        val solarNoonAltitude: Double,
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
        val duskAstronomical: Double?,
        val dayLength: Double
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

        // 1. Iterative Solar Noon (Meeus)
        var exactSolarNoon = 12.0 - (longitude / 15.0) + tzOffsetHours
        for (i in 1..2) {
            val t = calculateJulianCentury(date, exactSolarNoon, tzOffsetHours)
            val params = calculateSolarParams(t)
            exactSolarNoon = 12.0 - (longitude / 15.0) - (params.eotMinutes / 60.0) + tzOffsetHours
        }

        // 2. The Iterative Event Engine for finding specific altitude times
        fun findPreciseEventTimes(targetAltDeg: Double): Pair<Double, Double>? {
            val targetAltRad = Math.toRadians(targetAltDeg)

            // Start with rough guesses: 6 AM for morning, 6 PM for evening
            var dawnEstimate = 6.0
            var duskEstimate = 18.0

            // Iterate 3 times for sub-second convergence
            for (i in 1..3) {
                // Refine Dawn
                val dawnT = calculateJulianCentury(date, dawnEstimate, tzOffsetHours)
                val dawnParams = calculateSolarParams(dawnT)
                val dawnCosOmega = (sin(targetAltRad) - sin(latRad) * sin(dawnParams.declinationRad)) / (cos(latRad) * cos(dawnParams.declinationRad))

                if (dawnCosOmega < -1.0 || dawnCosOmega > 1.0) return null // Polar day/night

                val dawnOmegaDeg = Math.toDegrees(acos(dawnCosOmega))
                val dawnSolarNoonAtThatMoment = 12.0 - (longitude / 15.0) - (dawnParams.eotMinutes / 60.0) + tzOffsetHours
                dawnEstimate = dawnSolarNoonAtThatMoment - (dawnOmegaDeg / 15.0)

                // Refine Dusk
                val duskT = calculateJulianCentury(date, duskEstimate, tzOffsetHours)
                val duskParams = calculateSolarParams(duskT)
                val duskCosOmega = (sin(targetAltRad) - sin(latRad) * sin(duskParams.declinationRad)) / (cos(latRad) * cos(duskParams.declinationRad))

                val duskOmegaDeg = Math.toDegrees(acos(duskCosOmega))
                val duskSolarNoonAtThatMoment = 12.0 - (longitude / 15.0) - (duskParams.eotMinutes / 60.0) + tzOffsetHours
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

        // 3. Populate Azimuths for core events
        val noonPos = getPositionAtHour(date, exactSolarNoon, latitude, longitude, tzOffsetHours)

        val sunrisePos = sun?.first?.let { getPositionAtHour(date, it, latitude, longitude, tzOffsetHours) }
        val sunsetPos = sun?.second?.let { getPositionAtHour(date, it, latitude, longitude, tzOffsetHours) }

        val dayLength = getExactDayLength(sun?.first, sun?.second, noonPos.altitude)

        return DailyEvents(
            solarNoon = exactSolarNoon,
            solarNoonAltitude = noonPos.altitude,
            solarNoonAzimuth = noonPos.azimuth,
            sunrise = sun?.first, sunriseAzimuth = sunrisePos?.azimuth,
            sunset = sun?.second, sunsetAzimuth = sunsetPos?.azimuth,
            dawnCivil = civil?.first, duskCivil = civil?.second,
            dawnNautical = nautical?.first, duskNautical = nautical?.second,
            dawnAstronomical = astro?.first, duskAstronomical = astro?.second,
            dayLength = dayLength
        )
    }

    // --- NOAA / Meeus Mathematical Core ---

    private data class SolarParams(val declinationRad: Double, val eotMinutes: Double)

    private fun calculateJulianCentury(date: LocalDate, decimalHour: Double, tzOffsetHours: Double): Double {
        var y = date.year
        var m = date.monthValue
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)

        // Julian Day at 00:00 UTC
        val jdMidnight = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + date.dayOfMonth + b - 1524.5

        // Delta T (Terrestrial Time - Universal Time) in seconds.
        // Roughly ~69 seconds for the 2020s. Crucial for sub-minute accuracy.
        val deltaT = calculateDeltaT(date.year, date.monthValue)
        // Convert Local Timme to UTC, then add Delta T to achieve Terrestrial Time (TT)
        val utcDecimalHour = decimalHour - tzOffsetHours
        val ttDecimalHour = utcDecimalHour + (deltaT / 3600.0)
        val jdExact = jdMidnight + (utcDecimalHour / 24.0)

        return (jdExact - 2451545.0) / 36525.0
    }

    private fun calculateSolarParams(t: Double): SolarParams {
        // Geometric Mean Longitude
        val l0 = (280.46646 + 36000.76983 * t + 0.0003032 * t * t) % 360.0
        val l0Rad = Math.toRadians(l0)

        // Geometric Mean Anomaly
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val mRad = Math.toRadians(m)

        // Eccentricity of Earth Orbit
        val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t

        // Sun Equation of Center
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
                (0.019993 - 0.000101 * t) * sin(2 * mRad) +
                0.000289 * sin(3 * mRad)

        val sunTrueLong = l0 + c
        val sunAppLongRad = Math.toRadians(sunTrueLong - 0.00569 - 0.00478 * sin(Math.toRadians(125.04 - 1934.136 * t)))

        // Mean Obliquity of Ecliptic
        val epsilon0 = 23.0 + 26.0 / 60.0 + 21.448 / 3600.0 - (46.815 / 3600.0) * t - (0.00059 / 3600.0) * t * t + (0.001813 / 3600.0) * t * t * t
        val epsilonRad = Math.toRadians(epsilon0 + 0.00256 * cos(Math.toRadians(125.04 - 1934.136 * t)))

        // Declination
        val declinationRad = asin(sin(epsilonRad) * sin(sunAppLongRad))

        // Equation of Time (minutes)
        val y = tan(epsilonRad / 2.0) * tan(epsilonRad / 2.0)
        val eotMinutes = 4.0 * Math.toDegrees(
            y * sin(2 * l0Rad) -
                    2 * e * sin(mRad) +
                    4 * e * y * sin(mRad) * cos(2 * l0Rad) -
                    0.5 * y * y * sin(4 * l0Rad) -
                    1.25 * e * e * sin(2 * mRad)
        )

        return SolarParams(declinationRad, eotMinutes)
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
        val decimalHour = time.hour + time.minute / 60.0 + time.second / 3600.0
        val tzOffsetHours = time.offset.totalSeconds / 3600.0

        return getPositionAtHour(time.toLocalDate(), decimalHour, latitude, longitude, tzOffsetHours)
    }

    /**
     * Calculates both exact altitude and azimuth (compass direction) of the sun at a specific decimal hour.
     * Useful for finding the azimuth of sunrise, sunset, or any calculated event.
     */
    fun getPositionAtHour(
        date: LocalDate,
        decimalHour: Double,
        latitude: Double,
        longitude: Double,
        tzOffsetHours: Double
    ): SolarPosition {
        val latRad = Math.toRadians(latitude)

        // 1. Calculate continuous orbital variables for this exact moment
        val t = calculateJulianCentury(date, decimalHour, tzOffsetHours)
        val params = calculateSolarParams(t)

        // 2. Calculate True Solar Time (TST) and Hour Angle
        val tst = (decimalHour * 60.0) + params.eotMinutes + (4.0 * longitude) - (60.0 * tzOffsetHours)
        val hourAngleDeg = (tst / 4.0) - 180.0
        val hourAngleRad = Math.toRadians(hourAngleDeg)

        // 3. Calculate Altitude
        val sinAlt = sin(latRad) * sin(params.declinationRad) + cos(latRad) * cos(params.declinationRad) * cos(hourAngleRad)
        val altitudeRad = asin(sinAlt.coerceIn(-1.0, 1.0))

        // 4. Calculate raw Azimuth
        val cosAz = (sin(params.declinationRad) - sin(altitudeRad) * sin(latRad)) / (cos(altitudeRad) * cos(latRad))

        // Coerce is necessary to prevent NaN errors from tiny floating point rounding overflows
        val rawAzimuthDeg = Math.toDegrees(acos(cosAz.coerceIn(-1.0, 1.0)))

        // 5. Adjust for morning vs. afternoon
        val azimuthDeg = if (hourAngleDeg > 0) 360.0 - rawAzimuthDeg else rawAzimuthDeg

        // Return rounded to 2 decimal places
        return SolarPosition(
            altitude = round(Math.toDegrees(altitudeRad) * 100) / 100.0,
            azimuth = round(azimuthDeg * 100) / 100.0
        )
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

    /**
     * Calculates Delta T (Terrestrial Time - Universal Time) in seconds.
     * Uses the Espenak and Meeus (2006) polynomial approximations.
     */
    private fun calculateDeltaT(year: Int, month: Int): Double {
        // Fractional year for polynomial calculation
        val y = year + (month - 0.5) / 12.0

        return when {
            // Modern era (2005 - 2050)
            year in 2005..2050 -> {
                val t = y - 2000.0
                // Note: We use a slight hybrid correction here.
                // The strict Espenak formula is: 62.92 + 0.32217 * t + 0.005589 * t * t
                // But to account for the recent anomalous speed-up in the 2020s,
                // returning ~69.2 for the current decade is more accurate than the raw polynomial.
                if (year in 2020..2028) {
                    69.2
                } else {
                    62.92 + 0.32217 * t + 0.005589 * t * t
                }
            }

            // Late 20th Century (1986 - 2005)
            year in 1986..<2005 -> {
                val t = y - 2000.0
                63.86 + 0.3345 * t - 0.060374 * t * t + 0.0017275 * t * t * t + 0.000651814 * t * t * t * t + 0.00002373599 * t * t * t * t * t
            }

            // Space Age (1961 - 1986)
            year in 1961..<1986 -> {
                val t = y - 1975.0
                45.45 + 1.067 * t - 0.026 * t * t - 0.00181 * t * t * t
            }

            // World War II Era (1941 - 1961)
            year in 1941..<1961 -> {
                val t = y - 1950.0
                29.07 + 0.407 * t - 0.0041 * t * t + 0.000209 * t * t * t
            }

            // Early 20th Century (1920 - 1941)
            year in 1920..<1941 -> {
                val t = y - 1920.0
                21.20 + 0.84493 * t - 0.076100 * t * t + 0.0020936 * t * t * t
            }

            // Turn of the Century (1900 - 1920)
            year in 1900..<1920 -> {
                val t = y - 1900.0
                -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t * t * t - 0.000197 * t * t * t * t
            }

            // Future Fallback (2050 - 2150)
            year > 2050 -> {
                val t = y - 2000.0
                // A generalized parabola for the distant future
                62.92 + 0.32217 * t + 0.005589 * t * t
            }

            // Distant Past Fallback (Before 1900)
            else -> {
                val t = (y - 2000.0) / 100.0
                // Standard long-term approximation
                -20.0 + 32.0 * t * t
            }
        }
    }

    /**
     * Calculates and formats the exact duration of daylight.
     */
    fun getExactDayLength(sunriseHours: Double?, sunsetHours: Double?, solarNoonAltitude: Double): Double {
        // 1. Handle edge cases (Polar day/night)
        if (sunriseHours == null || sunsetHours == null) {
            return if (solarNoonAltitude > ALT_SUNRISE_SUNSET) {
                24.0 // Polar Day (Midnight Sun)
            } else {
                0.0 // Polar Night
            }
        }

        // 2. Calculate raw duration
        var durationHours = sunsetHours - sunriseHours

        // 3. Handle midnight wrap-around (if sunset spills into the next day mathematically)
        if (durationHours < 0) {
            durationHours += 24.0
        }

        return durationHours
    }
}