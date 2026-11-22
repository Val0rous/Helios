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
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.LocalCustomColors

@Composable
fun ColorTempBar(
    colorTemp: Float,
    isNight: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
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
        0.0f to customColors.ct2000,
        (3000f - minK) / rangeK to customColors.ct3000,
        (4000f - minK) / rangeK to customColors.ct4000,
        (5500f - minK) / rangeK to customColors.ct5500,
        1.0f to customColors.ct5500 // Cap at ct5500 for the 5800K peak, as in your helper
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val trackHeight = 8.dp.toPx()
        val thumbOuterRadius = 8.dp.toPx()
        val thumbInnerRadius = 5.5.dp.toPx()

        // Center the track vertically in the canvas
        val trackTop = (size.height - trackHeight) / 2f

        val usableWidth = size.width
        val trackStart = 0f

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

        // 2. Draw the Indicator Thumb (insetting within track bounds so the outer halo never clips)
        val thumbTravel = (usableWidth - thumbOuterRadius * 2f).coerceAtLeast(0f)
        val thumbX = trackStart + thumbOuterRadius + (thumbTravel * fraction)
        val thumbCenter = Offset(thumbX, size.height / 2f)

        if (!isNight && colorTemp > minK) {
            // 1. Outer cutout ring matching card surface (Google Weather AQI slider style)
            drawCircle(
                color = colorScheme.surface,
                radius = thumbOuterRadius,
                center = thumbCenter,
            )

            // 2. Inner active-color indicator dot
            drawCircle(
                color = activeColor,
                radius = thumbInnerRadius,
                center = thumbCenter,
            )
        }
    }
}