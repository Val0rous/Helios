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
        val isSupermoon: Boolean
    )

    fun calculateMetrics(time: ZonedDateTime): LunarMetricsResult {
        // 1. Convert to Julian Centuries (T)
        // We reuse your existing Julian Date logic, adjusted for UTC
        val decimalHour = time.hour + time.minute / 60.0 + time.second / 3600.0
        val tzOffsetHours = time.offset.totalSeconds / 3600.0
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

        // 6. Earth-Moon Distance (Meeus Chapter 47, truncated for extreme accuracy)
        // Calculates the distance in kilometers using the top perturbation waves.
        val distanceKm = 385000.56 -
                20905.15 * cos(mPrimeRad) -
                3699.11 * cos(2 * dRad - mPrimeRad) -
                2955.96 * cos(2 * dRad) -
                569.92 * cos(2 * mPrimeRad) +
                246.15 * cos(2 * dRad - 2 * mPrimeRad) -
                204.58 * cos(mPrimeRad - mRad) -
                170.73 * cos(dRad) -
                152.13 * cos(mPrimeRad + mRad)

        // 7. Angular Diameter (Relative Visual Size in the sky)
        // Measured in degrees. Usually ranges from 0.49 (Apogee) to 0.55 (Perigee)
        val angularDiameterDegrees = Math.toDegrees(2.0 * asin(1737.4 / distanceKm))

        // 8. Supermoon Calculation
        // A supermoon occurs when a Full Moon coincides with the Moon being at or near its closest
        // approach to Earth (Perigee). The astronomical standard is within 360,000 km.
        val isSupermoon = (phase == LunarPhase.FULL_MOON || illuminationPercent >= 98.0) && (distanceKm <= 360000.0)

        return LunarMetricsResult(
            illuminationPercent = round(illuminationPercent * 10) / 10.0,
            ageDays = round(ageDays * 100) / 100.0,
            phase = phase,
            distanceKm = round(distanceKm),
            angularDiameterDegrees = round(angularDiameterDegrees * 1000) / 1000.0,
            isSupermoon = isSupermoon
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
        val deltaTSeconds = SolarEphemeris.calculateDeltaT(year, month)
        val ttJdExact = jdExact + (deltaTSeconds / 86400.0) // Convert seconds to days and add
        return (ttJdExact - 2451545.0) / 36525.0
    }
}