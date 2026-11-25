package com.ephemeris.helios.data

import com.ephemeris.helios.ui.composables.cards.ChartArrays
import com.ephemeris.helios.ui.composables.cards.X_SIZE
import com.ephemeris.helios.ui.composables.cards.generateMoonData
import com.ephemeris.helios.ui.composables.cards.generateSunData
import com.ephemeris.helios.utils.calc.LiveUpdatesData
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.LunarLunisticeEphemeris
import com.ephemeris.helios.utils.calc.MoonLiveSnapshot
import com.ephemeris.helios.utils.calc.MoonMetrics
import com.ephemeris.helios.utils.calc.SeasonalBundle
import com.ephemeris.helios.utils.calc.SeasonalEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.calc.SunLiveSnapshot
import com.ephemeris.helios.utils.calc.SunMetrics
import com.ephemeris.helios.utils.calc.getLiveUpdates
import com.ephemeris.helios.utils.calc.getSeasonalEphemerisData
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.location.estimateHistoricalOzone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
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
    val lunisticeEvents: LunarLunisticeEphemeris.YearlyLunistices,
)

/**
 * Atomic Sun + Moon daily bundle so combo charts & maps always update both curves in a single frame.
 */
data class DailyEphemerisBundle(
    val sun: SunDailyData,
    val moon: MoonDailyData,
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
    val lunisticeEvents: LunarLunisticeEphemeris.YearlyLunistices get() = daily.lunisticeEvents
}

private data class SeasonalCacheKey(
    val year: Int,
    val zoneId: ZoneId,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val useHorizonDip: Boolean,
    val useInstantaneousSemidiameter: Boolean,
)

private data class DailyCacheKey(
    val coords: Coordinates,
    val date: LocalDate,
    val zoneId: ZoneId,
)

