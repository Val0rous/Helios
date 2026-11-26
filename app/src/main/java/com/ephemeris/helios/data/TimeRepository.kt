package com.ephemeris.helios.data

import com.ephemeris.helios.utils.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val settingsDataStore: SettingsDataStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentZoneId = MutableStateFlow<ZoneId>(ZoneId.systemDefault())
    val currentZoneId: StateFlow<ZoneId> = _currentZoneId.asStateFlow()

    private val _currentTime = MutableStateFlow(ZonedDateTime.now(_currentZoneId.value))
    val currentTime: StateFlow<ZonedDateTime> = _currentTime.asStateFlow()

    private val _isAutoUpdateEnabled = MutableStateFlow(true)
    val isAutoUpdateEnabled: StateFlow<Boolean> = _isAutoUpdateEnabled.asStateFlow()

    // Monotonically increments whenever the user explicitly resets TimeMachine to "now" so
    // downstream repositories immediately cancel any in-flight past/future calculation.
    private val _resetGeneration = MutableStateFlow(0)
    val resetGeneration: StateFlow<Int> = _resetGeneration.asStateFlow()

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

        // 2. Keep active ZoneId synchronized with custom timezone override or observer coordinates
        scope.launch {
            combine(
                locationRepository.coordinates,
                settingsDataStore.settingsFlow
                    .map { it.customTimezoneId }
                    .distinctUntilChanged(),
            ) { coords, customTzId ->
                coords to customTzId
            }.collect { (coords, customTzId) ->
                if (coords == null && customTzId == null) return@collect
                val newZone = customTzId?.let(::parseZoneIdOrNull)
                    ?: coords?.timezoneId?.let(::parseZoneIdOrNull)
                    ?: ZoneId.systemDefault()

                if (_currentZoneId.value != newZone) {
                    _currentZoneId.value = newZone
                    _currentTime.value = if (_isAutoUpdateEnabled.value) {
                        ZonedDateTime.now(newZone)
                    } else {
                        _currentTime.value.withZoneSameInstant(newZone)
                    }
                }
            }
        }
    }

    private fun parseZoneIdOrNull(id: String): ZoneId? = runCatching { ZoneId.of(id) }.getOrNull()

    fun setTime(newTime: ZonedDateTime) {
        _currentTime.value = newTime
    }

    /**
     * Enters TimeMachine manual mode and jumps directly to [newTime].
     */
    fun travelTo(newTime: ZonedDateTime) {
        _isAutoUpdateEnabled.value = false
        _currentTime.value = newTime
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        if (enabled) {
            resetToNow()
        } else {
            _isAutoUpdateEnabled.value = false
        }
    }

    /**
     * Immediately halts any past/future time travel and snaps back to live current time.
     */
    fun resetToNow() {
        _isAutoUpdateEnabled.value = true
        _currentTime.value = ZonedDateTime.now(_currentZoneId.value)
        _resetGeneration.value += 1
    }
}
