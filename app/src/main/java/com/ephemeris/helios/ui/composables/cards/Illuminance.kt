package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.IlluminancePill
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.MetricModePill
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.datastore.IlluminanceMode
import com.ephemeris.helios.utils.printSignificant

@Composable
fun Illuminance(
    currentIlluminance: Double,
    @Suppress("UNUSED_PARAMETER") peakIlluminance: Double,
    mode: IlluminanceMode = IlluminanceMode.GLOBAL,
    onToggleMode: () -> Unit = {},
    isLunar: Boolean = false,
) {
    val displayValue = if (isLunar) currentIlluminance * 1000.0 else currentIlluminance
    val waveValue = displayValue.toFloat()
    val unitLabel = if (isLunar) "mlx" else "lx"
    // Human-eye visibility floor (same units as waveValue):
    // - Moon (mlx): 2 mlx (~0.002 lux, natural starlight + airglow ceiling) hides pre-moonrise
    //   lunar twilight and invisible near-New-Moon crescents.
    // - Sun (lux): 0.01 lux (~-13.2°, astronomical/nautical twilight border) hides pitch-dark
    //   astronomical twilight while keeping deep nautical twilight clearly visible.
    val minLux = if (isLunar) 2f else 0.01f
    val maxLux = if (isLunar) {
        if (mode == IlluminanceMode.DIFFUSE) 30f else 300f
    } else {
        if (mode == IlluminanceMode.DIFFUSE) 10_000f else 100_000f
    }

    val description = if (isLunar) {
        when (mode) {
            IlluminanceMode.DIFFUSE -> when {
                waveValue < 0.001f -> "None"
                waveValue < 0.1f -> "Trace"
                waveValue < 1f -> "Faint"
                waveValue < 5f -> "Dim"
                waveValue < 15f -> "Soft"
                waveValue < 25f -> "Noticeable"
                else -> "Bright Glow"
            }
            IlluminanceMode.DIRECT -> when {
                waveValue < 0.001f -> "None"
                waveValue < 1f -> "Trace"
                waveValue < 5f -> "Very Faint"
                waveValue < 25f -> "Faint"
                waveValue < 80f -> "Dim"
                waveValue < 160f -> "Soft"
                waveValue < 250f -> "Bright"
                else -> "Brilliant"
            }
            IlluminanceMode.GLOBAL -> when {
                waveValue < 0.001f -> "None"
                waveValue < 0.5f -> "Trace"
                waveValue < 5f -> "Very Faint"
                waveValue < 25f -> "Faint"
                waveValue < 80f -> "Dim"
                waveValue < 160f -> "Soft"
                waveValue < 250f -> "Bright"
                else -> "Brilliant"
            }
        }
    } else {
        when (mode) {
            IlluminanceMode.DIFFUSE -> when {
                waveValue < 0.001f -> "Pitch Dark"
                waveValue < 0.5f -> "Very Dark"
                waveValue < 5f -> "Dark"
                waveValue < 50f -> "Very Dim"
                waveValue < 400f -> "Dim"
                waveValue < 2000f -> "Soft"
                waveValue < 6000f -> "Moderate"
                waveValue < 10000f -> "Bright"
                else -> "Very Bright"
            }
            IlluminanceMode.GLOBAL -> when {
                waveValue < 0.001f -> "Pitch Dark"
                waveValue < 0.5f -> "Very Dark"
                waveValue < 5f -> "Dark"
                waveValue < 50f -> "Very Dim"
                waveValue < 400f -> "Dim"
                waveValue < 2000f -> "Soft"
                waveValue < 10000f -> "Moderate"
                waveValue < 30000f -> "Bright"
                waveValue < 60000f -> "Very Bright"
                waveValue < 100000f -> "Intense"
                else -> "Extreme"
            }
            IlluminanceMode.DIRECT -> when {
                waveValue < 0.001f -> "None"           // Sun below horizon / deep night
                waveValue < 10f -> "Very Faint"        // Deep red horizon disk / twilight
                waveValue < 100f -> "Faint"            // Golden hour beginning
                waveValue < 1000f -> "Soft"            // Low-energy light
                waveValue < 10000f -> "Moderate"       // Gentle morning/evening light
                waveValue < 30000f -> "Bright"         // Comfortable, clear daylight
                waveValue < 60000f -> "Very Bright"    // High sun, casting harsh shadows
                waveValue < 105000f -> "Intense"       // Zenith peak overhead
                else -> "Extreme"                      // High altitude / Reflection spikes
            }
        }
    }

    val valueStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Black,
        fontFeatureSettings = "tnum",
        color = MaterialTheme.colorScheme.onSurface,
    )

    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val titleColor = LocalCustomColors.current.luxTitle

    IlluminancePill(
        currentIlluminance = waveValue,
        minLux = minLux,
        maxLux = maxLux,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            ) {
                HeaderEntry(
                    text = "Illuminance",
                    textColor = titleColor,
                    icon = R.drawable.ic_lightbulb_filled,
                    iconTint = titleColor,
//                    textVariant = "(Direct)",
                    horizontalAlignment = Alignment.CenterHorizontally,
                    iconSize = 20.dp,
                    textSize = 16.sp,
                    spacing = 8.dp,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text(
                    text = displayValue.printSignificant(),
                    style = valueStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = unitLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
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
                        .padding(end = 4.dp),
                )
                MetricModePill(
                    label = mode.label,
                    onClick = onToggleMode,
                    containerColor = titleColor.copy(alpha = if (isDarkSurface) 0.22f else 0.16f),
                    contentColor = titleColor,
                )
            }
        }
    }

//    CustomHorizontalDivider()
//    Row(Modifier.padding(horizontal = 12.dp)) {
//        TextEntry(
//            text = peakIlluminance.printSignificant(),
//            textVariant = "lux",
//            icon = R.drawable.ic_lightbulb,
//            desc = "Peak Illuminance"
//        )
//    }
}

@Preview(showBackground = true)
@Composable
private fun IlluminanceWavyTilePreview() {
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Illuminance(
                            currentIlluminance = 1200.0,
                            peakIlluminance = 85000.0,
                            mode = IlluminanceMode.DIRECT,
                        )
                    }
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Illuminance(
                            currentIlluminance = 0.24,
                            peakIlluminance = 0.30,
                            mode = IlluminanceMode.GLOBAL,
                            isLunar = true,
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Illuminance(
                            currentIlluminance = 2.4,
                            peakIlluminance = 85000.0,
                            mode = IlluminanceMode.GLOBAL,
                        )
                    }
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Illuminance(
                            currentIlluminance = 0.0185,
                            peakIlluminance = 0.025,
                            mode = IlluminanceMode.DIFFUSE,
                            isLunar = true,
                        )
                    }
                }
            }
        }
    }
}
