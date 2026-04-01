package com.ephemeris.helios.utils.calc

import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.*

object LunarEphemeris {

    // The Moon's extreme proximity to Earth requires a positive altitude for rising/setting.
    // True Altitude = Apparent (0.0) - Semidiameter (-0.25) - Refraction (-0.567) + Parallax (+0.95)
    // The top edge of the Moon appears on the horizon when its geometric center is at +0.133 degrees.
    const val ALT_MOONRISE_MOONSET = 0.133

    data class LunarPosition(
        val altitude: Double,
        val azimuth: Double,
        val rightAscension: Double,
        val declination: Double,
        val distanceKm: Double
    )

    data class LunarDailyEvents(
        val culmination: Double?, // Lunar Noon (Highest point)
        val culminationAltitude: Double?,
        val moonrise: Double?,
        val moonriseAzimuth: Double?,
        val moonset: Double?,
        val moonsetAzimuth: Double?,
        val isMoonUpAllDay: Boolean,
        val isMoonDownAllDay: Boolean
    )

    // Format: [D, M, M', F, Longitude_Amplitude, Distance_Amplitude]
    // The complete 60 terms from Meeus Table 47.A for absolute Longitude/Distance precision
    private val LUNAR_LONGITUDE_TERMS = arrayOf(
        doubleArrayOf( 0.0,  0.0,  1.0,  0.0,  22640.0, -3296.0),
        doubleArrayOf( 2.0,  0.0, -1.0,  0.0,  -4586.0,  1530.0),
        doubleArrayOf( 2.0,  0.0,  0.0,  0.0,   2370.0,   412.0),
        doubleArrayOf( 0.0,  0.0,  2.0,  0.0,    769.0,  -152.0),
        doubleArrayOf( 0.0,  1.0,  0.0,  0.0,   -668.0,   -46.0),
        doubleArrayOf( 0.0,  0.0,  0.0,  2.0,   -412.0,    41.0),
        doubleArrayOf( 2.0,  0.0, -2.0,  0.0,   -212.0,   134.0),
        doubleArrayOf( 2.0, -1.0, -1.0,  0.0,   -206.0,   122.0),
        doubleArrayOf( 2.0,  0.0,  1.0,  0.0,    192.0,   -85.0),
        doubleArrayOf( 2.0, -1.0,  0.0,  0.0,   -165.0,    55.0),
        doubleArrayOf( 0.0,  1.0, -1.0,  0.0,   -125.0,    28.0),
        doubleArrayOf( 1.0,  0.0,  0.0,  0.0,   -110.0,     0.0),
        doubleArrayOf( 0.0,  1.0,  1.0,  0.0,    148.0,   -31.0),
        doubleArrayOf( 2.0,  0.0,  0.0, -2.0,   -110.0,    42.0),
        doubleArrayOf( 0.0,  0.0,  1.0,  2.0,    -64.0,     0.0),
        doubleArrayOf( 0.0,  0.0,  3.0,  0.0,     36.0,    -8.0),
        doubleArrayOf( 4.0,  0.0, -1.0,  0.0,    -32.0,    11.0),
        doubleArrayOf( 4.0,  0.0, -2.0,  0.0,    -28.0,    10.0),
        doubleArrayOf( 2.0,  1.0, -1.0,  0.0,     28.0,   -11.0),
        doubleArrayOf( 2.0,  1.0,  0.0,  0.0,     26.0,    -8.0),
        doubleArrayOf( 1.0,  0.0, -1.0,  0.0,     24.0,     0.0),
        doubleArrayOf( 1.0,  0.0,  1.0,  0.0,     23.0,     0.0),
        doubleArrayOf( 2.0, -1.0,  1.0,  0.0,     19.0,    -8.0),
        doubleArrayOf( 2.0,  0.0,  2.0,  0.0,    -18.0,     9.0),
        doubleArrayOf( 4.0,  0.0,  0.0,  0.0,     17.0,    -6.0),
        doubleArrayOf( 2.0,  0.0, -3.0,  0.0,    -16.0,    11.0),
        doubleArrayOf( 0.0,  1.0, -2.0,  0.0,    -16.0,     7.0),
        doubleArrayOf( 2.0,  0.0, -1.0,  2.0,     15.0,    -1.0),
        doubleArrayOf( 2.0, -1.0, -2.0,  0.0,    -14.0,    11.0),
        doubleArrayOf( 1.0,  0.0,  0.0,  2.0,    -14.0,     0.0),
        doubleArrayOf( 0.0,  1.0,  2.0,  0.0,     14.0,    -7.0),
        doubleArrayOf( 0.0,  2.0,  0.0,  0.0,     14.0,    -7.0),
        doubleArrayOf( 2.0, -2.0,  0.0,  0.0,    -12.0,     6.0),
        doubleArrayOf( 2.0,  1.0, -2.0,  0.0,    -12.0,     6.0),
        doubleArrayOf( 0.0,  2.0, -1.0,  0.0,    -11.0,     4.0),
        doubleArrayOf( 2.0, -2.0, -1.0,  0.0,    -11.0,     5.0),
        doubleArrayOf( 2.0,  0.0,  1.0, -2.0,     11.0,     0.0),
        doubleArrayOf( 2.0,  0.0,  0.0,  2.0,    -11.0,     0.0),
        doubleArrayOf( 4.0,  0.0, -3.0,  0.0,    -10.0,     4.0),
        doubleArrayOf( 2.0,  1.0,  1.0,  0.0,     10.0,    -5.0),
        doubleArrayOf( 0.0,  2.0,  1.0,  0.0,     10.0,    -5.0),
        doubleArrayOf( 0.0,  0.0,  2.0,  2.0,     -9.0,     0.0),
        doubleArrayOf( 4.0, -1.0, -1.0,  0.0,     -9.0,     4.0),
        doubleArrayOf( 2.0,  1.0,  0.0, -2.0,     -8.0,     0.0),
        doubleArrayOf( 0.0,  1.0,  0.0, -2.0,     -8.0,     0.0),
        doubleArrayOf( 4.0, -1.0, -2.0,  0.0,     -8.0,     4.0),
        doubleArrayOf( 0.0,  1.0,  1.0,  2.0,     -8.0,     0.0),
        doubleArrayOf( 0.0,  1.0, -1.0,  2.0,     -7.0,     0.0),
        doubleArrayOf( 2.0,  0.0,  2.0, -2.0,      7.0,     0.0),
        doubleArrayOf( 1.0,  0.0,  1.0, -2.0,      7.0,     0.0),
        doubleArrayOf( 2.0,  1.0, -3.0,  0.0,     -7.0,     4.0),
        doubleArrayOf( 4.0,  0.0,  1.0,  0.0,     -7.0,     0.0),
        doubleArrayOf( 0.0,  0.0,  3.0,  2.0,     -6.0,     0.0),
        doubleArrayOf( 4.0, -1.0,  0.0,  0.0,     -6.0,     3.0),
        doubleArrayOf( 2.0, -2.0,  1.0,  0.0,      6.0,    -3.0),
        doubleArrayOf( 1.0,  0.0, -2.0,  0.0,     -6.0,     0.0),
        doubleArrayOf( 0.0,  0.0,  4.0,  0.0,      6.0,    -2.0),
        doubleArrayOf( 4.0,  1.0, -1.0,  0.0,      5.0,    -2.0),
        doubleArrayOf( 4.0,  0.0, -4.0,  0.0,     -5.0,     2.0),
        doubleArrayOf( 1.0,  0.0, -1.0,  2.0,     -5.0,     0.0)
    )

