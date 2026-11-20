package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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

private class ComboChartStaticData(
    val isTrajectory: Boolean,
    val shouldTransform: Boolean,
    val shiftTrajectory: Boolean,
    val drawSunX: FloatArray,
    val drawSunY: FloatArray,
    val drawMoonX: FloatArray,
    val drawMoonY: FloatArray,
    val minX: Float,
    val maxX: Float,
    val uniqueXPoints: List<Float>,
)

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
    isApparentElevation: Boolean = IS_APPARENT_ELEVATION_DEFAULT,
    liveHour: Float = currentHour,
) {
    val sunChartType = Charts.Sun.Daily.Elevation
    val moonChartType = Charts.Moon.Daily.Elevation

    // 1. Independent Icon Drawers
    val drawSunIcon = rememberChartIconDrawer(sunChartType)
    val drawMoonIcon = rememberChartIconDrawer(moonChartType)

    val colors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = remember(colorScheme.onSurfaceVariant) {
        TextStyle(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontSize = (9.5).sp,
            fontFamily = FontFamily.Monospace
        )
    }

    // 2. Cache day-level array transforms & twilight crossing points (recomputed only when date/chart/mode changes)
    val staticData = remember(
        sunXValues,
        sunYValues,
        moonXValues,
        moonYValues,
        chartType,
        isApparentElevation
    ) {
        if (sunXValues.isEmpty() || sunYValues.isEmpty() || moonXValues.isEmpty() || moonYValues.isEmpty()) {
            return@remember null
        }

        val className = chartType.javaClass.simpleName
        val isTrajectory = className.contains("Trajectory")
        val isElevationOrTrajectory = className.contains("Elevation") || isTrajectory
        val shouldTransform = isApparentElevation && isElevationOrTrajectory

        val drawSunY = if (shouldTransform) transformApparentElevation(sunYValues) else sunYValues
        val drawMoonY = if (shouldTransform) transformApparentElevation(moonYValues) else moonYValues

        // Trajectory shift logic dictated by Sun so both bodies share the same compass coordinates
        val shiftTrajectory = if (isTrajectory) {
            val sunPeakIndex = sunYValues.indices.maxByOrNull { sunYValues[it] } ?: 0
            sunXValues[sunPeakIndex] !in 90f..270f
        } else {
            false
        }

        val drawSunX = if (isTrajectory) {
            FloatArray(sunXValues.size) { i ->
                if (shiftTrajectory) (sunXValues[i] + 180f) % 360f else sunXValues[i]
            }
        } else {
            sunXValues
        }

        val drawMoonX = if (isTrajectory) {
            FloatArray(moonXValues.size) { i ->
                if (shiftTrajectory) (moonXValues[i] + 180f) % 360f else moonXValues[i]
            }
        } else {
            moonXValues
        }

        val uniqueXPoints = if (isTrajectory) {
            emptyList()
        } else {
            val thresholds = floatArrayOf(0f, -6f, -12f, -18f)
            val sortedXPoints = ArrayList<Float>(sunXValues.size + 16)
            for (x in sunXValues) sortedXPoints.add(x)

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
            sortedXPoints.distinct()
        }

        ComboChartStaticData(
            isTrajectory = isTrajectory,
            shouldTransform = shouldTransform,
            shiftTrajectory = shiftTrajectory,
            drawSunX = drawSunX,
            drawSunY = drawSunY,
            drawMoonX = drawMoonX,
            drawMoonY = drawMoonY,
            minX = getMinX(sunXValues, chartType),
            maxX = getMaxX(sunXValues, chartType),
            uniqueXPoints = uniqueXPoints
        )
    }

    // 3. Keep per-frame scrubbing values in stable State holders so reading them inside onDrawBehind
    //    only triggers a lightweight redraw without invalidating drawWithCache!
    val currentHourState = rememberUpdatedState(currentHour)
    val liveHourState = rememberUpdatedState(liveHour)
    val currentSunAzimuthState = rememberUpdatedState(currentSunAzimuth)
    val currentSunAltitudeState = rememberUpdatedState(currentSunAltitude)
    val currentMoonAzimuthState = rememberUpdatedState(currentMoonAzimuth)
    val currentMoonAltitudeState = rememberUpdatedState(currentMoonAltitude)

    // 4. Cache size-dependent geometry (Path objects & coordinate mappers) in Modifier.drawWithCache
    val drawModifier = remember(
        staticData,
        sunXValues,
        sunYValues,
        moonXValues,
        moonYValues,
        chartType,
        coordinates,
        colors,
        colorScheme,
        labelStyle,
        drawSunIcon,
        drawMoonIcon,
        textMeasurer,
        context
    ) {
        Modifier.drawWithCache {
            if (staticData == null) return@drawWithCache onDrawBehind { }

            val params = ChartData(
                xValues = sunXValues,
                yValues = staticData.drawSunY,
                minX = staticData.minX,
                maxX = staticData.maxX,
                minY = -90f, // Combo charts always span the full celestial sphere
                maxY = 90f,
                width = size.width,
                height = size.height,
                verticalPaddingPx = 16.dp.toPx()
            )

            fun mapX(x: Float) = getMapX(x, params)
            fun mapY(y: Float) = getMapY(y, params, chartType)
            val zeroYPixel = getZeroYPixel(chartType, ::mapY, coordinates, staticData.shouldTransform)

            fun buildDynPath(start: Int, end: Int, isFill: Boolean, xVals: FloatArray, yVals: FloatArray) =
                buildDynamicPath(start, end, isFill, xVals, yVals, zeroYPixel, ::mapX, ::mapY)

            val sunCurvePath: Path
            val sunFillPath: Path
            val moonCurvePath: Path
            val moonFillPath: Path

            if (staticData.isTrajectory) {
                sunCurvePath = buildDynPath(0, staticData.drawSunX.size - 1, false, staticData.drawSunX, staticData.drawSunY)
                sunFillPath = buildDynPath(0, staticData.drawSunX.size - 1, true, staticData.drawSunX, staticData.drawSunY)

                moonCurvePath = buildDynPath(0, staticData.drawMoonX.size - 1, false, staticData.drawMoonX, staticData.drawMoonY)
                moonFillPath = buildDynPath(0, staticData.drawMoonX.size - 1, true, staticData.drawMoonX, staticData.drawMoonY)
            } else {
                sunCurvePath = Path().apply {
                    moveTo(mapX(sunXValues[0]), mapY(staticData.drawSunY[0]))
                    for (i in 1 until sunXValues.size) lineTo(mapX(sunXValues[i]), mapY(staticData.drawSunY[i]))
                }
                sunFillPath = Path().apply {
                    moveTo(mapX(sunXValues[0]), zeroYPixel)
                    lineTo(mapX(sunXValues[0]), mapY(staticData.drawSunY[0]))
                    for (i in 1 until sunXValues.size) lineTo(mapX(sunXValues[i]), mapY(staticData.drawSunY[i]))
                    lineTo(mapX(sunXValues.last()), zeroYPixel)
                    close()
                }

                moonCurvePath = Path().apply {
                    moveTo(mapX(moonXValues[0]), mapY(staticData.drawMoonY[0]))
                    for (i in 1 until moonXValues.size) lineTo(mapX(moonXValues[i]), mapY(staticData.drawMoonY[i]))
                }
                moonFillPath = Path().apply {
                    moveTo(mapX(moonXValues[0]), zeroYPixel)
                    lineTo(mapX(moonXValues[0]), mapY(staticData.drawMoonY[0]))
                    for (i in 1 until moonXValues.size) lineTo(mapX(moonXValues[i]), mapY(staticData.drawMoonY[i]))
                    lineTo(mapX(moonXValues.last()), zeroYPixel)
                    close()
                }
            }

            // Cache trajectory elapsed paths per minute index to avoid reallocating on sub-minute redraws
            var cachedBestIndex = -1
            var cachedSunElapsedPath: Path? = null
            var cachedMoonElapsedPath: Path? = null

            onDrawBehind {
                val hour = currentHourState.value
                val liveHr = liveHourState.value

                val drawCurrentSunX: Float
                val drawCurrentMoonX: Float
                val rawSunAlt: Float
                val rawMoonAlt: Float
                val drawSunAlt: Float
                val drawMoonAlt: Float

                if (staticData.isTrajectory) {
                    val sunAz = currentSunAzimuthState.value
                    val sunAlt = currentSunAltitudeState.value
                    val moonAz = currentMoonAzimuthState.value
                    val moonAlt = currentMoonAltitudeState.value

                    drawCurrentSunX = if (staticData.shiftTrajectory) (sunAz + 180f) % 360f else sunAz
                    drawCurrentMoonX = if (staticData.shiftTrajectory) (moonAz + 180f) % 360f else moonAz
                    rawSunAlt = sunAlt
                    rawMoonAlt = moonAlt
                    drawSunAlt = if (staticData.shouldTransform) transformApparentElevation(sunAlt) else sunAlt
                    drawMoonAlt = if (staticData.shouldTransform) transformApparentElevation(moonAlt) else moonAlt

                    // Use liveHr so elapsed trajectory lines stay locked to liveData's azimuth/altitude
                    val bestIndex = ((liveHr / 24f) * (staticData.drawSunX.size - 1))
                        .toInt()
                        .coerceIn(0, staticData.drawSunX.size - 1)
                    if (bestIndex != cachedBestIndex) {
                        cachedBestIndex = bestIndex
                        cachedSunElapsedPath = buildDynPath(0, bestIndex, false, staticData.drawSunX, staticData.drawSunY)
                        cachedMoonElapsedPath = buildDynPath(0, bestIndex, false, staticData.drawMoonX, staticData.drawMoonY)
                    }
                } else {
                    // Time-based Elevation: interpolate Y directly from the plotted curve in O(1)
                    // so icons stay glued to the curve at 60/120fps with zero async lag
                    drawCurrentSunX = hour
                    drawCurrentMoonX = hour
                    rawSunAlt = interpolateYAtHour(sunYValues, hour)
                    rawMoonAlt = interpolateYAtHour(moonYValues, hour)
                    drawSunAlt = interpolateYAtHour(staticData.drawSunY, hour)
                    drawMoonAlt = interpolateYAtHour(staticData.drawMoonY, hour)
                }

                val currentSunXPx = mapX(drawCurrentSunX)
                val currentMoonXPx = mapX(drawCurrentMoonX)
                val currentSunYPx = if (drawSunAlt.isNaN()) zeroYPixel else mapY(drawSunAlt)
                val currentMoonYPx = if (drawMoonAlt.isNaN()) zeroYPixel else mapY(drawMoonAlt)

                // --- 1. BACKGROUNDS (Dictated by Sun) ---
                drawDayNightBackground(colorScheme, params, zeroYPixel)
                drawDayNightAreaFill(sunFillPath, colorScheme, zeroYPixel)
                if (staticData.isTrajectory) {
                    drawDayNightHorizontalTwilights(
                        sunFillPath,
                        colors,
                        params,
                        zeroYPixel,
                        ::mapY,
                        Charts.Sun.Daily.Trajectory,
                        staticData.shouldTransform
                    )
                } else {
                    clipPath(sunFillPath) {
                        // Base Day Fill
                        clipRect(bottom = zeroYPixel) {
                            drawRect(color = colors.dayBackground, topLeft = Offset(0f, 0f), size = Size(params.width, zeroYPixel))
                        }

                        // Draw Vertical Twilights (Pass Charts.Sun.Daily.Elevation to clear any internal type-checks!)
                        drawNightVerticalTwilights(
                            colors,
                            params,
                            staticData.uniqueXPoints,
                            ::mapX,
                            zeroYPixel,
                            Charts.Sun.Daily.Elevation,
                            staticData.shouldTransform
                        )

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
                                drawPath(path = moonFillPath, color = colors.elapsedNight)
                            }
                            clipRect(top = zeroYPixel) {
                                drawPath(path = moonFillPath, color = colors.elapsedNight)
                            }
                        }
                    }
                }

                // --- 2. MOON LAYER (Drawn First so it sits behind the Sun) ---
                drawCurvePath(moonCurvePath, colorScheme)
                if (staticData.isTrajectory) {
                    drawElapsedPath(cachedMoonElapsedPath!!, colors, Charts.Moon.Daily.Trajectory)
                } else {
                    drawElapsedTimePath(moonCurvePath, colors, Charts.Moon.Daily.Elevation, currentMoonXPx)
                }

                // --- 3. SUN LAYER (Drawn on Top) ---
                drawCurvePath(sunCurvePath, colorScheme)
                if (staticData.isTrajectory) {
                    drawElapsedPath(cachedSunElapsedPath!!, colors, Charts.Sun.Daily.Trajectory)
                } else {
                    drawElapsedTimePath(sunCurvePath, colors, Charts.Sun.Daily.Elevation, currentSunXPx)
                }

                // --- 4. GRIDS AND LABELS ---
                drawHorizonLine(colorScheme, params, zeroYPixel)
                drawYLabels(chartType, colorScheme, params, ::mapY, textMeasurer, labelStyle, staticData.shouldTransform)
                drawXLabels(
                    chartType,
                    colorScheme,
                    params,
                    ::mapX,
                    textMeasurer,
                    labelStyle,
                    context,
                    if (staticData.isTrajectory) staticData.shiftTrajectory else false
                )

                // --- 5. ICONS & DROP LINES ---
                // Draw Moon Icon First (Bottom layer)
                drawVerticalDropLine(colors, currentMoonXPx, currentMoonYPx, zeroYPixel)
                paintIcon(currentMoonXPx, rawMoonAlt, currentMoonYPx, zeroYPixel, moonChartType, coordinates, drawMoonIcon)

                // Draw Sun Icon Last (Top layer)
                drawVerticalDropLine(colors, currentSunXPx, currentSunYPx, zeroYPixel)
                paintIcon(currentSunXPx, rawSunAlt, currentSunYPx, zeroYPixel, sunChartType, coordinates, drawSunIcon)
            }
        }
    }

    Spacer(modifier = modifier.then(drawModifier))
}
