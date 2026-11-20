package com.ephemeris.helios.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ephemeris.helios.data.EphemerisRepository
import com.ephemeris.helios.data.LocationRepository
import com.ephemeris.helios.data.MoonDailyData
import com.ephemeris.helios.data.MoonUiData
import com.ephemeris.helios.data.SunDailyData
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
        ephemerisRepository.sunDaily,
        ephemerisRepository.moonDaily,
        ephemerisRepository.liveUpdates,
    ) { coords, sunDaily, moonDaily, live ->
        buildState(coords, sunDaily, moonDaily, live)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildState(
            coords = locationRepository.coordinates.value,
            sunDaily = ephemerisRepository.sunDaily.value,
            moonDaily = ephemerisRepository.moonDaily.value,
            live = ephemerisRepository.liveUpdates.value,
        ),
    )

    fun onMapCenterSettled(coordinates: Coordinates) {
        locationRepository.saveCoordinates(coordinates, isFromGPS = false)
    }

    private fun buildState(
        coords: Coordinates?,
        sunDaily: SunDailyData?,
        moonDaily: MoonDailyData?,
        live: LiveUpdatesData?,
    ): MapsUiState? {
        if (coords == null || sunDaily == null || moonDaily == null || live == null) return null
        return MapsUiState(
            coordinates = coords,
            sun = SunUiData(
                live = live.sun,
                daily = sunDaily,
                phase = getSunPhase(live.sun.position.altitude, coords.sunApparentHorizonAlt).desc,
            ),
            moon = MoonUiData(
                live = live.moon,
                daily = moonDaily,
            ),
        )
    }
}
