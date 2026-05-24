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
    val timezoneId: String? = null
) {
    // The raw angular dip of the horizon caused by observer altitude
    val horizonDipDeg: Double
        get() {
            return 0.0
//            val elevation = max(0.0, this.altitude)
//            return 0.032 * sqrt(elevation)
        }

    // The dynamic solar horizon (Sunrise/Sunset)
    val sunApparentHorizonAlt: Double
        get() = SolarEphemeris.ALT_SUNRISE_SUNSET - horizonDipDeg

    // The dynamic lunar horizon (Moonrise/Moonset)
    val moonApparentHorizonAlt: Double
        get() = LunarEphemeris.ALT_MOONRISE_MOONSET - horizonDipDeg
}