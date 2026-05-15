package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
fun IrradianceDial(
    currentIrradiance: Float,
    maxIrradiance: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 24.dp, // Easily scale the thickness of the ring
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable BoxScope.() -> Unit // Allows you to drop your text right into the middle!
) {
    val colors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    // 1. GEOMETRY: How far the dial sweeps (Relative to today's peak)
    val fraction = if (maxIrradiance > 0f) {
        (currentIrradiance / maxIrradiance).coerceIn(0f, 1f)
    } else 0f

    // 3. Lerp based on the absolute physical scale
    val fillColor = when {
        fraction <= 0.5f -> lerp(colors.irrLow, colors.irrMid, fraction * 2f)
        else -> lerp(colors.irrMid, colors.irrHigh, (fraction - 0.5f) * 2f)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()

            // 3. Force a perfect circle by calculating a square bounding box
            val diameter = min(size.width, size.height) - strokePx
            val arcSize = Size(diameter, diameter)

            // 4. Perfectly center the newly calculated square inside the available Canvas space
            val topLeftOffset = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )

            // Start at bottom-left (135 degrees) and sweep to bottom-right (270 degrees total)
            val startAngle = 135f
            val sweepAngle = 270f

            // 3. Draw the background track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // 4. Draw the filled track
            val activeSweep = fraction * sweepAngle

            // SKIA BUG FIX: Extremely small sweep angles with StrokeCap.Round
            // cause mathematical rendering artifacts (stray lines).
            // We snap tiny >0 values to a safe 1-degree minimum to bypass the glitch.
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

            // 5. Draw the inset triangle marker
            val currentAngle = startAngle + safeSweep
            rotate(degrees = currentAngle, pivot = center) {
                // Calculate exactly where the inner edge of the stroke is
                val radius = arcSize.width / 2f
                val offsetPx = 2.dp.toPx()
                val innerEdgeX = center.x + radius - (strokePx / 2f) - offsetPx

                // Scale the triangle proportionally to the stroke width!
                val triangleHeight = strokePx * 0.333f
                val triangleWidth = strokePx * 0.4f

                val path = Path().apply {
                    moveTo(innerEdgeX, center.y) // Tip touching the inner edge
                    lineTo(innerEdgeX - triangleHeight, center.y - triangleWidth / 2f) // Top corner
                    lineTo(innerEdgeX - triangleHeight, center.y + triangleWidth / 2f) // Bottom corner
                    close()
                }

                val markerColor = colorScheme.onSurface.copy(alpha = 0.9f)
                val cornerRadius = strokePx * 0.04f // Dynamic rounding based on dial size

                // 1. Fill the base triangle
                drawPath(path = path, color = markerColor)

                // 2. Wrap it in a rounded stroke to soften the sharp corners!
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

        // Render whatever text/UI you pass in perfectly centered
        content()
    }
}