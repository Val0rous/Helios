package com.ephemeris.helios.utils

import com.ephemeris.helios.R

enum class SunChartTypes(val label: Int, val icon: Int, val filledIcon: Int) {
    ELEVATION(R.string.elevation, R.drawable.ic_sunny, R.drawable.ic_sunny_filled), // Altitude
    IRRADIANCE(R.string.irradiance, R.drawable.ic_bolt, R.drawable.ic_bolt_filled), // Energy
    UV_INTENSITY(R.string.uv_intensity, R.drawable.ic_beach_access, R.drawable.ic_beach_access_filled), // Intensity, UV
    TRAJECTORY(R.string.trajectory, R.drawable.ic_explore, R.drawable.ic_explore_filled), // Path
    ILLUMINANCE(R.string.illuminance, R.drawable.ic_lightbulb, R.drawable.ic_lightbulb_filled), // Lux
    SHADOWS(R.string.shadows, R.drawable.ic_ev_shadow, R.drawable.ic_ev_shadow_filled),
    AIR_MASS(R.string.air_mass, R.drawable.ic_foggy, R.drawable.ic_foggy_filled), // Atmosphere
}