package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ephemeris.helios.ui.theme.LocalCustomColors

/**
 * Full-bleed wavy air mass fill linearized via `1 - 1 / AM` (`1.0` zenith → `0%`, horizon → `~100%`).
 * Empty (`0%`) at night (`< 0.1 AM`).
 */
@Composable
fun AirMassPill(
    currentAirMass: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = LocalCustomColors.current.amWave,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val isNight = currentAirMass < 0.1f
    val fraction = if (isNight) {
        0f
    } else {
        val safeAM = currentAirMass.coerceAtLeast(1f)
        (1f - (1f / safeAM)).coerceIn(0f, 1f)
    }

    WavyMetricFill(
        fraction = fraction,
        fillColor = fillColor,
        modifier = modifier,
        content = content,
    )
}
