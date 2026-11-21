package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.ShadowDial
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.TextEntry
import com.ephemeris.helios.utils.calc.MAX_SHADOW_RATIO
import com.ephemeris.helios.utils.printSignificant
import com.ephemeris.helios.utils.roundToSignificant

@Composable
fun ShadowRatio(
    currentShadowRatio: Double,
    peakShadowRatio: Double,
) {
    val isNight = currentShadowRatio <= 0.0
    val isInfinite = isNight || currentShadowRatio >= MAX_SHADOW_RATIO

    // Display ∞ when below the apparent horizon or at the 5,000:1 horizon-graze limit
    val displayValue = if (isInfinite) "∞" else currentShadowRatio.printSignificant()

    val description = when {
        isInfinite -> "Infinite"
        currentShadowRatio < 0.001 -> "None"
        currentShadowRatio < 0.25 -> "Minimal"
        currentShadowRatio < 0.5 -> "Very Short"
        currentShadowRatio < 0.75 -> "Short"
        currentShadowRatio < 1.0 -> "Reduced"
        currentShadowRatio < 2.0 -> "Moderate"
        currentShadowRatio < 3.0 -> "Extended"
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
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "to 1",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
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
        val peakText = if (peakShadowRatio <= 0.0 || peakShadowRatio >= MAX_SHADOW_RATIO) {
            "∞"
        } else {
            "${peakShadowRatio.roundToSignificant()} : 1"
        }
        TextEntry(
            text = peakText,
            icon = R.drawable.ic_ev_shadow,
            desc = "Peak Shadow Ratio"
        )
    }
}