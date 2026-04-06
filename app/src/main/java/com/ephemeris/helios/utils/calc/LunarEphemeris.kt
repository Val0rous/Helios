package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.*

object LunarEphemeris {

    // The Moon's extreme proximity to Earth requires a positive altitude for rising/setting.
    // Topocentric Altitude already accounts for Parallax.
    // True Horizon (-0.567 refraction) - Lunar Semidiameter (~0.258) = -0.825 degrees
    const val ALT_MOONRISE_MOONSET = -0.825

    data class LunarPosition (
        val altitude: Double,
        val azimuth: Double,
        val rightAscension: Double,
        val declination: Double,
        val distanceKm: Double,
        val topocentricHourAngle: Double
    )

    data class LunarDailyEvents(
        // Astronomical Culmination (Meridian Transit at exactly 180deg or 0deg)
        val transit: Double?,
        val transitAltitude: Double?,
        val transitAzimuth: Double?,
        // Physical Maximum Altitude (Can differ from transit by a few minutes)
        val culmination: Double?, // Lunar Noon (Highest point)
        val culminationAltitude: Double?,
        val culminationAzimuth: Double?,
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
        doubleArrayOf(0.0, 0.0, 1.0, 0.0, 6288774.0, -20905355.0),
        doubleArrayOf(2.0, 0.0, -1.0, 0.0, 1274027.0, -3699111.0),
        doubleArrayOf(2.0, 0.0, 0.0, 0.0, 658314.0, -2955968.0),
        doubleArrayOf(0.0, 0.0, 2.0, 0.0, 213618.0, -569925.0),
        doubleArrayOf(0.0, 1.0, 0.0, 0.0, -185116.0, 48888.0),
        doubleArrayOf(0.0, 0.0, 0.0, 2.0, -114332.0, -3149.0),
        doubleArrayOf(2.0, 0.0, -2.0, 0.0, 58793.0, 246158.0),
        doubleArrayOf(2.0, -1.0, -1.0, 0.0, 57066.0, -152138.0),
        doubleArrayOf(2.0, 0.0, 1.0, 0.0, 53322.0, -170733.0),
        doubleArrayOf(2.0, -1.0, 0.0, 0.0, 45758.0, -204586.0),
        doubleArrayOf(0.0, 1.0, -1.0, 0.0, -40923.0, -129620.0),
        doubleArrayOf(1.0, 0.0, 0.0, 0.0, -34720.0, 108743.0),
        doubleArrayOf(0.0, 1.0, 1.0, 0.0, -30383.0, 104755.0),
        doubleArrayOf(2.0, 0.0, 0.0, -2.0, 15327.0, 10321.0),
        doubleArrayOf(0.0, 0.0, 1.0, 2.0, -12528.0, 0.0),
        doubleArrayOf(0.0, 0.0, 3.0, 0.0, 10980.0, 4392.0),
        doubleArrayOf(4.0, 0.0, -1.0, 0.0, 10675.0, 6322.0),
        doubleArrayOf(4.0, 0.0, -2.0, 0.0, 10034.0, -9884.0),
        doubleArrayOf(2.0, 1.0, -1.0, 0.0, 8548.0, 5751.0),
        doubleArrayOf(2.0, 1.0, 0.0, 0.0, -8190.0, 0.0),
        doubleArrayOf(1.0, 0.0, -1.0, 0.0, -7927.0, -4664.0),
        doubleArrayOf(1.0, 0.0, 1.0, 0.0, -6766.0, -3820.0),
        doubleArrayOf(2.0, -1.0, 1.0, 0.0, 5163.0, 0.0),
        doubleArrayOf(2.0, 0.0, 2.0, 0.0, 4987.0, -2734.0),
        doubleArrayOf(4.0, 0.0, 0.0, 0.0, 4036.0, -4230.0),
        doubleArrayOf(2.0, 0.0, -3.0, 0.0, 3470.0, 3352.0),
        doubleArrayOf(0.0, 1.0, -2.0, 0.0, -3041.0, -1994.0),
        doubleArrayOf(2.0, 0.0, -1.0, 2.0, 2981.0, 1693.0),
        doubleArrayOf(2.0, -1.0, -2.0, 0.0, 2901.0, 1374.0),
        doubleArrayOf(1.0, 0.0, 0.0, 2.0, -2603.0, 0.0),
        doubleArrayOf(0.0, 1.0, 2.0, 0.0, -2268.0, 0.0),
        doubleArrayOf(0.0, 2.0, 0.0, 0.0, -2043.0, 0.0),
        doubleArrayOf(2.0, -2.0, 0.0, 0.0, 1827.0, 0.0),
        doubleArrayOf(2.0, 1.0, -2.0, 0.0, 1726.0, 0.0),
        doubleArrayOf(0.0, 2.0, -1.0, 0.0, -1590.0, 0.0),
        doubleArrayOf(2.0, -2.0, -1.0, 0.0, -1455.0, 0.0),
        doubleArrayOf(2.0, 0.0, 1.0, -2.0, 1377.0, 0.0),
        doubleArrayOf(2.0, 0.0, 0.0, 2.0, -1355.0, 0.0),
        doubleArrayOf(4.0, 0.0, -3.0, 0.0, 1282.0, 0.0),
        doubleArrayOf(2.0, 1.0, 1.0, 0.0, -1257.0, 0.0),
        doubleArrayOf(0.0, 2.0, 1.0, 0.0, -1202.0, 0.0),
        doubleArrayOf(0.0, 0.0, 2.0, 2.0, -1193.0, 0.0),
        doubleArrayOf(4.0, -1.0, -1.0, 0.0, 1150.0, 0.0),
        doubleArrayOf(2.0, 1.0, 0.0, -2.0, 1085.0, 0.0),
        doubleArrayOf(0.0, 1.0, 0.0, -2.0, 1058.0, 0.0),
        doubleArrayOf(4.0, -1.0, -2.0, 0.0, 1037.0, 0.0),
        doubleArrayOf(0.0, 1.0, 1.0, 2.0, -1026.0, 0.0),
        doubleArrayOf(0.0, 1.0, -1.0, 2.0, -1014.0, 0.0),
        doubleArrayOf(2.0, 0.0, 2.0, -2.0, 981.0, 0.0),
        doubleArrayOf(1.0, 0.0, 1.0, -2.0, -912.0, 0.0),
        doubleArrayOf(2.0, 1.0, -3.0, 0.0, 908.0, 0.0),
        doubleArrayOf(4.0, 0.0, 1.0, 0.0, 893.0, 0.0),
        doubleArrayOf(0.0, 0.0, 3.0, 2.0, -871.0, 0.0),
        doubleArrayOf(4.0, -1.0, 0.0, 0.0, 856.0, 0.0),
        doubleArrayOf(2.0, -2.0, 1.0, 0.0, -847.0, 0.0),
        doubleArrayOf(1.0, 0.0, -2.0, 0.0, -840.0, 0.0),
        doubleArrayOf(0.0, 0.0, 4.0, 0.0, 804.0, 0.0),
        doubleArrayOf(4.0, 1.0, -1.0, 0.0, 775.0, 0.0),
        doubleArrayOf(4.0, 0.0, -4.0, 0.0, 747.0, 0.0),
        doubleArrayOf(1.0, 0.0, -1.0, 2.0, -738.0, 0.0)
    )

