package com.ephemeris.helios.ui.composables.cards
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.TextEntry
import com.ephemeris.helios.utils.round
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SolarSpeed(
    currentSpeedDegPerMin: Double, // Vertical rate: +/- degrees per minute
    modifier: Modifier = Modifier
) {
    val speedFloat = currentSpeedDegPerMin.toFloat()
    val absSpeed = abs(speedFloat)

    // Descriptions based on the physical curve of the sun's trajectory
    val description = when {
        absSpeed < 0.02f -> "Hanging" // Flat trajectory
        speedFloat > 0 -> when {
            speedFloat > 0.15f -> "Rapid Ascent"
            speedFloat > 0.08f -> "Climbing"
            else -> "Slow Ascent"
        }
        else -> when {
            speedFloat < -0.15f -> "Rapid Descent"
            speedFloat < -0.08f -> "Plunging"
            else -> "Slow Descent"
        }
    }

    val sign = if (speedFloat > 0) "+" else if (speedFloat < 0) "-" else ""

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        HeaderEntry(
            text = "Solar Velocity",
            icon = R.drawable.ic_speed_filled // Replace with your speedometer icon
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: The Data
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$sign${abs(speedFloat).toDouble().round(3)}°",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
//                    Text(
//                        text = " °/min",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
//                    )
                }
                Text(
                    text = "per minute",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                )
            }

            // RIGHT SIDE: Visual Angle Indicator (Mimicking the Color Temp layout)
            SpeedAngleIndicator(
                speedDegPerMin = speedFloat,
                modifier = Modifier.width(48.dp).height(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BOTTOM BAR: The horizontal magnitude track
        SolarSpeedBar(
            speedDegPerMin = speedFloat,
            modifier = Modifier.fillMaxWidth().height(16.dp)
        )
    }

    CustomHorizontalDivider()

    Row(Modifier.padding(horizontal = 12.dp)) {
        TextEntry(
            text = "0.25 °/min",
            textVariant = "",
            icon = R.drawable.ic_speed, // Or a limit/maximum icon
            desc = "Absolute Physical Limit"
        )
    }
}

@Composable
fun SpeedAngleIndicator(
    speedDegPerMin: Float,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    // Scale the speed to a visual UI angle.
    // Absolute physical max is 0.25. We map +/- 0.25 to a +/- 45 degree visual slope.
    val maxSpeed = 0.25f
    val visualAngleDeg = (speedDegPerMin / maxSpeed) * -45f // Negative to flip the Y-axis (Canvas goes down)

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        // Draw a faint background circle
        drawCircle(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
            radius = radius,
            center = center
        )

        // Draw the flat horizon reference line
        drawLine(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = 1.dp.toPx()
        )

        // Calculate the angled trajectory line
        val rad = Math.toRadians(visualAngleDeg.toDouble())
        val dx = (radius * cos(rad)).toFloat()
        val dy = (radius * sin(rad)).toFloat()

        drawLine(
            color = colorScheme.primary,
            start = Offset(center.x - dx, center.y - dy),
            end = Offset(center.x + dx, center.y + dy),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Add a small sun dot exactly at the center
        drawCircle(
            color = colorScheme.onSurface,
            radius = 3.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun SolarSpeedBar(
    speedDegPerMin: Float,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val maxSpeed = 0.25f

    // Convert -0.25 -> +0.25 range into a 0.0 -> 1.0 UI fraction
    val fraction = ((speedDegPerMin + maxSpeed) / (maxSpeed * 2)).coerceIn(0f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val trackHeight = 6.dp.toPx()
        val thumbRadius = 6.dp.toPx()
        val usableWidth = size.width - (thumbRadius * 2)
        val trackStart = thumbRadius
        val trackTop = (size.height - trackHeight) / 2f

        // Draw neutral background track
        drawRoundRect(
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            topLeft = Offset(trackStart, trackTop),
            size = Size(usableWidth, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f)
        )

        val centerX = trackStart + (usableWidth / 2f)
        val activeX = trackStart + (usableWidth * fraction)

        // Fill from the center (0) outwards to the current speed
        val startX = minOf(centerX, activeX)
        val fillWidth = abs(activeX - centerX)

        // Color coding: Blue for descending, Amber for ascending
        val activeColor = if (speedDegPerMin >= 0) Color(0xFFFF8C00) else Color(0xFF0288D1)

        drawRoundRect(
            color = activeColor,
            topLeft = Offset(startX, trackTop),
            size = Size(fillWidth, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f)
        )

        // Center zero-marker
        drawLine(
            color = colorScheme.onSurface,
            start = Offset(centerX, trackTop - 2.dp.toPx()),
            end = Offset(centerX, trackTop + trackHeight + 2.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
    }
}