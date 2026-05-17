package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    // Direct Illuminance mathematically mirrors your Irradiance thresholds (* 105 efficacy)
    val description = when {
        luxFloat < 1f -> "None"
        luxFloat <= 1000f -> "Negligible"
        luxFloat <= 10500f -> "Marginal"
        luxFloat <= 26250f -> "Low"
        luxFloat <= 52500f -> "Moderate"
        luxFloat <= 84000f -> "High"
        luxFloat <= 105000f -> "Excellent"
        else -> "Extreme"
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
                )
                Text(
                    text = "lux",
                    style = MaterialTheme.typography.labelLarge,
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