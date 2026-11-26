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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.AirMassPill
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.printSignificant

@Composable
fun AirMass(
    currentAirMass: Double,
    @Suppress("UNUSED_PARAMETER") minAirMass: Double, // Passing MINIMUM, as 1.0 is the clearest sky
) {
    val amFloat = currentAirMass.toFloat()
    val isNight = amFloat < 0.1f

    // Custom non-linear scale matching the exponential nature of atmospheric thickness
    val description = when {
        isNight -> "N/A"                 // Night (AM is 0)
        amFloat < 1.0f -> "Sparse"       // High altitude anomaly (Sub-1 AM)
        amFloat < 1.15f -> "Very Thin"   // Absolute peak overhead (AM 1.0 - 1.15)
        amFloat < 1.5f -> "Thin"         // ~42°+ High sun, very clear
        amFloat < 3.0f -> "Moderate"     // ~20°+ Moderate scattering
        amFloat < 7.0f -> "Thick"        // ~8°+ Visible warming of the light
        amFloat < 15.0f -> "Very Thick"  // ~4°+ Golden hour territory
        amFloat < 30.0f -> "Dense"       // ~1.5°+ Deep horizon scattering
        else -> "Extreme"                // Sunrise/Sunset horizon hugging
    }

    val valueColor = if (isNight) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    AirMassPill(
        currentAirMass = amFloat,
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
                val titleColor = LocalCustomColors.current.amTitle
                HeaderEntry(
                    text = "Air Mass",
                    textColor = titleColor,
                    icon = R.drawable.ic_foggy_filled,
                    iconTint = titleColor,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    iconSize = 20.dp,
                    textSize = 16.sp,
                    spacing = 8.dp,
                )
            }
            Text(
                text = if (isNight) "--" else currentAirMass.printSignificant(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum",
                    color = valueColor,
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.align(Alignment.CenterStart),
            )
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
//            text = minAirMass.printSignificant(),
//            textVariant = "AM",
//            icon = R.drawable.ic_foggy,
//            desc = "Minimum Daily Air Mass"
//        )
//    }
}

@Preview(showBackground = true)
@Composable
private fun AirMassWavyTilePreview() {
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
                        AirMass(currentAirMass = 2.0, minAirMass = 1.15)
                    }
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AirMass(currentAirMass = 12.5, minAirMass = 1.15)
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
                        AirMass(currentAirMass = 0.0, minAirMass = 1.15)
                    }
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AirMass(currentAirMass = 3.5, minAirMass = 1.15)
                    }
                }
            }
        }
    }
}
