package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ephemeris.helios.ui.composables.FullscreenMap
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.location.Coordinates

@Composable
fun Maps(
    coordinates: Coordinates,
    currentSolarPosition: SolarEphemeris.SolarPosition,
    solarEvents: SolarEphemeris.DailyEvents,
    currentLunarPosition: LunarEphemeris.LunarPosition,
    lunarEvents: LunarEphemeris.LunarDailyEvents
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
//            .clip(RoundedCornerShape(16.dp))
    ) {
        FullscreenMap(
            location = coordinates,
            currentSolarPosition = currentSolarPosition,
            solarEvents = solarEvents,
        )
    }
}