package com.ephemeris.helios.ui.composables.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.R
import com.ephemeris.helios.utils.formatNumber
import com.ephemeris.helios.utils.roundToSignificant

@Composable
fun LiveMetricsEntry(
    irradiance: Double,
    uvIntensity: Double,
    luminance: Double,
    shadowRatio: Double
) {
    HeaderEntry(text = stringResource(R.string.live_metrics))
    TextEntry(text = "${irradiance.roundToSignificant()}", textVariant = "W/m²", icon = R.drawable.ic_bolt_filled, desc = "Current Irradiance")
    TextEntry(text = "UVI ${uvIntensity.roundToSignificant(2)}", textVariant = "250 mW/m²", icon = R.drawable.ic_beach_access_filled, desc = "Current UV Index")
    TextEntry(text = formatNumber(luminance.roundToSignificant()), textVariant = "Lux", icon = R.drawable.ic_lightbulb_filled, desc = "Current Luminance")
    TextEntry(text = "${shadowRatio.roundToSignificant()} : 1", icon = R.drawable.ic_ev_shadow_filled, desc = "Current Shadow Ratio")
}