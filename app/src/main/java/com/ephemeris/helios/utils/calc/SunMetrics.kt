package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.datastore.ColorTempMode
import com.ephemeris.helios.utils.datastore.IlluminanceMode
import com.ephemeris.helios.utils.datastore.IrradianceMode
import kotlin.math.*

object SunMetrics {
    private const val SOLAR_CONSTANT = 1361.0
    private const val DIRECT_LUMINOUS_EFFICACY = 105.0
    private const val DIFFUSE_LUMINOUS_EFFICACY = 115.0
    private const val TWILIGHT_HORIZON_DHI = 3.5 // Clear-sky horizontal diffuse irradiance at 0° (~400 lux)
    private const val NAUTICAL_TWILIGHT_DEG = -12.0
    private const val ASTRONOMICAL_TWILIGHT_DEG = -18.0
    private const val DEFAULT_OZONE_DU = 300.0
    private const val ATMOSPHERE_SCALE_HEIGHT = 8434.0
    private const val SUN_SEMI_DIAMETER_DEG = 0.266

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

    private fun computeDiffuseColorTempKelvin(
        sunElevDeg: Double,
        posSinElev: Double,
        amElevationModifier: Double,
    ): Double {
        if (sunElevDeg <= NAUTICAL_TWILIGHT_DEG) return Double.NaN
        // Clear-sky Rayleigh skylight (~8,500 K near horizon -> ~11,500 K at zenith) plus
        // Chappuis ozone absorption blue-hour bump peaking around -4.5° (~13,000 K).
        val baseSkyK = 8500.0 + 3000.0 * posSinElev.pow(0.6)
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
        // 1. UV Modifiers
        val uvOzoneModifier = DEFAULT_OZONE_DU / ozoneDU
        val uvAltitudeModifier = 1.0 + (observerAltitudeMeters / 10000.0)
        val combinedUvModifier = uvOzoneModifier * uvAltitudeModifier

        // 2. Air Mass Elevation Modifier: e^(-h / 8434)
        val amElevationModifier = exp(-observerAltitudeMeters / ATMOSPHERE_SCALE_HEIGHT)

        for (i in 0 until size) {
            val sunElevDeg = sunElevationsDeg[i]
            val distanceAu = distancesAu[i]
            val distanceModifier = 1.0 / (distanceAu * distanceAu)

            // If the sun is below the apparent horizon, all direct-beam metrics are strictly 0.
            // Ambient sky scatter (Diffuse & Global) still decays smoothly through twilight down to -18°.
            if (sunElevDeg <= horizonAltDeg) {
                outAirMass[i] = 0f
                outIrradiance[i] = 0f
                outUvi[i] = 0f
                outShadowRatio[i] = 0f
                outColorTemp[i] = Float.NaN

                val twilightDhi = if (sunElevDeg > ASTRONOMICAL_TWILIGHT_DEG) {
                    TWILIGHT_HORIZON_DHI * distanceModifier * exp(0.8 * sunElevDeg) * amElevationModifier
                } else {
                    0.0
                }
                val twilightLux = twilightDhi * DIFFUSE_LUMINOUS_EFFICACY
                val twilightColorTemp = computeDiffuseColorTempKelvin(
                    sunElevDeg = sunElevDeg,
                    posSinElev = 0.0,
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

            val sunElevRad = Math.toRadians(sunElevDeg)
            val sinElev = sin(sunElevRad)
            val posSinElev = max(0.0, sinElev)

            // 1. Air Mass (Kasten-Young with Sub-Zero Projection)
            val relativeAirMass = if (sunElevDeg >= 0.0) {
                val amDenominator = sinElev + 0.50572 * (sunElevDeg + 6.07995).pow(-1.6364)
                1.0 / amDenominator
            } else {
                val amZero = 1.0 / (0.50572 * 6.07995.pow(-1.6364)) // Exact value at 0.0° (~37.92)
                val absElev = abs(sunElevDeg)
                amZero + (10.0 * absElev) + (5.0 * absElev.pow(2))
            }
            val actualAirMass = relativeAirMass * amElevationModifier
            outAirMass[i] = actualAirMass.toFloat()

            // 2. Irradiance (Direct Normal, Diffuse Horizontal, Global Horizontal)
            val extraterrestrialIrradiance = SOLAR_CONSTANT * distanceModifier
            val directIrradiance = extraterrestrialIrradiance * 0.7.pow(actualAirMass)
            val directHorizontalIrradiance = directIrradiance * posSinElev

            // Liu-Jordan clear-sky diffuse model + continuous twilight horizon term
            val dayDiffuse = max(
                0.0,
                (0.271 * extraterrestrialIrradiance - 0.2939 * directIrradiance) * posSinElev,
            )
            val twilightComponent = if (sunElevDeg >= 0.0) {
                TWILIGHT_HORIZON_DHI * distanceModifier * exp(-0.18 * sunElevDeg) * amElevationModifier
            } else {
                TWILIGHT_HORIZON_DHI * distanceModifier * exp(0.8 * sunElevDeg) * amElevationModifier
            }
            val diffuseIrradiance = dayDiffuse + twilightComponent
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

            // 4. UV Index (Scaled by distance)
            val baseUv = (12.5 * distanceModifier) * posSinElev.pow(2.42)
            outUvi[i] = (baseUv * combinedUvModifier).toFloat()

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
                posSinElev = posSinElev,
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
