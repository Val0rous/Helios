package com.ephemeris.helios.utils.location

import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import kotlin.math.max
import kotlin.math.sqrt

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val locationName: String? = null,
    val timezoneId: String? = null,
    val useHorizonDip: Boolean = false,
    val useInstantaneousSemidiameter: Boolean = false,
) {
    // The raw angular dip of the horizon caused by observer altitude (controlled by AppSettings.useHorizonDip)
    val horizonDipDeg: Double
        get() {
            if (!useHorizonDip) return 0.0
            val elevation = max(0.0, this.altitude)
            return 0.032 * sqrt(elevation)
        }

    // The dynamic solar horizon (Sunrise/Sunset) using fixed mean solar semidiameter
    val sunApparentHorizonAlt: Double
        get() = SolarEphemeris.ALT_SUNRISE_SUNSET - horizonDipDeg

    // Solar apparent horizon with optional instantaneous distance-based semidiameter
    fun sunApparentHorizonAlt(distanceAu: Double): Double = if (useInstantaneousSemidiameter) {
        -(SolarEphemeris.REFRACTION_HORIZON_DEG + SolarEphemeris.calculateSemiDiameterDeg(distanceAu)) - horizonDipDeg
    } else {
        sunApparentHorizonAlt
    }

    // The dynamic lunar horizon (Moonrise/Moonset) using fixed mean lunar semidiameter
    val moonApparentHorizonAlt: Double
        get() = LunarEphemeris.ALT_MOONRISE_MOONSET - horizonDipDeg

    // Lunar apparent horizon with optional instantaneous distance-based semidiameter
    fun moonApparentHorizonAlt(distanceKm: Double): Double = if (useInstantaneousSemidiameter) {
        -(LunarEphemeris.REFRACTION_HORIZON_DEG + LunarEphemeris.calculateSemiDiameterDeg(distanceKm)) - horizonDipDeg
    } else {
        moonApparentHorizonAlt
    }
}