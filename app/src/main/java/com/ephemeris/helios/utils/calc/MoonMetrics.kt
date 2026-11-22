package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.datastore.MoonColorTempMode
import com.ephemeris.helios.utils.datastore.MoonIlluminanceMode
import com.ephemeris.helios.utils.location.Coordinates
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.*

object MoonMetrics {

    private const val ATMOSPHERE_SCALE_HEIGHT = 8434.0
    private const val MOON_SEMI_DIAMETER_DEG = 0.258
    private const val AIR_MASS_AT_ZERO = 1.0 / (0.50572 * 0.0521265083015075) // 6.07995^-1.6364 ≈ 0.0521265 -> AM ≈ 37.92
    // Clear-sky scattered horizon fraction matching SunMetrics (~0.28% of exo-atmospheric flux at 0°)
    private const val TWILIGHT_HORIZON_SCATTER_RATIO = 0.00282
    private const val ASTRONOMICAL_TWILIGHT_DEG = -18.0

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
        val isWaxing: Boolean,           // true from New Moon -> Full Moon, false from Full Moon -> New Moon
        val ageDays: Double,             // 0.0 to 29.53
        val phase: LunarPhase,
        val distanceKm: Double,          // Earth-Moon distance in kilometers
        val angularDiameterDegrees: Double, // Apparent visual size in the sky
        val isSupermoon: Boolean,
        /** Global Horizontal lunar illuminance (lux) — default illuminance metric (direct horizontal + ambient sky). */
        val illuminanceLux: Double,
        /** Direct beam lunar illuminance (lux). */
        val directIlluminanceLux: Double = illuminanceLux,
        /** Global Horizontal lunar illuminance (lux): alias for [illuminanceLux]. */
        val globalIlluminanceLux: Double = illuminanceLux,
        /** Diffuse / Ambient sky lunar illuminance (lux): Rayleigh-scattered moonlight (including lunar twilight). */
        val diffuseIlluminanceLux: Double = 0.0,
        val shadowRatio: Double,
        val airMass: Double,
        /** Physical direct lunar beam color temperature (K). */
        val colorTempKelvin: Double,
        /** Human dark-adapted perceived moonlight color temperature (K, Purkinje shift). */
        val perceivedColorTempKelvin: Double,
    ) {
        fun illuminanceFor(mode: MoonIlluminanceMode): Double = when (mode) {
            MoonIlluminanceMode.GLOBAL -> globalIlluminanceLux
            MoonIlluminanceMode.DIRECT -> directIlluminanceLux
        }

        fun colorTempFor(mode: MoonColorTempMode): Double = when (mode) {
            MoonColorTempMode.PURKINJE -> perceivedColorTempKelvin
            MoonColorTempMode.DIRECT -> colorTempKelvin
        }
    }

    /**
     * Advisories for when the Moon is above the horizon (so metrics are still computed),
     * yet too dim in practice for a human observer to notice shadows or lunar color temperature.
     */
    data class LunarMetricNoticeability(
        val shouldWarnUnnoticeableShadows: Boolean,
        val shadowsOnlyFaint: Boolean,
        val shouldWarnUnnoticeableColorTemp: Boolean,
    )

    private const val SHADOW_UNNOTICEABLE_MAX_LUX = 0.005 // 5 mlx (starlight/airglow floor ~1–2 mlx)
    private const val SHADOW_FAINT_MAX_LUX = 0.025        // 25 mlx (~Quarter Moon for crisp shadows)
    private const val COLOR_TEMP_UNNOTICEABLE_MAX_ILLUM_PERCENT = 1.0
    private const val COLOR_TEMP_UNNOTICEABLE_MAX_LUX = 0.0005 // 0.5 mlx
    private const val CIVIL_TWILIGHT_ALT_DEG = -6.0

    /**
     * Decides when to inform the user that lunar shadows or lunar color temperature
     * are not realistically noticeable (e.g., near New Moon, ultra-thin crescent, or
     * optionally when daylight/civil twilight overwhelms moonlight).
     *
     * Returns all-false when the Moon is below the horizon, since the UI already displays
     * an off/infinite state there.
     */
    @Suppress("unused")
    fun evaluateMetricNoticeability(
        metrics: LunarMetricsResult,
        sunElevationDeg: Double? = null,
    ): LunarMetricNoticeability {
        val isMoonUp = metrics.directIlluminanceLux > 0.0 && !metrics.colorTempKelvin.isNaN()
        if (!isMoonUp) {
            return LunarMetricNoticeability(
                shouldWarnUnnoticeableShadows = false,
                shadowsOnlyFaint = false,
                shouldWarnUnnoticeableColorTemp = false,
            )
        }

        val drownedOutBySun = sunElevationDeg != null && sunElevationDeg > CIVIL_TWILIGHT_ALT_DEG
        val shadowsUnnoticeable = drownedOutBySun || metrics.illuminanceLux < SHADOW_UNNOTICEABLE_MAX_LUX
        val shadowsFaint = !shadowsUnnoticeable && metrics.illuminanceLux < SHADOW_FAINT_MAX_LUX
        val colorTempUnnoticeable = drownedOutBySun ||
            metrics.illuminationPercent < COLOR_TEMP_UNNOTICEABLE_MAX_ILLUM_PERCENT ||
            metrics.illuminanceLux < COLOR_TEMP_UNNOTICEABLE_MAX_LUX

        return LunarMetricNoticeability(
            shouldWarnUnnoticeableShadows = shadowsUnnoticeable,
            shadowsOnlyFaint = shadowsFaint,
            shouldWarnUnnoticeableColorTemp = colorTempUnnoticeable,
        )
    }

    private data class LunarPhaseGeometry(
        val normalizedElongationDeg: Double,
        val signedPhaseAngleDeg: Double,
    )

    private data class LunarIlluminanceComponents(
        val directLux: Double,
        val diffuseLux: Double,
        val globalLux: Double,
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

    private fun computeExoAtmosphericLux(alphaDeg: Double, distanceKm: Double): Double {
        // 1. Exo-atmospheric visual magnitude (V) with opposition surge polynomial
        var visualMagnitude = -12.73 + 0.026 * alphaDeg + (4.0e-9) * alphaDeg.pow(4)

        // 2. Adjust magnitude for current Earth-Moon distance (closer moon = brighter)
        visualMagnitude += 5.0 * log10(distanceKm / 384400.0)

        // 3. Convert Magnitude to Lux: E = 10^((-m_V - 14.18) / 2.5)
        return 10.0.pow((-visualMagnitude - 14.18) / 2.5)
    }

    /**
     * Computes Direct Beam, Diffuse/Ambient Sky, and Global Horizontal lunar illuminance,
     * mirroring [SunMetrics]'s Liu-Jordan clear-sky + twilight scattering model.
     */
    private fun computeIlluminanceComponents(
        moonElevDeg: Double,
        horizonAltDeg: Double,
        alphaDeg: Double,
        distanceKm: Double,
        actualAirMass: Double,
        amElevationModifier: Double,
    ): LunarIlluminanceComponents {
        val exoAtmosphericLux = computeExoAtmosphericLux(alphaDeg, distanceKm)

        if (moonElevDeg <= horizonAltDeg) {
            val twilightDiffuse = if (moonElevDeg > ASTRONOMICAL_TWILIGHT_DEG) {
                TWILIGHT_HORIZON_SCATTER_RATIO * exoAtmosphericLux * exp(0.8 * moonElevDeg) * amElevationModifier
            } else {
                0.0
            }
            return LunarIlluminanceComponents(
                directLux = 0.0,
                diffuseLux = twilightDiffuse,
                globalLux = twilightDiffuse,
            )
        }

        val posSinElev = max(0.0, sin(Math.toRadians(moonElevDeg)))
        val beamTransmittance = 0.74.pow(actualAirMass)
        val directLux = exoAtmosphericLux * beamTransmittance
        val directHorizontalLux = directLux * posSinElev

        val skyDiffuse = max(
            0.0,
            (0.271 - 0.2939 * beamTransmittance) * exoAtmosphericLux * posSinElev,
        )
        val horizonScatter = if (moonElevDeg >= 0.0) {
            TWILIGHT_HORIZON_SCATTER_RATIO * exoAtmosphericLux * exp(-0.18 * moonElevDeg) * amElevationModifier
        } else {
            TWILIGHT_HORIZON_SCATTER_RATIO * exoAtmosphericLux * exp(0.8 * moonElevDeg) * amElevationModifier
        }
        val diffuseLux = skyDiffuse + horizonScatter
        val globalLux = directHorizontalLux + diffuseLux

        return LunarIlluminanceComponents(
            directLux = directLux,
            diffuseLux = diffuseLux,
            globalLux = globalLux,
        )
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
     * Direct moonlight is physically ~4100K at zenith, yet appears cool silvery-blue (~7000K–8000K)
     * to dark-adapted eyes because rods peak near 498 nm. Near the horizon, Rayleigh scattering
     * removes blue-green wavelengths so rising/setting Moons retain their warm amber/orange hue.
     */
    fun computePerceivedColorTempKelvin(
        physicalColorTempKelvin: Double,
        illuminanceLux: Double,
    ): Double {
        if (physicalColorTempKelvin.isNaN() || illuminanceLux <= 0.0) return Double.NaN

        // Convert horizontal illuminance (lx) to typical landscape luminance (cd/m², 18% reflectance).
        // Use a smooth Hill/sigmoid rod-dominance curve across the mesopic range (half-saturation at
        // 0.16 cd/m², the geometric midpoint of CIE 191's [0.005, 5.0] cd/m² bounds) so there are
        // no hard-clamp corners or artificial plateaus on bright Full Moon nights.
        val landscapeLuminance = illuminanceLux * (0.18 / PI)
        val rodFactor = 1.0 / (1.0 + (landscapeLuminance / 0.16).pow(0.75))

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

        // Wrap signed phase angle into (-180°, 180°]: positive = waxing (New -> Full), negative = waning (Full -> New)
        var wrappedSignedPhase = signedPhaseAngleDeg % 360.0
        if (wrappedSignedPhase > 180.0) wrappedSignedPhase -= 360.0
        if (wrappedSignedPhase <= -180.0) wrappedSignedPhase += 360.0
        val isWaxing = wrappedSignedPhase > 0.0

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

        val amElevationModifier = exp(-coordinates.altitude / ATMOSPHERE_SCALE_HEIGHT)
        var actualAirMass = 0.0
        var shadowRatio = 0.0
        var colorTempKelvin = Double.NaN
        var perceivedColorTempKelvin = Double.NaN

        val isAboveHorizon = position.altitude > coordinates.moonApparentHorizonAlt
        if (isAboveHorizon) {
            actualAirMass = computeAirMass(position.altitude, amElevationModifier)
        }

        val illuminance = computeIlluminanceComponents(
            moonElevDeg = position.altitude,
            horizonAltDeg = coordinates.moonApparentHorizonAlt,
            alphaDeg = alphaDeg,
            distanceKm = position.distanceKm,
            actualAirMass = actualAirMass,
            amElevationModifier = amElevationModifier,
        )

        if (isAboveHorizon) {
            shadowRatio = computeShadowRatio(
                geometricAltDeg = position.altitude,
                horizonAltDeg = coordinates.moonApparentHorizonAlt,
                semiDiameterDeg = MOON_SEMI_DIAMETER_DEG,
            )
            colorTempKelvin = computeColorTempKelvin(actualAirMass)
            perceivedColorTempKelvin = computePerceivedColorTempKelvin(colorTempKelvin, illuminance.globalLux)
        }

        return LunarMetricsResult(
            illuminationPercent = round(illuminationPercent * 10) / 10.0,
            isWaxing = isWaxing,
            ageDays = round(ageDays * 100) / 100.0,
            phase = phase,
            distanceKm = round(position.distanceKm),
            angularDiameterDegrees = round(angularDiameterDegrees * 1000) / 1000.0,
            isSupermoon = isSupermoon,
            illuminanceLux = illuminance.globalLux,
            directIlluminanceLux = illuminance.directLux,
            globalIlluminanceLux = illuminance.globalLux,
            diffuseIlluminanceLux = illuminance.diffuseLux,
            shadowRatio = shadowRatio,
            airMass = actualAirMass,
            colorTempKelvin = colorTempKelvin,
            perceivedColorTempKelvin = perceivedColorTempKelvin,
        )
    }

    /**
     * Single-pass batch metrics calculator for 1,441-point daily lunar charts (mirrors [SunMetrics]).
     * [outIlluminance] receives Global Horizontal lunar illuminance (default).
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
        outDirectIlluminance: FloatArray? = null,
        outDiffuseIlluminance: FloatArray? = null,
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
            val frac = hoursCalc[i] / 24.0
            val alphaDeg = foldPhaseAngleToAlpha(signedPhase0 + frac * deltaPhase)

            if (moonElevDeg <= horizonAltDeg) {
                outAirMass[i] = 0f
                outShadowRatio[i] = 0f
                outColorTemp[i] = Float.NaN

                val illuminance = computeIlluminanceComponents(
                    moonElevDeg = moonElevDeg,
                    horizonAltDeg = horizonAltDeg,
                    alphaDeg = alphaDeg,
                    distanceKm = distancesKm[i],
                    actualAirMass = 0.0,
                    amElevationModifier = amElevationModifier,
                )
                outIlluminance[i] = illuminance.globalLux.toFloat()
                outDirectIlluminance?.set(i, 0f)
                outDiffuseIlluminance?.set(i, illuminance.diffuseLux.toFloat())
                continue
            }

            val actualAirMass = computeAirMass(moonElevDeg, amElevationModifier)
            outAirMass[i] = actualAirMass.toFloat()

            val illuminance = computeIlluminanceComponents(
                moonElevDeg = moonElevDeg,
                horizonAltDeg = horizonAltDeg,
                alphaDeg = alphaDeg,
                distanceKm = distancesKm[i],
                actualAirMass = actualAirMass,
                amElevationModifier = amElevationModifier,
            )
            outIlluminance[i] = illuminance.globalLux.toFloat()
            outDirectIlluminance?.set(i, illuminance.directLux.toFloat())
            outDiffuseIlluminance?.set(i, illuminance.diffuseLux.toFloat())

            outShadowRatio[i] = computeShadowRatio(
                geometricAltDeg = moonElevDeg,
                horizonAltDeg = horizonAltDeg,
                semiDiameterDeg = MOON_SEMI_DIAMETER_DEG,
            ).toFloat()

            val physicalColorTemp = computeColorTempKelvin(actualAirMass)
            outColorTemp[i] = computePerceivedColorTempKelvin(physicalColorTemp, illuminance.globalLux).toFloat()
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
