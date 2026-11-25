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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

const val EOT_MIN_MINUTES = -14.3f
const val EOT_MAX_MINUTES = 16.4f

val EOT_FAST_COLOR = Color(0xFFD32F2F)
val EOT_SLOW_COLOR = Color(0xFF0288D1)

/**
 * Shared Equation-of-Time color mapping from slow (`-14.3m`, blue) through synchronized (`0m`, neutral)
 * to fast (`+16.4m`, red), keeping the track gradient and indicator thumb seamlessly aligned.
 */
fun colorForEot(eotMinutes: Float, neutralColor: Color): Color = when {
    eotMinutes > 0f -> lerp(neutralColor, EOT_FAST_COLOR, (eotMinutes / EOT_MAX_MINUTES).coerceIn(0f, 1f))
    eotMinutes < 0f -> lerp(neutralColor, EOT_SLOW_COLOR, (-eotMinutes / -EOT_MIN_MINUTES).coerceIn(0f, 1f))
    else -> neutralColor
}

@Composable
fun EquationOfTimeBar(
    eotMinutes: Float,
    activeColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.surface,
) {
    val colorScheme = MaterialTheme.colorScheme
    val neutralColor = lerp(colorScheme.surface, colorScheme.onSurfaceVariant, 0.38f)

    val range = EOT_MAX_MINUTES - EOT_MIN_MINUTES
    val zeroFraction = (-EOT_MIN_MINUTES / range).coerceIn(0f, 1f)
    val fraction = ((eotMinutes - EOT_MIN_MINUTES) / range).coerceIn(0f, 1f)

    val gradientBrush = Brush.horizontalGradient(
        0.00f to EOT_SLOW_COLOR,
        (zeroFraction * 0.55f) to lerp(EOT_SLOW_COLOR, neutralColor, 0.40f),
        zeroFraction to neutralColor,
        (zeroFraction + (1f - zeroFraction) * 0.45f) to lerp(EOT_FAST_COLOR, neutralColor, 0.40f),
        1.00f to EOT_FAST_COLOR,
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val borderWidth = 2.5.dp.toPx()
        val trackHeight = 8.dp.toPx()
        val outerTrackHeight = trackHeight + borderWidth * 2f
        val thumbOuterRadius = 8.dp.toPx()
        val thumbInnerRadius = 5.5.dp.toPx()

        val usableWidth = size.width
        val trackTop = (size.height - trackHeight) / 2f
        val outerTrackTop = (size.height - outerTrackHeight) / 2f

        // 1. Outer cutout capsule border so the slider stays crisp over the card's background tint
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(0f, outerTrackTop),
            size = Size(usableWidth, outerTrackHeight),
            cornerRadius = CornerRadius(outerTrackHeight / 2f),
        )

        // 2. Inner Slow (Blue) -> Neutral (0m) -> Fast (Red) gradient track
        drawRoundRect(
            brush = gradientBrush,
            topLeft = Offset(borderWidth, trackTop),
            size = Size((usableWidth - borderWidth * 2f).coerceAtLeast(0f), trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f),
        )

        // 3. Reference tick at 0m 0s (46.6% along the asymmetric -14.3m..+16.4m range)
        val thumbTravel = (usableWidth - thumbOuterRadius * 2f).coerceAtLeast(0f)
        val zeroX = thumbOuterRadius + (thumbTravel * zeroFraction)
        val tickHalfH = 6.5.dp.toPx()
        drawLine(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
            start = Offset(zeroX, size.height / 2f - tickHalfH),
            end = Offset(zeroX, size.height / 2f + tickHalfH),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // 4. Indicator thumb inset within track bounds
        val thumbX = thumbOuterRadius + (thumbTravel * fraction)
        val thumbCenter = Offset(thumbX, size.height / 2f)

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
