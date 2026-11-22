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
        val y = params.yValues[i]
        if (!y.isNaN()) {
            val fraction =
                ((params.xValues[i] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
            stops.add(fraction to getColor(i, params.yValues[i]))
        }
    }

    // Always map the peak explicitly so gradients peak perfectly
    val peakIndex = params.yValues.indices
        .filter { !params.yValues[it].isNaN() }
        .maxByOrNull { params.yValues[it] } ?: 0
    val peakFraction = ((params.xValues[peakIndex] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
    stops.add(peakFraction to getColor(peakIndex, params.yValues[peakIndex]))

    // Always map the end explicitly
    val lastValidIndex = params.yValues.indexOfLast { !it.isNaN() }
    if (lastValidIndex != -1) {
        val lastFraction = ((params.xValues[lastValidIndex] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
        stops.add(lastFraction to getColor(lastValidIndex, params.yValues[lastValidIndex]))
    }

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
    val fills = colors.fills
    val coolMoonFill = lerp(colors.ct5500, colors.moonPath, 0.45f).copy(alpha = fills.ct5500.alpha)
    return if (!isGradientHorizontal) {
        if (params.maxY > 6000f) {
            val topK = max(8000f, params.maxY)
            val span = (topK - 2000f).coerceAtLeast(1f)
            Brush.verticalGradient(
                0.0f to coolMoonFill,
                ((topK - 5500f) / span) to fills.ct5500,
                ((topK - 4000f) / span) to fills.ct4000,
                ((topK - 3000f) / span) to fills.ct3000,
                1.0f to fills.ct2000,
                startY = mapY(topK),
                endY = mapY(2000f)
            )
        } else {
            Brush.verticalGradient(
                0.0f to fills.ct5500,
                ((5500f - 4000f) / 3500f) to fills.ct4000, // ~0.42f
                ((5500f - 3000f) / 3500f) to fills.ct3000, // ~0.71f
                1.0f to fills.ct2000,
                startY = mapY(5500f),
                endY = mapY(2000f)
            )
        }
    } else {
        // Helper to interpolate exact Kelvin to our defined colors
        fun getCtColor(temp: Float): Color {
            return when {
                temp <= 2000f -> fills.ct2000
                temp <= 3000f -> lerp(fills.ct2000, fills.ct3000, (temp - 2000f) / 1000f)
                temp <= 4000f -> lerp(fills.ct3000, fills.ct4000, (temp - 3000f) / 1000f)
                temp <= 5500f -> lerp(fills.ct4000, fills.ct5500, (temp - 4000f) / 1500f)
                temp < 8000f -> lerp(fills.ct5500, coolMoonFill, (temp - 5500f) / 2500f)
                else -> coolMoonFill
            }
        }

        // Dynamically generate horizontal stops based on the actual data
        val ctStops = mutableListOf<Pair<Float, Color>>()

        // Sample points to create a smooth gradient (every ~12 points is plenty for 481 items)
        val step = max(1, params.xValues.size / 40)
        for (i in params.xValues.indices step step) {
            val y = params.yValues[i]
            if (!y.isNaN()) {
                val fraction = ((params.xValues[i] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
                ctStops.add(fraction to getCtColor(params.yValues[i]))
            }
        }

        // Ensure the exact peak temperature is mathematically represented
        val peakIndex = params.yValues.indices
            .filter { !params.yValues[it].isNaN() }
            .maxByOrNull { params.yValues[it] } ?: 0
        val peakFraction = ((params.xValues[peakIndex] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
        ctStops.add(peakFraction to getCtColor(params.yValues[peakIndex]))

        // Ensure the very last point is included to reach endX safely
        val lastValidIndex = params.yValues.indexOfLast { !it.isNaN() }
        if (lastValidIndex != -1) {
            val lastFraction = ((params.xValues[lastValidIndex] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
            ctStops.add(lastFraction to getCtColor(params.yValues[lastValidIndex]))
        }

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