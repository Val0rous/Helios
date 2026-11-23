package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.datastore.ColorTempMode
import com.ephemeris.helios.utils.datastore.IlluminanceMode
import com.ephemeris.helios.utils.datastore.IrradianceMode
import kotlin.math.*

object SunMetrics {
    private const val SOLAR_CONSTANT = 1361.0
    private const val DIRECT_LUMINOUS_EFFICACY = 105.0
    private const val DIFFUSE_LUMINOUS_EFFICACY = 115.0
    private const val NAUTICAL_TWILIGHT_DEG = -12.0
    private const val ASTRONOMICAL_TWILIGHT_DEG = -18.0
    private const val DEFAULT_OZONE_DU = 300.0
    private const val MIN_OZONE_DU = 80.0
    private const val ATMOSPHERE_SCALE_HEIGHT = 8434.0
    private const val SUN_SEMI_DIAMETER_DEG = 0.266

    // Softplus transition scale for diffuse sky scattering across the horizon:
    // gives C∞ continuity between twilight exponential sky glow (~3.9 lux at -6°, ~320 lux at sunrise,
    // ~570 lux at 0°) and daytime Liu-Jordan diffuse irradiance with zero kink at 0°.
    private const val DIFFUSE_SOFTPLUS_SCALE = 0.0195

    // Exact Kasten-Young air mass value & derivative at 0° for C1-smooth sub-zero continuation
    private val AIR_MASS_AT_ZERO = 1.0 / (0.50572 * 6.07995.pow(-1.6364))
    private const val AIR_MASS_SLOPE_AT_ZERO = 14.891

    // KNMI TEMIS v2.x (Allaart et al. 2004 / 2017) clear-sky erythemal UV parameterization constants
    private const val TEMIS_S = 2.0877
    private const val TEMIS_TAU = 1.0597
    private const val TEMIS_EPSILON = 0.2755
    private const val TEMIS_F = 0.0477
    private const val TEMIS_G = 1.6325
    private const val TEMIS_H = 5.6499
    private const val TEMIS_J = 0.0485
    private const val UVI_PER_WATT_PER_SQM = 40.0
    // Exact logarithmic slope d(ln UVW)/d(elevDeg) of TEMIS at 0°+ (~0.22244 / deg) for C1-smooth twilight decay
    private val TEMIS_TWILIGHT_DECAY_PER_DEG =
        (1.0 / TEMIS_EPSILON + TEMIS_TAU / (TEMIS_EPSILON * TEMIS_EPSILON)) *
            (1.0 - TEMIS_EPSILON) * (PI / 180.0)
    private val TEMIS_HORIZON_BASE = TEMIS_S * TEMIS_EPSILON * exp(-TEMIS_TAU / TEMIS_EPSILON)
    // Non-ozone clear-sky Rayleigh + boundary-layer aerosol UV altitude gain (~5.4%/km near sea level,
    // tapering with barometric pressure; combines with tropospheric ozone removal for ~6.5–7%/km total)
    private const val UV_ALTITUDE_PRESSURE_GAIN = 0.48
    private const val MIN_UV_ALTITUDE_MODIFIER = 0.85

