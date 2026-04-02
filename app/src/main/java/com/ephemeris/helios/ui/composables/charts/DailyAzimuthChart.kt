package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.charts.ChartData
import com.ephemeris.helios.utils.charts.buildDynamicPath
import com.ephemeris.helios.utils.charts.drawDayNightAreaFill
import com.ephemeris.helios.utils.charts.drawDayNightBackground
import com.ephemeris.helios.utils.charts.drawDayNightHorizontalTwilights
import com.ephemeris.helios.utils.charts.drawXLabels
import com.ephemeris.helios.utils.charts.drawYLabels
import com.ephemeris.helios.utils.charts.getElapsedLineColor
import com.ephemeris.helios.utils.charts.getMapX
import com.ephemeris.helios.utils.charts.getMapY
import com.ephemeris.helios.utils.charts.getMaxX
import com.ephemeris.helios.utils.charts.getMaxY
import com.ephemeris.helios.utils.charts.getMinX
import com.ephemeris.helios.utils.charts.getMinY
import com.ephemeris.helios.utils.charts.getZeroYPixel

@Composable
fun DailyAzimuthChart(
    xValues: FloatArray,
    yValues: FloatArray,
    currentHour: Float,
    chartType: Charts,
    currentAzimuth: Float,
    currentAltitude: Float,
    modifier: Modifier = Modifier
) {
    val drawChartIcon = rememberChartIconDrawer(chartType)

    val colors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme
    val dayBackground = MaterialTheme.colorScheme.surface
    val nightBackground = MaterialTheme.colorScheme.surfaceVariant
    val materialTheme = MaterialTheme.colorScheme
    val localCustomColors = LocalCustomColors.current
    // Text Measurer and styling for the legends
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        fontSize = (9.5).sp,
        fontFamily = FontFamily.Monospace
    )
    val context = LocalContext.current

    Canvas(modifier = modifier) {
        if (xValues.isEmpty() || yValues.isEmpty()) return@Canvas

        val params = ChartData(
            xValues = xValues,
            yValues = yValues,
            minX = getMinX(xValues, chartType),
            maxX = getMaxX(xValues, chartType),
            minY = getMinY(yValues, chartType),
            maxY = getMaxY(yValues, chartType),
            width = size.width,
            height = size.height,
            verticalPaddingPx = 16.dp.toPx()
        )

        // Dynamic Trajectory Shifting
        // Find where the sun reaches its highest point
        val peakIndex = yValues.indices.maxByOrNull { yValues[it] } ?: 0
        val peakAzimuth = xValues[peakIndex]
        // If it culminates North (near 0 or 360), we shift the chart by 180 degrees
        val shiftTrajectory = (peakAzimuth !in 90f..270f)

        // --- PURE SHIFT MAPPING ---
        // Keeps North(0) at the center, naturally preserving Right-To-Left motion!
        // Create a mapped array purely for continuous drawing
        val drawXValues = FloatArray(xValues.size) { i ->
            if (shiftTrajectory) (xValues[i] + 180f) % 360f else xValues[i]
        }

        // Helper functions to map mathematical coordinates to Canvas pixels
        fun mapX(x: Float) = getMapX(x, params)
        // Canvas Y=0 is at the top, so we invert the Y mapping
        fun mapY(y: Float) = getMapY(y, params, chartType)
        val zeroYPixel = getZeroYPixel(chartType, ::mapY)
        fun buildDynamicPath(startIndex: Int, endIndex: Int, isFill: Boolean) = buildDynamicPath(startIndex, endIndex, isFill, drawXValues, yValues, zeroYPixel, ::mapX, ::mapY)

        // Ensure the sun icon aligns with the physical inputs
        val drawCurrentX = if (shiftTrajectory) (currentAzimuth + 180f) % 360f else currentAzimuth
        val currentXPx = mapX(drawCurrentX)
        val currentYPx = mapY(currentAltitude)

        // --- FLAWLESS CHRONOLOGICAL INDEX ---
        // Mathematically maps current local time directly to the array index
        val exactIndex = (currentHour / 24f) * (xValues.size - 1)
        val bestIndex = exactIndex.toInt().coerceIn(0, xValues.size - 1)

        // 1. Build the smooth curve path
        val curvePath = buildDynamicPath(0, drawXValues.size - 1, false)

        // 2. Build the fill path that closes down to the X-axis
        val fillPath = buildDynamicPath(0, drawXValues.size - 1, true)

        // --- CHRONOLOGICAL ELAPSED LINE ---
        val elapsedLinePath = buildDynamicPath(0, bestIndex, false)
        val elapsedLineColor = getElapsedLineColor(chartType, localCustomColors)

        // Day & Night Background
        drawDayNightBackground(colorScheme, params, zeroYPixel)
        drawDayNightAreaFill(fillPath, colorScheme, zeroYPixel)

        // 3. Draw the Twilight Horizontal Bands
        drawDayNightHorizontalTwilights(fillPath, colors, params, zeroYPixel, ::mapY, chartType)

        // 4. Draw the full unclipped curve line for all values
        drawPath(
            path = curvePath,
            color = materialTheme.onSurfaceVariant,
            style = Stroke(width = (1.5).dp.toPx())
        )

        // --- Draw the elapsed path line on top ---
        drawPath(
            path = elapsedLinePath,
            color = elapsedLineColor,
            style = Stroke(width = 2.dp.toPx()) // I recommend 2.dp so it pops slightly over the base line
        )

        // 5. Draw a subtle X-Axis line to visually separate the zones (Horizon Line)
        drawLine(
            color = materialTheme.outline,
            start = Offset(0f, zeroYPixel),
            end = Offset(params.width, zeroYPixel),
            strokeWidth = (1.5).dp.toPx()
        )



        // 5a. Draw Vertical Legend (Y-axis Altitudes)
        drawYLabels(chartType, materialTheme, params, ::mapY, textMeasurer, labelStyle)

        // 5b. Draw Horizontal Legend (X-axis Hours)
        drawXLabels(chartType, materialTheme, params, ::mapX, textMeasurer, labelStyle, context, shiftTrajectory)

        // 6. Draw vertical drop line from Sun to Horizon (X-axis)
        drawLine(
            color = localCustomColors.dropLine,
            start = Offset(currentXPx, currentYPx),
            end = Offset(currentXPx, zeroYPixel),
            strokeWidth = 1.dp.toPx() // Using 1.dp keeps it crisp but visible across screen densities
        )

        // 7. Paint the Sun Icon if above horizon
        paintIcon(currentXPx, currentAltitude, currentYPx, zeroYPixel, chartType, drawChartIcon)
    }
}