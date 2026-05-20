package com.ephemeris.helios.utils.calc

import com.mapbox.maps.extension.style.expressions.dsl.generated.distance
import kotlin.math.*

object SunMetrics {
    private const val SOLAR_CONSTANT = 1361.0
    private const val LUMINOUS_EFFICACY = 105.0
    private const val DEFAULT_OZONE_DU = 300.0
    private const val ATMOSPHERE_SCALE_HEIGHT = 8434.0
    private const val DIRECT_BEAM_IRRADIANCE_CONSTANT = 1
    private const val GLOBAL_HORIZONTAL_IRRADIANCE_CONSTANT = 1.125 // +10-15%
    // Todo: let users choose their preferred irradiance calc, default is 1 (direct) for solar panels and 1.125 for photography/cinematic purposes

    data class SunMetricsResult(
        val irradiance: Double,
        val uvIntensity: Double,
        val luminance: Double,
        val shadowRatio: Double,
        val airMass: Double,
        val colorTemp: Double,
        val distanceKm: Double,
        val distanceAu: Double,
        val solarSpeed: Double
    )

    fun calculateMetrics(
        sunElevationDeg: Double,
        observerAltitudeMeters: Double,
        distanceAu: Double = 1.0,   // Default to 1 for safety
        ozoneDU: Double = DEFAULT_OZONE_DU,
        solarSpeed: Double = 0.0
    ): SunMetricsResult {
        val outIrradiance = FloatArray(1)
        val outUvi = FloatArray(1)
        val outIlluminance = FloatArray(1)
        val outShadowRatio = FloatArray(1)
        val outAirMass = FloatArray(1)
        val outColorTemp = FloatArray(1)

        calculateMetrics(
            sunElevationsDeg = doubleArrayOf(sunElevationDeg),
            observerAltitudeMeters = observerAltitudeMeters,
            distancesAu = doubleArrayOf(distanceAu),
            ozoneDU = ozoneDU,
            outIrradiance = outIrradiance,
            outUvi = outUvi,
            outIlluminance = outIlluminance,
            outShadowRatio = outShadowRatio,
            outAirMass = outAirMass,
            outColorTemp = outColorTemp
        )

        // 1 AU is exactly 149,597,870.7 kilometers
        val distanceKm = distanceAu * 149597870.7

        return SunMetricsResult(
            irradiance = outIrradiance[0].toDouble(),
            uvIntensity = outUvi[0].toDouble(),
            luminance = outIlluminance[0].toDouble(),
            shadowRatio = outShadowRatio[0].toDouble(),
            airMass = outAirMass[0].toDouble(),
            colorTemp = outColorTemp[0].toDouble(),
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
     * @param outIrradiance Pre-allocated array to store Irradiance (W/m^2).
     * @param outIlluminance Pre-allocated array to store Illuminance (lux).
     * @param outUvi Pre-allocated array to store UV Index.
     * @param outShadowRatio Pre-allocated array to store Shadow Ratios.
     */
    fun calculateMetrics(
        sunElevationsDeg: DoubleArray,
        observerAltitudeMeters: Double,
        distancesAu: DoubleArray,
        ozoneDU: Double = DEFAULT_OZONE_DU,
//        outSunElevations: FloatArray,
        outIrradiance: FloatArray,
        outUvi: FloatArray,
        outIlluminance: FloatArray,
        outShadowRatio: FloatArray,
        outAirMass: FloatArray,
        outColorTemp: FloatArray
    ) {
        val size = sunElevationsDeg.size

        // Hoist constant calculations outside the loop
        // 1. UV Modifiers
        val uvOzoneModifier = DEFAULT_OZONE_DU / ozoneDU
        val uvAltitudeModifier = 1.0 + (observerAltitudeMeters / 10000.0)
        val combinedUvModifier = uvOzoneModifier * uvAltitudeModifier

        // 2. Air Mass Elevation Modifier
        // e^(-h / 8434)
        val amElevationModifier = exp(-observerAltitudeMeters / ATMOSPHERE_SCALE_HEIGHT)

        for (i in 0 until size) {
            val sunElevDeg = sunElevationsDeg[i]

//            // 1. Instantly store the Float version for your UI
//            outSunElevations[i] = sunElevDeg.toFloat()

            // If the sun is below the horizon, all light metrics are strictly 0.
            if (sunElevDeg <= -0.833) {
                outAirMass[i] = 0f // 0f (instead of old 1f) aligns it to chart minimum
                outIrradiance[i] = 0f
                outIlluminance[i] = 0f
                outUvi[i] = 0f
                outShadowRatio[i] = 0f // 0 represents infinite/no shadow
                outColorTemp[i] = 2000f // Sun is down, no direct beam temp. 2000f aligns it to chart minimum
//                // Color Temperature Piecewise Logic
//                if (sunElevDeg >= -6.0) {
//                    // Sun is in Civil Twilight (Blue Hour)
//                    outColorTemp[i] = 10000.0 - 8000.0 * exp(sunElevDeg / 2.0)
//                } else {
//                    // Deep night. NaN stops the chart line from dropping to 0.
//                    outColorTemp[i] = Float.NaN
//                }
                continue
            }

            // --- Orbital Power Modifier ---
            // Inverse-Square Law: Intensity = 1 / R^2
            val distanceAu = distancesAu[i]
            val distanceModifier = 1.0 / (distanceAu * distanceAu)

            val sunElevRad = Math.toRadians(sunElevDeg)
            val sinElev = sin(sunElevRad)

            // 1. Air Mass (Kasten-Young)
            val amDenominator = sinElev + 0.50572 * (sunElevDeg + 6.07995).pow(-1.6364)
            val relativeAirMass = 1.0 / amDenominator
            val actualAirMass = relativeAirMass * amElevationModifier
            outAirMass[i] = actualAirMass.toFloat()

            // 2. Irradiance (Depends on Air Mass) (Scaled by distance)
            val irradiance = (SOLAR_CONSTANT * distanceModifier) * 0.7.pow(actualAirMass) * sinElev
            outIrradiance[i] = irradiance.toFloat()

            // 3. Illuminance (Depends on Irradiance)
            outIlluminance[i] = (irradiance * LUMINOUS_EFFICACY).toFloat()

            // 4. UV Index (Scaled by distance)
            // Note: max(0.0, sinAlt) prevents NaN errors if floating point inaccuracies dip below 0
            val baseUv = (12.5 * distanceModifier) * max(0.0, sinElev).pow(2.42)
            outUvi[i] = (baseUv * combinedUvModifier).toFloat()

            // 5. Shadow Ratio (Cotangent)
            // We use 1.0 / tan(x) instead of a custom cotangent function to save a method call
            outShadowRatio[i] = (1.0 / tan(sunElevRad)).toFloat()

            // 6. Color Temperature (Kelvin)
            // No clamp! Allowing Air Mass to drop below 1.0 at altitude mathematically
            // pushes the temperature naturally from 5500K (Sea Level) towards 5800K (Space).
            outColorTemp[i] = (2000.0 + 3500.0 * exp(-0.1 * (actualAirMass - 1.0))).toFloat()
        }
    }
}