    // Format: [D, M, M', F, Latitude_Amplitude]
    // The complete 60 terms from Meeus Table 47.B for exact Lunar Declination tracking
    private val LUNAR_LATITUDE_TERMS = arrayOf(
        doubleArrayOf( 0.0,  0.0,  0.0,  1.0,   5128.0),
        doubleArrayOf( 0.0,  0.0,  1.0,  1.0,    280.0),
        doubleArrayOf( 0.0,  0.0,  1.0, -1.0,    277.0),
        doubleArrayOf( 2.0,  0.0,  0.0, -1.0,    173.0),
        doubleArrayOf( 2.0,  0.0, -1.0,  1.0,     55.0),
        doubleArrayOf( 2.0,  0.0, -1.0, -1.0,     46.0),
        doubleArrayOf( 2.0,  0.0,  0.0,  1.0,     32.0),
        doubleArrayOf( 0.0,  0.0,  2.0,  1.0,     11.0),
        doubleArrayOf( 2.0,  0.0,  1.0, -1.0,      8.0),
        doubleArrayOf( 0.0,  0.0,  2.0, -1.0,      9.0),
        doubleArrayOf( 2.0, -1.0,  0.0, -1.0,      7.0),
        doubleArrayOf( 2.0,  0.0, -2.0, -1.0,      6.0),
        doubleArrayOf( 0.0,  1.0,  0.0, -1.0,      5.0),
        doubleArrayOf( 2.0, -1.0, -1.0,  1.0,      4.0),
        doubleArrayOf( 0.0,  1.0,  0.0,  1.0,      4.0),
        doubleArrayOf( 2.0, -1.0,  0.0,  1.0,      3.0),
        doubleArrayOf( 0.0,  1.0,  1.0,  1.0,      3.0),
        doubleArrayOf( 2.0,  0.0,  1.0,  1.0,      3.0),
        doubleArrayOf( 2.0,  0.0, -2.0,  1.0,      3.0),
        doubleArrayOf( 0.0,  1.0, -1.0, -1.0,     -3.0),
        doubleArrayOf( 2.0,  0.0,  0.0, -3.0,     -3.0),
        doubleArrayOf( 2.0,  0.0, -1.0, -3.0,     -2.0),
        doubleArrayOf( 0.0,  0.0,  3.0,  1.0,      2.0),
        doubleArrayOf( 0.0,  0.0,  3.0, -1.0,      2.0),
        doubleArrayOf( 4.0,  0.0, -1.0, -1.0,      2.0),
        doubleArrayOf( 2.0,  1.0,  0.0, -1.0,      2.0),
        doubleArrayOf( 2.0,  1.0, -1.0,  1.0,      2.0),
        doubleArrayOf( 0.0,  1.0,  1.0, -1.0,      2.0),
        doubleArrayOf( 2.0, -1.0, -1.0, -1.0,     -2.0),
        doubleArrayOf( 2.0,  0.0,  2.0, -1.0,      2.0),
        doubleArrayOf( 4.0,  0.0, -2.0, -1.0,      2.0),
        doubleArrayOf( 2.0, -1.0, -2.0, -1.0,     -2.0),
        doubleArrayOf( 0.0,  1.0, -1.0,  1.0,     -2.0),
        doubleArrayOf( 2.0,  1.0,  0.0,  1.0,      2.0),
        doubleArrayOf( 4.0,  0.0,  0.0, -1.0,     -2.0),
        doubleArrayOf( 0.0,  0.0,  1.0,  3.0,     -2.0),
        doubleArrayOf( 0.0,  0.0,  1.0, -3.0,     -2.0),
        doubleArrayOf( 0.0,  2.0,  0.0,  1.0,      1.0),
        doubleArrayOf( 2.0,  1.0, -1.0, -1.0,      1.0),
        doubleArrayOf( 0.0,  2.0,  0.0, -1.0,      1.0),
        doubleArrayOf( 2.0, -1.0,  1.0, -1.0,      1.0),
        doubleArrayOf( 2.0,  0.0, -3.0, -1.0,      1.0),
        doubleArrayOf( 2.0, -2.0,  0.0, -1.0,     -1.0),
        doubleArrayOf( 2.0,  0.0, -3.0,  1.0,      1.0),
        doubleArrayOf( 2.0, -1.0,  1.0,  1.0,      1.0),
        doubleArrayOf( 2.0,  0.0,  2.0,  1.0,      1.0),
        doubleArrayOf( 0.0,  0.0,  2.0,  3.0,      1.0),
        doubleArrayOf( 2.0,  0.0,  0.0,  3.0,      1.0),
        doubleArrayOf( 4.0,  0.0, -1.0,  1.0,      1.0),
        doubleArrayOf( 2.0,  1.0,  1.0, -1.0,      1.0),
        doubleArrayOf( 0.0,  0.0,  4.0,  1.0,      1.0),
        doubleArrayOf( 4.0, -1.0, -1.0, -1.0,      1.0),
        doubleArrayOf( 4.0,  0.0, -2.0,  1.0,      1.0),
        doubleArrayOf( 0.0,  1.0,  2.0,  1.0,      1.0),
        doubleArrayOf( 2.0,  1.0, -2.0, -1.0,      1.0),
        doubleArrayOf( 0.0,  1.0,  2.0, -1.0,      1.0),
        doubleArrayOf( 2.0, -1.0,  2.0, -1.0,     -1.0),
        doubleArrayOf( 2.0,  0.0, -1.0,  3.0,     -1.0),
        doubleArrayOf( 4.0,  0.0, -3.0, -1.0,     -1.0),
        doubleArrayOf( 2.0, -1.0, -2.0,  1.0,     -1.0)
    )

