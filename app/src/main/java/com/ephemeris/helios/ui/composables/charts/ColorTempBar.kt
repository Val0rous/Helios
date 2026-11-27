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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.CustomColorScheme
import com.ephemeris.helios.ui.theme.LocalCustomColors

private val KELVIN_STOPS = floatArrayOf(2000f, 3000f, 4000f, 5500f, 8000f, 11000f, 14000f)

/**
 * Shared Kelvin-to-Color curve across the full solar/lunar/twilight domain (`1,800 K – 14,000 K`),
 * ensuring the card glow, gradient slider track, and active thumb dot match seamlessly.
 */
fun colorForKelvin(tempK: Float, customColors: CustomColorScheme): Color {
    val coolSilverBlue = lerp(customColors.ct5500, customColors.moonPath, 0.45f)
    val deepTwilightBlue = lerp(customColors.moonPath, customColors.luxDim, 0.55f)
    return when {
        tempK <= 2000f -> customColors.ct2000
        tempK <= 3000f -> lerp(customColors.ct2000, customColors.ct3000, (tempK - 2000f) / 1000f)
        tempK <= 4000f -> lerp(customColors.ct3000, customColors.ct4000, (tempK - 3000f) / 1000f)
        tempK <= 5500f -> lerp(customColors.ct4000, customColors.ct5500, (tempK - 4000f) / 1500f)
        tempK <= 8000f -> lerp(customColors.ct5500, coolSilverBlue, (tempK - 5500f) / 2500f)
        tempK <= 11000f -> lerp(coolSilverBlue, customColors.moonPath, (tempK - 8000f) / 3000f)
        tempK <= 14000f -> lerp(customColors.moonPath, deepTwilightBlue, (tempK - 11000f) / 3000f)
        else -> deepTwilightBlue
    }
}

@Composable
fun ColorTempBar(
    colorTemp: Float,
    isNight: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    minK: Float = 1800f,
    maxK: Float = 5800f,
    borderColor: Color = MaterialTheme.colorScheme.surface,
) {
    val colorScheme = MaterialTheme.colorScheme
    val customColors = LocalCustomColors.current

    val rangeK = (maxK - minK).coerceAtLeast(1f)

    // Calculate thumb position fraction (0.0 to 1.0)
    val fraction = if (isNight || (colorTemp <= minK)) {
        0f
    } else {
        ((colorTemp - minK) / rangeK).coerceIn(0f, 1f)
    }

    // Build exact color stops for the active [minK, maxK] domain
    val gradientStops = buildList {
        add(0.0f to colorForKelvin(minK, customColors))
        for (stopK in KELVIN_STOPS) {
            if (stopK > minK && stopK < maxK) {
                add(((stopK - minK) / rangeK) to colorForKelvin(stopK, customColors))
            }
        }
        add(1.0f to colorForKelvin(maxK, customColors))
    }.toTypedArray()

    val gradientBrush = Brush.horizontalGradient(*gradientStops)

    Canvas(modifier = modifier.fillMaxSize()) {
        val borderWidth = 2.5.dp.toPx()
        val trackHeight = 8.dp.toPx()
        val outerTrackHeight = trackHeight + borderWidth * 2f
        val thumbOuterRadius = 8.dp.toPx()
        val thumbInnerRadius = 5.5.dp.toPx()

        val usableWidth = size.width
        val trackTop = (size.height - trackHeight) / 2f
        val outerTrackTop = (size.height - outerTrackHeight) / 2f

        // 1. Always draw the outer capsule border + inset track so width never wobbles between day and night (N/A)
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(0f, outerTrackTop),
            size = Size(usableWidth, outerTrackHeight),
            cornerRadius = CornerRadius(outerTrackHeight / 2f),
        )
        if (isNight) {
            drawRoundRect(
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                topLeft = Offset(borderWidth, trackTop),
                size = Size((usableWidth - borderWidth * 2f).coerceAtLeast(0f), trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f),
            )
        } else {
            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(borderWidth, trackTop),
                size = Size((usableWidth - borderWidth * 2f).coerceAtLeast(0f), trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f),
            )
        }

        // 2. Draw the Indicator Thumb (insetting within track bounds so the outer halo never clips)
        val thumbTravel = (usableWidth - thumbOuterRadius * 2f).coerceAtLeast(0f)
        val thumbX = thumbOuterRadius + (thumbTravel * fraction)
        val thumbCenter = Offset(thumbX, size.height / 2f)

        if (!isNight && colorTemp > 0f) {
            // Outer cutout ring matching the slider border
            drawCircle(
                color = borderColor,
                radius = thumbOuterRadius,
                center = thumbCenter,
            )

            // Inner active-color indicator dot
            drawCircle(
                color = activeColor,
                radius = thumbInnerRadius,
                center = thumbCenter,
            )
        }
    }
}