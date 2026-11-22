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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.ColorTempBar
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.printSignificant

@Composable
fun ColorTemperature(
    currentColorTemp: Double,
    @Suppress("UNUSED_PARAMETER") peakColorTemp: Double,
    modifier: Modifier = Modifier
) {
    val isNight = currentColorTemp.isNaN()
    val customColors = LocalCustomColors.current
    val tempF = currentColorTemp.toFloat()

    // 1. Calculate the exact current color using linear interpolation
    val activeColor = if (isNight) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    } else {
        when {
            tempF <= 2000f -> customColors.ct2000
            tempF <= 3000f -> lerp(customColors.ct2000, customColors.ct3000, (tempF - 2000f) / 1000f)
            tempF <= 4000f -> lerp(customColors.ct3000, customColors.ct4000, (tempF - 3000f) / 1000f)
            tempF < 5500f -> lerp(customColors.ct4000, customColors.ct5500, (tempF - 4000f) / 1500f)
            else -> customColors.ct5500
        }
    }

    // The upgraded description scale
    val description = when {
        isNight -> "N/A"
        currentColorTemp < 2500.0 -> "Crimson"
        currentColorTemp < 3000.0 -> "Amber"
        currentColorTemp < 3500.0 -> "Golden"
        currentColorTemp < 4200.0 -> "Soft Yellow"    // Replaced Pale Yellow
        currentColorTemp < 4800.0 -> "Neutral"
        currentColorTemp < 5400.0 -> "White"
        else -> "Crisp White"                         // Replaced Pure White
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            HeaderEntry(
                text = stringResource(R.string.color_temp),
                icon = R.drawable.ic_thermometer_filled,
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
                .align(Alignment.CenterStart)
                .padding(top = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
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

                // Rounded rectangle showing the actual color temperature
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 38.dp)
                        .background(
                            color = activeColor,
                            shape = RoundedCornerShape(8.dp),
                        ),
                )
            }

            // Continuous color temperature gradient slider
            ColorTempBar(
                colorTemp = tempF,
                isNight = isNight,
                activeColor = activeColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
            )
        }

        // Description at the bottom-left below the slider (Google Weather AQI style)
        Text(
            text = description,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
//    CustomHorizontalDivider()
//    Row(Modifier.padding(horizontal = 12.dp)) {
//        TextEntry(
//            text = peakColorTemp.printSignificant(),
//            textVariant = "K",
//            icon = R.drawable.ic_thermometer,
//            desc = "Peak Color Temperature"
//        )
//    }
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
                    ColorTemperature(currentColorTemp = 5240.0, peakColorTemp = 5500.0)
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    ColorTemperature(currentColorTemp = 2780.0, peakColorTemp = 5500.0)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    ColorTemperature(currentColorTemp = Double.NaN, peakColorTemp = 5500.0)
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    ColorTemperature(currentColorTemp = 4520.0, peakColorTemp = 5500.0)
                }
            }
        }
    }
}