package com.ephemeris.helios.utils.calc

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.*

object PlanetaryEphemeris {

    // Planets are point-sources (unlike the Sun/Moon which have visible disks).
    // Therefore, they rise/set exactly when their center crosses the geometric horizon,
    // adjusted ONLY for standard atmospheric refraction (-34 arcminutes).
    const val ALT_RISE_SET = -0.5667

    enum class Planet(
        val lTerms: Array<DoubleArray>, // Heliocentric Longitude (A, B, C)
        val bTerms: Array<DoubleArray>, // Heliocentric Latitude (A, B, C)
        val rTerms: Array<DoubleArray>  // Heliocentric Radius/Distance (A, B, C)
    ) {
        // VSOP87 Truncated Highest-Amplitude Terms: [A (Amplitude), B (Phase), C (Frequency)]
        MERCURY(
            lTerms = arrayOf(
                doubleArrayOf(3.1761467, 3.212736, 2608.790314), doubleArrayOf(0.1092567, 2.503111, 5217.580628),
                doubleArrayOf(0.0048600, 1.792510, 7826.370940), doubleArrayOf(0.0002497, 1.080510, 10435.16130)
            ),
            bTerms = arrayOf(
                doubleArrayOf(0.1220902, 1.543594, 2608.790314), doubleArrayOf(0.0050720, 0.835470, 5217.580628),
                doubleArrayOf(0.0002164, 0.134000, 7826.370900)
            ),
            rTerms = arrayOf(
                doubleArrayOf(0.0792376, 4.793268, 2608.790314), doubleArrayOf(0.0053915, 4.075486, 5217.580628),
                doubleArrayOf(0.0003551, 3.361950, 7826.370900), doubleArrayOf(0.0000244, 2.650800, 10435.16130)
            )
        ),
        VENUS(
            lTerms = arrayOf(
                doubleArrayOf(0.1189211, 4.293671, 1021.3285546), doubleArrayOf(0.0005230, 2.596001, 2042.657109),
                doubleArrayOf(0.0000032, 0.899000, 3063.985700)
            ),
            bTerms = arrayOf(
                doubleArrayOf(0.0594324, 0.380721, 1021.3285546), doubleArrayOf(0.0002661, 4.970220, 2042.657109)
            ),
            rTerms = arrayOf(
                doubleArrayOf(0.0049071, 5.864603, 1021.3285546), doubleArrayOf(0.0000424, 4.167888, 2042.657109)
            )
        ),
        MARS(
            lTerms = arrayOf(
                doubleArrayOf(0.6728286, 0.228966, 334.0612427), doubleArrayOf(0.0526744, 4.316041, 668.1224854),
                doubleArrayOf(0.0058204, 2.115828, 1002.183728), doubleArrayOf(0.0007213, 6.202356, 1336.244971),
                doubleArrayOf(0.0000958, 4.001640, 1670.306200), doubleArrayOf(0.0000133, 1.801000, 2004.367000)
            ),
            bTerms = arrayOf(
                doubleArrayOf(0.0324869, 1.340043, 334.0612427), doubleArrayOf(0.0026218, 5.433434, 668.1224854),
                doubleArrayOf(0.0002901, 3.235160, 1002.183728), doubleArrayOf(0.0000356, 1.036200, 1336.245000)
            ),
            rTerms = arrayOf(
                doubleArrayOf(0.1418863, 1.801121, 334.0612427), doubleArrayOf(0.0210746, 5.890632, 668.1224854),
                doubleArrayOf(0.0034606, 3.692482, 1002.183728), doubleArrayOf(0.0006001, 1.494793, 1336.244971),
                doubleArrayOf(0.0001083, 5.580250, 1670.306200), doubleArrayOf(0.0000201, 3.375000, 2004.367000)
            )
        ),
        JUPITER(
            lTerms = arrayOf(
                doubleArrayOf(0.2740447, 5.516592, 52.9690963), doubleArrayOf(0.0381483, 4.619071, 105.9381926),
                doubleArrayOf(0.0073539, 3.737158, 158.9072889), doubleArrayOf(0.0016075, 2.868770, 211.8763852),
                doubleArrayOf(0.0003756, 2.016330, 264.8454800), doubleArrayOf(0.0000918, 1.180400, 317.8145000)
            ),
            bTerms = arrayOf(
                doubleArrayOf(0.0228308, 0.354148, 52.9690963), doubleArrayOf(0.0031804, 5.748366, 105.9381926),
                doubleArrayOf(0.0006132, 4.864704, 158.9072889), doubleArrayOf(0.0001338, 3.992200, 211.8763852)
            ),
            rTerms = arrayOf(
                doubleArrayOf(0.2520697, 0.803738, 52.9690963), doubleArrayOf(0.0520448, 6.182419, 105.9381926),
                doubleArrayOf(0.0132338, 5.297775, 158.9072889), doubleArrayOf(0.0034237, 4.425832, 211.8763852),
                doubleArrayOf(0.0009121, 3.565810, 264.8454800), doubleArrayOf(0.0002492, 2.716100, 317.8145000)
            )
        ),
        SATURN(
            lTerms = arrayOf(
                doubleArrayOf(0.5369796, 0.400511, 21.3299095), doubleArrayOf(0.1228229, 5.742012, 42.6598191),
                doubleArrayOf(0.0354145, 5.087739, 63.9897286), doubleArrayOf(0.0114944, 4.436735, 85.3196381),
                doubleArrayOf(0.0039755, 3.788507, 106.6495476), doubleArrayOf(0.0014389, 3.142400, 127.9794572)
            ),
            bTerms = arrayOf(
                doubleArrayOf(0.0435948, 2.308579, 21.3299095), doubleArrayOf(0.0099616, 1.353386, 42.6598191),
                doubleArrayOf(0.0028822, 0.697413, 63.9897286), doubleArrayOf(0.0009386, 0.045230, 85.3196381)
            ),
            rTerms = arrayOf(
                doubleArrayOf(0.5186055, 1.968039, 21.3299095), doubleArrayOf(0.1706240, 1.020627, 42.6598191),
                doubleArrayOf(0.0601262, 0.366228, 63.9897286), doubleArrayOf(0.0223783, 5.968940, 85.3196381),
                doubleArrayOf(0.0086811, 5.321852, 106.6495476), doubleArrayOf(0.0034732, 4.676600, 127.9794572)
            )
        )
    }

