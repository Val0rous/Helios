package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.AirMassPill
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.TextEntry
import com.ephemeris.helios.utils.printSignificant

@Composable
fun AirMass(
    currentAirMass: Double,
    minAirMass: Double // Passing MINIMUM, as 1.0 is the clearest sky
) {
    val amFloat = currentAirMass.toFloat()

    // Custom non-linear scale matching the exponential nature of atmospheric thickness
    val description = when {
        amFloat < 0.1f -> "None"         // Night (AM is 0)
        amFloat < 1.0f -> "Sparse"       // High altitude anomaly (Sub-1 AM)
        amFloat < 1.15f -> "Very Thin"   // Absolute peak overhead (AM 1.0 - 1.15)
        amFloat < 1.5f -> "Thin"         // ~42°+ High sun, very clear
        amFloat < 3.0f -> "Moderate"     // ~20°+ Moderate scattering
        amFloat < 7.0f -> "Thick"        // ~8°+ Visible warming of the light
        amFloat < 15.0f -> "Very Thick"  // ~4°+ Golden hour territory
        amFloat < 30.0f -> "Dense"       // ~1.5°+ Deep horizon scattering
        else -> "Extreme"                // Sunrise/Sunset horizon hugging
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        HeaderEntry(
            text = "Air Mass",
            icon = R.drawable.ic_foggy_filled, // Replace with your preferred atmospheric icon
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

                if (amFloat > 0.1f) {
                    Text(
                        text = currentAirMass.printSignificant(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Text(
                        text = "--",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = "Air Masses",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // RIGHT SIDE: The Pill
            AirMassPill(
                currentAirMass = amFloat,
                modifier = Modifier.width(40.dp).height(72.dp)
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    CustomHorizontalDivider()

    Row(Modifier.padding(horizontal = 12.dp)) {
        TextEntry(
            text = minAirMass.printSignificant(),
            textVariant = "AM",
            icon = R.drawable.ic_foggy, // Match header icon
            desc = "Minimum Daily Air Mass" // Explains why it's a small number
        )
    }
}