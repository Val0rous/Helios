package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.cards.PathCard
import com.ephemeris.helios.ui.composables.cards.SmallCardRow
import com.ephemeris.helios.ui.composables.entries.DailyPeaksEntry
import com.ephemeris.helios.ui.composables.entries.DurationEntry
import com.ephemeris.helios.ui.composables.entries.LiveMetricsEntry
import com.ephemeris.helios.ui.composables.entries.NightEntry
import com.ephemeris.helios.ui.composables.entries.PlutoTimeEntry
import com.ephemeris.helios.ui.composables.entries.SolarNoonEntry
import com.ephemeris.helios.ui.composables.entries.SunriseSunsetEntry
import com.ephemeris.helios.ui.composables.entries.TwilightEntry
import com.ephemeris.helios.ui.theme.CustomColorScheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.SolarEphemeris
import com.ephemeris.helios.utils.SunMetrics
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.round
import java.time.ZonedDateTime

@Composable
fun Sun(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    currentPosition: SolarEphemeris.SolarPosition,
    events: SolarEphemeris.DailyEvents,
    durations: SolarEphemeris.DailyDurations,
    dailyPeakMetrics: SunMetrics.SunMetricsResult,
    liveMetrics: SunMetrics.SunMetricsResult
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            PathCard(
                currentTime = currentTime,
                coordinates = coordinates,
                events = events,
                currentPosition = currentPosition
            )
        }
        item {
            SmallCardRow(
                leftCard = {
                    SunriseSunsetEntry(
                        sunriseTime = events.sunrise.formatDecimalHours(),
                        sunsetTime = events.sunset.formatDecimalHours(),
                        sunriseAzimuth = events.sunriseAzimuth?.round(),
                        sunsetAzimuth = events.sunsetAzimuth?.round()
                    )
                },
                rightCard = {
                    SolarNoonEntry(
                        noonTime = events.solarNoon.formatDecimalHours(),
                        noonAzimuth = events.solarNoonAzimuth.round(),
                        noonAltitude = events.solarNoonAltitude.round()
                    )
                }
            )
        }
        item {
            SmallCardRow(
                leftCard = {
                    LiveMetricsEntry(
                        irradiance = liveMetrics.irradiance,
                        uvIntensity = liveMetrics.uvIntensity,
                        luminance = liveMetrics.luminance,
                        shadowRatio = liveMetrics.shadowRatio
                    )
                },
                rightCard = {
                    DailyPeaksEntry(
                        irradiance = dailyPeakMetrics.irradiance,
                        uvIntensity = dailyPeakMetrics.uvIntensity,
                        luminance = dailyPeakMetrics.luminance,
                        shadowRatio = dailyPeakMetrics.shadowRatio
                    )
                }
            )
        }
        val goldenHourColor = MaterialColors.Amber500
        val blueHourColor = MaterialColors.Blue700
        item {
            SmallCardRow(
                leftCard = {
                    DurationEntry(
                        title = R.string.golden_hour,
                        color = goldenHourColor,
                        morningStartTime = events.dawnGoldenLower.formatDecimalHours(),
                        morningEndTime = events.dawnGoldenUpper.formatDecimalHours(),
                        morningDuration = durations.goldenHour.morning.formatDuration(),
                        eveningStartTime = events.duskGoldenUpper.formatDecimalHours(),
                        eveningEndTime = events.duskGoldenLower.formatDecimalHours(),
                        eveningDuration = durations.goldenHour.evening.formatDuration()
                    )
                },
                rightCard = {
                    DurationEntry(
                        title = R.string.blue_hour,
                        color = blueHourColor,
                        morningStartTime = events.dawnBlueLower.formatDecimalHours(),
                        morningEndTime = events.dawnBlueUpper.formatDecimalHours(),
                        morningDuration = durations.blueHour.morning.formatDuration(),
                        eveningStartTime = events.duskBlueUpper.formatDecimalHours(),
                        eveningEndTime = events.duskBlueLower.formatDecimalHours(),
                        eveningDuration = durations.blueHour.evening.formatDuration()
                    )
                }
            )
        }
        val pinkHourColor = MaterialColors.Pink500
        val alpenglowColor = MaterialColors.Red700
        item {
            SmallCardRow(
                leftCard = {
                    DurationEntry(
                        title = R.string.pink_hour,
                        color = pinkHourColor,
                        morningStartTime = events.dawnPinkLower.formatDecimalHours(),
                        morningEndTime = events.dawnPinkUpper.formatDecimalHours(),
                        morningDuration = durations.pinkHour.morning.formatDuration(),
                        eveningStartTime = events.duskPinkUpper.formatDecimalHours(),
                        eveningEndTime = events.duskPinkLower.formatDecimalHours(),
                        eveningDuration = durations.pinkHour.evening.formatDuration()
                    )
                },
                rightCard = {
                    DurationEntry(
                        title = R.string.alpenglow,
                        color = alpenglowColor,
                        morningStartTime = events.dawnAlpenglowLower.formatDecimalHours(),
                        morningEndTime = events.dawnAlpenglowUpper.formatDecimalHours(),
                        morningDuration = durations.alpenglow.morning.formatDuration(),
                        eveningStartTime = events.duskAlpenglowUpper.formatDecimalHours(),
                        eveningEndTime = events.duskAlpenglowLower.formatDecimalHours(),
                        eveningDuration = durations.alpenglow.evening.formatDuration()
                    )
                }
            )
        }

        item {
            SmallCardRow(
                leftCard = {
                    TwilightEntry(
                        title = R.string.dawn,
                        subtitle = R.string.start,
                        civilTime = events.dawnCivil.formatDecimalHours(),
                        civilDuration = durations.civilTwilight.morning.formatDuration(),
                        nauticalTime = events.dawnNautical.formatDecimalHours(),
                        nauticalDuration = durations.nauticalTwilight.morning.formatDuration(),
                        astroTime = events.dawnAstronomical.formatDecimalHours(),
                        astroDuration = durations.astronomicalTwilight.morning.formatDuration(),
                        nightTime = "",
                        nightDuration = ""
                    )
                },
                rightCard = {
                    TwilightEntry(
                        title = R.string.dusk,
                        subtitle = R.string.end,
                        civilTime = events.duskCivil.formatDecimalHours(),
                        civilDuration = durations.civilTwilight.evening.formatDuration(),
                        nauticalTime = events.duskNautical.formatDecimalHours(),
                        nauticalDuration = durations.nauticalTwilight.evening.formatDuration(),
                        astroTime = events.duskAstronomical.formatDecimalHours(),
                        astroDuration = durations.astronomicalTwilight.evening.formatDuration(),
                        nightTime = "",
                        nightDuration = ""
                    )
                }
            )
        }

        item {
            SmallCardRow(
                leftCard = {
                    NightEntry(
                        duration = durations.night.total.formatDuration(true),
                        midnightTime = events.solarMidnight.formatDecimalHours(),
                        midnightAzimuth = events.solarMidnightAzimuth.round(),
                        midnightAltitude = events.solarMidnightAltitude.round()
                    )
                },
                rightCard = {
                    PlutoTimeEntry(
                        morningTime = events.morningPlutoTime.formatDecimalHours(),
                        eveningTime = events.eveningPlutoTime.formatDecimalHours()
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}