    data class SunMetricsResult(
        /** Direct Normal Irradiance (DNI, W/m²) — default irradiance metric. */
        val irradiance: Double,
        /** Global Horizontal Irradiance (GHI, W/m²): direct horizontal beam + diffuse sky. */
        val globalIrradiance: Double = irradiance,
        /** Diffuse Horizontal Irradiance (DHI, W/m²): scattered ambient sky radiation. */
        val diffuseIrradiance: Double = 0.0,
        val uvIntensity: Double,
        /** Global Horizontal Illuminance (lux) — default illuminance metric (direct horizontal + ambient sky). */
        val luminance: Double,
        /** Direct beam illuminance (lux). */
        val directIlluminance: Double = luminance,
        /** Global Horizontal Illuminance (lux): alias for [luminance]. */
        val globalIlluminance: Double = luminance,
        /** Diffuse / Ambient sky illuminance (lux): scattered skylight (including twilight). */
        val diffuseIlluminance: Double = 0.0,
        val shadowRatio: Double,
        val airMass: Double,
        /** Direct beam solar color temperature (K) — default color temperature metric. */
        val colorTemp: Double,
        /** Global Horizontal daylight color temperature (K): mired-weighted blend of direct + sky. */
        val globalColorTemp: Double = colorTemp,
        /** Diffuse / Ambient open-shade skylight color temperature (K), including blue hour/twilight. */
        val diffuseColorTemp: Double = Double.NaN,
        val distanceKm: Double,
        val distanceAu: Double,
        val solarSpeed: Double
    ) {
        fun irradianceFor(mode: IrradianceMode): Double = when (mode) {
            IrradianceMode.DIRECT -> irradiance
            IrradianceMode.GLOBAL -> globalIrradiance
            IrradianceMode.DIFFUSE -> diffuseIrradiance
        }

        fun illuminanceFor(mode: IlluminanceMode): Double = when (mode) {
            IlluminanceMode.GLOBAL -> globalIlluminance
            IlluminanceMode.DIRECT -> directIlluminance
            IlluminanceMode.DIFFUSE -> diffuseIlluminance
        }

        fun colorTempFor(mode: ColorTempMode): Double = when (mode) {
            ColorTempMode.DIRECT -> colorTemp
            ColorTempMode.GLOBAL -> globalColorTemp
            ColorTempMode.DIFFUSE -> diffuseColorTemp
        }
    }

    private fun smoothstep(t: Double): Double {
        val x = t.coerceIn(0.0, 1.0)
        return x * x * (3.0 - (2.0 * x))
    }

    /**
     * C∞-smooth softplus replacement for `max(0, sin(elev))` in clear-sky diffuse sky scattering.
     * Matches exponential twilight sky decay below the horizon and converges exponentially to exact
     * `sin(elev)` above +5° with zero kink at 0°.
     */
    internal fun smoothDiffuseSinElev(sinElev: Double): Double {
        val scaled = sinElev / DIFFUSE_SOFTPLUS_SCALE
        return when {
            scaled > 20.0 -> sinElev
            scaled < -30.0 -> 0.0
            else -> DIFFUSE_SOFTPLUS_SCALE * ln1p(exp(scaled))
        }
    }

    /**
     * Smooth altitude attenuation factor for diffuse sky scattering: applies barometric pressure
     * scaling in twilight (thinner high-altitude air scatters less twilight glow) and converges smoothly
     * to 1.0 in daytime (where Liu-Jordan already accounts for altitude via higher direct beam transmittance).
     */
    internal fun smoothTwilightAltitudeScale(sinElev: Double, amElevationModifier: Double): Double {
        if (amElevationModifier == 1.0) return 1.0
        val scaled = sinElev / DIFFUSE_SOFTPLUS_SCALE
        val twilightWeight = when {
            scaled > 20.0 -> 0.0
            scaled < -20.0 -> 1.0
            else -> 1.0 / (1.0 + exp(scaled))
        }
        return 1.0 + (amElevationModifier - 1.0) * twilightWeight
    }

    private fun computeDiffuseColorTempKelvin(
        sunElevDeg: Double,
        smoothSinElev: Double,
        amElevationModifier: Double,
    ): Double {
        if (sunElevDeg <= NAUTICAL_TWILIGHT_DEG) return Double.NaN
        // Clear-sky Rayleigh skylight (~8,500 K near horizon -> ~11,500 K at zenith) plus
        // Chappuis ozone absorption blue-hour bump peaking around -4.5° (~13,000 K).
        val baseSkyK = 8500.0 + (3000.0 * smoothSinElev.pow(0.6))
        val blueHourShift = 4500.0 * exp(-((sunElevDeg + 4.5) / 3.5).pow(2))
        val altitudeClarityFactor = 1.0 + 0.12 * (1.0 - amElevationModifier)
        return (baseSkyK + blueHourShift) * altitudeClarityFactor
    }

