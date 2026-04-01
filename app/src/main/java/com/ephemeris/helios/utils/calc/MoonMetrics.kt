package com.ephemeris.helios.utils.calc

import java.time.ZonedDateTime
import kotlin.math.*

object MoonMetrics {

    enum class LunarPhase(val displayName: String) {
        NEW_MOON("New Moon"),
        WAXING_CRESCENT("Waxing Crescent"),
        FIRST_QUARTER("First Quarter"),
        WAXING_GIBBOUS("Waxing Gibbous"),
        FULL_MOON("Full Moon"),
        WANING_GIBBOUS("Waning Gibbous"),
        LAST_QUARTER("Last Quarter"),
        WANING_CRESCENT("Waning Crescent")
    }

    data class LunarMetricsResult(
        val illuminationPercent: Double, // 0.0 to 100.0
        val ageDays: Double,             // 0.0 to 29.53
        val phase: LunarPhase,
        val distanceKm: Double,          // Earth-Moon distance in kilometers
        val angularDiameterDegrees: Double, // Apparent visual size in the sky
        val isSupermoon: Boolean,
        val illuminanceLux: Double,
        val shadowRatio: Double,
        val airMass: Double,
        val colorTempKelvin: Double
    )

    fun calculateMetrics(
        time: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        elevationMeters: Double = 0.0
    ): LunarMetricsResult {
        // 1. Fetch exact positional geometry from the Ephemeris engine and Convert to Julian Centuries (T) for Phase Math
        // We reuse your existing Julian Date logic, adjusted for UTC
        val decimalHour = time.hour + time.minute / 60.0 + time.second / 3600.0
        val tzOffsetHours = time.offset.totalSeconds / 3600.0
        val position = LunarEphemeris.calculatePosition(
            date = time.toLocalDate(),
            decimalHour = decimalHour,
            latitude = latitude,
            longitude = longitude,
            tzOffsetHours = tzOffsetHours,
            elevationMeters = elevationMeters
        )
        val t = calculateJulianCentury(time.year, time.monthValue, time.dayOfMonth, decimalHour, tzOffsetHours)

        // 2. Core Lunar Arguments (Meeus Chapter 47)
        // D: Moon's mean elongation (Angle between Sun and Moon)
        val dDeg = (297.8501921 + 445267.1114034 * t) % 360.0
        val dRad = Math.toRadians(dDeg)

        // M: Sun's mean anomaly
        val mDeg = (357.5291092 + 35999.0502909 * t) % 360.0
        val mRad = Math.toRadians(mDeg)

        // M': Moon's mean anomaly
        val mPrimeDeg = (134.9633964 + 477198.8675055 * t) % 360.0
        val mPrimeRad = Math.toRadians(mPrimeDeg)

        // 3. Phase Angle and Illumination (Meeus Chapter 48)
        // The phase angle (i) is the angle Sun-Moon-Earth.
        val phaseAngleDeg = 180.0 - dDeg -
                6.289 * sin(mPrimeRad) +
                2.100 * sin(mRad) -
                1.274 * sin(2 * dRad - mPrimeRad) -
                0.658 * sin(2 * dRad) -
                0.214 * sin(2 * mPrimeRad) -
                0.110 * sin(dRad)

        val phaseAngleRad = Math.toRadians(phaseAngleDeg)
        val illuminationFraction = (1.0 + cos(phaseAngleRad)) / 2.0
        val illuminationPercent = illuminationFraction * 100.0

        // 4. Moon Age (Synodic Month)
        // A full lunar cycle is exactly 29.530588853 days.
        // We normalize the elongation (D) to find exactly where we are in that cycle.
        var normalizedD = dDeg
        if (normalizedD < 0) normalizedD += 360.0
        val ageDays = (normalizedD / 360.0) * 29.530588853

        // 5. Lunar Phase Categorization
        val phase = when (normalizedD) {
            in 0.0..<6.0, in 354.0..360.0 -> LunarPhase.NEW_MOON
            in 6.0..<84.0 -> LunarPhase.WAXING_CRESCENT
            in 84.0..<96.0 -> LunarPhase.FIRST_QUARTER
            in 96.0..<174.0 -> LunarPhase.WAXING_GIBBOUS
            in 174.0..<186.0 -> LunarPhase.FULL_MOON
            in 186.0..<264.0 -> LunarPhase.WANING_GIBBOUS
            in 264.0..<276.0 -> LunarPhase.LAST_QUARTER
            in 276.0..<354.0 -> LunarPhase.WANING_CRESCENT
            else -> LunarPhase.NEW_MOON
        }

        // 7. Angular Diameter, using Ephemeris Distance (Relative Visual Size in the sky)
        // Measured in degrees. Usually ranges from 0.49 (Apogee) to 0.55 (Perigee)
        val angularDiameterDegrees = Math.toDegrees(2.0 * asin(1737.4 / position.distanceKm))

        // 8. Supermoon Calculation
        // A supermoon occurs when a Full Moon coincides with the Moon being at or near its closest
        // approach to Earth (Perigee). The astronomical standard is within 360,000 km.
        val isSupermoon = (phase == LunarPhase.FULL_MOON || illuminationPercent >= 98.0) && (position.distanceKm <= 360000.0)

        // Light and Atmosphere Metrics
        var actualAirMass = 0.0
        var illuminanceLux = 0.0
        var shadowRatio = 0.0
        var colorTempKelvin = 4100.0 // Physical baseline of reflected moonlight

        // Only calculate light if the moon is above the horizon
        if (position.altitude > 0.0) {
            val altRad = Math.toRadians(position.altitude)
            val sinAlt = sin(altRad)

            // A. Air Mass (Kasten-Young, identical to SunMetrics)
            val amDenominator = sinAlt + 0.50572 * (position.altitude + 6.07995).pow(-1.6364)
            val relativeAirMass = 1.0 / amDenominator
            // Apply elevation modifier (thinner atmosphere at high altitudes)
            val amElevationModifier = exp(-elevationMeters / 8434.0)
            actualAirMass = relativeAirMass * amElevationModifier

            // B. Visual Magnitude & Illuminance (Lux)
            // 1. Calculate the Moon's Visual Magnitude (V) outside the atmosphere.
            // Using the standard polynomial based on phase angle (alpha).
            // Notice the "Opposition Surge" (the Moon reflects light non-linearly) is accounted for here.
            val alpha = abs(phaseAngleDeg)
            var visualMagnitude = -12.73 + 0.026 * alpha + (4.0 * 10.0.pow(-9)) * alpha.pow(4)

            // 2. Adjust magnitude for current distance (closer moon = brighter)
            visualMagnitude += 5.0 * log10(position.distanceKm / 384400.0)

            // 3. Convert Magnitude to Lux (Standard Astronomical Formula)
            val exoAtmosphericLux = 10.0.pow((-visualMagnitude - 14.18) / -2.5)

            // 4. Attenuate light through the atmosphere using Air Mass
            // 0.74 is the standard clear-sky atmospheric transmittance coefficient for moonlight
            illuminanceLux = exoAtmosphericLux * 0.74.pow(actualAirMass)

            // C. Shadow Ratio
            shadowRatio = 1.0 / tan(altRad)

            // D. Color Temperature (Atmospheric Reddening)
            // Moonlight starts at ~4100K. As Air Mass increases, Rayleigh scattering
            // strips away blue light, dropping the temperature just like a sunset.
            val safeAirMass = max(1.0, actualAirMass)
            colorTempKelvin = max(2000.0, 4100.0 - (safeAirMass - 1.0) * 250.0)
        }

        return LunarMetricsResult(
            illuminationPercent = round(illuminationPercent * 10) / 10.0,
            ageDays = round(ageDays * 100) / 100.0,
            phase = phase,
            distanceKm = round(position.distanceKm),
            angularDiameterDegrees = round(angularDiameterDegrees * 1000) / 1000.0,
            isSupermoon = isSupermoon,
            illuminanceLux = illuminanceLux,
            shadowRatio = shadowRatio,
            airMass = actualAirMass,
            colorTempKelvin = colorTempKelvin
        )
    }

    /**
     * Helper to calculate Julian Centuries from J2000.0.
     * You can extract this to a shared `TimeUtils` file later since SolarEphemeris uses it too!
     */
    private fun calculateJulianCentury(year: Int, month: Int, day: Int, decimalHour: Double, tzOffsetHours: Double): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)

        val jdMidnight = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
        val utcDecimalHour = decimalHour - tzOffsetHours
        val jdExact = jdMidnight + (utcDecimalHour / 24.0)
        // Fetch Delta T from your SolarEphemeris (or a shared Utils file)
        val deltaTSeconds = calculateDeltaT(year, month)
        val ttJdExact = jdExact + (deltaTSeconds / 86400.0) // Convert seconds to days and add
        return (ttJdExact - 2451545.0) / 36525.0
    }
}