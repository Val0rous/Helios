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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.LocalCustomColors
import kotlin.math.min

@Composable
fun ShadowDial(
    currentShadow: Float,
    isNight: Boolean,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 24.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val colors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    // 1. GEOMETRY: Non-linear custom mapping!
    // 20f acts as our visual "infinity" to finish the dial sweep beautifully.
    val steps = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f, 5f, 10f, 20f)

    var sweepFraction = 1f
    if (!isNight) {
        for (i in 0 until steps.size - 1) {
            if (currentShadow <= steps[i + 1]) {
                val range = steps[i + 1] - steps[i]
                val past = currentShadow - steps[i]
                val segmentFraction = past / range
                sweepFraction = (i + segmentFraction) / (steps.size - 1).toFloat()
                break
            }
        }
        if (currentShadow > 20f) sweepFraction = 1f
    }

    // 2. COLORIMETRY: Absolute Coloring exactly like DailyChart.kt
    val colorFraction = if (isNight) 1f else (currentShadow.coerceIn(0f, 10f) / 10f)
    // We force alpha to 1f so the dial is vibrant, not ghostly!
    val fillColor = colors.shadowLong.copy(alpha = 1f) //lerp(colors.shadowShort, colors.shadowLong, colorFraction).copy(alpha = 1f)
    val trackColor = colors.shadowShort.copy(alpha = 1f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = min(size.width, size.height) - strokePx
            val arcSize = Size(diameter, diameter)

            val topLeftOffset = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )

            val startAngle = 135f
            val sweepAngle = 270f

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active Swept Track
            val activeSweep = sweepFraction * sweepAngle
            val safeSweep = if (activeSweep > 0f && activeSweep < 1f) 1f else activeSweep

            drawArc(
                color = fillColor,
                startAngle = startAngle,
                sweepAngle = safeSweep,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Inset Triangle Marker
            val currentAngle = startAngle + safeSweep
            rotate(degrees = currentAngle, pivot = center) {
                val radius = arcSize.width / 2f
                val offsetPx = 2.dp.toPx()
                val innerEdgeX = center.x + radius - (strokePx / 2f) - offsetPx

                val triangleHeight = strokePx * 0.333f
                val triangleWidth = strokePx * 0.4f

                val path = Path().apply {
                    moveTo(innerEdgeX, center.y)
                    lineTo(innerEdgeX - triangleHeight, center.y - triangleWidth / 2f)
                    lineTo(innerEdgeX - triangleHeight, center.y + triangleWidth / 2f)
                    close()
                }

                val markerColor = colorScheme.onSurface.copy(alpha = 0.9f)
                val cornerRadius = strokePx * 0.04f

                drawPath(path = path, color = markerColor)
                drawPath(
                    path = path,
                    color = markerColor,
                    style = Stroke(
                        width = cornerRadius * 2f,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.cornerPathEffect(cornerRadius)
                    )
                )
            }
        }
    }
}