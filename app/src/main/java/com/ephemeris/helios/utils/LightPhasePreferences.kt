package com.ephemeris.helios.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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

@Composable
fun LightPhaseSettings() {
    // State to hold the user's preference
    var goldenHourUpper by remember { mutableFloatStateOf(10.5f) }

    Column(modifier = Modifier.padding(16.dp)) {
        // Formatted display text
        Text(
            text = "Golden Hour Upper Bound: ${String.format("%.1f", goldenHourUpper)}°",
            style = MaterialTheme.typography.titleMedium
        )

        Slider(
            value = goldenHourUpper,
            onValueChange = { newValue ->
                // Ensures floating point math doesn't result in 10.5000001
                goldenHourUpper = (newValue * 10f).roundToInt() / 10f
            },
            valueRange = 6.0f..12.0f,
            // Calculate steps for 0.1 increments
            steps = 59
        )
    }
}