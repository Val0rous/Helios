package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.utils.calc.SolarEphemeris
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalemmaIndicator(
    currentEot: Float,
    currentDeclination: Float,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    // Fetch the flawless physical curve from the Meeus engine!
    // Remembering by year ensures it only calculates once when the app opens.
    val analemmaPoints = remember(currentTime.year) {
        SolarEphemeris.getUniversalAnalemma(currentTime.year)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Define exact data bounds to scale the drawing properly
        // We give it a tiny bit of padding (-17 to +17 on EoT)
        val minEoT = -15f
        val maxEoT = 17f
        val minDec = -24f
        val maxDec = 24f
//        val minEoT = -14.5f // Max slow (Mid-February)
//        val maxEoT = 16.5f  // Max fast (Early November)
//        val minDec = -23.44f // Winter Solstice
//        val maxDec = 23.44f  // Summer Solstice

        // Helper function to map data points (EoT, Declination) to Canvas (X, Y)
        fun mapToScreen(eot: Float, dec: Float): Offset {
            // X-axis: Equation of Time (Negative = Left/Slow, Positive = Right/Fast)
            val xFraction = (eot - minEoT) / (maxEoT - minEoT)
            val x = xFraction * width

            // Y-axis: Declination (Positive = Top/North, Negative = Bottom/South)
            // Note: Canvas Y runs top-to-bottom, so we invert the fraction!
            val yFraction = (dec - minDec) / (maxDec - minDec)
            val y = height - (yFraction * height)

            return Offset(x, y)
        }

        // 1a. Draw the "Mean Time" vertical centerline (0 minutes offset)
        val centerTop = mapToScreen(0f, maxDec)
        val centerBottom = mapToScreen(0f, minDec)
        drawLine(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            start = centerTop,
            end = centerBottom,
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 1b. Draw the "Zero Declination" horizontal centerline (0 degrees offset)
        val centerLeft = mapToScreen(minEoT, 0f)
        val centerRight = mapToScreen(maxEoT, 0f)
        drawLine(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            start = centerLeft,
            end = centerRight,
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Colors: Blue for Slow (-), Red for Fast (+)
        val slowColor = Color(0xFF0288D1)
        val fastColor = Color(0xFFD32F2F)

        val gradientBrush = Brush.horizontalGradient(
            0.0f to slowColor,
            1.0f to fastColor
        )

        // 2. Generate and draw the 365-day background Analemma path
        val analemmaPath = Path()
        analemmaPoints.forEachIndexed { index, point ->
            val screenPoint = mapToScreen(point.first.toFloat(), point.second.toFloat())
            if (index == 0) {
                analemmaPath.moveTo(screenPoint.x, screenPoint.y)
            } else {
                analemmaPath.lineTo(screenPoint.x, screenPoint.y)
            }
        }
        analemmaPath.close()

        drawPath(
            path = analemmaPath,
            brush = gradientBrush,
//            color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            alpha = 0.5f,
            style = Stroke(
                width = 2.dp.toPx(),
                join = StrokeJoin.Round
            )
        )

        // 3. Draw the exact current indicator thumb
        val currentPoint = mapToScreen(currentEot, currentDeclination)
        val thumbRadius = 4.dp.toPx()

        // Calculate the exact color of the dot based on its X-axis position
        val fraction = ((currentEot - minEoT) / (maxEoT - minEoT)).coerceIn(0f, 1f)
        val activeColor = lerp(slowColor, fastColor, fraction)

        // Thumb background blanker (cuts out the path underneath)
        drawCircle(
            color = colorScheme.onSurface.copy(alpha = 0.9f),
            radius = thumbRadius + 1.dp.toPx(),
            center = currentPoint
        )

        // Solid colored thumb
        drawCircle(
            color = activeColor,
            radius = thumbRadius,
            center = currentPoint
        )
    }
}