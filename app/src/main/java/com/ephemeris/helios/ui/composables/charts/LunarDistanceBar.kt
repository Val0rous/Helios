package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.LocalCustomColors

@Composable
fun LunarDistanceBar(
    distanceKm: Float,
    moonPhase: MoonPhaseAppearance,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.surface,
) {
    val colorScheme = MaterialTheme.colorScheme
    val customColors = LocalCustomColors.current
    val isDarkSurface = colorScheme.surface.luminance() < 0.5f

    // Earth-Moon orbital extremes (~356,400 km extreme perigee to ~406,800 km extreme apogee)
    val minKm = 356_400f
    val maxKm = 406_800f
    val rangeKm = maxKm - minKm

    // Position fraction (0.0 = Perigee, 0.5 = Midpoint ~381,600 km, 1.0 = Apogee)
    val fraction = ((distanceKm - minKm) / rangeKm).coerceIn(0f, 1f)

    // Perigee Red -> symmetric neutral average zone -> Apogee Blue
    val nearColor = Color(0xFFD32F2F)
    val farColor = Color(0xFF0288D1)
    val neutralColor = lerp(colorScheme.surface, colorScheme.onSurfaceVariant, 0.34f)

    val earthPainter = painterResource(id = R.drawable.ic_public)
    val oceanColor = if (isDarkSurface) Color(0xFF29B6F6) else Color(0xFF1E88E5)
    val landColor = if (isDarkSurface) Color(0xFF66BB6A) else Color(0xFF43A047)
    val rimColor = if (isDarkSurface) Color(0xFF4FC3F7) else Color(0xFF1565C0)
    val moonPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val borderWidth = 2.dp.toPx()
        val trackHeight = 8.dp.toPx()
        val outerTrackHeight = trackHeight + borderWidth * 2f
        val earthIconSize = 20.dp.toPx()
        val earthGap = 8.dp.toPx()
        val moonIconSize = 19.dp.toPx()
        val thumbCutoutRadius = moonIconSize * (10f / 24f) + 2.dp.toPx()

        val centerY = size.height / 2f
        val trackTop = centerY - trackHeight / 2f
        val outerTrackTop = centerY - outerTrackHeight / 2f
        val trackStart = earthIconSize + earthGap
        val trackWidth = (size.width - trackStart).coerceAtLeast(0f)

        // Bind gradient explicitly to [trackStart + borderWidth, size.width - borderWidth] so red and blue are 100% symmetrical
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

        // 1. Draw upright Earth icon on the left (Perigee side)
        val earthCenter = Offset(earthIconSize / 2f, centerY)
        drawCircle(
            color = oceanColor,
            radius = earthIconSize * 0.39f,
            center = earthCenter,
        )
        translate(left = 0f, top = centerY - earthIconSize / 2f) {
            with(earthPainter) {
                draw(
                    size = Size(earthIconSize, earthIconSize),
                    colorFilter = ColorFilter.tint(landColor),
                )
            }
        }
        drawCircle(
            color = rimColor,
            radius = earthIconSize * 0.375f,
            center = earthCenter,
            style = Stroke(width = earthIconSize * 0.09f),
        )

        // 2. Always draw the outer capsule border + inset track so track width never wobbles across card tint changes
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(trackStart, outerTrackTop),
            size = Size(trackWidth, outerTrackHeight),
            cornerRadius = CornerRadius(outerTrackHeight / 2f),
        )
        drawRoundRect(
            brush = gradientBrush,
            topLeft = Offset(trackStart + borderWidth, trackTop),
            size = Size((trackWidth - borderWidth * 2f).coerceAtLeast(0f), trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f),
        )

        // 3. Reference tick at mean lunar distance (384,400 km semi-major axis, mirroring 1 AU)
        val meanLunarKm = 384_400f
        val meanFraction = ((meanLunarKm - minKm) / rangeKm).coerceIn(0f, 1f)
        val thumbTravel = (trackWidth - thumbCutoutRadius * 2f).coerceAtLeast(0f)
        val meanX = trackStart + thumbCutoutRadius + (thumbTravel * meanFraction)
        val tickHalfH = 6.5.dp.toPx()
        drawLine(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
            start = Offset(meanX, centerY - tickHalfH),
            end = Offset(meanX, centerY + tickHalfH),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // 4. Draw our live-phase Moon icon as the Indicator Thumb
        val thumbX = trackStart + thumbCutoutRadius + (thumbTravel * fraction)
        val thumbCenter = Offset(thumbX, centerY)

        // Surface cutout ring so the track breaks cleanly around the Moon
        drawCircle(
            color = borderColor,
            radius = thumbCutoutRadius,
            center = thumbCenter,
        )

        translate(
            left = thumbCenter.x - moonIconSize / 2f,
            top = thumbCenter.y - moonIconSize / 2f,
        ) {
            drawMoonPhaseIcon(
                iconSize = moonIconSize,
                phase = moonPhase,
                fillColor = customColors.moon,
                borderColor = customColors.moonBorder,
                backgroundColor = customColors.moonUnlit,
                isDimmed = false,
                reusablePath = moonPath,
            )
        }
    }
}