    // Format: [D, M, M', F, Latitude_Amplitude]
    // The complete 60 terms from Meeus Table 47.B for exact Lunar Declination tracking
    private val LUNAR_LATITUDE_TERMS = arrayOf(
        doubleArrayOf(0.0, 0.0, 0.0, 1.0, 5128122.0),
        doubleArrayOf(0.0, 0.0, 1.0, 1.0, 280602.0),
        doubleArrayOf(0.0, 0.0, 1.0, -1.0, 277693.0),
        doubleArrayOf(2.0, 0.0, 0.0, -1.0, 173237.0),
        doubleArrayOf(2.0, 0.0, -1.0, 1.0, 55413.0),
        doubleArrayOf(2.0, 0.0, -1.0, -1.0, 46271.0),
        doubleArrayOf(2.0, 0.0, 0.0, 1.0, 32573.0),
        doubleArrayOf(0.0, 0.0, 2.0, 1.0, 17198.0),
        doubleArrayOf(2.0, 0.0, 1.0, -1.0, 9266.0),
        doubleArrayOf(0.0, 0.0, 2.0, -1.0, 8822.0),
        doubleArrayOf(2.0, -1.0, 0.0, -1.0, 8216.0),
        doubleArrayOf(2.0, 0.0, -2.0, -1.0, 4324.0),
        doubleArrayOf(0.0, 1.0, 0.0, -1.0, 4200.0),
        doubleArrayOf(2.0, -1.0, -1.0, 1.0, -3359.0),
        doubleArrayOf(0.0, 1.0, 0.0, 1.0, 2463.0),
        doubleArrayOf(2.0, -1.0, 0.0, 1.0, 2211.0),
        doubleArrayOf(0.0, 1.0, 1.0, 1.0, 2065.0),
        doubleArrayOf(2.0, 0.0, 1.0, 1.0, -2059.0),
        doubleArrayOf(2.0, 0.0, -2.0, 1.0, -1803.0),
        doubleArrayOf(0.0, 1.0, -1.0, -1.0, -1594.0),
        doubleArrayOf(2.0, 0.0, 0.0, -3.0, 1652.0),
        doubleArrayOf(2.0, 0.0, -1.0, -3.0, -1322.0),
        doubleArrayOf(0.0, 0.0, 3.0, 1.0, 1181.0),
        doubleArrayOf(0.0, 0.0, 3.0, -1.0, 742.0),
        doubleArrayOf(4.0, 0.0, -1.0, -1.0, 393.0),
        doubleArrayOf(2.0, 1.0, 0.0, -1.0, 321.0),
        doubleArrayOf(2.0, 1.0, -1.0, 1.0, 281.0),
        doubleArrayOf(0.0, 1.0, 1.0, -1.0, -229.0),
        doubleArrayOf(2.0, -1.0, -1.0, -1.0, 212.0),
        doubleArrayOf(2.0, 0.0, 2.0, -1.0, -207.0),
        doubleArrayOf(4.0, 0.0, -2.0, -1.0, 197.0),
        doubleArrayOf(2.0, -1.0, -2.0, -1.0, 182.0),
        doubleArrayOf(0.0, 1.0, -1.0, 1.0, 175.0),
        doubleArrayOf(2.0, 1.0, 0.0, 1.0, 165.0),
        doubleArrayOf(4.0, 0.0, 0.0, -1.0, -152.0),
        doubleArrayOf(0.0, 0.0, 1.0, 3.0, -147.0),
        doubleArrayOf(0.0, 0.0, 1.0, -3.0, -139.0),
        doubleArrayOf(0.0, 2.0, 0.0, 1.0, -136.0),
        doubleArrayOf(2.0, 1.0, -1.0, -1.0, 132.0),
        doubleArrayOf(0.0, 2.0, 0.0, -1.0, 127.0),
        doubleArrayOf(2.0, -1.0, 1.0, -1.0, -115.0),
        doubleArrayOf(2.0, 0.0, -3.0, -1.0, 113.0),
        doubleArrayOf(2.0, -2.0, 0.0, -1.0, 108.0),
        doubleArrayOf(2.0, 0.0, -3.0, 1.0, -106.0),
        doubleArrayOf(2.0, -1.0, 1.0, 1.0, -101.0),
        doubleArrayOf(2.0, 0.0, 2.0, 1.0, -100.0),
        doubleArrayOf(0.0, 0.0, 2.0, 3.0, -96.0),
        doubleArrayOf(2.0, 0.0, 0.0, 3.0, 87.0),
        doubleArrayOf(4.0, 0.0, -1.0, 1.0, -85.0),
        doubleArrayOf(2.0, 1.0, 1.0, -1.0, -81.0),
        doubleArrayOf(0.0, 0.0, 4.0, 1.0, 79.0),
        doubleArrayOf(4.0, -1.0, -1.0, -1.0, -76.0),
        doubleArrayOf(4.0, 0.0, -2.0, 1.0, -74.0),
        doubleArrayOf(0.0, 1.0, 2.0, 1.0, -74.0),
        doubleArrayOf(2.0, 1.0, -2.0, -1.0, -73.0),
        doubleArrayOf(0.0, 1.0, 2.0, -1.0, -68.0),
        doubleArrayOf(2.0, -1.0, 2.0, -1.0, 68.0),
        doubleArrayOf(2.0, 0.0, -1.0, 3.0, -67.0),
        doubleArrayOf(4.0, 0.0, -3.0, -1.0, 66.0),
        doubleArrayOf(2.0, -1.0, -2.0, 1.0, -65.0)
    )

