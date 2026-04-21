package com.ephemeris.helios.utils

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
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
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.location.MapboxElevationEngine
import com.ephemeris.helios.utils.location.NativeGeocodingEngine
import com.ephemeris.helios.utils.location.estimateHistoricalOzone
import com.ephemeris.helios.utils.network.TimeApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.div
import kotlin.math.round

class HeliosViewModel(application: Application) : AndroidViewModel(application) {
    private val locationDataStore = LocationDataStore(application)
    private val geocodingEngine = NativeGeocodingEngine(application)

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

    var sunChartArrays by mutableStateOf<ChartArrays?>(null)
        private set
    var moonChartArrays by mutableStateOf<ChartArrays?>(null)
        private set

    init {
        startClockTicker()

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

    fun saveCoordinates(newCoordinates: Coordinates, isFromGPS: Boolean = false) {
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
        viewModelScope.launch(Dispatchers.Default) {
            dayData = getDailyEphemerisData(currentTime, coordinates)

            // Isolate the 480-iteration math loop
            val hoursCalc = DoubleArray(X_SIZE) { round(it * 5.0) / 100.0 }
            val hours = FloatArray(X_SIZE) { hoursCalc[it].toFloat() }

//            val xDataMap = mutableMapOf<Charts, FloatArray>()
//            val yDataMap = mutableMapOf<Charts, FloatArray>()

            val tzOffset = currentTime.offset.totalSeconds / 3600.0
            val localDate = currentTime.toLocalDate()

            val dailyOzone = estimateHistoricalOzone(
                latitude = coordinates.latitude,
                date = localDate
            )

            val (xSun, ySun) = generateSunData(localDate, hoursCalc, coordinates, tzOffset, dailyOzone)
            sunChartArrays = ChartArrays(hours, xSun, ySun)

            val (xMoon, yMoon) = generateMoonData(localDate, hoursCalc, coordinates, tzOffset, currentTime)
            moonChartArrays = ChartArrays(hours, xMoon, yMoon)

            // Todo Planets
        }
    }

    // Ticker 2: Live UI Updates
    fun startLiveUpdatesTicker(coordinates: Coordinates) {
        viewModelScope.launch(Dispatchers.Default) {
            liveData = if (isAutoUpdateEnabled) {
                getLiveUpdates(ZonedDateTime.now(currentZoneId), coordinates)
            } else {
                getLiveUpdates(currentTime, coordinates)
            }
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