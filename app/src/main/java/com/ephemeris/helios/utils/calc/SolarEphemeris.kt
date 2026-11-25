package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.LightPhasePreferences
import com.ephemeris.helios.utils.round
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.*

object SolarEphemeris {

    // Target Altitudes (in degrees)
    const val REFRACTION_HORIZON_DEG = 0.567
    const val MEAN_SEMI_DIAMETER_DEG = 0.266
    const val SOLAR_RADIUS_AU = 695_700.0 / 149_597_870.7
    const val ALT_SUNRISE_SUNSET = -0.833
    const val ALT_CIVIL_TWILIGHT = -6.0
    const val ALT_NAUTICAL_TWILIGHT = -12.0
    const val ALT_ASTRONOMICAL_TWILIGHT = -18.0
    const val ALT_BLUE_HOUR_LOWER = -6.0 // Matches Civil Twilight
    const val ALT_BLUE_HOUR_UPPER = -4.0
    const val ALT_GOLDEN_HOUR_LOWER = -4.0
    const val ALT_GOLDEN_HOUR_UPPER = 6.0
    const val ALT_PLUTO_TIME = -2.20
    /** Narrow illumination band around [ALT_PLUTO_TIME] (~84–140 lux, Pluto's mean-to-perihelion noon). */
    const val ALT_PLUTO_TIME_LOWER = -2.50
    const val ALT_PLUTO_TIME_UPPER = -1.90

    /** Instantaneous geocentric angular semidiameter of the Sun in degrees (~0.262° at aphelion to ~0.271° at perihelion). */
    fun calculateSemiDiameterDeg(distanceAu: Double): Double =
        Math.toDegrees(asin((SOLAR_RADIUS_AU / distanceAu).coerceIn(-1.0, 1.0)))

    data class SolarPosition(
        val altitude: Double,    // degrees
        val azimuth: Double,     // degrees
        val distanceAu: Double,  // Orbital distance
        val declination: Double, // degrees
        val eotMinutes: Double,  // Equation of Time
        val solarSpeedDegPerMin: Double // Instantaneous vertical speed
    )

    enum class PhaseDirection {
        RISING,
        SETTING,
        CONTINUOUS,
    }

    data class PhaseInterval(
        val startHour: Double?,
        val endHour: Double?,
        val durationHours: Double,
        val direction: PhaseDirection,
        val isAllDay: Boolean = false,
        val wrapsMidnight: Boolean = false,
    )

    data class SolarMidnightEvent(
        val time: Double,
        val elapsedHours: Double,
        val altitude: Double,
        val azimuth: Double,
    )

    data class DailyEvents(
        val solarNoon: Double,
        val solarNoonAltitude: Double,
        val solarNoonAzimuth: Double,
        val sunrise: Double?,
        val sunriseAzimuth: Double?,
        val sunset: Double?,
        val sunsetAzimuth: Double?,
        val dawnCivil: Double?,
        val duskCivil: Double?,
        val dawnNautical: Double?,
        val duskNautical: Double?,
        val dawnAstronomical: Double?,
        val duskAstronomical: Double?,
        val dayLength: Double,
        val solarMidnight: Double?,
        val solarMidnightAltitude: Double,
        val solarMidnightAzimuth: Double,
        val dawnGoldenLower: Double?,
        val dawnGoldenUpper: Double?,
        val duskGoldenUpper: Double?,
        val duskGoldenLower: Double?,
        val dawnPinkLower: Double?,
        val dawnPinkUpper: Double?,
        val duskPinkUpper: Double?,
        val duskPinkLower: Double?,
        val dawnBlueLower: Double?,
        val dawnBlueUpper: Double?,
        val duskBlueUpper: Double?,
        val duskBlueLower: Double?,
        val dawnAlpenglowLower: Double?,
        val dawnAlpenglowUpper: Double?,
        val duskAlpenglowUpper: Double?,
        val duskAlpenglowLower: Double?,
        val morningPlutoTime: Double?,
        val eveningPlutoTime: Double?,
        val distanceAu: Double,
        val solarNoonElapsedHours: Double = solarNoon,
        val solarMidnightElapsedHours: Double? = solarMidnight,
        val solarMidnights: List<SolarMidnightEvent> = emptyList(),
        val dawnPlutoLower: Double? = null,
        val dawnPlutoUpper: Double? = null,
        val duskPlutoUpper: Double? = null,
        val duskPlutoLower: Double? = null,
        val civilIntervals: List<PhaseInterval> = emptyList(),
        val nauticalIntervals: List<PhaseInterval> = emptyList(),
        val astroIntervals: List<PhaseInterval> = emptyList(),
        val goldenIntervals: List<PhaseInterval> = emptyList(),
        val pinkIntervals: List<PhaseInterval> = emptyList(),
        val blueIntervals: List<PhaseInterval> = emptyList(),
        val alpenglowIntervals: List<PhaseInterval> = emptyList(),
        val plutoIntervals: List<PhaseInterval> = emptyList(),
        val riseSetEvents: List<RiseSetEvent> = emptyList(),
    )

    data class DailyDurations(
        val civilTwilight: PhaseDurations,
        val nauticalTwilight: PhaseDurations,
        val astronomicalTwilight: PhaseDurations,
        val night: PhaseDurations,
        val goldenHour: PhaseDurations,
        val blueHour: PhaseDurations,
        val pinkHour: PhaseDurations,
        val alpenglow: PhaseDurations,
        val plutoTime: PhaseDurations = PhaseDurations(0.0, 0.0),
    )

    data class PhaseDurations(
        val morning: Double,
        val evening: Double
    ) {
        val total: Double get() = morning + evening
    }

