package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.ui.theme.MaterialColors
import kotlin.math.round

@Composable
fun PathChart(
    xValues: FloatArray,
    yValues: FloatArray,
    currentHour: Float,
    modifier: Modifier = Modifier
) {
    val sunPainter = painterResource(id = R.drawable.ic_sunny_filled)
    val indicatorPainter = painterResource(id = R.drawable.ic_circle_filled)

    val colors = LocalCustomColors.current
    val sunYellow = colors.sun
    val dayFill = colors.day
    val civilTwilightFill = colors.civilTwilight
    val nauticalTwilightFill = colors.nauticalTwilight
    val astroTwilightFill = colors.astronomicalTwilight
    val nightFill = colors.night
    val dayBackground = colors.dayBackground
    val nightBackground = colors.nightBackground
    val elapsedDayFill = colors.elapsedDay
    val elapsedNightFill = colors.elapsedNight

    val backgroundColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        if (xValues.isEmpty() || yValues.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        val minX = xValues.minOrNull() ?: 0f
        val maxX = xValues.maxOrNull() ?: 24f
        val minY = -90f
        val maxY = 90f

        val verticalPaddingPx = 16.dp.toPx()
        val drawHeight = (height - (2 * verticalPaddingPx)).coerceAtLeast(1f)

        // Helper functions to map mathematical coordinates to Canvas pixels
        fun mapX(x: Float) = ((x - minX) / (maxX - minX)) * width
        // Canvas Y=0 is at the top, so we invert the Y mapping
        fun mapY(y: Float) = height - verticalPaddingPx - ((y - minY) / (maxY - minY)) * drawHeight

        val zeroYPixel = mapY(0f)
        val currentXPx = mapX(currentHour)

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

        // 1. Build the smooth curve path
        val curvePath = Path().apply {
            if(xValues.isNotEmpty()) {
                moveTo(mapX(xValues[0]), mapY(yValues[0]))
                for (i in 1 until xValues.size) {
                    lineTo(mapX(xValues[i]), mapY(yValues[i]))
                }
            }
        }

        // 2. Build the fill path that closes down to the X-axis
        val fillPath = Path().apply {
            addPath(curvePath)
            lineTo(mapX(xValues.last()), zeroYPixel)
            lineTo(mapX(xValues.first()), zeroYPixel)
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
            color = MaterialColors.Gray500,
            style = Stroke(width = (1.5).dp.toPx())
        )

        // 5. Draw a subtle X-Axis line to visually separate the zones
        drawLine(
            color = MaterialColors.Gray700,
            start = Offset(0f, zeroYPixel),
            end = Offset(width, zeroYPixel),
            strokeWidth = (1.5).dp.toPx()
        )

        // 6. Calculate exact Y position for the sun at currentHour
        var currentY = 0f
        for (i in 0 until xValues.size - 1) {
            if (currentHour in xValues[i]..xValues[i+1]) {
                val timeDelta = xValues[i+1] - xValues[i]
                if (timeDelta > 0f) {
                    // Linear interpolation to find the exact altitude at this moment
                    val fraction = (currentHour - xValues[i]) / timeDelta
                    currentY = yValues[i] + fraction * (yValues[i+1] - yValues[i])
                }
                break
            }
        }
        val currentYPx = mapY(currentY)

        // 7. Paint the Sun Icon if above horizon
        if (currentY >= 0f) {
            val iconSize = 24.dp.toPx()
//            val padding = 4.dp.toPx()
//            val radius = (iconSize / 2) + padding

//            drawCircle(
//                color = backgroundColor,
//                radius = radius,
//                center = Offset(currentXPx, currentYPx)
//            )

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