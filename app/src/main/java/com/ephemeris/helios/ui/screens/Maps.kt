package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.ephemeris.helios.ui.composables.InfoPager
import com.ephemeris.helios.ui.composables.maps.FullscreenAzimuthMap
import com.ephemeris.helios.ui.composables.maps.FullscreenShadeMap
import com.ephemeris.helios.ui.composables.cards.ChartArrays
import com.ephemeris.helios.utils.MapType
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.location.Coordinates

@Composable
fun Maps(
    coordinates: Coordinates,
    currentSolarPosition: SolarEphemeris.SolarPosition,
    solarEvents: SolarEphemeris.DailyEvents,
    sunChartArrays: ChartArrays?,
    currentLunarPosition: LunarEphemeris.LunarPosition,
    lunarEvents: LunarEphemeris.LunarDailyEvents,
    moonChartArrays: ChartArrays?,
    onMapCenterSettled: (Coordinates) -> Unit
) {
    var selectedMapType by rememberSaveable { mutableIntStateOf(MapType.AZIMUTH.ordinal) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
//            .clip(RoundedCornerShape(16.dp))
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
//            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedMapType) {
                MapType.AZIMUTH.ordinal -> {
                    FullscreenAzimuthMap(
                        location = coordinates,
                        currentSolarPosition = currentSolarPosition,
                        currentLunarPosition = currentLunarPosition,
                        solarEvents = solarEvents,
                        lunarEvents = lunarEvents,
                        sunChartArrays = sunChartArrays,
                        moonChartArrays = moonChartArrays,
                        onMapCenterSettled = onMapCenterSettled
                    )
                }

                MapType.SHADEMAP.ordinal -> {
                    FullscreenShadeMap(
                        location = coordinates,
                        currentSolarPosition = currentSolarPosition,
                        onMapCenterSettled = onMapCenterSettled
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
            currentSunPosition = currentSolarPosition,
            sunEvents = solarEvents,
            currentMoonPosition = currentLunarPosition,
            moonEvents = lunarEvents,
        )
    }
}