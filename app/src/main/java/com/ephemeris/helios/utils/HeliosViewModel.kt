package com.ephemeris.helios.utils

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ephemeris.helios.utils.calc.DayEphemerisData
import com.ephemeris.helios.utils.calc.LiveUpdatesData
import com.ephemeris.helios.utils.calc.getDailyEphemerisData
import com.ephemeris.helios.utils.calc.getLiveUpdates
import com.ephemeris.helios.utils.datastore.LocationDataStore
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.location.NativeGeocodingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class HeliosViewModel(application: Application) : AndroidViewModel(application) {
    private val locationDataStore = LocationDataStore(application)
    private val geocodingEngine = NativeGeocodingEngine(application)

    // DataStore Flow converted to a StateFlow for Compose
    val coordinatesState = locationDataStore.coordinatesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    var currentTime: ZonedDateTime by mutableStateOf(ZonedDateTime.now())
    var isAutoUpdateEnabled by mutableStateOf(true)

    var dayData by mutableStateOf<DayEphemerisData?>(null)
        private set
    var liveData by mutableStateOf<LiveUpdatesData?>(null)
        private set

    var isDataStoreLoaded by mutableStateOf(false)
        private set

    init {
        startClockTicker()

        // Listen for the very first DataStore emission
        viewModelScope.launch {
            locationDataStore.coordinatesFlow.collect {
                isDataStoreLoaded = true
            }
        }
    }

    fun saveCoordinates(newCoordinates: Coordinates) {
        // Run on IO Dispatcher so the geocoder doesn't block the UI
        viewModelScope.launch(Dispatchers.IO) {
            // Check if we need to fetch the name
            val finalCoords = if (newCoordinates.locationName == null) {
                // Convert to Android Location object for the Geocoder
                val loc = Location("").apply {
                    latitude = newCoordinates.latitude
                    longitude = newCoordinates.longitude
                }
                // Fetch street address
                val fetchedName = geocodingEngine.getStreetAddress(loc)
                newCoordinates.copy(locationName = fetchedName ?: "Unknown location")
            } else {
                newCoordinates  // Name already exists (e.g. from a forward search)
            }

            locationDataStore.saveCoordinates(finalCoords)
        }
    }

    // Triggered manually when location or date changes
    fun updateDayData(coordinates: Coordinates) {
        viewModelScope.launch(Dispatchers.Default) {
            dayData = getDailyEphemerisData(currentTime, coordinates)
        }
    }

    // Ticker 2: Live UI Updates
    fun startLiveUpdatesTicker(coordinates: Coordinates) {
        viewModelScope.launch(Dispatchers.Default) {
            if (isAutoUpdateEnabled) {
                liveData = getLiveUpdates(ZonedDateTime.now(), coordinates)
            } else {
                liveData = getLiveUpdates(currentTime, coordinates)
            }
        }
    }

    // Ticker 3: Pushes the clock forward
    private fun startClockTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                if (isAutoUpdateEnabled) {
                    currentTime = ZonedDateTime.now()
                }
                delay(1000)
            }
        }
    }
}