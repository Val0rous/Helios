package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.cards.AirMass
import com.ephemeris.helios.ui.composables.cards.ColorTemperature
import com.ephemeris.helios.ui.composables.cards.DailyPathCard
import com.ephemeris.helios.ui.composables.cards.Declination
import com.ephemeris.helios.ui.composables.cards.EquationOfTime
import com.ephemeris.helios.ui.composables.cards.Illuminance
import com.ephemeris.helios.ui.composables.cards.Irradiance
import com.ephemeris.helios.ui.composables.cards.ShadowRatio
import com.ephemeris.helios.ui.composables.cards.SmallCardRow
import com.ephemeris.helios.ui.composables.cards.SolarDistance
import com.ephemeris.helios.ui.composables.cards.SolarSpeed
import com.ephemeris.helios.ui.composables.cards.UvIntensity
import com.ephemeris.helios.ui.composables.charts.DecalobeShape
import com.ephemeris.helios.ui.composables.entries.DurationEntry
import com.ephemeris.helios.ui.composables.entries.NightEntry
import com.ephemeris.helios.ui.composables.entries.PlutoTimeEntry
import com.ephemeris.helios.ui.composables.entries.SeasonalEntry
import com.ephemeris.helios.ui.composables.entries.SolarNoonEntry
import com.ephemeris.helios.ui.composables.entries.SunriseSunsetEntry
import com.ephemeris.helios.ui.composables.entries.TwilightEntry
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.ui.viewmodels.SunViewModel
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.datastore.ColorTempMode
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.round
import org.koin.androidx.compose.koinViewModel

