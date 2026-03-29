package com.ephemeris.helios.ui.composables

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
import com.ephemeris.helios.utils.SunChartTypes
import com.ephemeris.helios.utils.formatHour
import com.ephemeris.helios.utils.formatNumber
import com.ephemeris.helios.utils.printRounded
import com.ephemeris.helios.utils.roundToSignificant
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

@Composable
fun SunPathChart(
    xValues: FloatArray,
    yValues: FloatArray,
    chartType: SunChartTypes,
    currentHour: Float,
    modifier: Modifier = Modifier
) {
    val sunPainter = painterResource(id = R.drawable.ic_brightness_empty_filled)
    val indicatorPainter = painterResource(id = R.drawable.ic_circle_filled)

    val colors = LocalCustomColors.current
    val sunYellow = colors.sun
    val dayFill = colors.day
    val civilTwilightFill = colors.civilTwilight
    val nauticalTwilightFill = colors.nauticalTwilight
    val astroTwilightFill = colors.astronomicalTwilight
    val nightFill = colors.night
    //val dayBackground = colors.dayBackground
    //val nightBackground = colors.nightBackground
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

        val minX = when (chartType) {
            SunChartTypes.TRAJECTORY -> 0f
            else -> xValues.minOrNull() ?: 0f
        }
        val maxX = when (chartType) {
            SunChartTypes.TRAJECTORY -> 360f
            else -> xValues.maxOrNull() ?: 24f
        }
        val minY = when (chartType) {
            SunChartTypes.ELEVATION, SunChartTypes.TRAJECTORY -> -90f
            SunChartTypes.COLOR_TEMPERATURE -> 2000f
            SunChartTypes.AIR_MASS -> 1f
            else -> 0f
        }
        val maxY = when (chartType) {
            SunChartTypes.ELEVATION, SunChartTypes.TRAJECTORY -> 90f
            SunChartTypes.IRRADIANCE -> max(1000f, yValues.max())
            SunChartTypes.UV_INTENSITY -> max(10f, yValues.max())
            SunChartTypes.ILLUMINANCE -> max(100000f, yValues.max())
            SunChartTypes.SHADOWS -> 10f
            SunChartTypes.COLOR_TEMPERATURE -> max(5500f, yValues.max())
            SunChartTypes.AIR_MASS -> 10f // or 15f
        }

        val verticalPaddingPx = 16.dp.toPx()
        val drawHeight = (height - (2 * verticalPaddingPx)).coerceAtLeast(1f)

        val isLogScale = when (chartType) {
            SunChartTypes.ILLUMINANCE, SunChartTypes.SHADOWS, SunChartTypes.AIR_MASS -> true
            else -> false
        }

        // Dynamic Trajectory Shifting
        // Find where the sun reaches its highest point
        val peakIndex = yValues.indices.maxByOrNull { yValues[it] } ?: 0
        val peakAzimuth = xValues[peakIndex]
        // If it culminates North (near 0 or 360), we shift the chart by 180 degrees
        val shiftTrajectory = chartType == SunChartTypes.TRAJECTORY && (peakAzimuth < 90f || peakAzimuth > 270f)

        // Create a mapped array purely for continuous drawing
        val drawXValues = FloatArray(xValues.size) { i ->
            // N(0) goes to 180, E(90) stays 90, W(270) stays 270. Time moves left-to-right!
            if (shiftTrajectory) (540f - xValues[i]) % 360f else xValues[i]
        }

        // Helper functions to map mathematical coordinates to Canvas pixels
        fun mapX(x: Float) = ((x - minX) / (maxX - minX)) * width
        // Canvas Y=0 is at the top, so we invert the Y mapping
        fun mapY(y: Float): Float {
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

        val zeroYPixel = when (chartType) {
            SunChartTypes.COLOR_TEMPERATURE -> mapY(2000f)
            SunChartTypes.AIR_MASS -> mapY(1f)
            else -> mapY(0f)
        }
        // Ensure the sun icon aligns with the shifted mapping
        val drawCurrentX = if (shiftTrajectory) (540f - currentHour) % 360f else currentHour
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
                moveTo(mapX(xValues[0]), mapY(yValues[0]))
                for (i in 1 until xValues.size) {
                    // If the azimuth wraps around 360, pick up the pen and move it!
                    if (chartType == SunChartTypes.TRAJECTORY && abs(drawXValues[i] - drawXValues[i-1]) > wrapThreshold) {
                        moveTo(mapX(drawXValues[i]), mapY(yValues[i]))
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
                    if (chartType == SunChartTypes.TRAJECTORY && abs(drawXValues[i] - drawXValues[i-1]) > wrapThreshold) {
                        // Drop down and seal the previous chunk safely
                        lineTo(mapX(drawXValues[i-1]), zeroYPixel)
                        close()

                        // Move to the new edge and start the next chunk
                        moveTo(mapX(drawXValues[i]), zeroYPixel)
                        lineTo(mapX(drawXValues[i]), mapY(yValues[i]))
                    } else {
                        lineTo(mapX(drawXValues[i]), mapY(yValues[i]))
                    }
                }
                lineTo(mapX(drawXValues.last()), zeroYPixel)
                close()
            }
        }

        if (chartType == SunChartTypes.TRAJECTORY) {
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
        for (x in xValues) {
            // Add all original X points
            sortedXPoints.add(x)
        }
        // Add all mathematical crossing points
        for (i in 0 until xValues.size - 1) {
            val x1 = xValues[i]
            val x2 = xValues[i+1]
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

            if (chartType == SunChartTypes.TRAJECTORY) {
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
                    for (j in 0 until xValues.size - 1) {
                        if (midX >= xValues[j] && midX <= xValues[j + 1]) {
                            val delta = xValues[j + 1] - xValues[j]
                            midY = if (delta > 0f) {
                                yValues[j] + ((midX - xValues[j]) / delta) * (yValues[j + 1] - yValues[j])
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
            SunChartTypes.ELEVATION, SunChartTypes.TRAJECTORY -> (-90 until 91 step 15).map { it.toFloat()}
            SunChartTypes.IRRADIANCE -> (0 until (maxY.toInt() + 1) step 100).map { it.toFloat() }
            SunChartTypes.UV_INTENSITY -> (0 until (maxY.toInt() + 1) step floor(maxY / 10f).toInt()).map { it.toFloat() }
            SunChartTypes.ILLUMINANCE -> listOf(0f) + (0..5).flatMap {
                val base = 10.0.pow(it.toDouble()).toFloat()
                listOf(base, base * 3f)
            }.filter { it <= maxY }
            SunChartTypes.SHADOWS -> listOf(0f, 0.25f, 0.5f, 1f, 1.5f, 2f, 3f, 4f, 5f, 6f, 7f, 10f)
            SunChartTypes.COLOR_TEMPERATURE -> (2000 until 5501 step 500).map { it.toFloat() }
            SunChartTypes.AIR_MASS -> listOf(1f, 1.5f, 2f, 3f, 4f, 5f, 6f, 7f, 10f)
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
                SunChartTypes.ELEVATION, SunChartTypes.TRAJECTORY -> "${yVal.toInt()}°"
                SunChartTypes.IRRADIANCE -> "${formatNumber(yVal.toDouble())} W/m²"
                SunChartTypes.COLOR_TEMPERATURE -> "${yVal.toInt()}K"
                SunChartTypes.ILLUMINANCE -> "${formatNumber(yVal.toDouble())} lx"
                SunChartTypes.SHADOWS, SunChartTypes.AIR_MASS -> yVal.toDouble().printRounded(2)
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
            SunChartTypes.TRAJECTORY -> (30..330 step 30).toList()
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
                SunChartTypes.TRAJECTORY -> {
                    // Reverse the shift just for the label string
                    val realAzimuth = if (shiftTrajectory) (540f - it.toFloat()) % 360 else it.toFloat()
                    val formatted = realAzimuth.toInt()
                    "${if (formatted == 360 || formatted == 0) 0 else formatted}°"
                }
                else -> formatHour(it, true, context)
            }
            val textLayout = textMeasurer.measure(text, labelStyle)
            val offset = when (chartType) {
                SunChartTypes.TRAJECTORY -> 2.5f
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
        for (i in 0 until xValues.size - 1) {
            var x1 = xValues[i]
            var x2 = xValues[i+1]

            // Robust wrap handling for both directions (359 -> 1 AND 1 -> 359), safely skipping erratic data points
            if (chartType == SunChartTypes.TRAJECTORY && abs(x1 - x2) > wrapThreshold) {
                if (x1 > x2) {
                    if (currentHour > 180f) x2 += 360f else x1 -= 360f
                } else {
                    if (currentHour < 180f) x2 -= 360f else x1 += 360f
                }
            }

            val minX = min(x1, x2)
            val maxX = max(x1, x2)

            if (currentHour in minX..maxX) {
                val timeDelta = x2 - x1
                if (timeDelta > 0f) {
                    // Linear interpolation to find the exact altitude at this moment
                    val fraction = (currentHour - x1) / timeDelta
                    currentY = yValues[i] + fraction * (yValues[i+1] - yValues[i])
                }
                break
            }
        }
        val currentYPx = mapY(currentY)

        // 7. Paint the Sun Icon if above horizon

        val condition = when (chartType) {
            SunChartTypes.COLOR_TEMPERATURE -> currentY > (zeroYPixel + 1456f) // Todo: this is the lowest threshold at which it works. Make it become useless
            else -> currentY >= zeroYPixel
        }

        if (condition) {
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