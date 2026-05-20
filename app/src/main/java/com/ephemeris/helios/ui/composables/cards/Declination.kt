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
import androidx.compose.ui.platform.LocalLocale
import com.ephemeris.helios.ui.composables.charts.DeclinationPill

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
        dec > 23.435 -> "Northern Solstice"
        dec > 20.29 -> "Peak North"
        dec > 16.57 -> "High North"
        dec > 11.72  -> "Mid North"
        dec > 6.06   -> "Low North"
        dec > 0.50 -> if (isHeadingNorth) "Leaving Equator" else "Reaching Equator"
        dec < -23.435 -> "Southern Solstice"
        dec < -20.29 -> "Peak South"
        dec < -16.57 -> "High South"
        dec < -11.72 -> "Mid South"
        dec < -6.06  -> "Low South"
        dec < -0.50 -> if (isHeadingNorth) "Reaching Equator" else "Leaving Equator"
        else -> "Celestial Equator"
    }

    val sign = if (dec > 0) "+" else if (dec < 0) "-" else ""

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        HeaderEntry(
            text = "Declination",
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

                Text(
                    text = "$sign${String.format(LocalLocale.current.platformLocale, "%.2f", abs(dec))}°",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Relative to Equator",
                    style = MaterialTheme.typography.labelSmall,
//                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            DeclinationPill(
                declination = currentDeclinationDeg.toFloat(),
                modifier = Modifier.width(40.dp).height(72.dp)
            )
        }
    }
}
