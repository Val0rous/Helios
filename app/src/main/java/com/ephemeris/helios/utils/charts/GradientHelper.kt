package com.ephemeris.helios.utils.charts

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.max

// --- HELPER: Reusable Horizontal Gradient Generator ---
// Extracts the complex stop-generation logic to keep code DRY
fun createHorizontalBrush(getColor: (index: Int, value: Float) -> Color, params: ChartData): Brush {
    val stops = mutableListOf<Pair<Float, Color>>()
    val step = max(1, params.xValues.size / 40)

    for (i in params.xValues.indices step step) {
        val fraction = ((params.xValues[i] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
        stops.add(fraction to getColor(i, params.yValues[i]))
    }

    // Always map the peak explicitly so gradients peak perfectly
    val peakIndex = params.yValues.indices.maxByOrNull { params.yValues[it] } ?: 0
    val peakFraction = ((params.xValues[peakIndex] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
    stops.add(peakFraction to getColor(peakIndex, params.yValues[peakIndex]))

    // Always map the end explicitly
    val lastIndex = params.xValues.lastIndex
    val lastFraction = ((params.xValues[lastIndex] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
    stops.add(lastFraction to getColor(lastIndex, params.yValues.last()))

    val finalStops = stops
        .distinctBy { it.first }
        .sortedBy { it.first }
        .toTypedArray()

    return Brush.horizontalGradient(*finalStops, startX = 0f, endX = params.width)
}

fun getColorTemperatureBrushGradient(
    isGradientHorizontal: Boolean = false,
    mapY: (Float) -> Float,
    params: ChartData
): Brush {
    val ct5500 = Color(0xFF81D4FA).copy(alpha = 0.5f) // Daylight Cool Blue
    val ct4000 = Color(0xFFFFF59D).copy(alpha = 0.5f) // Warm Pale Yellow
    val ct3000 = Color(0xFFFFB300).copy(alpha = 0.5f) // Golden Amber
    val ct2000 = Color(0xFFD84315).copy(alpha = 0.5f) // Deep Sunset Red

    return if (!isGradientHorizontal) {
        Brush.verticalGradient(
            0.0f to ct5500,
            ((5500f - 4000f) / 3500f) to ct4000, // ~0.42f
            ((5500f - 3000f) / 3500f) to ct3000, // ~0.71f
            1.0f to ct2000,
            startY = mapY(5500f),
            endY = mapY(2000f)
        )
    } else {
        // Helper to interpolate exact Kelvin to our defined colors
        fun getCtColor(temp: Float): Color {
            return when {
                temp <= 2000f -> ct2000
                temp <= 3000f -> lerp(ct2000, ct3000, (temp - 2000f) / 1000f)
                temp <= 4000f -> lerp(ct3000, ct4000, (temp - 3000f) / 1000f)
                temp < 5500f -> lerp(ct4000, ct5500, (temp - 4000f) / 1500f)
                else -> ct5500
            }
        }

        // Dynamically generate horizontal stops based on the actual data
        val ctStops = mutableListOf<Pair<Float, Color>>()

        // Sample points to create a smooth gradient (every ~12 points is plenty for 481 items)
        val step = max(1, params.xValues.size / 40)
        for (i in params.xValues.indices step step) {
            val fraction = ((params.xValues[i] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
            ctStops.add(fraction to getCtColor(params.yValues[i]))
        }

        // Ensure the exact peak temperature is mathematically represented
        val peakIndex = params.yValues.indices.maxByOrNull { params.yValues[it] } ?: 0
        val peakFraction = ((params.xValues[peakIndex] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
        ctStops.add(peakFraction to getCtColor(params.yValues[peakIndex]))

        // Ensure the very last point is included to reach endX safely
        val lastFraction = ((params.xValues.last() - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
        ctStops.add(lastFraction to getCtColor(params.yValues.last()))

        // Skia requires strictly ascending fractions without duplicates
        val finalStops = ctStops
            .distinctBy { it.first }
            .sortedBy { it.first }
            .toTypedArray()

        Brush.horizontalGradient(
            *finalStops,
            startX = 0f,
            endX = params.width
        )
    }
}