    fun calculateMetrics(
        sunElevationDeg: Double,
        observerAltitudeMeters: Double,
        distanceAu: Double = 1.0,   // Default to 1 for safety
        ozoneDU: Double = DEFAULT_OZONE_DU,
        solarSpeed: Double = 0.0,
        horizonAltDeg: Double = -0.833
    ): SunMetricsResult {
        val outIrradiance = FloatArray(1)
        val outGlobalIrradiance = FloatArray(1)
        val outDiffuseIrradiance = FloatArray(1)
        val outUvi = FloatArray(1)
        val outIlluminance = FloatArray(1)
        val outDirectIlluminance = FloatArray(1)
        val outDiffuseIlluminance = FloatArray(1)
        val outShadowRatio = FloatArray(1)
        val outAirMass = FloatArray(1)
        val outColorTemp = FloatArray(1)
        val outGlobalColorTemp = FloatArray(1)
        val outDiffuseColorTemp = FloatArray(1)

        calculateMetrics(
            sunElevationsDeg = doubleArrayOf(sunElevationDeg),
            observerAltitudeMeters = observerAltitudeMeters,
            distancesAu = doubleArrayOf(distanceAu),
            ozoneDU = ozoneDU,
            horizonAltDeg = horizonAltDeg,
            outIrradiance = outIrradiance,
            outUvi = outUvi,
            outIlluminance = outIlluminance,
            outShadowRatio = outShadowRatio,
            outAirMass = outAirMass,
            outColorTemp = outColorTemp,
            outGlobalIrradiance = outGlobalIrradiance,
            outDiffuseIrradiance = outDiffuseIrradiance,
            outDirectIlluminance = outDirectIlluminance,
            outDiffuseIlluminance = outDiffuseIlluminance,
            outGlobalColorTemp = outGlobalColorTemp,
            outDiffuseColorTemp = outDiffuseColorTemp,
        )

        // 1 AU is exactly 149,597,870.7 kilometers
        val distanceKm = distanceAu * 149597870.7

        return SunMetricsResult(
            irradiance = outIrradiance[0].toDouble(),
            globalIrradiance = outGlobalIrradiance[0].toDouble(),
            diffuseIrradiance = outDiffuseIrradiance[0].toDouble(),
            uvIntensity = outUvi[0].toDouble(),
            luminance = outIlluminance[0].toDouble(),
            directIlluminance = outDirectIlluminance[0].toDouble(),
            globalIlluminance = outIlluminance[0].toDouble(),
            diffuseIlluminance = outDiffuseIlluminance[0].toDouble(),
            shadowRatio = outShadowRatio[0].toDouble(),
            airMass = outAirMass[0].toDouble(),
            colorTemp = outColorTemp[0].toDouble(),
            globalColorTemp = outGlobalColorTemp[0].toDouble(),
            diffuseColorTemp = outDiffuseColorTemp[0].toDouble(),
            distanceKm = distanceKm,
            distanceAu = distanceAu,
            solarSpeed = solarSpeed
        )
    }

    private fun computeTemisUvi(
        sunElevDeg: Double,
        posSinElev: Double,
        invOzoneThousand: Double,
        temisConstantTerm: Double,
        distanceModifier: Double,
        uvAltitudeModifier: Double,
    ): Double {
        if (sunElevDeg <= NAUTICAL_TWILIGHT_DEG) return 0.0

        val uvw = if (sunElevDeg >= 0.0) {
            val muX = (1.0 - TEMIS_EPSILON) * posSinElev + TEMIS_EPSILON
            val xOzone = invOzoneThousand * posSinElev
            val temisBracket = if (xOzone > 0.0) {
                TEMIS_F * xOzone.pow(TEMIS_G) + temisConstantTerm
            } else {
                temisConstantTerm
            }
            TEMIS_S * muX * exp(-TEMIS_TAU / muX) * temisBracket
        } else {
            // C1-smooth diffuse twilight UV decay matching TEMIS's exact value & derivative at 0°,
            // tapering cleanly to 0 at nautical twilight (-12°) with zero corner at 0° or sunrise.
            val twilightTaper = 1.0 - smoothstep(-sunElevDeg / -NAUTICAL_TWILIGHT_DEG)
            TEMIS_HORIZON_BASE * temisConstantTerm * exp(TEMIS_TWILIGHT_DECAY_PER_DEG * sunElevDeg) * twilightTaper
        }
        return UVI_PER_WATT_PER_SQM * uvw * distanceModifier * uvAltitudeModifier
    }

