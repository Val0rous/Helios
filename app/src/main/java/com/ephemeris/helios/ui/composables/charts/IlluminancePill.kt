package com.ephemeris.helios.ui.composables.charts

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.LocalCustomColors
import kotlin.math.log10

@Composable
fun IlluminancePill(
    currentIlluminance: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
) {
    val colors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    // --- LOGARITHMIC BASE-10 MATH ---
    // Max Lux is 100,000. log10(100,000) = 5.
    // We floor current lux at 1f so log10(0) doesn't crash to negative infinity!
    val isNight = currentIlluminance < 0.001f
    val safeLux = currentIlluminance.coerceAtLeast(1f)
    val fraction = (log10(safeLux) / 5f).coerceIn(0f, 1f)

    // Calculate a single solid color using the logarithmic fraction
    val fillColor = if (isNight) {
        colorScheme.onSurfaceVariant
    } else {
        lerp(colors.luxDim, colors.luxBright, fraction).copy(alpha = 1f)
    }

    // --- DYNAMIC TRACK COLOR (Matches UvDial) ---
    // If night, use a faint onSurface grey.
    // Otherwise, wash out the active fill color to 15% opacity!
    val trackColor = if (isNight) {
//        colorScheme.onSurface.copy(alpha = 0.05f)
        colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    } else {
        fillColor.copy(alpha = 0.15f)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Reserve space on the left side for the indicator triangle
        val indicatorSpace = 12.dp.toPx()
        val pillWidth = size.width - indicatorSpace
        val cornerRadius = CornerRadius(pillWidth / 2f)

        // 1. Draw Background Track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(indicatorSpace, 0f),
            size = Size(pillWidth, size.height),
            cornerRadius = cornerRadius
        )

        // Calculate clipping height
        val fillHeightY = size.height - (size.height * fraction)

        // 2. Draw Active Solid Fill
        clipRect(
            left = indicatorSpace,
            top = fillHeightY,
            right = size.width,
            bottom = size.height
        ) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(indicatorSpace, 0f),
                size = Size(pillWidth, size.height),
                cornerRadius = cornerRadius
            )
        }

        // 3. Draw Left-Side Indicator Triangle
        val triangleWidth = 6.dp.toPx()
        val triangleHeight = 8.dp.toPx()
        val tipX = indicatorSpace - 2.dp.toPx() // Touches the edge of the pill!

        val trianglePath = Path().apply {
            moveTo(tipX, fillHeightY) // Tip pointing right
            lineTo(tipX - triangleWidth, fillHeightY - triangleHeight / 2f)
            lineTo(tipX - triangleWidth, fillHeightY + triangleHeight / 2f)
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