package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.ChartAlphas
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.calc.MoonMetrics
import com.ephemeris.helios.utils.location.Coordinates
import kotlin.math.abs

/**
 * Visual parameters describing the Moon's illuminated disc as seen by the observer.
 *
 * @property illuminationFraction Illuminated fraction of the lunar disc in `[0f, 1f]`
 *   (`0f` = New Moon, `0.5f` = Quarter, `1f` = Full Moon).
 * @property isWaxing `true` from New Moon to Full Moon, `false` from Full Moon to New Moon.
 * @property isSouthernHemisphere Flips the illuminated limb horizontally for Southern Hemisphere observers
 *   (Northern: waxing = right lit, waning = left lit; Southern: waxing = left lit, waning = right lit).
 */
@Immutable
data class MoonPhaseAppearance(
    val illuminationFraction: Float,
    val isWaxing: Boolean,
    val isSouthernHemisphere: Boolean = false,
) {
    companion object {
        val Full = MoonPhaseAppearance(illuminationFraction = 1f, isWaxing = true)

        fun from(
            metrics: MoonMetrics.LunarMetricsResult,
            coordinates: Coordinates? = null,
        ): MoonPhaseAppearance = MoonPhaseAppearance(
            illuminationFraction = (metrics.illuminationPercent / 100.0).toFloat().coerceIn(0f, 1f),
            isWaxing = metrics.isWaxing,
            isSouthernHemisphere = (coordinates?.latitude ?: 0.0) < 0.0,
        )
    }
}

/**
 * Draws an astronomically accurate Moon phase disc centered in `[0, iconSize] × [0, iconSize]`.
 *
 * - **New Moon (`0%`)**: empty circle with circular border.
 * - **Full Moon (`100%`)**: fully filled circle with circular border.
 * - **Intermediate phases (`0%..100%`)**: orthographic semi-elliptical terminator whose filled area
 *   equals `illuminationFraction` of the disc area, oriented to the lit limb for waxing vs. waning
 *   (and observer hemisphere).
 */
fun DrawScope.drawMoonPhaseIcon(
    iconSize: Float,
    phase: MoonPhaseAppearance,
    fillColor: Color,
    borderColor: Color,
    backgroundColor: Color = Color.Transparent,
    isDimmed: Boolean = false,
    reusablePath: Path = Path(),
    outerRadiusRatio: Float = 10f / 24f,
    strokeWidthPx: Float = iconSize * (1.75f / 24f),
) {
    val cx = iconSize / 2f
    val cy = iconSize / 2f
    val center = Offset(cx, cy)

    // Default matches Material 24dp icon optical disc diameter (20dp / 24dp) and Sun border stroke weight
    val outerRadius = iconSize * outerRadiusRatio
    val strokeWidth = strokeWidthPx
    val radius = (outerRadius - (strokeWidth / 2f)).coerceAtLeast(0.5f)

    val effectiveFill = if (isDimmed) fillColor.copy(alpha = ChartAlphas.ICON_BELOW_HORIZON) else fillColor
    val effectiveBorder = if (isDimmed) borderColor.copy(alpha = ChartAlphas.ICON_BELOW_HORIZON) else borderColor

    val f = phase.illuminationFraction.coerceIn(0f, 1f)

    // Unlit disc background fill so New Moon and dark limbs remain clearly visible (especially in dark mode)
    if (f < 0.999f && backgroundColor != Color.Transparent) {
        val effectiveBackground = if (isDimmed) {
            backgroundColor.copy(alpha = ChartAlphas.ICON_BELOW_HORIZON)
        } else {
            backgroundColor
        }
        drawCircle(
            color = effectiveBackground,
            radius = radius,
            center = center,
        )
    }

    when {
        f >= 0.999f -> {
            // Full Moon: complete filled disc
            drawCircle(
                color = effectiveFill,
                radius = radius,
                center = center,
            )
        }

        f > 0.001f -> {
            // Northern Hemisphere: Waxing -> right limb lit; Waning -> left limb lit.
            // Southern Hemisphere flips horizontally.
            val litOnRight = phase.isWaxing xor phase.isSouthernHemisphere
            val outerSweep = if (litOnRight) 180f else -180f
            val rx = radius * abs(2f * f - 1f)

            val outerRect = Rect(
                left = cx - radius,
                top = cy - radius,
                right = cx + radius,
                bottom = cy + radius,
            )

            reusablePath.reset()
            // 1. Lit outer limb from top pole (-90°) to bottom pole (+90°)
            reusablePath.arcTo(
                rect = outerRect,
                startAngleDegrees = -90f,
                sweepAngleDegrees = outerSweep,
                forceMoveTo = true,
            )
            // 2. Return from bottom pole (+90°) to top pole (-90°) along the terminator
            if (rx < 0.25f) {
                // Quarter Moon: straight vertical terminator
                reusablePath.lineTo(cx, cy - radius)
            } else {
                val terminatorRect = Rect(
                    left = cx - rx,
                    top = cy - radius,
                    right = cx + rx,
                    bottom = cy + radius,
                )
                val terminatorSweep = if (f > 0.5f) outerSweep else -outerSweep
                reusablePath.arcTo(
                    rect = terminatorRect,
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = terminatorSweep,
                    forceMoveTo = false,
                )
            }
            reusablePath.close()

            drawPath(path = reusablePath, color = effectiveFill)
        }
    }

    // Circular border is always drawn (both New Moon and Full Moon, above and below horizon)
    drawCircle(
        color = effectiveBorder,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth),
    )
}

