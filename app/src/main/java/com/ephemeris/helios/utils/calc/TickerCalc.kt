package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.location.estimateHistoricalOzone
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max

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

fun getDailyEphemerisData(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
): DayEphemerisData {
    val events = SolarEphemeris.calculateDailyEvents(currentTime, coordinates)
    val durations = SolarEphemeris.calculateDailyDurations(events, coordinates)
    val dailyOzone = estimateHistoricalOzone(
        latitude = coordinates.latitude,
        date = currentTime.toLocalDate()
    )

    val dynamicSunriseSunsetAlt = coordinates.sunApparentHorizonAlt

    val dailyPeakMetrics = SunMetrics.calculateMetrics(
        sunElevationDeg = events.solarNoonAltitude,
        observerAltitudeMeters = coordinates.altitude,
        distanceAu = events.distanceAu,
        ozoneDU = dailyOzone,
        horizonAltDeg = dynamicSunriseSunsetAlt
    )

    val seasonalEvents = SeasonalEphemeris.getSeasonalEvents(currentTime.year, ZoneId.systemDefault())
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
        decemberSolsticeSunAngle = dSo.solarNoonAltitude
    )

    val lunarEvents = LunarEphemeris.calculateDailyEvents(currentTime, coordinates)
    val lunarDailyPeakMetrics = MoonMetrics.calculatePeakMetrics(currentTime, coordinates)

    return DayEphemerisData(events, durations, dailyPeakMetrics, seasonalEvents, seasonalDailyEvents, lunarEvents, lunarDailyPeakMetrics)
}

fun getSunLiveSnapshot(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
): SunLiveSnapshot {
    val pos = SolarEphemeris.calculatePosition(currentTime, coordinates)
    val dailyOzone = estimateHistoricalOzone(
        latitude = coordinates.latitude,
        date = currentTime.toLocalDate()
    )

    // Calculate the dynamic horizon specifically for the live observer
    val elevation = max(0.0, coordinates.altitude)
    val horizonDipDeg = 0.032 * kotlin.math.sqrt(elevation)
    val dynamicSunriseSunsetAlt = SolarEphemeris.ALT_SUNRISE_SUNSET - horizonDipDeg

    val metrics = SunMetrics.calculateMetrics(
        sunElevationDeg = pos.altitude,
        observerAltitudeMeters = coordinates.altitude,
        distanceAu = pos.distanceAu,
        ozoneDU = dailyOzone,
        solarSpeed = pos.solarSpeedDegPerMin,
        horizonAltDeg = dynamicSunriseSunsetAlt
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