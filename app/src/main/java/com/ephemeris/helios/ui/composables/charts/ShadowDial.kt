package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.LocalCustomColors

// Non-linear breakpoints: 20f acts as the visual "infinity" ceiling for the full dial sweep.
private val ShadowDialScale = DialScale.piecewise(
    0f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f, 5f, 10f, 20f,
)

@Composable
fun ShadowDial(
    currentShadow: Float,
    isNight: Boolean,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    fillColor: Color = LocalCustomColors.current.shadowDial,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit = {},
) {
    CircularMetricDial(
        value = if (isNight) 0f else currentShadow,
        scale = ShadowDialScale,
        modifier = modifier,
        strokeWidth = strokeWidth,
        fillColor = fillColor,
        trackColor = trackColor,
        content = content,
    )
}
