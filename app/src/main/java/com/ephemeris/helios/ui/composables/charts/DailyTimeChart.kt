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
import com.ephemeris.helios.utils.formatHour
import com.ephemeris.helios.utils.formatNumber
import com.ephemeris.helios.utils.printRounded
import kotlin.collections.emptyList
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
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
    val sunYellow = colors.sun
    val dayFill = colors.dayBackground
    val civilTwilightFill = colors.civilTwilight
    val nauticalTwilightFill = colors.nauticalTwilight
    val astroTwilightFill = colors.astronomicalTwilight
    val nightFill = colors.nightBackground

    val uvDarkGreen = Color(0xFF2E7D32).copy(alpha = 0.5f)
    val uvGreen = Color(0xFF4CAF50).copy(alpha = 0.5f)
    val uvYellow = Color(0xFFFFEB3B).copy(alpha = 0.5f)
    val uvAmber = Color(0xFFFFC107).copy(alpha = 0.5f)
    val uvOrange = Color(0xFFFF9800).copy(alpha = 0.5f)
    val uvRed = Color(0xFFF44336).copy(alpha = 0.5f)
    val uvDarkRed = Color(0xFFB71C1C).copy(alpha = 0.5f)
    val uvPurple = Color(0xFF673AB7).copy(alpha = 0.5f)

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

        val width = size.width
        val height = size.height

        val minX = xValues.minOrNull() ?: 0f
        val maxX = xValues.maxOrNull() ?: 24f
        val minY = when (chartType) {
            Charts.Sun.Daily.Elevation, Charts.Moon.Daily.Elevation -> -90f
            Charts.Sun.Daily.ColorTemperature -> 2000f
//            Charts.Sun.Daily.AirMass -> 0f
            else -> 0f
        }
        val maxY = when (chartType) {
            Charts.Sun.Daily.Elevation, Charts.Moon.Daily.Elevation -> 90f
            Charts.Sun.Daily.Irradiance -> max(500f, (yValues.max() / 100.0).roundToInt() * 100f)
            Charts.Sun.Daily.UvIntensity -> max(5f, yValues.max().roundToInt().toFloat())
            Charts.Sun.Daily.Illuminance -> max(100000f, yValues.max())
            Charts.Sun.Daily.Shadows -> 10f
            Charts.Sun.Daily.ColorTemperature -> max(5500f, yValues.max())
            Charts.Sun.Daily.AirMass -> 10f // or 15f
            else -> 90f // Todo: Change
        }

        val verticalPaddingPx = 16.dp.toPx()
        val drawHeight = (height - (2 * verticalPaddingPx)).coerceAtLeast(1f)

        val isLogScale = when (chartType) {
            Charts.Sun.Daily.Illuminance, Charts.Sun.Daily.Shadows, Charts.Sun.Daily.AirMass -> true
            else -> false
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
            Charts.Sun.Daily.ColorTemperature -> mapY(2000f)
//            Charts.Sun.Daily.AirMass -> mapY(1f)
//            Charts.Sun.Daily.AirMass -> mapY(0f)
            else -> mapY(0f)
        }
        val currentXPx = mapX(currentHour)

        val params = ChartData(
            xValues = xValues,
            yValues = yValues,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            width = width,
            height = height
        )

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
                    Charts.Sun.Daily.UvIntensity -> {
                        // --- UV Slicing Logic ---
                        var currentBlockColor = Color.Transparent
                        var blockStartX = uniqueXPoints.firstOrNull() ?: 0f

                        for (i in 0 until uniqueXPoints.size - 1) {
                            val xA = uniqueXPoints[i]
                            val xB = uniqueXPoints[i + 1]
                            val midX = (xA + xB) / 2f

                            // Interpolate to find UV Index at the exact center of this stripe
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
                                midY < 0.01f -> Color.Transparent // Nighttime/Zero UV
                                midY < 2f -> uvDarkGreen
                                midY < 3f -> uvGreen
                                midY < 5f -> uvYellow
                                midY < 6f -> uvAmber
                                midY < 8f -> uvOrange
                                midY < 10f -> uvRed
                                midY < 11f -> uvDarkRed
                                else -> uvPurple
                            }

                            // Draw the block if the color changes
                            if (sliceColor != currentBlockColor) {
                                if (currentBlockColor != Color.Transparent && i > 0) {
                                    val startPx = round(mapX(blockStartX))
                                    val endPx = round(mapX(xA))
                                    drawRect(
                                        color = currentBlockColor,
                                        topLeft = Offset(startPx, 0f), // Starts at very top of canvas
                                        size = Size(endPx - startPx, zeroYPixel) // Extends down to horizon line
                                    )
                                }
                                currentBlockColor = sliceColor
                                blockStartX = xA
                            }
                        }

                        // Draw the final accumulated UV block
                        if (currentBlockColor != Color.Transparent) {
                            val startPx = round(mapX(blockStartX))
                            val endPx = round(mapX(uniqueXPoints.last()))
                            drawRect(
                                color = currentBlockColor,
                                topLeft = Offset(startPx, 0f),
                                size = Size(endPx - startPx, zeroYPixel)
                            )
                        }
                    }
                    Charts.Sun.Daily.ColorTemperature -> {
                        // --- Vertical Color Temperature Gradient ---
                        // Maps fractional stops exactly to their Kelvin altitude
                        val ctBrush = getColorTemperatureBrushGradient(false, ::mapY, params)

                        drawRect(
                            brush = ctBrush,
                            topLeft = Offset(0f, 0f),
                            size = Size(width, zeroYPixel)
                        )
                    }
                    Charts.Sun.Daily.AirMass -> {
//                        val amBrush = Brush.verticalGradient(
//                            0.0f to amZenith,
//                            1.0f to amHorizon,
//                            startY = mapY(1f),
//                            endY = mapY(10f) // Maps to the standard visual limit
//                        )
                        val amBrush = createHorizontalBrush({value ->
                            // Air Mass goes from 1 (Zenith) to ~10+ (Horizon)
                            // We lerp from Clear Blue to Hazy Gray
                            val fraction = ((value.coerceIn(1f, 10f) - 1f) / 9f)
                            lerp(amZenith, amHorizon, fraction)
                        }, params)
                        drawRect(brush = amBrush, topLeft = Offset(0f, 0f), size = Size(width, zeroYPixel))
                    }
                    Charts.Sun.Daily.Shadows -> {
//                        val shadowBrush = Brush.verticalGradient(
//                            0.0f to shadowShort, // Top = Short shadows
//                            1.0f to shadowLong,  // Bottom = Long shadows
//                            startY = mapY(0f),
//                            endY = mapY(10f)
//                        )
                        val shadowBrush = createHorizontalBrush({ value ->
                            // Shadows go from 0 (Short) to ~10+ (Long)
                            // We lerp from Light Silver to Deep Charcoal
                            val fraction = (value.coerceIn(0f, 10f) / 10f)
                            lerp(shadowShort, shadowLong, fraction)
                        }, params)
                        drawRect(brush = shadowBrush, topLeft = Offset(0f, 0f), size = Size(width, zeroYPixel))
                    }
                    Charts.Sun.Daily.Illuminance -> {
                        val luxBrush = Brush.verticalGradient(
                            0.0f to luxBright, // Top = Max brightness
                            1.0f to luxDim,    // Bottom = Dim 0 lux
                            startY = mapY(maxY),
                            endY = zeroYPixel
                        )
                        drawRect(brush = luxBrush, topLeft = Offset(0f, 0f), size = Size(width, zeroYPixel))
                    }
                    Charts.Sun.Daily.Irradiance -> {
//                        // Horizontal Heat Map using Lerp
//                        fun getIrrColor(value: Float): Color {
//                            val maxIrr = maxY.coerceAtLeast(1f)
//                            val fraction = (value / maxIrr).coerceIn(0f, 1f)
//                            return when {
//                                fraction <= 0.5f -> lerp(irrLow, irrMid, fraction * 2f)
//                                else -> lerp(irrMid, irrHigh, (fraction - 0.5f) * 2f)
//                            }
//                        }
//
//                        val irrStops = mutableListOf<Pair<Float, Color>>()
//                        val step = max(1, xValues.size / 40)
//
//                        for (i in xValues.indices step step) {
//                            val fraction = ((xValues[i] - minX) / (maxX - minX)).coerceIn(0f, 1f)
//                            irrStops.add(fraction to getIrrColor(yValues[i]))
//                        }
//
//                        val peakIndex = yValues.indices.maxByOrNull { yValues[it] } ?: 0
//                        val peakFraction = ((xValues[peakIndex] - minX) / (maxX - minX)).coerceIn(0f, 1f)
//                        irrStops.add(peakFraction to getIrrColor(yValues[peakIndex]))
//
//                        val lastFraction = ((xValues.last() - minX) / (maxX - minX)).coerceIn(0f, 1f)
//                        irrStops.add(lastFraction to getIrrColor(yValues.last()))
//
//                        val finalStops = irrStops
//                            .distinctBy { it.first }
//                            .sortedBy { it.first }
//                            .toTypedArray()

//                        val irrBrush = Brush.horizontalGradient(
//                            *finalStops,
//                            startX = 0f,
//                            endX = width
//                        )

                        val irrBrush = createHorizontalBrush({ value ->
                            // Heat Map: Soft Gold -> Orange -> Intense Red
                            val maxIrr = maxY.coerceAtLeast(1f)
                            val fraction = (value / maxIrr).coerceIn(0f, 1f)
                            when {
                                fraction <= 0.5f -> lerp(irrLow, irrMid, fraction * 2f)
                                else -> lerp(irrMid, irrHigh, (fraction - 0.5f) * 2f)
                            }
                        }, params)

                        drawRect(brush = irrBrush, topLeft = Offset(0f, 0f), size = Size(width, zeroYPixel))
                    }
                    else -> {
                        // Normal Day Fill for all other charts
                        drawRect(
                            color = dayFill,
                            topLeft = Offset(0f, 0f),
                            size = Size(width, zeroYPixel)
                        )
                    }
                }
