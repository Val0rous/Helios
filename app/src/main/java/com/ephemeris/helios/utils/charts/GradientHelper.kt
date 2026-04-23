package com.ephemeris.helios.utils.charts

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.ephemeris.helios.ui.theme.CustomColorScheme
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
    params: ChartData,
    colors: CustomColorScheme
): Brush {
    return if (!isGradientHorizontal) {
        Brush.verticalGradient(
            0.0f to colors.ct5500,
            ((5500f - 4000f) / 3500f) to colors.ct4000, // ~0.42f
            ((5500f - 3000f) / 3500f) to colors.ct3000, // ~0.71f
            1.0f to colors.ct2000,
            startY = mapY(5500f),
            endY = mapY(2000f)
        )
    } else {
        // Helper to interpolate exact Kelvin to our defined colors
        fun getCtColor(temp: Float): Color {
            return when {
                temp <= 2000f -> colors.ct2000
                temp <= 3000f -> lerp(colors.ct2000, colors.ct3000, (temp - 2000f) / 1000f)
                temp <= 4000f -> lerp(colors.ct3000, colors.ct4000, (temp - 3000f) / 1000f)
                temp < 5500f -> lerp(colors.ct4000, colors.ct5500, (temp - 4000f) / 1500f)
                else -> colors.ct5500
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