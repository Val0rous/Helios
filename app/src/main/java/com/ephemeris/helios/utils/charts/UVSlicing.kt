package com.ephemeris.helios.utils.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.round

fun DrawScope.drawUVSlices(
    params: ChartData,
    uniqueXPoints: List<Float>,
    mapX: (Float) -> Float,
    zeroYPixel: Float
) {
    val uvDarkGreen = Color(0xFF2E7D32).copy(alpha = 0.5f)
    val uvGreen = Color(0xFF4CAF50).copy(alpha = 0.5f)
    val uvYellow = Color(0xFFFFEB3B).copy(alpha = 0.5f)
    val uvAmber = Color(0xFFFFC107).copy(alpha = 0.5f)
    val uvOrange = Color(0xFFFF9800).copy(alpha = 0.5f)
    val uvRed = Color(0xFFF44336).copy(alpha = 0.5f)
    val uvDarkRed = Color(0xFFB71C1C).copy(alpha = 0.5f)
    val uvPurple = Color(0xFF673AB7).copy(alpha = 0.5f)

    // --- UV Slicing Logic ---
    var currentBlockColor = Color.Transparent
    var blockStartX = uniqueXPoints.firstOrNull() ?: 0f

    for (i in 0 until uniqueXPoints.size - 1) {
        val xA = uniqueXPoints[i]
        val xB = uniqueXPoints[i + 1]
        val midX = (xA + xB) / 2f

        // Interpolate to find UV Index at the exact center of this stripe
        var midY = 0f
        for (j in 0 until params.xValues.size - 1) {
            if (midX >= params.xValues[j] && midX <= params.xValues[j + 1]) {
                val delta = params.xValues[j + 1] - params.xValues[j]
                midY = if (delta > 0f) {
                    params.yValues[j] + ((midX - params.xValues[j]) / delta) * (params.yValues[j + 1] - params.yValues[j])
                } else params.yValues[j]
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