package com.ephemeris.helios.utils.charts

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.ChartAlphas
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.formatHour
import com.ephemeris.helios.utils.formatNumber
import kotlin.math.pow
import kotlin.math.roundToInt

class PreparedLabel(
    val layout: TextLayoutResult,
    val topLeft: Offset,
)

class PreparedYLabels(
    val gridLinesY: FloatArray,
    val gridLineColor: Color,
    val strokeWidthPx: Float,
    val dashEffect: PathEffect,
    val labels: List<PreparedLabel>,
)

class PreparedXLabels(
    val gridLinesX: FloatArray,
    val gridLineColor: Color,
    val strokeWidthPx: Float,
    val dashEffect: PathEffect,
    val labels: List<PreparedLabel>,
)

/**
 * Pre-measures all Y-axis text layouts and gridline coordinates once per day/size/theme change
 * inside Modifier.drawWithCache so per-frame scrubbing does zero string formatting or HarfBuzz layout.
 */
fun Density.prepareYLabels(
    chartType: Charts,
    materialTheme: ColorScheme,
    params: ChartData,
    mapY: (Float) -> Float,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    isApparentElevation: Boolean = false,
): PreparedYLabels {
    val dashPx = 2.dp.toPx()
    val horizontalGridDashEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx), 0f)
    val horizontalGridlineColor = materialTheme.outline.copy(alpha = ChartAlphas.GRID_HORIZONTAL)
    val strokeWidthPx = 1.dp.toPx()
    val leftPaddingPx = 4.dp.toPx()
    val className = chartType.javaClass.simpleName
    val isElevationOrTrajectory = className.contains("Elevation") || className.contains("Trajectory")

    val yLabels = when {
        isElevationOrTrajectory -> {
            if (isApparentElevation) {
                listOf(-90f, -56f, -41f, -30f, -20f, -10f, 0f, 10f, 20f, 30f, 41f, 56f, 90f)
            } else {
                (-90 until 91 step 15).map { it.toFloat() }
            }
        }
        className.contains("Irradiance") -> {
            val step = getIrradianceStep(params.maxY)
            (0..(params.maxY + 1e-3f).toInt() step step).map { it.toFloat() }
        }
        className.contains("UvIntensity") -> {
            val stepTenths = getUvIntensityStepTenths(params.maxY)
            val maxTenths = (params.maxY * 10f + 1e-3f).toInt()
            (0..maxTenths step stepTenths).map { it / 10f }
        }
        chartType == Charts.Moon.Daily.Illuminance -> listOf(0f, 0.001f, 0.003f, 0.01f, 0.03f, 0.1f, 0.3f, 1f).filter { it <= params.maxY }
        className.contains("Illuminance") -> listOf(0f) + (0..5).flatMap {
            val base = 10.0.pow(it.toDouble()).toFloat()
            listOf(base, base * 3f)
        }.filter { it <= params.maxY }
        className.contains("Shadows") -> {
            val ticks = when {
                params.maxY <= 80f -> listOf(0f, 0.1f, 0.25f, 0.5f, 1f, 2f, 5f, 10f, 20f, 50f)
                params.maxY <= 250f -> listOf(0f, 0.25f, 1f, 2f, 5f, 10f, 20f, 50f, 100f, 200f)
                params.maxY <= 550f -> listOf(0f, 0.25f, 1f, 5f, 10f, 20f, 50f, 100f, 200f, 500f)
                else -> listOf(0f, 0.25f, 1f, 5f, 20f, 100f, 500f, 2000f, 5000f)
            }
            ticks.filter { it <= params.maxY }
        }
        className.contains("AirMass") -> listOf(0f, 0.5f, 1f, 2f, 5f, 10f, 20f, 50f, 100f).filter { it <= params.maxY }
        className.contains("ColorTemperature") -> {
            val step = if (params.maxY > 6000f) 1000 else 500
            (2000..(params.maxY + 1e-3f).toInt() step step).map { it.toFloat() }
        }
        else -> emptyList()
    }

    val gridYs = ArrayList<Float>(yLabels.size)
    val preparedLabels = ArrayList<PreparedLabel>(yLabels.size)

    yLabels.forEach { yVal ->
        val mappedYVal = if (isApparentElevation && isElevationOrTrajectory) {
            transformApparentElevation(yVal)
        } else {
            yVal
        }
        val yPx = mapY(mappedYVal)

        // Skip 0f to avoid drawing over the solid horizon line
        if (yVal != 0f) {
            gridYs.add(yPx)
        }

        val text = when {
            isElevationOrTrajectory -> "${yVal.toInt()}°"
            className.contains("Irradiance") -> "${formatNumber(yVal.toDouble())} W/m²"
            className.contains("ColorTemperature") -> "${formatNumber(yVal.toDouble())}K"
            chartType == Charts.Moon.Daily.Illuminance -> "${formatNumber((yVal * 1000.0).roundToInt().toDouble())} mlx"
            className.contains("Illuminance") -> "${formatNumber(yVal.toDouble())} lx"
            className.contains("Shadows") || className.contains("AirMass") || className.contains("UvIntensity") -> formatNumber(yVal.toDouble())
            else -> "${yVal.toInt()}"
        }
        val textLayout = textMeasurer.measure(text, labelStyle)
        preparedLabels.add(
            PreparedLabel(
                layout = textLayout,
                topLeft = Offset(
                    x = leftPaddingPx,
                    y = yPx - (textLayout.size.height / 2f),
                ),
            ),
        )
    }

    return PreparedYLabels(
        gridLinesY = gridYs.toFloatArray(),
        gridLineColor = horizontalGridlineColor,
        strokeWidthPx = strokeWidthPx,
        dashEffect = horizontalGridDashEffect,
        labels = preparedLabels,
    )
}