    // Earth's Baseline Heliocentric Matrix (Required to find Geocentric coordinates of all other planets)
    private val EARTH_L = arrayOf(
        doubleArrayOf(0.0334166, 4.669257, 6283.075850), doubleArrayOf(0.0003489, 4.626100, 12566.15170)
    )
    private val EARTH_R = arrayOf(
        doubleArrayOf(0.0167060, 3.098464, 6283.075850), doubleArrayOf(0.0001396, 3.055250, 12566.15170)
    )

    data class PlanetaryPosition(
        val altitude: Double,
        val azimuth: Double,
        val rightAscension: Double,
        val declination: Double,
        val distanceKm: Double,
        val distanceAU: Double
    )

    data class PlanetaryDailyEvents(
        val culmination: Double?,
        val culminationAltitude: Double?,
        val rise: Double?,
        val riseAzimuth: Double?,
        val set: Double?,
        val setAzimuth: Double?,
        val timeUpHours: Double,
        val isUpAllDay: Boolean,
        val isDownAllDay: Boolean
    )

    data class ApproachEvent(
        val time: ZonedDateTime,
        val distanceKm: Double,
        val isClosestApproach: Boolean // True = Closest (Opposition), False = Farthest (Conjunction)
    )

    /**
     * Calculates the exact altitude, azimuth, and distance of a planet for a specific 12-second UI frame.
     */
    fun calculatePosition(
        planet: Planet,
        date: LocalDate,
        decimalHour: Double,
        latitude: Double,
        longitude: Double,
        tzOffsetHours: Double
    ): PlanetaryPosition {
        val tMillis = calculateJulianMillennium(date.year, date.monthValue, date.dayOfMonth, decimalHour, tzOffsetHours)

        // 1. Calculate Planet Heliocentric (L, B, R)
        val lPlanet = sumVSOP(planet.lTerms, tMillis)
        val bPlanet = sumVSOP(planet.bTerms, tMillis)
        val rPlanet = sumVSOP(planet.rTerms, tMillis)

        // 2. Calculate Earth Heliocentric (L, B, R)
        val lEarth = sumVSOP(EARTH_L, tMillis)
        // Earth's latitude is virtually 0 relative to the ecliptic plane
        val bEarth = 0.0
        val rEarth = 1.00000011 + sumVSOP(EARTH_R, tMillis)

        // 3. Convert Heliocentric to Geocentric Rectangular Coordinates (X, Y, Z)
        val x = rPlanet * cos(bPlanet) * cos(lPlanet) - rEarth * cos(bEarth) * cos(lEarth)
        val y = rPlanet * cos(bPlanet) * sin(lPlanet) - rEarth * cos(bEarth) * sin(lEarth)
        val z = rPlanet * sin(bPlanet) - rEarth * sin(bEarth)

        // 4. Geocentric Distance (in Astronomical Units, AU)
        val distanceAU = sqrt(x * x + y * y + z * z)
        val distanceKm = distanceAU * 149597870.7

        // 5. Geocentric Ecliptic Longitude and Latitude
        val geoLambda = atan2(y, x)
        val geoBeta = atan2(z, sqrt(x * x + y * y))

        // 6. Convert Ecliptic to Equatorial (Right Ascension, Declination)
        val tCenturies = tMillis * 10.0
        val epsilon0 = 23.439291 - 0.0130042 * tCenturies
        val epsRad = Math.toRadians(epsilon0)

        val raRad = atan2(sin(geoLambda) * cos(epsRad) - tan(geoBeta) * sin(epsRad), cos(geoLambda))
        val decRad = asin(sin(geoBeta) * cos(epsRad) + cos(geoBeta) * sin(epsRad) * sin(geoLambda))

        // 7. Calculate Local Sidereal Time
        val jd = calculateJulianDay(date.year, date.monthValue, date.dayOfMonth, decimalHour, tzOffsetHours)
        val jdMidnight = floor(jd - 0.5) + 0.5
        val timeInDays = jd - jdMidnight
        val gmst0 = 24110.54841 + 8640184.812866 * tCenturies + 0.093104 * tCenturies * tCenturies
        val gmstHour = (gmst0 / 3600.0 + timeInDays * 24.0 * 1.00273790935) % 24.0
        val lstRad = Math.toRadians((gmstHour * 15.0) + longitude)

        // 8. Hour Angle & Topocentric Conversion
        val hourAngleRad = lstRad - raRad
        val latRad = Math.toRadians(latitude)

        val geoSinAlt = sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(hourAngleRad)
        val geoAltRad = asin(geoSinAlt.coerceIn(-1.0, 1.0))
        val geoAzRad = atan2(sin(hourAngleRad), cos(hourAngleRad) * sin(latRad) - tan(decRad) * cos(latRad))

        var geoAzDeg = Math.toDegrees(geoAzRad) + 180.0
        if (geoAzDeg > 360.0) geoAzDeg -= 360.0

        return PlanetaryPosition(
            altitude = Math.toDegrees(geoAltRad),
            azimuth = geoAzDeg,
            rightAscension = Math.toDegrees(raRad),
            declination = Math.toDegrees(decRad),
            distanceKm = distanceKm,
            distanceAU = distanceAU
        )
    }

