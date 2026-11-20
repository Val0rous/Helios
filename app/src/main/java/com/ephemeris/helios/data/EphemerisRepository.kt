package com.ephemeris.helios.data

import com.ephemeris.helios.ui.composables.cards.ChartArrays
import com.ephemeris.helios.ui.composables.cards.X_SIZE
import com.ephemeris.helios.ui.composables.cards.generateMoonData
import com.ephemeris.helios.ui.composables.cards.generateSunData
import com.ephemeris.helios.utils.calc.LiveUpdatesData
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.MoonLiveSnapshot
import com.ephemeris.helios.utils.calc.MoonMetrics
import com.ephemeris.helios.utils.calc.SeasonalEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.calc.SunLiveSnapshot
import com.ephemeris.helios.utils.calc.SunMetrics
import com.ephemeris.helios.utils.calc.getDailyEphemerisData
import com.ephemeris.helios.utils.calc.getLiveUpdates
import com.ephemeris.helios.utils.location.estimateHistoricalOzone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

data class SunDailyData(
    val events: SolarEphemeris.DailyEvents,
    val durations: SolarEphemeris.DailyDurations,
    val dailyPeakMetrics: SunMetrics.SunMetricsResult,
    val seasonalEvents: SeasonalEphemeris.SeasonalEvents,
    val seasonalDailyEvents: SeasonalEphemeris.SeasonalDailyEvents,
    val chartArrays: ChartArrays,
)

data class MoonDailyData(
    val events: LunarEphemeris.LunarDailyEvents,
    val dailyPeakMetrics: MoonMetrics.LunarMetricsResult?,
    val chartArrays: ChartArrays,
)

/**
 * Grouped Sun UI model so screens write `sun.altitude`, `sun.azimuth`, `sun.phase`, etc.
 */
data class SunUiData(
    val live: SunLiveSnapshot,
    val daily: SunDailyData,
    val phase: String,
) {
    val timestamp: ZonedDateTime get() = live.timestamp
    val position: SolarEphemeris.SolarPosition get() = live.position
    val metrics: SunMetrics.SunMetricsResult get() = live.metrics
    val altitude: Double get() = live.position.altitude
    val azimuth: Double get() = live.position.azimuth
    val events: SolarEphemeris.DailyEvents get() = daily.events
    val durations: SolarEphemeris.DailyDurations get() = daily.durations
    val dailyPeakMetrics: SunMetrics.SunMetricsResult get() = daily.dailyPeakMetrics
    val seasonalEvents: SeasonalEphemeris.SeasonalEvents get() = daily.seasonalEvents
    val seasonalDailyEvents: SeasonalEphemeris.SeasonalDailyEvents get() = daily.seasonalDailyEvents
    val chartArrays: ChartArrays get() = daily.chartArrays
}

/**
 * Grouped Moon UI model so screens write `moon.altitude`, `moon.azimuth`, `moon.phase`, etc.
 */
data class MoonUiData(
    val live: MoonLiveSnapshot,
    val daily: MoonDailyData,
) {
    val timestamp: ZonedDateTime get() = live.timestamp
    val position: LunarEphemeris.LunarPosition get() = live.position
    val metrics: MoonMetrics.LunarMetricsResult get() = live.metrics
    val altitude: Double get() = live.position.altitude
    val azimuth: Double get() = live.position.azimuth
    val phase: String get() = live.metrics.phase.displayName
    val events: LunarEphemeris.LunarDailyEvents get() = daily.events
    val dailyPeakMetrics: MoonMetrics.LunarMetricsResult? get() = daily.dailyPeakMetrics
    val chartArrays: ChartArrays get() = daily.chartArrays
}

