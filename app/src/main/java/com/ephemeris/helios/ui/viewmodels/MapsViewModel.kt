package com.ephemeris.helios.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ephemeris.helios.data.DailyEphemerisBundle
import com.ephemeris.helios.data.EphemerisRepository
import com.ephemeris.helios.data.LocationRepository
import com.ephemeris.helios.data.MoonUiData
import com.ephemeris.helios.data.SunUiData
import com.ephemeris.helios.utils.calc.LiveUpdatesData
import com.ephemeris.helios.utils.getSunPhase
import com.ephemeris.helios.utils.location.Coordinates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MapsUiState(
    val coordinates: Coordinates,
    val sun: SunUiData,
    val moon: MoonUiData,
)

class MapsViewModel(
    private val locationRepository: LocationRepository,
    ephemerisRepository: EphemerisRepository,
) : ViewModel() {

    val uiState: StateFlow<MapsUiState?> = combine(
        locationRepository.coordinates,
        ephemerisRepository.dailyData,
        ephemerisRepository.liveUpdates,
    ) { coords, daily, live ->
        buildState(coords, daily, live)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildState(
            coords = locationRepository.coordinates.value,
            daily = ephemerisRepository.dailyData.value,
            live = ephemerisRepository.liveUpdates.value,
        ),
    )

    fun onMapCenterSettled(coordinates: Coordinates) {
        locationRepository.saveCoordinates(coordinates, isFromGPS = false)
    }

    private fun buildState(
        coords: Coordinates?,
        daily: DailyEphemerisBundle?,
        live: LiveUpdatesData?,
    ): MapsUiState? {
        if (coords == null || daily == null || live == null) return null
        return MapsUiState(
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
