package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.charts.ChartData
import com.ephemeris.helios.utils.charts.buildDynamicPath
import com.ephemeris.helios.utils.charts.createHorizontalBrush
import com.ephemeris.helios.utils.charts.drawCurvePath
import com.ephemeris.helios.utils.charts.drawDayNightAreaFill
import com.ephemeris.helios.utils.charts.drawDayNightBackground
import com.ephemeris.helios.utils.charts.drawDayNightHorizontalTwilights
import com.ephemeris.helios.utils.charts.drawElapsedPath
import com.ephemeris.helios.utils.charts.drawElapsedTimePath
import com.ephemeris.helios.utils.charts.drawHorizonLine
import com.ephemeris.helios.utils.charts.drawNightVerticalTwilights
import com.ephemeris.helios.utils.charts.drawUVSlices
import com.ephemeris.helios.utils.charts.drawVerticalDropLine
import com.ephemeris.helios.utils.charts.drawXLabels
import com.ephemeris.helios.utils.charts.drawYLabels
import com.ephemeris.helios.utils.charts.getColorTemperatureBrushGradient
import com.ephemeris.helios.utils.charts.getMapX
import com.ephemeris.helios.utils.charts.getMapY
import com.ephemeris.helios.utils.charts.getMaxX
import com.ephemeris.helios.utils.charts.getMaxY
import com.ephemeris.helios.utils.charts.getMinX
import com.ephemeris.helios.utils.charts.getMinY
import com.ephemeris.helios.utils.charts.getZeroYPixel
import kotlin.math.max
import kotlin.math.min