/**
 * Reactive ephemeris engine shared across all screens.
 *
 * Uses `resetGeneration.collectLatest { ... .conflate().collect { ... } }`:
 * - During rapid multi-day TimeMachine flings (`resetGeneration` unchanged), `.conflate().collect`
 *   finishes each calculation & publishes smoothly without starvation.
 * - When the user taps "Reset to now" (`resetGeneration` increments), `collectLatest` immediately
 *   cancels any in-flight past/future calculation and jumps straight to computing `now`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EphemerisRepository(
    private val locationRepository: LocationRepository,
    private val timeRepository: TimeRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Constant 00:00..24:00 sampling axes reused across every day calculation
    private val hoursCalc = DoubleArray(X_SIZE) { it / 60.0 }
    private val hoursFloat = FloatArray(X_SIZE) { hoursCalc[it].toFloat() }

    // Cache yearly equinox/solstice & lunar lunistice calculations so scrubbing across days skips them
    @Volatile private var cachedSeasonalKey: SeasonalCacheKey? = null
    @Volatile private var cachedSeasonalBundle: SeasonalBundle? = null
    @Volatile private var cachedLunisticeKey: SeasonalCacheKey? = null
    @Volatile private var cachedLunisticeData: LunarLunisticeEphemeris.YearlyLunistices? = null
    @Volatile private var lastPublishedDailyKey: DailyCacheKey? = null

    private val _dailyData = MutableStateFlow<DailyEphemerisBundle?>(null)
    val dailyData: StateFlow<DailyEphemerisBundle?> = _dailyData.asStateFlow()

    private val _liveUpdates = MutableStateFlow<LiveUpdatesData?>(null)
    val liveUpdates: StateFlow<LiveUpdatesData?> = _liveUpdates.asStateFlow()

    val isReady: StateFlow<Boolean> = combine(
        _dailyData,
        _liveUpdates,
    ) { daily, live ->
        (daily != null) && (live != null)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    init {
        // 1. Daily ephemeris & 1441-point chart arrays:
        scope.launch {
            timeRepository.resetGeneration.collectLatest {
                combine(
                    locationRepository.coordinates.filterNotNull(),
                    timeRepository.currentTime,
                ) { coords, time -> coords to time }
                    .distinctUntilChanged { (oldCoords, oldTime), (newCoords, newTime) ->
                        oldCoords == newCoords &&
                            oldTime.toLocalDate() == newTime.toLocalDate() &&
                            oldTime.zone == newTime.zone
                    }
                    .conflate()
                    .collect { (coords, targetTime) ->
                        val localDate = targetTime.toLocalDate()
                        val dailyKey = DailyCacheKey(coords, localDate, targetTime.zone)
                        if (dailyKey == lastPublishedDailyKey && _dailyData.value != null) {
                            return@collect
                        }

                        val dailyOzone = estimateHistoricalOzone(
                            latitude = coords.latitude,
                            date = localDate,
                            longitude = coords.longitude,
                            altitudeMeters = coords.altitude,
                        )

                        val seasonalKey = SeasonalCacheKey(
                            year = targetTime.year,
                            zoneId = targetTime.zone,
                            latitude = coords.latitude,
                            longitude = coords.longitude,
                            altitude = coords.altitude,
                            useHorizonDip = coords.useHorizonDip,
                            useInstantaneousSemidiameter = coords.useInstantaneousSemidiameter,
                        )
                        val seasonal = if (seasonalKey == cachedSeasonalKey && cachedSeasonalBundle != null) {
                            cachedSeasonalBundle!!
                        } else {
                            getSeasonalEphemerisData(targetTime.year, targetTime.zone, coords).also {
                                cachedSeasonalKey = seasonalKey
                                cachedSeasonalBundle = it
                            }
                        }

                        currentCoroutineContext().ensureActive()

                        // Run Sun and Moon daily math in parallel across CPU cores
                        val bundle = coroutineScope {
                            val sunDeferred = async {
                                val events = SolarEphemeris.calculateDailyEvents(targetTime, coords)
                                val durations = SolarEphemeris.calculateDailyDurations(
                                    events = events,
                                    coordinates = coords,
                                    time = targetTime,
                                )
                                val peakMetrics = SunMetrics.calculateMetrics(
                                    sunElevationDeg = events.solarNoonAltitude,
                                    observerAltitudeMeters = coords.altitude,
                                    distanceAu = events.distanceAu,
                                    ozoneDU = dailyOzone,
                                    horizonAltDeg = coords.sunApparentHorizonAlt(events.distanceAu),
                                )
                                currentCoroutineContext().ensureActive()
                                val (xSun, ySun) = generateSunData(localDate, hoursCalc, coords, targetTime.zone, dailyOzone)
                                SunDailyData(
                                    events = events,
                                    durations = durations,
                                    dailyPeakMetrics = peakMetrics,
                                    seasonalEvents = seasonal.seasonalEvents,
                                    seasonalDailyEvents = seasonal.seasonalDailyEvents,
                                    chartArrays = ChartArrays(hoursFloat, xSun, ySun),
                                )
                            }

                            val moonDeferred = async {
                                val lunarModel = LunarEphemeris.createDailyModel(localDate, coords, targetTime.zone)
                                currentCoroutineContext().ensureActive()
                                val lunarEvents = lunarModel.calculateDailyEvents()
                                val lunarPeakMetrics = MoonMetrics.calculatePeakMetrics(
                                    time = targetTime,
                                    coordinates = coords,
                                    dailyEvents = lunarEvents,
                                )
                                currentCoroutineContext().ensureActive()
                                val (xMoon, yMoon) = generateMoonData(
                                    lunarModel = lunarModel,
                                    localDate = localDate,
                                    hoursCalc = hoursCalc,
                                    coordinates = coords,
                                    zoneId = targetTime.zone,
                                )
                                val lunistices = if (seasonalKey == cachedLunisticeKey && cachedLunisticeData != null) {
                                    cachedLunisticeData!!
                                } else {
                                    LunarLunisticeEphemeris.getYearlyLunistices(
                                        year = targetTime.year,
                                        zoneId = targetTime.zone,
                                        coordinates = coords,
                                    ).also {
                                        cachedLunisticeKey = seasonalKey
                                        cachedLunisticeData = it
                                    }
                                }
                                MoonDailyData(
                                    events = lunarEvents,
                                    dailyPeakMetrics = lunarPeakMetrics,
                                    chartArrays = ChartArrays(hoursFloat, xMoon, yMoon),
                                    lunisticeEvents = lunistices,
                                )
                            }

                            DailyEphemerisBundle(
                                sun = sunDeferred.await(),
                                moon = moonDeferred.await(),
                            )
                        }

                        currentCoroutineContext().ensureActive()
                        _dailyData.value = bundle
                        lastPublishedDailyKey = dailyKey
                    }
            }
        }

        // 2. Live ephemeris snapshots: ticks every 12s in auto mode, or reacts smoothly to manual scrubbing.
        //    Wrapped in resetGeneration.collectLatest so Reset cancels any in-flight past/future calculation immediately.
        scope.launch {
            timeRepository.resetGeneration.collectLatest {
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
                    .conflate()
                    .collect { (coords, targetTime) ->
                        val live = getLiveUpdates(targetTime, coords)
                        currentCoroutineContext().ensureActive()
                        _liveUpdates.value = live
                    }
            }
        }
    }
}
