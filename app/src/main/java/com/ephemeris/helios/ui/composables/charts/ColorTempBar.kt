package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.LocalCustomColors

@Composable
fun ColorTempBar(
    colorTemp: Float,
    isNight: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val isLightMode = !isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    val customColors = LocalCustomColors.current

    // Define our math boundaries
    val minK = 1800f
    val maxK = 5800f
    val rangeK = maxK - minK

    // Calculate thumb position fraction (0.0 to 1.0)
    val fraction = if (isNight || colorTemp <= minK) {
        0f
    } else {
        ((colorTemp - minK) / (maxK - minK)).coerceIn(0f, 1f)
    }

    // Mathematically tie the gradient stops to your GradientHelper's exact Kelvin values!
    // This perfectly mirrors the verticalGradient logic found in GradientHelper.kt
    val gradientBrush = Brush.horizontalGradient(
        0.0f to customColors.ct2000.copy(alpha = 1f),
        (3000f - minK) / rangeK to customColors.ct3000.copy(alpha = 1f),
        (4000f - minK) / rangeK to customColors.ct4000.copy(alpha = 1f),
        (5500f - minK) / rangeK to customColors.ct5500.copy(alpha = 1f),
        1.0f to customColors.ct5500.copy(alpha = 1f) // Cap at ct5500 for the 5800K peak, as in your helper
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val trackHeight = 8.dp.toPx()
        val thumbRadius = 8.dp.toPx()

        // Center the track vertically in the canvas
        val trackTop = (size.height - trackHeight) / 2f

        // Account for thumb overhang so the thumb doesn't clip off the sides
//        val usableWidth = size.width - (thumbRadius * 2)
//        val trackStart = thumbRadius
        val usableWidth = size.width
        val trackStart = 0.dp.toPx()

        // 1. Draw the Background Gradient Track
        if (isNight) {
            drawRoundRect(
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                topLeft = Offset(trackStart, trackTop),
                size = Size(usableWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f)
            )
        } else {
            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(trackStart, trackTop),
                size = Size(usableWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f)
            )
        }

        // 2. Draw the Indicator Thumb
        val thumbX = trackStart + (usableWidth * fraction)
        val thumbCenter = Offset(thumbX, size.height / 2f)

        if (isNight) {
//            // "Parked" disabled thumb to maintain visual height at night
//            drawCircle(
//                color = colorScheme.surface, // Matches background to look hollow
//                radius = thumbRadius - 2.dp.toPx(),
//                center = thumbCenter
//            )
//            drawCircle(
//                color = colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
//                radius = thumbRadius - 1.dp.toPx(),
//                center = thumbCenter,
//                style = Stroke(width = 2.dp.toPx())
//            )
        } else if (colorTemp > minK) {
            // 1. Inner fill (painted safely over the opaque blocker)
            drawCircle(
                color = activeColor,
                radius = thumbRadius - 2.dp.toPx(),
                center = thumbCenter
            )

            // 2. Outer ring (creates a crisp border so it pops against the gradient)
            drawCircle(
                color = colorScheme.onSurface.copy(alpha = 0.9f),
                radius = thumbRadius - 1.dp.toPx(),
                center = thumbCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}