@Composable
fun DailyChart(
    xValues: FloatArray,
    yValues: FloatArray,
    chartType: Charts,
    currentHour: Float,
    currentAzimuth: Float,
    currentAltitude: Float,
    modifier: Modifier = Modifier
) {
    val drawChartIcon = rememberChartIconDrawer(chartType)
    val colors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme
    val dayFill = colors.dayBackground

    val elapsedDayFill = when (chartType) {
        Charts.Sun.Daily.Elevation -> colors.elapsedDay
        else -> MaterialColors.Gray400.copy(alpha = 0.4f)
    }
    val elapsedNightFill = colors.elapsedNight

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

        val isTrajectory = chartType.javaClass.simpleName.contains("Trajectory")

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

        // --- Trajectory Shift Logic ---
        // Dynamic Trajectory Shifting
        // Find where the sun reaches its highest point
        val peakIndex = yValues.indices.maxByOrNull { yValues[it] } ?: 0
        val peakAzimuth = xValues[peakIndex]
        // If it culminates North (near 0 or 360), we shift the chart by 180 degrees
        val shiftTrajectory = isTrajectory && (peakAzimuth !in 90f..270f)

        // --- PURE SHIFT MAPPING ---
        // Keeps North(0) at the center, naturally preserving Right-To-Left motion!
        // Create a mapped array purely for continuous drawing
        val drawXValues = if (isTrajectory) {
            FloatArray(xValues.size) { i ->
                if (shiftTrajectory) (xValues[i] + 180f) % 360f else xValues[i]
            }
        } else {
            xValues
        }

        // Helper functions to map mathematical coordinates to Canvas pixels
        fun mapX(x: Float) = getMapX(x, params)
        fun mapY(y: Float) = getMapY(y, params, chartType)

        val zeroYPixel = getZeroYPixel(chartType, ::mapY)

        // --- Current Position Mapping ---
        val drawCurrentX = if (isTrajectory) {
            if (shiftTrajectory) (currentAzimuth + 180f) % 360f else currentAzimuth
        } else {
            currentHour
        }

        val currentXPx = mapX(drawCurrentX)

        // Calculate exact Y position for the sun at currentHour
        var currentY = 0f
        if (isTrajectory) {
            currentY = currentAltitude
        } else {
            for (i in 0 until xValues.size - 1) {
                val x1 = xValues[i]
                val x2 = xValues[i + 1]
                val minX = min(x1, x2)
                val maxX = max(x1, x2)

                if (currentHour in minX..maxX) {
                    val timeDelta = x2 - x1
                    if (timeDelta != 0f) {
                        // Linear interpolation to find the exact altitude at this moment
                        val fraction = (currentHour - x1) / timeDelta
                        currentY =
                            yValues[i] + fraction * (yValues[i + 1] - yValues[i])
                    }
                    break
                }
            }
        }
        val currentYPx = mapY(currentY)
        val bestIndex = ((currentHour / 24f) * (xValues.size - 1)).toInt().coerceIn(0, xValues.size - 1)

        // Path Building
        val primaryCurvePath: Path
        val primaryFillPath: Path
        var elapsedLinePath: Path? = null

        if (isTrajectory) {
            fun buildDynPath(start: Int, end: Int, isFill: Boolean) =
                buildDynamicPath(start, end, isFill, drawXValues, yValues, zeroYPixel, ::mapX, ::mapY)

            primaryCurvePath = buildDynPath(0, drawXValues.size - 1, false)
            primaryFillPath = buildDynPath(0, drawXValues.size - 1, true)
            elapsedLinePath = buildDynPath(0, bestIndex, false)
        } else {
            primaryCurvePath = Path().apply {
                moveTo(mapX(xValues[0]), mapY(yValues[0]))
                for (i in 1 until xValues.size) lineTo(mapX(xValues[i]), mapY(yValues[i]))
            }
            primaryFillPath = Path().apply {
                moveTo(mapX(xValues[0]), zeroYPixel)
                lineTo(mapX(xValues[0]), mapY(yValues[0]))
                for (i in 1 until xValues.size) lineTo(mapX(xValues[i]), mapY(yValues[i]))
                lineTo(mapX(xValues.last()), zeroYPixel)
                close()
            }
        }

        // Day & Night Background
        drawDayNightBackground(colorScheme, params, zeroYPixel)

//        // 1. Build the smooth curve path
//        val primaryCurvePath = Path().apply {
//            moveTo(mapX(xValues[0]), mapY(primaryYValues[0]))
//            for (i in 1 until xValues.size) {
//                lineTo(mapX(xValues[i]), mapY(primaryYValues[i]))
//            }
//        }
//
//        // 2. Build the fill path that closes down to the X-axis
//        val primaryFillPath = Path().apply {
//            moveTo(mapX(xValues[0]), zeroYPixel)
//            lineTo(mapX(xValues[0]), mapY(primaryYValues[0]))
//            for (i in 1 until xValues.size) {
//                lineTo(mapX(xValues[i]), mapY(primaryYValues[i]))
//            }
//            lineTo(mapX(xValues.last()), zeroYPixel)
//            close()
//        }

        drawDayNightAreaFill(primaryFillPath, colorScheme, zeroYPixel)

        if (isTrajectory) {
            // 3. Draw the Twilight Horizontal Bands
            drawDayNightHorizontalTwilights(primaryFillPath, colors, params, zeroYPixel, ::mapY, chartType)
        } else {
            // 3a. Calculate exact X intersections for vertical stripes
            val thresholds = when (chartType) {
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
                val x2 = xValues[i + 1]
                val y1 = yValues[i]
                val y2 = yValues[i + 1]

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
            clipPath(primaryFillPath) {

                // 1. Day area: Removed 'right = currentXPx' so sunset is always visible by default
                clipRect(bottom = zeroYPixel) {
//                if (zeroYPixel > 0f) {
                    when (chartType) {
                        Charts.Sun.Daily.UvIntensity -> drawUVSlices(
                            params,
                            uniqueXPoints,
                            ::mapX,
                            zeroYPixel
                        )

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
                            val amBrush = createHorizontalBrush({ i, value ->
                                // Detect if this 0f is Night (bounded by edges or other 0s)
                                val isNight = value == 0f && (
                                    i == 0 ||
                                    i == params.xValues.lastIndex ||
                                    params.yValues.getOrNull(i - 5) == 0f ||
                                    params.yValues.getOrNull(i + 5) == 0f
                                )
                                // Air Mass goes from 1 (Zenith) to ~10+ (Horizon)
                                if (isNight) {
                                    colors.amHorizon  // Lock to Gray to prevent blue bleed
                                } else {
                                    // We lerp from Clear Blue to Hazy Gray
                                    val fraction = ((value.coerceIn(1f, 10f) - 1f) / 9f)
                                    lerp(colors.amZenith, colors.amHorizon, fraction)
                                }
                            }, params)
                            drawRect(
                                brush = amBrush,
                                topLeft = Offset(0f, 0f),
                                size = Size(params.width, zeroYPixel)
                            )
                        }

                        Charts.Sun.Daily.Shadows -> {
                            val shadowBrush = createHorizontalBrush({ i, value ->
                                // Detect if this 0f is Night (bounded by edges or other 0s)
                                val isNight = value == 0f && (
                                    i == 0 ||
                                    i == params.xValues.lastIndex ||
                                    params.yValues.getOrNull(i - 5) == 0f ||
                                    params.yValues.getOrNull(i + 5) == 0f
                                )
                                // Shadows go from 0 (Short) to ~10+ (Long)
                                if (isNight) {
                                    colors.shadowLong  // Lock to Dark to prevent light bleed
                                } else {
                                    // We lerp from Light Silver to Deep Charcoal
                                    val fraction = (value.coerceIn(0f, 10f) / 10f)
                                    lerp(colors.shadowShort, colors.shadowLong, fraction)
                                }
                            }, params)
                            drawRect(
                                brush = shadowBrush,
                                topLeft = Offset(0f, 0f),
                                size = Size(params.width, zeroYPixel)
                            )
                        }

                        Charts.Sun.Daily.Illuminance -> {
                            val luxBrush = Brush.verticalGradient(
                                0.0f to colors.luxBright, // Top = Max brightness
                                1.0f to colors.luxDim,    // Bottom = Dim 0 lux
                                startY = mapY(params.maxY),
                                endY = zeroYPixel
                            )
                            drawRect(
                                brush = luxBrush,
                                topLeft = Offset(0f, 0f),
                                size = Size(params.width, zeroYPixel)
                            )
                        }

                        Charts.Sun.Daily.Irradiance -> {
                            val irrBrush = createHorizontalBrush({ i, value ->
                                // Irradiance doesn't need the isNight check because 0 is already the lowest color
                                // Heat Map: Soft Gold -> Orange -> Intense Red
                                val maxIrr = params.maxY.coerceAtLeast(1f)
                                val fraction = (value / maxIrr).coerceIn(0f, 1f)
                                when {
                                    fraction <= 0.5f -> lerp(
                                        colors.irrLow,
                                        colors.irrMid,
                                        fraction * 2f
                                    )

                                    else -> lerp(
                                        colors.irrMid,
                                        colors.irrHigh,
                                        (fraction - 0.5f) * 2f
                                    )
                                }
                            }, params)

                            drawRect(
                                brush = irrBrush,
                                topLeft = Offset(0f, 0f),
                                size = Size(params.width, zeroYPixel)
                            )
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

                drawNightVerticalTwilights(
                    colors,
                    params,
                    uniqueXPoints,
                    ::mapX,
                    zeroYPixel,
                    chartType
                )

                // 3. Draw the elapsed time overlay (clipped strictly up to currentHour)
                clipRect(right = currentXPx) {
                    // Elapsed Day (Above 0deg)
                    clipRect(bottom = zeroYPixel) {
                        drawPath(path = primaryFillPath, color = elapsedDayFill)
                    }

                    // Elapsed Night (Below 0deg)
                    clipRect(top = zeroYPixel) {
                        drawPath(path = primaryFillPath, color = elapsedNightFill)
                    }
                }
            }
        }

        // 4. Draw the full unclipped curve line for all values
        drawCurvePath(primaryCurvePath, materialTheme)

        if (isTrajectory) {
            drawElapsedPath(elapsedLinePath!!, localCustomColors, chartType)
        } else {
            // --- Draw the elapsed path line on top
            drawElapsedTimePath(primaryCurvePath, localCustomColors, chartType, currentXPx)
        }

        // 5. Draw a subtle X-Axis line to visually separate the zones
        drawHorizonLine(materialTheme, params, zeroYPixel)

        // 5a. Draw Vertical Legend (Y-axis Altitudes)
        drawYLabels(chartType, materialTheme, params, ::mapY, textMeasurer, labelStyle)

        if (isTrajectory) {
            drawXLabels(chartType, materialTheme, params, ::mapX, textMeasurer, labelStyle, context, shiftTrajectory)
        } else {
            // 5b. Draw Horizontal Legend (X-axis Hours)
            drawXLabels(chartType, materialTheme, params, ::mapX, textMeasurer, labelStyle, context)
        }

        // --- Draw vertical drop line from Sun to Horizon (X-axis) ---
        drawVerticalDropLine(localCustomColors, currentXPx, currentYPx, zeroYPixel)

        // 7. Paint the Sun Icon if above horizon
        paintIcon(currentXPx, currentY, currentYPx, zeroYPixel, chartType, drawChartIcon)
    }
}
