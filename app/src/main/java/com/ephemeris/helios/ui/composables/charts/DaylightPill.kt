package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ephemeris.helios.ui.theme.LocalCustomColors
import kotlin.math.roundToInt

/**
 * Full-bleed wavy fill container representing the fraction of a calendar day a celestial body
 * spends above the apparent horizon (`0h` -> empty `0f`, `24h` -> completely full `1f`).
 *
 * Uses [WavyMetricFill] so wave amplitude gradually emerges from the bottom edge and flattens
 * smoothly at the top edge, matching [IlluminancePill] and [AirMassPill].
 */
@Composable
fun DaylightPill(
    durationHours: Double,
    modifier: Modifier = Modifier,
    totalDayHours: Double = 24.0,
    isLunar: Boolean = false,
    fillColor: Color = if (isLunar) LocalCustomColors.current.coolWave else LocalCustomColors.current.luxWave,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val safeTotal = totalDayHours.coerceAtLeast(1e-6)
    val totalDaySeconds = (safeTotal * 3600.0).roundToInt()
    val durationSeconds = if (durationHours.isNaN()) 0 else (durationHours * 3600.0).roundToInt()
    val fraction = when {
        durationHours.isNaN() || durationHours <= 0.0 || durationSeconds <= 0 -> 0f
        durationHours >= safeTotal || durationSeconds >= totalDaySeconds -> 1f
        else -> (durationHours / safeTotal).toFloat().coerceIn(0f, 1f)
    }

    WavyMetricFill(
        fraction = fraction,
        fillColor = fillColor,
        modifier = modifier,
        content = content,
    )
}
