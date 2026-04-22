package com.ephemeris.helios.utils.charts

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.formatHour
import com.ephemeris.helios.utils.formatNumber
import com.ephemeris.helios.utils.printRounded
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.text.toFloat

fun DrawScope.drawYLabels(
    chartType: Charts,
    materialTheme: ColorScheme,
    params: ChartData,
    mapY: (Float) -> Float,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle
) {
    val horizontalGridDashEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f)
    val horizontalGridlineColor = materialTheme.outline.copy(alpha = 0.3f) // Light and subtle
    val className = chartType.javaClass.simpleName

    val yLabels = when {
        className.contains("Elevation") || className.contains("Trajectory") -> (-90 until 91 step 15).map { it.toFloat() }
        className.contains("Irradiance") -> (0 until ((params.maxY / 100.0).roundToInt() * 100 + 1) step 100).map { it.toFloat() }
        className.contains("UvIntensity") -> (0 until (params.maxY.roundToInt() + 1) step floor(params.maxY / 10f).toInt().coerceAtLeast(1)).map { it.toFloat() }
        className.contains("Illuminance") -> listOf(0f) + (0..5).flatMap {
            val base = 10.0.pow(it.toDouble()).toFloat()
            listOf(base, base * 3f)
        }.filter { it <= params.maxY }
        className.contains("Shadows") || className.contains("AirMass") -> listOf(0f, 0.25f, 0.5f, 1f, 1.5f, 2f, 3f, 4f, 5f, 6f, 7f, 10f)
        className.contains("ColorTemperature") -> (2000 until (params.maxY.roundToInt() + 1) step 500).map { it.toFloat() }
        else -> emptyList()
    }
    yLabels.forEach { yVal ->
        val yPx = mapY(yVal)
//            if (yVal == 0f) return@forEach // Skip the zero

        // Draw horizontal dotted grid line (Skip 0f to avoid drawing over the solid horizon line)
        if (yVal != 0f) {
            drawLine(
                color = horizontalGridlineColor,
                start = Offset(0f, yPx),
                end = Offset(params.width, yPx),
                strokeWidth = 1.dp.toPx(),
                pathEffect = horizontalGridDashEffect
            )
        }

        val text = when {
            className.contains("Elevation") || className.contains("Trajectory") -> "${yVal.toInt()}°"
            className.contains("Irradiance") -> "${formatNumber(yVal.toDouble())} W/m²"
            className.contains("ColorTemperature") -> "${formatNumber(yVal.toDouble())}K"
            className.contains("Illuminance") -> "${formatNumber(yVal.toDouble())} lx"
            className.contains("Shadows") || className.contains("AirMass") -> yVal.toDouble().printRounded(2)
            else -> "${yVal.toInt()}"
        }
        val textLayout = textMeasurer.measure(text, labelStyle)
        drawText(
            textMeasurer = textMeasurer,
            text = text,
            style = labelStyle,
            topLeft = Offset(
                x = 4.dp.toPx(), // Slight padding from the left edge
                y = yPx - (textLayout.size.height / 2f) // Perfectly centered vertically on the line
            )
        )
    }
}

fun DrawScope.drawXLabels(
    chartType: Charts,
    materialTheme: ColorScheme,
    params: ChartData,
    mapX: (Float) -> Float,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    context: Context,
    isShiftTrajectory: Boolean = false
) {
    val verticalGridDashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 0.dp.toPx()), 0f)
    val verticalGridlineColor = materialTheme.outlineVariant.copy(alpha = 0.15f) // Light and subtle
    val className = chartType.javaClass.simpleName

    val xLabels = when {
        className.contains("Trajectory") -> (30..330 step 30).toList()
        else -> (3..21 step 3).toList()
    }
    xLabels.forEach {
        val xPx = mapX(it.toFloat())

        // Draw vertical dotted grid line
        drawLine(
            color = verticalGridlineColor,
            start = Offset(xPx, 0f),
            end = Offset(xPx, params.height), // Spans the entire canvas height
            strokeWidth = 1.dp.toPx(),
            pathEffect = verticalGridDashEffect
        )

        val text = when {
            className.contains("Trajectory") -> {
                // Mathematically reverse the shift for the label
                val realAzimuth = if (isShiftTrajectory) (it.toFloat() + 180f) % 360 else it.toFloat()
                val formatted = realAzimuth.toInt()
                "${if (formatted == 360 || formatted == 0) 0 else formatted}°"
            }
            else -> formatHour(it, true, context)
        }
        val textLayout = textMeasurer.measure(text, labelStyle)

        val offset = when {
            className.contains("Trajectory") -> 2.5f
            else -> 2f
        }
        drawText(
            textMeasurer = textMeasurer,
            text = text,
            style = labelStyle,
            topLeft = Offset(
                x = xPx - (textLayout.size.width / offset), // Centered horizontally on the hour mark
                y = params.height - textLayout.size.height - 2.dp.toPx() // Pinned near the bottom edge of the canvas
            )
        )
    }
}