package com.ephemeris.helios.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.cos

object SeasonalEphemeris {
    data class SeasonalEvents(
        val marchEquinox: ZonedDateTime,
        val juneSolstice: ZonedDateTime,
        val septemberEquinox: ZonedDateTime,
        val decemberSolstice: ZonedDateTime
    )

    // A static array of the top 15 highest-impact perturbation terms from Meeus Table 27.C.
    // Format: [A, B, C]
    private val PERIODIC_TERMS = arrayOf(
        doubleArrayOf(485.0, 324.96, 1934.136),
        doubleArrayOf(203.0, 337.23, 32964.467),
        doubleArrayOf(199.0, 342.08, 20.186),
        doubleArrayOf(182.0, 27.85, 445267.112),
        doubleArrayOf(156.0, 73.14, 45036.886),
        doubleArrayOf(136.0, 171.52, 22518.443),
        doubleArrayOf(77.0, 222.54, 65928.934),
        doubleArrayOf(74.0, 296.72, 3034.906),
        doubleArrayOf(70.0, 343.98, 83996.857),
        doubleArrayOf(58.0, 119.81, 529.691),
        doubleArrayOf(52.0, 297.65, 71430.700),
        doubleArrayOf(50.0, 320.81, 156.980),
        doubleArrayOf(45.0, 227.73, 4282.660),
        doubleArrayOf(40.0, 312.59, 5273.629),
        doubleArrayOf(18.0, 302.77, 10813.62)
    )

    data class SeasonalDailyEvents(
        val marchEquinoxDaylight: Double,
        val juneSolsticeDaylight: Double,
        val septemberEquinoxDaylight: Double,
        val decemberSolsticeDaylight: Double,
        val marchEquinoxSunAngle: Double,
        val juneSolsticeSunAngle: Double,
        val septemberEquinoxSunAngle: Double,
        val decemberSolsticeSunAngle: Double
    )

    fun getDaily(dt: ZonedDateTime, coordinates: Coordinates): SolarEphemeris.DailyEvents = SolarEphemeris.calculateDailyEvents(
        date = dt.toLocalDate(),
        latitude = coordinates.latitude,
        longitude = coordinates.longitude,
        tzOffsetHours = dt.offset.totalSeconds / 3600.0
    )

    /**
     * Calculates the exact time of the March Equinox for a given year.
     * Returns a ZonedDateTime adjusted to the user's local timezone.
     */
    fun getMarchEquinox(year: Int, zoneId: ZoneId): ZonedDateTime {
        // 1. Calculate Julian Millennium (Y) from the year 2000
        val m = (year - 2000) / 1000.0

        // 2. Meeus Polynomial for March Equinox (Julian Ephemeris Days)
        val jde0 = 2451623.80984 +
                365242.37404 * m +
                0.05169 * (m * m) -
                0.00411 * (m * m * m) -
                0.00057 * (m * m * m * m)

        // 3. Convert Julian Days back to standard Unix time
        return julianToZonedTime(jde0, zoneId)
    }

    /**
     * Calculates the exact time of the June Solstice.
     */
    fun getJuneSolstice(year: Int, zoneId: ZoneId): ZonedDateTime {
        val m = (year - 2000) / 1000.0
        val jde0 = 2451716.56767 +
                365241.62603 * m +
                0.00325 * (m * m) +
                0.00888 * (m * m * m) -
                0.00030 * (m * m * m * m)

        return julianToZonedTime(jde0, zoneId)
    }

    /**
     * Calculates the exact time of the September Equinox.
     * Often referred to as the Autumnal Equinox in the Northern Hemisphere.
     */
    fun getSeptemberEquinox(year: Int, zoneId: ZoneId): ZonedDateTime {
        val m = (year - 2000) / 1000.0

        val jde0 = 2451810.21715 +
                365242.01767 * m -
                0.11575 * (m * m) +
                0.00337 * (m * m * m) +
                0.00078 * (m * m * m * m)

        return julianToZonedTime(jde0, zoneId)
    }

    /**
     * Calculates the exact time of the December Solstice.
     * Often referred to as the Winter Solstice in the Northern Hemisphere.
     */
    fun getDecemberSolstice(year: Int, zoneId: ZoneId): ZonedDateTime {
        val m = (year - 2000) / 1000.0

        val jde0 = 2451900.05952 +
                365242.74049 * m -
                0.06223 * (m * m) -
                0.00823 * (m * m * m) +
                0.00032 * (m * m * m * m)

        return julianToZonedTime(jde0, zoneId)
    }

    /**
     * Applies periodic planetary perturbations to convert the Mean JDE to the True JDE.
     */
    private fun getTrueEquinoxSolstice(jde0: Double): Double {
        // T is Julian centuries from J2000.0 (Note: Not millennia like 'm')
        val t = (jde0 - 2451545.0) / 36525.0

        // Calculate W (Sun's mean longitude) in radians for Kotlin math
        val wDeg = 35999.373 * t - 2.47
        val wRad = Math.toRadians(wDeg)

        // Calculate delta lambda (variation in Sun's apparent speed)
        val deltaLambda = 1.0 + 0.0334 * cos(wRad) + 0.0007 * cos(2.0 * wRad)

        // Sum the periodic terms
        var s = 0.0
        for (term in PERIODIC_TERMS) {
            val a = term[0]
            val b = term[1]
            val c = term[2]
            val angleRad = Math.toRadians(b + c * t)
            s += a * cos(angleRad)
        }

        // Apply the correction to the base Julian Date
        return jde0 + (0.00001 * s) / deltaLambda
    }

    /**
     * Converts a Julian Ephemeris Day into a usable ZonedDateTime (UTC).
     */
    private fun julianToZonedTime(jde: Double, zoneId: ZoneId): ZonedDateTime {
        // Delta T approximation (roughly 69 seconds for the 2020s)
        val deltaTSeconds = 69.0
        val deltaTDays = deltaTSeconds / 86400.0

        // Convert Terrestrial Time to Universal Time by subtracting Delta T
        val jdUt = jde - deltaTDays

        // Standard Julian epoch starts at noon on Jan 1, 4713 BC.
        // Unix epoch (Jan 1, 1970) is Julian Day 2440587.5
        val unixTimeSeconds = (jdUt - 2440587.5) * 86400.0

        val instant = Instant.ofEpochMilli((unixTimeSeconds * 1000).toLong())
        return ZonedDateTime.ofInstant(instant, zoneId)
    }

    fun getSeasonalEvents(year: Int, zoneId: ZoneId): SeasonalEvents {
        return SeasonalEvents(
            getMarchEquinox(year, zoneId),
            getJuneSolstice(year, zoneId),
            getSeptemberEquinox(year, zoneId),
            getDecemberSolstice(year, zoneId)
        )
    }
}