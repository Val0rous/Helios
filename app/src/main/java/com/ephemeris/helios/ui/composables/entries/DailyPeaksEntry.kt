package com.ephemeris.helios.ui.composables.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.R
import com.ephemeris.helios.utils.formatNumber
import com.ephemeris.helios.utils.round
import com.ephemeris.helios.utils.roundToSignificant
import kotlin.math.roundToInt

@Composable
fun DailyPeaksEntry(
    irradiance: Double,
    uvIntensity: Double,
    luminance: Double,
    shadowRatio: Double
) {
    HeaderEntry(text = stringResource(R.string.daily_peaks))
    TextEntry(text = "${irradiance.roundToSignificant()}", textVariant = "W/m²", icon = R.drawable.ic_bolt, desc = "Max Irradiance")
    TextEntry(
        text = "UVI ${uvIntensity.round(1)}",
        textVariant = "${(25 * uvIntensity).roundToInt()} mW/m²",
        icon = R.drawable.ic_beach_access,
        desc = "Max UV Index"
    )
    TextEntry(text = formatNumber(luminance.roundToSignificant()), textVariant = "lx", icon = R.drawable.ic_lightbulb, desc = "Max Luminance")
    TextEntry(text = "${shadowRatio.roundToSignificant()} : 1", icon = R.drawable.ic_ev_shadow, desc = "Min Shadow Ratio")
}