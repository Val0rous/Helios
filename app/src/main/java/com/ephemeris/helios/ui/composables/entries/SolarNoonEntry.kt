package com.ephemeris.helios.ui.composables.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.R

@Composable
fun SolarNoonEntry(
    noonTime: String = "",
    noonAzimuth: Double,
    noonAltitude: Double
) {
    // Todo: make altitude turn red if sun is always below horizon
    HeaderEntry(text = stringResource(R.string.solar_noon))
    TextEntry(text = noonTime, textVariant = "@ $noonAzimuth°", icon = R.drawable.ic_pace, desc = "Time of Solar Noon")
    TextEntry(text = "$noonAltitude°", textVariant = "", icon = R.drawable.ic_brightness_7, desc = "Altitude at Solar Noon")
}