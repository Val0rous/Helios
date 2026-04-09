package com.ephemeris.helios.utils

import com.ephemeris.helios.R
import java.io.Serializable

interface ChartStrategy {
    val minX: Float get() = 0f
    val maxX: Float get() = 24f
    val minY: Float get() = 0f
    val maxY: Float get() = 90f
    val isLogScale: Boolean get() = false
    val zeroValue: Float get() = 0f

    fun getDynamicMaxY(yValues: FloatArray): Float = maxY
}

sealed class Charts(val label: Int, val icon: Int, val filledIcon: Int) : Serializable {
    // This ensures that when an 'object' is deserialized,
    // it returns the existing Singleton instance instead of creating a new one.
    protected fun readResolve(): Any = when (this) {
        // Sun Charts
        is Sun.Daily.Elevation -> Sun.Daily.Elevation
        is Sun.Daily.Irradiance -> Sun.Daily.Irradiance
        is Sun.Daily.UvIntensity -> Sun.Daily.UvIntensity
        is Sun.Daily.Trajectory -> Sun.Daily.Trajectory
        is Sun.Daily.Illuminance -> Sun.Daily.Illuminance
        is Sun.Daily.Shadows -> Sun.Daily.Shadows
        is Sun.Daily.ColorTemperature -> Sun.Daily.ColorTemperature
        is Sun.Daily.AirMass -> Sun.Daily.AirMass

        // Moon Charts
        is Moon.Daily.Elevation -> Moon.Daily.Elevation
        is Moon.Daily.Trajectory -> Moon.Daily.Trajectory
        is Moon.Daily.Illuminance -> Moon.Daily.Illuminance
        is Moon.Daily.Shadows -> Moon.Daily.Shadows
        is Moon.Daily.ColorTemperature -> Moon.Daily.ColorTemperature
        is Moon.Daily.AirMass -> Moon.Daily.AirMass

        // Combo Charts
        is SunMoonCombo.Daily.Elevation -> SunMoonCombo.Daily.Elevation
        is SunMoonCombo.Daily.Trajectory -> SunMoonCombo.Daily.Trajectory

        // Planet Charts
        is Venus.Daily.Elevation -> Venus.Daily.Elevation
        is Mars.Daily.Elevation -> Mars.Daily.Elevation
        is Jupiter.Daily.Elevation -> Jupiter.Daily.Elevation
        is Saturn.Daily.Elevation -> Saturn.Daily.Elevation
        is Mercury.Daily.Elevation -> Mercury.Daily.Elevation
    }

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
            object Elevation : Daily(R.string.elevation, R.drawable.ic_nightlight, R.drawable.ic_nightlight_filled) // Altitude
            object Trajectory : Daily(R.string.trajectory, R.drawable.ic_explore, R.drawable.ic_explore_filled) // Azimuth
            object Illuminance : Daily(R.string.illuminance, R.drawable.ic_lightbulb, R.drawable.ic_lightbulb_filled)
            object Shadows : Daily(R.string.shadows, R.drawable.ic_ev_shadow, R.drawable.ic_ev_shadow_filled)
            object ColorTemperature : Daily(R.string.color_temperature, R.drawable.ic_thermometer, R.drawable.ic_thermometer_filled)
            object AirMass : Daily(R.string.air_mass, R.drawable.ic_foggy, R.drawable.ic_foggy_filled)

            companion object {
                val entries get() = listOf(Elevation, Trajectory, Illuminance, Shadows, ColorTemperature, AirMass)
            }
        }
    }

    sealed class SunMoonCombo(label: Int, icon: Int, filledIcon: Int) : Charts(label, icon, filledIcon) {
        sealed class Daily(label: Int, icon: Int, filledIcon: Int) : SunMoonCombo(label, icon, filledIcon) {
            object Elevation : Daily(R.string.elevation, R.drawable.ic_routine, R.drawable.ic_routine_filled)   // Altitude
            object Trajectory : Daily(R.string.trajectory, R.drawable.ic_explore, R.drawable.ic_explore_filled) // Azimuth

            companion object {
                val entries get() = listOf(Elevation, Trajectory)
            }
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