fun DrawScope.drawPreparedYLabels(
    prepared: PreparedYLabels,
    width: Float,
    alpha: Float = 1f,
) {
    if (alpha <= 0.005f) return
    val gridColor = prepared.gridLineColor.copy(alpha = prepared.gridLineColor.alpha * alpha)
    val gridYs = prepared.gridLinesY
    for (i in gridYs.indices) {
        val yPx = gridYs[i]
        drawLine(
            color = gridColor,
            start = Offset(0f, yPx),
            end = Offset(width, yPx),
            strokeWidth = prepared.strokeWidthPx,
            pathEffect = prepared.dashEffect,
        )
    }
    val labels = prepared.labels
    for (i in labels.indices) {
        val item = labels[i]
        drawText(
            textLayoutResult = item.layout,
            topLeft = item.topLeft,
            alpha = alpha,
        )
    }
}

/**
 * Pre-measures all X-axis text layouts and gridline coordinates once per day/size/theme change
 * inside Modifier.drawWithCache so per-frame scrubbing does zero string formatting or HarfBuzz layout.
 */
fun Density.prepareXLabels(
    chartType: Charts,
    materialTheme: ColorScheme,
    params: ChartData,
    mapX: (Float) -> Float,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    context: Context,
    isShiftTrajectory: Boolean = false,
): PreparedXLabels {
    val verticalGridDashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 0.dp.toPx()), 0f)
    val verticalGridlineColor = materialTheme.outlineVariant.copy(alpha = ChartAlphas.GRID_VERTICAL)
    val strokeWidthPx = 1.dp.toPx()
    val bottomPaddingPx = 2.dp.toPx()
    val className = chartType.javaClass.simpleName
    val isTrajectory = className.contains("Trajectory")

    val xLabels = if (isTrajectory) (30..330 step 30).toList() else (3..21 step 3).toList()
    val gridXs = FloatArray(xLabels.size)
    val preparedLabels = ArrayList<PreparedLabel>(xLabels.size)
    val offsetDivisor = if (isTrajectory) 2.5f else 2f

    xLabels.forEachIndexed { index, value ->
        val xPx = mapX(value.toFloat())
        gridXs[index] = xPx

        val text = if (isTrajectory) {
            val realAzimuth = if (isShiftTrajectory) (value.toFloat() + 180f) % 360 else value.toFloat()
            val formatted = realAzimuth.toInt()
            "${if (formatted == 360 || formatted == 0) 0 else formatted}°"
        } else {
            formatHour(value, true, context)
        }
        val textLayout = textMeasurer.measure(text, labelStyle)
        preparedLabels.add(
            PreparedLabel(
                layout = textLayout,
                topLeft = Offset(
                    x = xPx - (textLayout.size.width / offsetDivisor),
                    y = params.height - textLayout.size.height - bottomPaddingPx,
                ),
            ),
        )
    }

    return PreparedXLabels(
        gridLinesX = gridXs,
        gridLineColor = verticalGridlineColor,
        strokeWidthPx = strokeWidthPx,
        dashEffect = verticalGridDashEffect,
        labels = preparedLabels,
    )
}

fun DrawScope.drawPreparedXLabels(
    prepared: PreparedXLabels,
    height: Float,
    alpha: Float = 1f,
) {
    if (alpha <= 0.005f) return
    val gridColor = prepared.gridLineColor.copy(alpha = prepared.gridLineColor.alpha * alpha)
    val gridXs = prepared.gridLinesX
    for (i in gridXs.indices) {
        val xPx = gridXs[i]
        drawLine(
            color = gridColor,
            start = Offset(xPx, 0f),
            end = Offset(xPx, height),
            strokeWidth = prepared.strokeWidthPx,
            pathEffect = prepared.dashEffect,
        )
    }
    val labels = prepared.labels
    for (i in labels.indices) {
        val item = labels[i]
        drawText(
            textLayoutResult = item.layout,
            topLeft = item.topLeft,
            alpha = alpha,
        )
    }
}