    /**
     * Computes clear-sky erythemal UV Index using the operational KNMI TEMIS v2.x parameterization
     * (Allaart et al., 2004 / 2017) with C1-smooth civil-twilight diffuse UV decay and a
     * barometrically tapered non-ozone altitude modifier.
     */
    fun calculateClearSkyUvi(
        sunElevationDeg: Double,
        observerAltitudeMeters: Double,
        distanceAu: Double = 1.0,
        ozoneDU: Double = DEFAULT_OZONE_DU,
        @Suppress("UNUSED_PARAMETER") horizonAltDeg: Double = -0.833,
    ): Double {
        if (sunElevationDeg <= NAUTICAL_TWILIGHT_DEG) return 0.0
        val safeOzone = ozoneDU.coerceAtLeast(MIN_OZONE_DU)
        val amElevationModifier = exp(-observerAltitudeMeters / ATMOSPHERE_SCALE_HEIGHT)
        val uvAltitudeModifier = (1.0 + UV_ALTITUDE_PRESSURE_GAIN * (1.0 - amElevationModifier))
            .coerceAtLeast(MIN_UV_ALTITUDE_MODIFIER)
        val distanceModifier = 1.0 / (distanceAu * distanceAu)
        val posSinElev = max(0.0, sin(Math.toRadians(sunElevationDeg)))
        return computeTemisUvi(
            sunElevDeg = sunElevationDeg,
            posSinElev = posSinElev,
            invOzoneThousand = 1000.0 / safeOzone,
            temisConstantTerm = TEMIS_H / safeOzone + TEMIS_J,
            distanceModifier = distanceModifier,
            uvAltitudeModifier = uvAltitudeModifier,
        )
    }