    /**
     * Calculates all daily events (sunrise, sunset, twilights) in decimal hours, using iterative refinement for continuous accuracy.
     *
     * @param useInstantaneousSemidiameter When `true`, computes sunrise/sunset using the Sun's
     *   instantaneous distance-based angular semidiameter (`~0.262°..0.271°`) instead of the fixed
     *   mean `0.266°` (`-0.833°` total with standard `34'` refraction). Defaults to [Coordinates.useInstantaneousSemidiameter].
     */
    fun calculateDailyEvents(
        time: ZonedDateTime,
        coordinates: Coordinates,
        prefs: LightPhasePreferences = LightPhasePreferences(),
        useInstantaneousSemidiameter: Boolean = coordinates.useInstantaneousSemidiameter,
    ): DailyEvents {
        val latitude = coordinates.latitude
        val longitude = coordinates.longitude
        val latRad = Math.toRadians(latitude)
        val date = time.toLocalDate()
        val timeline = LocalDayTimeline(date, time.zone)
        val tzOffsetHours = timeline.startUtcAnchorOffsetHours
        val dayHours = timeline.durationHours

        // 1. Iterative Solar Noon (Meeus) in continuous elapsed hours since local start-of-day
        var exactSolarNoon = 12.0 - (longitude / 15.0) + tzOffsetHours
        for (i in 1..2) {
            val t = calculateJulianCentury(date, exactSolarNoon, tzOffsetHours)
            val params = calculateSolarParams(t)
            exactSolarNoon = 12.0 - (longitude / 15.0) - (params.eotMinutes / 60.0) + tzOffsetHours
        }
        val noonPos = getPositionAtHour(date, exactSolarNoon, latitude, longitude, tzOffsetHours)

        val dynamicSunriseSunsetAlt = if (useInstantaneousSemidiameter) {
            -(REFRACTION_HORIZON_DEG + calculateSemiDiameterDeg(noonPos.distanceAu)) - coordinates.horizonDipDeg
        } else {
            coordinates.sunApparentHorizonAlt
        }

        // Inject the dynamic horizon into the user's preferences
        val prefs = prefs.copy(apparentHorizonAlt = dynamicSunriseSunsetAlt)

        fun refineSolarCrossing(
            startHour: Double,
            endHour: Double,
            isRising: Boolean,
            signedAlt: (Double) -> Double,
        ): Double {
            var low = startHour
            var high = endHour
            for (i in 0..15) {
                val mid = (low + high) / 2.0
                if (signedAlt(mid) > 0.0) {
                    if (isRising) high = mid else low = mid
                } else {
                    if (isRising) low = mid else high = mid
                }
            }
            return (low + high) / 2.0
        }

        // Fallback scanner for polar / grazing crossings (handles exact ±90° poles, single-crossing
        // polar transition days, and arbitrarily brief grazing sunrise/sunset touches around solar noon/midnight).
        fun scanPolarEventTimes(signedAlt: (Double) -> Double): Pair<Double?, Double?> {
            var riseHour: Double? = null
            var setHour: Double? = null

            val stepMinutes = 10
            val stepHours = stepMinutes / 60.0
            val totalMinutes = (dayHours * 60.0).roundToInt()
            val numSamples = (totalMinutes / stepMinutes) + 1
            val signed = DoubleArray(numSamples)
            signed[0] = signedAlt(0.0)

            for (idx in 1 until numSamples) {
                val h = min(idx * stepHours, dayHours)
                val prevH = (idx - 1) * stepHours
                val curr = signedAlt(h)
                signed[idx] = curr
                val prev = signed[idx - 1]
                if (prev < 0.0 && curr >= 0.0) {
                    riseHour = refineSolarCrossing(prevH, h, isRising = true, signedAlt = signedAlt)
                } else if (prev > 0.0 && curr <= 0.0) {
                    setHour = refineSolarCrossing(prevH, h, isRising = false, signedAlt = signedAlt)
                }
            }

            // Guarantee even a <10-minute grazing touch at solar noon or midnight is captured
            if (riseHour == null && setHour == null) {
                val normalizedNoon = ((exactSolarNoon % 24.0) + 24.0) % 24.0
                val normalizedMidnight = ((normalizedNoon + 12.0) % 24.0)
                if (signed[0] < 0.0 && normalizedNoon in 0.0..dayHours && signedAlt(normalizedNoon) >= 0.0) {
                    val leftGrid = (floor(normalizedNoon / stepHours) * stepHours).coerceAtLeast(0.0)
                    val rightGrid = (ceil(normalizedNoon / stepHours) * stepHours).coerceAtMost(dayHours)
                    riseHour = refineSolarCrossing(leftGrid, normalizedNoon, isRising = true, signedAlt = signedAlt)
                    setHour = refineSolarCrossing(normalizedNoon, rightGrid, isRising = false, signedAlt = signedAlt)
                } else if (signed[0] >= 0.0 && normalizedMidnight in 0.0..dayHours && signedAlt(normalizedMidnight) < 0.0) {
                    val leftGrid = (floor(normalizedMidnight / stepHours) * stepHours).coerceAtLeast(0.0)
                    val rightGrid = (ceil(normalizedMidnight / stepHours) * stepHours).coerceAtMost(dayHours)
                    setHour = refineSolarCrossing(leftGrid, normalizedMidnight, isRising = false, signedAlt = signedAlt)
                    riseHour = refineSolarCrossing(normalizedMidnight, rightGrid, isRising = true, signedAlt = signedAlt)
                }
            }

            return Pair(riseHour, setHour)
        }

        // 2. The Iterative Event Engine for finding specific altitude times
        fun findPreciseEventTimes(
            targetAltDeg: Double,
            dynamicSunHorizon: Boolean = false,
        ): Pair<Double?, Double?> {
            fun targetAt(distanceAu: Double): Double = if (dynamicSunHorizon) {
                -(REFRACTION_HORIZON_DEG + calculateSemiDiameterDeg(distanceAu)) - coordinates.horizonDipDeg
            } else {
                targetAltDeg
            }

            fun signedAltAt(hour: Double): Double {
                val pos = getPositionAtHour(date, hour, latitude, longitude, tzOffsetHours)
                return pos.altitude - targetAt(pos.distanceAu)
            }

            // Near exact poles, cos(lat) approaches 0 so use the continuous topocentric scanner directly
            if (abs(latitude) >= 89.5) {
                return scanPolarEventTimes(::signedAltAt)
            }

            // Start with rough guesses: 6 AM for morning, 6 PM for evening
            var dawnEstimate = 6.0
            var duskEstimate = 18.0
            var nearPolarGrazing = false

            // Iterate 3 times for sub-second convergence
            for (i in 1..3) {
                val dawnT = calculateJulianCentury(date, dawnEstimate, tzOffsetHours)
                val dawnParams = calculateSolarParams(dawnT)
                val dawnTargetRad = Math.toRadians(targetAt(dawnParams.distanceAu))
                val dawnCosOmega = (sin(dawnTargetRad) - sin(latRad) * sin(dawnParams.declinationRad)) /
                    (cos(latRad) * cos(dawnParams.declinationRad))

                val duskT = calculateJulianCentury(date, duskEstimate, tzOffsetHours)
                val duskParams = calculateSolarParams(duskT)
                val duskTargetRad = Math.toRadians(targetAt(duskParams.distanceAu))
                val duskCosOmega = (sin(duskTargetRad) - sin(latRad) * sin(duskParams.declinationRad)) /
                    (cos(latRad) * cos(duskParams.declinationRad))

                // Near grazing or polar transition thresholds (|cosOmega| > 0.995), switch to the
                // continuous polar scanner so we never produce NaN or miss a brief grazing arch
                if (abs(dawnCosOmega) > 0.995 || abs(duskCosOmega) > 0.995) {
                    nearPolarGrazing = true
                    break
                }

                val dawnOmegaDeg = Math.toDegrees(acos(dawnCosOmega))
                val dawnSolarNoonAtThatMoment = 12.0 - (longitude / 15.0) - (dawnParams.eotMinutes / 60.0) + tzOffsetHours
                dawnEstimate = dawnSolarNoonAtThatMoment - (dawnOmegaDeg / 15.0)

                val duskOmegaDeg = Math.toDegrees(acos(duskCosOmega))
                val duskSolarNoonAtThatMoment = 12.0 - (longitude / 15.0) - (duskParams.eotMinutes / 60.0) + tzOffsetHours
                duskEstimate = duskSolarNoonAtThatMoment + (duskOmegaDeg / 15.0)
            }

            if (nearPolarGrazing) {
                return scanPolarEventTimes(::signedAltAt)
            }

            return Pair(dawnEstimate, duskEstimate)
        }

        // Use the dynamically adjusted altitude for Sunrise/Sunset ONLY!
        val sun = findPreciseEventTimes(
            targetAltDeg = dynamicSunriseSunsetAlt,
            dynamicSunHorizon = useInstantaneousSemidiameter,
        )
        val civil = findPreciseEventTimes(ALT_CIVIL_TWILIGHT)
        val nautical = findPreciseEventTimes(ALT_NAUTICAL_TWILIGHT)
        val astro = findPreciseEventTimes(ALT_ASTRONOMICAL_TWILIGHT)

        val goldenLower = findPreciseEventTimes(prefs.goldenHourRange.start)
        val goldenUpper = findPreciseEventTimes(prefs.goldenHourRange.endInclusive)

        val pinkLower = findPreciseEventTimes(prefs.pinkHourRange.start)
        val pinkUpper = findPreciseEventTimes(prefs.pinkHourRange.endInclusive)

        val blueLower = findPreciseEventTimes(prefs.blueHourRange.start)
        val blueUpper = findPreciseEventTimes(prefs.blueHourRange.endInclusive)

        val alpenglowLower = findPreciseEventTimes(prefs.alpenglowRange.start)
        val alpenglowUpper = findPreciseEventTimes(prefs.alpenglowRange.endInclusive)

        val pluto = findPreciseEventTimes(ALT_PLUTO_TIME)
        val plutoLower = findPreciseEventTimes(ALT_PLUTO_TIME_LOWER)
        val plutoUpper = findPreciseEventTimes(ALT_PLUTO_TIME_UPPER)

        // 3. Populate Azimuths for core events (evaluated on continuous elapsed timeline)
        val sunrisePos = sun.first?.let { getPositionAtHour(date, it, latitude, longitude, tzOffsetHours) }
        val sunsetPos = sun.second?.let { getPositionAtHour(date, it, latitude, longitude, tzOffsetHours) }

        val dayLength = getExactDayLength(sun.first, sun.second, noonPos.altitude, dynamicSunriseSunsetAlt, dayHours)

        // 4. Exact Solar Midnight (lower meridian transit, hour angle = ±180°) on [0, dayHours).
        // Because Equation of Time drift stretches the true solar day past 24h (notably around
        // December) and DST spring-forward shortens the civil day to 23h, a handful of days each
        // year have no lower meridian transit inside [0, dayHours). Solving without modulo
        // wrapping detects those skipped days cleanly instead of aliasing into the wrong day.
        val inDayMidnights = (-2..2).mapNotNull { k ->
            var candidate = (24.0 * k) - (longitude / 15.0) + tzOffsetHours
            for (i in 1..3) {
                val midnightT = calculateJulianCentury(date, candidate, tzOffsetHours)
                val midnightParams = calculateSolarParams(midnightT)
                candidate = (24.0 * k) - (longitude / 15.0) - (midnightParams.eotMinutes / 60.0) + tzOffsetHours
            }
            candidate.takeIf { it >= 0.0 && it < dayHours }
        }.sorted()

        fun Double.toWallClock(): Double = timeline.elapsedHourToWallClockHour(this)
        fun Double?.toWallClock(): Double? = this?.let { timeline.elapsedHourToWallClockHour(it) }

        val solarMidnightEvents = inDayMidnights.map { elapsed ->
            val pos = getPositionAtHour(date, elapsed, latitude, longitude, tzOffsetHours)
            SolarMidnightEvent(
                time = elapsed.toWallClock(),
                elapsedHours = elapsed,
                altitude = pos.altitude,
                azimuth = pos.azimuth,
            )
        }

        val exactSolarMidnightElapsed: Double? = solarMidnightEvents.firstOrNull()?.elapsedHours
        val fallbackNadirPos = if (solarMidnightEvents.isEmpty()) {
            val startPos = getPositionAtHour(date, 0.0, latitude, longitude, tzOffsetHours)
            val endPos = getPositionAtHour(date, dayHours, latitude, longitude, tzOffsetHours)
            if (startPos.altitude <= endPos.altitude) startPos else endPos
        } else {
            null
        }
        val solarMidnightAltitude = solarMidnightEvents.minOfOrNull { it.altitude }
            ?: fallbackNadirPos!!.altitude
        val solarMidnightAzimuth = solarMidnightEvents.firstOrNull()?.azimuth
            ?: fallbackNadirPos!!.azimuth

        // 5. Shared exact solar-altitude band scanner over [0, dayHours]
        // Captures 0, 1, 2, or 3 intervals per calendar day (including Scandinavian midnight-split
        // twilights, continuous overnight/midday bands, polar transitions, and 23h/25h DST days).
        val scanStepHours = 10.0 / 60.0
        val sampleGrid = mutableListOf<Double>()
        var cursor = 0.0
        while (cursor < dayHours - 1e-6) {
            sampleGrid.add(cursor)
            cursor += scanStepHours
        }
        sampleGrid.add(dayHours)
        val noonInDay = ((exactSolarNoon % dayHours) + dayHours) % dayHours
        if (noonInDay > 1e-4 && noonInDay < dayHours - 1e-4) sampleGrid.add(noonInDay)
        inDayMidnights.forEach { midnightInDay ->
            if (midnightInDay > 1e-4 && midnightInDay < dayHours - 1e-4) sampleGrid.add(midnightInDay)
        }
        sampleGrid.sort()

        val sampleHours = DoubleArray(sampleGrid.size)
        val sampleAlts = DoubleArray(sampleGrid.size)
        val sampleSunHorizons = DoubleArray(sampleGrid.size)
        for (idx in sampleGrid.indices) {
            val h = sampleGrid[idx]
            val pos = getPositionAtHour(date, h, latitude, longitude, tzOffsetHours)
            sampleHours[idx] = h
            sampleAlts[idx] = pos.altitude
            sampleSunHorizons[idx] = if (useInstantaneousSemidiameter) {
                -(REFRACTION_HORIZON_DEG + calculateSemiDiameterDeg(pos.distanceAu)) - coordinates.horizonDipDeg
            } else {
                dynamicSunriseSunsetAlt
            }
        }

        fun findBandIntervals(
            lowerAltDeg: Double,
            upperAltDeg: Double,
            dynamicUpperSunHorizon: Boolean = false,
        ): List<PhaseInterval> {
            fun upperAtPos(pos: SolarPosition): Double = if (dynamicUpperSunHorizon) {
                -(REFRACTION_HORIZON_DEG + calculateSemiDiameterDeg(pos.distanceAu)) - coordinates.horizonDipDeg
            } else {
                upperAltDeg
            }

            fun bisectBoundary(h0: Double, h1: Double, signedFn: (Double) -> Double): Double {
                val f0 = signedFn(h0)
                var lo = h0
                var hi = h1
                repeat(16) {
                    val mid = (lo + hi) * 0.5
                    val fm = signedFn(mid)
                    if ((f0 <= 0.0 && fm <= 0.0) || (f0 > 0.0 && fm > 0.0)) {
                        lo = mid
                    } else {
                        hi = mid
                    }
                }
                return (lo + hi) * 0.5
            }

            val rawStarts = mutableListOf<Double>()
            val rawEnds = mutableListOf<Double>()

            for (i in 0 until sampleHours.lastIndex) {
                val h0 = sampleHours[i]
                val h1 = sampleHours[i + 1]
                if (h1 - h0 <= 1e-7) continue

                val a0 = sampleAlts[i]
                val a1 = sampleAlts[i + 1]
                val u0 = if (dynamicUpperSunHorizon) sampleSunHorizons[i] else upperAltDeg
                val u1 = if (dynamicUpperSunHorizon) sampleSunHorizons[i + 1] else upperAltDeg

                val cuts = mutableListOf(h0, h1)

                val dl0 = a0 - lowerAltDeg
                val dl1 = a1 - lowerAltDeg
                if ((dl0 < 0.0 && dl1 > 0.0) || (dl0 > 0.0 && dl1 < 0.0)) {
                    cuts.add(
                        bisectBoundary(h0, h1) { h ->
                            getPositionAtHour(date, h, latitude, longitude, tzOffsetHours).altitude - lowerAltDeg
                        },
                    )
                }

                val du0 = a0 - u0
                val du1 = a1 - u1
                if ((du0 < 0.0 && du1 > 0.0) || (du0 > 0.0 && du1 < 0.0)) {
                    cuts.add(
                        bisectBoundary(h0, h1) { h ->
                            val p = getPositionAtHour(date, h, latitude, longitude, tzOffsetHours)
                            p.altitude - upperAtPos(p)
                        },
                    )
                }

                cuts.sort()
                for (k in 0 until cuts.lastIndex) {
                    val s = cuts[k]
                    val e = cuts[k + 1]
                    if (e - s <= 1e-6) continue
                    val midPos = getPositionAtHour(date, (s + e) * 0.5, latitude, longitude, tzOffsetHours)
                    val upperMid = upperAtPos(midPos)
                    if (midPos.altitude >= lowerAltDeg && midPos.altitude <= upperMid) {
                        if (rawEnds.isNotEmpty() && abs(s - rawEnds.last()) <= 1e-4) {
                            rawEnds[rawEnds.lastIndex] = e
                        } else {
                            rawStarts.add(s)
                            rawEnds.add(e)
                        }
                    }
                }
            }

            if (rawStarts.isEmpty()) return emptyList()

            // Trapped inside the band all day (e.g. exact poles during equinox)
            if (rawStarts.size == 1 && rawStarts[0] <= 1e-4 && rawEnds[0] >= dayHours - 1e-4) {
                return listOf(
                    PhaseInterval(
                        startHour = null,
                        endHour = null,
                        durationHours = dayHours,
                        direction = PhaseDirection.CONTINUOUS,
                        isAllDay = true,
                    ),
                )
            }

            // Single continuous overnight band split only by clock midnight (e.g. Scandinavia midsummer
            // Nautical twilight: 23:25 – 24:00 + 00:00 – 02:35 -> unified 23:25 – 02:35)
            if (rawStarts.size == 2 && rawStarts[0] <= 1e-4 && rawEnds[1] >= dayHours - 1e-4) {
                val startElapsed = rawStarts[1]
                val endElapsed = rawEnds[0]
                val totalDuration = (dayHours - startElapsed) + endElapsed
                return listOf(
                    PhaseInterval(
                        startHour = startElapsed.toWallClock(),
                        endHour = endElapsed.toWallClock(),
                        durationHours = totalDuration,
                        direction = PhaseDirection.CONTINUOUS,
                        wrapsMidnight = true,
                    ),
                )
            }

            return rawStarts.indices.map { idx ->
                val s = rawStarts[idx]
                val e = rawEnds[idx]
                val eps = minOf(0.01, (e - s) * 0.25)
                val startSpeed = getPositionAtHour(date, s + eps, latitude, longitude, tzOffsetHours).solarSpeedDegPerMin
                val endSpeed = getPositionAtHour(date, e - eps, latitude, longitude, tzOffsetHours).solarSpeedDegPerMin
                val direction = when {
                    startSpeed > 0.0 && endSpeed > 0.0 -> PhaseDirection.RISING
                    startSpeed < 0.0 && endSpeed < 0.0 -> PhaseDirection.SETTING
                    else -> PhaseDirection.CONTINUOUS
                }
                val startWall = if (s <= 1e-4) 0.0 else s.toWallClock()
                val endWall = if (e >= dayHours - 1e-4) 24.0 else e.toWallClock()
                PhaseInterval(
                    startHour = startWall,
                    endHour = endWall,
                    durationHours = (e - s).coerceAtLeast(0.0),
                    direction = direction,
                )
            }
        }

        val civilIntervals = findBandIntervals(
            lowerAltDeg = ALT_CIVIL_TWILIGHT,
            upperAltDeg = dynamicSunriseSunsetAlt,
            dynamicUpperSunHorizon = useInstantaneousSemidiameter,
        )
        val nauticalIntervals = findBandIntervals(ALT_NAUTICAL_TWILIGHT, ALT_CIVIL_TWILIGHT)
        val astroIntervals = findBandIntervals(ALT_ASTRONOMICAL_TWILIGHT, ALT_NAUTICAL_TWILIGHT)
        val goldenIntervals = findBandIntervals(prefs.goldenHourRange.start, prefs.goldenHourRange.endInclusive)
        val pinkIntervals = findBandIntervals(prefs.pinkHourRange.start, prefs.pinkHourRange.endInclusive)
        val blueIntervals = findBandIntervals(prefs.blueHourRange.start, prefs.blueHourRange.endInclusive)
        val alpenglowIntervals = findBandIntervals(prefs.alpenglowRange.start, prefs.alpenglowRange.endInclusive)
        val plutoIntervals = findBandIntervals(ALT_PLUTO_TIME_LOWER, ALT_PLUTO_TIME_UPPER)

        // 6. Chronological horizon-crossing scanner over [0, dayHours]
        // Captures 0, 1, 2, or 3 sunrise/sunset crossings per local calendar day (e.g., arctic
        // Set -> Rise -> Set or Rise -> Set -> Rise days) with exact per-event azimuths.
        fun signedSunAltAt(hour: Double): Double {
            val pos = getPositionAtHour(date, hour, latitude, longitude, tzOffsetHours)
            val horizon = if (useInstantaneousSemidiameter) {
                -(REFRACTION_HORIZON_DEG + calculateSemiDiameterDeg(pos.distanceAu)) - coordinates.horizonDipDeg
            } else {
                dynamicSunriseSunsetAlt
            }
            return pos.altitude - horizon
        }

        val riseSetCrossings = mutableListOf<Pair<Double, Boolean>>()
        for (i in 0 until sampleHours.lastIndex) {
            val h0 = sampleHours[i]
            val h1 = sampleHours[i + 1]
            if (h1 - h0 <= 1e-7) continue
            val s0 = sampleAlts[i] - sampleSunHorizons[i]
            val s1 = sampleAlts[i + 1] - sampleSunHorizons[i + 1]
            if (s0 < 0.0 && s1 >= 0.0) {
                val crossing = refineSolarCrossing(h0, h1, isRising = true, signedAlt = ::signedSunAltAt)
                riseSetCrossings.add(crossing to true)
            } else if (s0 > 0.0 && s1 <= 0.0) {
                val crossing = refineSolarCrossing(h0, h1, isRising = false, signedAlt = ::signedSunAltAt)
                riseSetCrossings.add(crossing to false)
            } else if (i == 0 && s0 == 0.0 && s1 != 0.0) {
                riseSetCrossings.add(0.0 to (s1 > 0.0))
            }
        }

        val riseSetEvents = riseSetCrossings.map { (elapsedHour, isRise) ->
            val pos = getPositionAtHour(date, elapsedHour, latitude, longitude, tzOffsetHours)
            RiseSetEvent(
                timeHour = elapsedHour.toWallClock(),
                azimuth = pos.azimuth,
                isRise = isRise,
            )
        }

        val resolvedDayLength = if (riseSetCrossings.size >= 3) {
            var isAbove = (sampleAlts[0] - sampleSunHorizons[0]) >= 0.0
            var aboveStart = if (isAbove) 0.0 else Double.NaN
            var totalAbove = 0.0
            for ((t, isRise) in riseSetCrossings) {
                if (isRise && !isAbove) {
                    aboveStart = t
                    isAbove = true
                } else if (!isRise && isAbove) {
                    totalAbove += (t - aboveStart).coerceAtLeast(0.0)
                    isAbove = false
                }
            }
            if (isAbove) {
                totalAbove += (dayHours - aboveStart).coerceAtLeast(0.0)
            }
            totalAbove.coerceIn(0.0, dayHours)
        } else {
            dayLength
        }

        return DailyEvents(
            solarNoon = exactSolarNoon.toWallClock(),
            solarNoonAltitude = noonPos.altitude,
            solarNoonAzimuth = noonPos.azimuth,
            sunrise = sun.first.toWallClock(), sunriseAzimuth = sunrisePos?.azimuth,
            sunset = sun.second.toWallClock(), sunsetAzimuth = sunsetPos?.azimuth,
            dawnCivil = civil.first.toWallClock(), duskCivil = civil.second.toWallClock(),
            dawnNautical = nautical.first.toWallClock(), duskNautical = nautical.second.toWallClock(),
            dawnAstronomical = astro.first.toWallClock(), duskAstronomical = astro.second.toWallClock(),
            dayLength = resolvedDayLength,
            solarMidnight = exactSolarMidnightElapsed.toWallClock(),
            solarMidnightAltitude = solarMidnightAltitude,
            solarMidnightAzimuth = solarMidnightAzimuth,
            dawnGoldenLower = goldenLower.first.toWallClock(), dawnGoldenUpper = goldenUpper.first.toWallClock(),
            duskGoldenUpper = goldenUpper.second.toWallClock(), duskGoldenLower = goldenLower.second.toWallClock(),
            dawnPinkLower = pinkLower.first.toWallClock(), dawnPinkUpper = pinkUpper.first.toWallClock(),
            duskPinkUpper = pinkUpper.second.toWallClock(), duskPinkLower = pinkLower.second.toWallClock(),
            dawnBlueLower = blueLower.first.toWallClock(), dawnBlueUpper = blueUpper.first.toWallClock(),
            duskBlueUpper = blueUpper.second.toWallClock(), duskBlueLower = blueLower.second.toWallClock(),
            dawnAlpenglowLower = alpenglowLower.first.toWallClock(), dawnAlpenglowUpper = alpenglowUpper.first.toWallClock(),
            duskAlpenglowUpper = alpenglowUpper.second.toWallClock(), duskAlpenglowLower = alpenglowLower.second.toWallClock(),
            morningPlutoTime = pluto.first.toWallClock(), eveningPlutoTime = pluto.second.toWallClock(),
            distanceAu = noonPos.distanceAu,
            solarNoonElapsedHours = exactSolarNoon,
            solarMidnightElapsedHours = exactSolarMidnightElapsed,
            solarMidnights = solarMidnightEvents,
            dawnPlutoLower = plutoLower.first.toWallClock(), dawnPlutoUpper = plutoUpper.first.toWallClock(),
            duskPlutoUpper = plutoUpper.second.toWallClock(), duskPlutoLower = plutoLower.second.toWallClock(),
            civilIntervals = civilIntervals,
            nauticalIntervals = nauticalIntervals,
            astroIntervals = astroIntervals,
            goldenIntervals = goldenIntervals,
            pinkIntervals = pinkIntervals,
            blueIntervals = blueIntervals,
            alpenglowIntervals = alpenglowIntervals,
            plutoIntervals = plutoIntervals,
            riseSetEvents = riseSetEvents,
        )
    }