/**
 * Reactive ephemeris engine shared across all screens.
 * Automatically recomputes daily arrays when location/date/zone change,
 * and live snapshots on every manual TimeMachine scrub or 12-second live tick.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EphemerisRepository(
    private val locationRepository: LocationRepository,
    private val timeRepository: TimeRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _sunDaily = MutableStateFlow<SunDailyData?>(null)
    val sunDaily: StateFlow<SunDailyData?> = _sunDaily.asStateFlow()

    private val _moonDaily = MutableStateFlow<MoonDailyData?>(null)
    val moonDaily: StateFlow<MoonDailyData?> = _moonDaily.asStateFlow()

    private val _liveUpdates = MutableStateFlow<LiveUpdatesData?>(null)
    val liveUpdates: StateFlow<LiveUpdatesData?> = _liveUpdates.asStateFlow()

    val isReady: StateFlow<Boolean> = combine(
        _sunDaily,
        _moonDaily,
        _liveUpdates,
    ) { sun, moon, live ->
        sun != null && moon != null && live != null
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    init {
        // 1. Heavy daily ephemeris & 1441-point chart arrays: only runs when location, date, or zone changes.
        //    collectLatest automatically cancels stale calculations during fast date scrubbing.
        scope.launch {
            combine(
                locationRepository.coordinates.filterNotNull(),
                timeRepository.currentTime,
            ) { coords, time -> coords to time }
                .distinctUntilChanged { old, new ->
                    old.first == new.first &&
                        old.second.toLocalDate() == new.second.toLocalDate() &&
                        old.second.zone == new.second.zone
                }
                .collectLatest { (coords, targetTime) ->
                    val newDayData = getDailyEphemerisData(targetTime, coords)
                    currentCoroutineContext().ensureActive()

                    val hoursCalc = DoubleArray(X_SIZE) { it / 60.0 }
                    val hours = FloatArray(X_SIZE) { hoursCalc[it].toFloat() }
                    val tzOffset = targetTime.offset.totalSeconds / 3600.0
                    val localDate = targetTime.toLocalDate()
                    val dailyOzone = estimateHistoricalOzone(
                        latitude = coords.latitude,
                        date = localDate,
                    )

                    val (xSun, ySun) = generateSunData(localDate, hoursCalc, coords, tzOffset, dailyOzone)
                    currentCoroutineContext().ensureActive()
                    val newSunDaily = SunDailyData(
                        events = newDayData.events,
                        durations = newDayData.durations,
                        dailyPeakMetrics = newDayData.dailyPeakMetrics,
                        seasonalEvents = newDayData.seasonalEvents,
                        seasonalDailyEvents = newDayData.seasonalDailyEvents,
                        chartArrays = ChartArrays(hours, xSun, ySun),
                    )

                    val (xMoon, yMoon) = generateMoonData(localDate, hoursCalc, coords, tzOffset, targetTime)
                    currentCoroutineContext().ensureActive()
                    val newMoonDaily = MoonDailyData(
                        events = newDayData.lunarEvents,
                        dailyPeakMetrics = newDayData.lunarDailyPeakMetrics,
                        chartArrays = ChartArrays(hours, xMoon, yMoon),
                    )

                    _sunDaily.value = newSunDaily
                    _moonDaily.value = newMoonDaily
                }
        }

        // 2. Live ephemeris snapshots: ticks every 12s in auto mode, or reacts immediately to manual scrubbing.
        scope.launch {
            combine(
                locationRepository.coordinates.filterNotNull(),
                timeRepository.isAutoUpdateEnabled,
                timeRepository.currentZoneId,
            ) { coords, isAuto, zoneId -> Triple(coords, isAuto, zoneId) }
                .flatMapLatest { (coords, isAuto, zoneId) ->
                    if (isAuto) {
                        flow {
                            while (true) {
                                emit(coords to ZonedDateTime.now(zoneId))
                                delay(12.seconds)
                            }
                        }
                    } else {
                        timeRepository.currentTime.map { time -> coords to time }
                    }
                }
                .collectLatest { (coords, targetTime) ->
                    val updated = getLiveUpdates(targetTime, coords)
                    currentCoroutineContext().ensureActive()
                    _liveUpdates.value = updated
                }
        }
    }
}