    /**
     * Calculates all metrics in a single pass to maximize CPU cache efficiency.
     * @param sunElevationsDeg Input array of sun altitudes in degrees.
     * @param observerAltitudeMeters Physical elevation of the observer in meters.
     * @param ozoneDU Ozone thickness in Dobson Units.
     * @param outAirMass Pre-allocated array to store Air Mass results.
     * @param outIrradiance Pre-allocated array to store Direct Normal Irradiance (W/m^2).
     * @param outIlluminance Pre-allocated array to store Global Horizontal Illuminance (lux).
     * @param outUvi Pre-allocated array to store UV Index.
     * @param outShadowRatio Pre-allocated array to store Shadow Ratios.
     * @param outGlobalIrradiance Optional pre-allocated array for Global Horizontal Irradiance (W/m^2).
     * @param outDiffuseIrradiance Optional pre-allocated array for Diffuse Horizontal Irradiance (W/m^2).
     * @param outDirectIlluminance Optional pre-allocated array for Direct beam Illuminance (lux).
     * @param outDiffuseIlluminance Optional pre-allocated array for Diffuse/Ambient Illuminance (lux).
     * @param outGlobalColorTemp Optional pre-allocated array for Global Horizontal daylight Color Temp (K).
     * @param outDiffuseColorTemp Optional pre-allocated array for Diffuse/Ambient skylight Color Temp (K).
     */
    fun calculateMetrics(
        sunElevationsDeg: DoubleArray,
        observerAltitudeMeters: Double,
        distancesAu: DoubleArray,
        ozoneDU: Double = DEFAULT_OZONE_DU,
        horizonAltDeg: Double = -0.833,
        outIrradiance: FloatArray,
        outUvi: FloatArray,
        outIlluminance: FloatArray,
        outShadowRatio: FloatArray,
        outAirMass: FloatArray,
        outColorTemp: FloatArray,
        outGlobalIrradiance: FloatArray? = null,
        outDiffuseIrradiance: FloatArray? = null,
        outDirectIlluminance: FloatArray? = null,
        outDiffuseIlluminance: FloatArray? = null,
        outGlobalColorTemp: FloatArray? = null,
        outDiffuseColorTemp: FloatArray? = null,
    ) {
        val size = sunElevationsDeg.size

        // Hoist constant calculations outside the loop
        // 1. Air Mass Elevation Modifier: e^(-h / 8434)
        val amElevationModifier = exp(-observerAltitudeMeters / ATMOSPHERE_SCALE_HEIGHT)

        // 2. KNMI TEMIS v2.x UV Modifiers (pressure-tapered altitude scaling, no double-counting with tropospheric O3)
        val safeOzone = ozoneDU.coerceAtLeast(MIN_OZONE_DU)
        val invOzoneThousand = 1000.0 / safeOzone
        val temisConstantTerm = TEMIS_H / safeOzone + TEMIS_J
        val uvAltitudeModifier = (1.0 + UV_ALTITUDE_PRESSURE_GAIN * (1.0 - amElevationModifier))
            .coerceAtLeast(MIN_UV_ALTITUDE_MODIFIER)

        for (i in 0 until size) {
            val sunElevDeg = sunElevationsDeg[i]
            val distanceAu = distancesAu[i]
            val distanceModifier = 1.0 / (distanceAu * distanceAu)
            val extraterrestrialIrradiance = SOLAR_CONSTANT * distanceModifier
            val sunElevRad = Math.toRadians(sunElevDeg)
            val sinElev = sin(sunElevRad)
            val posSinElev = max(0.0, sinElev)
            val smoothSinElev = smoothDiffuseSinElev(sinElev)

            // If the sun is below the apparent horizon, all direct-beam metrics are strictly 0.
            // Ambient sky scatter (Diffuse, Global, & twilight UV) decays smoothly through twilight.
            if (sunElevDeg <= horizonAltDeg) {
                outAirMass[i] = 0f
                outIrradiance[i] = 0f
                outShadowRatio[i] = 0f
                outColorTemp[i] = Float.NaN

                outUvi[i] = computeTemisUvi(
                    sunElevDeg = sunElevDeg,
                    posSinElev = 0.0,
                    invOzoneThousand = invOzoneThousand,
                    temisConstantTerm = temisConstantTerm,
                    distanceModifier = distanceModifier,
                    uvAltitudeModifier = uvAltitudeModifier,
                ).toFloat()

                val twilightDhi = if (sunElevDeg > ASTRONOMICAL_TWILIGHT_DEG) {
                    val altScale = smoothTwilightAltitudeScale(sinElev, amElevationModifier)
                    0.271 * extraterrestrialIrradiance * smoothSinElev * altScale
                } else {
                    0.0
                }
                val twilightLux = twilightDhi * DIFFUSE_LUMINOUS_EFFICACY
                val twilightColorTemp = computeDiffuseColorTempKelvin(
                    sunElevDeg = sunElevDeg,
                    smoothSinElev = smoothSinElev,
                    amElevationModifier = amElevationModifier,
                ).toFloat()

                outIlluminance[i] = twilightLux.toFloat()
                outDirectIlluminance?.set(i, 0f)
                outDiffuseIrradiance?.set(i, twilightDhi.toFloat())
                outGlobalIrradiance?.set(i, twilightDhi.toFloat())
                outDiffuseIlluminance?.set(i, twilightLux.toFloat())
                outDiffuseColorTemp?.set(i, twilightColorTemp)
                outGlobalColorTemp?.set(i, twilightColorTemp)
                continue
            }

            // 1. Air Mass (Kasten-Young with C1-smooth Sub-Zero Continuation)
            val relativeAirMass = if (sunElevDeg >= 0.0) {
                val amDenominator = sinElev + 0.50572 * (sunElevDeg + 6.07995).pow(-1.6364)
                1.0 / amDenominator
            } else {
                val absElev = abs(sunElevDeg)
                AIR_MASS_AT_ZERO + (AIR_MASS_SLOPE_AT_ZERO * absElev) + (5.0 * absElev * absElev)
            }
            val actualAirMass = relativeAirMass * amElevationModifier
            outAirMass[i] = actualAirMass.toFloat()

            // 2. Irradiance (Direct Normal, Diffuse Horizontal, Global Horizontal)
            val directIrradiance = extraterrestrialIrradiance * 0.7.pow(actualAirMass)
            val directHorizontalIrradiance = directIrradiance * posSinElev

            // Unified C∞-smooth Liu-Jordan clear-sky + twilight diffuse model (zero kink at 0°)
            val altScale = smoothTwilightAltitudeScale(sinElev, amElevationModifier)
            val diffuseIrradiance = max(
                0.0,
                (0.271 * extraterrestrialIrradiance - 0.2939 * directIrradiance) * smoothSinElev * altScale,
            )
            val globalIrradiance = directHorizontalIrradiance + diffuseIrradiance

            outIrradiance[i] = directIrradiance.toFloat()
            outDiffuseIrradiance?.set(i, diffuseIrradiance.toFloat())
            outGlobalIrradiance?.set(i, globalIrradiance.toFloat())

            // 3. Illuminance (Global Horizontal default, Direct Beam, Diffuse/Ambient Sky)
            val directIlluminance = directIrradiance * DIRECT_LUMINOUS_EFFICACY
            val directHorizontalIlluminance = directHorizontalIrradiance * DIRECT_LUMINOUS_EFFICACY
            val diffuseIlluminance = diffuseIrradiance * DIFFUSE_LUMINOUS_EFFICACY
            val globalIlluminance = directHorizontalIlluminance + diffuseIlluminance

            outIlluminance[i] = globalIlluminance.toFloat()
            outDirectIlluminance?.set(i, directIlluminance.toFloat())
            outDiffuseIlluminance?.set(i, diffuseIlluminance.toFloat())

            // 4. UV Index (KNMI TEMIS v2.x clear-sky erythemal model with C1-smooth twilight decay)
            outUvi[i] = computeTemisUvi(
                sunElevDeg = sunElevDeg,
                posSinElev = posSinElev,
                invOzoneThousand = invOzoneThousand,
                temisConstantTerm = temisConstantTerm,
                distanceModifier = distanceModifier,
                uvAltitudeModifier = uvAltitudeModifier,
            ).toFloat()

            // 5. Shadow Ratio (Refracted apparent elevation with smooth partial-disk rise/set)
            outShadowRatio[i] = computeShadowRatio(
                geometricAltDeg = sunElevDeg,
                horizonAltDeg = horizonAltDeg,
                semiDiameterDeg = SUN_SEMI_DIAMETER_DEG,
            ).toFloat()

            // 6. Color Temperature (Direct Beam, Diffuse/Ambient Skylight, and Mired-Weighted Global Daylight)
            val directColorTemp = 1800.0 + 3700.0 * exp(-0.1 * (actualAirMass - 1.0))
            val diffuseColorTemp = computeDiffuseColorTempKelvin(
                sunElevDeg = sunElevDeg,
                smoothSinElev = smoothSinElev,
                amElevationModifier = amElevationModifier,
            )
            val globalColorTemp = if (globalIlluminance > 0.0 && !diffuseColorTemp.isNaN()) {
                val directWeight = directHorizontalIlluminance / globalIlluminance
                val diffuseWeight = diffuseIlluminance / globalIlluminance
                val mixedMired = directWeight * (1_000_000.0 / directColorTemp) +
                    diffuseWeight * (1_000_000.0 / diffuseColorTemp)
                1_000_000.0 / mixedMired
            } else {
                directColorTemp
            }

            outColorTemp[i] = directColorTemp.toFloat()
            outDiffuseColorTemp?.set(i, diffuseColorTemp.toFloat())
            outGlobalColorTemp?.set(i, globalColorTemp.toFloat())
        }
    }
}
