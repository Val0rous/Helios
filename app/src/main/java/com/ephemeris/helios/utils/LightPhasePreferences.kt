package com.ephemeris.helios.utils

data class LightPhasePreferences(
    val goldenHourRange: ClosedFloatingPointRange<Double> = -0.833..10.5, // physics -0.83..6deg, photographer -1..8deg
    val pinkHourRange: ClosedFloatingPointRange<Double> = -3.5..-0.833, // physics -0.83..-3deg, photographer -1..-3.5deg
    val blueHourRange: ClosedFloatingPointRange<Double> = -6.5..-3.5, // physics -3..-6deg, photographer -3.5..-6.5deg
    val alpenglowRange: ClosedFloatingPointRange<Double> = -2.5..2.5 // physics -2..2, photographer -2.5..2.5
)

// fun isGoldenHour(currentAltitude: Double, prefs: LightPhasePreferences): Boolean {
//     return currentAltitude in prefs.goldenHourRange
// }
//val plutoTime = findPreciseEventTimes(ALT_PLUTO_TIME)
//
//// 3. Output the results
//println("Morning Pluto Time: ${formatDecimalHours(plutoTime?.first)}")
//println("Evening Pluto Time: ${formatDecimalHours(plutoTime?.second)}")

// Todo: set up sliders to let users choose their upper bounds
// Golden hour upper: 6.0..12.0
// Pink hour lower: -4.0..-3.0
// Alpenglow: -3.0..3.0, -2.0..2.0
// Blue hour lower: -10.0..-2.0