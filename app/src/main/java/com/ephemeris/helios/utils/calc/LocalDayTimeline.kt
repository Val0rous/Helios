package com.ephemeris.helios.utils.calc

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Maps between a local calendar day's continuous physical timeline (`elapsedHour ∈ [0, durationHours]`,
 * where `durationHours` is `23.0` on spring-forward DST days, `24.0` on standard days, and `25.0`
 * on fall-back DST days) and local wall-clock decimal hours (`0.0..24.0`).
 *
 * All astronomical root-finding and integration run on the continuous `elapsedHour` timeline using
 * [startUtcAnchorOffsetHours] (which equals the zone's UTC offset at `00:00` local time, and also
 * handles midnight DST transitions such as `America/Havana` cleanly), eliminating ±1h shifts on
 * DST transition days with zero overhead on normal 24-hour days.
 */
class LocalDayTimeline(
    val date: LocalDate,
    val zoneId: ZoneId,
) {
    val startOfDay: ZonedDateTime = date.atStartOfDay(zoneId)
    val startInstant: Instant = startOfDay.toInstant()
    val endInstant: Instant = date.plusDays(1).atStartOfDay(zoneId).toInstant()
    val durationHours: Double = Duration.between(startInstant, endInstant).seconds / 3600.0

    /**
     * Effective UTC offset in hours anchored at [startInstant], such that for any physical
     * `elapsedHour` since local start-of-day:
     * `utcDecimalHour(since date 00:00 UTC) == elapsedHour - startUtcAnchorOffsetHours`.
     */
    val startUtcAnchorOffsetHours: Double =
        Duration.between(startInstant, date.atStartOfDay().toInstant(ZoneOffset.UTC)).seconds / 3600.0

    val hasDstTransition: Boolean = abs(durationHours - 24.0) > 1e-6

    /**
     * Converts a physical [elapsedHour] since start-of-day (`[0, durationHours]`) to the exact
     * unambiguous [ZonedDateTime] in [zoneId].
     */
    fun elapsedHourToZonedDateTime(elapsedHour: Double): ZonedDateTime {
        val seconds = (elapsedHour * 3600.0).roundToLong()
        return ZonedDateTime.ofInstant(startInstant.plusSeconds(seconds), zoneId)
    }

    /**
     * Converts a physical [elapsedHour] since start-of-day (`[0, durationHours]`) into the
     * observer's local wall-clock decimal hour (`[0, 24)` — or `24.0` at exact next midnight).
     */
    fun elapsedHourToWallClockHour(elapsedHour: Double): Double {
        if (!hasDstTransition) return elapsedHour
        val millis = (elapsedHour * 3_600_000.0).roundToLong()
        val instant = startInstant.plusMillis(millis)
        val zdt = ZonedDateTime.ofInstant(instant, zoneId)
        val dayDiff = ChronoUnit.DAYS.between(date, zdt.toLocalDate())
        return dayDiff * 24.0 +
            zdt.hour +
            zdt.minute / 60.0 +
            zdt.second / 3600.0 +
            zdt.nano / 3_600_000_000_000.0
    }

    /**
     * Converts a local wall-clock decimal hour (`0..24`) on [date] into the corresponding physical
     * elapsed hour since start-of-day (`[0, durationHours]`).
     *
     * - On normal days: returns [wallClockHour] in O(1).
     * - On spring-forward days: wall-clock times inside the non-existent gap pin smoothly to the
     *   transition instant so daily chart curves bridge seamlessly without a vertical cliff.
     * - On fall-back days: uses the zone's standard earlier offset inside the repeated hour and
     *   the post-transition offset afterward.
     */
    fun wallClockHourToElapsedHour(wallClockHour: Double): Double {
        if (!hasDstTransition) return wallClockHour
        val clampedSeconds = (wallClockHour * 3600.0).roundToLong().coerceIn(0L, 86_400L)
        if (clampedSeconds == 86_400L) return durationHours
        val ldt = LocalDateTime.of(date, LocalTime.ofSecondOfDay(clampedSeconds))
        val transition = zoneId.rules.getTransition(ldt)
        val instant = if (transition != null && transition.isGap) {
            transition.instant
        } else {
            ZonedDateTime.of(ldt, zoneId).toInstant()
        }
        return Duration.between(startInstant, instant).seconds / 3600.0
    }

    companion object {
        fun fixedOffset(date: LocalDate, tzOffsetHours: Double): LocalDayTimeline =
            LocalDayTimeline(date, ZoneOffset.ofTotalSeconds((tzOffsetHours * 3600.0).roundToInt()))
    }
}
