package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Pluggable mapping from a raw metric value to a normalized `[0f, 1f]` dial sweep fraction.
 */
fun interface DialScale {
    fun toFraction(value: Float): Float

    companion object {
        /** Linear `[min..max]` mapping (e.g. Irradiance `0..950 W/m²`). */
        fun linear(max: Float, min: Float = 0f): DialScale = DialScale { value ->
            val span = max - min
            if (span <= 0f) 0f else ((value - min) / span).coerceIn(0f, 1f)
        }

        /**
         * Piecewise-linear mapping across monotonic breakpoints (e.g. Shadows),
         * allocating equal visual sweep angle to each consecutive interval.
         */
        fun piecewise(vararg stops: Float): DialScale = DialScale { value ->
            if (stops.size < 2) return@DialScale 0f
            if (value <= stops.first()) return@DialScale 0f
            if (value >= stops.last()) return@DialScale 1f
            val segments = (stops.size - 1).toFloat()
            for (i in 0 until stops.lastIndex) {
                if (value <= stops[i + 1]) {
                    val segFraction = (value - stops[i]) / (stops[i + 1] - stops[i])
                    return@DialScale ((i + segFraction) / segments).coerceIn(0f, 1f)
                }
            }
            1f
        }
    }
}

/**
 * Unified circular perimeter gauge dial (270° horseshoe arc starting at bottom-left `135°`)
 * with pluggable [DialScale], single-color active arc, neutral track, and centered inner [content].
 */
@Composable
fun CircularMetricDial(
    value: Float,
    scale: DialScale,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val fraction = scale.toFraction(value).coerceIn(0f, 1f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()

            // Force a perfect circle by calculating a square bounding box
            val diameter = min(size.width, size.height) - strokePx
            val arcSize = Size(diameter, diameter)

            // Perfectly center the square inside the available Canvas space
            val topLeftOffset = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f,
            )

            // Start at bottom-left (135°) and sweep clockwise 270° to bottom-right (45°)
            val startAngle = 135f
            val sweepAngle = 270f

            // Neutral background track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Single-color active fill track
            val activeSweep = fraction * sweepAngle

            // SKIA BUG FIX: Extremely small sweep angles with StrokeCap.Round
            // cause stray-line rendering artifacts; snap tiny >0 values to 1°.
            val safeSweep = if ((activeSweep > 0f) && (activeSweep < 1f)) 1f else activeSweep

            if (safeSweep > 0f) {
                drawArc(
                    color = fillColor,
                    startAngle = startAngle,
                    sweepAngle = safeSweep,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }

        content()
    }
}
