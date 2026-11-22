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
        val isMoonDownAllDay: Boolean,
        val moonDuration: Double, // Total hours above apparent horizon within the 24h calendar day
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

    private data class GeocentricLunarCoords(
        val raRad: Double,
        val decRad: Double,
        val distanceKm: Double,
    )

    private fun calculateGeocentricAtHour(
        date: LocalDate,
        decimalHour: Double,
        tzOffsetHours: Double,
    ): GeocentricLunarCoords {
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

        return GeocentricLunarCoords(raRad, decRad, distanceKm)
    }

    /**
     * Lightweight Earth–Moon geocentric distance evaluation in kilometers for a given instant.
     */
    fun calculateDistanceKm(time: ZonedDateTime): Double {
        val decimalHour = time.hour + (time.minute / 60.0) + (time.second / 3600.0)
        val tzOffsetHours = time.offset.totalSeconds / 3600.0
        val t = calculateJulianCentury(time.year, time.monthValue, time.dayOfMonth, decimalHour, tzOffsetHours)

        val d = (297.8501921 + 445267.1114034 * t) % 360.0
        val m = (357.5291092 + 35999.0502909 * t) % 360.0
        val mPrime = (134.9633964 + 477198.8675055 * t) % 360.0
        val f = (93.2720950 + 483202.0175233 * t) % 360.0

        var sumR = 0.0
        for (term in LUNAR_LONGITUDE_TERMS) {
            val distAmp = term[5]
            if (distAmp != 0.0) {
                val angleRad = Math.toRadians(term[0] * d + term[1] * m + term[2] * mPrime + term[3] * f)
                sumR += distAmp * cos(angleRad)
            }
        }
        return 385000.56 + (sumR / 1000.0)
    }

    /**
     * Precomputed 24-hour lunar ephemeris model for a single date & observer location.
     *
     * Because geocentric lunar equatorial coordinates (RA, Dec, Distance) vary smoothly over a day
     * (~0.27° per 30 min), sampling Meeus Ch. 47 every 30 minutes (49 knots) and applying exact
     * topocentric parallax + sidereal rotation per query achieves <0.0002° error (60x finer than
     * 0.01° rounding) while reducing trig work for full-day curves & events by ~30x.
     */
    class LunarDayModel internal constructor(
        private val coordinates: Coordinates,
        private val sinLat: Double,
        private val cosLat: Double,
        private val rhoSinPhiPrime: Double,
        private val rhoCosPhiPrime: Double,
        private val lstRad0: Double,
        private val raRadKnots: DoubleArray,
        private val decRadKnots: DoubleArray,
        private val sinPiKnots: DoubleArray,
        private val distanceKmKnots: DoubleArray,
    ) {
        private companion object {
            const val SIDEREAL_RAD_PER_HOUR = (15.0 * 1.00273790935) * PI / 180.0
            const val TWO_PI = 2.0 * PI
        }

        fun evaluateAtHour(decimalHour: Double): LunarPosition {
            val knotPos = decimalHour * 2.0
            val k0 = knotPos.toInt().coerceIn(0, raRadKnots.size - 2)
            val frac = knotPos - k0

            val raRad = raRadKnots[k0] + frac * (raRadKnots[k0 + 1] - raRadKnots[k0])
            val decRad = decRadKnots[k0] + frac * (decRadKnots[k0 + 1] - decRadKnots[k0])
            val sinPi = sinPiKnots[k0] + frac * (sinPiKnots[k0 + 1] - sinPiKnots[k0])
            val distanceKm = distanceKmKnots[k0] + frac * (distanceKmKnots[k0 + 1] - distanceKmKnots[k0])

            val lstRad = lstRad0 + decimalHour * SIDEREAL_RAD_PER_HOUR
            val hRad = lstRad - raRad

            val sinDec = sin(decRad)
            val cosDec = cos(decRad)
            val sinH = sin(hRad)
            val cosH = cos(hRad)

            // Full 3D Topocentric Parallax Correction (Meeus Chapter 40)
            val deltaRaNum = -rhoCosPhiPrime * sinPi * sinH
            val deltaRaDen = cosDec - rhoCosPhiPrime * sinPi * cosH
            val deltaRa = atan2(deltaRaNum, deltaRaDen)

            val topoHRad = hRad - deltaRa
            val topoDecNum = (sinDec - rhoSinPhiPrime * sinPi) * cos(deltaRa)
            val topoDecRad = atan2(topoDecNum, deltaRaDen)

            val sinTopoDec = sin(topoDecRad)
            val cosTopoDec = cos(topoDecRad)
            val sinTopoH = sin(topoHRad)
            val cosTopoH = cos(topoHRad)

            val topoSinAlt = sinLat * sinTopoDec + cosLat * cosTopoDec * cosTopoH
            val topoAltRad = asin(topoSinAlt.coerceIn(-1.0, 1.0))

            val topoAzRad = atan2(sinTopoH, cosTopoH * sinLat - tan(topoDecRad) * cosLat)
            var topoAzDeg = Math.toDegrees(topoAzRad) + 180.0
            if (topoAzDeg > 360.0) topoAzDeg -= 360.0

            var topoHDeg = Math.toDegrees(topoHRad) % 360.0
            if (topoHDeg > 180.0) topoHDeg -= 360.0
            if (topoHDeg < -180.0) topoHDeg += 360.0

            // Normalize unwrapped RA back into [-PI, PI] for external consumers
            var normRa = raRad % TWO_PI
            if (normRa > PI) normRa -= TWO_PI
            if (normRa < -PI) normRa += TWO_PI

            return LunarPosition(
                altitude = Math.toDegrees(topoAltRad),
                azimuth = topoAzDeg,
                rightAscension = Math.toDegrees(normRa),
                declination = Math.toDegrees(decRad),
                distanceKm = distanceKm,
                topocentricHourAngle = topoHDeg,
            )
        }

        /**
         * Zero-allocation batch curve generator for 1,441-point daily lunar charts.
         * Optionally fills [outDistancesKm] alongside elevation & azimuth from the same 49 knots.
         */
        fun fillDailyCurve(
            hoursCalc: DoubleArray,
            outElevation: FloatArray,
            outAzimuth: FloatArray,
            outDistancesKm: DoubleArray? = null,
        ) {
            val maxKnot = raRadKnots.size - 2
            for (i in hoursCalc.indices) {
                val decimalHour = hoursCalc[i]
                val knotPos = decimalHour * 2.0
                val k0 = knotPos.toInt().coerceIn(0, maxKnot)
                val frac = knotPos - k0

                val raRad = raRadKnots[k0] + frac * (raRadKnots[k0 + 1] - raRadKnots[k0])
                val decRad = decRadKnots[k0] + frac * (decRadKnots[k0 + 1] - decRadKnots[k0])
                val sinPi = sinPiKnots[k0] + frac * (sinPiKnots[k0 + 1] - sinPiKnots[k0])
                if (outDistancesKm != null) {
                    outDistancesKm[i] = distanceKmKnots[k0] + frac * (distanceKmKnots[k0 + 1] - distanceKmKnots[k0])
                }

                val lstRad = lstRad0 + decimalHour * SIDEREAL_RAD_PER_HOUR
                val hRad = lstRad - raRad

                val sinDec = sin(decRad)
                val cosDec = cos(decRad)
                val sinH = sin(hRad)
                val cosH = cos(hRad)

                val deltaRaNum = -rhoCosPhiPrime * sinPi * sinH
                val deltaRaDen = cosDec - rhoCosPhiPrime * sinPi * cosH
                val deltaRa = atan2(deltaRaNum, deltaRaDen)

                val topoHRad = hRad - deltaRa
                val topoDecNum = (sinDec - rhoSinPhiPrime * sinPi) * cos(deltaRa)
                val topoDecRad = atan2(topoDecNum, deltaRaDen)

                val sinTopoDec = sin(topoDecRad)
                val cosTopoDec = cos(topoDecRad)
                val sinTopoH = sin(topoHRad)
                val cosTopoH = cos(topoHRad)

                val topoSinAlt = sinLat * sinTopoDec + cosLat * cosTopoDec * cosTopoH
                val topoAltRad = asin(topoSinAlt.coerceIn(-1.0, 1.0))

                val topoAzRad = atan2(sinTopoH, cosTopoH * sinLat - tan(topoDecRad) * cosLat)
                var topoAzDeg = Math.toDegrees(topoAzRad) + 180.0
                if (topoAzDeg > 360.0) topoAzDeg -= 360.0

                outElevation[i] = Math.toDegrees(topoAltRad).toFloat()
                outAzimuth[i] = topoAzDeg.toFloat()
            }
        }

        fun calculateDailyEvents(): LunarDailyEvents {
            var riseHour: Double? = null
            var setHour: Double? = null
            var transitHour: Double? = null
            var riseAz: Double? = null
            var setAz: Double? = null

            val startPos = evaluateAtHour(0.0)
            var prevAlt = startPos.altitude
            var prevHA = startPos.topocentricHourAngle
            var prevHour = 0.0

            var maxAlt = prevAlt
            var peakHour = 0.0

            val dynamicHorizonAlt = coordinates.moonApparentHorizonAlt
            var isCurrentlyAbove = prevAlt >= dynamicHorizonAlt
            var aboveStartHour = if (isCurrentlyAbove) 0.0 else Double.NaN
            var totalAboveHours = 0.0

            for (minute in 30..1440 step 30) {
                val currentHour = minute / 60.0
                val pos = evaluateAtHour(currentHour)

                if (pos.altitude > maxAlt) {
                    maxAlt = pos.altitude
                    peakHour = currentHour
                }

                if (prevHA <= 0.0 && pos.topocentricHourAngle > 0.0) {
                    transitHour = refineTransit(prevHour, currentHour)
                }

                if (prevAlt < dynamicHorizonAlt && pos.altitude >= dynamicHorizonAlt) {
                    val refinedRise = refineEvent(prevHour, currentHour, isRising = true, targetAlt = dynamicHorizonAlt)
                    riseHour = refinedRise
                    if (!isCurrentlyAbove) {
                        aboveStartHour = refinedRise
                        isCurrentlyAbove = true
                    }
                } else if (prevAlt > dynamicHorizonAlt && pos.altitude <= dynamicHorizonAlt) {
                    val refinedSet = refineEvent(prevHour, currentHour, isRising = false, targetAlt = dynamicHorizonAlt)
                    setHour = refinedSet
                    if (isCurrentlyAbove) {
                        totalAboveHours += (refinedSet - aboveStartHour).coerceAtLeast(0.0)
                        isCurrentlyAbove = false
                    }
                }

                prevAlt = pos.altitude
                prevHA = pos.topocentricHourAngle
                prevHour = currentHour
            }

            if (isCurrentlyAbove) {
                totalAboveHours += (24.0 - aboveStartHour).coerceAtLeast(0.0)
            }
            val moonDuration = totalAboveHours.coerceIn(0.0, 24.0)

            val culminationHour = refineCulmination(max(0.0, peakHour - 1.0), min(24.0, peakHour + 1.0))
            val peakPos = evaluateAtHour(culminationHour)

            val transitPos = transitHour?.let { evaluateAtHour(it) }
            if (riseHour != null) riseAz = evaluateAtHour(riseHour).azimuth
            if (setHour != null) setAz = evaluateAtHour(setHour).azimuth

            val isUpAllDay = riseHour == null && setHour == null && prevAlt > dynamicHorizonAlt
            val isDownAllDay = riseHour == null && setHour == null && prevAlt < dynamicHorizonAlt

            return LunarDailyEvents(
                transit = transitHour,
                transitAltitude = transitPos?.altitude,
                transitAzimuth = transitPos?.azimuth,
                culmination = culminationHour,
                culminationAltitude = peakPos.altitude,
                culminationAzimuth = peakPos.azimuth,
                moonrise = riseHour,
                moonriseAzimuth = riseAz,
                moonset = setHour,
                moonsetAzimuth = setAz,
                isMoonUpAllDay = isUpAllDay,
                isMoonDownAllDay = isDownAllDay,
                moonDuration = moonDuration,
            )
        }

        private fun refineEvent(
            startHour: Double,
            endHour: Double,
            isRising: Boolean,
            targetAlt: Double,
        ): Double {
            var low = startHour
            var high = endHour
            for (i in 0..10) {
                val mid = (low + high) / 2.0
                val alt = evaluateAtHour(mid).altitude
                if (alt > targetAlt) {
                    if (isRising) high = mid else low = mid
                } else {
                    if (isRising) low = mid else high = mid
                }
            }
            return (low + high) / 2.0
        }

        private fun refineCulmination(startHour: Double, endHour: Double): Double {
            var low = startHour
            var high = endHour
            val phi = (1.0 + sqrt(5.0)) / 2.0
            var c = high - (high - low) / phi
            var d = low + (high - low) / phi

            for (i in 0..15) {
                val altC = evaluateAtHour(c).altitude
                val altD = evaluateAtHour(d).altitude
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

        private fun refineTransit(startHour: Double, endHour: Double): Double {
            var low = startHour
            var high = endHour
            for (i in 0..15) {
                val mid = (low + high) / 2.0
                val ha = evaluateAtHour(mid).topocentricHourAngle
                if (ha > 0.0) high = mid else low = mid
            }
            return (low + high) / 2.0
        }
    }

    /**
     * Builds a reusable [LunarDayModel] for [date] and [coordinates] using 49 half-hour Meeus Ch. 47
     * geocentric knots. Share this across [LunarDayModel.calculateDailyEvents] and
     * [LunarDayModel.fillDailyCurve] so daily updates run in ~1.5 ms instead of ~50 ms.
     */
    fun createDailyModel(
        date: LocalDate,
        coordinates: Coordinates,
        tzOffsetHours: Double,
    ): LunarDayModel {
        val latRad = Math.toRadians(coordinates.latitude)
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val elevKm = coordinates.altitude / 1000.0
        val bA = 0.99664719 // Earth oblateness factor (WGS84)
        val u = atan(bA * tan(latRad))
        val rhoSinPhiPrime = bA * sin(u) + (elevKm / 6378.14) * sinLat
        val rhoCosPhiPrime = cos(u) + (elevKm / 6378.14) * cosLat

        // Exact Local Sidereal Time at 00:00 local time
        val jd0 = calculateJulianDay(date.year, date.monthValue, date.dayOfMonth, 0.0, tzOffsetHours)
        val jdMidnight = floor(jd0 - 0.5) + 0.5
        val timeInDays0 = jd0 - jdMidnight
        val tMidnight = (jdMidnight - 2451545.0) / 36525.0
        val gmst0 = 24110.54841 + 8640184.812866 * tMidnight + 0.093104 * tMidnight * tMidnight
        val gmstHour0 = (gmst0 / 3600.0 + timeInDays0 * 24.0 * 1.00273790935) % 24.0
        val lstRad0 = Math.toRadians((gmstHour0 * 15.0) + coordinates.longitude)

        val numKnots = 49 // 0.0h .. 24.0h every 30 minutes
        val raKnots = DoubleArray(numKnots)
        val decKnots = DoubleArray(numKnots)
        val sinPiKnots = DoubleArray(numKnots)
        val distKnots = DoubleArray(numKnots)
        val twoPi = 2.0 * PI

        for (k in 0 until numKnots) {
            val geo = calculateGeocentricAtHour(date, k * 0.5, tzOffsetHours)
            var ra = geo.raRad
            if (k > 0) {
                var delta = ra - raKnots[k - 1]
                while (delta > PI) {
                    ra -= twoPi
                    delta -= twoPi
                }
                while (delta < -PI) {
                    ra += twoPi
                    delta += twoPi
                }
            }
            raKnots[k] = ra
            decKnots[k] = geo.decRad
            sinPiKnots[k] = 6378.14 / geo.distanceKm
            distKnots[k] = geo.distanceKm
        }

        return LunarDayModel(
            coordinates = coordinates,
            sinLat = sinLat,
            cosLat = cosLat,
            rhoSinPhiPrime = rhoSinPhiPrime,
            rhoCosPhiPrime = rhoCosPhiPrime,
            lstRad0 = lstRad0,
            raRadKnots = raKnots,
            decRadKnots = decKnots,
            sinPiKnots = sinPiKnots,
            distanceKmKnots = distKnots,
        )
    }

    /**
     * FAST INTERNAL ENGINE: Evaluates exact lunar position at a single timestamp.
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

        val geo = calculateGeocentricAtHour(date, decimalHour, tzOffsetHours)
        val raRad = geo.raRad
        val decRad = geo.decRad
        val distanceKm = geo.distanceKm

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
        val sinPi = 6378.14 / distanceKm
        val elevKm = elevationMeters / 1000.0
        val bA = 0.99664719 // Earth oblateness factor (WGS84)
        val u = atan(bA * tan(latRad))

        val rhoSinPhiPrime = bA * sin(u) + (elevKm / 6378.14) * sin(latRad)
        val rhoCosPhiPrime = cos(u) + (elevKm / 6378.14) * cos(latRad)

        val deltaRaNum = -rhoCosPhiPrime * sinPi * sin(hRad)
        val deltaRaDen = cos(decRad) - rhoCosPhiPrime * sinPi * cos(hRad)
        val deltaRa = atan2(deltaRaNum, deltaRaDen)

        // The true Topocentric Equatorial Coordinates
        val topoHRad = hRad - deltaRa

        val topoDecNum = (sin(decRad) - rhoSinPhiPrime * sinPi) * cos(deltaRa)
        val topoDecRad = atan2(topoDecNum, deltaRaDen)

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
        val tzOffsetHours = time.offset.totalSeconds / 3600.0
        return createDailyModel(time.toLocalDate(), coordinates, tzOffsetHours).calculateDailyEvents()
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