package com.ephemeris.helios.utils.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ephemeris.helios.ui.theme.CustomColorScheme
import com.ephemeris.helios.utils.Charts
import kotlin.math.round

/**
 * Precomputed pixel-snapped horizontal band for twilight or UV slices.
 * Computed once per day/size change in O(N) inside Modifier.drawWithCache so per-frame
 * drawing is just a handful of drawRect() calls.
 */
data class HorizontalColorBand(
    val startPx: Float,
    val endPx: Float,
    val color: Color,
)

/**
 * Single-pass O(N) horizontal band builder.
 *
 * Splits only the rare segments that cross a value in [sortedThresholdsAsc] (exact linear
 * interpolation) and merges contiguous sub-segments sharing the same color into pixel-snapped
 * [HorizontalColorBand]s.
 */
internal fun buildHorizontalColorBands(
    xValues: FloatArray,
    yValues: FloatArray,
    sortedThresholdsAsc: FloatArray,
    mapX: (Float) -> Float,
    colorForY: (Float) -> Color,
): List<HorizontalColorBand> {
    val n = minOf(xValues.size, yValues.size)
    if (n < 2) return emptyList()

    val bands = ArrayList<HorizontalColorBand>(16)
    var currentColor = Color.Transparent
    var blockStartX = xValues[0]

    fun emitSubSegment(xStart: Float, midY: Float) {
        val sliceColor = if (midY.isNaN()) Color.Transparent else colorForY(midY)
        if (sliceColor != currentColor) {
            if (currentColor != Color.Transparent) {
                val startPx = round(mapX(blockStartX))
                val endPx = round(mapX(xStart))
                if (endPx > startPx) {
                    bands.add(HorizontalColorBand(startPx, endPx, currentColor))
                }
            }
            currentColor = sliceColor
            blockStartX = xStart
        }
    }

    for (i in 0 until n - 1) {
        val x1 = xValues[i]
        val x2 = xValues[i + 1]
        val y1 = yValues[i]
        val y2 = yValues[i + 1]

        if (y1.isNaN() || y2.isNaN()) {
            emitSubSegment(x1, Float.NaN)
            continue
        }

        val c1 = colorForY(y1)
        val c2 = colorForY(y2)
        if (c1 == c2) {
            // Fast path (~99.5% of segments): no threshold crossed between adjacent samples
            emitSubSegment(x1, (y1 + y2) * 0.5f)
            continue
        }

        // Slow path (~6-10 segments/day): split exactly at every crossed threshold in x order
        val dy = y2 - y1
        val dx = x2 - x1
        var prevX = x1
        var prevY = y1

        if (dy > 0f) {
            for (tIdx in sortedThresholdsAsc.indices) {
                val th = sortedThresholdsAsc[tIdx]
                if (th > y1 && th < y2) {
                    val crossX = x1 + ((th - y1) / dy) * dx
                    emitSubSegment(prevX, (prevY + th) * 0.5f)
                    prevX = crossX
                    prevY = th
                }
            }
        } else if (dy < 0f) {
            for (tIdx in sortedThresholdsAsc.lastIndex downTo 0) {
                val th = sortedThresholdsAsc[tIdx]
                if (th < y1 && th > y2) {
                    val crossX = x1 + ((th - y1) / dy) * dx
                    emitSubSegment(prevX, (prevY + th) * 0.5f)
                    prevX = crossX
                    prevY = th
                }
            }
        }

        emitSubSegment(prevX, (prevY + y2) * 0.5f)
    }

    if (currentColor != Color.Transparent) {
        val startPx = round(mapX(blockStartX))
        val endPx = round(mapX(xValues[n - 1]))
        if (endPx > startPx) {
            bands.add(HorizontalColorBand(startPx, endPx, currentColor))
        }
    }

    return bands
}

fun buildNightVerticalTwilightBands(
    colors: CustomColorScheme,
    params: ChartData,
    mapX: (Float) -> Float,
    chartType: Charts,
    isApparentElevation: Boolean = false,
): List<HorizontalColorBand> {
    if (chartType != Charts.Sun.Daily.Elevation) return emptyList()

    val className = chartType.javaClass.simpleName
    val isElevationOrTrajectory = className.contains("Elevation") || className.contains("Trajectory")
    val shouldTransform = isApparentElevation && isElevationOrTrajectory

    val t0 = if (shouldTransform) transformApparentElevation(0f) else 0f
    val t6 = if (shouldTransform) transformApparentElevation(-6f) else -6f
    val t12 = if (shouldTransform) transformApparentElevation(-12f) else -12f
    val t18 = if (shouldTransform) transformApparentElevation(-18f) else -18f

    val fills = colors.fills
    val civilTwilightFill = fills.civilTwilight
    val nauticalTwilightFill = fills.nauticalTwilight
    val astroTwilightFill = fills.astronomicalTwilight
    val nightFill = fills.nightBackground

    return buildHorizontalColorBands(
        xValues = params.xValues,
        yValues = params.yValues,
        sortedThresholdsAsc = floatArrayOf(t18, t12, t6, t0),
        mapX = mapX,
    ) { y ->
        when {
            y >= t0 -> Color.Transparent // Day area already drawn above
            y >= t6 -> civilTwilightFill
            y >= t12 -> nauticalTwilightFill
            y >= t18 -> astroTwilightFill
            else -> nightFill
        }
    }
}

fun DrawScope.drawNightVerticalTwilights(
    bands: List<HorizontalColorBand>,
    zeroYPixel: Float,
    canvasHeight: Float,
) {
    val bandHeight = canvasHeight - zeroYPixel
    for (i in bands.indices) {
        val band = bands[i]
        drawRect(
            color = band.color,
            topLeft = Offset(band.startPx, zeroYPixel),
            size = Size(band.endPx - band.startPx, bandHeight),
        )
    }
}
