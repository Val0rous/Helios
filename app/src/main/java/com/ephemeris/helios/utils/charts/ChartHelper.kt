package com.ephemeris.helios.utils.charts

import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.calc.MAX_SHADOW_RATIO
import com.ephemeris.helios.utils.location.Coordinates
import java.time.ZonedDateTime
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin


const val IS_APPARENT_ELEVATION_DEFAULT: Boolean = true

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
        Charts.Sun.Daily.Illuminance, Charts.Sun.Daily.Shadows, Charts.Sun.Daily.AirMass,
        Charts.Moon.Daily.Illuminance, Charts.Moon.Daily.Shadows, Charts.Moon.Daily.AirMass -> true
        else -> false
    }
    return if (isLogScale) {
        // Scale low-range log charts so sub-1 and sub-2 values get clear vertical resolution
        // instead of flattening near 0 in log10(y + 1):
        //  - Moon Illuminance (0..0.3 lx / 300 mlx): ×1000 (millilux)
        //  - Shadows (0..50+): ×10 (gives ~39% height to 0..1 and ~49% to 0..2)
        //  - Air Mass (0..50): ×4 (gives ~30% height to 0..1 and ~41% to 0..2)
        val logScale = when (chartType) {
            Charts.Moon.Daily.Illuminance -> 1000f
            Charts.Sun.Daily.Shadows, Charts.Moon.Daily.Shadows -> 10f
            Charts.Sun.Daily.AirMass, Charts.Moon.Daily.AirMass -> 4f
            else -> 1f
        }
        val logY = log10(y.coerceAtLeast(0f) * logScale + 1f)
        val logMin = log10(params.minY.coerceAtLeast(0f) * logScale + 1f)
        val logMax = log10(params.maxY.coerceAtLeast(0f) * logScale + 1f)

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
    if (className.contains("ColorTemperature")) return 1800f
    return 0f
}

private const val LINEAR_HEADROOM_FACTOR = 1.10f

/**
 * Nice linear Y-axis step for Irradiance (`W/m²`), tuned for [LINEAR_HEADROOM_FACTOR]
 * continuous scaling (`1.10 ×` the nominal `10, 20, 50, 100, 250, 500, 1000` thresholds).
 */
internal fun getIrradianceStep(maxValue: Float): Int = when {
    maxValue <= 11f -> 1
    maxValue <= 22f -> 2
    maxValue <= 55f -> 5
    maxValue <= 110f -> 10
    maxValue <= 275f -> 25
    maxValue <= 550f -> 50
    maxValue <= 1100f -> 100
    else -> 200
}

/**
 * Nice linear Y-axis step in tenths of a UV Index unit (`5` = `0.5`, `10` = `1.0`), tuned for
 * [LINEAR_HEADROOM_FACTOR] continuous scaling (`1.10 ×` the nominal `1, 2, 5, 12` thresholds).
 */
internal fun getUvIntensityStepTenths(maxValue: Float): Int = when {
    maxValue <= 1.1f -> 1    // 0.1
    maxValue <= 2.2f -> 2    // 0.2
    maxValue <= 5.5f -> 5    // 0.5 (keeps 0.5 steps when data max is 5)
    maxValue <= 13.2f -> 10  // 1 (keeps step 1 across data max 0–12)
    else -> 20               // 2
}

fun getMaxY(yValues: FloatArray, chartType: Charts): Float {
    val className = chartType.javaClass.simpleName
    // Strip out the NaNs so math operations don't crash
    val validY = yValues.filter { !it.isNaN() }
    val maxY = validY.maxOrNull() ?: 0f

    if (className.contains("Elevation")) return 90f
    if (className.contains("Trajectory")) return 90f
    if (className.contains("Irradiance")) return max(10f, maxY * LINEAR_HEADROOM_FACTOR)
    if (className.contains("UvIntensity")) return max(1f, maxY * LINEAR_HEADROOM_FACTOR)
    if (className.contains("Illuminance")) {
        val floorMax = if (chartType == Charts.Moon.Daily.Illuminance) 0.3f else 100000f
        return max(floorMax, maxY)
    }
    if (className.contains("Shadows")) {
        val maxShadow = MAX_SHADOW_RATIO.toFloat()
        val minPositive = validY.asSequence().filter { it > 0f }.minOrNull()
            ?: return maxShadow // Polar night (never rises): display full maximum scale
        return max(50f, 5f * minPositive).coerceAtMost(maxShadow)
    }
    if (className.contains("ColorTemperature")) {
        val floorMax = if (chartType == Charts.Moon.Daily.ColorTemperature) 8000f else 5500f
        val minY = 1800f
        return max(floorMax, minY + (maxY - minY) * LINEAR_HEADROOM_FACTOR)
    }
    if (className.contains("AirMass")) return max(50f, maxY)
    return 90f // Todo: Change
}

/**
/ Transforms true altitude (in degrees) to apparent visual elevation using y = 90 * sin(x).
 */
fun transformApparentElevation(y: Float): Float {
    if (y.isNaN()) return Float.NaN
    val clamped = y.coerceIn(-90f, 90f)
    return 90f * sin(Math.toRadians(clamped.toDouble())).toFloat()
}

fun transformApparentElevation(yValues: FloatArray): FloatArray {
    return FloatArray(yValues.size) { i -> transformApparentElevation(yValues[i]) }
}

fun getZeroYPixel(
    chartType: Charts,
    mapY: (Float) -> Float,
    coordinates: Coordinates? = null,
    isApparentElevation: Boolean = false
): Float {
    val className = chartType.javaClass.simpleName
    if (className.contains("ColorTemperature")) return mapY(1800f)
    if (className.contains("Elevation") || className.contains("Trajectory")) {
        val dip = coordinates?.horizonDipDeg?.toFloat() ?: 0f
        val targetY = if (isApparentElevation) transformApparentElevation(-dip) else -dip
        return mapY(targetY)
    }
//            Charts.Sun.Daily.AirMass -> mapY(1f)
//            Charts.Sun.Daily.AirMass -> mapY(0f)
    return mapY(0f)
}

fun ZonedDateTime.toDecimalHour(): Float =
    hour.toFloat() + minute / 60f + second / 3600f

/**
 * O(1) linear interpolation on a uniformly sampled 24-hour array (00:00..24:00).
 * Keeps chart icons glued directly to the rendered curve without async lag or linear scans.
 */
fun interpolateYAtHour(yValues: FloatArray, hour: Float): Float {
    if (yValues.isEmpty()) return Float.NaN
    val maxIdx = yValues.lastIndex
    val exactIdx = ((hour / 24f) * maxIdx).coerceIn(0f, maxIdx.toFloat())
    val i0 = exactIdx.toInt()
    val i1 = (i0 + 1).coerceAtMost(maxIdx)
    val y0 = yValues[i0]
    val y1 = yValues[i1]
    val fraction = exactIdx - i0
    return when {
        !y0.isNaN() && !y1.isNaN() -> y0 + fraction * (y1 - y0)
        !y0.isNaN() -> y0
        !y1.isNaN() -> y1
        else -> Float.NaN
    }
}