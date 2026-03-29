package com.ephemeris.helios.ui.composables.charts

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.formatHour
import com.ephemeris.helios.utils.formatNumber
import com.ephemeris.helios.utils.printRounded
import kotlin.collections.copy
import kotlin.div
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

@Composable
fun DailyAzimuthChart(
    xValues: FloatArray,
    yValues: FloatArray,
    chartType: Charts,
    currentAzimuth: Float,
    currentAltitude: Float,
    modifier: Modifier = Modifier
) {
    val sunPainter = painterResource(id = when (chartType) {
        is Charts.Sun -> R.drawable.ic_brightness_empty_filled
        is Charts.Moon -> R.drawable.ic_moon_stars_filled // Todo: change icon
        else -> R.drawable.ic_circle_filled
    })
    val indicatorPainter = painterResource(id = R.drawable.ic_circle_filled)

    val colors = LocalCustomColors.current
    val sunYellow = colors.sun
    val dayFill = colors.day
    val civilTwilightFill = colors.civilTwilight
    val nauticalTwilightFill = colors.nauticalTwilight
    val astroTwilightFill = colors.astronomicalTwilight
    val nightFill = colors.night
    val dayBackground = MaterialTheme.colorScheme.surface
    val nightBackground = MaterialTheme.colorScheme.surfaceVariant
    val elapsedDayFill = colors.elapsedDay
    val elapsedNightFill = colors.elapsedNight
    val backgroundColor = MaterialTheme.colorScheme.surface
    val materialTheme = MaterialTheme.colorScheme
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

        val minX = 0f
        val maxX = 360f
        val minY = -90f
        val maxY = 90f

        val verticalPaddingPx = 16.dp.toPx()
        val drawHeight = (height - (2 * verticalPaddingPx)).coerceAtLeast(1f)

        // Dynamic Trajectory Shifting
        // Find where the sun reaches its highest point
        val peakIndex = yValues.indices.maxByOrNull { yValues[it] } ?: 0
        val peakAzimuth = xValues[peakIndex]
        // If it culminates North (near 0 or 360), we shift the chart by 180 degrees
        val shiftTrajectory = chartType == Charts.Sun.Daily.Trajectory && (peakAzimuth !in 90f..270f)

        // Create a mapped array purely for continuous drawing
        val drawXValues = FloatArray(xValues.size) { i ->
            // N(0) goes to 180, E(90) stays 90, W(270) stays 270. Time moves left-to-right!
            if (shiftTrajectory) (540f - xValues[i]) % 360f else xValues[i]
        }

        // --- THE SQUIRCLE KILLER ---
        // Forces raw azimuth data to be monotonically continuous, intercepting "bounces"
        val fixedXValues = FloatArray(xValues.size)
        if (xValues.isNotEmpty()) {
            var currentUnwrapped = xValues[0]
            fixedXValues[0] = currentUnwrapped

            for (i in 1 until xValues.size) {
                var raw = xValues[i]
                var delta = raw - currentUnwrapped

                // Normalize shortest path
                while (delta < -180f) delta += 360f
                while (delta > 180f) delta -= 360f

                // If the path goes against physical rotation, it's a backend U-Turn bounce!
                if (chartType == Charts.Sun.Daily.Trajectory && abs(delta) < 90f) {
                    if (shiftTrajectory && delta > 0f) {
                        // Supposed to decrease, but increased!
                        raw = (360f - raw) % 360f
                    } else if (!shiftTrajectory && delta < 0f) {
                        // Supposed to increase, but decreased!
                        raw = (360f - raw) % 360f
                    }
                    // Recalculate normalized delta with fixed raw value
                    delta = raw - currentUnwrapped
                    while (delta < -180f) delta += 360f
                    while (delta > 180f) delta -= 360f
                }

                currentUnwrapped = (currentUnwrapped + delta + 360f) % 360f
                fixedXValues[i] = currentUnwrapped
            }
        }

        // Helper functions to map mathematical coordinates to Canvas pixels
        fun mapX(x: Float) = ((x - minX) / (maxX - minX)) * width
        // Canvas Y=0 is at the top, so we invert the Y mapping
        fun mapY(y: Float): Float {
            val fraction = if (maxY == minY) 0f else (y - minY) / (maxY - minY)
            return (height - verticalPaddingPx - (fraction * drawHeight))
        }

        val zeroYPixel = when (chartType) {
            Charts.Sun.Daily.ColorTemperature -> mapY(2000f)
            Charts.Sun.Daily.AirMass -> mapY(1f)
            else -> mapY(0f)
        }
        // Ensure the sun icon aligns with the shifted mapping
        val drawCurrentX = if (shiftTrajectory) (540f - currentAzimuth) % 360f else currentAzimuth
        val currentXPx = mapX(drawCurrentX)

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

        val wrapThreshold = 200f // Instantly cuts off erratic backend interpolation

        // 1. Build the smooth curve path
        val curvePath = Path().apply {
            if(xValues.isNotEmpty()) {
                moveTo(mapX(drawXValues[0]), mapY(yValues[0]))
                for (i in 1 until drawXValues.size) {
                    // If the azimuth wraps around 360, pick up the pen and move it!
                    if (chartType == Charts.Sun.Daily.Trajectory && abs(drawXValues[i] - drawXValues[i-1]) > wrapThreshold) {
                        // Mathematically predict the altitude exactly at the 360° / 0° edge
                        val xA = drawXValues[i-1]
                        val xB = drawXValues[i]
                        val deltaX = (xB + 360f) - xA
                        val fraction = if (deltaX != 0f) (360f - xA) / deltaX else 0f
                        val edgeY = yValues[i-1] + fraction * (yValues[i] - yValues[i-1])
//                        moveTo(mapX(drawXValues[i]), mapY(yValues[i]))
                        // Draw perfectly to the right edge, then pick up and move to the left edge
                        lineTo(mapX(360f), mapY(edgeY))
                        moveTo(mapX(0f), mapY(edgeY))
                    }
                    lineTo(mapX(drawXValues[i]), mapY(yValues[i]))
                }
            }
        }

        // 2. Build the fill path that closes down to the X-axis
        val fillPath = Path().apply {
            if (drawXValues.isNotEmpty()) {
                moveTo(mapX(drawXValues[0]), zeroYPixel)
                lineTo(mapX(drawXValues[0]), mapY(yValues[0]))
                for (i in 1 until drawXValues.size) {
                    if (chartType == Charts.Sun.Daily.Trajectory && abs(drawXValues[i] - drawXValues[i-1]) > wrapThreshold) {
                        // Calculate edge altitude again
                        val xA = drawXValues[i-1]
                        val xB = drawXValues[i]
                        val deltaX = (xB + 360f) - xA
                        val fraction = if (deltaX != 0f) (360f - xA) / deltaX else 0f
                        val edgeY = yValues[i-1] + fraction * (yValues[i] - yValues[i-1])

                        // Drop down and seal the previous chunk exactly at the right edge
                        lineTo(mapX(360f), mapY(edgeY))
                        lineTo(mapX(360f), zeroYPixel)
                        close()

                        // Move to the new edge and start the next chunk
                        moveTo(mapX(0f), zeroYPixel)
                        lineTo(mapX(0f), mapY(edgeY))
                    } else {
                        lineTo(mapX(drawXValues[i]), mapY(yValues[i]))
                    }
                }
                lineTo(mapX(drawXValues.last()), zeroYPixel)
                close()
            }
        }

        if (chartType == Charts.Sun.Daily.Trajectory) {
            for (i in 0 until xValues.size) {
                Log.i("(x,y)", "${xValues[i]} ${yValues[i]}")
            }
        }

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

        // 3a. Calculate exact X intersections for vertical stripes
        val thresholds = floatArrayOf(0f, -6f, -12f, -18f)
        val sortedXPoints = mutableListOf<Float>()
        for (x in fixedXValues) {
            // Add all original X points
            sortedXPoints.add(x)
        }
        // Add all mathematical crossing points
        for (i in 0 until fixedXValues.size - 1) {
            val x1 = fixedXValues[i]
            val x2 = fixedXValues[i+1]
            val y1 = yValues[i]
            val y2 = yValues[i+1]

            for (th in thresholds) {
                if ((y1 < th && y2 > th) || (y1 > th && y2 < th)) {
                    // Linear interpolation to find the exact X coordinate of the threshold
                    val fraction = (th - y1) / (y2 - y1)
                    val x = x1 + fraction * (x2 - x1)
                    sortedXPoints.add(x)
                }
            }
        }
        sortedXPoints.sort()
        val uniqueXPoints = sortedXPoints.distinct()

        // --- NEW: Draw the clipped vertical stripes ---
        clipPath(fillPath) {

            // 1. Day area: Removed 'right = currentXPx' so sunset is always visible by default
            clipRect(bottom = zeroYPixel) {
//                if (zeroYPixel > 0f) {
                drawRect(
                    color = dayFill,
                    topLeft = Offset(0f, 0f),
                    size = Size(width, zeroYPixel)
                )
//                }
            }

            if (chartType == Charts.Sun.Daily.Trajectory) {
                // For Trajectory, Y is altitude! Twilights are just simple horizontal bands.
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
            } else {
                // 2. Night twilights: Vertical stripes extending past currentX
                var currentBlockColor = Color.Transparent
                var blockStartX = uniqueXPoints.firstOrNull() ?: 0f

                for (i in 0 until uniqueXPoints.size - 1) {
                    val xA = uniqueXPoints[i]
                    val xB = uniqueXPoints[i + 1]
                    val midX = (xA + xB) / 2f // Find the center point of this slice

                    // Interpolate to find the Y altitude at the exact center of this stripe
                    var midY = 0f
                    for (j in 0 until fixedXValues.size - 1) {
                        if (midX >= fixedXValues[j] && midX <= fixedXValues[j + 1]) {
                            val delta = fixedXValues[j + 1] - fixedXValues[j]
                            midY = if (delta > 0f) {
                                yValues[j] + ((midX - fixedXValues[j]) / delta) * (yValues[j + 1] - yValues[j])
                            } else yValues[j]
                            break
                        }
                    }

                    val sliceColor = when {
                        midY >= 0f -> Color.Transparent // Day area already drawn above
                        midY >= -6f -> civilTwilightFill
                        midY >= -12f -> nauticalTwilightFill
                        midY >= -18f -> astroTwilightFill
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
                                size = Size(endPx - startPx, height - zeroYPixel)
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
                        size = Size(endPx - startPx, height - zeroYPixel)
                    )
                }
            }

            // 3. Draw the elapsed time overlay (clipped strictly up to currentHour)
            clipRect(right = currentXPx) {
                // Elapsed Day (Above 0deg)
                clipRect(bottom = zeroYPixel) {
                    drawPath(path = fillPath, color = elapsedDayFill)
                }

                // Elapsed Night (Below 0deg)
                clipRect(top = zeroYPixel) {
                    drawPath(path = fillPath, color = elapsedNightFill)
                }
            }
        }

        // 4. Draw the full unclipped curve line for all values
        drawPath(
            path = curvePath,
            color = materialTheme.onSurfaceVariant,
            style = Stroke(width = (1.5).dp.toPx())
        )

        // 5. Draw a subtle X-Axis line to visually separate the zones
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
        val yLabels = when (chartType) {
            Charts.Sun.Daily.Elevation, Charts.Sun.Daily.Trajectory -> (-90 until 91 step 15).map { it.toFloat()}
            Charts.Sun.Daily.Irradiance -> (0 until (maxY.toInt() + 1) step 100).map { it.toFloat() }
            Charts.Sun.Daily.UvIntensity -> (0 until (maxY.toInt() + 1) step floor(maxY / 10f).toInt()).map { it.toFloat() }
            Charts.Sun.Daily.Illuminance -> listOf(0f) + (0..5).flatMap {
                val base = 10.0.pow(it.toDouble()).toFloat()
                listOf(base, base * 3f)
            }.filter { it <= maxY }
            Charts.Sun.Daily.Shadows -> listOf(0f, 0.25f, 0.5f, 1f, 1.5f, 2f, 3f, 4f, 5f, 6f, 7f, 10f)
            Charts.Sun.Daily.ColorTemperature -> (2000 until 5501 step 500).map { it.toFloat() }
            Charts.Sun.Daily.AirMass -> listOf(1f, 1.5f, 2f, 3f, 4f, 5f, 6f, 7f, 10f)
            else -> emptyList()
        }
        yLabels.forEach { yVal ->
            val yPx = mapY(yVal)
//            if (yVal == 0f) return@forEach // Skip the zero

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

            val text = when (chartType) {
                Charts.Sun.Daily.Elevation, Charts.Sun.Daily.Trajectory -> "${yVal.toInt()}°"
                Charts.Sun.Daily.Irradiance -> "${formatNumber(yVal.toDouble())} W/m²"
                Charts.Sun.Daily.ColorTemperature -> "${yVal.toInt()}K"
                Charts.Sun.Daily.Illuminance -> "${formatNumber(yVal.toDouble())} lx"
                Charts.Sun.Daily.Shadows, Charts.Sun.Daily.AirMass -> yVal.toDouble().printRounded(2)
                else -> "${yVal.toInt()}"
            }
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
        val xLabels = when (chartType) {
            Charts.Sun.Daily.Trajectory -> (30..330 step 30).toList()
            else -> (3..21 step 3).toList()
        }
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

            val text = when (chartType) {
                Charts.Sun.Daily.Trajectory -> {
                    // Reverse the shift just for the label string
                    val realAzimuth = if (shiftTrajectory) (540f - it.toFloat()) % 360 else it.toFloat()
                    val formatted = realAzimuth.toInt()
                    "${if (formatted == 360 || formatted == 0) 0 else formatted}°"
                }
                else -> formatHour(it, true, context)
            }
            val textLayout = textMeasurer.measure(text, labelStyle)
            val offset = when (chartType) {
                Charts.Sun.Daily.Trajectory -> 2.5f
                else -> 2f
            }
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = labelStyle,
                topLeft = Offset(
                    x = xPx - (textLayout.size.width / offset), // Centered horizontally on the hour mark
                    y = height - textLayout.size.height - 2.dp.toPx() // Pinned near the bottom edge of the canvas
                )
            )
        }

        // 6. Calculate exact Y position for the sun at currentHour
        var currentY = 0f
