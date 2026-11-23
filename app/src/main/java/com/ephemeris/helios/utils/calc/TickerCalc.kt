package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.location.estimateHistoricalOzone
import java.time.ZoneId
import java.time.ZonedDateTime

data class SeasonalBundle(
    val seasonalEvents: SeasonalEphemeris.SeasonalEvents,
    val seasonalDailyEvents: SeasonalEphemeris.SeasonalDailyEvents,
)

data class DayEphemerisData(
    val events: SolarEphemeris.DailyEvents,
    val durations: SolarEphemeris.DailyDurations,
    val dailyPeakMetrics: SunMetrics.SunMetricsResult,
    val seasonalEvents: SeasonalEphemeris.SeasonalEvents,
    val seasonalDailyEvents: SeasonalEphemeris.SeasonalDailyEvents,
    val lunarEvents: LunarEphemeris.LunarDailyEvents,
    val lunarDailyPeakMetrics: MoonMetrics.LunarMetricsResult?,
)

data class SunLiveSnapshot(
    val timestamp: ZonedDateTime,
    val position: SolarEphemeris.SolarPosition,
    val metrics: SunMetrics.SunMetricsResult,
)

data class MoonLiveSnapshot(
    val timestamp: ZonedDateTime,
    val position: LunarEphemeris.LunarPosition,
    val metrics: MoonMetrics.LunarMetricsResult,
)

data class LiveUpdatesData(
    val timestamp: ZonedDateTime,
    val sun: SunLiveSnapshot,
    val moon: MoonLiveSnapshot,
) {
    val currentSunPosition: SolarEphemeris.SolarPosition get() = sun.position
    val liveSunMetrics: SunMetrics.SunMetricsResult get() = sun.metrics
    val currentMoonPosition: LunarEphemeris.LunarPosition get() = moon.position
    val liveMoonMetrics: MoonMetrics.LunarMetricsResult get() = moon.metrics
}

/**
 * Computes yearly equinoxes/solstices and their daylight/sun-angle metrics.
 * Depends only on (year, zoneId, coordinates), so callers can cache this across all 365 days of a year.
 */
fun getSeasonalEphemerisData(
    year: Int,
    zoneId: ZoneId,
    coordinates: Coordinates,
): SeasonalBundle {
    val seasonalEvents = SeasonalEphemeris.getSeasonalEvents(year, zoneId)
    val mEq = SeasonalEphemeris.getDaily(seasonalEvents.marchEquinox, coordinates)
    val jSo = SeasonalEphemeris.getDaily(seasonalEvents.juneSolstice, coordinates)
    val sEq = SeasonalEphemeris.getDaily(seasonalEvents.septemberEquinox, coordinates)
    val dSo = SeasonalEphemeris.getDaily(seasonalEvents.decemberSolstice, coordinates)

    val seasonalDailyEvents = SeasonalEphemeris.SeasonalDailyEvents(
        marchEquinoxDaylight = mEq.dayLength,
        juneSolsticeDaylight = jSo.dayLength,
        septemberEquinoxDaylight = sEq.dayLength,
        decemberSolsticeDaylight = dSo.dayLength,
        marchEquinoxSunAngle = mEq.solarNoonAltitude,
        juneSolsticeSunAngle = jSo.solarNoonAltitude,
        septemberEquinoxSunAngle = sEq.solarNoonAltitude,
        decemberSolsticeSunAngle = dSo.solarNoonAltitude,
    )
    return SeasonalBundle(seasonalEvents, seasonalDailyEvents)
}

fun getDailyEphemerisData(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    cachedSeasonal: SeasonalBundle? = null,
): DayEphemerisData {
    val events = SolarEphemeris.calculateDailyEvents(currentTime, coordinates)
    val durations = SolarEphemeris.calculateDailyDurations(events, coordinates, time = currentTime)
    val dailyOzone = estimateHistoricalOzone(
        latitude = coordinates.latitude,
        date = currentTime.toLocalDate(),
    )

    val dynamicSunriseSunsetAlt = coordinates.sunApparentHorizonAlt

    val dailyPeakMetrics = SunMetrics.calculateMetrics(
        sunElevationDeg = events.solarNoonAltitude,
        observerAltitudeMeters = coordinates.altitude,
        distanceAu = events.distanceAu,
        ozoneDU = dailyOzone,
        horizonAltDeg = dynamicSunriseSunsetAlt,
    )

    val seasonal = cachedSeasonal ?: getSeasonalEphemerisData(
        year = currentTime.year,
        zoneId = currentTime.zone,
        coordinates = coordinates,
    )

    val lunarEvents = LunarEphemeris.calculateDailyEvents(currentTime, coordinates)
    val lunarDailyPeakMetrics = MoonMetrics.calculatePeakMetrics(currentTime, coordinates)

    return DayEphemerisData(
        events = events,
        durations = durations,
        dailyPeakMetrics = dailyPeakMetrics,
        seasonalEvents = seasonal.seasonalEvents,
        seasonalDailyEvents = seasonal.seasonalDailyEvents,
        lunarEvents = lunarEvents,
        lunarDailyPeakMetrics = lunarDailyPeakMetrics,
    )
}

fun getSunLiveSnapshot(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
): SunLiveSnapshot {
    val pos = SolarEphemeris.calculatePosition(currentTime, coordinates)
    val dailyOzone = estimateHistoricalOzone(
        latitude = coordinates.latitude,
        date = currentTime.toLocalDate(),
    )

    val dynamicSunriseSunsetAlt = coordinates.sunApparentHorizonAlt

    val metrics = SunMetrics.calculateMetrics(
        sunElevationDeg = pos.altitude,
        observerAltitudeMeters = coordinates.altitude,
        distanceAu = pos.distanceAu,
        ozoneDU = dailyOzone,
        solarSpeed = pos.solarSpeedDegPerMin,
        horizonAltDeg = dynamicSunriseSunsetAlt,
    )

    return SunLiveSnapshot(
        timestamp = currentTime,
        position = pos,
        metrics = metrics,
    )
}

fun getMoonLiveSnapshot(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
): MoonLiveSnapshot {
    val lunarPos = LunarEphemeris.calculatePosition(currentTime, coordinates)
    val lunarMetrics = MoonMetrics.calculateMetrics(currentTime, coordinates)

    return MoonLiveSnapshot(
        timestamp = currentTime,
        position = lunarPos,
        metrics = lunarMetrics,
    )
}

fun getLiveUpdates(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
): LiveUpdatesData {
    return LiveUpdatesData(
        timestamp = currentTime,
        sun = getSunLiveSnapshot(currentTime, coordinates),
        moon = getMoonLiveSnapshot(currentTime, coordinates),
    )
}
