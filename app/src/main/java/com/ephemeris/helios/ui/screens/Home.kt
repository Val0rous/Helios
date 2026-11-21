package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ephemeris.helios.ui.composables.cards.DailyComboPathCard
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.ui.viewmodels.HomeViewModel
import com.ephemeris.helios.utils.Charts
import org.koin.androidx.compose.koinViewModel

@Composable
fun Home(
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ui = state ?: return
    val sun = ui.sun
    val moon = ui.moon

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            DailyComboPathCard(
                currentTime = ui.currentTime,
                liveTime = sun.timestamp,
                coordinates = ui.coordinates,
                dayLength = sun.events.dayLength,
                moonDuration = moon.events.moonDuration,
                currentSunAltitude = sun.altitude,
                currentSunAzimuth = sun.azimuth,
                sunPhase = sun.phase,
                currentMoonAltitude = moon.altitude,
                currentMoonAzimuth = moon.azimuth,
                moonPhase = moon.phase,
                type = Charts.SunMoonCombo.Daily.Elevation,
                sunChartArrays = sun.chartArrays,
                moonChartArrays = moon.chartArrays,
                moonPhaseAppearance = MoonPhaseAppearance.from(moon.metrics, ui.coordinates),
            )
        }
    }
}
