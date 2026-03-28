package com.ephemeris.helios.ui.composables.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.R

@Composable
fun SunriseSunsetEntry(
    sunriseTime: String = "",
    sunsetTime: String = "",
    sunriseAzimuth: Double?,
    sunsetAzimuth: Double?
) {
    HeaderEntry(text = stringResource(R.string.sunrise_sunset))
    // Todo: implement always above/always below behavior
    TextEntry(
        text = sunriseTime,
        textVariant = if (sunriseAzimuth != null) "@ $sunriseAzimuth°" else "",
        icon = R.drawable.ic_wb_sunny_filled,
        desc = "Sunrise"
    )
    TextEntry(
        text = sunsetTime,
        textVariant = if (sunsetAzimuth != null) "@ $sunsetAzimuth°" else "",
        icon = R.drawable.ic_wb_twilight_filled,
        desc = "Sunset"
    )
}