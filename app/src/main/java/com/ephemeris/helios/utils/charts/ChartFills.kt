package com.ephemeris.helios.utils.charts

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect

fun DrawScope.drawDayNightBackground(
    colorScheme: ColorScheme,
    params: ChartData,
    zeroYPixel: Float
) {
    val dayBackground = colorScheme.surface
    val nightBackground = colorScheme.surfaceVariant

    // Day Background
    drawRect(
        color = dayBackground,
        topLeft = Offset(0f, 0f),
        size = Size(params.width, zeroYPixel)
    )

    // Night Background
    drawRect(
        color = nightBackground,
        topLeft = Offset(0f, zeroYPixel),
        size = Size(params.width, params.height - zeroYPixel)
    )
}

fun DrawScope.drawDayNightAreaFill(
    fillPath: Path,
    colorScheme: ColorScheme,
    zeroYPixel: Float
) {
    val dayBackground = colorScheme.surface
    val nightBackground = colorScheme.surfaceVariant
    clipRect(bottom = zeroYPixel) {
        drawPath(path = fillPath, color = dayBackground.copy(alpha = 1.0f))
    }

    clipRect(top = zeroYPixel) {
        drawPath(path = fillPath, color = nightBackground.copy(alpha = 1.0f))
    }
}