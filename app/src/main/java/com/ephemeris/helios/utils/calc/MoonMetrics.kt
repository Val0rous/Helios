package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.*

object MoonMetrics {

    private const val ATMOSPHERE_SCALE_HEIGHT = 8434.0
    private const val AIR_MASS_AT_ZERO = 1.0 / (0.50572 * 0.0521265083015075) // 6.07995^-1.6364 ≈ 0.0521265 -> AM ≈ 37.92

    enum class LunarPhase(val displayName: String) {
        NEW_MOON("New Moon"),
        WAXING_CRESCENT("Waxing Crescent"),
        FIRST_QUARTER("First Quarter"),
        WAXING_GIBBOUS("Waxing Gibbous"),
        FULL_MOON("Full Moon"),
        WANING_GIBBOUS("Waning Gibbous"),
        LAST_QUARTER("Last Quarter"),
        WANING_CRESCENT("Waning Crescent")
    }

    data class LunarMetricsResult(
        val illuminationPercent: Double, // 0.0 to 100.0
        val ageDays: Double,             // 0.0 to 29.53
        val phase: LunarPhase,
        val distanceKm: Double,          // Earth-Moon distance in kilometers
        val angularDiameterDegrees: Double, // Apparent visual size in the sky
        val isSupermoon: Boolean,
        val illuminanceLux: Double,
        val shadowRatio: Double,
        val airMass: Double,
        val colorTempKelvin: Double,
        val perceivedColorTempKelvin: Double,
    )

    private data class LunarPhaseGeometry(
        val normalizedElongationDeg: Double,
        val signedPhaseAngleDeg: Double,
    )

    private fun calculatePhaseGeometry(t: Double): LunarPhaseGeometry {
        // Core Lunar Arguments (Meeus Chapter 47)
        var dDeg = (297.8501921 + 445267.1114034 * t) % 360.0
        if (dDeg < 0.0) dDeg += 360.0
        val dRad = Math.toRadians(dDeg)

        val mDeg = (357.5291092 + 35999.0502909 * t) % 360.0
        val mRad = Math.toRadians(mDeg)

        val mPrimeDeg = (134.9633964 + 477198.8675055 * t) % 360.0
        val mPrimeRad = Math.toRadians(mPrimeDeg)

        // Phase Angle (Meeus Chapter 48): angle Sun-Moon-Earth, signed around 0° (Full Moon)
        val signedPhaseAngleDeg = 180.0 - dDeg -
            6.289 * sin(mPrimeRad) +
            2.100 * sin(mRad) -
            1.274 * sin(2 * dRad - mPrimeRad) -
            0.658 * sin(2 * dRad) -
            0.214 * sin(2 * mPrimeRad) -
            0.110 * sin(dRad)

        return LunarPhaseGeometry(
            normalizedElongationDeg = dDeg,
            signedPhaseAngleDeg = signedPhaseAngleDeg,
        )
    }

    private fun foldPhaseAngleToAlpha(signedPhaseAngleDeg: Double): Double {
        var alpha = abs(signedPhaseAngleDeg) % 360.0
        if (alpha > 180.0) alpha = 360.0 - alpha
        return alpha
    }

    private fun computeAirMass(altitudeDeg: Double, elevationModifier: Double): Double {
        val relativeAirMass = if (altitudeDeg >= 0.0) {
            val sinAlt = sin(Math.toRadians(altitudeDeg))
            val amDenominator = sinAlt + 0.50572 * (altitudeDeg + 6.07995).pow(-1.6364)
            1.0 / amDenominator
        } else {
            val absElev = abs(altitudeDeg)
            AIR_MASS_AT_ZERO + (10.0 * absElev) + (5.0 * absElev * absElev)
        }
        return relativeAirMass * elevationModifier
    }

    private fun computeIlluminanceLux(alphaDeg: Double, distanceKm: Double, actualAirMass: Double): Double {
        // 1. Exo-atmospheric visual magnitude (V) with opposition surge polynomial
        var visualMagnitude = -12.73 + 0.026 * alphaDeg + (4.0e-9) * alphaDeg.pow(4)

        // 2. Adjust magnitude for current Earth-Moon distance (closer moon = brighter)
        visualMagnitude += 5.0 * log10(distanceKm / 384400.0)

        // 3. Convert Magnitude to Lux: E = 10^((-m_V - 14.18) / 2.5)
        val exoAtmosphericLux = 10.0.pow((-visualMagnitude - 14.18) / 2.5)

        // 4. Attenuate light through the atmosphere using Air Mass (0.74 clear-sky transmittance)
        return exoAtmosphericLux * 0.74.pow(actualAirMass)
    }

    private fun computeColorTempKelvin(actualAirMass: Double): Double {
        // Smooth exponential Rayleigh reddening (matches SunMetrics' physical model):
        // 4100K baseline at zenith (AM=1), smoothly approaching 1800K at moonrise/moonset.
        return 1800.0 + 2300.0 * exp(-0.1 * (actualAirMass - 1.0))
    }

