package com.ephemeris.helios.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

/**
 * Single source of truth for the active clock, timezone synchronization,
 * and TimeMachine auto/manual mode across all screens.
 */
class TimeRepository(
    private val locationRepository: LocationRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentZoneId = MutableStateFlow<ZoneId>(ZoneId.systemDefault())
    val currentZoneId: StateFlow<ZoneId> = _currentZoneId.asStateFlow()

    private val _currentTime = MutableStateFlow(ZonedDateTime.now(_currentZoneId.value))
    val currentTime: StateFlow<ZonedDateTime> = _currentTime.asStateFlow()

    private val _isAutoUpdateEnabled = MutableStateFlow(true)
    val isAutoUpdateEnabled: StateFlow<Boolean> = _isAutoUpdateEnabled.asStateFlow()

    init {
        // 1. Push the live clock forward every second when auto-update is enabled
        scope.launch {
            while (true) {
                if (_isAutoUpdateEnabled.value) {
                    _currentTime.value = ZonedDateTime.now(_currentZoneId.value)
                }
                delay(1.seconds)
            }
        }

        // 2. Keep active ZoneId synchronized with the observer's coordinates
        scope.launch {
            locationRepository.coordinates.collect { coords ->
                val tzId = coords?.timezoneId ?: return@collect
                try {
                    val newZone = ZoneId.of(tzId)
                    if (_currentZoneId.value != newZone) {
                        _currentZoneId.value = newZone
                        _currentTime.value = if (_isAutoUpdateEnabled.value) {
                            ZonedDateTime.now(newZone)
                        } else {
                            _currentTime.value.withZoneSameInstant(newZone)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setTime(newTime: ZonedDateTime) {
        _currentTime.value = newTime
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        _isAutoUpdateEnabled.value = enabled
        if (enabled) {
            _currentTime.value = ZonedDateTime.now(_currentZoneId.value)
        }
    }
}
