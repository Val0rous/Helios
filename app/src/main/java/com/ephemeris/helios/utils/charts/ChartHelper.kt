package com.ephemeris.helios.utils.charts

import com.ephemeris.helios.utils.Charts
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt


fun mapX(
    x: Float,
    minX: Float,
    maxX: Float,
    width: Float
): Float {
    return ((x - minX) / (maxX - minX)) * width
}

// Canvas Y=0 is at the top, so we invert the Y mapping
fun mapY(
    y: Float,
    minY: Float,
    maxY: Float,
    height: Float,
    drawHeight: Float,
    verticalPaddingPx: Float,
    isLogScale: Boolean = false
): Float {
    return if (isLogScale) {
        // log10(y + 1) safely handles 0 values without throwing negative infinity
        val logY = log10(y.coerceAtLeast(0f) + 1f)
        val logMin = log10(minY.coerceAtLeast(0f) + 1f)
        val logMax = log10(maxY.coerceAtLeast(0f) + 1f)

        val fraction = if (logMax == logMin) 0f else (logY - logMin) / (logMax - logMin)
        height - verticalPaddingPx - (fraction * drawHeight)
    } else {
        val fraction = if (maxY == minY) 0f else (y - minY) / (maxY - minY)
        height - verticalPaddingPx - (fraction * drawHeight)
    }
}

fun getMinX(xValues: FloatArray, chartType: Charts): Float {
    return when (chartType) {
        Charts.Sun.Daily.Trajectory, Charts.Moon.Daily.Trajectory -> 0f
        else -> 0f //xValues.minOrNull() ?: 0f
    }
}

fun getMaxX(xValues: FloatArray, chartType: Charts): Float {
    return when (chartType) {
        Charts.Sun.Daily.Trajectory, Charts.Moon.Daily.Trajectory -> 360f
        else -> 24f //xValues.maxOrNull() ?: 24f
    }
}

fun getMinY(yValues: FloatArray, chartType: Charts): Float {
    return when (chartType) {
        Charts.Sun.Daily.Elevation, Charts.Moon.Daily.Elevation,
             Charts.Sun.Daily.Trajectory, Charts.Moon.Daily.Trajectory -> -90f
        Charts.Sun.Daily.ColorTemperature -> 2000f
        else -> 0f
    }
}

fun getMaxY(yValues: FloatArray, chartType: Charts): Float {
    return when (chartType) {
        Charts.Sun.Daily.Elevation, Charts.Moon.Daily.Elevation,
             Charts.Sun.Daily.Trajectory, Charts.Moon.Daily.Trajectory -> 90f
        Charts.Sun.Daily.Irradiance -> max(500f, (yValues.max() / 100.0).roundToInt() * 100f)
        Charts.Sun.Daily.UvIntensity -> max(5f, yValues.max().roundToInt().toFloat())
        Charts.Sun.Daily.Illuminance -> max(100000f, yValues.max())
        Charts.Sun.Daily.Shadows -> 10f
        Charts.Sun.Daily.ColorTemperature -> max(5500f, yValues.max())
        Charts.Sun.Daily.AirMass -> 10f // or 15f
        else -> 90f // Todo: Change
    }
}