    /**
     * Calculates Daily Events for the "Slow State" background loop.
     */
    fun calculateDailyEvents(
        planet: Planet,
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        tzOffsetHours: Double
    ): PlanetaryDailyEvents {
        var riseHour: Double? = null
        var setHour: Double? = null
        var culminationHour: Double? = null
        var culminationAlt: Double? = null

        var prevAlt = calculatePosition(planet, date, 0.0, latitude, longitude, tzOffsetHours).altitude
        var prevHour = 0.0
        var maxAlt = prevAlt
        var peakHour = 0.0

        // Step through the day in 30-minute blocks
        for (minute in 30..1440 step 30) {
            val currentHour = minute / 60.0
            val pos = calculatePosition(planet, date, currentHour, latitude, longitude, tzOffsetHours)

            if (pos.altitude > maxAlt) {
                maxAlt = pos.altitude
                peakHour = currentHour
            }

            if (prevAlt < ALT_RISE_SET && pos.altitude >= ALT_RISE_SET) {
                riseHour = refineEvent(planet, date, prevHour, currentHour, latitude, longitude, tzOffsetHours, true)
            } else if (prevAlt > ALT_RISE_SET && pos.altitude <= ALT_RISE_SET) {
                setHour = refineEvent(planet, date, prevHour, currentHour, latitude, longitude, tzOffsetHours, false)
            }

            prevAlt = pos.altitude
            prevHour = currentHour
        }

        culminationHour = refineCulmination(planet, date, max(0.0, peakHour - 1.0), min(24.0, peakHour + 1.0), latitude, longitude, tzOffsetHours)
        if (culminationHour != null) {
            culminationAlt = calculatePosition(planet, date, culminationHour, latitude, longitude, tzOffsetHours).altitude
        }

        val riseAz = riseHour?.let { calculatePosition(planet, date, it, latitude, longitude, tzOffsetHours).azimuth }
        val setAz = setHour?.let { calculatePosition(planet, date, it, latitude, longitude, tzOffsetHours).azimuth }

        val isUpAllDay = riseHour == null && setHour == null && prevAlt > 0.0
        val isDownAllDay = riseHour == null && setHour == null && prevAlt < 0.0

        var timeUp = 0.0
        if (isUpAllDay) timeUp = 24.0
        else if (riseHour != null && setHour != null) {
            timeUp = setHour - riseHour
            if (timeUp < 0) timeUp += 24.0
        } else if (riseHour != null) {
            timeUp = 24.0 - riseHour // Rises but doesn't set before midnight
        } else if (setHour != null) {
            timeUp = setHour // Set after rising yesterday
        }

        return PlanetaryDailyEvents(
            culmination = culminationHour,
            culminationAltitude = culminationAlt,
            rise = riseHour,
            riseAzimuth = riseAz,
            set = setHour,
            setAzimuth = setAz,
            timeUpHours = timeUp,
            isUpAllDay = isUpAllDay,
            isDownAllDay = isDownAllDay
        )
    }