//                }
            }

            if (chartType is Charts.Sun) {
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
            } else {
                // TODO: drawRect()
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
            Charts.Sun.Daily.Elevation,
            Charts.Moon.Daily.Elevation-> (-90 until 91 step 15).map { it.toFloat()}
            Charts.Sun.Daily.Irradiance -> (0 until ((maxY / 100.0).roundToInt() * 100 + 1) step 100).map { it.toFloat() }
            Charts.Sun.Daily.UvIntensity -> (0 until (maxY.roundToInt() + 1) step floor(maxY / 10f).toInt().coerceAtLeast(1)).map { it.toFloat() }
            Charts.Sun.Daily.Illuminance,
            Charts.Moon.Daily.Illuminance -> listOf(0f) + (0..5).flatMap {
                val base = 10.0.pow(it.toDouble()).toFloat()
                listOf(base, base * 3f)
            }.filter { it <= maxY }
            Charts.Sun.Daily.Shadows, Charts.Sun.Daily.AirMass,
            Charts.Moon.Daily.Shadows, Charts.Moon.Daily.AirMass -> listOf(0f, 0.25f, 0.5f, 1f, 1.5f, 2f, 3f, 4f, 5f, 6f, 7f, 10f)
            Charts.Sun.Daily.ColorTemperature -> (2000 until (maxY.roundToInt() + 1) step 500).map { it.toFloat() }
//            Charts.Sun.Daily.AirMass -> {
//                val base = mutableListOf(0f, 0.5f, 1f, 1.5f, 2f, 3f, 4f, 5f, 6f, 7f, 10f)
//                when (minY) {
//                    in 0.5f..1f -> {
//                        base.add(0, 0.5f)
//                        base
//                    }
//                    in 0f..0.5f -> {
//                        base.add(0, 0.5f)
//                        base.add(0, 0f)
//                    }
//                }
//                base.toList()
//            }
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
                end = Offset(xPx, height), // Spans the entire canvas height
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

data class ChartData(
    val xValues: FloatArray,
    val yValues: FloatArray,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val width: Float,
    val height: Float,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChartData

        if (minX != other.minX) return false
        if (maxX != other.maxX) return false
        if (minY != other.minY) return false
        if (maxY != other.maxY) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!xValues.contentEquals(other.xValues)) return false
        if (!yValues.contentEquals(other.yValues)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minX.hashCode()
        result = 31 * result + maxX.hashCode()
        result = 31 * result + minY.hashCode()
        result = 31 * result + maxY.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + xValues.contentHashCode()
        result = 31 * result + yValues.contentHashCode()
        return result
    }
}

