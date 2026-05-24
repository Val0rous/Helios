package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.ColorTempBar
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.TextEntry
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.printSignificant
import com.ephemeris.helios.utils.round
import kotlin.math.roundToInt

@Composable
fun ColorTemperature(
    currentColorTemp: Double,
    peakColorTemp: Double,
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
        }.copy(alpha = 1f)
    }

    // The upgraded description scale
    val description = when {
        isNight -> "None"
        currentColorTemp < 2500.0 -> "Crimson"
        currentColorTemp < 3000.0 -> "Amber"
        currentColorTemp < 3500.0 -> "Golden"
        currentColorTemp < 4200.0 -> "Soft Yellow"    // Replaced Pale Yellow
        currentColorTemp < 4800.0 -> "Neutral"
        currentColorTemp < 5400.0 -> "White"
        else -> "Crisp White"                         // Replaced Pure White
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        HeaderEntry(text = stringResource(R.string.color_temperature), icon = R.drawable.ic_thermometer_filled)

        Row(
            modifier = Modifier
                .fillMaxWidth()
//                .height(120.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Description at the top
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Color temp with 'K' on the same line
                if (!isNight) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = currentColorTemp.printSignificant(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "K",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 0.dp, start = 8.dp)
                        )
                    }
                } else {
                    // Nighttime fallback
                    Text(
                        text = "-- K",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            // 2. The vertically elongated squircle indicator
            Box(
                modifier = Modifier
//                    .padding(bottom = 6.dp)
                    .size(width = 36.dp, height = 54.dp)
                    .padding(top = 0.dp)
                    .background(
                        color = activeColor,
                        shape = RoundedCornerShape(9.dp) // Creates the squircle/pill shape
                    )
            )
        }


        // The horizontal line indicator component
        ColorTempBar(
            colorTemp = tempF,
            isNight = isNight,
            activeColor = activeColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp) // Gives enough vertical space for the thumb overlap
        )
    }
    CustomHorizontalDivider()
    Row(Modifier.padding(horizontal = 12.dp)) {
        TextEntry(
            text = peakColorTemp.printSignificant(),
            textVariant = "K",
            icon = R.drawable.ic_thermometer,
            desc = "Peak Color Temperature"
        )
    }
}