    /**
     * Finds the next Closest Approach (Opposition) or Farthest Approach (Conjunction).
     * This is an expensive operation that scans years into the future. Run on a background thread!
     */
    fun calculateNextApproach(
        planet: Planet,
        startDate: ZonedDateTime,
        findClosest: Boolean
    ): ApproachEvent {
        // 1. Identify the scanning window based on the planet's synodic period
        val jumpDays = if (planet == Planet.MERCURY || planet == Planet.VENUS) 5.0 else 15.0
        var currentDate = startDate

        // Ensure standard UI times don't offset the distance calculus
        val baseHour = 12.0
        val tz = 0.0

        var prevDistance = calculatePosition(planet, currentDate.toLocalDate(), baseHour, 0.0, 0.0, tz).distanceKm
        var isApproaching = true

        // 2. Iterate forward in large jumps to find the "valley" or "peak"
        for (i in 1..200) {
            val nextDate = currentDate.plusDays(jumpDays.toLong())
            val nextDistance = calculatePosition(planet, nextDate.toLocalDate(), baseHour, 0.0, 0.0, tz).distanceKm

            if (findClosest) {
                if (nextDistance > prevDistance) {
                    // We just passed the closest point!
                    break
                }
            } else {
                if (nextDistance < prevDistance) {
                    // We just passed the farthest point!
                    break
                }
            }
            prevDistance = nextDistance
            currentDate = nextDate
        }

        // 3. Golden Section Search to refine to the EXACT hour
        val phi = (1.0 + sqrt(5.0)) / 2.0
        var lowDay = -jumpDays
        var highDay = jumpDays
        var c = highDay - (highDay - lowDay) / phi
        var d = lowDay + (highDay - lowDay) / phi

        for (i in 0..20) {
            val dateC = currentDate.plusSeconds((c * 86400).toLong())
            val dateD = currentDate.plusSeconds((d * 86400).toLong())

            val distC = calculatePosition(planet, dateC.toLocalDate(), dateC.hour.toDouble(), 0.0, 0.0, tz).distanceKm
            val distD = calculatePosition(planet, dateD.toLocalDate(), dateD.hour.toDouble(), 0.0, 0.0, tz).distanceKm

            val condition = if (findClosest) distC > distD else distC < distD

            if (condition) {
                highDay = d
                d = c
                c = highDay - (highDay - lowDay) / phi
            } else {
                lowDay = c
                c = d
                d = lowDay + (highDay - lowDay) / phi
            }
        }

        val exactDate = currentDate.plusSeconds((((lowDay + highDay) / 2.0) * 86400).toLong())
        val finalDist = calculatePosition(planet, exactDate.toLocalDate(), exactDate.hour.toDouble(), 0.0, 0.0, tz).distanceKm

        return ApproachEvent(exactDate, finalDist, findClosest)
    }

