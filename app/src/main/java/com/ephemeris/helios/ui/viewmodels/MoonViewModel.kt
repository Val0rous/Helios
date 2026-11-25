package com.ephemeris.helios.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ephemeris.helios.data.EphemerisRepository
import com.ephemeris.helios.data.LocationRepository
import com.ephemeris.helios.data.MoonDailyData
import com.ephemeris.helios.data.MoonUiData
import com.ephemeris.helios.data.TimeRepository
import com.ephemeris.helios.utils.calc.LocalDayTimeline
import com.ephemeris.helios.utils.calc.MoonLiveSnapshot
import com.ephemeris.helios.utils.datastore.AppSettings
import com.ephemeris.helios.utils.datastore.MoonColorTempMode
import com.ephemeris.helios.utils.datastore.MoonDeclinationMode
import com.ephemeris.helios.utils.datastore.MoonIlluminanceMode
import com.ephemeris.helios.utils.datastore.SettingsDataStore
import com.ephemeris.helios.utils.location.Coordinates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class MoonUiState(
    val currentTime: ZonedDateTime,
    val coordinates: Coordinates,
    val moon: MoonUiData,
    val illuminanceMode: MoonIlluminanceMode = MoonIlluminanceMode.GLOBAL,
    val colorTempMode: MoonColorTempMode = MoonColorTempMode.PURKINJE,
    val declinationMode: MoonDeclinationMode = MoonDeclinationMode.GEOCENTRIC,
)

class MoonViewModel(
    locationRepository: LocationRepository,
    private val timeRepository: TimeRepository,
    ephemerisRepository: EphemerisRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val uiState: StateFlow<MoonUiState?> = combine(
        timeRepository.currentTime,
        locationRepository.coordinates,
        ephemerisRepository.dailyData,
        ephemerisRepository.liveUpdates,
        settingsDataStore.settingsFlow,
    ) { time, coords, daily, live, settings ->
        buildState(time, coords, daily?.moon, live?.moon, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildState(
            time = timeRepository.currentTime.value,
            coords = locationRepository.coordinates.value,
            moonDaily = ephemerisRepository.dailyData.value?.moon,
            moonLive = ephemerisRepository.liveUpdates.value?.moon,
            settings = AppSettings(),
        ),
    )

    fun cycleIlluminanceMode() {
        val current = uiState.value?.illuminanceMode ?: MoonIlluminanceMode.GLOBAL
        viewModelScope.launch {
            settingsDataStore.setMoonIlluminanceMode(current.next())
        }
    }

    fun cycleColorTempMode() {
        val current = uiState.value?.colorTempMode ?: MoonColorTempMode.PURKINJE
        viewModelScope.launch {
            settingsDataStore.setMoonColorTempMode(current.next())
        }
    }

    fun cycleDeclinationMode() {
        val current = uiState.value?.declinationMode ?: MoonDeclinationMode.GEOCENTRIC
        viewModelScope.launch {
            settingsDataStore.setMoonDeclinationMode(current.next())
        }
    }

    fun jumpToCulmination() {
        val state = uiState.value ?: return
        val culminationElapsedHours = state.moon.events.culminationElapsedHours ?: return
        if (!culminationElapsedHours.isFinite()) return

        val targetTime = LocalDayTimeline(state.currentTime.toLocalDate(), state.currentTime.zone)
            .elapsedHourToZonedDateTime(culminationElapsedHours)
        timeRepository.travelTo(targetTime)
    }

    fun jumpToTime(targetTime: ZonedDateTime) {
        timeRepository.travelTo(targetTime)
    }

    private fun buildState(
        time: ZonedDateTime,
        coords: Coordinates?,
        moonDaily: MoonDailyData?,
        moonLive: MoonLiveSnapshot?,
        settings: AppSettings,
    ): MoonUiState? {
        if (coords == null || moonDaily == null || moonLive == null) return null
        return MoonUiState(
            currentTime = time,
            coordinates = coords,
            moon = MoonUiData(
                live = moonLive,
                daily = moonDaily,
            ),
            illuminanceMode = settings.moonIlluminanceMode,
            colorTempMode = settings.moonColorTempMode,
            declinationMode = settings.moonDeclinationMode,
        )
    }
}
