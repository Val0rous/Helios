package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.charts.getMaxX
import com.ephemeris.helios.utils.charts.getMaxY
import com.ephemeris.helios.utils.charts.getMinX
import com.ephemeris.helios.utils.charts.getMinY
import com.ephemeris.helios.utils.charts.mapX
import com.ephemeris.helios.utils.charts.mapY
import kotlin.math.abs

@Composable
fun DailyAzimuthChart(
    xValues: FloatArray,
    yValues: FloatArray,
    currentHour: Float,
    chartType: Charts,
    currentAzimuth: Float,
    currentAltitude: Float,
    modifier: Modifier = Modifier
) {
    val drawChartIcon = rememberChartIconDrawer(chartType)

    val colors = LocalCustomColors.current
    val sunYellow = colors.sun
    val dayFill = colors.dayBackground
    val civilTwilightFill = colors.civilTwilight
    val nauticalTwilightFill = colors.nauticalTwilight
    val astroTwilightFill = colors.astronomicalTwilight
    val nightFill = colors.nightBackground
    val dayBackground = MaterialTheme.colorScheme.surface
    val nightBackground = MaterialTheme.colorScheme.surfaceVariant
    val elapsedDayFill = colors.elapsedDay
    val elapsedNightFill = colors.elapsedNight
    val backgroundColor = MaterialTheme.colorScheme.surface
    val materialTheme = MaterialTheme.colorScheme
    val localCustomColors = LocalCustomColors.current
    val context = LocalContext.current
    // Text Measurer and styling for the legends
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        fontSize = (9.5).sp,
        fontFamily = FontFamily.Monospace
    )

    Canvas(modifier = modifier) {
        if (xValues.isEmpty() || yValues.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        val minX = getMinX(xValues, chartType)
        val maxX = getMaxX(xValues, chartType)
        val minY = getMinY(yValues, chartType)
        val maxY = getMaxY(yValues, chartType)

        val verticalPaddingPx = 16.dp.toPx()
        val drawHeight = (height - (2 * verticalPaddingPx)).coerceAtLeast(1f)

        // Dynamic Trajectory Shifting
        // Find where the sun reaches its highest point
        val peakIndex = yValues.indices.maxByOrNull { yValues[it] } ?: 0
        val peakAzimuth = xValues[peakIndex]
        // If it culminates North (near 0 or 360), we shift the chart by 180 degrees
        val shiftTrajectory = (peakAzimuth !in 90f..270f)

        // --- PURE SHIFT MAPPING ---
        // Keeps North(0) at the center, naturally preserving Right-To-Left motion!
        // Create a mapped array purely for continuous drawing
        val drawXValues = FloatArray(xValues.size) { i ->
            if (shiftTrajectory) (xValues[i] + 180f) % 360f else xValues[i]
        }

        // Helper functions to map mathematical coordinates to Canvas pixels
        fun mapX(x: Float) = mapX(x, minX, maxX, width)
        // Canvas Y=0 is at the top, so we invert the Y mapping
        fun mapY(y: Float) = mapY(y, minY, maxY, height, drawHeight, verticalPaddingPx)

        val zeroYPixel = mapY(0f)

        // Ensure the sun icon aligns with the physical inputs
        val drawCurrentX = if (shiftTrajectory) (currentAzimuth + 180f) % 360f else currentAzimuth
        val currentXPx = mapX(drawCurrentX)
        val currentYPx = mapY(currentAltitude)

        // --- FLAWLESS CHRONOLOGICAL INDEX ---
        // Mathematically maps current local time directly to the array index
        val exactIndex = (currentHour / 24f) * (xValues.size - 1)
        val bestIndex = exactIndex.toInt().coerceIn(0, xValues.size - 1)

        // Visual Nadir Tracing
        // Find Solar Midnight to avoid showing early AM disconnected leftovers
        val nadirIndex = yValues.indices.minByOrNull { yValues[it] } ?: 0

        // --- DIRECTION-AWARE PATH BUILDER ---
        // Generates flawless polygons regardless of left-to-right or right-to-left tracing
        fun buildDynamicPath(startIndex: Int, endIndex: Int, isFill: Boolean): Path {
            return Path().apply {
                if (startIndex !in 0..endIndex || drawXValues.isEmpty()) return@apply

                if (isFill) {
                    moveTo(mapX(drawXValues[startIndex]), zeroYPixel)
                    lineTo(mapX(drawXValues[startIndex]), mapY(yValues[startIndex]))
                } else {
                    moveTo(mapX(drawXValues[startIndex]), mapY(yValues[startIndex]))
                }

                for (i in (startIndex + 1)..endIndex) {
                    val xA = drawXValues[i-1]
                    val xB = drawXValues[i]

                    if (abs(xB - xA) > 200f) { // wrapThreshold seamlessly catches 360 wraps in BOTH directions
                        // Calculate exact crossing direction
                        val wrapForward = xA > 180f && xB < 180f
                        val edgeX = if (wrapForward) 360f else 0f
                        val newStartX = if (wrapForward) 0f else 360f

                        val deltaX = if (wrapForward) (xB + 360f) - xA else xB - (xA + 360f)
                        val fraction = if (deltaX != 0f) (edgeX - xA) / deltaX else 0f
                        val edgeY = yValues[i-1] + fraction * (yValues[i] - yValues[i-1])

                        lineTo(mapX(edgeX), mapY(edgeY))
                        if (isFill) {
                            lineTo(mapX(edgeX), zeroYPixel)
                            close()
                            moveTo(mapX(newStartX), zeroYPixel)
                        } else {
                            moveTo(mapX(newStartX), mapY(edgeY))
                        }
                        lineTo(mapX(newStartX), mapY(edgeY))
                    } else {
                        lineTo(mapX(xB), mapY(yValues[i]))
                    }
                }
                if (isFill) {
                    lineTo(mapX(drawXValues[endIndex]), zeroYPixel)
                    close()
                }
            }
        }

        // 1. Build the smooth curve path
        val curvePath = buildDynamicPath(0, drawXValues.size - 1, false)

        // 2. Build the fill path that closes down to the X-axis
        val fillPath = buildDynamicPath(0, drawXValues.size - 1, true)

//        // --- CHRONOLOGICAL ELAPSED FILL ---
//        // Stops exactly at 'bestIndex', naturally tracking from midnight up to current time
//        val elapsedStartIndex = if (bestIndex >= nadirIndex) nadirIndex else 0
//        val elapsedFillPath = buildDynamicPath(elapsedStartIndex, bestIndex, true)

        // --- CHRONOLOGICAL ELAPSED LINE ---
        val elapsedLinePath = buildDynamicPath(0, bestIndex, false)
        val elapsedLineColor = when (chartType) {
            is Charts.Sun -> localCustomColors.sunPath
            is Charts.Moon -> localCustomColors.moonPath
            else -> Color.Green // TODO
        }

        // Day Background
        drawRect(
            color = dayBackground,
            topLeft = Offset(0f, 0f),
            size = Size(width, zeroYPixel)
        )

        // Night Background
        drawRect(
            color = nightBackground,
            topLeft = Offset(0f, zeroYPixel),
            size = Size(width, height - zeroYPixel)
        )

        clipRect(bottom = zeroYPixel) {
            drawPath(path = fillPath, color = dayBackground.copy(alpha = 1.0f))
        }

        clipRect(top = zeroYPixel) {
            drawPath(path = fillPath, color = nightBackground.copy(alpha = 1.0f))
        }

//        // 3. Draw the colored areas (clipped strictly up to currentHour)
//        clipRect(right = currentXPx) {
//
//            // Day (Above 0deg)
//            clipRect(bottom = zeroYPixel) {
//                drawPath(path = fillPath, color = dayOrangeFill)
//            }
//
//            // Civil Twilight (0deg to -6deg)
//            clipRect(top = zeroYPixel, bottom = civilYPixel) {
//                drawPath(path = fillPath, color = civilTwilightFill)
//            }
//
//            // Nautical Twilight (-6deg to -12deg)
//            clipRect(top = civilYPixel, bottom = nauticalYPixel) {
//                drawPath(path = fillPath, color = nauticalTwilightFill)
//            }
//
//            // Astronomical Twilight (-12deg to -18deg)
//            clipRect(top = nauticalYPixel, bottom = astroYPixel) {
//                drawPath(path = fillPath, color = astroTwilightFill)
//            }
//
//            // Night (Below -18deg)
//            clipRect(top = astroYPixel, bottom = height) {
//                drawPath(path = fillPath, color = deepNightFill)
//            }
//        }

        // 3. Draw the Twilight Horizontal Bands
        if (chartType is Charts.Sun) {
            clipPath(fillPath) {

                // Day area: Removed 'right = currentXPx' so sunset is always visible by default
                clipRect(bottom = zeroYPixel) {
                    drawRect(
                        color = dayFill,
                        topLeft = Offset(0f, 0f),
                        size = Size(width, zeroYPixel)
                    )
                }

                clipRect(top = zeroYPixel, bottom = mapY(-6f)) {
                    drawRect(
                        color = civilTwilightFill,
                        topLeft = Offset(0f, zeroYPixel),
                        size = Size(width, mapY(-6f) - zeroYPixel)
                    )
                }
                clipRect(top = mapY(-6f), bottom = mapY(-12f)) {
                    drawRect(
                        color = nauticalTwilightFill,
                        topLeft = Offset(0f, mapY(-6f)),
                        size = Size(width, mapY(-12f) - mapY(-6f))
                    )
                }
                clipRect(top = mapY(-12f), bottom = mapY(-18f)) {
                    drawRect(
                        color = astroTwilightFill,
                        topLeft = Offset(0f, mapY(-12f)),
                        size = Size(width, mapY(-18f) - mapY(-12f))
                    )
                }
                clipRect(top = mapY(-18f)) {
                    drawRect(
                        color = nightFill,
                        topLeft = Offset(0f, mapY(-18f)),
                        size = Size(width, height - mapY(-18f))
                    )
                }
            }
        }

        // 4. Draw the full unclipped curve line for all values
        drawPath(
            path = curvePath,
            color = materialTheme.onSurfaceVariant,
            style = Stroke(width = (1.5).dp.toPx())
        )

        // --- Draw the elapsed path line on top ---
        drawPath(
            path = elapsedLinePath,
            color = elapsedLineColor,
            style = Stroke(width = 2.dp.toPx()) // I recommend 2.dp so it pops slightly over the base line
        )

        // 5. Draw a subtle X-Axis line to visually separate the zones (Horizon Line)
        drawLine(
            color = materialTheme.outline,
            start = Offset(0f, zeroYPixel),
            end = Offset(width, zeroYPixel),
            strokeWidth = (1.5).dp.toPx()
        )

        // Define the dotted path effect and grid color ---
        // floatArrayOf(on, off) in pixels. Using dp ensures consistent dot spacing on all screens.
        val verticalGridDashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 0.dp.toPx()), 0f)
        val horizontalGridDashEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f)
        val verticalGridlineColor = materialTheme.outlineVariant.copy(alpha = 0.15f) // Light and subtle
        val horizontalGridlineColor = materialTheme.outline.copy(alpha = 0.3f) // Light and subtle

        // 5a. Draw Vertical Legend (Y-axis Altitudes)
        val yLabels = (-90 until 91 step 15).map { it.toFloat()}
        yLabels.forEach { yVal ->
            val yPx = mapY(yVal)

            // Draw horizontal dotted grid line (Skip 0f to avoid drawing over the solid horizon line)
            if (yVal != 0f) {
                drawLine(
                    color = horizontalGridlineColor,
                    start = Offset(0f, yPx),
                    end = Offset(width, yPx),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = horizontalGridDashEffect
                )
            }

            val text = "${yVal.toInt()}°"
            val textLayout = textMeasurer.measure(text, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = labelStyle,
                topLeft = Offset(
                    x = 4.dp.toPx(), // Slight padding from the left edge
                    y = yPx - (textLayout.size.height / 2f) // Perfectly centered vertically on the line
                )
            )
        }

        // 5b. Draw Horizontal Legend (X-axis Hours)
        val xLabels = (30..330 step 30).toList()
        xLabels.forEach {
            val xPx = mapX(it.toFloat())

            // Draw vertical dotted grid line
            drawLine(
                color = verticalGridlineColor,
                start = Offset(xPx, 0f),
                end = Offset(xPx, height), // Spans the entire canvas height
                strokeWidth = 1.dp.toPx(),
                pathEffect = verticalGridDashEffect
            )

            // Mathematically reverse the shift for the label
            val realAzimuth = if (shiftTrajectory) (it.toFloat() + 180f) % 360 else it.toFloat()
            val formatted = realAzimuth.toInt()
            val text = "${if (formatted == 360 || formatted == 0) 0 else formatted}°"

            val textLayout = textMeasurer.measure(text, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = labelStyle,
                topLeft = Offset(
                    x = xPx - (textLayout.size.width / 2.5f), // Centered horizontally on the hour mark
                    y = height - textLayout.size.height - 2.dp.toPx() // Pinned near the bottom edge of the canvas
                )
            )
        }

        // 6. Draw vertical drop line from Sun to Horizon (X-axis)
        drawLine(
            color = localCustomColors.dropLine,
            start = Offset(currentXPx, currentYPx),
            end = Offset(currentXPx, zeroYPixel),
            strokeWidth = 1.dp.toPx() // Using 1.dp keeps it crisp but visible across screen densities
        )

        // 7. Paint the Sun Icon if above horizon
        if (currentAltitude >= 0f) {
            val iconSize = 24.dp.toPx()
//            val padding = 4.dp.toPx()
//            val radius = (iconSize / 2) + padding

//            drawCircle(
//                color = backgroundColor,
//                radius = radius,
//                center = Offset(currentXPx, currentYPx)
//            )

            clipRect(bottom = (zeroYPixel - 2f)) {
                translate(
                    left = currentXPx - iconSize / 2,
                    top = currentYPx - iconSize / 2
                ) {
                    drawChartIcon(iconSize, false)
                }
            }
        } else {
            val iconSize = 12.dp.toPx()
            translate(
                left = currentXPx - iconSize / 2,
                top = currentYPx - iconSize / 2
            ) {
                drawChartIcon(iconSize, true)
            }
        }
    }
}