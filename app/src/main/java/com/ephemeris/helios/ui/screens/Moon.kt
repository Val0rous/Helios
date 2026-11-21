package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ephemeris.helios.ui.composables.cards.DailyPathCard
import com.ephemeris.helios.ui.composables.cards.SmallCardRow
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.ui.composables.entries.LunarCulminationEntry
import com.ephemeris.helios.ui.composables.entries.MoonriseMoonsetEntry
import com.ephemeris.helios.ui.viewmodels.MoonViewModel
import com.ephemeris.helios.utils.Charts
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
    }
}
