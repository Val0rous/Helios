package com.ephemeris.helios.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.ephemeris.helios.data.EphemerisRepository
import com.ephemeris.helios.data.LocationRepository
import com.ephemeris.helios.data.TimeRepository
import com.ephemeris.helios.utils.LocationStatus
import com.ephemeris.helios.utils.location.Coordinates
import kotlinx.coroutines.flow.StateFlow
import java.time.ZonedDateTime

/**
 * Shared app-shell ViewModel powering MainActivity's TopBar, TimeMachine, and loading guards.
 */
class MainViewModel(
    private val locationRepository: LocationRepository,
    private val timeRepository: TimeRepository,
    ephemerisRepository: EphemerisRepository,
) : ViewModel() {

    val coordinates: StateFlow<Coordinates?> = locationRepository.coordinates
    val isDataStoreLoaded: StateFlow<Boolean> = locationRepository.isLoaded
    val locationStatus: StateFlow<LocationStatus> = locationRepository.locationStatus
    val isContinuousGPSTrackingEnabled: StateFlow<Boolean> = locationRepository.isContinuousTrackingEnabled

    val currentTime: StateFlow<ZonedDateTime> = timeRepository.currentTime
    val isAutoUpdateEnabled: StateFlow<Boolean> = timeRepository.isAutoUpdateEnabled

    val isEphemerisReady: StateFlow<Boolean> = ephemerisRepository.isReady

    fun saveCoordinates(coordinates: Coordinates, isFromGPS: Boolean = false) {
        locationRepository.saveCoordinates(coordinates, isFromGPS)
    }

    fun onLocationRequestStarted() = locationRepository.onLocationRequestStarted()

    fun onLocationRequestFailed() = locationRepository.onLocationRequestFailed()

    fun setContinuousTracking(enabled: Boolean) = locationRepository.setContinuousTracking(enabled)

    fun setTime(time: ZonedDateTime) = timeRepository.setTime(time)

    fun setAutoUpdateEnabled(enabled: Boolean) = timeRepository.setAutoUpdateEnabled(enabled)
}
