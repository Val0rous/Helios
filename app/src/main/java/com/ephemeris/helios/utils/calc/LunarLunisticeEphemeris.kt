package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes annual lunar standstills (lunistices — northern and southern declination extrema)
 * for a given local calendar year and classifies the 18.6-year lunar nodal cycle phase.
 *
 * Unlike the Sun (whose solstices stay fixed near ±23.44°), the Moon reaches a North and South
 * lunistice every tropical month (~27.32 days, ~13–14 pairs per year) while its peak declination
 * envelope drifts between ~±18.1° (Minor Standstill) and ~±28.7° (Major Standstill) over the
 * 18.613-year regression of the lunar nodes.
 */
object LunarLunisticeEphemeris {

    enum class StandstillCyclePhase {
        MAJOR_STANDSTILL,
        MINOR_STANDSTILL,
        NEAR_MAJOR_WIDENING,
        NEAR_MAJOR_NARROWING,
        NEAR_MINOR_NARROWING,
        NEAR_MINOR_WIDENING,
        WIDENING,
        NARROWING,
    }

    data class LunisticeEvent(
        val dateTime: ZonedDateTime,
        val declinationDeg: Double,
        val moonUptimeHours: Double,
        val maxAltitudeDeg: Double?,
        val isNorth: Boolean = declinationDeg >= 0.0,
    )

    data class YearlyLunistices(
        val year: Int,
        val maxNorth: LunisticeEvent,
        val maxSouth: LunisticeEvent,
        val minNorth: LunisticeEvent,
        val minSouth: LunisticeEvent,
        val cyclePhase: StandstillCyclePhase,
        val cycleStartAbsDecDeg: Double,
        val cycleEndAbsDecDeg: Double,
        val totalLunisticeCount: Int,
        val allLunistices: List<LunisticeEvent> = emptyList(),
    )

    private data class RawLunistice(
        val instant: Instant,
        val declinationDeg: Double,
    )

    private const val STEP_SECONDS = 12.0 * 3600.0 // 12h scan step (tropical half-month is ~328h)
    private const val BUFFER_SECONDS = 24.0 * 3600.0 // ±1 day margin to bracket Jan 1 / Dec 31 extrema
    private val GOLDEN_RATIO_CONJUGATE = (sqrt(5.0) - 1.0) / 2.0

    /**
     * Evaluates geocentric lunar declination in degrees at a continuous UTC epoch-second timestamp
     * using Meeus Chapter 47 (matching [LunarEphemeris.calculateDeclinationDeg]).
     */
    internal fun declinationAtEpochSeconds(epochSeconds: Double, yearHint: Int, monthHint: Int): Double {
        val jdUt = (epochSeconds / 86400.0) + 2440587.5
        val deltaTDays = calculateDeltaT(yearHint, monthHint) / 86400.0
        val t = (jdUt + deltaTDays - 2451545.0) / 36525.0

        val ecliptic = LunarEphemeris.calculateGeocentricEcliptic(t)
        val lambdaRad = Math.toRadians(ecliptic.longitudeDeg)
        val betaRad = Math.toRadians(ecliptic.latitudeDeg)
        val epsRad = Math.toRadians(23.439291 - 0.0130042 * t)

        val sinDec = (sin(betaRad) * cos(epsRad) + cos(betaRad) * sin(epsRad) * sin(lambdaRad))
            .coerceIn(-1.0, 1.0)
        return Math.toDegrees(asin(sinDec))
    }

