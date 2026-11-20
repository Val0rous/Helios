package com.ephemeris.helios.utils

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ephemeris.helios.BuildConfig
import com.ephemeris.helios.ui.composables.cards.ChartArrays
import com.ephemeris.helios.ui.composables.cards.X_SIZE
import com.ephemeris.helios.ui.composables.cards.generateMoonData
import com.ephemeris.helios.ui.composables.cards.generateSunData
import com.ephemeris.helios.utils.calc.DayEphemerisData
import com.ephemeris.helios.utils.calc.LiveUpdatesData
import com.ephemeris.helios.utils.calc.getDailyEphemerisData
import com.ephemeris.helios.utils.calc.getLiveUpdates
import com.ephemeris.helios.utils.datastore.LocationDataStore
import com.ephemeris.helios.utils.location.AltitudeCorrector
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.location.MapboxElevationEngine
import com.ephemeris.helios.utils.location.NativeGeocodingEngine
import com.ephemeris.helios.utils.location.estimateHistoricalOzone
import com.ephemeris.helios.utils.network.TimeApiClient
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime

class HeliosViewModel(
    private val locationDataStore: LocationDataStore,
    private val geocodingEngine: NativeGeocodingEngine,
    private val locationService: LocationService,
    private val altitudeCorrector: AltitudeCorrector,
) : ViewModel() {
    // DataStore Flow converted to a StateFlow for Compose
    val coordinatesState = locationDataStore.coordinatesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    var currentZoneId by mutableStateOf(ZoneId.systemDefault())
        private set

    var currentTime: ZonedDateTime by mutableStateOf(ZonedDateTime.now(currentZoneId))
    var isAutoUpdateEnabled by mutableStateOf(true)

    var dayData by mutableStateOf<DayEphemerisData?>(null)
        private set
    var liveData by mutableStateOf<LiveUpdatesData?>(null)
        private set

    var isDataStoreLoaded by mutableStateOf(false)
        private set

    var locationStatus by mutableStateOf(LocationStatus.DISABLED)
        private set
    var isContinuousGPSTrackingEnabled by mutableStateOf(false)
        private set

    var sunChartArrays by mutableStateOf<ChartArrays?>(null)
        private set
    var moonChartArrays by mutableStateOf<ChartArrays?>(null)
        private set

    private var dayDataJob: Job? = null
    private var liveUpdateJob: Job? = null

    init {
        startClockTicker()

        // Collect GPS hardware fixes off the Main thread (also keeps Geoid fallback off the Main thread)
        viewModelScope.launch(Dispatchers.IO) {
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
                        locationStatus = LocationStatus.CURRENT
                        saveCoordinates(gpsCoords, isFromGPS = true)
                    }
                    GpsEvent.Failed -> {
                        onLocationRequestFailed()
                    }
                }
            }
        }

        // Listen for the very first DataStore emission
        viewModelScope.launch {
            locationDataStore.coordinatesFlow.collect {
                isDataStoreLoaded = true
            }
        }

        // No API calls here, just listen for cached data from DataStore
        viewModelScope.launch(Dispatchers.IO) {
            coordinatesState.collect { coords ->
                if (coords?.timezoneId != null) {
                    try {
                        val newZone = ZoneId.of(coords.timezoneId)
                        // Only update if the timezone actually changed
                        if (currentZoneId != newZone) {
                            currentZoneId = newZone
                            if (isAutoUpdateEnabled) {
                                currentTime = ZonedDateTime.now(currentZoneId)
                            } else {
                                currentTime = currentTime.withZoneSameInstant(currentZoneId)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun onLocationRequestStarted() {
        locationStatus = LocationStatus.SEARCHING
    }

    fun onLocationRequestFailed() {
        if (locationStatus == LocationStatus.SEARCHING) {
            locationStatus = LocationStatus.DISABLED
        }
    }

    fun setContinuousTracking(enabled: Boolean) {
        isContinuousGPSTrackingEnabled = enabled
        if (enabled && locationStatus != LocationStatus.CURRENT) {
            locationStatus = LocationStatus.SEARCHING
        }
    }

    fun saveCoordinates(newCoordinates: Coordinates, isFromGPS: Boolean = false) {
        if (!isFromGPS) {
            locationStatus = LocationStatus.DISABLED
        }
        // Run on IO Dispatcher so the geocoder doesn't block the UI
        viewModelScope.launch(Dispatchers.IO) {
            var finalCoords = newCoordinates
            // Fetch Street Address if missing (GeocodingEngine already catches offline errors safely)
            if (newCoordinates.locationName == null) {
                // Convert to Android Location object for the Geocoder
                val loc = Location("").apply {
                    latitude = newCoordinates.latitude
                    longitude = newCoordinates.longitude
                }
                // Fetch street address
                val fetchedName = geocodingEngine.getStreetAddress(loc)
                finalCoords = finalCoords.copy(locationName = fetchedName ?: "Unknown location")
            }

            // Fetch Timezone API if missing (Prevents offline crashes)
            if (finalCoords.timezoneId == null) {
                try {
                    val tzResponse = TimeApiClient.getTimezoneForCoordinates(
                        finalCoords.latitude,
                        finalCoords.longitude
                    )
                    if (tzResponse != null) {
                        finalCoords = finalCoords.copy(timezoneId = tzResponse.timezone)
                    }
                } catch (e: Exception) {
                    e.printStackTrace() // Offline? Ignore and keep previous timezone
                }
            }

            // Fetch altitude ONLY if this didn't come directly from the phone's GPS sensor
            if (!isFromGPS) {
                val realAltitude = MapboxElevationEngine.getElevation(
                    lat = newCoordinates.latitude,
                    lon = newCoordinates.longitude,
                    accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
                ).round(1) ?: newCoordinates.altitude
                finalCoords = finalCoords.copy(altitude = realAltitude)
            }

            // Save to disk
            locationDataStore.saveCoordinates(finalCoords)
        }
    }

    // Triggered manually when location or date changes
    fun updateDayData(coordinates: Coordinates) {
        val targetTime = currentTime
        dayDataJob?.cancel()
        dayDataJob = viewModelScope.launch(Dispatchers.Default) {
            val newDayData = getDailyEphemerisData(targetTime, coordinates)
            ensureActive()

            // Isolate the 1441-iteration math loop
            val hoursCalc = DoubleArray(X_SIZE) { it / 60.0 }
            val hours = FloatArray(X_SIZE) { hoursCalc[it].toFloat() }

            val tzOffset = targetTime.offset.totalSeconds / 3600.0
            val localDate = targetTime.toLocalDate()

            val dailyOzone = estimateHistoricalOzone(
                latitude = coordinates.latitude,
                date = localDate
            )

            val (xSun, ySun) = generateSunData(localDate, hoursCalc, coordinates, tzOffset, dailyOzone)
            ensureActive()
            val newSunChartArrays = ChartArrays(hours, xSun, ySun)

            val (xMoon, yMoon) = generateMoonData(localDate, hoursCalc, coordinates, tzOffset, targetTime)
            ensureActive()
            val newMoonChartArrays = ChartArrays(hours, xMoon, yMoon)

            Snapshot.withMutableSnapshot {
                dayData = newDayData
                sunChartArrays = newSunChartArrays
                moonChartArrays = newMoonChartArrays
            }

            // Todo Planets
        }
    }

    // Ticker 2: Live UI Updates
    fun startLiveUpdatesTicker(coordinates: Coordinates) {
        val targetTime = if (isAutoUpdateEnabled) {
            ZonedDateTime.now(currentZoneId)
        } else {
            currentTime
        }
        liveUpdateJob?.cancel()
        liveUpdateJob = viewModelScope.launch(Dispatchers.Default) {
            val updated = getLiveUpdates(targetTime, coordinates)
            ensureActive()
            liveData = updated
        }
    }

    // Ticker 3: Pushes the clock forward
    private fun startClockTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                if (isAutoUpdateEnabled) {
                    currentTime = ZonedDateTime.now(currentZoneId)
                }
                delay(1000)
            }
        }
    }
}