package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.composables.cards.PathCard
import com.ephemeris.helios.ui.composables.cards.SmallCardRow
import com.ephemeris.helios.ui.composables.entries.LunarCulminationEntry
import com.ephemeris.helios.ui.composables.entries.MoonriseMoonsetEntry
import com.ephemeris.helios.ui.composables.entries.SolarNoonEntry
import com.ephemeris.helios.ui.composables.entries.SunriseSunsetEntry
import com.ephemeris.helios.utils.Coordinates
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
    liveMetrics: MoonMetrics.LunarMetricsResult
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
//            PathCard(
//                currentTime = currentTime,
//                coordinates = coordinates,
//                events = events,
//                currentPosition = currentPosition
//            )
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