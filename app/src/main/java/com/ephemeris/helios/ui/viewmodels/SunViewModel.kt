package com.ephemeris.helios.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ephemeris.helios.data.EphemerisRepository
import com.ephemeris.helios.data.LocationRepository
import com.ephemeris.helios.data.SunDailyData
import com.ephemeris.helios.data.SunUiData
import com.ephemeris.helios.data.TimeRepository
import com.ephemeris.helios.utils.calc.SunLiveSnapshot
import com.ephemeris.helios.utils.datastore.AppSettings
import com.ephemeris.helios.utils.datastore.IlluminanceMode
import com.ephemeris.helios.utils.datastore.IrradianceMode
import com.ephemeris.helios.utils.datastore.SettingsDataStore
import com.ephemeris.helios.utils.getSunPhase
import com.ephemeris.helios.utils.location.Coordinates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.roundToLong

data class SunUiState(
    val currentTime: ZonedDateTime,
    val coordinates: Coordinates,
    val sun: SunUiData,
    val irradianceMode: IrradianceMode = IrradianceMode.DIRECT,
    val illuminanceMode: IlluminanceMode = IlluminanceMode.GLOBAL,
)

class SunViewModel(
    locationRepository: LocationRepository,
    private val timeRepository: TimeRepository,
    ephemerisRepository: EphemerisRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val uiState: StateFlow<SunUiState?> = combine(
        timeRepository.currentTime,
        locationRepository.coordinates,
        ephemerisRepository.dailyData,
        ephemerisRepository.liveUpdates,
        settingsDataStore.settingsFlow,
    ) { time, coords, daily, live, settings ->
        buildState(time, coords, daily?.sun, live?.sun, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildState(
            time = timeRepository.currentTime.value,
            coords = locationRepository.coordinates.value,
            sunDaily = ephemerisRepository.dailyData.value?.sun,
            sunLive = ephemerisRepository.liveUpdates.value?.sun,
            settings = AppSettings(),
        ),
    )

    fun cycleIrradianceMode() {
        val current = uiState.value?.irradianceMode ?: IrradianceMode.DIRECT
        viewModelScope.launch {
            settingsDataStore.setIrradianceMode(current.next())
        }
    }

    fun cycleIlluminanceMode() {
        val current = uiState.value?.illuminanceMode ?: IlluminanceMode.GLOBAL
        viewModelScope.launch {
            settingsDataStore.setIlluminanceMode(current.next())
        }
    }

    fun jumpToSolarNoon() {
        val state = uiState.value ?: return
        val solarNoonHours = state.sun.events.solarNoon
        if (!solarNoonHours.isFinite()) return

        val totalSeconds = (solarNoonHours * 3600.0).roundToLong()
        val dayOffset = Math.floorDiv(totalSeconds, 86_400L)
        val secondOfDay = Math.floorMod(totalSeconds, 86_400L)
        val targetTime = ZonedDateTime.of(
            state.currentTime.toLocalDate().plusDays(dayOffset),
            LocalTime.ofSecondOfDay(secondOfDay),
            state.currentTime.zone,
        )
        timeRepository.travelTo(targetTime)
    }

    private fun buildState(
        time: ZonedDateTime,
        coords: Coordinates?,
        sunDaily: SunDailyData?,
        sunLive: SunLiveSnapshot?,
        settings: AppSettings,
    ): SunUiState? {
        if (coords == null || sunDaily == null || sunLive == null) return null
        return SunUiState(
            currentTime = time,
            coordinates = coords,
            sun = SunUiData(
                live = sunLive,
                daily = sunDaily,
                phase = getSunPhase(sunLive.position.altitude, coords.sunApparentHorizonAlt).desc,
            ),
            irradianceMode = settings.irradianceMode,
            illuminanceMode = settings.illuminanceMode,
        )
    }
}
