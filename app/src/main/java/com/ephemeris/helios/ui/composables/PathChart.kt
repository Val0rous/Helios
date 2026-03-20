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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R

@Composable
fun PathChart(
    xValues: FloatArray,
    yValues: FloatArray,
    currentHour: Float,
    modifier: Modifier = Modifier
) {
    val sunPainter = painterResource(id = R.drawable.ic_sunny_filled)

    val sunYellow = Color(0xFFFFEB3B)
    val dayOrangeFill = Color(0xFFFF9800).copy(alpha = 0.4f)
    val nightBlueFill = Color(0xFF2196F3).copy(alpha = 0.4f)
    val dayBackground = Color(0xFFFFF9C4).copy(alpha = 0.2f)
    val nightBackground = Color(0xFFE3F2FD).copy(alpha = 0.2f)

    val backgroundColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        if (xValues.isEmpty() || yValues.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        val minX = xValues.minOrNull() ?: 0f
        val maxX = xValues.maxOrNull() ?: 24f
        val minY = yValues.minOrNull() ?: -90f
        val maxY = yValues.maxOrNull() ?: 90f

        val verticalPaddingPx = 32.dp.toPx()
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
            moveTo(mapX(xValues[0]), mapY(yValues[0]))
            for (i in 0 until xValues.size - 1) {
                // Get previous point (or duplicate current if at the start edge)
                val x0 = mapX(xValues[(i - 1).coerceAtLeast(0)])
                val y0 = mapY(yValues[(i - 1).coerceAtLeast(0)])

                // Current point
                val x1 = mapX(xValues[i])
                val y1 = mapY(yValues[i])

                // Next point
                val x2 = mapX(xValues[i + 1])
                val y2 = mapY(yValues[i + 1])

                // Point after next (or duplicate next if at the end edge)
                val x3 = mapX(xValues[(i + 2).coerceAtMost(xValues.size - 1)])
                val y3 = mapY(yValues[(i + 2).coerceAtMost(yValues.size - 1)])

                // Tension controls how "tight" the curve is. 0.2f is standard for sine waves
                val tension = 0.0f
                val cx1 = x1 + (x2 - x0) * tension
                val cy1 = y1 + (y2 - y0) * tension
                val cx2 = x2 - (x3 - x1) * tension
                val cy2 = y2 - (y3 - y1) * tension

                //lineTo
                cubicTo(cx1, cy1, cx2, cy2, x2, y2)
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

        // 3. Draw the colored areas (clipped strictly up to currentHour)
        clipRect(right = currentXPx) {

            // Positive Y (Above X-axis -> from Canvas top down to zeroYPixel)
            clipRect(bottom = zeroYPixel) {
                drawPath(path = fillPath, color = dayOrangeFill)
            }

            // Negative Y (Below X-axis -> from zeroYPixel down to Canvas bottom)
            clipRect(top = zeroYPixel) {
                drawPath(path = fillPath, color = nightBlueFill)
            }
        }

        // 4. Draw the full unclipped curve line for all values
        drawPath(
            path = curvePath,
            color = Color.Gray,
            style = Stroke(width = 3.dp.toPx())
        )

        // 5. Draw a subtle X-Axis line to visually separate the zones
        drawLine(
            color = Color.LightGray,
            start = Offset(0f, zeroYPixel),
            end = Offset(width, zeroYPixel),
            strokeWidth = 2.dp.toPx()
        )

        // 6. Calculate exact Y position for the sun at currentHour
        var currentY = 0f
        for (i in 0 until xValues.size - 1) {
            if (currentHour in xValues[i]..xValues[i+1]) {
                val timeDelta = (xValues[i+1] - xValues[i])
                if (timeDelta > 0) {
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
            val iconSize = 28.dp.toPx()
            val padding = 4.dp.toPx()
            val radius = (iconSize / 2) + padding

            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = Offset(currentXPx, currentYPx)
            )

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
    }
}