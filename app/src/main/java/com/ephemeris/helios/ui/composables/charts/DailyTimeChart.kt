package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
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
import com.ephemeris.helios.utils.charts.ChartData
import com.ephemeris.helios.utils.charts.createHorizontalBrush
import com.ephemeris.helios.utils.charts.drawNightVerticalTwilights
import com.ephemeris.helios.utils.charts.drawUVSlices
import com.ephemeris.helios.utils.charts.getColorTemperatureBrushGradient
import com.ephemeris.helios.utils.charts.getMapX
import com.ephemeris.helios.utils.charts.getMapY
import com.ephemeris.helios.utils.charts.getMaxX
import com.ephemeris.helios.utils.charts.getMaxY
import com.ephemeris.helios.utils.charts.getMinX
import com.ephemeris.helios.utils.charts.getMinY
import com.ephemeris.helios.utils.charts.getZeroYPixel
import com.ephemeris.helios.utils.formatHour
import com.ephemeris.helios.utils.formatNumber
import com.ephemeris.helios.utils.printRounded
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun DailyTimeChart(
    xValues: FloatArray,
    yValues: FloatArray,
    chartType: Charts,
    currentHour: Float,
    modifier: Modifier = Modifier
) {
    val drawChartIcon = rememberChartIconDrawer(chartType)

    val colors = LocalCustomColors.current
    val dayFill = colors.dayBackground

    // --- NEW: Gradient Theme Colors ---
    // Air Mass (Clear Sky to Hazy Horizon)
    val amZenith = Color(0xFF29B6F6).copy(alpha = 0.5f) // Clear Light Blue
    val amHorizon = Color(0xFFCFD8DC).copy(alpha = 0.5f) // Hazy Grey/White

    // Shadows (Light/Short to Dark/Long)
    val shadowShort = Color(0xFFE0E0E0).copy(alpha = 0.5f)
    val shadowLong = Color(0xFF424242).copy(alpha = 0.5f)

    // Illuminance (Blinding Light to Dim)
    val luxBright = Color(0xFFFFF59D).copy(alpha = 0.6f) // Glowing Pale Yellow
    val luxDim = Color(0xFF5C6BC0).copy(alpha = 0.3f)   // Dim Twilight Blue

    // Irradiance Heat Map (Warm to Hot)
    val irrLow = Color(0xFFFFCC80).copy(alpha = 0.4f)   // Soft Dawn Gold
    val irrMid = Color(0xFFFF9800).copy(alpha = 0.5f)   // Orange Energy
    val irrHigh = Color(0xFFE65100).copy(alpha = 0.6f)  // Intense Heat Red

    val dayBackground = MaterialTheme.colorScheme.surface
    val nightBackground = MaterialTheme.colorScheme.surfaceVariant
    val elapsedDayFill = when (chartType) {
        Charts.Sun.Daily.Elevation -> colors.elapsedDay
        else -> MaterialColors.Gray400.copy(alpha = 0.4f)
    }
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

        val params = ChartData(
            xValues = xValues,
            yValues = yValues,
            minX = getMinX(xValues, chartType),
            maxX = getMaxX(xValues, chartType),
            minY = getMinY(yValues, chartType),
            maxY = getMaxY(yValues, chartType),
            width = size.width,
            height = size.height,
            verticalPaddingPx = 16.dp.toPx()
        )

        // Helper functions to map mathematical coordinates to Canvas pixels
        fun mapX(x: Float) = getMapX(x, params)
        fun mapY(y: Float) = getMapY(y, params, chartType)

        val zeroYPixel = getZeroYPixel(chartType, ::mapY)
        val currentXPx = mapX(currentHour)


        // Day Background
        drawRect(
            color = dayBackground,
            topLeft = Offset(0f, 0f),
            size = Size(params.width, zeroYPixel)
        )

        // Night Background
        drawRect(
            color = nightBackground,
            topLeft = Offset(0f, zeroYPixel),
            size = Size(params.width, params.height - zeroYPixel)
        )

        val wrapThreshold = 200f // Instantly cuts off erratic backend interpolation

        // 1. Build the smooth curve path
        val curvePath = Path().apply {
            moveTo(mapX(xValues[0]), mapY(yValues[0]))
            for (i in 1 until xValues.size) {
                lineTo(mapX(xValues[i]), mapY(yValues[i]))
            }
        }

        // 2. Build the fill path that closes down to the X-axis
        val fillPath = Path().apply {
            moveTo(mapX(xValues[0]), zeroYPixel)
            lineTo(mapX(xValues[0]), mapY(yValues[0]))
            for (i in 1 until xValues.size) {
                lineTo(mapX(xValues[i]), mapY(yValues[i]))
            }
            lineTo(mapX(xValues.last()), zeroYPixel)
            close()
        }

        clipRect(bottom = zeroYPixel) {
            drawPath(path = fillPath, color = dayBackground.copy(alpha = 1.0f))
        }

        clipRect(top = zeroYPixel) {
            drawPath(path = fillPath, color = nightBackground.copy(alpha = 1.0f))
        }

        // 3a. Calculate exact X intersections for vertical stripes
        val thresholds = when(chartType) {
            Charts.Sun.Daily.UvIntensity -> floatArrayOf(0f, 2f, 3f, 5f, 6f, 8f, 10f, 11f)
            else -> floatArrayOf(0f, -6f, -12f, -18f)
        }
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
                when (chartType) {
                    Charts.Sun.Daily.UvIntensity -> drawUVSlices(params, uniqueXPoints, ::mapX, zeroYPixel)
                    Charts.Sun.Daily.ColorTemperature -> {
                        // --- Vertical Color Temperature Gradient ---
                        // Maps fractional stops exactly to their Kelvin altitude
                        val ctBrush = getColorTemperatureBrushGradient(false, ::mapY, params)

                        drawRect(
                            brush = ctBrush,
                            topLeft = Offset(0f, 0f),
                            size = Size(params.width, zeroYPixel)
                        )
                    }
                    Charts.Sun.Daily.AirMass -> {
                        val amBrush = createHorizontalBrush({value ->
                            // Air Mass goes from 1 (Zenith) to ~10+ (Horizon)
                            // We lerp from Clear Blue to Hazy Gray
                            val fraction = ((value.coerceIn(1f, 10f) - 1f) / 9f)
                            lerp(amZenith, amHorizon, fraction)
                        }, params)
                        drawRect(brush = amBrush, topLeft = Offset(0f, 0f), size = Size(params.width, zeroYPixel))
                    }
                    Charts.Sun.Daily.Shadows -> {
                        val shadowBrush = createHorizontalBrush({ value ->
                            // Shadows go from 0 (Short) to ~10+ (Long)
                            // We lerp from Light Silver to Deep Charcoal
                            val fraction = (value.coerceIn(0f, 10f) / 10f)
                            lerp(shadowShort, shadowLong, fraction)
                        }, params)
                        drawRect(brush = shadowBrush, topLeft = Offset(0f, 0f), size = Size(params.width, zeroYPixel))
                    }
                    Charts.Sun.Daily.Illuminance -> {
                        val luxBrush = Brush.verticalGradient(
                            0.0f to luxBright, // Top = Max brightness
                            1.0f to luxDim,    // Bottom = Dim 0 lux
                            startY = mapY(params.maxY),
                            endY = zeroYPixel
                        )
                        drawRect(brush = luxBrush, topLeft = Offset(0f, 0f), size = Size(params.width, zeroYPixel))
                    }
                    Charts.Sun.Daily.Irradiance -> {
                        val irrBrush = createHorizontalBrush({ value ->
                            // Heat Map: Soft Gold -> Orange -> Intense Red
                            val maxIrr = params.maxY.coerceAtLeast(1f)
                            val fraction = (value / maxIrr).coerceIn(0f, 1f)
                            when {
                                fraction <= 0.5f -> lerp(irrLow, irrMid, fraction * 2f)
                                else -> lerp(irrMid, irrHigh, (fraction - 0.5f) * 2f)
                            }
                        }, params)

                        drawRect(brush = irrBrush, topLeft = Offset(0f, 0f), size = Size(params.width, zeroYPixel))
                    }
                    else -> {
                        // Normal Day Fill for all other charts
                        drawRect(
                            color = dayFill,
                            topLeft = Offset(0f, 0f),
                            size = Size(params.width, zeroYPixel)
                        )
                    }
                }
//                }
            }

            drawNightVerticalTwilights(colors, params, uniqueXPoints, ::mapX, zeroYPixel, chartType)

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

        // --- Draw the elapsed path line on top
        val pathColor = when (chartType) {
            is Charts.Sun -> localCustomColors.sunPath
            is Charts.Moon -> localCustomColors.moonPath
            else -> Color.Green // TODO
        }
        clipRect(right = currentXPx) {
            drawPath(
                path = curvePath,
                color = pathColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 5. Draw a subtle X-Axis line to visually separate the zones
        drawLine(
            color = materialTheme.outline,
            start = Offset(0f, zeroYPixel),
            end = Offset(params.width, zeroYPixel),
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
            Charts.Sun.Daily.Elevation,
            Charts.Moon.Daily.Elevation-> (-90 until 91 step 15).map { it.toFloat()}
            Charts.Sun.Daily.Irradiance -> (0 until ((params.maxY / 100.0).roundToInt() * 100 + 1) step 100).map { it.toFloat() }
            Charts.Sun.Daily.UvIntensity -> (0 until (params.maxY.roundToInt() + 1) step floor(params.maxY / 10f).toInt().coerceAtLeast(1)).map { it.toFloat() }
            Charts.Sun.Daily.Illuminance,
            Charts.Moon.Daily.Illuminance -> listOf(0f) + (0..5).flatMap {
                val base = 10.0.pow(it.toDouble()).toFloat()
                listOf(base, base * 3f)
            }.filter { it <= params.maxY }
            Charts.Sun.Daily.Shadows, Charts.Sun.Daily.AirMass,
            Charts.Moon.Daily.Shadows, Charts.Moon.Daily.AirMass -> listOf(0f, 0.25f, 0.5f, 1f, 1.5f, 2f, 3f, 4f, 5f, 6f, 7f, 10f)
            Charts.Sun.Daily.ColorTemperature -> (2000 until (params.maxY.roundToInt() + 1) step 500).map { it.toFloat() }
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
                    end = Offset(params.width, yPx),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = horizontalGridDashEffect
                )
            }

            val text = when (chartType) {
                Charts.Sun.Daily.Elevation -> "${yVal.toInt()}°"
                Charts.Sun.Daily.Irradiance -> "${formatNumber(yVal.toDouble())} W/m²"
                Charts.Sun.Daily.ColorTemperature -> "${formatNumber(yVal.toDouble())}K"
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
        val xLabels = (3..21 step 3).toList()
        xLabels.forEach {
            val xPx = mapX(it.toFloat())

            // Draw vertical dotted grid line
            drawLine(
                color = verticalGridlineColor,
                start = Offset(xPx, 0f),
                end = Offset(xPx, params.height), // Spans the entire canvas height
                strokeWidth = 1.dp.toPx(),
                pathEffect = verticalGridDashEffect
            )

            val text = formatHour(it, true, context)
            val textLayout = textMeasurer.measure(text, labelStyle)

            drawText(
                textMeasurer = textMeasurer,
                text = text,
                style = labelStyle,
                topLeft = Offset(
                    x = xPx - (textLayout.size.width / 2f), // Centered horizontally on the hour mark
                    y = params.height - textLayout.size.height - 2.dp.toPx() // Pinned near the bottom edge of the canvas
                )
            )
        }

        // 6. Calculate exact Y position for the sun at currentHour
        var currentY = 0f
        for (i in 0 until xValues.size - 1) {
            val x1 = xValues[i]
            val x2 = xValues[i+1]
            val minX = min(x1, x2)
            val maxX = max(x1, x2)

            if (currentHour in minX..maxX) {
                val timeDelta = x2 - x1
                if (timeDelta != 0f) {
                    // Linear interpolation to find the exact altitude at this moment
                    val fraction = (currentHour - x1) / timeDelta
                    currentY = yValues[i] + fraction * (yValues[i+1] - yValues[i])
                }
                break
            }
        }
//        }
        val currentYPx = mapY(currentY)

        // --- Draw vertical drop line from Sun to Horizon (X-axis) ---
        drawLine(
            color = localCustomColors.dropLine,
            start = Offset(currentXPx, currentYPx),
            end = Offset(currentXPx, zeroYPixel),
            strokeWidth = 1.dp.toPx()
        )

        // 7. Paint the Sun Icon if above horizon
        val isSunUp = when (chartType) {
            Charts.Sun.Daily.Elevation -> currentY >= 0f
            Charts.Sun.Daily.ColorTemperature -> currentY > 2000f
            Charts.Sun.Daily.AirMass -> currentY > 1f
            else -> currentY > 0f
        }

        if (isSunUp) {
            val iconSize = 24.dp.toPx()

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