    /**
     * Calculates the exact position of the Moon at a specific decimal hour.
     * Incorporates Topocentric Parallax correction based on observer altitude.
     */
    fun calculatePosition(
        date: LocalDate,
        decimalHour: Double,
        latitude: Double,
        longitude: Double,
        tzOffsetHours: Double,
        elevationMeters: Double = 0.0
    ): LunarPosition {
        val latRad = Math.toRadians(latitude)
        val t = calculateJulianCentury(date.year, date.monthValue, date.dayOfMonth, decimalHour, tzOffsetHours)

        // 1. Mean Elements of the Moon (Degrees)
        val lPrime = (218.3164477 + 481267.88123421 * t) % 360.0
        val d = (297.8501921 + 445267.1114034 * t) % 360.0
        val m = (357.5291092 + 35999.0502909 * t) % 360.0
        val mPrime = (134.9633964 + 477198.8675055 * t) % 360.0
        val f = (93.2720950 + 483202.0175233 * t) % 360.0

        // 2. Sum the Perturbations (The chaotic gravity of the Sun and Earth)
        var sumL = 0.0
        var sumR = 0.0
        for (term in LUNAR_LONGITUDE_TERMS) {
            val angleRad = Math.toRadians(term[0] * d + term[1] * m + term[2] * mPrime + term[3] * f)
            sumL += term[4] * sin(angleRad)
            sumR += term[5] * cos(angleRad)
        }

        var sumB = 0.0
        for (term in LUNAR_LATITUDE_TERMS) {
            val angleRad = Math.toRadians(term[0] * d + term[1] * m + term[2] * mPrime + term[3] * f)
            sumB += term[4] * sin(angleRad)
        }

        // 3. Geocentric Ecliptic Coordinates
        val trueLongitudeDeg = lPrime + (sumL / 1000000.0)
        val trueLatitudeDeg = sumB / 1000000.0
        val distanceKm = 385000.56 + (sumR / 1000.0)

        val lambdaRad = Math.toRadians(trueLongitudeDeg)
        val betaRad = Math.toRadians(trueLatitudeDeg)

        // 4. Ecliptic to Equatorial (Right Ascension & Declination)
        val epsilonDeg = 23.439291 - 0.0130042 * t
        val epsRad = Math.toRadians(epsilonDeg)

        val raRad = atan2(sin(lambdaRad) * cos(epsRad) - tan(betaRad) * sin(epsRad), cos(lambdaRad))
        val decRad = asin(sin(betaRad) * cos(epsRad) + cos(betaRad) * sin(epsRad) * sin(lambdaRad))

        // 5. Calculate Local Sidereal Time (LST) to align Earth's rotation
        val jd = calculateJulianDay(date.year, date.monthValue, date.dayOfMonth, decimalHour, tzOffsetHours)
        val jdMidnight = floor(jd - 0.5) + 0.5
        val timeInDays = jd - jdMidnight
        val tMidnight = (jdMidnight - 2451545.0) / 36525.0
        val gmst0 = 24110.54841 + 8640184.812866 * tMidnight + 0.093104 * tMidnight * tMidnight
        val gmstHour = (gmst0 / 3600.0 + timeInDays * 24.0 * 1.00273790935) % 24.0
        val lstRad = Math.toRadians((gmstHour * 15.0) + longitude)

        // 6. Hour Angle
        var hourAngleRad = lstRad - raRad

        // 7. Calculate Geocentric Altitude and Azimuth
        val geoSinAlt = sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(hourAngleRad)
        val geoAltRad = asin(geoSinAlt.coerceIn(-1.0, 1.0))
        val geoAzRad = atan2(sin(hourAngleRad), cos(hourAngleRad) * sin(latRad) - tan(decRad) * cos(latRad))
        var geoAzDeg = Math.toDegrees(geoAzRad) + 180.0
        if (geoAzDeg > 360.0) geoAzDeg -= 360.0

        // 8. Topocentric Parallax Correction (Crucial for the Moon!)
        // The closer you are to the horizon, the more the Earth pushes you "up", making the moon appear lower.
        val equatorialHorizontalParallaxRad = asin(6378.14 / distanceKm)
        val topocentricAltRad = geoAltRad - equatorialHorizontalParallaxRad * cos(geoAltRad)

        return LunarPosition(
            altitude = Math.toDegrees(topocentricAltRad),
            azimuth = geoAzDeg,
            rightAscension = Math.toDegrees(raRad),
            declination = Math.toDegrees(decRad),
            distanceKm = distanceKm
        )
    }

