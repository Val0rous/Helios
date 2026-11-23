package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ephemeris.helios.ui.theme.LocalCustomColors
import kotlin.math.abs

private const val MAX_SOLAR_DECLINATION_DEG = 23.436f

/**
 * Full-bleed directional wavy declination fill:
 * - Positive declination (`0° -> +23.44°`, North): warm orange waves rising from bottom to top,
 *   filling the tile completely at the June Solstice.
 * - Zero (`0°`, Equinoxes): completely clear surface.
 * - Negative declination (`0° -> -23.44°`, South): cool blue waves descending from top to bottom,
 *   filling the tile completely at the December Solstice.
 */
@Composable
fun DeclinationPill(
    declination: Float,
    modifier: Modifier = Modifier,
    northColor: Color = LocalCustomColors.current.warmWave,
    southColor: Color = LocalCustomColors.current.coolWave,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val isSouth = declination < 0f
    val fraction = (abs(declination) / MAX_SOLAR_DECLINATION_DEG).coerceIn(0f, 1f)
    val fillColor = if (isSouth) southColor else northColor

    WavyMetricFill(
        fraction = fraction,
        fillColor = fillColor,
        fromTop = isSouth,
        modifier = modifier,
        content = content,
    )
}
