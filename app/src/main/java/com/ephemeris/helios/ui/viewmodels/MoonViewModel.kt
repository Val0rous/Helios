package com.ephemeris.helios.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ephemeris.helios.data.EphemerisRepository
import com.ephemeris.helios.data.LocationRepository
import com.ephemeris.helios.data.MoonDailyData
import com.ephemeris.helios.data.MoonUiData
import com.ephemeris.helios.data.TimeRepository
import com.ephemeris.helios.utils.calc.MoonLiveSnapshot
import com.ephemeris.helios.utils.location.Coordinates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZonedDateTime

data class MoonUiState(
    val currentTime: ZonedDateTime,
    val coordinates: Coordinates,
    val moon: MoonUiData,
)

class MoonViewModel(
    locationRepository: LocationRepository,
    timeRepository: TimeRepository,
    ephemerisRepository: EphemerisRepository,
) : ViewModel() {

    val uiState: StateFlow<MoonUiState?> = combine(
        timeRepository.currentTime,
        locationRepository.coordinates,
        ephemerisRepository.moonDaily,
        ephemerisRepository.liveUpdates,
    ) { time, coords, moonDaily, live ->
        buildState(time, coords, moonDaily, live?.moon)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildState(
            time = timeRepository.currentTime.value,
            coords = locationRepository.coordinates.value,
            moonDaily = ephemerisRepository.moonDaily.value,
            moonLive = ephemerisRepository.liveUpdates.value?.moon,
        ),
    )

    private fun buildState(
        time: ZonedDateTime,
        coords: Coordinates?,
        moonDaily: MoonDailyData?,
        moonLive: MoonLiveSnapshot?,
    ): MoonUiState? {
        if (coords == null || moonDaily == null || moonLive == null) return null
        return MoonUiState(
            currentTime = time,
            coordinates = coords,
            moon = MoonUiData(
                live = moonLive,
                daily = moonDaily,
            ),
        )
    }
}
