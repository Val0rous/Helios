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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.charts.ChartData
import com.ephemeris.helios.utils.charts.HorizontalColorBand
import com.ephemeris.helios.utils.charts.buildHorizontalColorBands
import com.ephemeris.helios.utils.charts.buildUVSliceBands
import com.ephemeris.helios.utils.charts.createHorizontalBrush
import com.ephemeris.helios.utils.location.Coordinates
import kotlin.math.ln

/**
 * 1D horizontal timeline strip rendered in the collapsed Sun/Moon header.
 *
 * Keeps the exact vertical color bands for night, astronomical/nautical/civil twilight, and
 * daylight (or active metric fill), plus elapsed shading and a crisp vertical current-time marker,
 * omitting the 2D sine curve, horizon line, axis labels, and Sun/Moon icon for high legibility
 * at compact height.
 */
@Composable
fun CompactDailyRibbon(
    timeXValues: FloatArray,
    elevationYValues: FloatArray,
    activeYValues: FloatArray,
    chartType: Charts,
    currentHour: Float,
    coordinates: Coordinates?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme
    val fills = colors.fills
    val isSun = chartType is Charts.Sun
    val elapsedDayFill = when (chartType) {
        Charts.Sun.Daily.Elevation -> fills.elapsedDay
        else -> fills.elapsedOther
    }
    val elapsedNightFill = fills.elapsedNight
    val markerColor = if (isSun) colors.sunPath else colors.moonPath

    val currentHourState = rememberUpdatedState(currentHour)

    val drawModifier = remember(
        timeXValues,
        elevationYValues,
        activeYValues,
        chartType,
        coordinates,
        colors,
        colorScheme,
        elapsedDayFill,
        elapsedNightFill,
        markerColor,
    ) {
        Modifier.drawWithCache {
            if (timeXValues.isEmpty() || elevationYValues.isEmpty() || size.width <= 0f) {
                return@drawWithCache onDrawBehind { }
            }

            val width = size.width
            val minX = 0f
            val maxX = 24f
            fun mapX(hour: Float): Float = ((hour - minX) / (maxX - minX)).coerceIn(0f, 1f) * width

            val horizonAlt = when {
                isSun -> (coordinates?.sunApparentHorizonAlt?.toFloat() ?: 0f).coerceAtLeast(-5.5f)
                else -> coordinates?.moonApparentHorizonAlt?.toFloat() ?: 0f
            }

            // Below-horizon vertical bands (Sun has night + astro/nautical/civil twilight; Moon stays surfaceVariant)
            val belowHorizonBands: List<HorizontalColorBand> = if (isSun) {
                buildHorizontalColorBands(
                    xValues = timeXValues,
                    yValues = elevationYValues,
                    sortedThresholdsAsc = floatArrayOf(-18f, -12f, -6f, horizonAlt),
                    mapX = ::mapX,
                ) { y ->
                    when {
                        y >= horizonAlt -> Color.Transparent
                        y >= -6f -> fills.civilTwilight
                        y >= -12f -> fills.nauticalTwilight
                        y >= -18f -> fills.astronomicalTwilight
                        else -> fills.nightBackground
                    }
                }
            } else {
                emptyList()
            }

            // Above-horizon intervals (daylight for Sun, above-horizon arc for Moon)
            val aboveHorizonBands: List<HorizontalColorBand> = buildHorizontalColorBands(
                xValues = timeXValues,
                yValues = elevationYValues,
                sortedThresholdsAsc = floatArrayOf(horizonAlt),
                mapX = ::mapX,
            ) { y ->
                if (y >= horizonAlt) Color.White else Color.Transparent
            }

            val metricParams = ChartData(
                xValues = timeXValues,
                yValues = activeYValues,
                minX = minX,
                maxX = maxX,
                minY = 0f,
                maxY = 1f,
                width = width,
                height = size.height.coerceAtLeast(1f),
                verticalPaddingPx = 0f,
            )

            val uvSliceBands: List<HorizontalColorBand> =
                if (chartType == Charts.Sun.Daily.UvIntensity && activeYValues.isNotEmpty()) {
                    buildUVSliceBands(params = metricParams, mapX = ::mapX, colors = colors)
                } else {
                    emptyList()
                }

            val metricBrush: Brush? = if (activeYValues.isEmpty()) {
                null
            } else {
                when (chartType) {
                    Charts.Sun.Daily.Irradiance -> createHorizontalBrush({ _, value ->
                        val fraction = (value / 950f).coerceIn(0f, 1f)
                        if (fraction <= 0.5f) {
                            lerp(fills.irrLow, fills.irrMid, fraction * 2f)
                        } else {
                            lerp(fills.irrMid, fills.irrHigh, (fraction - 0.5f) * 2f)
                        }
                    }, metricParams)

                    Charts.Sun.Daily.AirMass, Charts.Moon.Daily.AirMass -> createHorizontalBrush({ i, value ->
                        val isNight = value == 0f && (
                            i == 0 ||
                                i == timeXValues.lastIndex ||
                                activeYValues.getOrNull(i - 5) == 0f ||
                                activeYValues.getOrNull(i + 5) == 0f
                            )
                        if (isNight) {
                            fills.amHorizon
                        } else {
                            val fraction = ((value.coerceIn(1f, 10f) - 1f) / 9f)
                            lerp(fills.amZenith, fills.amHorizon, fraction)
                        }
                    }, metricParams)

                    Charts.Sun.Daily.Shadows, Charts.Moon.Daily.Shadows -> createHorizontalBrush({ i, value ->
                        val isNight = value == 0f && (
                            i == 0 ||
                                i == timeXValues.lastIndex ||
                                activeYValues.getOrNull(i - 5) == 0f ||
                                activeYValues.getOrNull(i + 5) == 0f
                            )
                        if (isNight) {
                            fills.shadowLong
                        } else {
                            val fraction = (value.coerceIn(0f, 10f) / 10f)
                            lerp(fills.shadowShort, fills.shadowLong, fraction)
                        }
                    }, metricParams)

                    Charts.Sun.Daily.Illuminance, Charts.Moon.Daily.Illuminance -> {
                        val maxLux = if (chartType == Charts.Moon.Daily.Illuminance) 0.3f else 100000f
                        val denom = ln(1f + maxLux)
                        createHorizontalBrush({ _, value ->
                            val frac = (ln(1f + value.coerceAtLeast(0f)) / denom).coerceIn(0f, 1f)
                            lerp(fills.luxDim, fills.luxBright, frac)
                        }, metricParams)
                    }

                    Charts.Sun.Daily.ColorTemperature, Charts.Moon.Daily.ColorTemperature -> {
                        createHorizontalBrush({ _, kelvin ->
                            when {
                                kelvin <= 2000f -> fills.ct2000
                                kelvin <= 3000f -> lerp(fills.ct2000, fills.ct3000, (kelvin - 2000f) / 1000f)
                                kelvin <= 4000f -> lerp(fills.ct3000, fills.ct4000, (kelvin - 3000f) / 1000f)
                                kelvin <= 5500f -> lerp(fills.ct4000, fills.ct5500, (kelvin - 4000f) / 1500f)
                                else -> fills.ct5500
                            }
                        }, metricParams)
                    }

                    else -> null
                }
            }

            val dayBase = colorScheme.surfaceContainer
            val nightBase = colorScheme.surfaceVariant
            val dayFill = fills.dayBackground
            val markerStrokePx = 2.dp.toPx()

            onDrawBehind {
                val h = size.height
                if (h <= 0f) return@onDrawBehind

                val currentXPx = mapX(currentHourState.value)

                // 1. Below-horizon base + night/twilight vertical bands
                drawRect(color = nightBase, topLeft = Offset.Zero, size = Size(width, h))
                for (i in belowHorizonBands.indices) {
                    val band = belowHorizonBands[i]
                    drawRect(
                        color = band.color,
                        topLeft = Offset(band.startPx, 0f),
                        size = Size(band.endPx - band.startPx, h),
                    )
                }

                // 2. Elapsed shading for below-horizon regions (above-horizon is painted cleanly in step 3)
                if (currentXPx > 0f) {
                    drawRect(
                        color = elapsedNightFill,
                        topLeft = Offset.Zero,
                        size = Size(currentXPx, h),
                    )
                }

                // 3. Above-horizon (daylight / moon-up) vertical bands + elapsed shading
                for (i in aboveHorizonBands.indices) {
                    val band = aboveHorizonBands[i]
                    val bandWidth = band.endPx - band.startPx
                    drawRect(
                        color = dayBase,
                        topLeft = Offset(band.startPx, 0f),
                        size = Size(bandWidth, h),
                    )
                    clipRect(left = band.startPx, top = 0f, right = band.endPx, bottom = h) {
                        when {
                            chartType == Charts.Sun.Daily.UvIntensity -> {
                                for (u in uvSliceBands.indices) {
                                    val uvBand = uvSliceBands[u]
                                    drawRect(
                                        color = uvBand.color,
                                        topLeft = Offset(uvBand.startPx, 0f),
                                        size = Size(uvBand.endPx - uvBand.startPx, h),
                                    )
                                }
                            }
                            metricBrush != null -> {
                                drawRect(
                                    brush = metricBrush,
                                    topLeft = Offset.Zero,
                                    size = Size(width, h),
                                )
                            }
                            else -> {
                                drawRect(
                                    color = dayFill,
                                    topLeft = Offset(band.startPx, 0f),
                                    size = Size(bandWidth, h),
                                )
                            }
                        }
                    }

                    if (currentXPx > band.startPx) {
                        val elapsedEnd = currentXPx.coerceAtMost(band.endPx)
                        drawRect(
                            color = elapsedDayFill,
                            topLeft = Offset(band.startPx, 0f),
                            size = Size(elapsedEnd - band.startPx, h),
                        )
                    }
                }

                // 4. Crisp vertical current-time marker (no icon)
                val clampedX = currentXPx.coerceIn(markerStrokePx / 2f, width - markerStrokePx / 2f)
                drawLine(
                    color = markerColor,
                    start = Offset(clampedX, 0f),
                    end = Offset(clampedX, h),
                    strokeWidth = markerStrokePx,
                )
            }
        }
    }

    Spacer(modifier = modifier.then(drawModifier))
}
