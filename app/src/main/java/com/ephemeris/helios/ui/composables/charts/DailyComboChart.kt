package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.charts.*
import com.ephemeris.helios.utils.location.Coordinates
import kotlin.math.max
import kotlin.math.min

@Composable
fun DailyComboChart(
    sunXValues: FloatArray,
    sunYValues: FloatArray,
    moonXValues: FloatArray,
    moonYValues: FloatArray,
    chartType: Charts,
    currentHour: Float,
    currentSunAzimuth: Float,
    currentSunAltitude: Float,
    currentMoonAzimuth: Float,
    currentMoonAltitude: Float,
    coordinates: Coordinates?,
    modifier: Modifier = Modifier,
    isApparentElevation: Boolean = IS_APPARENT_ELEVATION_DEFAULT
) {
    val sunChartType = Charts.Sun.Daily.Elevation
    val moonChartType = Charts.Moon.Daily.Elevation

    // 1. Independent Icon Drawers
    val drawSunIcon = rememberChartIconDrawer(sunChartType)
    val drawMoonIcon = rememberChartIconDrawer(moonChartType) // Assuming Moon chart type

    val colors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme
    val materialTheme = MaterialTheme.colorScheme
    val localCustomColors = LocalCustomColors.current
    val context = LocalContext.current

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        fontSize = (9.5).sp,
        fontFamily = FontFamily.Monospace
    )

    Canvas(modifier = modifier) {
        if (sunXValues.isEmpty() || sunYValues.isEmpty() || moonXValues.isEmpty()) return@Canvas

        val className = chartType.javaClass.simpleName
        val isElevationOrTrajectory = className.contains("Elevation") || className.contains("Trajectory")
        val shouldTransform = isApparentElevation && isElevationOrTrajectory
        val isTrajectory = chartType.javaClass.simpleName.contains("Trajectory")

        val drawSunYValues = if (shouldTransform) transformApparentElevation(sunYValues) else sunYValues
        val drawMoonYValues = if (shouldTransform) transformApparentElevation(moonYValues) else moonYValues

        val drawCurrentSunAltitude = if (shouldTransform) transformApparentElevation(currentSunAltitude) else currentSunAltitude
        val drawCurrentMoonAltitude = if (shouldTransform) transformApparentElevation(currentMoonAltitude) else currentMoonAltitude


        // We base the chart bounds on the Sun's properties (since it dictates the daylight constraints)
        val params = ChartData(
            xValues = sunXValues,
            yValues = drawSunYValues,
            minX = getMinX(sunXValues, chartType),
            maxX = getMaxX(sunXValues, chartType),
            minY = -90f, // Combo charts always span the full celestial sphere
            maxY = 90f,
            width = size.width,
            height = size.height,
            verticalPaddingPx = 16.dp.toPx()
        )

        // --- TRAJECTORY SHIFT LOGIC (Dictated by Sun) ---
        val sunPeakIndex = sunYValues.indices.maxByOrNull { sunYValues[it] } ?: 0
        val sunPeakAzimuth = sunXValues[sunPeakIndex]
        val shiftTrajectory = isTrajectory && (sunPeakAzimuth !in 90f..270f)

        // Shift BOTH arrays by the exact same amount so they share the same compass coordinates
        val drawSunX = if (isTrajectory) FloatArray(sunXValues.size) { i -> if (shiftTrajectory) (sunXValues[i] + 180f) % 360f else sunXValues[i] } else sunXValues
        val drawMoonX = if (isTrajectory) FloatArray(moonXValues.size) { i -> if (shiftTrajectory) (moonXValues[i] + 180f) % 360f else moonXValues[i] } else moonXValues

        fun mapX(x: Float) = getMapX(x, params)
        fun mapY(y: Float) = getMapY(y, params, chartType)
        val zeroYPixel = getZeroYPixel(chartType, ::mapY, coordinates, shouldTransform)

        // --- CURRENT POSITIONS ---
        val drawCurrentSunX = if (isTrajectory) { if (shiftTrajectory) (currentSunAzimuth + 180f) % 360f else currentSunAzimuth } else currentHour
        val drawCurrentMoonX = if (isTrajectory) { if (shiftTrajectory) (currentMoonAzimuth + 180f) % 360f else currentMoonAzimuth } else currentHour

        val currentSunXPx = mapX(drawCurrentSunX)
        val currentMoonXPx = mapX(drawCurrentMoonX)
        val currentSunYPx = mapY(drawCurrentSunAltitude)
        val currentMoonYPx = mapY(drawCurrentMoonAltitude)

        val bestIndex = ((currentHour / 24f) * (sunXValues.size - 1)).toInt().coerceIn(0, sunXValues.size - 1)

        // --- PATH BUILDING ---
        val sunCurvePath: Path; val sunFillPath: Path; var sunElapsedPath: Path? = null
        val moonCurvePath: Path; val moonFillPath: Path; var moonElapsedPath: Path? = null

        if (isTrajectory) {
            fun buildDynPath(start: Int, end: Int, isFill: Boolean, xVals: FloatArray, yVals: FloatArray) =
                buildDynamicPath(start, end, isFill, xVals, yVals, zeroYPixel, ::mapX, ::mapY)

            sunCurvePath = buildDynPath(0, drawSunX.size - 1, false, drawSunX, drawSunYValues)
            sunFillPath = buildDynPath(0, drawSunX.size - 1, true, drawSunX, drawSunYValues)
            sunElapsedPath = buildDynPath(0, bestIndex, false, drawSunX, drawSunYValues)

            moonCurvePath = buildDynPath(0, drawMoonX.size - 1, false, drawMoonX, drawMoonYValues)
            moonFillPath = buildDynPath(0, drawMoonX.size - 1, true, drawMoonX, drawMoonYValues)
            moonElapsedPath = buildDynPath(0, bestIndex, false, drawMoonX, drawMoonYValues)
        } else {
            sunCurvePath = Path().apply { moveTo(mapX(sunXValues[0]), mapY(drawSunYValues[0])); for (i in 1 until sunXValues.size) lineTo(mapX(sunXValues[i]), mapY(drawSunYValues[i])) }
            sunFillPath = Path().apply { moveTo(mapX(sunXValues[0]), zeroYPixel); lineTo(mapX(sunXValues[0]), mapY(drawSunYValues[0])); for (i in 1 until sunXValues.size) lineTo(mapX(sunXValues[i]), mapY(drawSunYValues[i])); lineTo(mapX(sunXValues.last()), zeroYPixel); close() }

            moonCurvePath = Path().apply { moveTo(mapX(moonXValues[0]), mapY(drawMoonYValues[0])); for (i in 1 until moonXValues.size) lineTo(mapX(moonXValues[i]), mapY(drawMoonYValues[i])) }
            moonFillPath = Path().apply { moveTo(mapX(moonXValues[0]), zeroYPixel); lineTo(mapX(moonXValues[0]), mapY(drawMoonYValues[0])); for (i in 1 until moonXValues.size) lineTo(mapX(moonXValues[i]), mapY(drawMoonYValues[i])); lineTo(mapX(moonXValues.last()), zeroYPixel); close() }
        }

        // --- 1. BACKGROUNDS (Dictated by Sun) ---
        drawDayNightBackground(colorScheme, params, zeroYPixel)
        drawDayNightAreaFill(sunFillPath, colorScheme, zeroYPixel)
        if (isTrajectory) {
//            drawDayNightHorizontalTwilights(moonFillPath, colors, params, zeroYPixel, ::mapY, Charts.Moon.Daily.Trajectory)
            drawDayNightHorizontalTwilights(sunFillPath, colors, params, zeroYPixel, ::mapY, Charts.Sun.Daily.Trajectory, shouldTransform)
        } else {
            // Time-Based Vertical Twilights
            val thresholds = floatArrayOf(0f, -6f, -12f, -18f)
            val sortedXPoints = mutableListOf<Float>()
            for (x in sunXValues) sortedXPoints.add(x)

            // Calculate exact mathematical crossings for the Sun's altitude
            for (i in 0 until sunXValues.size - 1) {
                val x1 = sunXValues[i]
                val x2 = sunXValues[i + 1]
                val y1 = sunYValues[i]
                val y2 = sunYValues[i + 1]

                for (th in thresholds) {
                    if ((y1 < th && y2 > th) || (y1 > th && y2 < th)) {
                        val fraction = (th - y1) / (y2 - y1)
                        sortedXPoints.add(x1 + fraction * (x2 - x1))
                    }
                }
            }
            sortedXPoints.sort()
            val uniqueXPoints = sortedXPoints.distinct()

            clipPath(sunFillPath) {
                // Base Day Fill
                clipRect(bottom = zeroYPixel) {
                    drawRect(color = colors.dayBackground, topLeft = Offset(0f, 0f), size = Size(params.width, zeroYPixel))
                }

                // Draw Vertical Twilights (Pass Charts.Sun.Daily.Elevation to clear any internal type-checks!)
                drawNightVerticalTwilights(colors, params, uniqueXPoints, ::mapX, zeroYPixel, Charts.Sun.Daily.Elevation, shouldTransform)

                clipRect(right = currentSunXPx) {
                    clipRect(bottom = zeroYPixel) {
                        // Yellow Sun elapsed fill above the horizon
                        drawPath(path = sunFillPath, color = colors.elapsedDay)
                    }
                    clipRect(top = zeroYPixel) {
                        // Dimmer Sun elapsed fill below the horizon
                        drawPath(path = sunFillPath, color = colors.elapsedNight)
                    }
                }
            }

            clipPath(moonFillPath) {
                clipRect(right = currentMoonXPx) {
                    clipRect(bottom = zeroYPixel) {
                        // Using a 35% moon color alpha to mix nicely with the daylight background
                        drawPath(path = moonFillPath, color = colors.elapsedNight)
                    }
                    clipRect(top = zeroYPixel) {
                        // Very subtle 15% alpha below the horizon
                        drawPath(path = moonFillPath, color = colors.elapsedNight)
                    }
                }
            }
        }

        // --- 2. MOON LAYER (Drawn First so it sits behind the Sun) ---
        // Draw Moon Line (Using a slightly dimmer/different color to distinguish from Sun)
        drawCurvePath(moonCurvePath, materialTheme) // Or pass a specific Moon line color
        if (isTrajectory) {
            drawElapsedPath(moonElapsedPath!!, localCustomColors, Charts.Moon.Daily.Trajectory) // Make sure this uses a moon-specific color if desired
        } else {
            drawElapsedTimePath(moonCurvePath, localCustomColors, Charts.Moon.Daily.Elevation, currentMoonXPx)
        }

        // --- 3. SUN LAYER (Drawn on Top) ---
        drawCurvePath(sunCurvePath, materialTheme)
        if (isTrajectory) {
            drawElapsedPath(sunElapsedPath!!, localCustomColors, Charts.Sun.Daily.Trajectory)
        } else {
            drawElapsedTimePath(sunCurvePath, localCustomColors, Charts.Sun.Daily.Elevation, currentSunXPx)
        }

        // --- 4. GRIDS AND LABELS ---
        drawHorizonLine(materialTheme, params, zeroYPixel)
        drawYLabels(chartType, materialTheme, params, ::mapY, textMeasurer, labelStyle, shouldTransform)
        drawXLabels(chartType, materialTheme, params, ::mapX, textMeasurer, labelStyle, context, if (isTrajectory) shiftTrajectory else false)

        // --- 5. ICONS & DROP LINES ---
        // Draw Moon Icon First (Bottom layer)
        drawVerticalDropLine(localCustomColors, currentMoonXPx, currentMoonYPx, zeroYPixel)
        paintIcon(currentMoonXPx, currentMoonAltitude, currentMoonYPx, zeroYPixel, moonChartType, coordinates, drawMoonIcon)

        // Draw Sun Icon Last (Top layer)
        drawVerticalDropLine(localCustomColors, currentSunXPx, currentSunYPx, zeroYPixel)
        paintIcon(currentSunXPx, currentSunAltitude, currentSunYPx, zeroYPixel, sunChartType, coordinates, drawSunIcon)
    }
}