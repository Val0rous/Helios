package com.ephemeris.helios.utils.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ephemeris.helios.ui.theme.CustomColorScheme
import com.ephemeris.helios.utils.Charts
import kotlin.math.round

fun DrawScope.drawNightVerticalTwilights(
    colors: CustomColorScheme,
    params: ChartData,
    uniqueXPoints: List<Float>,
    mapX: (Float) -> Float,
    zeroYPixel: Float,
    chartType: Charts,
    isApparentElevation: Boolean = false
) {
    val className = chartType.javaClass.simpleName
    val isElevationOrTrajectory = className.contains("Elevation") || className.contains("Trajectory")
    val shouldTransform = isApparentElevation && isElevationOrTrajectory

    val civilTwilightFill = colors.civilTwilight
    val nauticalTwilightFill = colors.nauticalTwilight
    val astroTwilightFill = colors.astronomicalTwilight
    val nightFill = colors.nightBackground

    val t0 = if (shouldTransform) transformApparentElevation(0f) else 0f
    val t6 = if (shouldTransform) transformApparentElevation(-6f) else -6f
    val t12 = if (shouldTransform) transformApparentElevation(-12f) else -12f
    val t18 = if (shouldTransform) transformApparentElevation(-18f) else -18f


    val showTwilights = when (chartType) {
        Charts.Sun.Daily.Elevation -> true
        else -> false
    }
    if (showTwilights) {
        // 2. Night twilights: Vertical stripes extending past currentX
        var currentBlockColor = Color.Transparent
        var blockStartX = uniqueXPoints.firstOrNull() ?: 0f

        for (i in 0 until uniqueXPoints.size - 1) {
            val xA = uniqueXPoints[i]
            val xB = uniqueXPoints[i + 1]
            val midX = (xA + xB) / 2f // Find the center point of this slice

            // Interpolate to find the Y altitude at the exact center of this stripe
            var midY = 0f
            for (j in 0 until params.xValues.size - 1) {
                if (midX >= params.xValues[j] && midX <= params.xValues[j + 1]) {
                    val delta = params.xValues[j + 1] - params.xValues[j]
                    midY = if (delta > 0f) {
                        params.yValues[j] + ((midX - params.xValues[j]) / delta) * (params.yValues[j + 1] - params.yValues[j])
                    } else params.yValues[j]
                    break
                }
            }

            val sliceColor = when {
                midY >= t0 -> Color.Transparent // Day area already drawn above
                midY >= t6 -> civilTwilightFill
                midY >= t12 -> nauticalTwilightFill
                midY >= t18 -> astroTwilightFill
                else -> nightFill
            }

            // If the color changes, draw the accumulated block from the previous segments
            if (sliceColor != currentBlockColor) {
                if (currentBlockColor != Color.Transparent && i > 0) {
                    // Snap to exact pixels to prevent alpha-stacking artifacts
                    val startPx = round(mapX(blockStartX))
                    val endPx = round(mapX(xA))
                    drawRect(
                        color = currentBlockColor,
                        topLeft = Offset(startPx, zeroYPixel),
                        size = Size(endPx - startPx, params.height - zeroYPixel)
                    )
                }
                // Start tracking the new color block
                currentBlockColor = sliceColor
                blockStartX = xA
            }
        }

        // Draw the final accumulated block that hits the edge of the chart
        if (currentBlockColor != Color.Transparent) {
            val startPx = round(mapX(blockStartX))
            val endPx = round(mapX(uniqueXPoints.last()))
            drawRect(
                color = currentBlockColor,
                topLeft = Offset(startPx, zeroYPixel),
                size = Size(endPx - startPx, params.height - zeroYPixel)
            )
        }
    } else {
        // TODO: drawRect()
    }
}