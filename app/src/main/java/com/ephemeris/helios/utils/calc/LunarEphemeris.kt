package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.*

object LunarEphemeris {

    // Standard horizon atmospheric refraction (34 arcminutes) and mean lunar angular semidiameter.
    // Topocentric Altitude already accounts for Parallax, so upper-limb rise/set occurs at:
    // -(refraction + semidiameter) = -(0.567 + 0.258) = -0.825 degrees.
    const val REFRACTION_HORIZON_DEG = 0.567
    const val MEAN_SEMI_DIAMETER_DEG = 0.258
    const val MOON_RADIUS_KM = 1737.4
    const val ALT_MOONRISE_MOONSET = -0.825

    enum class CulminationMode {
        /** Physical maximum altitude (the true peak of the daily elevation curve). */
        MAX_ALTITUDE,
        /** Astronomical upper meridian transit (topocentric hour angle = 0°, azimuth = 180° or 0°). */
        MERIDIAN_TRANSIT,
    }

    /** Instantaneous geocentric angular semidiameter of the Moon in degrees (~0.245° at apogee to ~0.279° at perigee). */
    fun calculateSemiDiameterDeg(distanceKm: Double): Double =
        Math.toDegrees(asin((MOON_RADIUS_KM / distanceKm).coerceIn(-1.0, 1.0)))

    private fun horizonAltitudeDeg(
        coordinates: Coordinates,
        distanceKm: Double,
        useInstantaneousSemidiameter: Boolean,
    ): Double = if (useInstantaneousSemidiameter) {
        -(REFRACTION_HORIZON_DEG + calculateSemiDiameterDeg(distanceKm)) - coordinates.horizonDipDeg
    } else {
        coordinates.moonApparentHorizonAlt
    }

    data class LunarPosition (
        val altitude: Double,
        val azimuth: Double,
        val rightAscension: Double,
        val declination: Double,
        val distanceKm: Double,
        val topocentricHourAngle: Double,
        val lunarSpeedDegPerMin: Double = 0.0,
        val topocentricDeclination: Double = declination,
    )