// --- HELPER: Reusable Horizontal Gradient Generator ---
// Extracts the complex stop-generation logic to keep code DRY
fun createHorizontalBrush(getColor: (Float) -> Color, params: ChartData): Brush {
    val stops = mutableListOf<Pair<Float, Color>>()
    val step = max(1, params.xValues.size / 40)

    for (i in params.xValues.indices step step) {
        val fraction = ((params.xValues[i] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
        stops.add(fraction to getColor(params.yValues[i]))
    }

    // Always map the peak explicitly so gradients peak perfectly
    val peakIndex = params.yValues.indices.maxByOrNull { params.yValues[it] } ?: 0
    val peakFraction = ((params.xValues[peakIndex] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
    stops.add(peakFraction to getColor(params.yValues[peakIndex]))

    // Always map the end explicitly
    val lastFraction = ((params.xValues.last() - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
    stops.add(lastFraction to getColor(params.yValues.last()))

    val finalStops = stops
        .distinctBy { it.first }
        .sortedBy { it.first }
        .toTypedArray()

    return Brush.horizontalGradient(*finalStops, startX = 0f, endX = params.width)
}

fun getColorTemperatureBrushGradient(
    isGradientHorizontal: Boolean = false,
    mapY: (Float) -> Float,
    params: ChartData
): Brush {
    val ct5500 = Color(0xFF81D4FA).copy(alpha = 0.5f) // Daylight Cool Blue
    val ct4000 = Color(0xFFFFF59D).copy(alpha = 0.5f) // Warm Pale Yellow
    val ct3000 = Color(0xFFFFB300).copy(alpha = 0.5f) // Golden Amber
    val ct2000 = Color(0xFFD84315).copy(alpha = 0.5f) // Deep Sunset Red

    return if (!isGradientHorizontal) {
        Brush.verticalGradient(
            0.0f to ct5500,
            ((5500f - 4000f) / 3500f) to ct4000, // ~0.42f
            ((5500f - 3000f) / 3500f) to ct3000, // ~0.71f
            1.0f to ct2000,
            startY = mapY(5500f),
            endY = mapY(2000f)
        )
    } else {
        // Helper to interpolate exact Kelvin to our defined colors
        fun getCtColor(temp: Float): Color {
            return when {
                temp <= 2000f -> ct2000
                temp <= 3000f -> lerp(ct2000, ct3000, (temp - 2000f) / 1000f)
                temp <= 4000f -> lerp(ct3000, ct4000, (temp - 3000f) / 1000f)
                temp < 5500f -> lerp(ct4000, ct5500, (temp - 4000f) / 1500f)
                else -> ct5500
            }
        }

        // Dynamically generate horizontal stops based on the actual data
        val ctStops = mutableListOf<Pair<Float, Color>>()

        // Sample points to create a smooth gradient (every ~12 points is plenty for 481 items)
        val step = max(1, params.xValues.size / 40)
        for (i in params.xValues.indices step step) {
            val fraction = ((params.xValues[i] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
            ctStops.add(fraction to getCtColor(params.yValues[i]))
        }

        // Ensure the exact peak temperature is mathematically represented
        val peakIndex = params.yValues.indices.maxByOrNull { params.yValues[it] } ?: 0
        val peakFraction = ((params.xValues[peakIndex] - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
        ctStops.add(peakFraction to getCtColor(params.yValues[peakIndex]))

        // Ensure the very last point is included to reach endX safely
        val lastFraction = ((params.xValues.last() - params.minX) / (params.maxX - params.minX)).coerceIn(0f, 1f)
        ctStops.add(lastFraction to getCtColor(params.yValues.last()))

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