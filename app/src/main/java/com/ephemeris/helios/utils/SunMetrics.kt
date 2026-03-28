package com.ephemeris.helios.utils

import kotlin.Float.Companion.NaN
import kotlin.math.*

object SunMetrics {
    private const val SOLAR_CONSTANT = 1361.0
    private const val LUMINOUS_EFFICACY = 105.0
    private const val DEFAULT_OZONE_DU = 300.0
    private const val ATMOSPHERE_SCALE_HEIGHT = 8434.0
    private const val DIRECT_BEAM_IRRADIANCE_CONSTANT = 1
    private const val GLOBAL_HORIZONTAL_IRRADIANCE_CONSTANT = 1.125 // +10-15%
    // Todo: let users choose their preferred irradiance calc, default is 1 (direct) for solar panels and 1.125 for photography/cinematic purposes
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
            if (sunElevDeg <= 0.0) {
                outAirMass[i] = 1f // 1f aligns it to chart minimum
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

            val sunElevRad = Math.toRadians(sunElevDeg)
            val sinElev = sin(sunElevRad)

            // 1. Air Mass (Kasten-Young)
            val amDenominator = sinElev + 0.50572 * (sunElevDeg + 6.07995).pow(-1.6364)
            val relativeAirMass = 1.0 / amDenominator
            val actualAirMass = relativeAirMass * amElevationModifier
            outAirMass[i] = actualAirMass.toFloat()

            // 2. Irradiance (Depends on Air Mass)
            val irradiance = SOLAR_CONSTANT * 0.7.pow(actualAirMass) * sinElev
            outIrradiance[i] = irradiance.toFloat()

            // 3. Illuminance (Depends on Irradiance)
            outIlluminance[i] = (irradiance * LUMINOUS_EFFICACY).toFloat()

            // 4. UV Index
            // Note: max(0.0, sinAlt) prevents NaN errors if floating point inaccuracies dip below 0
            val baseUv = 12.5 * max(0.0, sinElev).pow(2.42)
            outUvi[i] = (baseUv * combinedUvModifier).toFloat()

            // 5. Shadow Ratio (Cotangent)
            // We use 1.0 / tan(x) instead of a custom cotangent function to save a method call
            outShadowRatio[i] = (1.0 / tan(sunElevRad)).toFloat()

            // 6. Color Temperature (Kelvin)
            // Clamp actualAirMass to a minimum of 1.0 to prevent mathematical anomalies
            // if high-altitude calculations push AM slightly below 1
            val safeAirMass = max(1.0, actualAirMass)
            outColorTemp[i] = (2000.0 + 3500.0 * exp(-0.1 * (safeAirMass - 1.0))).toFloat()
        }
    }
}