    fun calculateDailyDurations(
        events: DailyEvents,
        coordinates: Coordinates,
        prefs: LightPhasePreferences = LightPhasePreferences(),
        time: ZonedDateTime? = null,
        useInstantaneousSemidiameter: Boolean = coordinates.useInstantaneousSemidiameter,
    ): DailyDurations {
        val dynamicSunriseSunsetAlt = if (useInstantaneousSemidiameter) {
            -(REFRACTION_HORIZON_DEG + calculateSemiDiameterDeg(events.distanceAu)) - coordinates.horizonDipDeg
        } else {
            coordinates.sunApparentHorizonAlt
        }

        // Inject the dynamic horizon into the user's preferences
        val prefs = prefs.copy(apparentHorizonAlt = dynamicSunriseSunsetAlt)
        val timeline = time?.let { LocalDayTimeline(it.toLocalDate(), it.zone) }
        val dayHours = timeline?.durationHours ?: 24.0
        fun toElapsed(wallHour: Double?): Double? =
            wallHour?.let { timeline?.wallClockHourToElapsedHour(it) ?: it }
        val noonElapsed = events.solarNoonElapsedHours
        val midnightElapsed = events.solarMidnightElapsedHours
            ?: events.solarMidnight?.let { timeline?.wallClockHourToElapsedHour(it) ?: it }
            ?: (((noonElapsed + 12.0) % dayHours + dayHours) % dayHours)

        val civilDurations = calculatePhaseDuration(
            lowerBoundAlt = ALT_CIVIL_TWILIGHT,
            upperBoundAlt = dynamicSunriseSunsetAlt,
            morningLowerTime = toElapsed(events.dawnCivil),
            eveningLowerTime = toElapsed(events.duskCivil),
            morningUpperTime = toElapsed(events.sunrise),
            eveningUpperTime = toElapsed(events.sunset),
            solarNoon = noonElapsed,
            solarNoonAltitude = events.solarNoonAltitude,
            solarMidnightAltitude = events.solarMidnightAltitude,
            solarMidnight = midnightElapsed,
            dayDurationHours = dayHours,
        )

        val nauticalDurations = calculatePhaseDuration(
            lowerBoundAlt = ALT_NAUTICAL_TWILIGHT,
            upperBoundAlt = ALT_CIVIL_TWILIGHT,
            morningLowerTime = toElapsed(events.dawnNautical),
            eveningLowerTime = toElapsed(events.duskNautical),
            morningUpperTime = toElapsed(events.dawnCivil),
            eveningUpperTime = toElapsed(events.duskCivil),
            solarNoon = noonElapsed,
            solarNoonAltitude = events.solarNoonAltitude,
            solarMidnightAltitude = events.solarMidnightAltitude,
            solarMidnight = midnightElapsed,
            dayDurationHours = dayHours,
        )

        val astronomicalDurations = calculatePhaseDuration(
            lowerBoundAlt = ALT_ASTRONOMICAL_TWILIGHT,
            upperBoundAlt = ALT_NAUTICAL_TWILIGHT,
            morningLowerTime = toElapsed(events.dawnAstronomical),
            eveningLowerTime = toElapsed(events.duskAstronomical),
            morningUpperTime = toElapsed(events.dawnNautical),
            eveningUpperTime = toElapsed(events.duskNautical),
            solarNoon = noonElapsed,
            solarNoonAltitude = events.solarNoonAltitude,
            solarMidnightAltitude = events.solarMidnightAltitude,
            solarMidnight = midnightElapsed,
            dayDurationHours = dayHours,
        )

        val goldenDurations = calculatePhaseDuration(
            lowerBoundAlt = prefs.goldenHourRange.start,
            upperBoundAlt = prefs.goldenHourRange.endInclusive,
            morningLowerTime = toElapsed(events.dawnGoldenLower),
            eveningLowerTime = toElapsed(events.duskGoldenLower),
            morningUpperTime = toElapsed(events.dawnGoldenUpper),
            eveningUpperTime = toElapsed(events.duskGoldenUpper),
            solarNoon = noonElapsed,
            solarNoonAltitude = events.solarNoonAltitude,
            solarMidnightAltitude = events.solarMidnightAltitude,
            solarMidnight = midnightElapsed,
            dayDurationHours = dayHours,
        )

        val pinkDurations = calculatePhaseDuration(
            lowerBoundAlt = prefs.pinkHourRange.start,
            upperBoundAlt = prefs.pinkHourRange.endInclusive,
            morningLowerTime = toElapsed(events.dawnPinkLower),
            eveningLowerTime = toElapsed(events.duskPinkLower),
            morningUpperTime = toElapsed(events.dawnPinkUpper),
            eveningUpperTime = toElapsed(events.duskPinkUpper),
            solarNoon = noonElapsed,
            solarNoonAltitude = events.solarNoonAltitude,
            solarMidnightAltitude = events.solarMidnightAltitude,
            solarMidnight = midnightElapsed,
            dayDurationHours = dayHours,
        )

        val blueDurations = calculatePhaseDuration(
            lowerBoundAlt = prefs.blueHourRange.start,
            upperBoundAlt = prefs.blueHourRange.endInclusive,
            morningLowerTime = toElapsed(events.dawnBlueLower),
            eveningLowerTime = toElapsed(events.duskBlueLower),
            morningUpperTime = toElapsed(events.dawnBlueUpper),
            eveningUpperTime = toElapsed(events.duskBlueUpper),
            solarNoon = noonElapsed,
            solarNoonAltitude = events.solarNoonAltitude,
            solarMidnightAltitude = events.solarMidnightAltitude,
            solarMidnight = midnightElapsed,
            dayDurationHours = dayHours,
        )

        val alpenglowDurations = calculatePhaseDuration(
            lowerBoundAlt = prefs.alpenglowRange.start,
            upperBoundAlt = prefs.alpenglowRange.endInclusive,
            morningLowerTime = toElapsed(events.dawnAlpenglowLower),
            eveningLowerTime = toElapsed(events.duskAlpenglowLower),
            morningUpperTime = toElapsed(events.dawnAlpenglowUpper),
            eveningUpperTime = toElapsed(events.duskAlpenglowUpper),
            solarNoon = noonElapsed,
            solarNoonAltitude = events.solarNoonAltitude,
            solarMidnightAltitude = events.solarMidnightAltitude,
            solarMidnight = midnightElapsed,
            dayDurationHours = dayHours,
        )

        val nightDurations = calculatePhaseDuration(
            lowerBoundAlt = -90.0, // Absolute bottom
            upperBoundAlt = ALT_ASTRONOMICAL_TWILIGHT, // -18.0
            morningLowerTime = null, // Never crosses -90
            eveningLowerTime = null,
            morningUpperTime = toElapsed(events.dawnAstronomical),
            eveningUpperTime = toElapsed(events.duskAstronomical),
            solarNoon = noonElapsed,
            solarNoonAltitude = events.solarNoonAltitude,
            solarMidnightAltitude = events.solarMidnightAltitude,
            solarMidnight = midnightElapsed,
            dayDurationHours = dayHours,
        )

        val plutoDurations = calculatePhaseDuration(
            lowerBoundAlt = ALT_PLUTO_TIME_LOWER,
            upperBoundAlt = ALT_PLUTO_TIME_UPPER,
            morningLowerTime = toElapsed(events.dawnPlutoLower),
            eveningLowerTime = toElapsed(events.duskPlutoLower),
            morningUpperTime = toElapsed(events.dawnPlutoUpper),
            eveningUpperTime = toElapsed(events.duskPlutoUpper),
            solarNoon = noonElapsed,
            solarNoonAltitude = events.solarNoonAltitude,
            solarMidnightAltitude = events.solarMidnightAltitude,
            solarMidnight = midnightElapsed,
            dayDurationHours = dayHours,
        )

        return DailyDurations(
            civilTwilight = civilDurations,
            nauticalTwilight = nauticalDurations,
            astronomicalTwilight = astronomicalDurations,
            night = nightDurations,
            goldenHour = goldenDurations,
            blueHour = blueDurations,
            pinkHour = pinkDurations,
            alpenglow = alpenglowDurations,
            plutoTime = plutoDurations,
        )
    }

