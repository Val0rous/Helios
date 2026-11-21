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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.IlluminancePill
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.printSignificant

@Composable
fun Illuminance(
    currentIlluminance: Double,
    @Suppress("UNUSED_PARAMETER") peakIlluminance: Double,
) {
    val luxFloat = currentIlluminance.toFloat()
    // Direct Beam Illuminance Scale (Matched to Half-Decade Chart steps)
    // 0 lux = Sun is below the horizon (Twilight is ambient, not direct!)
    val description = when {
        luxFloat < 0.001f -> "None"           // Sun below horizon
//        luxFloat < 1f -> "Very Faint"       // < 1.5° : Sun just cresting, negligible direct beam // Same as 10f for a smoother UX
        luxFloat < 10f -> "Very Faint"        // ~2.3°  : Deep red horizon disk
        luxFloat < 100f -> "Faint"            // ~4.0°  : Golden hour beginning, weak beam
        luxFloat < 1000f -> "Soft"            // ~7.0°  : Clearly visible but low-energy light
        luxFloat < 10000f -> "Moderate"       // ~15.0° : Gentle morning/evening light
        luxFloat < 30000f -> "Bright"         // ~27.0° : Comfortable, clear daylight
        luxFloat < 60000f -> "Very Bright"    // ~44.0° : High sun, casting harsh shadows
        luxFloat < 105000f -> "Intense"       // Zenith : Absolute peak overhead
        else -> "Extreme"                     // High altitude / Reflection spikes
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
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.CenterStart),
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
                    style = TextStyle(
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
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Illuminance(currentIlluminance = 1200.0, peakIlluminance = 85000.0)
                    }
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Illuminance(currentIlluminance = 105000.0, peakIlluminance = 105000.0)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Illuminance(currentIlluminance = 0.0, peakIlluminance = 85000.0)
                    }
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Illuminance(currentIlluminance = 35000.0, peakIlluminance = 85000.0)
                    }
                }
            }
        }
    }
}