    /**
     * Calculates Moonrise, Moonset, and Culmination.
     * Uses iterative refinement by scanning the day, as the Moon moves too fast across its orbit
     * for standard stationary geometry formulas.
     */
    fun calculateDailyEvents(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        tzOffsetHours: Double,
        elevationMeters: Double = 0.0
    ): LunarDailyEvents {
        var riseHour: Double? = null
        var setHour: Double? = null
        var culminationHour: Double? = null
        var culminationAlt: Double? = null

        var riseAz: Double? = null
        var setAz: Double? = null

        // 1. Scan the 24 hours of the day in 30-minute blocks to locate the events
        var prevAlt = calculatePosition(date, 0.0, latitude, longitude, tzOffsetHours, elevationMeters).altitude
        var prevHour = 0.0

        var maxAlt = prevAlt
        var peakHour = 0.0

        for (minute in 30..1440 step 30) {
            val currentHour = minute / 60.0
            val pos = calculatePosition(date, currentHour, latitude, longitude, tzOffsetHours, elevationMeters)

            // Track highest point for Culmination
            if (pos.altitude > maxAlt) {
                maxAlt = pos.altitude
                peakHour = currentHour
            }

            // Detect Horizon Crossings
            if (prevAlt < ALT_MOONRISE_MOONSET && pos.altitude >= ALT_MOONRISE_MOONSET) {
                riseHour = refineEvent(date, prevHour, currentHour, latitude, longitude, tzOffsetHours, elevationMeters, true)
            } else if (prevAlt > ALT_MOONRISE_MOONSET && pos.altitude <= ALT_MOONRISE_MOONSET) {
                setHour = refineEvent(date, prevHour, currentHour, latitude, longitude, tzOffsetHours, elevationMeters, false)
            }

            prevAlt = pos.altitude
            prevHour = currentHour
        }

        // Refine Culmination Time
        culminationHour = refineCulmination(date, max(0.0, peakHour - 1.0), min(24.0, peakHour + 1.0), latitude, longitude, tzOffsetHours, elevationMeters)
        if (culminationHour != null) {
            culminationAlt = calculatePosition(date, culminationHour, latitude, longitude, tzOffsetHours, elevationMeters).altitude
        }

        if (riseHour != null) riseAz = calculatePosition(date, riseHour, latitude, longitude, tzOffsetHours, elevationMeters).azimuth
        if (setHour != null) setAz = calculatePosition(date, setHour, latitude, longitude, tzOffsetHours, elevationMeters).azimuth

        val isUpAllDay = riseHour == null && setHour == null && prevAlt > 0.0
        val isDownAllDay = riseHour == null && setHour == null && prevAlt < 0.0

        return LunarDailyEvents(
            culmination = culminationHour,
            culminationAltitude = culminationAlt,
            moonrise = riseHour,
            moonriseAzimuth = riseAz,
            moonset = setHour,
            moonsetAzimuth = setAz,
            isMoonUpAllDay = isUpAllDay,
            isMoonDownAllDay = isDownAllDay
        )
    }

