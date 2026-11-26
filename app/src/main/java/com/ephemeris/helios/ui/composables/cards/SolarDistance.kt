package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.SolarDistanceBar
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.MetricModePill
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.datastore.DistanceUnit
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Returns `true` when [currentTime] falls within the exact 24-hour window (`±12h`)
 * surrounding Earth's annual perihelion (strict local minimum of Earth–Sun distance).
 */
private fun isWithin24hOfPerihelion(au: Double, currentTime: ZonedDateTime): Boolean {
    if (au >= 0.9845) return false
    val decreasingBefore = SolarEphemeris.calculateDistanceAu(currentTime.minusHours(12).minusMinutes(15)) >=
        SolarEphemeris.calculateDistanceAu(currentTime.minusHours(12).plusMinutes(15))
    val increasingAfter = SolarEphemeris.calculateDistanceAu(currentTime.plusHours(12).minusMinutes(15)) <=
        SolarEphemeris.calculateDistanceAu(currentTime.plusHours(12).plusMinutes(15))
    return decreasingBefore && increasingAfter
}

/**
 * Returns `true` when [currentTime] falls within the exact 24-hour window (`±12h`)
 * surrounding Earth's annual aphelion (strict local maximum of Earth–Sun distance).
 */
private fun isWithin24hOfAphelion(au: Double, currentTime: ZonedDateTime): Boolean {
    if (au <= 1.0155) return false
    val increasingBefore = SolarEphemeris.calculateDistanceAu(currentTime.minusHours(12).minusMinutes(15)) <=
        SolarEphemeris.calculateDistanceAu(currentTime.minusHours(12).plusMinutes(15))
    val decreasingAfter = SolarEphemeris.calculateDistanceAu(currentTime.plusHours(12).minusMinutes(15)) >=
        SolarEphemeris.calculateDistanceAu(currentTime.plusHours(12).plusMinutes(15))
    return increasingBefore && decreasingAfter
}

@Composable
fun SolarDistance(
    currentDistanceAu: Double,
    currentDistanceKm: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier,
    distanceUnit: DistanceUnit = DistanceUnit.AU,
    onToggleUnit: () -> Unit = {},
) {
    val locale = LocalLocale.current.platformLocale
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val au = currentDistanceAu

    val isPerihelion = isWithin24hOfPerihelion(au, currentTime)
    val isAphelion = isWithin24hOfAphelion(au, currentTime)

    // Exact 24h peak detection for Perihelion/Aphelion, with concise orbital proximity tiers
    val description = when {
        isPerihelion -> "Perihelion"
        isAphelion -> "Aphelion"
        au < 0.9855 -> "Closest"
        au < 0.992 -> "Very Close"
        au < 0.998 -> "Closer"
        au <= 1.002 -> "Average"
        au < 1.008 -> "Further"
        au <= 1.0145 -> "Very Far"
        else -> "Furthest"
    }

    // Flat Material-compliant tint only during notable extremes (Perihelion / Aphelion / Closest / Furthest)
    val perihelionColor = Color(0xFFD32F2F)
    val aphelionColor = Color(0xFF0288D1)
    val tintColor: Color? = when {
        isPerihelion -> perihelionColor.copy(alpha = if (isDarkSurface) 0.24f else 0.20f)
        isAphelion -> aphelionColor.copy(alpha = if (isDarkSurface) 0.24f else 0.20f)
        au < 0.9855 -> perihelionColor.copy(alpha = if (isDarkSurface) 0.14f else 0.11f)
        au > 1.0145 -> aphelionColor.copy(alpha = if (isDarkSurface) 0.14f else 0.11f)
        else -> null
    }

    // AU always displays 5 decimal places (e.g., "0.98850", "1.01671"); km displays mln with 2 decimals
    val formattedNumber = if (distanceUnit == DistanceUnit.AU) {
        String.format(locale, "%.5f", currentDistanceAu)
    } else {
        String.format(locale, "%.2f", currentDistanceKm / 1_000_000.0)
    }

    /*
    // Calculate exact time it takes light to bridge this distance
    val lightSeconds = currentDistanceKm / 299792.458
    val minutes = (lightSeconds / 60).toInt()
    val seconds = (lightSeconds % 60).toInt()
    */

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (tintColor != null) Modifier.background(tintColor) else Modifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            val titleColor = MaterialTheme.colorScheme.onSurface
            HeaderEntry(
                text = stringResource(R.string.sun_distance),
                textColor = titleColor,
                icon = R.drawable.ic_straighten_filled,
                iconTint = titleColor,
                horizontalAlignment = Alignment.CenterHorizontally,
                iconSize = 20.dp,
                textSize = 16.sp,
                spacing = 8.dp,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart),
        ) {
            val valueStyle = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 25.5.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                fontFeatureSettings = "tnum",
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formattedNumber,
                    style = valueStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = if (distanceUnit == DistanceUnit.AU) "AU" else "mln km",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
            }

            /*
            // Formats with commas (e.g., 149,597,870)
            Text(
                text = "${currentDistanceKm.printSignificant()} km",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            */

            SolarDistanceBar(
                distanceAu = currentDistanceAu.toFloat(),
                showTrackBorder = tintColor != null,
                borderColor = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
            )
        }

        // Description at bottom-left; unit toggle pill anchored to bottom-right so it expands leftward without shifting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 4.dp),
            )
            MetricModePill(
                label = distanceUnit.label,
                onClick = onToggleUnit,
            )
        }

        /*
        Text(
            text = "Light travels in ${minutes}m ${seconds}s",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        */
    }
}

@Preview(showBackground = true)
@Composable
private fun SolarDistanceTilePreview() {
    val perihelionTime = ZonedDateTime.of(2026, 1, 3, 17, 0, 0, 0, ZoneOffset.UTC)
    val meanTime = ZonedDateTime.of(2026, 4, 3, 12, 0, 0, 0, ZoneOffset.UTC)
    val aphelionTime = ZonedDateTime.of(2026, 7, 4, 12, 0, 0, 0, ZoneOffset.UTC)
    val novTime = ZonedDateTime.of(2026, 11, 22, 12, 0, 0, 0, ZoneOffset.UTC)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .background(Color(0xFFECEEF4))
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarDistance(
                        currentDistanceAu = 0.98329,
                        currentDistanceKm = 147_100_000.0,
                        currentTime = perihelionTime,
                        distanceUnit = DistanceUnit.AU,
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarDistance(
                        currentDistanceAu = 0.98904,
                        currentDistanceKm = 147_950_000.0,
                        currentTime = novTime,
                        distanceUnit = DistanceUnit.KM,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarDistance(
                        currentDistanceAu = 1.00014,
                        currentDistanceKm = 149_610_000.0,
                        currentTime = meanTime,
                        distanceUnit = DistanceUnit.AU,
                    )
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarDistance(
                        currentDistanceAu = 1.01671,
                        currentDistanceKm = 152_090_000.0,
                        currentTime = aphelionTime,
                        distanceUnit = DistanceUnit.KM,
                    )
                }
            }
        }
    }
}
