package com.ephemeris.helios.data

import android.location.Location
import com.ephemeris.helios.BuildConfig
import com.ephemeris.helios.utils.GpsEvent
import com.ephemeris.helios.utils.LocationService
import com.ephemeris.helios.utils.LocationStatus
import com.ephemeris.helios.utils.datastore.LocationDataStore
import com.ephemeris.helios.utils.location.AltitudeCorrector
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.location.MapboxElevationEngine
import com.ephemeris.helios.utils.location.NativeGeocodingEngine
import com.ephemeris.helios.utils.network.TimeApiClient
import com.ephemeris.helios.utils.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single source of truth for observer coordinates, DataStore persistence,
 * geocoding/timezone/elevation enrichment, and incoming GPS hardware fixes.
 */
class LocationRepository(
    private val locationDataStore: LocationDataStore,
    private val geocodingEngine: NativeGeocodingEngine,
    private val locationService: LocationService,
    private val altitudeCorrector: AltitudeCorrector,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val coordinates: StateFlow<Coordinates?> = locationDataStore.coordinatesFlow
        .onEach { _isLoaded.value = true }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    private val _locationStatus = MutableStateFlow(LocationStatus.DISABLED)
    val locationStatus: StateFlow<LocationStatus> = _locationStatus.asStateFlow()

    private val _isContinuousTrackingEnabled = MutableStateFlow(false)
    val isContinuousTrackingEnabled: StateFlow<Boolean> = _isContinuousTrackingEnabled.asStateFlow()

    init {
        // Collect GPS fixes off the Main thread (also keeps Geoid fallback I/O off the Main thread)
        scope.launch {
            locationService.gpsEvents.collect { event ->
                when (event) {
                    is GpsEvent.Fix -> {
                        val realAltitude = altitudeCorrector.getRealAltitude(event.location)
                        val roundedAltitude = kotlin.math.round(realAltitude * 10.0) / 10.0
                        val gpsCoords = Coordinates(
                            latitude = event.location.latitude,
                            longitude = event.location.longitude,
                            altitude = roundedAltitude,
                        )
                        _locationStatus.value = LocationStatus.CURRENT
                        saveCoordinates(gpsCoords, isFromGPS = true)
                    }
                    GpsEvent.Failed -> {
                        onLocationRequestFailed()
                    }
                }
            }
        }
    }

    fun onLocationRequestStarted() {
        _locationStatus.value = LocationStatus.SEARCHING
    }

    fun onLocationRequestFailed() {
        if (_locationStatus.value == LocationStatus.SEARCHING) {
            _locationStatus.value = LocationStatus.DISABLED
        }
    }

    fun setContinuousTracking(enabled: Boolean) {
        _isContinuousTrackingEnabled.value = enabled
        if (enabled && _locationStatus.value != LocationStatus.CURRENT) {
            _locationStatus.value = LocationStatus.SEARCHING
        }
    }

    fun saveCoordinates(newCoordinates: Coordinates, isFromGPS: Boolean = false) {
        if (!isFromGPS) {
            _locationStatus.value = LocationStatus.DISABLED
        }

        scope.launch {
            var finalCoords = newCoordinates

            // Fetch Street Address if missing (GeocodingEngine catches offline errors safely)
            if (finalCoords.locationName == null) {
                val loc = Location("").apply {
                    latitude = finalCoords.latitude
                    longitude = finalCoords.longitude
                }
                val fetchedName = geocodingEngine.getStreetAddress(loc)
                finalCoords = finalCoords.copy(locationName = fetchedName ?: "Unknown location")
            }

            // Fetch Timezone API if missing (prevents offline crashes)
            if (finalCoords.timezoneId == null) {
                try {
                    val tzResponse = TimeApiClient.getTimezoneForCoordinates(
                        finalCoords.latitude,
                        finalCoords.longitude,
                    )
                    if (tzResponse != null) {
                        finalCoords = finalCoords.copy(timezoneId = tzResponse.timezone)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fetch elevation ONLY if this didn't come directly from the phone's GPS sensor
            if (!isFromGPS) {
                val realAltitude = MapboxElevationEngine.getElevation(
                    lat = finalCoords.latitude,
                    lon = finalCoords.longitude,
                    accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN,
                ).round(1) ?: finalCoords.altitude
                finalCoords = finalCoords.copy(altitude = realAltitude)
            }

            locationDataStore.saveCoordinates(finalCoords)
        }
    }
}
