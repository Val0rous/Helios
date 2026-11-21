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
import com.ephemeris.helios.utils.charts.IS_APPARENT_ELEVATION_DEFAULT
import com.ephemeris.helios.utils.charts.buildDynamicPath
import com.ephemeris.helios.utils.charts.buildNightVerticalTwilightBands
import com.ephemeris.helios.utils.charts.buildUVSliceBands
import com.ephemeris.helios.utils.charts.createHorizontalBrush
import com.ephemeris.helios.utils.charts.drawCurvePath
import com.ephemeris.helios.utils.charts.drawDayNightAreaFill
import com.ephemeris.helios.utils.charts.drawDayNightBackground
import com.ephemeris.helios.utils.charts.drawDayNightHorizontalTwilights
import com.ephemeris.helios.utils.charts.drawElapsedPath
import com.ephemeris.helios.utils.charts.drawElapsedTimePath
import com.ephemeris.helios.utils.charts.drawHorizonLine
import com.ephemeris.helios.utils.charts.drawNightVerticalTwilights
import com.ephemeris.helios.utils.charts.drawPreparedXLabels
import com.ephemeris.helios.utils.charts.drawPreparedYLabels
import com.ephemeris.helios.utils.charts.drawUVSlices
import com.ephemeris.helios.utils.charts.drawVerticalDropLine
import com.ephemeris.helios.utils.charts.getColorTemperatureBrushGradient
import com.ephemeris.helios.utils.charts.getMapX
import com.ephemeris.helios.utils.charts.getMapY
import com.ephemeris.helios.utils.charts.getMaxX
import com.ephemeris.helios.utils.charts.getMaxY
import com.ephemeris.helios.utils.charts.getMinX
import com.ephemeris.helios.utils.charts.getMinY
import com.ephemeris.helios.utils.charts.getZeroYPixel
import com.ephemeris.helios.utils.charts.interpolateYAtHour
import com.ephemeris.helios.utils.charts.prepareXLabels
import com.ephemeris.helios.utils.charts.prepareYLabels
import com.ephemeris.helios.utils.charts.transformApparentElevation
import com.ephemeris.helios.utils.location.Coordinates

private class DailyChartStaticData(
    val isTrajectory: Boolean,
    val shouldTransform: Boolean,
    val shiftTrajectory: Boolean,
    val drawXValues: FloatArray,
    val drawYValues: FloatArray,
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
)

