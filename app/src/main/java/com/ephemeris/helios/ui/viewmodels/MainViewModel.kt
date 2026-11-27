package com.ephemeris.helios.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ephemeris.helios.data.EphemerisRepository
import com.ephemeris.helios.data.LocationRepository
import com.ephemeris.helios.data.TimeRepository
import com.ephemeris.helios.utils.LocationStatus
import com.ephemeris.helios.utils.calc.AtmosphericConditions
import com.ephemeris.helios.utils.datastore.CoordinateFormat
import com.ephemeris.helios.utils.datastore.SettingsDataStore
import com.ephemeris.helios.utils.location.Coordinates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * Shared app-shell ViewModel powering MainActivity's TopBar, TimeMachine, and loading guards.
 */
class MainViewModel(
    private val locationRepository: LocationRepository,
    private val timeRepository: TimeRepository,
    ephemerisRepository: EphemerisRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val coordinates: StateFlow<Coordinates?> = locationRepository.coordinates
    val isDataStoreLoaded: StateFlow<Boolean> = locationRepository.isLoaded
    val locationStatus: StateFlow<LocationStatus> = locationRepository.locationStatus
    val isContinuousGPSTrackingEnabled: StateFlow<Boolean> = locationRepository.isContinuousTrackingEnabled

    val currentTime: StateFlow<ZonedDateTime> = timeRepository.currentTime
    val isAutoUpdateEnabled: StateFlow<Boolean> = timeRepository.isAutoUpdateEnabled

    val isEphemerisReady: StateFlow<Boolean> = ephemerisRepository.isReady

    val coordinateFormat: StateFlow<CoordinateFormat> = settingsDataStore.settingsFlow
        .map { it.coordinateFormat }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CoordinateFormat.DECIMAL,
        )

    val customTimezoneId: StateFlow<String?> = settingsDataStore.settingsFlow
        .map { it.customTimezoneId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val atmosphericConditions: StateFlow<AtmosphericConditions> = settingsDataStore.settingsFlow
        .map { it.atmosphericConditions }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AtmosphericConditions.DEFAULT,
        )

    fun saveCoordinates(
        coordinates: Coordinates,
        isFromGPS: Boolean = false,
        fetchAltitude: Boolean = !isFromGPS,
    ) {
        locationRepository.saveCoordinates(coordinates, isFromGPS, fetchAltitude)
    }

    fun toggleCoordinateFormat() {
        val nextFormat = coordinateFormat.value.next()
        viewModelScope.launch {
            settingsDataStore.setCoordinateFormat(nextFormat)
        }
    }

    fun setCustomTimezoneId(timezoneId: String?) {
        viewModelScope.launch {
            settingsDataStore.setCustomTimezoneId(timezoneId)
        }
    }

    fun setAtmosphericConditions(conditions: AtmosphericConditions) {
        viewModelScope.launch {
            settingsDataStore.setAtmosphericConditions(conditions)
        }
    }

    fun onLocationRequestStarted() = locationRepository.onLocationRequestStarted()

    fun onLocationRequestFailed() = locationRepository.onLocationRequestFailed()

    fun setContinuousTracking(enabled: Boolean) = locationRepository.setContinuousTracking(enabled)

    fun setTime(time: ZonedDateTime) = timeRepository.setTime(time)

    fun setAutoUpdateEnabled(enabled: Boolean) = timeRepository.setAutoUpdateEnabled(enabled)

    fun resetToNow() = timeRepository.resetToNow()
}
