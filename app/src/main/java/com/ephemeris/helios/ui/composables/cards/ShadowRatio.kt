package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.ShadowDial
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.TextEntry
import com.ephemeris.helios.utils.printRounded
import com.ephemeris.helios.utils.printSignificant
import com.ephemeris.helios.utils.roundToSignificant

@Composable
fun ShadowRatio(
    currentShadowRatio: Double,
    peakShadowRatio: Double,
    sunAltitude: Double,
) {
    val isNight = sunAltitude <= 0.0

    // If the shadow ratio is higher than or infinite, we display the infinity symbol
    // No longer clipping >10 to ∞. If it's 35.6, we show 35.6!
    val displayValue = if (isNight) "∞" else currentShadowRatio.printSignificant()

    val description = when {
        isNight -> "Infinite"
        currentShadowRatio < 0.01 -> "None"
        currentShadowRatio < 0.25 -> "Overhead"
        currentShadowRatio < 0.5 -> "Minimal"
        currentShadowRatio < 0.75 -> "Very Short"
        currentShadowRatio < 1.0 -> "Short"
        currentShadowRatio < 2.0 -> "Moderate"
        currentShadowRatio < 5.0 -> "Long"
        currentShadowRatio < 10.0 -> "Very Long"
        else -> "Extreme"
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        HeaderEntry(text = "Shadow Ratio", icon = R.drawable.ic_ev_shadow_filled) // Replace with your icon

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "to 1",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        modifier = Modifier.offset(y = (2).dp)
                )
            }

            ShadowDial(
                currentShadow = currentShadowRatio.toFloat(),
                isNight = isNight,
                modifier = Modifier.size(72.dp),
                strokeWidth = 16.dp
            )
        }
    }
    CustomHorizontalDivider()
    Row(Modifier.padding(horizontal = 12.dp)) {
        TextEntry(
            text = "${peakShadowRatio.roundToSignificant()} : 1",
            icon = R.drawable.ic_ev_shadow,
            desc = "Peak Shadow Ratio"
        )
    }
}