package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.composables.cards.DailyComboPathCard
import com.ephemeris.helios.ui.composables.cards.DailyPathCard
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.location.Coordinates
import java.time.ZonedDateTime

@Composable
fun Home(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    currentSunPosition: SolarEphemeris.SolarPosition,
    sunEvents: SolarEphemeris.DailyEvents,
    currentMoonPosition: LunarEphemeris.LunarPosition,
    moonEvents: LunarEphemeris.LunarDailyEvents
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            DailyComboPathCard(
                currentTime = currentTime,
                coordinates = coordinates,
                dayLength = sunEvents.dayLength,
                currentSunAltitude = currentSunPosition.altitude,
                currentSunAzimuth = currentSunPosition.azimuth,
                currentMoonAltitude = currentMoonPosition.altitude,
                currentMoonAzimuth = currentMoonPosition.azimuth,
                type = Charts.SunMoonCombo.Daily.Elevation
            )
        }
    }
}