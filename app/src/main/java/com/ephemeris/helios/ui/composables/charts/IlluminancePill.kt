package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ephemeris.helios.ui.theme.LocalCustomColors
import kotlin.math.log10

/**
 * Full-bleed wavy illuminance fill using a base-10 logarithmic scale (`1..100,000 lux` → `0..1`).
 * Empty (`0%`) at night (`< 0.001 lux`).
 */
@Composable
fun IlluminancePill(
    currentIlluminance: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = LocalCustomColors.current.luxWave,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val isNight = currentIlluminance < 0.001f
    val fraction = if (isNight) {
        0f
    } else {
        val safeLux = currentIlluminance.coerceAtLeast(1f)
        (log10(safeLux) / 5f).coerceIn(0f, 1f)
    }

    WavyMetricFill(
        fraction = fraction,
        fillColor = fillColor,
        modifier = modifier,
        content = content,
    )
}
