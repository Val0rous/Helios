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
import com.ephemeris.helios.ui.composables.cards.Alpenglow
import com.ephemeris.helios.ui.composables.cards.BlueHour
import com.ephemeris.helios.ui.composables.cards.ColorTemperature
import com.ephemeris.helios.ui.composables.cards.DailyPathCard
import com.ephemeris.helios.ui.composables.cards.Declination
import com.ephemeris.helios.ui.composables.cards.EquationOfTime
import com.ephemeris.helios.ui.composables.cards.GoldenHour
import com.ephemeris.helios.ui.composables.cards.Illuminance
import com.ephemeris.helios.ui.composables.cards.Irradiance
import com.ephemeris.helios.ui.composables.cards.PinkHour
import com.ephemeris.helios.ui.composables.cards.PlutoTime
import com.ephemeris.helios.ui.composables.cards.ShadowRatio
import com.ephemeris.helios.ui.composables.cards.SmallCardRow
import com.ephemeris.helios.ui.composables.cards.SolarDistance
import com.ephemeris.helios.ui.composables.cards.SolarNoon
import com.ephemeris.helios.ui.composables.cards.SolarSpeed
import com.ephemeris.helios.ui.composables.cards.TwilightCard
import com.ephemeris.helios.ui.composables.cards.UvIntensity
import com.ephemeris.helios.ui.composables.charts.DecalobeShape
import com.ephemeris.helios.ui.composables.entries.NightEntry
import com.ephemeris.helios.ui.composables.entries.SeasonalEntry
import com.ephemeris.helios.ui.composables.entries.SunriseSunsetEntry
import com.ephemeris.helios.ui.theme.LocalCustomColors
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
                rightFullBleed = true,
                forceSquare = true,
                leftCard = {
                    SunriseSunsetEntry(
                        sunriseTime = sun.events.sunrise.formatDecimalHours(),
                        sunsetTime = sun.events.sunset.formatDecimalHours(),
                        sunriseAzimuth = sun.events.sunriseAzimuth.round(),
                        sunsetAzimuth = sun.events.sunsetAzimuth.round()
                    )
                },
                rightCard = {
                    SolarNoon(
                        noonTime = sun.events.solarNoon,
                        noonAzimuth = sun.events.solarNoonAzimuth,
                        noonAltitude = sun.events.solarNoonAltitude,
                        coordinates = ui.coordinates,
                        currentTime = ui.currentTime,
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
                rightCircular = true,
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
                        currentSpeedDegPerMin = sun.metrics.solarSpeed,
                        solarNoonAzimuth = sun.events.solarNoonAzimuth,
                    )
                }
            )
        }
        item {
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftFullBleed = true,
                rightFullBleed = true,
                leftCard = {
                    Declination(
                        currentDeclinationDeg = sun.position.declination,
                        currentTime = ui.currentTime,
                        seasonalEvents = sun.seasonalEvents,
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
        item {
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftFullBleed = true,
                rightFullBleed = true,
                forceSquare = true,
                leftCard = {
                    GoldenHour(
                        dawnLower = sun.events.dawnGoldenLower,
                        dawnUpper = sun.events.dawnGoldenUpper,
                        duskUpper = sun.events.duskGoldenUpper,
                        duskLower = sun.events.duskGoldenLower,
                        morningDuration = sun.durations.goldenHour.morning,
                        eveningDuration = sun.durations.goldenHour.evening,
                        currentTime = ui.currentTime,
                        phaseIntervals = sun.events.goldenIntervals,
                    )
                },
                rightCard = {
                    Alpenglow(
                        dawnLower = sun.events.dawnAlpenglowLower,
                        dawnUpper = sun.events.dawnAlpenglowUpper,
                        duskUpper = sun.events.duskAlpenglowUpper,
                        duskLower = sun.events.duskAlpenglowLower,
                        morningDuration = sun.durations.alpenglow.morning,
                        eveningDuration = sun.durations.alpenglow.evening,
                        currentTime = ui.currentTime,
                        phaseIntervals = sun.events.alpenglowIntervals,
                    )
                },
            )
        }
        item {
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftFullBleed = true,
                rightFullBleed = true,
                forceSquare = true,
                leftCard = {
                    PinkHour(
                        dawnLower = sun.events.dawnPinkLower,
                        dawnUpper = sun.events.dawnPinkUpper,
                        duskUpper = sun.events.duskPinkUpper,
                        duskLower = sun.events.duskPinkLower,
                        morningDuration = sun.durations.pinkHour.morning,
                        eveningDuration = sun.durations.pinkHour.evening,
                        currentTime = ui.currentTime,
                        phaseIntervals = sun.events.pinkIntervals,
                    )
                },
                rightCard = {
                    BlueHour(
                        dawnLower = sun.events.dawnBlueLower,
                        dawnUpper = sun.events.dawnBlueUpper,
                        duskUpper = sun.events.duskBlueUpper,
                        duskLower = sun.events.duskBlueLower,
                        morningDuration = sun.durations.blueHour.morning,
                        eveningDuration = sun.durations.blueHour.evening,
                        currentTime = ui.currentTime,
                        phaseIntervals = sun.events.blueIntervals,
                    )
                },
            )
        }

        item {
            TwilightCard(
                sunrise = sun.events.sunrise,
                sunset = sun.events.sunset,
                dawnCivil = sun.events.dawnCivil,
                duskCivil = sun.events.duskCivil,
                dawnNautical = sun.events.dawnNautical,
                duskNautical = sun.events.duskNautical,
                dawnAstronomical = sun.events.dawnAstronomical,
                duskAstronomical = sun.events.duskAstronomical,
                civilMorningDuration = sun.durations.civilTwilight.morning,
                civilEveningDuration = sun.durations.civilTwilight.evening,
                nauticalMorningDuration = sun.durations.nauticalTwilight.morning,
                nauticalEveningDuration = sun.durations.nauticalTwilight.evening,
                astroMorningDuration = sun.durations.astronomicalTwilight.morning,
                astroEveningDuration = sun.durations.astronomicalTwilight.evening,
                currentTime = ui.currentTime,
                civilIntervals = sun.events.civilIntervals,
                nauticalIntervals = sun.events.nauticalIntervals,
                astroIntervals = sun.events.astroIntervals,
            )
        }

        item {
            SmallCardRow(
                rightFullBleed = true,
                forceSquare = true,
                leftCard = {
                    NightEntry(
                        duration = sun.durations.night.total.formatDuration(true),
                        midnightTime = sun.events.solarMidnight.formatDecimalHours(),
                        midnightAzimuth = sun.events.solarMidnightAzimuth.round(),
                        midnightAltitude = sun.events.solarMidnightAltitude.round()
                    )
                },
                rightCard = {
                    PlutoTime(
                        dawnLower = sun.events.dawnPlutoLower,
                        dawnUpper = sun.events.dawnPlutoUpper,
                        duskUpper = sun.events.duskPlutoUpper,
                        duskLower = sun.events.duskPlutoLower,
                        morningDuration = sun.durations.plutoTime.morning,
                        eveningDuration = sun.durations.plutoTime.evening,
                        currentTime = ui.currentTime,
                        phaseIntervals = sun.events.plutoIntervals,
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