@Composable
fun DailyChart(
    xValues: FloatArray,
    yValues: FloatArray,
    chartType: Charts,
    currentHour: Float,
    currentAzimuth: Float,
    currentAltitude: Float,
    coordinates: Coordinates?,
    modifier: Modifier = Modifier,
    isApparentElevation: Boolean = IS_APPARENT_ELEVATION_DEFAULT,
    liveHour: Float = currentHour,
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

    val context = LocalContext.current
    // Text Measurer and styling for the legends
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)
    val labelStyle = remember(colorScheme.onSurfaceVariant) {
        TextStyle(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontSize = (9.5).sp,
            fontFamily = FontFamily.Monospace
        )
    }

    // 1. Cache day-level array transforms, bounds, and trajectory shift
    val staticData = remember(xValues, yValues, chartType, isApparentElevation) {
        if (xValues.isEmpty() || yValues.isEmpty()) return@remember null

        val className = chartType.javaClass.simpleName
        val isTrajectory = className.contains("Trajectory")
        val isElevationOrTrajectory = className.contains("Elevation") || isTrajectory
        val shouldTransform = isApparentElevation && isElevationOrTrajectory

        // Transformed arrays used solely for rendering
        val drawYValues = if (shouldTransform) transformApparentElevation(yValues) else yValues

        // Dynamic Trajectory Shifting: if the body culminates North (near 0 or 360), shift by 180°
        val shiftTrajectory = if (isTrajectory) {
            val peakIndex = yValues.indices.maxByOrNull { yValues[it] } ?: 0
            xValues[peakIndex] !in 90f..270f
        } else {
            false
        }

        // Pure shift mapping keeps North(0) at the center, preserving Right-To-Left motion
        val drawXValues = if (isTrajectory) {
            FloatArray(xValues.size) { i ->
                if (shiftTrajectory) (xValues[i] + 180f) % 360f else xValues[i]
            }
        } else {
            xValues
        }

        DailyChartStaticData(
            isTrajectory = isTrajectory,
            shouldTransform = shouldTransform,
            shiftTrajectory = shiftTrajectory,
            drawXValues = drawXValues,
            drawYValues = drawYValues,
            minX = getMinX(xValues, chartType),
            maxX = getMaxX(xValues, chartType),
            minY = getMinY(yValues, chartType),
            maxY = getMaxY(yValues, chartType),
        )
    }

    // 2. Keep per-frame scrubbing values in stable State holders so onDrawBehind redraws
    //    without ever invalidating the cached paths, bands, labels, & gradient brushes in drawWithCache
    val currentHourState = rememberUpdatedState(currentHour)
    val liveHourState = rememberUpdatedState(liveHour)
    val currentAzimuthState = rememberUpdatedState(currentAzimuth)
    val currentAltitudeState = rememberUpdatedState(currentAltitude)

    // 3. Cache size-dependent Paths, bands, labels & gradient Brushes inside Modifier.drawWithCache
    val drawModifier = remember(
        staticData,
        xValues,
        yValues,
        chartType,
        coordinates,
        colors,
        colorScheme,
        dayFill,
        elapsedDayFill,
        elapsedNightFill,
        labelStyle,
        drawChartIcon,
        textMeasurer,
        context
    ) {
        Modifier.drawWithCache {
            if (staticData == null) return@drawWithCache onDrawBehind { }

            val params = ChartData(
                xValues = xValues,
                yValues = staticData.drawYValues,
                minX = staticData.minX,
                maxX = staticData.maxX,
                minY = staticData.minY,
                maxY = staticData.maxY,
                width = size.width,
                height = size.height,
                verticalPaddingPx = 16.dp.toPx()
            )

            fun mapX(x: Float) = getMapX(x, params)
            fun mapY(y: Float) = getMapY(y, params, chartType)
            val zeroYPixel = getZeroYPixel(chartType, ::mapY, coordinates, staticData.shouldTransform)

            fun buildDynPath(start: Int, end: Int, isFill: Boolean) =
                buildDynamicPath(start, end, isFill, staticData.drawXValues, staticData.drawYValues, zeroYPixel, ::mapX, ::mapY)

            val primaryCurvePath: Path
            val primaryFillPath: Path

            if (staticData.isTrajectory) {
                primaryCurvePath = buildDynPath(0, staticData.drawXValues.size - 1, false)
                primaryFillPath = buildDynPath(0, staticData.drawXValues.size - 1, true)
            } else {
                // Only draw the continuous curve when values are valid (skipping NaNs)
                primaryCurvePath = Path().apply {
                    var isFirst = true
                    for (i in xValues.indices) {
                        val y = staticData.drawYValues[i]
                        if (!y.isNaN()) {
                            if (isFirst) {
                                moveTo(mapX(xValues[i]), mapY(y))
                                isFirst = false
                            } else {
                                lineTo(mapX(xValues[i]), mapY(y))
                            }
                        }
                    }
                }
                // Seal the fill area safely around the valid day-band
                primaryFillPath = Path().apply {
                    var firstValidIndex = -1
                    var lastValidIndex = -1
                    for (i in xValues.indices) {
                        if (!staticData.drawYValues[i].isNaN()) {
                            if (firstValidIndex == -1) firstValidIndex = i
                            lastValidIndex = i
                        }
                    }
                    if (firstValidIndex != -1) {
                        moveTo(mapX(xValues[firstValidIndex]), zeroYPixel)
                        lineTo(mapX(xValues[firstValidIndex]), mapY(staticData.drawYValues[firstValidIndex]))
                        for (i in firstValidIndex + 1..lastValidIndex) {
                            if (!staticData.drawYValues[i].isNaN()) {
                                lineTo(mapX(xValues[i]), mapY(staticData.drawYValues[i]))
                            }
                        }
                        lineTo(mapX(xValues[lastValidIndex]), zeroYPixel)
                        close()
                    }
                }
            }

            // Precompute vertical twilight & UV bands once per day/size in O(N)
            val nightTwilightBands = if (staticData.isTrajectory) {
                emptyList()
            } else {
                buildNightVerticalTwilightBands(
                    colors = colors,
                    params = params,
                    mapX = ::mapX,
                    chartType = chartType,
                    isApparentElevation = staticData.shouldTransform,
                )
            }
            val uvSliceBands = if (!staticData.isTrajectory && chartType == Charts.Sun.Daily.UvIntensity) {
                buildUVSliceBands(params = params, mapX = ::mapX, colors = colors)
            } else {
                emptyList()
            }

            // Pre-measure axis labels & gridlines once per day/size/theme
            val preparedYLabels = prepareYLabels(
                chartType = chartType,
                materialTheme = colorScheme,
                params = params,
                mapY = ::mapY,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                isApparentElevation = staticData.shouldTransform,
            )
            val preparedXLabels = prepareXLabels(
                chartType = chartType,
                materialTheme = colorScheme,
                params = params,
                mapX = ::mapX,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                context = context,
                isShiftTrajectory = if (staticData.isTrajectory) staticData.shiftTrajectory else false,
            )

            // Pre-build static gradient brushes once per data/size/theme change
            val dayBrush: Brush? = if (staticData.isTrajectory) {
                null
            } else {
                when (chartType) {
                    Charts.Sun.Daily.ColorTemperature -> {
                        getColorTemperatureBrushGradient(false, ::mapY, params, colors)
                    }

                    Charts.Sun.Daily.AirMass -> {
                        createHorizontalBrush({ i, value ->
                            val isNight = value == 0f && (
                                i == 0 ||
                                i == params.xValues.lastIndex ||
                                params.yValues.getOrNull(i - 5) == 0f ||
                                params.yValues.getOrNull(i + 5) == 0f
                            )
                            if (isNight) {
                                colors.amHorizon
                            } else {
                                val fraction = ((value.coerceIn(1f, 10f) - 1f) / 9f)
                                lerp(colors.amZenith, colors.amHorizon, fraction)
                            }
                        }, params)
                    }

                    Charts.Sun.Daily.Shadows -> {
                        createHorizontalBrush({ i, value ->
                            val isNight = value == 0f && (
                                i == 0 ||
                                i == params.xValues.lastIndex ||
                                params.yValues.getOrNull(i - 5) == 0f ||
                                params.yValues.getOrNull(i + 5) == 0f
                            )
                            if (isNight) {
                                colors.shadowLong
                            } else {
                                val fraction = (value.coerceIn(0f, 10f) / 10f)
                                lerp(colors.shadowShort, colors.shadowLong, fraction)
                            }
                        }, params)
                    }

                    Charts.Sun.Daily.Illuminance -> {
                        val absoluteMaxLux = 100000f
                        Brush.verticalGradient(
                            0.0f to colors.luxBright,
                            1.0f to colors.luxDim,
                            startY = mapY(absoluteMaxLux),
                            endY = zeroYPixel
                        )
                    }

                    Charts.Sun.Daily.Irradiance -> {
                        createHorizontalBrush({ _, value ->
                            val absoluteMaxIrr = 950f
                            val fraction = (value / absoluteMaxIrr).coerceIn(0f, 1f)
                            when {
                                fraction <= 0.5f -> lerp(colors.irrLow, colors.irrMid, fraction * 2f)
                                else -> lerp(colors.irrMid, colors.irrHigh, (fraction - 0.5f) * 2f)
                            }
                        }, params)
                    }

                    else -> null
                }
            }

            // Cache trajectory elapsed path per minute index
            var cachedBestIndex = -1
            var cachedElapsedLinePath: Path? = null

            onDrawBehind {
                val hour = currentHourState.value
                val liveHr = liveHourState.value

                val drawCurrentX: Float
                val rawCurrentY: Float
                val drawCurrentY: Float

                if (staticData.isTrajectory) {
                    val azimuth = currentAzimuthState.value
                    val altitude = currentAltitudeState.value

                    drawCurrentX = if (staticData.shiftTrajectory) (azimuth + 180f) % 360f else azimuth
                    rawCurrentY = altitude
                    drawCurrentY = if (staticData.shouldTransform && !altitude.isNaN()) {
                        transformApparentElevation(altitude)
                    } else {
                        altitude
                    }

                    // Use liveHr so elapsed trajectory line stays locked to liveData's azimuth/altitude
                    val bestIndex = ((liveHr / 24f) * (staticData.drawXValues.size - 1))
                        .toInt()
                        .coerceIn(0, staticData.drawXValues.size - 1)
                    if (bestIndex != cachedBestIndex) {
                        cachedBestIndex = bestIndex
                        cachedElapsedLinePath = buildDynPath(0, bestIndex, false)
                    }
                } else {
                    // O(1) curve interpolation keeps the icon locked to the rendered curve
                    drawCurrentX = hour
                    rawCurrentY = interpolateYAtHour(yValues, hour)
                    drawCurrentY = interpolateYAtHour(staticData.drawYValues, hour)
                }

                val currentXPx = mapX(drawCurrentX)
                // If it's night (NaN), park the Y-pixel firmly on the baseline
                val currentYPx = if (drawCurrentY.isNaN()) zeroYPixel else mapY(drawCurrentY)

                // Day & Night Background
                drawDayNightBackground(colorScheme, params, zeroYPixel)
                drawDayNightAreaFill(primaryFillPath, colorScheme, zeroYPixel)

                if (staticData.isTrajectory) {
                    drawDayNightHorizontalTwilights(
                        primaryFillPath,
                        colors,
                        params,
                        zeroYPixel,
                        ::mapY,
                        chartType,
                        staticData.shouldTransform
                    )
                } else {
                    clipPath(primaryFillPath) {
                        clipRect(bottom = zeroYPixel) {
                            when {
                                chartType == Charts.Sun.Daily.UvIntensity -> drawUVSlices(
                                    bands = uvSliceBands,
                                    zeroYPixel = zeroYPixel,
                                )

                                dayBrush != null -> drawRect(
                                    brush = dayBrush,
                                    topLeft = Offset(0f, 0f),
                                    size = Size(params.width, zeroYPixel)
                                )

                                else -> drawRect(
                                    color = dayFill,
                                    topLeft = Offset(0f, 0f),
                                    size = Size(params.width, zeroYPixel)
                                )
                            }
                        }

                        drawNightVerticalTwilights(
                            bands = nightTwilightBands,
                            zeroYPixel = zeroYPixel,
                            canvasHeight = params.height,
                        )

                        // Elapsed time overlay (clipped strictly up to currentHour)
                        clipRect(right = currentXPx) {
                            clipRect(bottom = zeroYPixel) {
                                drawPath(path = primaryFillPath, color = elapsedDayFill)
                            }
                            clipRect(top = zeroYPixel) {
                                drawPath(path = primaryFillPath, color = elapsedNightFill)
                            }
                        }
                    }
                }

                // Full unclipped curve line
                drawCurvePath(primaryCurvePath, colorScheme)

                if (staticData.isTrajectory) {
                    drawElapsedPath(cachedElapsedLinePath!!, colors, chartType)
                } else {
                    drawElapsedTimePath(primaryCurvePath, colors, chartType, currentXPx)
                }

                // Horizon line & pre-measured legends
                drawHorizonLine(colorScheme, params, zeroYPixel)
                drawPreparedYLabels(preparedYLabels, params.width)
                drawPreparedXLabels(preparedXLabels, params.height)

                // Vertical drop line & icon (using interpolated rawCurrentY so horizon/metric thresholds stay in sync)
                drawVerticalDropLine(colors, currentXPx, currentYPx, zeroYPixel)
                paintIcon(currentXPx, rawCurrentY, currentYPx, zeroYPixel, chartType, coordinates, drawChartIcon)
            }
        }
    }

    Spacer(modifier = modifier.then(drawModifier))
}
