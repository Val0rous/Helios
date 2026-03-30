package com.ephemeris.helios.ui.composables.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.CustomColorScheme
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.ui.theme.MaterialColors

@Composable
fun NightEntry(
    duration: String = "",
    midnightTime: String = "",
    midnightAzimuth: Double,
    midnightAltitude: Double
) {
    val nightColor = LocalCustomColors.current.nightPrimary
    HeaderEntry(text = stringResource(R.string.night_length_midnight), color = nightColor)
    TextEntry(text = duration, icon = R.drawable.ic_timelapse, desc = "Night Length", iconTint = nightColor)
    TextEntry(text = midnightTime, textVariant = "@ $midnightAzimuth°", icon = R.drawable.ic_pace, desc = "Time of Solar Midnight", iconTint = nightColor)
    TextEntry(text = "$midnightAltitude°", icon = R.drawable.ic_brightness_empty, desc = "Altitude at Solar Midnight", iconTint = nightColor)
}