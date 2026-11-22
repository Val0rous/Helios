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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.IlluminancePill
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.MetricModePill
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.datastore.IlluminanceMode
import com.ephemeris.helios.utils.printSignificant

@Composable
fun Illuminance(
    currentIlluminance: Double,
    @Suppress("UNUSED_PARAMETER") peakIlluminance: Double,
    mode: IlluminanceMode = IlluminanceMode.GLOBAL,
    onToggleMode: () -> Unit = {},
) {
    val luxFloat = currentIlluminance.toFloat()
    val maxLux = if (mode == IlluminanceMode.DIFFUSE) 10_000f else 100_000f

    val description = if (mode == IlluminanceMode.DIFFUSE) {
        when {
            luxFloat < 0.001f -> "None"
            luxFloat < 1f -> "Twilight"
            luxFloat < 50f -> "Dusk"
            luxFloat < 400f -> "Dim"
            luxFloat < 2000f -> "Soft"
            luxFloat < 6000f -> "Moderate"
            luxFloat < 10000f -> "Bright"
            else -> "Very Bright"
        }
    } else {
        // Direct / Global Illuminance Scale (Matched to Half-Decade Chart steps)
        when {
            luxFloat < 0.001f -> "None"           // Sun below horizon / deep night
            luxFloat < 10f -> "Very Faint"        // Deep red horizon disk / twilight
            luxFloat < 100f -> "Faint"            // Golden hour beginning
            luxFloat < 1000f -> "Soft"            // Low-energy light
            luxFloat < 10000f -> "Moderate"       // Gentle morning/evening light
            luxFloat < 30000f -> "Bright"         // Comfortable, clear daylight
            luxFloat < 60000f -> "Very Bright"    // High sun, casting harsh shadows
            luxFloat < 105000f -> "Intense"       // Zenith peak overhead
            else -> "Extreme"                     // High altitude / Reflection spikes
        }
    }

    val valueStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Black,
        fontFeatureSettings = "tnum",
        color = MaterialTheme.colorScheme.onSurface,
    )

    IlluminancePill(
        currentIlluminance = luxFloat,
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
                    icon = R.drawable.ic_lightbulb_filled,
//                    textVariant = "(Direct)",
                    horizontalAlignment = Alignment.CenterHorizontally,
                    iconSize = 20.dp,
                    textSize = 16.sp,
                    spacing = 8.dp,
                )
            }

            // Center the value + description pair vertically between the top header and the
            // bottom-left tonal pill so whitespace stays balanced above and below.
            Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = currentIlluminance.printSignificant(),
                            style = valueStyle,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Text(
                            text = "lux",
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
                    Text(
                        text = description,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                },
            ) { measurables, constraints ->
                val loose = constraints.copy(minWidth = 0, minHeight = 0)
                val valuePlaceable = measurables[0].measure(loose)
                val descPlaceable = measurables[1].measure(loose)
                val gapPx = 2.dp.roundToPx()
                val groupHeight = valuePlaceable.height + gapPx + descPlaceable.height
                val valueY = ((constraints.maxHeight - groupHeight) / 2 + 5.dp.roundToPx()).coerceAtLeast(0)
                val descY = valueY + valuePlaceable.height + gapPx
                layout(constraints.maxWidth, constraints.maxHeight) {
                    valuePlaceable.placeRelative(0, valueY)
                    descPlaceable.placeRelative(0, descY)
                }
            }

            MetricModePill(
                label = mode.label,
                onClick = onToggleMode,
                modifier = Modifier.align(Alignment.BottomStart),
            )
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
                            currentIlluminance = 105000.0,
                            peakIlluminance = 105000.0,
                            mode = IlluminanceMode.GLOBAL,
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
                            currentIlluminance = 0.0,
                            peakIlluminance = 85000.0,
                            mode = IlluminanceMode.DIRECT,
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
                            currentIlluminance = 9800.0,
                            peakIlluminance = 10500.0,
                            mode = IlluminanceMode.DIFFUSE,
                        )
                    }
                }
            }
        }
    }
}
