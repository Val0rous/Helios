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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.charts.HorizontalColorBand
import com.ephemeris.helios.utils.charts.buildHorizontalColorBands
import com.ephemeris.helios.utils.location.Coordinates

/**
 * 1D horizontal elevation timeline strip rendered in the collapsed Sun/Moon header.
 *
 * Always displays the vertical elevation bands for night, astronomical/nautical/civil twilight,
 * and daylight (or moon-up), plus elapsed shading and a crisp vertical current-time marker,
 * omitting the 2D sine curve, horizon line, axis labels, and Sun/Moon icon for high legibility
 * at compact height.
 */
@Composable
fun CompactDailyRibbon(
    timeXValues: FloatArray,
    elevationYValues: FloatArray,
    chartType: Charts,
    currentHour: Float,
    coordinates: Coordinates?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme
    val fills = colors.fills
    val isSun = chartType is Charts.Sun
    val elapsedDayFill = if (isSun) fills.elapsedDay else fills.elapsedOther
    val elapsedNightFill = fills.elapsedNight
    val markerColor = if (isSun) colors.sunPath else colors.moonPath

    val currentHourState = rememberUpdatedState(currentHour)

    val drawModifier = remember(
        timeXValues,
        elevationYValues,
        isSun,
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
                    drawRect(
                        color = dayFill,
                        topLeft = Offset(band.startPx, 0f),
                        size = Size(bandWidth, h),
                    )

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
