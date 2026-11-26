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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.LunarDistanceBar
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.MoonMetrics
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/**
 * Returns `true` when [currentTime] falls within the `±12h` window surrounding lunar perigee
 * (strict local minimum of Earth–Moon distance).
 */
private fun isWithin24hOfPerigee(distanceKm: Double, currentTime: ZonedDateTime): Boolean {
    if (distanceKm > 371_000.0) return false
    val decreasingBefore = LunarEphemeris.calculateDistanceKm(currentTime.minusHours(12).minusMinutes(15)) >=
        LunarEphemeris.calculateDistanceKm(currentTime.minusHours(12).plusMinutes(15))
    val increasingAfter = LunarEphemeris.calculateDistanceKm(currentTime.plusHours(12).minusMinutes(15)) <=
        LunarEphemeris.calculateDistanceKm(currentTime.plusHours(12).plusMinutes(15))
    return decreasingBefore && increasingAfter
}

/**
 * Returns `true` when [currentTime] falls within the `±12h` window surrounding lunar apogee
 * (strict local maximum of Earth–Moon distance).
 */
private fun isWithin24hOfApogee(distanceKm: Double, currentTime: ZonedDateTime): Boolean {
    if (distanceKm < 403_000.0) return false
    val increasingBefore = LunarEphemeris.calculateDistanceKm(currentTime.minusHours(12).minusMinutes(15)) <=
        LunarEphemeris.calculateDistanceKm(currentTime.minusHours(12).plusMinutes(15))
    val decreasingAfter = LunarEphemeris.calculateDistanceKm(currentTime.plusHours(12).minusMinutes(15)) >=
        LunarEphemeris.calculateDistanceKm(currentTime.plusHours(12).plusMinutes(15))
    return increasingBefore && decreasingAfter
}

@Composable
fun LunarDistance(
    currentDistanceKm: Double,
    currentTime: ZonedDateTime,
    illuminationPercent: Double,
    phase: MoonMetrics.LunarPhase,
    moonPhase: MoonPhaseAppearance,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f

    val isFullWindow = phase == MoonMetrics.LunarPhase.FULL_MOON || illuminationPercent >= 98.0
    val isSupermoon = isFullWindow && currentDistanceKm <= 360_000.0
    val isMicromoon = isFullWindow && currentDistanceKm >= 405_000.0
    val isPerigee = isWithin24hOfPerigee(currentDistanceKm, currentTime)
    val isApogee = isWithin24hOfApogee(currentDistanceKm, currentTime)

    val description = when {
        isSupermoon -> "Supermoon"
        isMicromoon -> "Micromoon"
        isPerigee -> "Perigee · Larger"
        isApogee -> "Apogee · Smaller"
        currentDistanceKm <= 363_000.0 -> "Much Larger"
        currentDistanceKm <= 372_000.0 -> "Larger Than Usual"
        currentDistanceKm <= 379_000.0 -> "Slightly Larger"
        currentDistanceKm <= 389_000.0 -> "Average Size"
        currentDistanceKm <= 396_000.0 -> "Slightly Smaller"
        currentDistanceKm <= 403_000.0 -> "Smaller Than Usual"
        else -> "Much Smaller"
    }

    // Flat Material-compliant tint only during notable extremes (Supermoon / Micromoon / Perigee / Apogee)
    val perigeeColor = Color(0xFFD32F2F)
    val apogeeColor = Color(0xFF0288D1)
    val tintColor: Color? = when {
        isSupermoon -> perigeeColor.copy(alpha = if (isDarkSurface) 0.24f else 0.20f)
        isMicromoon -> apogeeColor.copy(alpha = if (isDarkSurface) 0.24f else 0.20f)
        isPerigee || currentDistanceKm <= 363_000.0 -> perigeeColor.copy(alpha = if (isDarkSurface) 0.14f else 0.11f)
        isApogee || currentDistanceKm >= 403_000.0 -> apogeeColor.copy(alpha = if (isDarkSurface) 0.14f else 0.11f)
        else -> null
    }

    val formattedKm = String.format(locale, "%,d", currentDistanceKm.roundToInt())

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
                text = stringResource(R.string.moon_distance),
                textColor = titleColor,
                icon = R.drawable.ic_orbit_filled,
                iconTint = titleColor,
                horizontalAlignment = Alignment.CenterHorizontally,
                iconSize = 19.dp,
                textSize = 15.sp,
                spacing = 6.dp,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart),
        ) {
            val valueStyle = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                fontFeatureSettings = "tnum",
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = buildAnnotatedString {
                    append(formattedKm)
                    withStyle(
                        SpanStyle(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        append("\u2009km")
                    }
                },
                style = valueStyle,
                maxLines = 1,
                softWrap = false,
            )

            LunarDistanceBar(
                distanceKm = currentDistanceKm.toFloat(),
                moonPhase = moonPhase,
                showTrackBorder = tintColor != null,
                borderColor = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
            )
        }

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
                .fillMaxWidth()
                .align(Alignment.BottomStart),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LunarDistanceTilePreview() {
    val sampleTime = ZonedDateTime.of(2026, 11, 22, 12, 0, 0, 0, ZoneOffset.UTC)

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
                    LunarDistance(
                        currentDistanceKm = 356_824.0,
                        currentTime = sampleTime,
                        illuminationPercent = 99.8,
                        phase = MoonMetrics.LunarPhase.FULL_MOON,
                        moonPhase = MoonPhaseAppearance(1.0f, isWaxing = true),
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    LunarDistance(
                        currentDistanceKm = 384_400.0,
                        currentTime = sampleTime,
                        illuminationPercent = 68.4,
                        phase = MoonMetrics.LunarPhase.WAXING_GIBBOUS,
                        moonPhase = MoonPhaseAppearance(0.684f, isWaxing = true),
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
                    LunarDistance(
                        currentDistanceKm = 361_400.0,
                        currentTime = sampleTime,
                        illuminationPercent = 24.0,
                        phase = MoonMetrics.LunarPhase.WAXING_CRESCENT,
                        moonPhase = MoonPhaseAppearance(0.24f, isWaxing = true),
                    )
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    LunarDistance(
                        currentDistanceKm = 406_120.0,
                        currentTime = sampleTime,
                        illuminationPercent = 99.6,
                        phase = MoonMetrics.LunarPhase.FULL_MOON,
                        moonPhase = MoonPhaseAppearance(1.0f, isWaxing = false),
                    )
                }
            }
        }
    }
}
