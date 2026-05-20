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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
fun SolarDistanceBar(
    distanceAu: Float,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    // The Earth's exact orbital extremes
    val minAu = 0.983f // Perihelion (Closest - Early January)
    val maxAu = 1.017f // Aphelion (Furthest - Early July)
    val rangeAu = maxAu - minAu

    // Calculate fraction (0.0 = Near, 1.0 = Far)
    val fraction = ((distanceAu - minAu) / rangeAu).coerceIn(0f, 1f)

    // Orbital proximity colors
    val nearColor = Color(0xFFFF8C00) // Fiery Orange (Close/Hot)
    val farColor = Color(0xFF0288D1)  // Distant Blue (Far/Cool)

    // The exact color of the thumb on the track
    val activeColor = lerp(nearColor, farColor, fraction).copy(alpha = 1f)

    val gradientBrush = Brush.horizontalGradient(
        0.0f to nearColor,
        1.0f to farColor
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val trackHeight = 8.dp.toPx()
        val thumbRadius = 8.dp.toPx()

        val trackTop = (size.height - trackHeight) / 2f

        val usableWidth = size.width
        val trackStart = 0.dp.toPx()

        // 1. Draw the Background Gradient Track
        drawRoundRect(
            brush = gradientBrush,
            topLeft = Offset(trackStart, trackTop),
            size = Size(usableWidth, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f)
        )

        // 2. Draw the Indicator Thumb
        val thumbX = trackStart + (usableWidth * fraction)
        val thumbCenter = Offset(thumbX, size.height / 2f)

        // Opaque blocker to completely hide the gradient track underneath
//        drawCircle(
//            color = colorScheme.surface,
//            radius = thumbRadius - 2.dp.toPx(),
//            center = thumbCenter
//        )

        // Inner fill (Matches exact location color)
        drawCircle(
            color = activeColor,
            radius = thumbRadius - 2.dp.toPx(),
            center = thumbCenter
        )

        // Outer ring
        drawCircle(
            color = colorScheme.onSurface.copy(alpha = 0.9f),
            radius = thumbRadius - 1.dp.toPx(),
            center = thumbCenter,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}