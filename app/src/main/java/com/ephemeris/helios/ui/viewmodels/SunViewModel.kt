package com.ephemeris.helios.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ephemeris.helios.data.EphemerisRepository
import com.ephemeris.helios.data.LocationRepository
import com.ephemeris.helios.data.SunDailyData
import com.ephemeris.helios.data.SunUiData
import com.ephemeris.helios.data.TimeRepository
import com.ephemeris.helios.utils.calc.SunLiveSnapshot
import com.ephemeris.helios.utils.getSunPhase
import com.ephemeris.helios.utils.location.Coordinates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZonedDateTime

data class SunUiState(
    val currentTime: ZonedDateTime,
    val coordinates: Coordinates,
    val sun: SunUiData,
)

class SunViewModel(
    locationRepository: LocationRepository,
    timeRepository: TimeRepository,
    ephemerisRepository: EphemerisRepository,
) : ViewModel() {

    val uiState: StateFlow<SunUiState?> = combine(
        timeRepository.currentTime,
        locationRepository.coordinates,
        ephemerisRepository.sunDaily,
        ephemerisRepository.liveUpdates,
    ) { time, coords, sunDaily, live ->
        buildState(time, coords, sunDaily, live?.sun)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildState(
            time = timeRepository.currentTime.value,
            coords = locationRepository.coordinates.value,
            sunDaily = ephemerisRepository.sunDaily.value,
            sunLive = ephemerisRepository.liveUpdates.value?.sun,
        ),
    )

    private fun buildState(
        time: ZonedDateTime,
        coords: Coordinates?,
        sunDaily: SunDailyData?,
        sunLive: SunLiveSnapshot?,
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
        )
    }
}