    /**
     * Calculates the exact position of the Moon at a specific decimal hour.
     * Incorporates Topocentric Parallax correction based on observer altitude.
     */
    fun calculatePosition(
        time: ZonedDateTime,
        coordinates: Coordinates
    ): LunarPosition {
        val decimalHour = time.hour + (time.minute / 60.0) + (time.second / 3600.0)
        val tzOffsetHours = time.offset.totalSeconds / 3600.0
        return getPositionAtHour(
            time.toLocalDate(),
            decimalHour,
            coordinates,
            tzOffsetHours
        )
    }

    /**
     * FAST INTERNAL ENGINE: Used by the iterative scanners so they don't spawn thousands of ZonedDateTime objects.
     */
    fun getPositionAtHour(
        date: LocalDate,
        decimalHour: Double,
        coordinates: Coordinates,
        tzOffsetHours: Double
    ): LunarPosition {
        val latitude = coordinates.latitude
        val longitude = coordinates.longitude
        val elevationMeters = coordinates.altitude
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

        // 6. Geocentric Hour Angle
        val hRad = lstRad - raRad

        // 7. Full 3D Topocentric Parallax Correction (Meeus Chapter 40)
        // Shifts the Moon's coordinates based on the observer's exact spot on the Earth's crust
        val piRad = asin(6378.14 / distanceKm)
        val elevKm = elevationMeters / 1000.0
        val b_a = 0.99664719 // Earth oblateness factor (WGS84)
        val u = atan(b_a * tan(latRad))

        val rhoSinPhiPrime = b_a * sin(u) + (elevKm / 6378.14) * sin(latRad)
        val rhoCosPhiPrime = cos(u) + (elevKm / 6378.14) * cos(latRad)

        val deltaRaNum = -rhoCosPhiPrime * sin(piRad) * sin(hRad)
        val deltaRaDen = cos(decRad) - rhoCosPhiPrime * sin(piRad) * cos(hRad)
        val deltaRa = atan2(deltaRaNum, deltaRaDen)

        // The true Topocentric Equatorial Coordinates
        val topoRaRad = raRad + deltaRa
        val topoHRad = hRad - deltaRa

        val topoDecNum = (sin(decRad) - rhoSinPhiPrime * sin(piRad)) * cos(deltaRa)
        val topoDecDen = cos(decRad) - rhoCosPhiPrime * sin(piRad) * cos(hRad)
        val topoDecRad = atan2(topoDecNum, topoDecDen)


        // 8. Calculate True Topocentric Altitude and Azimuth
        // The closer you are to the horizon, the more the Earth pushes you "up", making the moon appear lower.
        val topoSinAlt = sin(latRad) * sin(topoDecRad) + cos(latRad) * cos(topoDecRad) * cos(topoHRad)
        val topoAltRad = asin(topoSinAlt.coerceIn(-1.0, 1.0))

        val topoAzRad = atan2(sin(topoHRad), cos(topoHRad) * sin(latRad) - tan(topoDecRad) * cos(latRad))
        var topoAzDeg = Math.toDegrees(topoAzRad) + 180.0
        if (topoAzDeg > 360.0) topoAzDeg -= 360.0

        // Normalize Hour Angle to swing from -180 to +180
        var topoHDeg = Math.toDegrees(topoHRad) % 360.0
        if (topoHDeg > 180.0) topoHDeg -= 360.0
        if (topoHDeg < -180.0) topoHDeg += 360.0

        return LunarPosition(
            altitude = Math.toDegrees(topoAltRad),
            azimuth = topoAzDeg,
            rightAscension = Math.toDegrees(raRad),
            declination = Math.toDegrees(decRad),
            distanceKm = distanceKm,
            topocentricHourAngle = topoHDeg
        )
    }