    // --- Private Math Engines ---

    private fun sumVSOP(terms: Array<DoubleArray>, tau: Double): Double {
        var sum = 0.0
        for (term in terms) {
            sum += term[0] * cos(term[1] + term[2] * tau)
        }
        return sum
    }

    private fun calculateJulianMillennium(year: Int, month: Int, day: Int, decimalHour: Double, tzOffsetHours: Double): Double {
        val jdExact = calculateJulianDay(year, month, day, decimalHour, tzOffsetHours)
        val deltaTSeconds = calculateDeltaT(year, month)
        val ttJdExact = jdExact + (deltaTSeconds / 86400.0)
        // VSOP87 strictly requires Julian Millennia from J2000.0 (Tau)
        return (ttJdExact - 2451545.0) / 365250.0
    }

    private fun calculateJulianDay(year: Int, month: Int, day: Int, decimalHour: Double, tzOffsetHours: Double): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1; m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)
        val jdMidnight = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
        val utcDecimalHour = decimalHour - tzOffsetHours
        return jdMidnight + (utcDecimalHour / 24.0)
    }

    private fun refineEvent(planet: Planet, date: LocalDate, startH: Double, endH: Double, lat: Double, lon: Double, tz: Double, isRising: Boolean): Double {
        var low = startH
        var high = endH
        for (i in 0..10) {
            val mid = (low + high) / 2.0
            val alt = calculatePosition(planet, date, mid, lat, lon, tz).altitude
            if (alt > ALT_RISE_SET) {
                if (isRising) high = mid else low = mid
            } else {
                if (isRising) low = mid else high = mid
            }
        }
        return (low + high) / 2.0
    }

    private fun refineCulmination(planet: Planet, date: LocalDate, startH: Double, endH: Double, lat: Double, lon: Double, tz: Double): Double {
        var low = startH
        var high = endH
        val phi = (1.0 + sqrt(5.0)) / 2.0
        var c = high - (high - low) / phi
        var d = low + (high - low) / phi

        for (i in 0..15) {
            val altC = calculatePosition(planet, date, c, lat, lon, tz).altitude
            val altD = calculatePosition(planet, date, d, lat, lon, tz).altitude
            if (altC > altD) {
                high = d; d = c; c = high - (high - low) / phi
            } else {
                low = c; c = d; d = low + (high - low) / phi
            }
        }
        return (low + high) / 2.0
    }
}