package com.ephemeris.helios.utils

import com.ephemeris.helios.R
import java.io.Serializable

sealed class Charts(val label: Int, val icon: Int, val filledIcon: Int) : Serializable {
    sealed class Sun(label: Int, icon: Int, filledIcon: Int) : Charts(label, icon, filledIcon) {
        sealed class Daily(label: Int, icon: Int, filledIcon: Int) : Sun(label, icon, filledIcon) {
            object Elevation: Daily(R.string.elevation, R.drawable.ic_sunny, R.drawable.ic_sunny_filled) // Altitude
            object Irradiance: Daily(R.string.irradiance, R.drawable.ic_bolt, R.drawable.ic_bolt_filled) // Energy
            object UvIntensity: Daily(R.string.uv_intensity, R.drawable.ic_beach_access, R.drawable.ic_beach_access_filled) // Intensity, UV
            object Trajectory: Daily(R.string.trajectory, R.drawable.ic_explore, R.drawable.ic_explore_filled) // Path
            object Illuminance: Daily(R.string.illuminance, R.drawable.ic_lightbulb, R.drawable.ic_lightbulb_filled) // Lux
            object Shadows: Daily(R.string.shadows, R.drawable.ic_ev_shadow, R.drawable.ic_ev_shadow_filled)
            object ColorTemperature: Daily(R.string.color_temperature, R.drawable.ic_thermometer, R.drawable.ic_thermometer_filled) // Kelvin
            object AirMass: Daily(R.string.air_mass, R.drawable.ic_foggy, R.drawable.ic_foggy_filled) // Atmosphere

            companion object {
                val entries get() = listOf(Elevation, Irradiance, UvIntensity, Trajectory, Illuminance, Shadows, ColorTemperature, AirMass)
            }
        }
    }

    sealed class Moon(label: Int, icon: Int, filledIcon: Int) : Charts(label, icon, filledIcon) {
        sealed class Daily(label: Int, icon: Int, filledIcon: Int) : Moon(label, icon, filledIcon) {
            object Elevation : Daily(R.string.elevation, R.drawable.ic_moon_stars, R.drawable.ic_moon_stars_filled) // Altitude
        }
    }

    sealed class Venus(label: Int, icon: Int, filledIcon: Int) : Charts(label, icon, filledIcon) {
        sealed class Daily(label: Int, icon: Int, filledIcon: Int) : Venus(label, icon, filledIcon) {
            object Elevation : Daily(R.string.elevation, R.drawable.ic_circle, R.drawable.ic_circle_filled) // Altitude
        }
    }

    sealed class Mars(label: Int, icon: Int, filledIcon: Int) : Charts(label, icon, filledIcon) {
        sealed class Daily(label: Int, icon: Int, filledIcon: Int) : Mars(label, icon, filledIcon) {
            object Elevation : Daily(R.string.elevation, R.drawable.ic_circle, R.drawable.ic_circle_filled) // Altitude
        }
    }

    sealed class Jupiter(label: Int, icon: Int, filledIcon: Int) : Charts(label, icon, filledIcon) {
        sealed class Daily(label: Int, icon: Int, filledIcon: Int) : Jupiter(label, icon, filledIcon) {
            object Elevation : Daily(R.string.elevation, R.drawable.ic_circle, R.drawable.ic_circle_filled) // Altitude
        }
    }

    sealed class Saturn(label: Int, icon: Int, filledIcon: Int) : Charts(label, icon, filledIcon) {
        sealed class Daily(label: Int, icon: Int, filledIcon: Int) : Saturn(label, icon, filledIcon) {
            object Elevation : Daily(R.string.elevation, R.drawable.ic_circle, R.drawable.ic_circle_filled) // Altitude
        }
    }

    sealed class Mercury(label: Int, icon: Int, filledIcon: Int) : Charts(label, icon, filledIcon) {
        sealed class Daily(label: Int, icon: Int, filledIcon: Int) : Mercury(label, icon, filledIcon) {
            object Elevation: Daily(R.string.elevation, R.drawable.ic_circle, R.drawable.ic_circle_filled) // Altitude
        }
    }
}
