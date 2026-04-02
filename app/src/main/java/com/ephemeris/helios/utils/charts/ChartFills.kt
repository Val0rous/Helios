package com.ephemeris.helios.utils.charts

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.CustomColorScheme
import com.ephemeris.helios.utils.Charts

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

fun DrawScope.drawCurvePath(
    curvePath: Path,
    materialTheme: ColorScheme
) {
    drawPath(
        path = curvePath,
        color = materialTheme.onSurfaceVariant,
        style = Stroke(width = (1.5).dp.toPx())
    )
}

fun DrawScope.drawElapsedPath(
    curvePath: Path,
    localCustomColors: CustomColorScheme,
    chartType: Charts,
    currentXPx: Float
) {
    val elapsedPathColor = when (chartType) {
        is Charts.Sun -> localCustomColors.sunPath
        is Charts.Moon -> localCustomColors.moonPath
        else -> Color.Green // TODO
    }
    clipRect(right = currentXPx) {
        drawPath(
            path = curvePath,
            color = elapsedPathColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

fun DrawScope.drawHorizonLine(
    materialTheme: ColorScheme,
    params: ChartData,
    zeroYPixel: Float
) {
    drawLine(
        color = materialTheme.outline,
        start = Offset(0f, zeroYPixel),
        end = Offset(params.width, zeroYPixel),
        strokeWidth = (1.5).dp.toPx()
    )
}

fun DrawScope.drawVerticalDropLine(
    localCustomColors: CustomColorScheme,
    currentXPx: Float,
    currentYPx: Float,
    zeroYPixel: Float
) {
    drawLine(
        color = localCustomColors.dropLine,
        start = Offset(currentXPx, currentYPx),
        end = Offset(currentXPx, zeroYPixel),
        strokeWidth = 1.dp.toPx()
    )
}