package com.ephemeris.helios.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ephemeris.helios.data.DailyEphemerisBundle
import com.ephemeris.helios.data.EphemerisRepository
import com.ephemeris.helios.data.LocationRepository
import com.ephemeris.helios.data.MoonUiData
import com.ephemeris.helios.data.SunUiData
import com.ephemeris.helios.data.TimeRepository
import com.ephemeris.helios.utils.calc.LiveUpdatesData
import com.ephemeris.helios.utils.getSunPhase
import com.ephemeris.helios.utils.location.Coordinates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZonedDateTime

data class HomeUiState(
    val currentTime: ZonedDateTime,
    val coordinates: Coordinates,
    val sun: SunUiData,
    val moon: MoonUiData,
)

class HomeViewModel(
    locationRepository: LocationRepository,
    timeRepository: TimeRepository,
    ephemerisRepository: EphemerisRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState?> = combine(
        timeRepository.currentTime,
        locationRepository.coordinates,
        ephemerisRepository.dailyData,
        ephemerisRepository.liveUpdates,
    ) { time, coords, daily, live ->
        buildState(time, coords, daily, live)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildState(
            time = timeRepository.currentTime.value,
            coords = locationRepository.coordinates.value,
            daily = ephemerisRepository.dailyData.value,
            live = ephemerisRepository.liveUpdates.value,
        ),
    )

    private fun buildState(
        time: ZonedDateTime,
        coords: Coordinates?,
        daily: DailyEphemerisBundle?,
        live: LiveUpdatesData?,
    ): HomeUiState? {
        if (coords == null || daily == null || live == null) return null
        return HomeUiState(
            currentTime = time,
            coordinates = coords,
            sun = SunUiData(
                live = live.sun,
                daily = daily.sun,
                phase = getSunPhase(live.sun.position.altitude, coords.sunApparentHorizonAlt).desc,
            ),
            moon = MoonUiData(
                live = live.moon,
                daily = daily.moon,
            ),
        )
    }
}
