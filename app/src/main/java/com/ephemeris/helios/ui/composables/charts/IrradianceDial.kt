package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.LocalCustomColors

@Composable
fun IrradianceDial(
    currentIrradiance: Float,
    maxIrradiance: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    fillColor: Color = LocalCustomColors.current.irrDial,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val scale = remember(maxIrradiance) { DialScale.linear(max = maxIrradiance) }
    CircularMetricDial(
        value = currentIrradiance,
        scale = scale,
        modifier = modifier,
        strokeWidth = strokeWidth,
        fillColor = fillColor,
        trackColor = trackColor,
        content = content,
    )
}
