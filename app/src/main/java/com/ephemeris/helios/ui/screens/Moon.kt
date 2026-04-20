package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.composables.cards.ChartArrays
import com.ephemeris.helios.ui.composables.cards.DailyPathCard
import com.ephemeris.helios.ui.composables.cards.SmallCardRow
import com.ephemeris.helios.ui.composables.entries.LunarCulminationEntry
import com.ephemeris.helios.ui.composables.entries.MoonriseMoonsetEntry
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.MoonMetrics
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.round
import java.time.ZonedDateTime

@Composable
fun Moon(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    currentPosition: LunarEphemeris.LunarPosition,
    events: LunarEphemeris.LunarDailyEvents,
    dailyPeakMetrics: MoonMetrics.LunarMetricsResult,
    liveMetrics: MoonMetrics.LunarMetricsResult,
    moonChartArrays: ChartArrays?
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            DailyPathCard(
                currentTime = currentTime,
                coordinates = coordinates,
                dayLength = 0.0,
                currentAltitude = currentPosition.altitude,
                currentAzimuth = currentPosition.azimuth,
                phase = liveMetrics.phase.displayName,
                type = Charts.Moon.Daily.Elevation,
                chartArrays = moonChartArrays
            )
        }
        item {
            SmallCardRow(
                leftCard = {
                    MoonriseMoonsetEntry(
                        moonriseTime = events.moonrise.formatDecimalHours(),
                        moonsetTime = events.moonset.formatDecimalHours(),
                        moonriseAzimuth = events.moonriseAzimuth?.round(),
                        moonsetAzimuth = events.moonsetAzimuth?.round()
                    )
                },
                rightCard = {
                    LunarCulminationEntry(
                        culminationTime = events.culmination.formatDecimalHours(),
                        culminationAzimuth = events.culminationAzimuth!!.round(),
                        culminationAltitude = events.culminationAltitude!!.round()
                    )
                }
            )
        }
    }
}