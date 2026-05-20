package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
fun DeclinationPill(
    declination: Float,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val maxDec = 23.44f

    // Map -23.44 -> +23.44 to a 0.0 (South) -> 1.0 (North) fraction
    val fraction = ((declination + maxDec) / (maxDec * 2)).coerceIn(0f, 1f)

    val southColor = Color(0xFF0288D1)
    val northColor = Color(0xFFFF8C00)
    val activeColor = lerp(southColor, northColor, fraction)

    Canvas(modifier = modifier.fillMaxSize()) {
        val indicatorSpace = 12.dp.toPx()
        val pillWidth = size.width - indicatorSpace
        val cornerRadius = CornerRadius(pillWidth / 2f)

        val centerY = size.height / 2f
        val thumbY = size.height - (size.height * fraction)

        // 1. Draw Neutral Background Track
        drawRoundRect(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
            topLeft = Offset(indicatorSpace, 0f),
            size = Size(pillWidth, size.height),
            cornerRadius = cornerRadius
        )

        // 2. Tint the Active Hemisphere's Background
        // If North (fraction > 0.5), tint the top half. If South, tint the bottom half.
        val isNorth = fraction >= 0.5f
        val bgClipTop = if (isNorth) 0f else centerY
        val bgClipBottom = if (isNorth) centerY else size.height

        clipRect(
            left = indicatorSpace,
            top = bgClipTop,
            right = size.width,
            bottom = bgClipBottom
        ) {
            drawRoundRect(
                color = activeColor.copy(alpha = 0.15f),
                topLeft = Offset(indicatorSpace, 0f),
                size = Size(pillWidth, size.height),
                cornerRadius = cornerRadius
            )
        }

        // 3. Draw Active Solid Fill (Clipped from equator to current declination)
        val clipTop = minOf(centerY, thumbY)
        val clipBottom = maxOf(centerY, thumbY)

        clipRect(
            left = indicatorSpace,
            top = clipTop,
            right = size.width,
            bottom = clipBottom
        ) {
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(indicatorSpace, 0f),
                size = Size(pillWidth, size.height),
                cornerRadius = cornerRadius
            )
        }

        // 4. Center Equator Marker (0°) - Spans ONLY the pill width
        drawLine(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            start = Offset(indicatorSpace, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 2.dp.toPx()
        )

        // 5. Draw Left-Side Indicator Triangle
        val triangleWidth = 6.dp.toPx()
        val triangleHeight = 8.dp.toPx()
        val tipX = indicatorSpace - 2.dp.toPx()

        val trianglePath = Path().apply {
            moveTo(tipX, thumbY)
            lineTo(tipX - triangleWidth, thumbY - triangleHeight / 2f)
            lineTo(tipX - triangleWidth, thumbY + triangleHeight / 2f)
            close()
        }

        val markerColor = colorScheme.onSurface.copy(alpha = 0.9f)

        drawPath(trianglePath, markerColor, style = Fill)
        drawPath(
            trianglePath,
            markerColor,
            style = Stroke(
                width = 2.dp.toPx(),
                join = StrokeJoin.Round,
                pathEffect = PathEffect.cornerPathEffect(2.dp.toPx())
            )
        )
    }
}