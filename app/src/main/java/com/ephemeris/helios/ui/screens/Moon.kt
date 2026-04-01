package com.ephemeris.helios.ui.screens

import androidx.compose.runtime.Composable
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.MoonMetrics
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

}