    /**
     * Calculates Moonrise, Moonset, and Culmination.
     * Uses iterative refinement by scanning the day, as the Moon moves too fast across its orbit
     * for standard stationary geometry formulas.
     */
    fun calculateDailyEvents(
        time: ZonedDateTime,
        coordinates: Coordinates
    ): LunarDailyEvents {
        val latitude = coordinates.latitude
        val longitude = coordinates.longitude
        val elevationMeters = coordinates.altitude
        var riseHour: Double? = null
        var setHour: Double? = null
        var culminationHour: Double? = null
        var transitHour: Double? = null

        val date = time.toLocalDate()
        val tzOffsetHours = time.offset.totalSeconds / 3600.0

        var riseAz: Double? = null
        var setAz: Double? = null

        // 1. Scan the 24 hours of the day in 30-minute blocks to locate the events
        var startPos = getPositionAtHour(date, 0.0, coordinates, tzOffsetHours)
        var prevAlt = startPos.altitude
        var prevHA = startPos.topocentricHourAngle
        var prevHour = 0.0

        var maxAlt = prevAlt
        var peakHour = 0.0

        for (minute in 30..1440 step 30) {
            val currentHour = minute / 60.0
            val pos = getPositionAtHour(date, currentHour, coordinates, tzOffsetHours)

            // Track highest point for Culmination
            if (pos.altitude > maxAlt) {
                maxAlt = pos.altitude
                peakHour = currentHour
            }

            // Detect Upper Transit (Hour Angle crosses 0.0)
            if (prevHA <= 0.0 && pos.topocentricHourAngle > 0.0) {
                transitHour = refineTransit(date, prevHour, currentHour, coordinates, tzOffsetHours)
            }

            // Detect Horizon Crossings
            if (prevAlt < ALT_MOONRISE_MOONSET && pos.altitude >= ALT_MOONRISE_MOONSET) {
                riseHour = refineEvent(date, prevHour, currentHour, coordinates, tzOffsetHours, true)
            } else if (prevAlt > ALT_MOONRISE_MOONSET && pos.altitude <= ALT_MOONRISE_MOONSET) {
                setHour = refineEvent(date, prevHour, currentHour, coordinates, tzOffsetHours, false)
            }

            prevAlt = pos.altitude
            prevHA = pos.topocentricHourAngle
            prevHour = currentHour
        }

        // A. Refine Culmination Time
        culminationHour = refineCulmination(date, max(0.0, peakHour - 1.0), min(24.0, peakHour + 1.0), coordinates, tzOffsetHours)

        var culminationAlt: Double? = null
        var culminationAz: Double? = null

        if (culminationHour != null) {
            val peakPos = getPositionAtHour(date, culminationHour, coordinates, tzOffsetHours)
            culminationAlt = peakPos.altitude
            culminationAz = peakPos.azimuth
        }

        // B. Populate Transit Data
        var transitAlt: Double? = null
        var transitAz: Double? = null
        if (transitHour != null) {
            val transitPos = getPositionAtHour(date, transitHour, coordinates, tzOffsetHours)
            transitAlt = transitPos.altitude
            transitAz = transitPos.azimuth
        }

        // C. Populate Rise/Set Data
        if (riseHour != null) riseAz = getPositionAtHour(date, riseHour, coordinates, tzOffsetHours).azimuth
        if (setHour != null) setAz = getPositionAtHour(date, setHour, coordinates, tzOffsetHours).azimuth

        val isUpAllDay = riseHour == null && setHour == null && prevAlt > 0.0
        val isDownAllDay = riseHour == null && setHour == null && prevAlt < 0.0

        return LunarDailyEvents(
            transit = transitHour,
            transitAltitude = transitAlt,
            transitAzimuth = transitAz,
            culmination = culminationHour,
            culminationAltitude = culminationAlt,
            culminationAzimuth = culminationAz,
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
        date: LocalDate,
        startHour: Double,
        endHour: Double,
        coordinates: Coordinates,
        tz: Double,
        isRising: Boolean
    ): Double {

        var low = startHour
        var high = endHour
        // Binary search to find the exact sub-minute crossing
        for (i in 0..10) {
            val mid = (low + high) / 2.0
            val alt = getPositionAtHour(date, mid, coordinates, tz).altitude

            if (alt > ALT_MOONRISE_MOONSET) {
                if (isRising) high = mid else low = mid
            } else {
                if (isRising) low = mid else high = mid
            }
        }
        return (low + high) / 2.0
    }

    private fun refineCulmination(
        date: LocalDate,
        startHour: Double,
        endHour: Double,
        coordinates: Coordinates,
        tz: Double,
    ): Double {
        var low = startHour
        var high = endHour
        // Golden Section Search to find the exact peak of the curve
        val phi = (1.0 + sqrt(5.0)) / 2.0
        var c = high - (high - low) / phi
        var d = low + (high - low) / phi

        for (i in 0..15) {
            val altC = getPositionAtHour(date, c, coordinates, tz).altitude
            val altD = getPositionAtHour(date, d, coordinates, tz).altitude

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

    private fun refineTransit(
        date: LocalDate,
        startHour: Double,
        endHour: Double,
        coordinates: Coordinates,
        tz: Double,
    ): Double {
        var low = startHour
        var high = endHour
        // Binary search to find the exact sub-minute meridian crossing (Hour Angle = 0)
        for (i in 0..15) {
            val mid = (low + high) / 2.0
            val ha = getPositionAtHour(date, mid, coordinates, tz).topocentricHourAngle

            if (ha > 0.0) {
                high = mid
            } else {
                low = mid
            }
        }
        return (low + high) / 2.0
    }
}