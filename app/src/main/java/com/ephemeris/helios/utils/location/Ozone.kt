package com.ephemeris.helios.utils.location

import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun estimateHistoricalOzone(latitude: Double, date: LocalDate): Double {
    // 1. Convert latitude to radians for math functions
    val latRad = Math.toRadians(latitude)
    val dayOfYear = date.dayOfYear

    // 2. Base ozone increases from the equator (~275) to the poles (~375)
    // Using sin^2 creates a smooth curve that stays flat at the equator and rises sharply later
    val baseOzone = 275.0 + (100.0 * Math.pow(Math.sin(latRad), 2.0))

    // 3. Seasonal variation (Amplitude)
    // Variation is 0 at the equator, and up to 50 DU at the poles
    val amplitude = 50.0 * Math.pow(Math.sin(latRad), 2.0)

    // 4. Determine the Spring Peak based on the hemisphere
    // Northern Hemisphere spring peak is roughly mid-April (Day 105)
    // Southern Hemisphere spring peak is roughly mid-October (Day 288)
    val isNorthernHemisphere = latitude >= 0
    val peakDay = if (isNorthernHemisphere) 105.0 else 288.0

    // 5. Calculate the standard seasonal shift using a Cosine wave
    val seasonalShift = amplitude * Math.cos((2 * Math.PI / 365.25) * (dayOfYear - peakDay))

    var estimatedDu = baseOzone + seasonalShift

    // 5. THE ANTARCTIC ANOMALY PATCH (The Ozone Hole)
    // Only triggers if the user is deep in the Southern Hemisphere (below -60 latitude)
    if (latitude <= -60.0) {

        // The hole opens in September (Day 244) and closes in November (Day 334)
        if (dayOfYear in 244..334) {

            // The hole is at its absolute worst in early October (approx Day 280)
            val peakDepletionDay = 280.0

            // Calculate how many days we are from the absolute worst day
            val daysFromPeak = Math.abs(dayOfYear - peakDepletionDay)

            // Calculate the drop. It drops by up to 200 DU at the center, tapering to 0 at the edges of the season
            val depletionDrop = 200.0 * (1.0 - (daysFromPeak / 45.0))

            // Subtract the massive drop from our initial estimate
            estimatedDu -= depletionDrop
        }
    }

    // 6. Return the final estimated Dobson Units
    return estimatedDu
}


// Use clear day UV index to estimate ozone DU for a location, using altitude from weather API
fun reverseEngineerOzone(
    hourlyClearSkyUv: List<Double>,
    solarNoonAltitudeDegrees: Double,
    elevationMeters: Double
): Double {

    // 1. Find the highest UVI of the day (Solar Noon)
    val peakUvi = hourlyClearSkyUv.maxOrNull() ?: 0.0

    // Safety check: In deep winter near the poles, UVI might be near 0 all day.
    // Return the baseline 300 DU to avoid dividing by zero.
    if (peakUvi < 0.5 || solarNoonAltitudeDegrees <= 0) {
        return 300.0
    }

    // 2. Convert Solar Altitude to Radians for the math library
    val altRad = Math.toRadians(solarNoonAltitudeDegrees)

    // 3. Calculate the Elevation Multiplier (1 + 5% per 1000m)
    val elevMod = 1.0 + (0.05 * (elevationMeters / 1000.0))

    // 4. Calculate the base denominator from the formula
    val denominator = 12.5 * sin(altRad).pow(2.42) * elevMod

    // 5. Run the final algebraic reversal
    val calculatedDu = 300.0 / sqrt(peakUvi / denominator)

    // 6. Clamp the result to realistic atmospheric bounds (e.g., 100 to 500 DU)
    return calculatedDu.coerceIn(100.0, 500.0)
}