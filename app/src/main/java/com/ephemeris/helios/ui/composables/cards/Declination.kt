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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.TextEntry
import com.ephemeris.helios.utils.round
import java.time.ZonedDateTime
import kotlin.math.abs

@Composable
fun Declination(
    currentDeclinationDeg: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier
) {
    val dayOfYear = currentTime.dayOfYear

    // The sun heads North from the Winter Solstice (~Dec 21, day 355) to the Summer Solstice (~Jun 21, day 172)
    val isHeadingNorth = dayOfYear in 1..172 || dayOfYear >= 355

    val dec = currentDeclinationDeg
    val description = when {
        dec > 23.3 -> "Northern Solstice"
        dec < -23.3 -> "Southern Solstice"
        abs(dec) < 0.5 -> "At Celestial Equator"
        dec > 0 -> if (isHeadingNorth) "Ascending North" else "Descending to Equator"
        else -> if (isHeadingNorth) "Ascending to Equator" else "Descending South"
    }

    val sign = if (dec > 0) "+" else if (dec < 0) "-" else ""

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        HeaderEntry(
            text = "Solar Declination",
            icon = R.drawable.ic_globe // Compass or globe icon
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$sign${abs(dec).round(2)}°",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "Relative to Equator",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DeclinationBar(
            declination = currentDeclinationDeg.toFloat(),
            modifier = Modifier.fillMaxWidth().height(24.dp)
        )
    }
}

@Composable
fun DeclinationBar(
    declination: Float,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val maxDec = 23.44f

    // Map -23.44 -> +23.44 to a 0.0 -> 1.0 fraction
    val fraction = ((declination + maxDec) / (maxDec * 2)).coerceIn(0f, 1f)

    // Cold Blue (South) to Hot Amber (North)
    val southColor = Color(0xFF0288D1)
    val northColor = Color(0xFFFF8C00)
    val activeColor = lerp(southColor, northColor, fraction)

    val gradientBrush = Brush.horizontalGradient(0.0f to southColor, 1.0f to northColor)

    Canvas(modifier = modifier.fillMaxSize()) {
        val trackHeight = 8.dp.toPx()
        val thumbRadius = 10.dp.toPx()
        val trackTop = (size.height - trackHeight) / 2f
        val usableWidth = size.width - (thumbRadius * 2)
        val trackStart = thumbRadius

        // 1. Gradient Track
        drawRoundRect(
            brush = gradientBrush,
            topLeft = Offset(trackStart, trackTop),
            size = Size(usableWidth, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f)
        )

        // 2. Center Equator Line
        val centerX = trackStart + (usableWidth / 2f)
        drawLine(
            color = colorScheme.onSurfaceVariant,
            start = Offset(centerX, trackTop - 4.dp.toPx()),
            end = Offset(centerX, trackTop + trackHeight + 4.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )

        // 3. Thumb Indicator
        val thumbX = trackStart + (usableWidth * fraction)
        val thumbCenter = Offset(thumbX, size.height / 2f)

        drawCircle(color = colorScheme.surface, radius = thumbRadius - 2.dp.toPx(), center = thumbCenter)
        drawCircle(color = activeColor, radius = thumbRadius - 2.dp.toPx(), center = thumbCenter)
        drawCircle(color = colorScheme.onSurface, radius = thumbRadius - 1.dp.toPx(), center = thumbCenter, style = Stroke(width = 2.dp.toPx()))
    }
}