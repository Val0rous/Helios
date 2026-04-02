package com.ephemeris.helios.utils.charts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.ephemeris.helios.ui.theme.CustomColorScheme
import com.ephemeris.helios.utils.Charts
import kotlin.math.abs

// --- DIRECTION-AWARE PATH BUILDER ---
// Generates flawless polygons regardless of left-to-right or right-to-left tracing
fun buildDynamicPath(
    startIndex: Int,
    endIndex: Int,
    isFill: Boolean,
    drawXValues: FloatArray,
    yValues: FloatArray,
    zeroYPixel: Float,
    mapX: (Float) -> Float,
    mapY: (Float) -> Float,
): Path {
    return Path().apply {
        if (startIndex !in 0..endIndex || drawXValues.isEmpty()) return@apply

        if (isFill) {
            moveTo(mapX(drawXValues[startIndex]), zeroYPixel)
            lineTo(mapX(drawXValues[startIndex]), mapY(yValues[startIndex]))
        } else {
            moveTo(mapX(drawXValues[startIndex]), mapY(yValues[startIndex]))
        }

        for (i in (startIndex + 1)..endIndex) {
            val xA = drawXValues[i-1]
            val xB = drawXValues[i]

            if (abs(xB - xA) > 200f) { // wrapThreshold seamlessly catches 360 wraps in BOTH directions
                // Calculate exact crossing direction
                val wrapForward = xA > 180f && xB < 180f
                val edgeX = if (wrapForward) 360f else 0f
                val newStartX = if (wrapForward) 0f else 360f

                val deltaX = if (wrapForward) (xB + 360f) - xA else xB - (xA + 360f)
                val fraction = if (deltaX != 0f) (edgeX - xA) / deltaX else 0f
                val edgeY = yValues[i-1] + fraction * (yValues[i] - yValues[i-1])

                lineTo(mapX(edgeX), mapY(edgeY))
                if (isFill) {
                    lineTo(mapX(edgeX), zeroYPixel)
                    close()
                    moveTo(mapX(newStartX), zeroYPixel)
                } else {
                    moveTo(mapX(newStartX), mapY(edgeY))
                }
                lineTo(mapX(newStartX), mapY(edgeY))
            } else {
                lineTo(mapX(xB), mapY(yValues[i]))
            }
        }
        if (isFill) {
            lineTo(mapX(drawXValues[endIndex]), zeroYPixel)
            close()
        }
    }
}

fun getElapsedLineColor(chartType: Charts, localCustomColors: CustomColorScheme): Color {
    return when (chartType) {
        is Charts.Sun -> localCustomColors.sunPath
        is Charts.Moon -> localCustomColors.moonPath
        else -> Color.Green // TODO
    }
}