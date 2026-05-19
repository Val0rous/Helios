package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.LocalCustomColors

@Composable
fun AirMassPill(
    currentAirMass: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    val isNight = currentAirMass < 0.1f

    // The visual fraction (1.0 = Sunrise full pill, 0.0 = Zenith empty pill)
    // By using 1.0 - (1 / AM), we completely linearize the exponential drop!
    val safeAM = currentAirMass.coerceAtLeast(1f)
    val fraction = if (isNight) 1f else (1f - (1f / safeAM)).coerceIn(0f, 1f)

    // The gradient represents the physical atmosphere.
    // Thickest/Horizon (Top of the pill) to Thinnest/Zenith (Bottom of the pill)
    val gradientBrush = Brush.verticalGradient(
        0.0f to colors.amHorizon, // Assuming you have horizon/extreme AM colors defined
        1.0f to colors.amZenith   // Assuming you have zenith/minimal AM colors defined
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val indicatorSpace = 12.dp.toPx()
        val pillWidth = size.width - indicatorSpace
        val cornerRadius = CornerRadius(pillWidth / 2f)

        // 1. Draw Background Track (The whole gradient, washed out to 20% opacity)
        if (isNight) {
            drawRoundRect(
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                topLeft = Offset(indicatorSpace, 0f),
                size = Size(pillWidth, size.height),
                cornerRadius = cornerRadius
            )
        } else {
            drawRoundRect(
                brush = gradientBrush,
                alpha = 0.2f, // This makes the "inactive" zone transparent but still colored!
                topLeft = Offset(indicatorSpace, 0f),
                size = Size(pillWidth, size.height),
                cornerRadius = cornerRadius
            )
        }

        // Calculate clipping height based on the reversed fraction
        // When fraction is 1.0 (Sunrise), fillHeightY is 0 (Pill is totally full)
        val fillHeightY = size.height - (size.height * fraction)

        // 2. Draw Active Solid Fill
        if (!isNight) {
            clipRect(
                left = indicatorSpace,
                top = fillHeightY,
                right = size.width,
                bottom = size.height
            ) {
                // We draw the EXACT same gradient, but at 100% opacity, constrained by the clip!
                drawRoundRect(
                    brush = gradientBrush,
                    topLeft = Offset(indicatorSpace, 0f),
                    size = Size(pillWidth, size.height),
                    cornerRadius = cornerRadius
                )
            }
        }

        // 3. Draw Left-Side Indicator Triangle
        val triangleWidth = 6.dp.toPx()
        val triangleHeight = 8.dp.toPx()
        val tipX = indicatorSpace - 2.dp.toPx()

        val trianglePath = Path().apply {
            moveTo(tipX, fillHeightY)
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