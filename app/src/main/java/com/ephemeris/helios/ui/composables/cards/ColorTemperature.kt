package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
// import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.ColorTempBar
import com.ephemeris.helios.ui.composables.charts.colorForKelvin
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.MetricModePill
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.printSignificant

@Composable
fun ColorTemperature(
    currentColorTemp: Double,
    @Suppress("UNUSED_PARAMETER") peakColorTemp: Double,
    modifier: Modifier = Modifier,
    minK: Float = 1800f,
    maxK: Float = 5800f,
    modeLabel: String? = null,
    onToggleMode: (() -> Unit)? = null,
) {
    val isNight = currentColorTemp.isNaN()
    val customColors = LocalCustomColors.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val tempF = currentColorTemp.toFloat()

    // 1. Exact current color from the shared Kelvin spectrum
    val activeColor = if (isNight) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    } else {
        colorForKelvin(tempF, customColors)
    }

    // 2. Flat Material-compliant tonal tint derived from the active color temperature
    val glowModifier = if (!isNight) {
        val tintAlpha = if (isDarkSurface) 0.22f else 0.28f
        Modifier.background(activeColor.copy(alpha = tintAlpha))
        /*
        val topAlpha = if (isDarkSurface) 0.24f else 0.30f
        val bottomAlpha = if (isDarkSurface) 0.46f else 0.62f
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    activeColor.copy(alpha = topAlpha),
                    activeColor.copy(alpha = bottomAlpha),
                ),
            ),
        )
        */
    } else {
        Modifier
    }

    // Description scale spanning 1,800 K (horizon) to 14,000 K (blue hour / open shade)
    val description = when {
        isNight -> "N/A"
        currentColorTemp < 2500.0 -> "Crimson"
        currentColorTemp < 3000.0 -> "Amber"
        currentColorTemp < 3500.0 -> "Golden"
        currentColorTemp < 4200.0 -> "Soft Yellow"
        currentColorTemp < 4800.0 -> "Neutral"
        currentColorTemp < 5400.0 -> "White"
        currentColorTemp < 6200.0 -> "Crisp White"
        currentColorTemp < 7000.0 -> "Cool White"
        currentColorTemp < 8200.0 -> "Silver Blue"
        currentColorTemp < 10200.0 -> "Sky Blue"
        currentColorTemp < 12000.0 -> "Azure"
        else -> "Cobalt"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(glowModifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            val titleColor = MaterialTheme.colorScheme.onSurface
            HeaderEntry(
                text = stringResource(R.string.color_temp),
                textColor = titleColor,
                icon = R.drawable.ic_thermometer_filled,
                iconTint = titleColor,
                horizontalAlignment = Alignment.CenterHorizontally,
                iconSize = 20.dp,
                textSize = 16.sp,
                spacing = 8.dp,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart),
        ) {
            val tempValueStyle = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                fontFeatureSettings = "tnum",
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Color temp with 'K' pinned at a stable horizontal position and aligned on baseline
            if (!isNight) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier.alignByBaseline(),
                    ) {
                        Text(
                            text = 8888.0.printSignificant(),
                            style = tempValueStyle,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .alpha(0f)
                                .clearAndSetSemantics {},
                        )
                        Text(
                            text = currentColorTemp.printSignificant(),
                            style = tempValueStyle,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Text(
                        text = "K",
                        style = tempValueStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            } else {
                Text(
                    text = "-- K",
                    style = tempValueStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    softWrap = false,
                )
            }

            // Continuous color temperature gradient slider with matching surface cutout border
            ColorTempBar(
                colorTemp = tempF,
                isNight = isNight,
                activeColor = activeColor,
                minK = minK,
                maxK = maxK,
                borderColor = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
            )
        }

        // Description at bottom-left, with mode tonal pill at bottom-right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = if (modeLabel != null) 4.dp else 0.dp),
            )
            if (modeLabel != null && onToggleMode != null) {
                MetricModePill(
                    label = modeLabel,
                    onClick = onToggleMode,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorTemperatureTilePreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .background(Color(0xFFECEEF4))
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    ColorTemperature(
                        currentColorTemp = 2780.0,
                        peakColorTemp = 5500.0,
                        minK = 1800f,
                        maxK = 5800f,
                        modeLabel = "Direct",
                        onToggleMode = {},
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    ColorTemperature(
                        currentColorTemp = 5820.0,
                        peakColorTemp = 13000.0,
                        minK = 3500f,
                        maxK = 14000f,
                        modeLabel = "Global",
                        onToggleMode = {},
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    ColorTemperature(
                        currentColorTemp = 11400.0,
                        peakColorTemp = 13500.0,
                        minK = 5500f,
                        maxK = 14000f,
                        modeLabel = "Ambient",
                        onToggleMode = {},
                    )
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    ColorTemperature(
                        currentColorTemp = 7420.0,
                        peakColorTemp = 8000.0,
                        minK = 1800f,
                        maxK = 8000f,
                        modeLabel = "Purkinje",
                        onToggleMode = {},
                    )
                }
            }
        }
    }
}