package com.ephemeris.helios.data

import android.location.Location
import com.ephemeris.helios.BuildConfig
import com.ephemeris.helios.utils.GpsEvent
import com.ephemeris.helios.utils.LocationService
import com.ephemeris.helios.utils.LocationStatus
import com.ephemeris.helios.utils.datastore.LocationDataStore
import com.ephemeris.helios.utils.datastore.SettingsDataStore
import com.ephemeris.helios.utils.location.AltitudeCorrector
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.location.MapboxElevationEngine
import com.ephemeris.helios.utils.location.NativeGeocodingEngine
import com.ephemeris.helios.utils.network.TimeApiClient
import com.ephemeris.helios.utils.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt

/**
 * Single source of truth for observer coordinates, DataStore persistence,
 * geocoding/timezone/elevation enrichment, and incoming GPS hardware fixes.
 */
class LocationRepository(
    private val locationDataStore: LocationDataStore,
    settingsDataStore: SettingsDataStore,
    private val geocodingEngine: NativeGeocodingEngine,
    private val locationService: LocationService,
    private val altitudeCorrector: AltitudeCorrector,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var saveJob: Job? = null

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val coordinates: StateFlow<Coordinates?> = combine(
        locationDataStore.coordinatesFlow,
        settingsDataStore.settingsFlow,
    ) { coords, settings ->
        coords?.copy(
            useHorizonDip = settings.useHorizonDip,
            useInstantaneousSemidiameter = settings.useInstantaneousSemidiameter,
        )
    }
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
                        val realAltitude = altitudeCorrector.getRealAltitudeOrNull(event.location)
                        val roundedAltitude = realAltitude?.let { kotlin.math.round(it * 10.0) / 10.0 } ?: 0.0
                        val gpsCoords = Coordinates(
                            latitude = event.location.latitude,
                            longitude = event.location.longitude,
                            altitude = roundedAltitude,
                        )
                        _locationStatus.value = LocationStatus.CURRENT
                        saveCoordinates(
                            newCoordinates = gpsCoords,
                            isFromGPS = true,
                            // If the Fused/network fix lacked an altitude sensor reading, fetch DEM elevation
                            fetchAltitude = realAltitude == null,
                        )
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
        } else if (!enabled && _locationStatus.value == LocationStatus.SEARCHING) {
            _locationStatus.value = LocationStatus.DISABLED
        }
    }

    fun saveCoordinates(
        newCoordinates: Coordinates,
        isFromGPS: Boolean = false,
        fetchAltitude: Boolean = !isFromGPS,
    ) {
        if (!isFromGPS) {
            _locationStatus.value = LocationStatus.DISABLED
        }

        // Cancel any slower in-flight lookup so stale manual/map results can never overwrite a newer GPS fix
        saveJob?.cancel()
        saveJob = scope.launch {
            val finalCoords = coroutineScope {
                val nameDeferred = async {
                    newCoordinates.locationName ?: run {
                        val loc = Location("").apply {
                            latitude = newCoordinates.latitude
                            longitude = newCoordinates.longitude
                        }
                        geocodingEngine.getStreetAddress(loc) ?: "Unknown location"
                    }
                }

                val timezoneDeferred = async {
                    newCoordinates.timezoneId ?: run {
                        val fetched = try {
                            TimeApiClient.getTimezoneForCoordinates(
                                newCoordinates.latitude,
                                newCoordinates.longitude,
                            )?.timezone
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                        fetched ?: if (isFromGPS) {
                            // Offline GPS fix: device's own timezone is authoritative
                            ZoneId.systemDefault().id
                        } else {
                            // Offline manual location: solar-meridian UTC offset fallback
                            val offsetHours = (newCoordinates.longitude / 15.0).roundToInt().coerceIn(-12, 14)
                            ZoneOffset.ofHours(offsetHours).id
                        }
                    }
                }

                val altitudeDeferred = async {
                    if (fetchAltitude) {
                        MapboxElevationEngine.getElevation(
                            lat = newCoordinates.latitude,
                            lon = newCoordinates.longitude,
                            accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN,
                        ).round(1) ?: newCoordinates.altitude
                    } else {
                        newCoordinates.altitude
                    }
                }

                newCoordinates.copy(
                    locationName = nameDeferred.await(),
                    timezoneId = timezoneDeferred.await(),
                    altitude = altitudeDeferred.await(),
                )
            }

            locationDataStore.saveCoordinates(finalCoords)
        }
    }
}