    /**
     * Mean longitude of the Moon's ascending node Ω in `[0°, 360°)` (Meeus Eq. 47.7).
     * Ω regresses ~19.34°/yr: `Ω ≈ 0°` is Major Standstill, `Ω ≈ 180°` is Minor Standstill,
     * `0° < Ω < 180°` means peak declination is widening, `180° < Ω < 360°` means narrowing.
     */
    internal fun meanAscendingNodeDeg(year: Int): Double {
        val midYearJd = (ZonedDateTime.of(year, 7, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
            .toInstant().epochSecond / 86400.0) + 2440587.5
        val t = (midYearJd - 2451545.0) / 36525.0
        val raw = (125.0445479 - 1934.1362891 * t + 0.0020754 * t * t) % 360.0
        return if (raw < 0.0) raw + 360.0 else raw
    }

    private fun refineExtremum(
        leftSec: Double,
        rightSec: Double,
        findMax: Boolean,
        yearHint: Int,
        monthHint: Int,
    ): RawLunistice {
        var a = leftSec
        var b = rightSec
        var c = b - GOLDEN_RATIO_CONJUGATE * (b - a)
        var d = a + GOLDEN_RATIO_CONJUGATE * (b - a)
        var fc = declinationAtEpochSeconds(c, yearHint, monthHint)
        var fd = declinationAtEpochSeconds(d, yearHint, monthHint)

        // 28 golden-section iterations narrow a 24h bracket to < 0.2 seconds
        repeat(28) {
            val pickLeft = if (findMax) fc > fd else fc < fd
            if (pickLeft) {
                b = d
                d = c
                fd = fc
                c = b - GOLDEN_RATIO_CONJUGATE * (b - a)
                fc = declinationAtEpochSeconds(c, yearHint, monthHint)
            } else {
                a = c
                c = d
                fc = fd
                d = a + GOLDEN_RATIO_CONJUGATE * (b - a)
                fd = declinationAtEpochSeconds(d, yearHint, monthHint)
            }
        }

        val bestSec = 0.5 * (a + b)
        val bestDec = declinationAtEpochSeconds(bestSec, yearHint, monthHint)
        return RawLunistice(
            instant = Instant.ofEpochMilli((bestSec * 1000.0).toLong()),
            declinationDeg = bestDec,
        )
    }

    private fun findAllYearlyRawLunistices(
        year: Int,
        zoneId: ZoneId,
    ): Pair<List<RawLunistice>, List<RawLunistice>> {
        val yearStart = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, zoneId).toInstant()
        val yearEnd = ZonedDateTime.of(year + 1, 1, 1, 0, 0, 0, 0, zoneId).toInstant()

        val scanStartSec = yearStart.epochSecond - BUFFER_SECONDS
        val scanEndSec = yearEnd.epochSecond + BUFFER_SECONDS
        val count = ((scanEndSec - scanStartSec) / STEP_SECONDS).toInt() + 2

        val times = DoubleArray(count) { i -> scanStartSec + i * STEP_SECONDS }
        val decs = DoubleArray(count) { i ->
            val approxMonth = (((times[i] - yearStart.epochSecond) / (30.4375 * 86400.0)).toInt() + 1)
                .coerceIn(1, 12)
            declinationAtEpochSeconds(times[i], year, approxMonth)
        }

        val north = mutableListOf<RawLunistice>()
        val south = mutableListOf<RawLunistice>()

        for (i in 1 until count - 1) {
            val prev = decs[i - 1]
            val curr = decs[i]
            val next = decs[i + 1]
            val approxMonth = (((times[i] - yearStart.epochSecond) / (30.4375 * 86400.0)).toInt() + 1)
                .coerceIn(1, 12)

            if (curr > 0.0 && curr >= prev && curr >= next && (curr > prev || curr > next)) {
                val refined = refineExtremum(times[i - 1], times[i + 1], findMax = true, year, approxMonth)
                if (!refined.instant.isBefore(yearStart) && refined.instant.isBefore(yearEnd)) {
                    val isDuplicate = north.lastOrNull()?.let {
                        abs(refined.instant.epochSecond - it.instant.epochSecond) < 86_400L
                    } ?: false
                    if (!isDuplicate) north.add(refined)
                }
            } else if (curr < 0.0 && curr <= prev && curr <= next && (curr < prev || curr < next)) {
                val refined = refineExtremum(times[i - 1], times[i + 1], findMax = false, year, approxMonth)
                if (!refined.instant.isBefore(yearStart) && refined.instant.isBefore(yearEnd)) {
                    val isDuplicate = south.lastOrNull()?.let {
                        abs(refined.instant.epochSecond - it.instant.epochSecond) < 86_400L
                    } ?: false
                    if (!isDuplicate) south.add(refined)
                }
            }
        }

        return north to south
    }