    /**
     * Estimates human-perceived moonlight color temperature accounting for the Purkinje effect
     * (CIE 191:2010 mesopic-to-scotopic rod dominance blended in perceptually uniform mireds).
     *
     * Direct moonlight is physically ~4100K at zenith, yet appears cool silvery-blue (~6900K–8000K)
     * to dark-adapted eyes because rods peak near 498 nm. Near the horizon, Rayleigh scattering
     * removes blue-green wavelengths so rising/setting Moons retain their warm amber/orange hue.
     */
    fun computePerceivedColorTempKelvin(
        physicalColorTempKelvin: Double,
        illuminanceLux: Double,
    ): Double {
        if (physicalColorTempKelvin.isNaN() || illuminanceLux <= 0.0) return Double.NaN

        // Convert horizontal illuminance (lx) to typical landscape luminance (cd/m², 18% reflectance)
        // and map across the CIE mesopic transition range [0.005 cd/m² (scotopic), 5.0 cd/m² (photopic)]
        val landscapeLuminance = illuminanceLux * (0.18 / PI)
        val clampedLuminance = landscapeLuminance.coerceIn(0.005, 5.0)
        val rodFactor = ((log10(5.0) - log10(clampedLuminance)) / 3.0).coerceIn(0.0, 1.0)

        // Short-wavelength availability for rod stimulation (drops sharply near the horizon)
        val normalizedBlue = ((physicalColorTempKelvin - 1800.0) / 2300.0).coerceIn(0.0, 1.0)
        val blueAvailability = normalizedBlue * normalizedBlue

        val scotopicTargetK = 8000.0
        val shiftWeight = rodFactor * blueAvailability
        val physicalMired = 1_000_000.0 / physicalColorTempKelvin
        val targetMired = 1_000_000.0 / scotopicTargetK
        val perceivedMired = physicalMired + shiftWeight * (targetMired - physicalMired)
        return 1_000_000.0 / perceivedMired
    }

    fun calculateMetrics(
        time: ZonedDateTime,
        coordinates: Coordinates
    ): LunarMetricsResult {
        val position = LunarEphemeris.calculatePosition(
            time = time,
            coordinates = coordinates
        )
        val decimalHour = time.hour + (time.minute / 60.0) + (time.second / 3600.0)
        val tzOffsetHours = time.offset.totalSeconds / 3600.0
        val t = calculateJulianCentury(time.year, time.monthValue, time.dayOfMonth, decimalHour, tzOffsetHours)

        val (normalizedD, signedPhaseAngleDeg) = calculatePhaseGeometry(t)
        val alphaDeg = foldPhaseAngleToAlpha(signedPhaseAngleDeg)

        val illuminationFraction = (1.0 + cos(Math.toRadians(alphaDeg))) / 2.0
        val illuminationPercent = illuminationFraction * 100.0

        // Moon Age (Synodic Month = 29.530588853 days)
        val ageDays = (normalizedD / 360.0) * 29.530588853

        val phase = when (normalizedD) {
            in 0.0..<6.0, in 354.0..360.0 -> LunarPhase.NEW_MOON
            in 6.0..<84.0 -> LunarPhase.WAXING_CRESCENT
            in 84.0..<96.0 -> LunarPhase.FIRST_QUARTER
            in 96.0..<174.0 -> LunarPhase.WAXING_GIBBOUS
            in 174.0..<186.0 -> LunarPhase.FULL_MOON
            in 186.0..<264.0 -> LunarPhase.WANING_GIBBOUS
            in 264.0..<276.0 -> LunarPhase.LAST_QUARTER
            in 276.0..<354.0 -> LunarPhase.WANING_CRESCENT
            else -> LunarPhase.NEW_MOON
        }

        // Apparent angular diameter in degrees (~0.49° at apogee to ~0.55° at perigee)
        val angularDiameterDegrees = Math.toDegrees(2.0 * asin(1737.4 / position.distanceKm))

        // Supermoon: Full Moon within 360,000 km perigee threshold
        val isSupermoon = (phase == LunarPhase.FULL_MOON || illuminationPercent >= 98.0) && (position.distanceKm <= 360000.0)

        var actualAirMass = 0.0
        var illuminanceLux = 0.0
        var shadowRatio = 0.0
        var colorTempKelvin = Double.NaN
        var perceivedColorTempKelvin = Double.NaN

        if (position.altitude > coordinates.moonApparentHorizonAlt) {
            val amElevationModifier = exp(-coordinates.altitude / ATMOSPHERE_SCALE_HEIGHT)
            actualAirMass = computeAirMass(position.altitude, amElevationModifier)
            illuminanceLux = computeIlluminanceLux(alphaDeg, position.distanceKm, actualAirMass)

            val safeShadowElevRad = Math.toRadians(max(0.01, position.altitude))
            shadowRatio = 1.0 / tan(safeShadowElevRad)
            colorTempKelvin = computeColorTempKelvin(actualAirMass)
            perceivedColorTempKelvin = computePerceivedColorTempKelvin(colorTempKelvin, illuminanceLux)
        }

        return LunarMetricsResult(
            illuminationPercent = round(illuminationPercent * 10) / 10.0,
            ageDays = round(ageDays * 100) / 100.0,
            phase = phase,
            distanceKm = round(position.distanceKm),
            angularDiameterDegrees = round(angularDiameterDegrees * 1000) / 1000.0,
            isSupermoon = isSupermoon,
            illuminanceLux = illuminanceLux,
            shadowRatio = shadowRatio,
            airMass = actualAirMass,
            colorTempKelvin = colorTempKelvin,
            perceivedColorTempKelvin = perceivedColorTempKelvin,
        )
    }

