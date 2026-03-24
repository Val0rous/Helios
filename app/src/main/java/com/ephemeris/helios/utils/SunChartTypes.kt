package com.ephemeris.helios.utils

import com.ephemeris.helios.R

enum class SunChartTypes(val label: String, val icon: Int, val filledIcon: Int) {
    ELEVATION("Elevation", R.drawable.ic_sunny, R.drawable.ic_sunny_filled), // Altitude
    IRRADIANCE("Irradiance", R.drawable.ic_bolt, R.drawable.ic_bolt_filled), // Energy
    UV_INTENSITY("UV Intensity", R.drawable.ic_beach_access, R.drawable.ic_beach_access_filled), // Intensity, UV
    TRAJECTORY("Trajectory", R.drawable.ic_explore, R.drawable.ic_explore_filled), // Path
    ILLUMINANCE("Illuminance", R.drawable.ic_lightbulb, R.drawable.ic_lightbulb_filled), // Lux
    SHADOWS("Shadows", R.drawable.ic_ev_shadow, R.drawable.ic_ev_shadow_filled),
    AIR_MASS("Air Mass", R.drawable.ic_foggy, R.drawable.ic_foggy_filled), // Atmosphere
}