/**
 * Standalone composable rendering the Moon at its exact illumination percentage and waxing/waning orientation.
 */
@Composable
fun MoonPhaseIcon(
    phase: MoonPhaseAppearance,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    fillColor: Color = LocalCustomColors.current.moon,
    borderColor: Color = LocalCustomColors.current.moonBorder,
    backgroundColor: Color = LocalCustomColors.current.moonUnlit,
    isDimmed: Boolean = false,
) {
    val path = remember { Path() }
    Canvas(modifier = modifier.size(size)) {
        drawMoonPhaseIcon(
            iconSize = this.size.minDimension,
            phase = phase,
            fillColor = fillColor,
            borderColor = borderColor,
            backgroundColor = backgroundColor,
            isDimmed = isDimmed,
            reusablePath = path,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MoonPhaseIconCyclePreview() {
    val phases = listOf(
        MoonPhaseAppearance(0.00f, isWaxing = true),   // New Moon
        MoonPhaseAppearance(0.20f, isWaxing = true),   // Waxing Crescent
        MoonPhaseAppearance(0.50f, isWaxing = true),   // First Quarter
        MoonPhaseAppearance(0.75f, isWaxing = true),   // Waxing Gibbous
        MoonPhaseAppearance(1.00f, isWaxing = true),   // Full Moon
        MoonPhaseAppearance(0.75f, isWaxing = false),  // Waning Gibbous
        MoonPhaseAppearance(0.50f, isWaxing = false),  // Last Quarter
        MoonPhaseAppearance(0.20f, isWaxing = false),  // Waning Crescent
    )
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xFFF8F9FF))
                .padding(12.dp),
        ) {
            phases.forEach { phase ->
                MoonPhaseIcon(
                    phase = phase,
                    size = 24.dp,
                    fillColor = Color(0xFFD6E3FF),
                    borderColor = Color(0xFF2962FF),
                    backgroundColor = Color(0xFFE2E6F0),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xFF10131C))
                .padding(12.dp),
        ) {
            phases.forEach { phase ->
                MoonPhaseIcon(
                    phase = phase,
                    size = 24.dp,
                    fillColor = Color(0xFF003FA6),
                    borderColor = Color(0xFFB4C5FF),
                    backgroundColor = Color(0xFF333742),
                )
            }
        }
    }
}
