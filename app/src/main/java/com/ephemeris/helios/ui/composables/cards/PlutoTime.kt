package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.calc.SolarEphemeris
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Resolves Pluto Time intervals for the `-2.50° .. -1.90°` illumination band (~84–140 lux,
 * matching Pluto's mean-to-perihelion noon brightness). Always displays the exact interval
 * corresponding to the active highlight window so `Now` only lights up within the shown range.
 */
fun resolvePlutoIntervals(
    dawnLower: Double?,
    dawnUpper: Double?,
    duskUpper: Double?,
    duskLower: Double?,
    morningDuration: Double,
    eveningDuration: Double,
    currentTime: ZonedDateTime,
    phaseIntervals: List<SolarEphemeris.PhaseInterval>? = null,
): List<LightPhaseInterval> {
    return phaseIntervals?.let { resolveLightPhaseIntervals(it, currentTime) }
        ?: resolveLightPhaseIntervals(
            dawnLower = dawnLower,
            dawnUpper = dawnUpper,
            duskUpper = duskUpper,
            duskLower = duskLower,
            morningDuration = morningDuration,
            eveningDuration = eveningDuration,
            currentTime = currentTime,
        )
}

@Composable
fun PlutoTime(
    dawnLower: Double?,
    dawnUpper: Double?,
    duskUpper: Double?,
    duskLower: Double?,
    morningDuration: Double,
    eveningDuration: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier,
    phaseIntervals: List<SolarEphemeris.PhaseInterval>? = null,
) {
    val intervals = resolvePlutoIntervals(
        dawnLower = dawnLower,
        dawnUpper = dawnUpper,
        duskUpper = duskUpper,
        duskLower = duskLower,
        morningDuration = morningDuration,
        eveningDuration = eveningDuration,
        currentTime = currentTime,
        phaseIntervals = phaseIntervals,
    )

    // Pluto terracotta / Cthulhu Macula rust palette
    val tintSeed = Color(0xFFB85D3B)
    val lightAccent = Color(0xFF7D3419)
    val darkAccent = Color(0xFFFFAB91)

    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val accentColor = if (isDarkSurface) darkAccent else lightAccent
    val cardTint = tintSeed.copy(alpha = if (isDarkSurface) 0.15f else 0.14f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cardTint)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeaderEntry(
            text = stringResource(R.string.pluto_times),
            textColor = accentColor,
            icon = R.drawable.ic_planet_filled,
            iconTint = accentColor,
            horizontalAlignment = Alignment.CenterHorizontally,
            iconSize = 20.dp,
            textSize = 15.5.sp,
            spacing = 6.dp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                intervals.isEmpty() -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = surfaceColor.copy(alpha = if (isDarkSurface) 0.40f else 0.62f),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = "None today",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = "0h",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFeatureSettings = "tnum",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                ),
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }

                intervals.size == 1 -> {
                    LightPhaseIntervalChip(
                        interval = intervals.first(),
                        tintSeed = tintSeed,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        density = IntervalDensity.SINGLE,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                intervals.size == 2 -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        intervals.forEach { interval ->
                            LightPhaseIntervalChip(
                                interval = interval,
                                tintSeed = tintSeed,
                                accentColor = accentColor,
                                surfaceColor = surfaceColor,
                                isDarkSurface = isDarkSurface,
                                density = IntervalDensity.DOUBLE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        }
                    }
                }

                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        intervals.take(3).forEach { interval ->
                            LightPhaseIntervalChip(
                                interval = interval,
                                tintSeed = tintSeed,
                                accentColor = accentColor,
                                surfaceColor = surfaceColor,
                                isDarkSurface = isDarkSurface,
                                density = IntervalDensity.COMPACT,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        }
                    }
                }
            }
        }

        /*
        Text(
            text = stringResource(R.string.pluto_time_desc),
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            ),
        )
        */
    }
}

// Values are only an estimate. I've never been to Pluto, and you probably haven't either.

@Preview(showBackground = true)
@Composable
private fun PlutoTimeLightPreview() {
    val morningActive = ZonedDateTime.of(2026, 6, 15, 5, 22, 0, 0, ZoneOffset.UTC)
    val polarGrazeActive = ZonedDateTime.of(2026, 12, 21, 11, 50, 0, 0, ZoneOffset.UTC)
    val polarNight = ZonedDateTime.of(2026, 12, 21, 12, 0, 0, 0, ZoneOffset.UTC)

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
                    PlutoTime(
                        dawnLower = 5.333,
                        dawnUpper = 5.4,
                        duskUpper = 20.917,
                        duskLower = 20.983,
                        morningDuration = 0.067,
                        eveningDuration = 0.067,
                        currentTime = morningActive,
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    PlutoTime(
                        dawnLower = 11.25,
                        dawnUpper = null,
                        duskUpper = null,
                        duskLower = 12.75,
                        morningDuration = 0.75,
                        eveningDuration = 0.75,
                        currentTime = polarGrazeActive,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    PlutoTime(
                        dawnLower = 9.25,
                        dawnUpper = 9.583,
                        duskUpper = 14.417,
                        duskLower = 14.75,
                        morningDuration = 0.333,
                        eveningDuration = 0.333,
                        currentTime = polarNight,
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    PlutoTime(
                        dawnLower = null,
                        dawnUpper = null,
                        duskUpper = null,
                        duskLower = null,
                        morningDuration = 0.0,
                        eveningDuration = 0.0,
                        currentTime = polarNight,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlutoTimeDarkPreview() {
    val eveningActive = ZonedDateTime.of(2026, 6, 15, 20, 57, 0, 0, ZoneOffset.UTC)
    val polarOvernightActive = ZonedDateTime.of(2026, 5, 1, 0, 15, 0, 0, ZoneOffset.UTC)

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .background(Color(0xFF141416))
            .padding(16.dp),
    ) {
        HeliosTheme(darkTheme = true, dynamicColor = false) {
            Card(
                modifier = Modifier.size(168.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                PlutoTime(
                    dawnLower = 5.333,
                    dawnUpper = 5.4,
                    duskUpper = 20.917,
                    duskLower = 20.983,
                    morningDuration = 0.067,
                    eveningDuration = 0.067,
                    currentTime = eveningActive,
                )
            }
        }
        HeliosTheme(darkTheme = true, dynamicColor = false) {
            Card(
                modifier = Modifier.size(168.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                PlutoTime(
                    dawnLower = null,
                    dawnUpper = 1.25,
                    duskUpper = 22.75,
                    duskLower = null,
                    morningDuration = 1.25,
                    eveningDuration = 1.25,
                    currentTime = polarOvernightActive,
                )
            }
        }
    }
}
