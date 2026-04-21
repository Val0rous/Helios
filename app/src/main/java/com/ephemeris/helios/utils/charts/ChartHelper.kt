package com.ephemeris.helios.utils.charts

import com.ephemeris.helios.utils.Charts
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt


fun getMapX(
    x: Float,
    params: ChartData
): Float {
    return ((x - params.minX) / (params.maxX - params.minX)) * params.width
}

// Canvas Y=0 is at the top, so we invert the Y mapping
fun getMapY(
    y: Float,
    params: ChartData,
    chartType: Charts
): Float {
    val drawHeight = (params.height - (2 * params.verticalPaddingPx)).coerceAtLeast(1f)
    val isLogScale = when (chartType) {
        Charts.Sun.Daily.Illuminance, Charts.Sun.Daily.Shadows, Charts.Sun.Daily.AirMass -> true
        else -> false
    }
    return if (isLogScale) {
        // log10(y + 1) safely handles 0 values without throwing negative infinity
        val logY = log10(y.coerceAtLeast(0f) + 1f)
        val logMin = log10(params.minY.coerceAtLeast(0f) + 1f)
        val logMax = log10(params.maxY.coerceAtLeast(0f) + 1f)

        val fraction = if (logMax == logMin) 0f else (logY - logMin) / (logMax - logMin)
        params.height - params.verticalPaddingPx - (fraction * drawHeight)
    } else {
        val fraction = if (params.maxY == params.minY) 0f else (y - params.minY) / (params.maxY - params.minY)
        params.height - params.verticalPaddingPx - (fraction * drawHeight)
    }
}

fun getMinX(xValues: FloatArray, chartType: Charts): Float {
    if (chartType.javaClass.simpleName.contains("Trajectory")) return 0f
    return 0f //xValues.minOrNull() ?: 0f
}

fun getMaxX(xValues: FloatArray, chartType: Charts): Float {
    if (chartType.javaClass.simpleName.contains("Trajectory")) return 360f
    return 24f //xValues.maxOrNull() ?: 24f
}

fun getMinY(yValues: FloatArray, chartType: Charts): Float {
    val className = chartType.javaClass.simpleName
    if (className.contains("Elevation")) return -90f
    if (className.contains("Trajectory")) return -90f
    if (className.contains("ColorTemperature")) return 2000f
    return 0f
}

fun getMaxY(yValues: FloatArray, chartType: Charts): Float {
    val className = chartType.javaClass.simpleName
    if (className.contains("Elevation")) return 90f
    if (className.contains("Trajectory")) return 90f
    if (className.contains("Irradiance")) return max(300f, yValues.max())
    if (className.contains("UvIntensity")) return max(3f, yValues.max())
    if (className.contains("Illuminance")) return max(100000f, yValues.max())
    if (className.contains("Shadows")) return max(10f, 5f * yValues.filter { it > 0 }.min())
    if (className.contains("ColorTemperature")) return max(5500f, yValues.max())    // TODO: Edit
    if (className.contains("AirMass")) return max(10f, 5f * yValues.filter { it > 0 }.min()) // or 15f
    return 90f // Todo: Change
}

fun getZeroYPixel(chartType: Charts, mapY: (Float) -> Float): Float {
    if (chartType.javaClass.simpleName.contains("ColorTemperature")) return mapY(2000f)
//            Charts.Sun.Daily.AirMass -> mapY(1f)
//            Charts.Sun.Daily.AirMass -> mapY(0f)
    return mapY(0f)
}