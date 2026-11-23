package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ephemeris.helios.ui.theme.LocalCustomColors

import kotlin.math.ln

/**
 * Full-bleed wavy air mass fill:
 * - Empty (0% fill, no waves) at night (`< 0.1 AM`) or at high altitude around zenith (`<= 0.60 AM`, e.g. ~4,200m+ like Mauna Kea).
 * - Modest baseline fill (~11%) at sea-level zenith (`1.0 AM`) representing the full standard atmosphere overhead.
 * - Completely filled tile (waves disappear) at extreme daylight horizon limit (`>= 50 AM`, sunset/sunrise).
 * - Smooth logarithmic progression (`0.60` to `50.0 AM`) across the entire daylight arc.
 */
@Composable
fun AirMassPill(
    currentAirMass: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = LocalCustomColors.current.amWave,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val isNight = currentAirMass < 0.1f
    val fraction = when {
        isNight || (currentAirMass <= 0.60f) -> 0f
        currentAirMass >= 50f -> 1f
        else -> {
            val minAm = 0.60
            val maxAm = 50.0
            (ln(currentAirMass.toDouble() / minAm) / ln(maxAm / minAm)).toFloat().coerceIn(0f, 1f)
        }
    }

    WavyMetricFill(
        fraction = fraction,
        fillColor = fillColor,
        modifier = modifier,
        content = content,
    )
}
