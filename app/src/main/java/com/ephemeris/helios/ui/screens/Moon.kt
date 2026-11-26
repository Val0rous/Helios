package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ephemeris.helios.ui.composables.cards.AirMass
import com.ephemeris.helios.ui.composables.cards.CollapsibleDailyPathScaffold
import com.ephemeris.helios.ui.composables.cards.ColorTemperature
import com.ephemeris.helios.ui.composables.cards.Declination
import com.ephemeris.helios.ui.composables.cards.Illuminance
import com.ephemeris.helios.ui.composables.cards.LunarCulmination
import com.ephemeris.helios.ui.composables.cards.LunarDistance
import com.ephemeris.helios.ui.composables.cards.LunarIllumination
import com.ephemeris.helios.ui.composables.cards.LunarLunisticeCard
import com.ephemeris.helios.ui.composables.cards.MoonriseMoonset
import com.ephemeris.helios.ui.composables.cards.ShadowRatio
import com.ephemeris.helios.ui.composables.cards.SmallCardRow
import com.ephemeris.helios.ui.composables.cards.SolarSpeed
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.ui.viewmodels.MoonViewModel
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.datastore.MoonColorTempMode
import com.ephemeris.helios.utils.datastore.MoonDeclinationMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun Moon(
    viewModel: MoonViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ui = state ?: return
    val moon = ui.moon
    val moonPhase = MoonPhaseAppearance.from(moon.metrics, ui.coordinates)

    CollapsibleDailyPathScaffold(
        currentTime = ui.currentTime,
        liveTime = moon.timestamp,
        coordinates = ui.coordinates,
        currentAltitude = moon.altitude,
        currentAzimuth = moon.azimuth,
        phase = moon.phase,
        type = Charts.Moon.Daily.Elevation,
        chartArrays = moon.chartArrays,
        moonPhase = moonPhase,
    ) {
        item {
            SmallCardRow(
                leftFullBleed = true,
                rightFullBleed = true,
                forceSquare = true,
                leftCard = {
                    MoonriseMoonset(
                        events = moon.events.riseSetEvents,
                        moonDurationHours = moon.events.moonDuration,
                        isMoonUpAllDay = moon.events.isMoonUpAllDay,
                    )
                },
                rightCard = {
                    LunarCulmination(
                        culminationTime = moon.events.culmination,
                        culminationAzimuth = moon.events.culminationAzimuth,
                        culminationAltitude = moon.events.culminationAltitude,
                        coordinates = ui.coordinates,
                        currentTime = ui.currentTime,
                    )
                },
                onRightClick = if (moon.events.culmination != null) viewModel::jumpToCulmination else null,
            )
        }
        item {
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftCircular = true,
                rightFullBleed = true,
                forceSquare = true,
                leftCard = {
                    LunarIllumination(
                        illuminationPercent = moon.metrics.illuminationPercent,
                        phase = moon.metrics.phase,
                        ageDays = moon.metrics.ageDays,
                        moonPhase = moonPhase,
                    )
                },
                rightCard = {
                    LunarDistance(
                        currentDistanceKm = moon.metrics.distanceKm,
                        currentTime = ui.currentTime,
                        illuminationPercent = moon.metrics.illuminationPercent,
                        phase = moon.metrics.phase,
                        moonPhase = moonPhase,
                    )
                },
            )
        }
        item {
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftFullBleed = true,
                rightCircular = true,
                leftCard = {
                    Illuminance(
                        currentIlluminance = moon.metrics.illuminanceFor(ui.illuminanceMode),
                        peakIlluminance = 0.0,
                        mode = ui.illuminanceMode.toIlluminanceMode(),
                        onToggleMode = viewModel::cycleIlluminanceMode,
                        isLunar = true,
                    )
                },
                rightCard = {
                    ShadowRatio(
                        currentShadowRatio = moon.metrics.shadowRatio,
                        peakShadowRatio = 0.0,
                    )
                },
            )
        }
        item {
            val (ctMinK, ctMaxK) = when (ui.colorTempMode) {
                MoonColorTempMode.PURKINJE -> 1800f to 8000f
                MoonColorTempMode.DIRECT -> 1800f to 4500f
            }
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftFullBleed = true,
                rightFullBleed = true,
                leftCard = {
                    ColorTemperature(
                        currentColorTemp = moon.metrics.colorTempFor(ui.colorTempMode),
                        peakColorTemp = 0.0,
                        minK = ctMinK,
                        maxK = ctMaxK,
                        modeLabel = ui.colorTempMode.label,
                        onToggleMode = viewModel::cycleColorTempMode,
                    )
                },
                rightCard = {
                    AirMass(
                        currentAirMass = moon.metrics.airMass,
                        minAirMass = 0.0,
                    )
                },
            )
        }
        item {
            val displayedDeclination = when (ui.declinationMode) {
                MoonDeclinationMode.GEOCENTRIC -> moon.position.declination
                MoonDeclinationMode.TOPOCENTRIC -> moon.position.topocentricDeclination
            }
            // Prefer upper meridian transit azimuth (strictly 180° South or 0°/360° North) so
            // near-zenith subtropical transits flip cleanly without distortion from declination drift.
            // On skipped-transit days, fall back to topocentric declination vs. observer latitude.
            val lunarCulminationAzimuth = moon.events.transitAzimuth
                ?: if (ui.coordinates.latitude >= moon.position.topocentricDeclination) 180.0 else 0.0
            SmallCardRow(
                disableInnerHorizontalPadding = true,
                leftFullBleed = true,
                rightCircular = true,
                leftCard = {
                    Declination(
                        currentDeclinationDeg = displayedDeclination,
                        currentTime = ui.currentTime,
                        isLunar = true,
                        geocentricDeclinationDeg = moon.position.declination,
                        modeLabel = ui.declinationMode.label,
                        onToggleMode = viewModel::cycleDeclinationMode,
                    )
                },
                rightCard = {
                    SolarSpeed(
                        currentSpeedDegPerMin = moon.position.lunarSpeedDegPerMin,
                        solarNoonAzimuth = lunarCulminationAzimuth,
                    )
                },
            )
        }
        item {
            LunarLunisticeCard(
                lunistices = moon.lunisticeEvents,
                currentTime = ui.currentTime,
                coordinates = ui.coordinates,
                onEventClick = viewModel::jumpToTime,
            )
        }
        item {
            Spacer(modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}
