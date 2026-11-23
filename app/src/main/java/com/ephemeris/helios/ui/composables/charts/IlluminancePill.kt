package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ephemeris.helios.ui.theme.LocalCustomColors
import kotlin.math.log10
import kotlin.math.pow

/**
 * Full-bleed wavy illuminance fill using a human-perception logarithmic scale starting at a
 * visual visibility threshold [minLux]:
 * - Strictly 0% fill (no lingering bottom waves) at `<= minLux` (too dark for the human eye,
 *   e.g. deep astronomical twilight / starlight floor).
 * - Completely filled tile (waves disappear) at `>= maxLux`.
 * - Smooth perceptual progression (`(log10(v / minLux) / log10(maxLux / minLux))^1.4`) across
 *   the visible range so deep nautical twilight still shows a gentle wave while bright daylight
 *   or Full Moon fills the tile naturally.
 */
@Composable
fun IlluminancePill(
    currentIlluminance: Float,
    modifier: Modifier = Modifier,
    minLux: Float = 0.01f,
    maxLux: Float = 100_000f,
    fillColor: Color = LocalCustomColors.current.luxWave,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val safeMin = minLux.coerceAtLeast(1e-6f)
    val fraction = when {
        currentIlluminance <= safeMin -> 0f
        currentIlluminance >= maxLux -> 1f
        else -> {
            val logRange = log10(maxLux / safeMin)
            if (logRange <= 0f) {
                0f
            } else {
                val normalized = (log10(currentIlluminance / safeMin) / logRange).coerceIn(0f, 1f)
                normalized.pow(1.4f).coerceIn(0f, 1f)
            }
        }
    }

    WavyMetricFill(
        fraction = fraction,
        fillColor = fillColor,
        modifier = modifier,
        content = content,
    )
}