    data class LunarDailyEvents(
        // Astronomical Culmination (Meridian Transit at exactly 180deg or 0deg)
        val transit: Double?,
        val transitAltitude: Double?,
        val transitAzimuth: Double?,
        // Selected Culmination (Physical Maximum Altitude by default, or Meridian Transit per CulminationMode)
        val culmination: Double?, // Lunar Noon (Highest point, or null on skipped-culmination days)
        val culminationAltitude: Double?,
        val culminationAzimuth: Double?,
        val moonrise: Double?,
        val moonriseAzimuth: Double?,
        val moonset: Double?,
        val moonsetAzimuth: Double?,
        val isMoonUpAllDay: Boolean,
        val isMoonDownAllDay: Boolean,
        val moonDuration: Double, // Total hours above apparent horizon within the calendar day
        val culminationElapsedHours: Double? = culmination,
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

    internal data class GeocentricEcliptic(
        val longitudeDeg: Double,
        val latitudeDeg: Double,
        val distanceKm: Double,
    )

    private data class GeocentricLunarCoords(
        val raRad: Double,
        val decRad: Double,
        val distanceKm: Double,
    )

    /**
     * Evaluates geocentric lunar ecliptic coordinates (Meeus Chapter 47, full Table 47.A & 47.B)
     * for Julian Century [t] (TT since J2000.0).
     */
    internal fun calculateGeocentricEcliptic(t: Double): GeocentricEcliptic {
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        // 1. Mean Elements of the Moon (Degrees, Meeus Eqs. 47.1–47.5)
        val lPrime = (218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841.0 - t4 / 65194000.0) % 360.0
        val d = (297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868.0 - t4 / 113065000.0) % 360.0
        val m = (357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000.0) % 360.0
        val mPrime = (134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699.0 - t4 / 14712000.0) % 360.0
        val f = (93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000.0 + t4 / 863310000.0) % 360.0

        // Eccentricity factor for terms involving Earth's mean anomaly M (Meeus Eq. 47.6)
        val e = 1.0 - 0.002516 * t - 0.0000074 * t2
        val e2 = e * e

        // 2. Sum the Perturbations (Meeus Table 47.A & 47.B)
        var sumL = 0.0
        var sumR = 0.0
        for (term in LUNAR_LONGITUDE_TERMS) {
            val mCoeff = abs(term[1])
            val eFactor = when (mCoeff) {
                1.0 -> e
                2.0 -> e2
                else -> 1.0
            }
            val angleRad = Math.toRadians(term[0] * d + term[1] * m + term[2] * mPrime + term[3] * f)
            sumL += term[4] * eFactor * sin(angleRad)
            sumR += term[5] * eFactor * cos(angleRad)
        }

        var sumB = 0.0
        for (term in LUNAR_LATITUDE_TERMS) {
            val mCoeff = abs(term[1])
            val eFactor = when (mCoeff) {
                1.0 -> e
                2.0 -> e2
                else -> 1.0
            }
            val angleRad = Math.toRadians(term[0] * d + term[1] * m + term[2] * mPrime + term[3] * f)
            sumB += term[4] * eFactor * sin(angleRad)
        }

        // Additive planetary / Venus / Jupiter / Earth-flattening corrections (Meeus p. 338)
        val a1Rad = Math.toRadians(119.75 + 131.849 * t)
        val a2Rad = Math.toRadians(53.09 + 479264.290 * t)
        val a3Rad = Math.toRadians(313.45 + 481266.484 * t)
        val lPrimeRad = Math.toRadians(lPrime)
        val fRad = Math.toRadians(f)
        sumL += 3958.0 * sin(a1Rad) + 1962.0 * sin(lPrimeRad - fRad) + 318.0 * sin(a2Rad)
        sumB += -2235.0 * sin(lPrimeRad) + 382.0 * sin(a3Rad) + 175.0 * sin(a1Rad - fRad) +
            175.0 * sin(a1Rad + fRad) + 127.0 * sin(lPrimeRad - Math.toRadians(mPrime)) -
            115.0 * sin(lPrimeRad + Math.toRadians(mPrime))

        // 3. Geocentric Ecliptic Coordinates
        val trueLongitudeDeg = lPrime + (sumL / 1000000.0)
        val trueLatitudeDeg = sumB / 1000000.0
        val distanceKm = 385000.56 + (sumR / 1000.0)

        return GeocentricEcliptic(trueLongitudeDeg, trueLatitudeDeg, distanceKm)
    }

    private fun calculateGeocentricAtHour(
        date: LocalDate,
        decimalHour: Double,
        tzOffsetHours: Double,
    ): GeocentricLunarCoords {
        val t = calculateJulianCentury(date.year, date.monthValue, date.dayOfMonth, decimalHour, tzOffsetHours)
        val ecliptic = calculateGeocentricEcliptic(t)

        val lambdaRad = Math.toRadians(ecliptic.longitudeDeg)
        val betaRad = Math.toRadians(ecliptic.latitudeDeg)

        // 4. Ecliptic to Equatorial (Right Ascension & Declination)
        val epsilonDeg = 23.439291 - 0.0130042 * t
        val epsRad = Math.toRadians(epsilonDeg)

        val raRad = atan2(sin(lambdaRad) * cos(epsRad) - tan(betaRad) * sin(epsRad), cos(lambdaRad))
        val decRad = asin(sin(betaRad) * cos(epsRad) + cos(betaRad) * sin(epsRad) * sin(lambdaRad))

        return GeocentricLunarCoords(raRad, decRad, ecliptic.distanceKm)
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
     * Evaluates geocentric lunar declination in degrees for a given instant (used for lunistice detection).
     */
    fun calculateDeclinationDeg(time: ZonedDateTime): Double {
        val decimalHour = time.hour + (time.minute / 60.0) + (time.second / 3600.0)
        val tzOffsetHours = time.offset.totalSeconds / 3600.0
        val geo = calculateGeocentricAtHour(time.toLocalDate(), decimalHour, tzOffsetHours)
        return Math.toDegrees(geo.decRad)
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
        internal val timeline: LocalDayTimeline,
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
            const val SPEED_HALF_STEP_HOURS = 15.0 / 3600.0 // ±15s central difference (30s span = 0.5 min)
        }

        private fun altitudeAtHour(decimalHour: Double): Double {
            val knotPos = decimalHour * 2.0
            val k0 = knotPos.toInt().coerceIn(0, raRadKnots.size - 2)
            val frac = knotPos - k0

            val raRad = raRadKnots[k0] + frac * (raRadKnots[k0 + 1] - raRadKnots[k0])
            val decRad = decRadKnots[k0] + frac * (decRadKnots[k0 + 1] - decRadKnots[k0])
            val sinPi = sinPiKnots[k0] + frac * (sinPiKnots[k0 + 1] - sinPiKnots[k0])

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

            val topoSinAlt = sinLat * sin(topoDecRad) + cosLat * cos(topoDecRad) * cos(topoHRad)
            return Math.toDegrees(asin(topoSinAlt.coerceIn(-1.0, 1.0)))
        }

        private fun distanceKmAtHour(decimalHour: Double): Double {
            val knotPos = decimalHour * 2.0
            val k0 = knotPos.toInt().coerceIn(0, distanceKmKnots.size - 2)
            val frac = knotPos - k0
            return distanceKmKnots[k0] + frac * (distanceKmKnots[k0 + 1] - distanceKmKnots[k0])
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

            val altPlus = altitudeAtHour(decimalHour + SPEED_HALF_STEP_HOURS)
            val altMinus = altitudeAtHour(decimalHour - SPEED_HALF_STEP_HOURS)
            val speedDegPerMin = (altPlus - altMinus) / (2.0 * SPEED_HALF_STEP_HOURS * 60.0)

            return LunarPosition(
                altitude = Math.toDegrees(topoAltRad),
                azimuth = topoAzDeg,
                rightAscension = Math.toDegrees(normRa),
                declination = Math.toDegrees(decRad),
                distanceKm = distanceKm,
                topocentricHourAngle = topoHDeg,
                lunarSpeedDegPerMin = speedDegPerMin,
                topocentricDeclination = Math.toDegrees(topoDecRad),
            )
        }

        /**
         * Zero-allocation batch curve generator for 1,441-point daily lunar charts.
         * Maps wall-clock [hoursCalc] (`0..24`) onto the continuous physical timeline so DST
         * transition days align seamlessly with local wall-clock events and live positions.
         * Optionally fills [outDistancesKm] alongside elevation & azimuth from the same knots.
         */
        fun fillDailyCurve(
            hoursCalc: DoubleArray,
            outElevation: FloatArray,
            outAzimuth: FloatArray,
            outDistancesKm: DoubleArray? = null,
        ) {
            val maxKnot = raRadKnots.size - 2
            for (i in hoursCalc.indices) {
                val elapsedHour = timeline.wallClockHourToElapsedHour(hoursCalc[i])
                val knotPos = elapsedHour * 2.0
                val k0 = knotPos.toInt().coerceIn(0, maxKnot)
                val frac = knotPos - k0

                val raRad = raRadKnots[k0] + frac * (raRadKnots[k0 + 1] - raRadKnots[k0])
                val decRad = decRadKnots[k0] + frac * (decRadKnots[k0 + 1] - decRadKnots[k0])
                val sinPi = sinPiKnots[k0] + frac * (sinPiKnots[k0 + 1] - sinPiKnots[k0])
                if (outDistancesKm != null) {
                    outDistancesKm[i] = distanceKmKnots[k0] + frac * (distanceKmKnots[k0 + 1] - distanceKmKnots[k0])
                }

                val lstRad = lstRad0 + elapsedHour * SIDEREAL_RAD_PER_HOUR
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

        fun calculateDailyEvents(
            culminationMode: CulminationMode = CulminationMode.MAX_ALTITUDE,
            useInstantaneousSemidiameter: Boolean = false,
        ): LunarDailyEvents {
            fun signedAltAboveHorizon(hour: Double): Double {
                val alt = altitudeAtHour(hour)
                val targetAlt = horizonAltitudeDeg(
                    coordinates = coordinates,
                    distanceKm = distanceKmAtHour(hour),
                    useInstantaneousSemidiameter = useInstantaneousSemidiameter,
                )
                return alt - targetAlt
            }

            // 5-minute coarse scan across the full physical calendar day (23h / 24h / 25h)
            val dayHours = timeline.durationHours
            val stepMinutes = 5
            val stepHours = stepMinutes / 60.0
            val totalMinutes = (dayHours * 60.0).roundToInt()
            val numSamples = (totalMinutes / stepMinutes) + 1
            val alts = DoubleArray(numSamples)
            val signedAlts = DoubleArray(numSamples)

            var riseHour: Double? = null
            var setHour: Double? = null
            var transitHour: Double? = null
            var riseAz: Double? = null
            var setAz: Double? = null

            val startPos = evaluateAtHour(0.0)
            alts[0] = startPos.altitude
            signedAlts[0] = startPos.altitude - horizonAltitudeDeg(
                coordinates = coordinates,
                distanceKm = startPos.distanceKm,
                useInstantaneousSemidiameter = useInstantaneousSemidiameter,
            )
            var prevHA = startPos.topocentricHourAngle
            var prevSigned = signedAlts[0]
            var prevHour = 0.0

            var isCurrentlyAbove = prevSigned >= 0.0
            var aboveStartHour = if (isCurrentlyAbove) 0.0 else Double.NaN
            var totalAboveHours = 0.0

            for (idx in 1 until numSamples) {
                val currentHour = min(idx * stepHours, dayHours)
                val pos = evaluateAtHour(currentHour)
                val currSigned = pos.altitude - horizonAltitudeDeg(
                    coordinates = coordinates,
                    distanceKm = pos.distanceKm,
                    useInstantaneousSemidiameter = useInstantaneousSemidiameter,
                )
                alts[idx] = pos.altitude
                signedAlts[idx] = currSigned

                if (prevHA <= 0.0 && pos.topocentricHourAngle > 0.0) {
                    transitHour = refineTransit(prevHour, currentHour)
                }

                if (prevSigned < 0.0 && currSigned >= 0.0) {
                    val refinedRise = refineHorizonCrossing(
                        startHour = prevHour,
                        endHour = currentHour,
                        isRising = true,
                        signedAlt = ::signedAltAboveHorizon,
                    )
                    riseHour = refinedRise
                    if (!isCurrentlyAbove) {
                        aboveStartHour = refinedRise
                        isCurrentlyAbove = true
                    }
                } else if (prevSigned > 0.0 && currSigned <= 0.0) {
                    val refinedSet = refineHorizonCrossing(
                        startHour = prevHour,
                        endHour = currentHour,
                        isRising = false,
                        signedAlt = ::signedAltAboveHorizon,
                    )
                    setHour = refinedSet
                    if (isCurrentlyAbove) {
                        totalAboveHours += (refinedSet - aboveStartHour).coerceAtLeast(0.0)
                        isCurrentlyAbove = false
                    }
                }

                prevSigned = currSigned
                prevHA = pos.topocentricHourAngle
                prevHour = currentHour
            }

            if (isCurrentlyAbove) {
                totalAboveHours += (dayHours - aboveStartHour).coerceAtLeast(0.0)
            }

            // Detect genuine interior local maximum (physical culmination) and local minimum (nadir).
            // On ~once-per-month "skipped culmination" days (due to the 24h 50m lunar cycle), there is
            // no interior maximum inside [0, dayHours], so maxAltHour stays null instead of clamping
            // to a bogus start/end endpoint.
            val eps = 1.0 / 3600.0 // 1 second
            var maxBracket: Pair<Double, Double>? = null
            var minBracket: Pair<Double, Double>? = null

            for (k in 1 until numSamples - 1) {
                if (maxBracket == null &&
                    ((alts[k] >= alts[k - 1] && alts[k] > alts[k + 1]) ||
                        (alts[k] > alts[k - 1] && alts[k] >= alts[k + 1]))
                ) {
                    maxBracket = ((k - 1) * stepHours) to min((k + 1) * stepHours, dayHours)
                }
                if (minBracket == null &&
                    ((alts[k] <= alts[k - 1] && alts[k] < alts[k + 1]) ||
                        (alts[k] < alts[k - 1] && alts[k] <= alts[k + 1]))
                ) {
                    minBracket = ((k - 1) * stepHours) to min((k + 1) * stepHours, dayHours)
                }
            }

            // Catch genuine peaks/nadirs falling strictly within the first or last 5-minute slice
            val slopeStart = altitudeAtHour(eps) - alts[0]
            val slopeAfterFirst = altitudeAtHour(stepHours + eps) - altitudeAtHour(stepHours - eps)
            val slopeBeforeLast = altitudeAtHour(dayHours - stepHours + eps) - altitudeAtHour(dayHours - stepHours - eps)
            val slopeEnd = alts[numSamples - 1] - altitudeAtHour(dayHours - eps)

            if (maxBracket == null) {
                if (slopeStart > 0.0 && slopeAfterFirst <= 0.0) {
                    maxBracket = 0.0 to stepHours
                } else if (slopeBeforeLast >= 0.0 && slopeEnd < 0.0) {
                    maxBracket = (dayHours - stepHours) to dayHours
                }
            }
            if (minBracket == null) {
                if (slopeStart < 0.0 && slopeAfterFirst >= 0.0) {
                    minBracket = 0.0 to stepHours
                } else if (slopeBeforeLast <= 0.0 && slopeEnd > 0.0) {
                    minBracket = (dayHours - stepHours) to dayHours
                }
            }

            val maxAltHour = maxBracket?.let { (a, b) -> refineExtremum(a, b, findMax = true) }
            val minAltHour = minBracket?.let { (a, b) -> refineExtremum(a, b, findMax = false) }

            // Polar grazing rise/set guarantee: even if the Moon pokes above (or dips below) the
            // horizon for less than 5 minutes between grid points, its true extremum detects it.
            if (riseHour == null && setHour == null) {
                if (signedAlts[0] < 0.0 && maxAltHour != null && signedAltAboveHorizon(maxAltHour) >= 0.0) {
                    val leftGrid = (floor(maxAltHour / stepHours) * stepHours).coerceAtLeast(0.0)
                    val rightGrid = (ceil(maxAltHour / stepHours) * stepHours).coerceAtMost(dayHours)
                    riseHour = refineHorizonCrossing(leftGrid, maxAltHour, isRising = true, signedAlt = ::signedAltAboveHorizon)
                    setHour = refineHorizonCrossing(maxAltHour, rightGrid, isRising = false, signedAlt = ::signedAltAboveHorizon)
                    totalAboveHours = (setHour - riseHour).coerceAtLeast(0.0)
                } else if (signedAlts[0] >= 0.0 && minAltHour != null && signedAltAboveHorizon(minAltHour) < 0.0) {
                    val leftGrid = (floor(minAltHour / stepHours) * stepHours).coerceAtLeast(0.0)
                    val rightGrid = (ceil(minAltHour / stepHours) * stepHours).coerceAtMost(dayHours)
                    setHour = refineHorizonCrossing(leftGrid, minAltHour, isRising = false, signedAlt = ::signedAltAboveHorizon)
                    riseHour = refineHorizonCrossing(minAltHour, rightGrid, isRising = true, signedAlt = ::signedAltAboveHorizon)
                    totalAboveHours = (dayHours - (riseHour - setHour)).coerceIn(0.0, dayHours)
                }
            }

            val moonDuration = totalAboveHours.coerceIn(0.0, dayHours)

            val transitPos = transitHour?.let { evaluateAtHour(it) }
            val selectedCulminationHour = when (culminationMode) {
                CulminationMode.MAX_ALTITUDE -> maxAltHour
                CulminationMode.MERIDIAN_TRANSIT -> transitHour
            }
            val selectedCulminationPos = when (culminationMode) {
                CulminationMode.MAX_ALTITUDE -> maxAltHour?.let { evaluateAtHour(it) }
                CulminationMode.MERIDIAN_TRANSIT -> transitPos
            }

            if (riseHour != null) riseAz = evaluateAtHour(riseHour).azimuth
            if (setHour != null) setAz = evaluateAtHour(setHour).azimuth

            val isUpAllDay = riseHour == null && setHour == null && signedAlts[numSamples - 1] >= 0.0
            val isDownAllDay = riseHour == null && setHour == null && signedAlts[numSamples - 1] < 0.0

            fun Double?.toWallClock(): Double? = this?.let { timeline.elapsedHourToWallClockHour(it) }

            return LunarDailyEvents(
                transit = transitHour.toWallClock(),
                transitAltitude = transitPos?.altitude,
                transitAzimuth = transitPos?.azimuth,
                culmination = selectedCulminationHour.toWallClock(),
                culminationAltitude = selectedCulminationPos?.altitude,
                culminationAzimuth = selectedCulminationPos?.azimuth,
                moonrise = riseHour.toWallClock(),
                moonriseAzimuth = riseAz,
                moonset = setHour.toWallClock(),
                moonsetAzimuth = setAz,
                isMoonUpAllDay = isUpAllDay,
                isMoonDownAllDay = isDownAllDay,
                moonDuration = moonDuration,
                culminationElapsedHours = selectedCulminationHour,
            )
        }

        private inline fun refineHorizonCrossing(
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

        private fun refineExtremum(startHour: Double, endHour: Double, findMax: Boolean): Double {
            var low = startHour
            var high = endHour
            val phi = (1.0 + sqrt(5.0)) / 2.0
            var c = high - (high - low) / phi
            var d = low + (high - low) / phi
            var altC = altitudeAtHour(c)
            var altD = altitudeAtHour(d)

            for (i in 0..24) {
                val pickLeft = if (findMax) altC > altD else altC < altD
                if (pickLeft) {
                    high = d
                    d = c
                    altD = altC
                    c = high - (high - low) / phi
                    altC = altitudeAtHour(c)
                } else {
                    low = c
                    c = d
                    altC = altD
                    d = low + (high - low) / phi
                    altD = altitudeAtHour(d)
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
     * Builds a DST-aware reusable [LunarDayModel] for [date], [coordinates], and [zoneId] using
     * half-hour Meeus Ch. 47 geocentric knots spanning the full physical calendar day (23h, 24h,
     * or 25h). Share this across [LunarDayModel.calculateDailyEvents] and
     * [LunarDayModel.fillDailyCurve] so daily updates run in ~1.5 ms instead of ~50 ms.
     */
    fun createDailyModel(
        date: LocalDate,
        coordinates: Coordinates,
        zoneId: ZoneId,
    ): LunarDayModel = createDailyModel(LocalDayTimeline(date, zoneId), coordinates)

    /**
     * Fixed-offset overload of [createDailyModel] for callers that hold a numeric UTC offset.
     */
    fun createDailyModel(
        date: LocalDate,
        coordinates: Coordinates,
        tzOffsetHours: Double,
    ): LunarDayModel = createDailyModel(LocalDayTimeline.fixedOffset(date, tzOffsetHours), coordinates)

    private fun createDailyModel(
        timeline: LocalDayTimeline,
        coordinates: Coordinates,
    ): LunarDayModel {
        val date = timeline.date
        val tzOffsetHours = timeline.startUtcAnchorOffsetHours
        val latRad = Math.toRadians(coordinates.latitude)
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val elevKm = coordinates.altitude / 1000.0
        val bA = 0.99664719 // Earth oblateness factor (WGS84)
        val u = atan(bA * tan(latRad))
        val rhoSinPhiPrime = bA * sin(u) + (elevKm / 6378.14) * sinLat
        val rhoCosPhiPrime = cos(u) + (elevKm / 6378.14) * cosLat

        // Exact Local Sidereal Time at local start-of-day (elapsedHour = 0.0)
        val jd0 = calculateJulianDay(date.year, date.monthValue, date.dayOfMonth, 0.0, tzOffsetHours)
        val jdMidnight = floor(jd0 - 0.5) + 0.5
        val timeInDays0 = jd0 - jdMidnight
        val tMidnight = (jdMidnight - 2451545.0) / 36525.0
        val gmst0 = 24110.54841 + 8640184.812866 * tMidnight + 0.093104 * tMidnight * tMidnight
        val gmstHour0 = (gmst0 / 3600.0 + timeInDays0 * 24.0 * 1.00273790935) % 24.0
        val lstRad0 = Math.toRadians((gmstHour0 * 15.0) + coordinates.longitude)

        // Half-hour knots covering [0, durationHours] (47 for 23h, 49 for 24h, 51 for 25h)
        val numKnots = max(2, ceil(timeline.durationHours * 2.0).toInt() + 1)
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
            timeline = timeline,
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

    private fun evaluateSinglePosition(
        date: LocalDate,
        decimalHour: Double,
        coordinates: Coordinates,
        tzOffsetHours: Double,
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
            topocentricHourAngle = topoHDeg,
            topocentricDeclination = Math.toDegrees(topoDecRad),
        )
    }

    /**
     * FAST INTERNAL ENGINE: Evaluates exact lunar position (including true topocentric vertical
     * velocity accounting for lunar RA/Dec drift and topocentric parallax rate) at a single timestamp.
     */
    fun getPositionAtHour(
        date: LocalDate,
        decimalHour: Double,
        coordinates: Coordinates,
        tzOffsetHours: Double
    ): LunarPosition {
        val center = evaluateSinglePosition(date, decimalHour, coordinates, tzOffsetHours)
        val halfStepHours = 15.0 / 3600.0 // ±15s central difference (30s span = 0.5 min)
        val altPlus = evaluateSinglePosition(date, decimalHour + halfStepHours, coordinates, tzOffsetHours).altitude
        val altMinus = evaluateSinglePosition(date, decimalHour - halfStepHours, coordinates, tzOffsetHours).altitude
        val speedDegPerMin = (altPlus - altMinus) / (2.0 * halfStepHours * 60.0)
        return center.copy(lunarSpeedDegPerMin = speedDegPerMin)
    }

    /**
     * Calculates Moonrise, Moonset, and Culmination.
     * Uses iterative refinement by scanning the day, as the Moon moves too fast across its orbit
     * for standard stationary geometry formulas.
     *
     * @param culminationMode Whether [LunarDailyEvents.culmination] reports the physical maximum
     *   altitude ([CulminationMode.MAX_ALTITUDE], default) or astronomical upper meridian transit
     *   ([CulminationMode.MERIDIAN_TRANSIT]).
     * @param useInstantaneousSemidiameter When `true`, uses the Moon's instantaneous distance-based
     *   angular semidiameter (`~0.245°..0.279°`) instead of the fixed mean `0.258°` (default `false`).
     */
    fun calculateDailyEvents(
        time: ZonedDateTime,
        coordinates: Coordinates,
        culminationMode: CulminationMode = CulminationMode.MAX_ALTITUDE,
        useInstantaneousSemidiameter: Boolean = false,
    ): LunarDailyEvents {
        return createDailyModel(time.toLocalDate(), coordinates, time.zone).calculateDailyEvents(
            culminationMode = culminationMode,
            useInstantaneousSemidiameter = useInstantaneousSemidiameter,
        )
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