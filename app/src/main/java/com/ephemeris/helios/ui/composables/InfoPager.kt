package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.composables.cards.CelestialBodyTelemetry
import com.ephemeris.helios.ui.composables.cards.ChartTelemetryRow
import com.ephemeris.helios.ui.composables.cards.RiseSetTelemetryStrip
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.RiseSetEvent
import com.ephemeris.helios.utils.calc.SolarEphemeris

private fun resolveSunRiseSetEvents(sunEvents: SolarEphemeris.DailyEvents): List<RiseSetEvent> {
    if (sunEvents.riseSetEvents.isNotEmpty()) return sunEvents.riseSetEvents
    return buildList {
        sunEvents.sunrise?.takeIf { !it.isNaN() }?.let { hour ->
            add(RiseSetEvent(timeHour = hour, azimuth = sunEvents.sunriseAzimuth ?: Double.NaN, isRise = true))
        }
        sunEvents.sunset?.takeIf { !it.isNaN() }?.let { hour ->
            add(RiseSetEvent(timeHour = hour, azimuth = sunEvents.sunsetAzimuth ?: Double.NaN, isRise = false))
        }
    }.sortedBy { it.timeHour }
}

private fun resolveMoonRiseSetEvents(moonEvents: LunarEphemeris.LunarDailyEvents): List<RiseSetEvent> {
    if (moonEvents.riseSetEvents.isNotEmpty()) return moonEvents.riseSetEvents
    return buildList {
        moonEvents.moonrise?.takeIf { !it.isNaN() }?.let { hour ->
            add(RiseSetEvent(timeHour = hour, azimuth = moonEvents.moonriseAzimuth ?: Double.NaN, isRise = true))
        }
        moonEvents.moonset?.takeIf { !it.isNaN() }?.let { hour ->
            add(RiseSetEvent(timeHour = hour, azimuth = moonEvents.moonsetAzimuth ?: Double.NaN, isRise = false))
        }
    }.sortedBy { it.timeHour }
}

@Composable
fun InfoPager(
    currentSunPosition: SolarEphemeris.SolarPosition,
    sunEvents: SolarEphemeris.DailyEvents,
    currentMoonPosition: LunarEphemeris.LunarPosition,
    moonEvents: LunarEphemeris.LunarDailyEvents,
    sunPhase: String = "",
    moonPhaseName: String = "",
    moonPhase: MoonPhaseAppearance = MoonPhaseAppearance.Full,
    initialPage: Int = 0,
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { 2 }
    val resolvedSunEvents = resolveSunRiseSetEvents(sunEvents)
    val resolvedMoonEvents = resolveMoonRiseSetEvents(moonEvents)
    val isSunUpAllDay = resolvedSunEvents.isEmpty() && sunEvents.dayLength >= 12.0

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 12.dp,
            ) { page ->
                when (page) {
                    0 -> {
                        MapBodyTelemetryCard(
                            body = CelestialBodyTelemetry.SUN,
                            altitude = currentSunPosition.altitude,
                            azimuth = currentSunPosition.azimuth,
                            phase = sunPhase,
                            events = resolvedSunEvents,
                            isUpAllDay = isSunUpAllDay,
                        )
                    }
                    1 -> {
                        MapBodyTelemetryCard(
                            body = CelestialBodyTelemetry.MOON,
                            altitude = currentMoonPosition.altitude,
                            azimuth = currentMoonPosition.azimuth,
                            phase = moonPhaseName,
                            moonPhase = moonPhase,
                            events = resolvedMoonEvents,
                            isUpAllDay = moonEvents.isMoonUpAllDay,
                        )
                    }
                }
            }
            DotsIndicator(
                totalDots = 2,
                selectedIndex = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MapBodyTelemetryCard(
    body: CelestialBodyTelemetry,
    altitude: Double,
    azimuth: Double,
    phase: String,
    events: List<RiseSetEvent>,
    isUpAllDay: Boolean = false,
    moonPhase: MoonPhaseAppearance = MoonPhaseAppearance.Full,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ChartTelemetryRow(
            body = body,
            altitude = altitude,
            azimuth = azimuth,
            phase = phase,
            moonPhase = moonPhase,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = 5.dp,
                bottomEnd = 5.dp,
            ),
        )
        RiseSetTelemetryStrip(
            body = body,
            events = events,
            isUpAllDay = isUpAllDay,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun InfoPagerCompactLightPreview() {
    HeliosTheme(darkTheme = false, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
        ) {
            // Standard 2-event Sun
            MapBodyTelemetryCard(
                body = CelestialBodyTelemetry.SUN,
                altitude = 54.3,
                azimuth = 178.6,
                phase = "Daylight",
                events = listOf(
                    RiseSetEvent(timeHour = 6.2, azimuth = 62.3, isRise = true),
                    RiseSetEvent(timeHour = 20.78, azimuth = 297.6, isRise = false),
                ),
            )
            // 3-event Arctic / near-midnight Moon
            MapBodyTelemetryCard(
                body = CelestialBodyTelemetry.MOON,
                altitude = -12.4,
                azimuth = 287.4,
                phase = "Waxing Gibbous",
                moonPhase = MoonPhaseAppearance(illuminationFraction = 0.78f, isWaxing = true),
                events = listOf(
                    RiseSetEvent(timeHour = 0.18, azimuth = 351.2, isRise = true),
                    RiseSetEvent(timeHour = 10.42, azimuth = 228.5, isRise = false),
                    RiseSetEvent(timeHour = 23.86, azimuth = 349.8, isRise = true),
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun InfoPagerDarkPreview() {
    HeliosTheme(darkTheme = true, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
        ) {
            // 0-event Polar Night Sun
            MapBodyTelemetryCard(
                body = CelestialBodyTelemetry.SUN,
                altitude = -14.8,
                azimuth = 307.2,
                phase = "Astronomical Twilight",
                events = emptyList(),
                isUpAllDay = false,
            )
            // 1-event Moon (only Moonset today)
            MapBodyTelemetryCard(
                body = CelestialBodyTelemetry.MOON,
                altitude = 38.9,
                azimuth = 234.7,
                phase = "Waning Crescent",
                moonPhase = MoonPhaseAppearance(illuminationFraction = 0.22f, isWaxing = false),
                events = listOf(
                    RiseSetEvent(timeHour = 14.32, azimuth = 247.1, isRise = false),
                ),
            )
        }
    }
}