//        if (chartType == SunChartTypes.TRAJECTORY && fixedXValues.isNotEmpty()) {
//            for (i in 0 until fixedXValues.size - 1) {
//                val x1 = fixedXValues[i]
//                val x2 = fixedXValues[i+1]
//
//                // Dynamically unwrap the target to match the local array segment
//                var tempTarget = currentHour
//                while (tempTarget < min(x1, x2) - 180f) tempTarget += 360f
//                while (tempTarget > max(x1, x2) + 180f) tempTarget -= 360f
//
//                if (tempTarget in min(x1, x2)..max(x1, x2)) {
//                    val timeDelta = x2 - x1
//                    if (timeDelta != 0f) {
//                        val fraction = (tempTarget - x1) / timeDelta
//                        currentY = yValues[i] + fraction * (yValues[i+1] - yValues[i])
//                    }
//                    break
//                }
//            }
//        } else {
        // Normal fallback for Time-based charts
        for (i in 0 until fixedXValues.size - 1) {
            val x1 = fixedXValues[i]
            val x2 = fixedXValues[i+1]
            val minX = min(x1, x2)
            val maxX = max(x1, x2)

            if (currentAzimuth in minX..maxX) {
                val timeDelta = x2 - x1
                if (timeDelta != 0f) {
                    // Linear interpolation to find the exact altitude at this moment
                    val fraction = (currentAzimuth - x1) / timeDelta
                    currentY = yValues[i] + fraction * (yValues[i+1] - yValues[i])
                }
                break
            }
        }
//        }
        val currentYPx = mapY(currentY)

        // 7. Paint the Sun Icon if above horizon
        val isSunUp = when (chartType) {
            Charts.Sun.Daily.Elevation, Charts.Sun.Daily.Trajectory -> currentY >= 0f
            Charts.Sun.Daily.ColorTemperature -> currentY > 2000f
            Charts.Sun.Daily.AirMass -> currentY > 1f
            else -> currentY > 0f
        }

        if (isSunUp) {
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
                    with(sunPainter) {
                        draw(
                            size = Size(iconSize, iconSize),
                            colorFilter = ColorFilter.tint(sunYellow)
                        )
                    }
                }
            }
        } else {
            val iconSize = 12.dp.toPx()
            translate(
                left = currentXPx - iconSize / 2,
                top = currentYPx - iconSize / 2
            ) {
                with(indicatorPainter) {
                    draw(
                        size = Size(iconSize, iconSize),
                        colorFilter = ColorFilter.tint(sunYellow.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}