@Composable
fun Sun(
    viewModel: SunViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ui = state ?: return
    val sun = ui.sun

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            DailyPathCard(
                currentTime = ui.currentTime,
                liveTime = sun.timestamp,
                coordinates = ui.coordinates,
                dayLength = sun.events.dayLength,
                currentAltitude = sun.altitude,
                currentAzimuth = sun.azimuth,
                phase = sun.phase,
                type = Charts.Sun.Daily.Elevation,
                chartArrays = sun.chartArrays
            )
        }
        item {
            SmallCardRow(
                leftCard = {
                    SunriseSunsetEntry(
                        sunriseTime = sun.events.sunrise.formatDecimalHours(),
                        sunsetTime = sun.events.sunset.formatDecimalHours(),
                        sunriseAzimuth = sun.events.sunriseAzimuth.round(),
                        sunsetAzimuth = sun.events.sunsetAzimuth.round()
                    )
                },
                rightCard = {
                    SolarNoonEntry(
                        noonTime = sun.events.solarNoon.formatDecimalHours(),
                        noonAzimuth = sun.events.solarNoonAzimuth.round(),
                        noonAltitude = sun.events.solarNoonAltitude.round()
                    )
                },
                onRightClick = viewModel::jumpToSolarNoon,
            )
        }
        item {
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftCircular = true,
                rightFullBleed = true,
                rightShape = DecalobeShape,
                forceSquare = true,
                leftCard = {
                    Irradiance(
                        currentIrradiance = sun.metrics.irradianceFor(ui.irradianceMode),
                        peakIrradiance = sun.dailyPeakMetrics.irradianceFor(ui.irradianceMode),
                        mode = ui.irradianceMode,
                        onToggleMode = viewModel::cycleIrradianceMode,
                    )
                },
                rightCard = {
                    UvIntensity(
                        currentUvIntensity = sun.metrics.uvIntensity,
                        peakUvIntensity = sun.dailyPeakMetrics.uvIntensity
                    )
                }
            )
        }
        item {
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftFullBleed = true,
                rightCircular = true,
                leftCard = {
                    Illuminance(
                        currentIlluminance = sun.metrics.illuminanceFor(ui.illuminanceMode),
                        peakIlluminance = sun.dailyPeakMetrics.illuminanceFor(ui.illuminanceMode),
                        mode = ui.illuminanceMode,
                        onToggleMode = viewModel::cycleIlluminanceMode,
                    )
                },
                rightCard = {
                    ShadowRatio(
                        currentShadowRatio = sun.metrics.shadowRatio,
                        peakShadowRatio = sun.dailyPeakMetrics.shadowRatio,
                    )
                }
            )
        }
        item {
            val (ctMinK, ctMaxK) = when (ui.colorTempMode) {
                ColorTempMode.DIRECT -> 1800f to 5800f
                ColorTempMode.GLOBAL -> 3500f to 14000f
                ColorTempMode.DIFFUSE -> 5500f to 14000f
            }
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftFullBleed = true,
                rightFullBleed = true,
                leftCard = {
                    ColorTemperature(
                        currentColorTemp = sun.metrics.colorTempFor(ui.colorTempMode),
                        peakColorTemp = sun.dailyPeakMetrics.colorTempFor(ui.colorTempMode),
                        minK = ctMinK,
                        maxK = ctMaxK,
                        modeLabel = ui.colorTempMode.label,
                        onToggleMode = viewModel::cycleColorTempMode,
                    )
                },
                rightCard = {
                    AirMass(
                        currentAirMass = sun.metrics.airMass,
                        minAirMass = sun.dailyPeakMetrics.airMass
                    )
                }
            )
        }
        item {
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftFullBleed = true,
                forceSquare = true,
                leftCard = {
                    SolarDistance(
                        currentDistanceAu = sun.position.distanceAu,
                        currentDistanceKm = sun.metrics.distanceKm,
                        currentTime = ui.currentTime,
                        distanceUnit = ui.distanceUnit,
                        onToggleUnit = viewModel::toggleDistanceUnit,
                    )
                },
                rightCard = {
                    SolarSpeed(
                        currentSpeedDegPerMin = sun.metrics.solarSpeed
                    )
                }
            )
        }
        item {
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                rightFullBleed = true,
                forceSquare = true,
                leftCard = {
                    Declination(
                        currentDeclinationDeg = sun.position.declination,
                        currentTime = ui.currentTime
                    )
                },
                rightCard = {
                    EquationOfTime(
                        currentEotMinutes = sun.position.eotMinutes,
                        currentDeclinationDeg = sun.position.declination,
                        currentTime = ui.currentTime
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
                        morningStartTime = sun.events.dawnGoldenLower.formatDecimalHours(),
                        morningEndTime = sun.events.dawnGoldenUpper.formatDecimalHours(),
                        morningDuration = sun.durations.goldenHour.morning.formatDuration(),
                        eveningStartTime = sun.events.duskGoldenUpper.formatDecimalHours(),
                        eveningEndTime = sun.events.duskGoldenLower.formatDecimalHours(),
                        eveningDuration = sun.durations.goldenHour.evening.formatDuration()
                    )
                },
                rightCard = {
                    DurationEntry(
                        title = R.string.blue_hour,
                        color = blueHourColor,
                        morningStartTime = sun.events.dawnBlueLower.formatDecimalHours(),
                        morningEndTime = sun.events.dawnBlueUpper.formatDecimalHours(),
                        morningDuration = sun.durations.blueHour.morning.formatDuration(),
                        eveningStartTime = sun.events.duskBlueUpper.formatDecimalHours(),
                        eveningEndTime = sun.events.duskBlueLower.formatDecimalHours(),
                        eveningDuration = sun.durations.blueHour.evening.formatDuration()
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
                        morningStartTime = sun.events.dawnPinkLower.formatDecimalHours(),
                        morningEndTime = sun.events.dawnPinkUpper.formatDecimalHours(),
                        morningDuration = sun.durations.pinkHour.morning.formatDuration(),
                        eveningStartTime = sun.events.duskPinkUpper.formatDecimalHours(),
                        eveningEndTime = sun.events.duskPinkLower.formatDecimalHours(),
                        eveningDuration = sun.durations.pinkHour.evening.formatDuration()
                    )
                },
                rightCard = {
                    DurationEntry(
                        title = R.string.alpenglow,
                        color = alpenglowColor,
                        morningStartTime = sun.events.dawnAlpenglowLower.formatDecimalHours(),
                        morningEndTime = sun.events.dawnAlpenglowUpper.formatDecimalHours(),
                        morningDuration = sun.durations.alpenglow.morning.formatDuration(),
                        eveningStartTime = sun.events.duskAlpenglowUpper.formatDecimalHours(),
                        eveningEndTime = sun.events.duskAlpenglowLower.formatDecimalHours(),
                        eveningDuration = sun.durations.alpenglow.evening.formatDuration()
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
                        civilTime = sun.events.dawnCivil.formatDecimalHours(),
                        civilDuration = sun.durations.civilTwilight.morning.formatDuration(),
                        nauticalTime = sun.events.dawnNautical.formatDecimalHours(),
                        nauticalDuration = sun.durations.nauticalTwilight.morning.formatDuration(),
                        astroTime = sun.events.dawnAstronomical.formatDecimalHours(),
                        astroDuration = sun.durations.astronomicalTwilight.morning.formatDuration(),
                        nightTime = "",
                        nightDuration = ""
                    )
                },
                rightCard = {
                    TwilightEntry(
                        title = R.string.dusk,
                        subtitle = R.string.end,
                        civilTime = sun.events.duskCivil.formatDecimalHours(),
                        civilDuration = sun.durations.civilTwilight.evening.formatDuration(),
                        nauticalTime = sun.events.duskNautical.formatDecimalHours(),
                        nauticalDuration = sun.durations.nauticalTwilight.evening.formatDuration(),
                        astroTime = sun.events.duskAstronomical.formatDecimalHours(),
                        astroDuration = sun.durations.astronomicalTwilight.evening.formatDuration(),
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
                        duration = sun.durations.night.total.formatDuration(true),
                        midnightTime = sun.events.solarMidnight.formatDecimalHours(),
                        midnightAzimuth = sun.events.solarMidnightAzimuth.round(),
                        midnightAltitude = sun.events.solarMidnightAltitude.round()
                    )
                },
                rightCard = {
                    PlutoTimeEntry(
                        morningTime = sun.events.morningPlutoTime.formatDecimalHours(),
                        eveningTime = sun.events.eveningPlutoTime.formatDecimalHours()
                    )
                }
            )
        }

        item {
            SmallCardRow(
                leftCard = {
                    SeasonalEntry(
                        title = R.string.equinoxes,
                        color = MaterialTheme.colorScheme.primary,
                        dateTimeTop = sun.seasonalEvents.marchEquinox,
                        daylightTop = sun.seasonalDailyEvents.marchEquinoxDaylight,
                        sunAngleTop = sun.seasonalDailyEvents.marchEquinoxSunAngle,
                        dateTimeBottom = sun.seasonalEvents.septemberEquinox,
                        daylightBottom = sun.seasonalDailyEvents.septemberEquinoxDaylight,
                        sunAngleBottom = sun.seasonalDailyEvents.septemberEquinoxSunAngle
                    )
                },
                rightCard = {
                    SeasonalEntry(
                        title = R.string.solstices,
                        color = LocalCustomColors.current.nightPrimary,
                        dateTimeTop = sun.seasonalEvents.juneSolstice,
                        daylightTop = sun.seasonalDailyEvents.juneSolsticeDaylight,
                        sunAngleTop = sun.seasonalDailyEvents.juneSolsticeSunAngle,
                        dateTimeBottom = sun.seasonalEvents.decemberSolstice,
                        daylightBottom = sun.seasonalDailyEvents.decemberSolsticeDaylight,
                        sunAngleBottom = sun.seasonalDailyEvents.decemberSolsticeSunAngle
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}
