package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ephemeris.helios.ui.composables.InfoPager
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.ui.composables.maps.FullscreenAzimuthMap
import com.ephemeris.helios.ui.composables.maps.FullscreenShadeMap
import com.ephemeris.helios.ui.viewmodels.MapsViewModel
import com.ephemeris.helios.utils.MapType
import org.koin.androidx.compose.koinViewModel

@Composable
fun Maps(
    viewModel: MapsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ui = state ?: return
    val sun = ui.sun
    val moon = ui.moon
    val moonPhase = MoonPhaseAppearance.from(moon.metrics, ui.coordinates)

    var selectedMapType by rememberSaveable { mutableIntStateOf(MapType.AZIMUTH.ordinal) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedMapType,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            MapType.entries.forEachIndexed { index, mapType ->
                Tab(
                    selected = selectedMapType == index,
                    onClick = { selectedMapType = index },
                    text = {
                        Text(
                            text = mapType.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            when (selectedMapType) {
                MapType.AZIMUTH.ordinal -> {
                    FullscreenAzimuthMap(
                        location = ui.coordinates,
                        currentSolarPosition = sun.position,
                        currentLunarPosition = moon.position,
                        solarEvents = sun.events,
                        lunarEvents = moon.events,
                        sunChartArrays = sun.chartArrays,
                        moonChartArrays = moon.chartArrays,
                        moonPhase = moonPhase,
                        onMapCenterSettled = viewModel::onMapCenterSettled
                    )
                }

                MapType.SHADEMAP.ordinal -> {
                    FullscreenShadeMap(
                        location = ui.coordinates,
                        currentSolarPosition = sun.position,
                        onMapCenterSettled = viewModel::onMapCenterSettled
                    )
                }

                MapType.AR_VIEW.ordinal -> {
//                    PlaceholderScreen("Augmented Reality View Coming Soon")
                }

                MapType.COMPASS.ordinal -> {
//                    PlaceholderScreen("Native Hardware Compass Coming Soon")
                }
            }
        }
        InfoPager(
            currentSunPosition = sun.position,
            sunEvents = sun.events,
            currentMoonPosition = moon.position,
            moonEvents = moon.events,
            sunPhase = sun.phase,
            moonPhaseName = moon.phase,
            moonPhase = moonPhase,
        )
    }
}