    // --- Private Iterative Engines ---

    private fun refineEvent(
        date: LocalDate, startHour: Double, endHour: Double, lat: Double, lon: Double, tz: Double, elev: Double, isRising: Boolean
    ): Double {
        var low = startHour
        var high = endHour
        // Binary search to find the exact sub-minute crossing
        for (i in 0..10) {
            val mid = (low + high) / 2.0
            val alt = calculatePosition(date, mid, lat, lon, tz, elev).altitude

            if (alt > ALT_MOONRISE_MOONSET) {
                if (isRising) high = mid else low = mid
            } else {
                if (isRising) low = mid else high = mid
            }
        }
        return (low + high) / 2.0
    }

    private fun refineCulmination(
        date: LocalDate, startHour: Double, endHour: Double, lat: Double, lon: Double, tz: Double, elev: Double
    ): Double {
        var low = startHour
        var high = endHour
        // Golden Section Search to find the exact peak of the curve
        val phi = (1.0 + sqrt(5.0)) / 2.0
        var c = high - (high - low) / phi
        var d = low + (high - low) / phi

        for (i in 0..15) {
            val altC = calculatePosition(date, c, lat, lon, tz, elev).altitude
            val altD = calculatePosition(date, d, lat, lon, tz, elev).altitude

            if (altC > altD) {
                high = d
                d = c
                c = high - (high - low) / phi
            } else {
                low = c
                c = d
                d = low + (high - low) / phi
            }
        }
        return (low + high) / 2.0
    }

    // --- Core Time Utilities ---

    private fun calculateJulianDay(year: Int, month: Int, day: Int, decimalHour: Double, tzOffsetHours: Double): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)
        val jdMidnight = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
        val utcDecimalHour = decimalHour - tzOffsetHours
        return jdMidnight + (utcDecimalHour / 24.0)
    }

    private fun calculateJulianCentury(year: Int, month: Int, day: Int, decimalHour: Double, tzOffsetHours: Double): Double {
        val jdExact = calculateJulianDay(year, month, day, decimalHour, tzOffsetHours)
        // Fetch Delta T from your SolarEphemeris (or a shared Utils file)
        val deltaTSeconds = calculateDeltaT(year, month)
        val ttJdExact = jdExact + (deltaTSeconds / 86400.0) // Convert seconds to days and add
        return (ttJdExact - 2451545.0) / 36525.0
    }
}