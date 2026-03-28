package com.ephemeris.helios.utils

import kotlin.math.*

object SunMetrics {
    private const val SOLAR_CONSTANT = 1361.0
    private const val LUMINOUS_EFFICACY = 105.0
    private const val DEFAULT_OZONE_DU = 300.0

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
        outAirMass: FloatArray
    ) {
        val size = sunElevationsDeg.size
        // Hoist constant calculations outside the loop
        // Pre-calculate the UV modifiers that don't change per-angle
        val uvOzoneModifier = DEFAULT_OZONE_DU / ozoneDU
        val uvAltitudeModifier = 1.0 + (observerAltitudeMeters / 10000.0)
        val combinedUvModifier = uvOzoneModifier * uvAltitudeModifier

        for (i in 0 until size) {
            val sunElevDeg = sunElevationsDeg[i]

//            // 1. Instantly store the Float version for your UI
//            outSunElevations[i] = sunElevDeg.toFloat()

            // If the sun is below the horizon, all light metrics are strictly 0.
            if (sunElevDeg <= 0.0) {
                outAirMass[i] = 0f
                outIrradiance[i] = 0f
                outIlluminance[i] = 0f
                outUvi[i] = 0f
                outShadowRatio[i] = -1f // -1 represents infinite/no shadow
                continue
            }

            val sunElevRad = Math.toRadians(sunElevDeg)
            val sinElev = sin(sunElevRad)

            // 1. Air Mass (Kasten-Young)
            val amDenominator = sinElev + 0.50572 * (sunElevDeg + 6.07995).pow(-1.6364)
            val airMass = 1.0 / amDenominator
            outAirMass[i] = airMass.toFloat()

            // 2. Irradiance (Depends on Air Mass)
            val irradiance = SOLAR_CONSTANT * 0.7.pow(airMass) * sinElev
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
        }
    }
}