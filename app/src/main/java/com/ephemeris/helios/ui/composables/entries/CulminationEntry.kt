package com.ephemeris.helios.ui.composables.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.R
import com.ephemeris.helios.utils.EphemerisType

@Composable
fun CulminationEntry(
    time: String = "",
    azimuth: Double,
    altitude: Double,
    type: EphemerisType = EphemerisType.PLANETS
) {
    // Todo: make altitude turn red if sun is always below horizon
    HeaderEntry(text = stringResource(when (type) {
        EphemerisType.SUN -> R.string.solar_noon
        EphemerisType.MOON -> R.string.lunar_culmination
        else -> R.string.culmination
    }))
    TextEntry(text = time, textVariant = "@ $azimuth°", icon = R.drawable.ic_pace, desc = stringResource(when (type) {
        EphemerisType.SUN -> R.string.solar_noon_time
        EphemerisType.MOON -> R.string.lunar_culmination_time
        else -> R.string.culmination_time
    }))
    TextEntry(text = "$altitude°", textVariant = "", icon = R.drawable.ic_brightness_7, desc = stringResource(when(type) {
        EphemerisType.SUN -> R.string.solar_noon_altitude
        EphemerisType.MOON -> R.string.lunar_culmination_altitude
        else -> R.string.culmination_altitude
    }))
}

@Composable
fun SolarNoonEntry(
    noonTime: String = "",
    noonAzimuth: Double,
    noonAltitude: Double
) {
    CulminationEntry(
        time = noonTime,
        azimuth = noonAzimuth,
        altitude = noonAltitude,
        type = EphemerisType.SUN
    )
}

@Composable
fun LunarCulminationEntry(
    culminationTime: String = "",
    culminationAzimuth: Double,
    culminationAltitude: Double
) {
    CulminationEntry(
        time = culminationTime,
        azimuth = culminationAzimuth,
        altitude = culminationAltitude,
        type = EphemerisType.MOON
    )
}