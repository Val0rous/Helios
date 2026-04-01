package com.ephemeris.helios.ui.composables.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.R
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.EphemerisType

@Composable
fun RiseSetEntry(
    riseTime: String = "",
    setTime: String = "",
    riseAzimuth: Double?,
    setAzimuth: Double?,
    type: EphemerisType = EphemerisType.PLANETS
) {
    HeaderEntry(text = stringResource(when (type) {
        EphemerisType.SUN -> R.string.sunrise_sunset
        EphemerisType.MOON -> R.string.moonrise_moonset
        else -> R.string.rise_set
    }))
    // Todo: implement always above/always below behavior
    TextEntry(
        text = riseTime,
        textVariant = if (riseAzimuth != null) "@ $riseAzimuth°" else "",
        icon = when (type) {
            EphemerisType.SUN -> R.drawable.ic_wb_sunny_filled
            EphemerisType.MOON -> R.drawable.ic_bedtime_filled
            else -> R.drawable.ic_arrow_upward
        },
        desc = stringResource(when (type) {
            EphemerisType.SUN -> R.string.sunrise
            EphemerisType.MOON -> R.string.moonrise
            else -> R.string.rise
        })
    )
    TextEntry(
        text = setTime,
        textVariant = if (setAzimuth != null) "@ $setAzimuth°" else "",
        icon = when (type) {
            EphemerisType.SUN -> R.drawable.ic_wb_twilight_filled
            EphemerisType.MOON -> R.drawable.ic_bedtime_off
            else -> R.drawable.ic_arrow_downward
        },
        desc = stringResource(when (type) {
            EphemerisType.SUN -> R.string.sunset
            EphemerisType.MOON -> R.string.moonset
            else -> R.string.set
        })
    )
}

@Composable
fun SunriseSunsetEntry(
    sunriseTime: String = "",
    sunsetTime: String = "",
    sunriseAzimuth: Double?,
    sunsetAzimuth: Double?
) {
    RiseSetEntry(
        riseTime = sunriseTime,
        setTime = sunsetTime,
        riseAzimuth = sunriseAzimuth,
        setAzimuth = sunsetAzimuth,
        type = EphemerisType.SUN
    )
}

@Composable
fun MoonriseMoonsetEntry(
    moonriseTime: String = "",
    moonsetTime: String = "",
    moonriseAzimuth: Double?,
    moonsetAzimuth: Double?
) {
    RiseSetEntry(
        riseTime = moonriseTime,
        setTime = moonsetTime,
        riseAzimuth = moonriseAzimuth,
        setAzimuth = moonsetAzimuth,
        type = EphemerisType.MOON
    )
}