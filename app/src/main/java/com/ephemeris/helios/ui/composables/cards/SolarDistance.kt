package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.SolarDistanceBar
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.TextEntry
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalLocale
import com.ephemeris.helios.utils.printSignificant
import java.time.ZonedDateTime

@Composable
fun SolarDistance(
    currentDistanceAu: Double,
    currentDistanceKm: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier
) {
//    // 1 AU is exactly 149,597,870.7 km
//    val distanceKm = currentDistanceAu * 149597870.7

    // Determine orbital momentum by combining actual distance with the calendar half.
    // Perihelion is ~0.983 AU. Aphelion is ~1.017 AU.
    val au = currentDistanceAu
    val isOutbound = currentTime.dayOfYear < 184 // Roughly Jan 4 to July 4

    // Scale split roughly into fifths (Total Range: ~0.983 to ~1.017)
    // 1. Inner Fifth (< 0.990): Near Perihelion
    // 2. Inner-Mid Fifth (0.990 - 0.997): Closer than average
    // 3. Center Fifth (0.997 - 1.003): Mean Distance Zone
    // 4. Outer-Mid Fifth (1.003 - 1.010): Further than average
    // 5. Outer Fifth (> 1.010): Near Aphelion
    val description = when {
        au <= 0.985 -> "At Perihelion"                // The absolute minimums (~Jan 2-6)
        au >= 1.015 -> "At Aphelion"                  // The absolute maximums (~July 2-6)
        isOutbound -> when {
            au < 0.990 -> "Departing Perihelion"      // Early Spring
            au < 0.997 -> "Closer Than Average"       // Mid Spring
            au <= 1.003 -> "Mean Distance (1 AU)"     // Exact Center (~April)
            au < 1.010 -> "Further Than Average"      // Late Spring
            else -> "Approaching Aphelion"            // Early Summer
        }
        else -> when {
            au > 1.010 -> "Departing Aphelion"        // Early Autumn
            au > 1.003 -> "Further Than Average"      // Mid Autumn
            au >= 0.997 -> "Mean Distance (1 AU)"     // Exact Center (~October)
            au > 0.990 -> "Closer Than Average"       // Late Autumn
            else -> "Approaching Perihelion"          // Early Winter
        }
    }

    // Calculate exact time it takes light to bridge this distance
    val lightSeconds = currentDistanceKm / 299792.458
    val minutes = (lightSeconds / 60).toInt()
    val seconds = (lightSeconds % 60).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        HeaderEntry(
            text = "Distance from Sun",
            icon = R.drawable.ic_orbit_filled // Use whichever orbit/distance icon you prefer
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE (Takes full width since there is no squircle)
            Column(horizontalAlignment = Alignment.Start) {
                // The new Momentum Description
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    // AU measurement
                    Text(
                        text = String.format(LocalLocale.current.platformLocale, "%.5f", currentDistanceAu),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "AU",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 0.dp, start = 8.dp)
                    )
                }
                // Formats with commas (e.g., 149,597,870)
                Text(
                    text = "${currentDistanceKm.printSignificant()} km",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

//                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // The horizontal line indicator component
        SolarDistanceBar(
            distanceAu = currentDistanceAu.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        )
    }

//    CustomHorizontalDivider()
    Column(Modifier.padding(horizontal = 12.dp)) {
        Text(
            text = "Light travels in ${minutes}m ${seconds}s",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}