package com.ephemeris.helios.utils.calc

import android.util.Log
import com.ephemeris.helios.utils.location.Coordinates
import java.time.ZoneId
import java.time.ZonedDateTime

data class DayEphemerisData(
    val events: SolarEphemeris.DailyEvents,
    val durations: SolarEphemeris.DailyDurations,
    val dailyPeakMetrics: SunMetrics.SunMetricsResult,
    val seasonalEvents: SeasonalEphemeris.SeasonalEvents,
    val seasonalDailyEvents: SeasonalEphemeris.SeasonalDailyEvents,
    val lunarEvents: LunarEphemeris.LunarDailyEvents,
    val lunarDailyPeakMetrics: MoonMetrics.LunarMetricsResult?,
)

data class LiveUpdatesData(
    val currentSunPosition: SolarEphemeris.SolarPosition,
    val liveSunMetrics: SunMetrics.SunMetricsResult,
    val currentMoonPosition: LunarEphemeris.LunarPosition,
    val liveMoonMetrics: MoonMetrics.LunarMetricsResult
)

fun getDailyEphemerisData(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
): DayEphemerisData {
    val events = SolarEphemeris.calculateDailyEvents(currentTime, coordinates)
    val durations = SolarEphemeris.calculateDailyDurations(events)
    val dailyPeakMetrics = SunMetrics.calculateMetrics(events.solarNoonAltitude, coordinates.altitude)

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

fun getLiveUpdates(
    currentTime: ZonedDateTime,
    coordinates: Coordinates
): LiveUpdatesData {
    val pos = SolarEphemeris.calculatePosition(currentTime, coordinates)
    val metrics = SunMetrics.calculateMetrics(pos.altitude, coordinates.altitude)

    val lunarPos = LunarEphemeris.calculatePosition(currentTime, coordinates)
    val lunarMetrics = MoonMetrics.calculateMetrics(currentTime, coordinates)

    return LiveUpdatesData(pos, metrics, lunarPos, lunarMetrics)
}