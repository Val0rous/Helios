package com.ephemeris.helios.utils.calc

import kotlin.math.tan

/**
 * Calculates Delta T (Terrestrial Time - Universal Time) in seconds.
 * Uses the Espenak and Meeus (2006) polynomial approximations.
 */
internal fun calculateDeltaT(year: Int, month: Int): Double {
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
 * Converts a True Geometric Altitude (where the body mathematically is)
 * into an Apparent Visual Altitude (where the eye actually sees it).
 */
fun Double.applyAtmosphericRefraction(): Double {
    // Refraction math physically breaks down and explodes to infinity deep below the horizon.
    // In ephemeris software, we only apply dynamic refraction if the body is above -2.0 degrees.
    if (this < -2.0) {
        return this
    }

    // Sæmundsson’s Formula for True-to-Apparent refraction
    // Calculates the atmospheric bend in arcminutes
    val rArcMinutes = 1.02 / tan(Math.toRadians(this + 10.3 / (this + 5.11)))

    // Convert arcminutes to decimal degrees and add it to the physical altitude
    return this + (rArcMinutes / 60.0)
}