package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.IlluminancePill
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.TextEntry
import com.ephemeris.helios.utils.printSignificant
import com.ephemeris.helios.utils.round
import com.ephemeris.helios.utils.roundToSignificant
import kotlin.math.roundToInt

@Composable
fun Illuminance(
    currentIlluminance: Double,
    peakIlluminance: Double
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        HeaderEntry(
            text = "Illuminance",
            icon = R.drawable.ic_lightbulb_filled,
            textVariant = "(Direct)"
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currentIlluminance.printSignificant(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "lux",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // RIGHT SIDE: The Pill and limits
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
                IlluminancePill(
                    currentIlluminance = currentIlluminance.toFloat(),
                    modifier = Modifier.width(40.dp).height(72.dp) // Proportions map perfectly to the humidity image
                )

//                Column(
//                    verticalArrangement = Arrangement.SpaceBetween,
//                    modifier = Modifier.height(80.dp)
//                ) {
//                    Text("100k", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                    Text("0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
//                }
//            }
        }
    }
    CustomHorizontalDivider()
    Row(Modifier.padding(horizontal = 12.dp)) {
        TextEntry(
            text = peakIlluminance.printSignificant(),
            textVariant = "lux",
            icon = R.drawable.ic_lightbulb,
            desc = "Peak Illuminance"
        )
    }
}