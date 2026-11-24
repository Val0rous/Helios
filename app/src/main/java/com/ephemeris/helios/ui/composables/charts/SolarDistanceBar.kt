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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.utils.Charts

@Composable
fun SolarDistanceBar(
    distanceAu: Float,
    modifier: Modifier = Modifier,
    showTrackBorder: Boolean = false,
    borderColor: Color = MaterialTheme.colorScheme.surface,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkSurface = colorScheme.surface.luminance() < 0.5f

    // Earth's orbital extremes (~1.0000 ± 0.0167 AU: perihelion to aphelion)
    val minAu = 0.98329f
    val maxAu = 1.01671f
    val rangeAu = maxAu - minAu

    // Position fraction (0.0 = Perihelion, 0.5 = Mean 1.0000 AU, 1.0 = Aphelion)
    val fraction = ((distanceAu - minAu) / rangeAu).coerceIn(0f, 1f)

    // Perihelion Red -> symmetric neutral mean zone -> Aphelion Blue
    val nearColor = Color(0xFFD32F2F)
    val farColor = Color(0xFF0288D1)
    val neutralColor = lerp(colorScheme.surface, colorScheme.onSurfaceVariant, 0.34f)

    val sunIconDrawer = rememberChartIconDrawer(Charts.Sun.Daily.Elevation)
    val earthPainter = painterResource(id = R.drawable.ic_public)
    val oceanColor = if (isDarkSurface) Color(0xFF29B6F6) else Color(0xFF1E88E5)
    val landColor = if (isDarkSurface) Color(0xFF66BB6A) else Color(0xFF43A047)
    val rimColor = if (isDarkSurface) Color(0xFF4FC3F7) else Color(0xFF1565C0)

    Canvas(modifier = modifier.fillMaxSize()) {
        val borderWidth = if (showTrackBorder) 2.dp.toPx() else 0f
        val trackHeight = 8.dp.toPx()
        val outerTrackHeight = trackHeight + borderWidth * 2f
        val sunIconSize = 20.dp.toPx()
        val sunGap = 8.dp.toPx()
        val earthSize = 18.dp.toPx()
        val thumbCutoutRadius = earthSize * 0.42f + 2.dp.toPx()

        val centerY = size.height / 2f
        val trackTop = centerY - trackHeight / 2f
        val outerTrackTop = centerY - outerTrackHeight / 2f
        val trackStart = sunIconSize + sunGap
        val trackWidth = (size.width - trackStart).coerceAtLeast(0f)

        // Bind gradient explicitly to [trackStart, size.width] so red and blue are 100% symmetrical
        val gradientBrush = Brush.horizontalGradient(
            0.00f to nearColor,
            0.18f to lerp(nearColor, neutralColor, 0.35f),
            0.38f to neutralColor,
            0.62f to neutralColor,
            0.82f to lerp(farColor, neutralColor, 0.35f),
            1.00f to farColor,
            startX = trackStart + borderWidth,
            endX = size.width - borderWidth,
        )

        // Draw Sun Icon at the left (Perihelion side)
        translate(left = 0f, top = centerY - sunIconSize / 2f) {
            sunIconDrawer(sunIconSize, false)
        }

        // 1. Draw the symmetric Red -> Neutral -> Blue Gradient Track
        if (showTrackBorder) {
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(trackStart, outerTrackTop),
                size = Size(trackWidth, outerTrackHeight),
                cornerRadius = CornerRadius(outerTrackHeight / 2f),
            )
        }
        drawRoundRect(
            brush = gradientBrush,
            topLeft = Offset(trackStart + borderWidth, trackTop),
            size = Size((trackWidth - borderWidth * 2f).coerceAtLeast(0f), trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f),
        )

        // 2. Draw the non-rotating Earth Indicator Thumb (inset so it never clips or hits the Sun)
        val thumbTravel = (trackWidth - thumbCutoutRadius * 2f).coerceAtLeast(0f)
        val thumbX = trackStart + thumbCutoutRadius + (thumbTravel * fraction)
        val thumbCenter = Offset(thumbX, centerY)

        // Surface cutout ring so the track breaks cleanly around Earth
        drawCircle(
            color = borderColor,
            radius = thumbCutoutRadius,
            center = thumbCenter,
        )

        // Ocean fill behind continents
        drawCircle(
            color = oceanColor,
            radius = earthSize * 0.39f,
            center = thumbCenter,
        )

        // Upright (non-rotating) Earth continents via ic_public
        translate(
            left = thumbCenter.x - earthSize / 2f,
            top = thumbCenter.y - earthSize / 2f,
        ) {
            with(earthPainter) {
                draw(
                    size = Size(earthSize, earthSize),
                    colorFilter = ColorFilter.tint(landColor),
                )
            }
        }

        // Crisp blue globe rim over ic_public's circular outline
        drawCircle(
            color = rimColor,
            radius = earthSize * 0.375f,
            center = thumbCenter,
            style = Stroke(width = earthSize * 0.09f),
        )
    }
}
