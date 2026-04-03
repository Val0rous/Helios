package com.ephemeris.helios.utils

import android.app.Application
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class HeliosViewModel(application: Application) : AndroidViewModel(application) {
    private val locationDataStore = LocationDataStore(application)

    // DataStore Flow converted to a StateFlow for Compose
    val coordinatesState = locationDataStore.coordinatesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    var currentTime by mutableStateOf(ZonedDateTime.now())
    var isAutoUpdateEnabled by mutableStateOf(true)

    var dayData by mutableStateOf<DayEphemerisData?>(null)
        private set
    var liveData by mutableStateOf<LiveUpdatesData?>(null)
        private set

    init {
        startClockTicker()
    }

    fun saveCoordinates(newCoordinates: Coordinates) {
        viewModelScope.launch {
            locationDataStore.saveCoordinates(newCoordinates)
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