    private fun enrichEvent(
        raw: RawLunistice,
        zoneId: ZoneId,
        coordinates: Coordinates,
    ): LunisticeEvent {
        val dt = ZonedDateTime.ofInstant(raw.instant, zoneId)
        val events = LunarEphemeris.calculateDailyEvents(dt, coordinates)
        var maxAlt = events.culminationAltitude

        // Because the lunar day is ~24h 50m, ~1 day per month has no upper culmination.
        // Fall back to the nearest neighboring day's culmination so the lunistice altitude stays populated.
        if (maxAlt == null) {
            val prev = LunarEphemeris.calculateDailyEvents(dt.minusDays(1), coordinates)
            val next = LunarEphemeris.calculateDailyEvents(dt.plusDays(1), coordinates)
            maxAlt = when {
                prev.culminationAltitude != null && next.culminationAltitude != null -> {
                    val prevDist = prev.culminationElapsedHours?.let {
                        abs(dt.toLocalTime().toSecondOfDay() / 3600.0 + (24.0 - it))
                    } ?: Double.MAX_VALUE
                    val nextDist = next.culminationElapsedHours?.let {
                        abs((24.0 - dt.toLocalTime().toSecondOfDay() / 3600.0) + it)
                    } ?: Double.MAX_VALUE
                    if (prevDist <= nextDist) prev.culminationAltitude else next.culminationAltitude
                }
                else -> prev.culminationAltitude ?: next.culminationAltitude
            }
        }

        return LunisticeEvent(
            dateTime = dt,
            declinationDeg = raw.declinationDeg,
            moonUptimeHours = events.moonDuration,
            maxAltitudeDeg = maxAlt,
        )
    }

    /**
     * Calculates the 4 annual lunistice extrema (most extreme & shallowest North and South)
     * strictly within local calendar [year] in [zoneId], enriched with observer uptime and
     * culmination altitude at [coordinates], plus 18.6-year nodal standstill cycle classification.
     */
    fun getYearlyLunistices(
        year: Int,
        zoneId: ZoneId,
        coordinates: Coordinates,
    ): YearlyLunistices {
        val (north, south) = findAllYearlyRawLunistices(year, zoneId)
        require(north.isNotEmpty() && south.isNotEmpty()) {
            "Expected at least one north and south lunistice in year $year"
        }

        val northEvents = north.map { enrichEvent(it, zoneId, coordinates) }
        val southEvents = south.map { enrichEvent(it, zoneId, coordinates) }
        val allLunistices = (northEvents + southEvents).sortedBy { it.dateTime }

        val maxNorth = northEvents.maxBy { it.declinationDeg }
        val minNorth = northEvents.minBy { it.declinationDeg }
        val maxSouth = southEvents.minBy { it.declinationDeg }
        val minSouth = southEvents.maxBy { it.declinationDeg }

        val peakAbsDec = max(abs(maxNorth.declinationDeg), abs(maxSouth.declinationDeg))
        val minAbsDec = min(abs(minNorth.declinationDeg), abs(minSouth.declinationDeg))

        // Estimate start-of-year and end-of-year envelope magnitudes (averaging first/last N+S pair
        // to smooth out the slight asymmetry between individual northern and southern passes).
        val startAbsDec = 0.5 * (abs(northEvents.first().declinationDeg) + abs(southEvents.first().declinationDeg))
        val endAbsDec = 0.5 * (abs(northEvents.last().declinationDeg) + abs(southEvents.last().declinationDeg))

        val omegaMid = meanAscendingNodeDeg(year)
        val isWidening = omegaMid in 0.0..<180.0

        val cyclePhase = when {
            peakAbsDec >= 28.45 -> StandstillCyclePhase.MAJOR_STANDSTILL
            minAbsDec <= 18.55 -> StandstillCyclePhase.MINOR_STANDSTILL
            peakAbsDec >= 27.2 -> if (isWidening) {
                StandstillCyclePhase.NEAR_MAJOR_WIDENING
            } else {
                StandstillCyclePhase.NEAR_MAJOR_NARROWING
            }
            minAbsDec <= 19.8 -> if (isWidening) {
                StandstillCyclePhase.NEAR_MINOR_WIDENING
            } else {
                StandstillCyclePhase.NEAR_MINOR_NARROWING
            }
            isWidening -> StandstillCyclePhase.WIDENING
            else -> StandstillCyclePhase.NARROWING
        }

        return YearlyLunistices(
            year = year,
            maxNorth = maxNorth,
            maxSouth = maxSouth,
            minNorth = minNorth,
            minSouth = minSouth,
            cyclePhase = cyclePhase,
            cycleStartAbsDecDeg = startAbsDec,
            cycleEndAbsDecDeg = endAbsDec,
            totalLunisticeCount = allLunistices.size,
            allLunistices = allLunistices,
        )
    }
}