    /**
     * Single-pass batch metrics calculator for 1,441-point daily lunar charts (mirrors [SunMetrics]).
     */
    fun calculateMetrics(
        date: LocalDate,
        hoursCalc: DoubleArray,
        tzOffsetHours: Double,
        moonElevationsDeg: FloatArray,
        distancesKm: DoubleArray,
        observerAltitudeMeters: Double,
        horizonAltDeg: Double,
        outIlluminance: FloatArray,
        outShadowRatio: FloatArray,
        outColorTemp: FloatArray,
        outAirMass: FloatArray,
    ) {
        val size = moonElevationsDeg.size
        val amElevationModifier = exp(-observerAltitudeMeters / ATMOSPHERE_SCALE_HEIGHT)

        // Signed phase angle changes smoothly (~-12°/day), so interpolating its unwrapped value
        // between 00:00 and 24:00 accurately captures Full Moon (0°) and New Moon (180°) crossings.
        val t0 = calculateJulianCentury(date.year, date.monthValue, date.dayOfMonth, 0.0, tzOffsetHours)
        val t24 = calculateJulianCentury(date.year, date.monthValue, date.dayOfMonth, 24.0, tzOffsetHours)
        val signedPhase0 = calculatePhaseGeometry(t0).signedPhaseAngleDeg
        var deltaPhase = calculatePhaseGeometry(t24).signedPhaseAngleDeg - signedPhase0
        while (deltaPhase > 180.0) deltaPhase -= 360.0
        while (deltaPhase < -180.0) deltaPhase += 360.0

        for (i in 0 until size) {
            val moonElevDeg = moonElevationsDeg[i].toDouble()
            if (moonElevDeg <= horizonAltDeg) {
                outAirMass[i] = 0f
                outIlluminance[i] = 0f
                outShadowRatio[i] = 0f
                outColorTemp[i] = Float.NaN
                continue
            }

            val actualAirMass = computeAirMass(moonElevDeg, amElevationModifier)
            outAirMass[i] = actualAirMass.toFloat()

            val frac = hoursCalc[i] / 24.0
            val alphaDeg = foldPhaseAngleToAlpha(signedPhase0 + frac * deltaPhase)
            outIlluminance[i] = computeIlluminanceLux(alphaDeg, distancesKm[i], actualAirMass).toFloat()

            val safeShadowElevRad = Math.toRadians(max(0.01, moonElevDeg))
            outShadowRatio[i] = (1.0 / tan(safeShadowElevRad)).toFloat()

            outColorTemp[i] = computeColorTempKelvin(actualAirMass).toFloat()
        }
    }

    /**
     * Calculates the peak physical metrics for the Moon on a given day.
     * This evaluates the light and atmospheric conditions at the exact second of Culmination.
     * Returns null if the Moon does not culminate on this specific calendar day.
     */
    fun calculatePeakMetrics(
        time: ZonedDateTime, // Pass any time during the target day
        coordinates: Coordinates,
        dailyEvents: LunarEphemeris.LunarDailyEvents? = null,
    ): LunarMetricsResult? {
        // 1. Reuse precomputed daily events when available to avoid scanning the day twice
        val resolvedEvents = dailyEvents ?: LunarEphemeris.calculateDailyEvents(
            time = time,
            coordinates = coordinates
        )

        // If the moon does not culminate today (due to the 24h 50m lunar cycle), there is no peak.
        val culminationDecimalHour = resolvedEvents.culmination ?: return null

        // 2. Convert the decimal hour back into strict hours, minutes, and seconds
        val hour = culminationDecimalHour.toInt().coerceIn(0, 23)
        val minutesRemainder = (culminationDecimalHour - hour) * 60.0
        val minute = minutesRemainder.toInt().coerceIn(0, 59)
        val second = ((minutesRemainder - minute) * 60.0).toInt().coerceIn(0, 59)

        // 3. Construct the exact ZonedDateTime of the peak
        val peakTime = ZonedDateTime.of(
            time.toLocalDate(),
            java.time.LocalTime.of(hour, minute, second),
            time.zone
        )

        // 4. Run the full physics engine at that exact second
        return calculateMetrics(
            time = peakTime,
            coordinates = coordinates
        )
    }

    private fun calculateJulianCentury(year: Int, month: Int, day: Int, decimalHour: Double, tzOffsetHours: Double): Double {
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
        val jdExact = jdMidnight + (utcDecimalHour / 24.0)
        val deltaTSeconds = calculateDeltaT(year, month)
        val ttJdExact = jdExact + (deltaTSeconds / 86400.0)
        return (ttJdExact - 2451545.0) / 36525.0
    }
}
