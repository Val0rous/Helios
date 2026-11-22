package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ephemeris.helios.ui.theme.LocalCustomColors
import kotlin.math.log10

/**
 * Full-bleed wavy illuminance fill using a smooth base-10 logarithmic scale
 * (`log10(value + 1) / log10(maxValue + 1)`).
 * - Strictly 0% fill (no waves) at `<= 0`.
 * - Completely filled tile (waves disappear) at `>= maxLux`.
 * - Waves visible across the entire positive range (`0 < value < maxLux`), including sub-1
 *   twilight lux and lunar millilux.
 */
@Composable
fun IlluminancePill(
    currentIlluminance: Float,
    modifier: Modifier = Modifier,
    maxLux: Float = 100_000f,
    fillColor: Color = LocalCustomColors.current.luxWave,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val fraction = when {
        currentIlluminance <= 0f -> 0f
        currentIlluminance >= maxLux -> 1f
        else -> {
            val maxLog = log10(maxLux.coerceAtLeast(1f) + 1f)
            (log10(currentIlluminance + 1f) / maxLog).coerceIn(0.01f, 0.99f)
        }
    }

    WavyMetricFill(
        fraction = fraction,
        fillColor = fillColor,
        modifier = modifier,
        content = content,
    )
}