    // --- NOAA / Meeus Mathematical Core ---

    private data class SolarParams(
        val declinationRad: Double,
        val eotMinutes: Double,
        val distanceAu: Double
    )

    private fun calculateJulianCentury(date: LocalDate, decimalHour: Double, tzOffsetHours: Double): Double {
        var y = date.year
        var m = date.monthValue
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)

        // Julian Day at 00:00 UTC
        val jdMidnight = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + date.dayOfMonth + b - 1524.5

        // Delta T (Terrestrial Time - Universal Time) in seconds.
        // Roughly ~69 seconds for the 2020s. Crucial for sub-minute accuracy.
        val deltaT = calculateDeltaT(date.year, date.monthValue)
        // Convert Local Time to UTC, then add Delta T to achieve Terrestrial Time (TT)
        val utcDecimalHour = decimalHour - tzOffsetHours
        val ttDecimalHour = utcDecimalHour + (deltaT / 3600.0)
        val jdExact = jdMidnight + (ttDecimalHour / 24.0)

        return (jdExact - 2451545.0) / 36525.0
    }

    private fun calculateSolarParams(t: Double): SolarParams {
        // Geometric Mean Longitude
        val l0 = (280.46646 + 36000.76983 * t + 0.0003032 * t * t) % 360.0
        val l0Rad = Math.toRadians(l0)

        // Geometric Mean Anomaly
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val mRad = Math.toRadians(m)

        // Eccentricity of Earth Orbit
        val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t

        // Sun Equation of Center
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
                (0.019993 - 0.000101 * t) * sin(2 * mRad) +
                0.000289 * sin(3 * mRad)

        val sunTrueLong = l0 + c
        val sunAppLongRad = Math.toRadians(sunTrueLong - 0.00569 - 0.00478 * sin(Math.toRadians(125.04 - 1934.136 * t)))

        // --- Calculate Exact Distance (Radius Vector) ---
        // True Anomaly (v) = Mean Anomaly (m) + Equation of Center (c)
        val vRad = mRad + Math.toRadians(c)
        val distanceAu = (1.000001018 * (1.0 - e * e)) / (1.0 + e * cos(vRad))

        // Mean Obliquity of Ecliptic
        val epsilon0 = 23.0 + 26.0 / 60.0 + 21.448 / 3600.0 - (46.815 / 3600.0) * t - (0.00059 / 3600.0) * t * t + (0.001813 / 3600.0) * t * t * t
        val epsilonRad = Math.toRadians(epsilon0 + 0.00256 * cos(Math.toRadians(125.04 - 1934.136 * t)))

        // Declination
        val declinationRad = asin(sin(epsilonRad) * sin(sunAppLongRad))

        // Equation of Time (minutes)
        val y = tan(epsilonRad / 2.0) * tan(epsilonRad / 2.0)
        val eotMinutes = 4.0 * Math.toDegrees(
            y * sin(2 * l0Rad) -
                    2 * e * sin(mRad) +
                    4 * e * y * sin(mRad) * cos(2 * l0Rad) -
                    0.5 * y * y * sin(4 * l0Rad) -
                    1.25 * e * e * sin(2 * mRad)
        )

        return SolarParams(declinationRad, eotMinutes, distanceAu)
    }

    internal data class SolarEcliptic(
        val trueLongitudeDeg: Double,
        val distanceAu: Double,
    )

    /**
     * Evaluates geometric true solar ecliptic longitude (degrees) and Earth–Sun radius vector (AU)
     * for Julian Century [t] (TT since J2000.0, Meeus Chapter 25).
     */
    internal fun calculateEcliptic(t: Double): SolarEcliptic {
        val l0 = (280.46646 + 36000.76983 * t + 0.0003032 * t * t) % 360.0
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val mRad = Math.toRadians(m)
        val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
            (0.019993 - 0.000101 * t) * sin(2 * mRad) +
            0.000289 * sin(3 * mRad)
        val sunTrueLong = l0 + c
        val vRad = mRad + Math.toRadians(c)
        val distanceAu = (1.000001018 * (1.0 - e * e)) / (1.0 + e * cos(vRad))
        return SolarEcliptic(sunTrueLong, distanceAu)
    }

    /**
     * Lightweight Earth–Sun radius vector evaluation in Astronomical Units (AU) for a given instant.
     */
    fun calculateDistanceAu(time: ZonedDateTime): Double {
        val date = time.toLocalDate()
        val decimalHour = time.hour + time.minute / 60.0 + time.second / 3600.0
        val tzOffsetHours = time.offset.totalSeconds / 3600.0
        val t = calculateJulianCentury(date, decimalHour, tzOffsetHours)
        return calculateSolarParams(t).distanceAu
    }

    // --- Extracted continuous math helpers for clean code ---

    private fun calculateGamma(dayOfYear: Int, hour: Double): Double {
        return (2.0 * PI / 365.0) * (dayOfYear - 1.0 + (hour - 12.0) / 24.0)
    }

    private fun calculateEquationOfTime(gamma: Double): Double {
        return 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma)
                - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
    }

    private fun calculateDeclination(gamma: Double): Double {
        return 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
    }

    /**
     * Lightweight solar-noon peak altitude for fast multi-day/year gradient rendering.
     * Uses a single [calculateSolarParams] evaluation instead of full daily event root-finding.
     */
    fun calculateSolarNoonAltitude(
        time: ZonedDateTime,
        coordinates: Coordinates,
    ): Double {
        val date = time.toLocalDate()
        val tzOffsetHours = time.offset.totalSeconds / 3600.0
        val approxNoon = 12.0 - (coordinates.longitude / 15.0) + tzOffsetHours
        val t = calculateJulianCentury(date, approxNoon, tzOffsetHours)
        val params = calculateSolarParams(t)
        val latRad = Math.toRadians(coordinates.latitude)
        val sinAlt = sin(latRad) * sin(params.declinationRad) + cos(latRad) * cos(params.declinationRad)
        return Math.toDegrees(asin(sinAlt.coerceIn(-1.0, 1.0))).round(2)
    }

    /**
     * Zero-allocation solar declination (radians) at a given UTC epoch millisecond.
     * Used by the Year TimeMachine 2D daylight ribbon to render the full 24h diurnal profile per day.
     */
    fun calculateSolarDeclinationRad(epochMillis: Long): Double {
        val jdUtc = epochMillis / 86_400_000.0 + 2440587.5
        val t = (jdUtc - 2451545.0) / 36525.0
        return calculateSolarParams(t).declinationRad
    }

    /**
     * Calculates the current position of the Sun.
     */
    fun calculatePosition(
        time: ZonedDateTime,
        coordinates: Coordinates
    ): SolarPosition {
        val decimalHour = time.hour + time.minute / 60.0 + time.second / 3600.0
        val tzOffsetHours = time.offset.totalSeconds / 3600.0

        return getPositionAtHour(time.toLocalDate(), decimalHour, coordinates.latitude, coordinates.longitude, tzOffsetHours)
    }

    /**
     * Calculates both exact altitude and azimuth (compass direction) of the sun at a specific decimal hour.
     * Useful for finding the azimuth of sunrise, sunset, or any calculated event.
     */
    fun getPositionAtHour(
        date: LocalDate,
        decimalHour: Double,
        latitude: Double,
        longitude: Double,
        tzOffsetHours: Double
    ): SolarPosition {
        val latRad = Math.toRadians(latitude)

        // 1. Calculate continuous orbital variables for this exact moment
        val t = calculateJulianCentury(date, decimalHour, tzOffsetHours)
        val params = calculateSolarParams(t)

        // 2. Calculate True Solar Time (TST) and Hour Angle
        val tst = (decimalHour * 60.0) + params.eotMinutes + (4.0 * longitude) - (60.0 * tzOffsetHours)
        var hourAngleDeg = (tst / 4.0) - 180.0
        // Hour Angle Normalization
        // Prevents local midnight from breaking the Morning/Afternoon detector
        while (hourAngleDeg < -180.0) hourAngleDeg += 360.0
        while (hourAngleDeg > 180.0) hourAngleDeg -= 360.0
        val hourAngleRad = Math.toRadians(hourAngleDeg)

        // 3. Calculate Altitude
        val sinAlt = sin(latRad) * sin(params.declinationRad) + cos(latRad) * cos(params.declinationRad) * cos(hourAngleRad)
        val altitudeRad = asin(sinAlt.coerceIn(-1.0, 1.0))

        // --- Analytical Solar Speed (Calculus Derivative) ---
        // d(Alt)/dt = 0.25 * (-cos(Lat) * cos(Dec) * sin(HourAngle)) / cos(Alt)
        // safeCosAlt prevents DivisionByZero crashes when the sun is exactly at Zenith (90°)
        val safeCosAlt = max(0.00001, cos(altitudeRad))
        val speedDegPerMin = 0.25 * (cos(latRad) * cos(params.declinationRad) * -sin(hourAngleRad)) / safeCosAlt

        // 4. Calculate raw Azimuth
        val cosAz = (sin(params.declinationRad) - sin(altitudeRad) * sin(latRad)) / (cos(altitudeRad) * cos(latRad))

        // Coerce is necessary to prevent NaN errors from tiny floating point rounding overflows
        val rawAzimuthDeg = Math.toDegrees(acos(cosAz.coerceIn(-1.0, 1.0)))

        // 5. Adjust for morning vs. afternoon
        val azimuthDeg = if (hourAngleDeg > 0) 360.0 - rawAzimuthDeg else rawAzimuthDeg

        // Keep full unrounded precision for downstream physics (shadows, air mass, charts);
        // display formatting rounds at the UI boundary.
        return SolarPosition(
            altitude = Math.toDegrees(altitudeRad),
            azimuth = azimuthDeg,
            distanceAu = params.distanceAu,
            declination = Math.toDegrees(params.declinationRad),
            eotMinutes = params.eotMinutes,
            solarSpeedDegPerMin = speedDegPerMin
        )
    }

    /**
     * Calculates and formats the exact duration of daylight.
     */
    fun getExactDayLength(
        sunriseHours: Double?,
        sunsetHours: Double?,
        solarNoonAltitude: Double,
        sunriseSunsetAltThreshold: Double,
        dayDurationHours: Double = 24.0,
    ): Double {
        // 1. Handle edge cases (Polar day/night & single-crossing transition days)
        if (sunriseHours == null && sunsetHours == null) {
            return if (solarNoonAltitude > sunriseSunsetAltThreshold) {
                dayDurationHours // Polar Day (Midnight Sun)
            } else {
                0.0 // Polar Night
            }
        }
        if (sunriseHours != null && sunsetHours == null) {
            // Rose during the day and did not set before midnight (entering Polar Day)
            return (dayDurationHours - (((sunriseHours % dayDurationHours) + dayDurationHours) % dayDurationHours))
                .coerceIn(0.0, dayDurationHours)
        }
        if (sunriseHours == null && sunsetHours != null) {
            // Up since midnight and set without rising again (entering Polar Night)
            return (((sunsetHours % dayDurationHours) + dayDurationHours) % dayDurationHours)
                .coerceIn(0.0, dayDurationHours)
        }

        // 2. Calculate raw duration
        var durationHours = sunsetHours!! - sunriseHours!!

        // 3. Handle midnight wrap-around (if sunset spills into the next day mathematically)
        if (durationHours < 0) {
            durationHours += dayDurationHours
        }

        return durationHours
    }

    /**
     * Universally calculates the duration of any solar phase (Twilights, Golden Hour, Night).
     * Flawlessly handles Polar Day, Polar Night, DST transition days, and mid-phase intersections.
     */
    fun calculatePhaseDuration(
        lowerBoundAlt: Double,
        upperBoundAlt: Double,
        morningLowerTime: Double?,
        eveningLowerTime: Double?,
        morningUpperTime: Double?,
        eveningUpperTime: Double?,
        solarNoon: Double,
        solarNoonAltitude: Double,
        solarMidnightAltitude: Double,
        solarMidnight: Double = (solarNoon + 12.0) % 24.0,
        dayDurationHours: Double = 24.0,
    ): PhaseDurations {
        val mStart = morningLowerTime
        val mEnd = morningUpperTime
        val eStart = eveningUpperTime
        val eEnd = eveningLowerTime

        // Case 1: Standard Day (Crosses both upper and lower boundaries)
        if (mStart != null && mEnd != null && eStart != null && eEnd != null) {
            return PhaseDurations(
                morning = (mEnd - mStart + dayDurationHours) % dayDurationHours,
                evening = (eEnd - eStart + dayDurationHours) % dayDurationHours
            )
        }

        // Case 2: Peaks inside the phase (Crosses lower, but never reaches upper)
        // Splits the continuous phase at Solar Noon
        if (mStart != null && eEnd != null && mEnd == null && eStart == null) {
            return PhaseDurations(
                morning = (solarNoon - mStart + dayDurationHours) % dayDurationHours,
                evening = (eEnd - solarNoon + dayDurationHours) % dayDurationHours
            )
        }

        // Case 3: Bottoms out inside the phase (Crosses upper, never drops below lower)
        // Splits the continuous phase at Solar Midnight
        if (mEnd != null && eStart != null && mStart == null && eEnd == null) {
            return PhaseDurations(
                morning = (mEnd - solarMidnight + dayDurationHours) % dayDurationHours,
                evening = (solarMidnight - eStart + dayDurationHours) % dayDurationHours
            )
        }

        // Case 4: Never crosses either boundary.
        // It is either completely above, completely below, or permanently trapped inside.
        if (solarNoonAltitude < lowerBoundAlt || solarMidnightAltitude > upperBoundAlt) {
            return PhaseDurations(0.0, 0.0) // Missed the phase entirely
        }

        // The sun's cycle is permanently trapped inside this phase (e.g., Twilight all day)
        return PhaseDurations(dayDurationHours / 2.0, dayDurationHours / 2.0)
    }

    /**
     * Generates the precise 365-day universal Analemma (Equation of Time vs Declination).
     * This curve is identical for the entire Earth.
     */
    fun getUniversalAnalemma(year: Int): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        // Step 2 calculates every other day to save CPU while keeping the curve perfectly smooth
        for (day in 1..365 step 2) {
            val date = LocalDate.ofYearDay(year, day)
            // Calculate for UTC Noon to get the pure orbital parameters
            val t = calculateJulianCentury(date, 12.0, 0.0)
            val params = calculateSolarParams(t)

            // Return Pair(EquationOfTime, Declination)
            points.add(Pair(params.eotMinutes, Math.toDegrees(params.declinationRad)))
        }
        return points
    }
}