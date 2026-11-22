package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ephemeris.helios.ui.composables.cards.AirMass
import com.ephemeris.helios.ui.composables.cards.ColorTemperature
import com.ephemeris.helios.ui.composables.cards.DailyPathCard
import com.ephemeris.helios.ui.composables.cards.Illuminance
import com.ephemeris.helios.ui.composables.cards.ShadowRatio
import com.ephemeris.helios.ui.composables.cards.SmallCardRow
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.ui.composables.entries.LunarCulminationEntry
import com.ephemeris.helios.ui.composables.entries.MoonriseMoonsetEntry
import com.ephemeris.helios.ui.viewmodels.MoonViewModel
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.datastore.MoonColorTempMode
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.round
import org.koin.androidx.compose.koinViewModel

@Composable
fun Moon(
    viewModel: MoonViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ui = state ?: return
    val moon = ui.moon

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            DailyPathCard(
                currentTime = ui.currentTime,
                liveTime = moon.timestamp,
                coordinates = ui.coordinates,
                dayLength = moon.events.moonDuration,
                currentAltitude = moon.altitude,
                currentAzimuth = moon.azimuth,
                phase = moon.phase,
                type = Charts.Moon.Daily.Elevation,
                chartArrays = moon.chartArrays,
                moonPhase = MoonPhaseAppearance.from(moon.metrics, ui.coordinates),
            )
        }
        item {
            SmallCardRow(
                leftCard = {
                    MoonriseMoonsetEntry(
                        moonriseTime = moon.events.moonrise.formatDecimalHours(),
                        moonsetTime = moon.events.moonset.formatDecimalHours(),
                        moonriseAzimuth = moon.events.moonriseAzimuth?.round(),
                        moonsetAzimuth = moon.events.moonsetAzimuth?.round()
                    )
                },
                rightCard = {
                    LunarCulminationEntry(
                        culminationTime = moon.events.culmination.formatDecimalHours(),
                        culminationAzimuth = moon.events.culminationAzimuth!!.round(),
                        culminationAltitude = moon.events.culminationAltitude!!.round()
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
                        currentIlluminance = moon.metrics.illuminanceFor(ui.illuminanceMode),
                        peakIlluminance = 0.0,
                        mode = ui.illuminanceMode,
                        onToggleMode = viewModel::cycleIlluminanceMode,
                        isLunar = true,
                    )
                },
                rightCard = {
                    ShadowRatio(
                        currentShadowRatio = moon.metrics.shadowRatio,
                        peakShadowRatio = 0.0,
                    )
                },
            )
        }
        item {
            val (ctMinK, ctMaxK) = when (ui.colorTempMode) {
                MoonColorTempMode.DIRECT -> 1800f to 4500f
                MoonColorTempMode.GLOBAL -> 1800f to 10000f
                MoonColorTempMode.DIFFUSE -> 5500f to 10000f
                MoonColorTempMode.PURKINJE -> 1800f to 8000f
            }
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftFullBleed = true,
                rightFullBleed = true,
                leftCard = {
                    ColorTemperature(
                        currentColorTemp = moon.metrics.colorTempFor(ui.colorTempMode),
                        peakColorTemp = 0.0,
                        minK = ctMinK,
                        maxK = ctMaxK,
                        modeLabel = ui.colorTempMode.label,
                        onToggleMode = viewModel::cycleColorTempMode,
                    )
                },
                rightCard = {
                    AirMass(
                        currentAirMass = moon.metrics.airMass,
                        minAirMass = 0.0,
                    )
                },
            )
        }
        item {
            Spacer(modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}
