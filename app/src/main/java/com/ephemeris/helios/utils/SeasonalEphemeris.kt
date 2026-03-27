package com.ephemeris.helios.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object SeasonalEphemeris {

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

    // --- Core Helper ---

    /**
     * Converts a Julian Ephemeris Day into a usable Java/Kotlin ZonedDateTime.
     */
    private fun julianToZonedTime(jde: Double, zoneId: ZoneId): ZonedDateTime {
        // Standard Julian epoch starts at noon on Jan 1, 4713 BC.
        // Unix epoch (Jan 1, 1970) is Julian Day 2440587.5
        val unixTimeSeconds = (jde - 2440587.5) * 86400.0

        val instant = Instant.ofEpochMilli((unixTimeSeconds * 1000).toLong())
        return ZonedDateTime.ofInstant(instant, zoneId)
    }
}