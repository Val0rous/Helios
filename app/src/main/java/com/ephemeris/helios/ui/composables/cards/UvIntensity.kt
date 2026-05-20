package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.UvDial
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.TextEntry
import com.ephemeris.helios.ui.composables.entries.TextEntryVariant
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.printRounded
import com.ephemeris.helios.utils.round
import kotlin.math.roundToInt

@Composable
fun UvIntensity(
    currentUvIntensity: Double,
    peakUvIntensity: Double
) {
    val uvFloat = currentUvIntensity.round(1).toFloat()

    // Standard WHO text descriptions
    val description = when {
        uvFloat < 0.01f -> "None"
        uvFloat < 3f -> "Low"
        uvFloat < 6f -> "Moderate"
        uvFloat < 8f -> "High"
        uvFloat < 11f -> "Very High"
        else -> "Extreme"
    }

    val isLightMode = !isSystemInDarkTheme()
    val colors = LocalCustomColors.current
    // 2. Exact Color Map pulled directly from UVSlicing.kt
    // Leaving native alphas untouched per your request!
    val rawFillColor = when {
        currentUvIntensity < 0.01f -> MaterialTheme.colorScheme.onSurfaceVariant // Nighttime/Zero UV
        currentUvIntensity < 2f -> colors.uvDarkGreen
        currentUvIntensity < 3f -> colors.uvGreen
        currentUvIntensity < 5f -> colors.uvYellow
        currentUvIntensity < 6f -> colors.uvAmber
        currentUvIntensity < 8f -> colors.uvOrange
        currentUvIntensity < 10f -> colors.uvRed
        currentUvIntensity < 11f -> colors.uvDarkRed
        else -> colors.uvPurple
    }.copy(alpha = 1f)

    // --- ACCESSIBILITY TRANSFORM ---
    // If we are in light mode, check the mathematical brightness of the hue.
    // Yellows/Greens trigger this and get darkened by 25%. Reds/Purples ignore it!
    val fillColor = if (isLightMode && rawFillColor.luminance() > 0.4f) {
        Color(
            red = rawFillColor.red * 0.75f,
            green = rawFillColor.green * 0.75f,
            blue = rawFillColor.blue * 0.75f,
            alpha = rawFillColor.alpha
        )
    } else rawFillColor

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        // Assuming you have a string resource for "UV Index"
        HeaderEntry(text = stringResource(R.string.uv_intensity), icon = R.drawable.ic_beach_access_filled)

        Row(
            modifier = Modifier
                .fillMaxWidth()
//                .height(120.dp)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: The 1-decimal value and text descriptor
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currentUvIntensity.printRounded(decimals = 1, stripTrailingZeros = false), // Uses your exact Formatter for 1 decimal
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
//                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${(25 * currentUvIntensity).roundToInt()} mW/m²",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
//                Text(
//                    text = "",
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    style = MaterialTheme.typography.titleSmall,
//                    modifier = Modifier.offset(y = (-2).dp)
//                )
            }

            // RIGHT SIDE: The 11+, the Dial, and the 0
            Column(
                modifier = Modifier
                    .offset(x = (5).dp)
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
//                Text(
//                    text = "11+",
//                    style = MaterialTheme.typography.labelMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )

                UvDial(
                    currentUv = uvFloat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                )

//                Text(
//                    text = "0",
//                    style = MaterialTheme.typography.labelMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
            }
        }
    }
    CustomHorizontalDivider()
    Row(Modifier.padding(horizontal = 12.dp)) {
        TextEntry(
            text = "UVI ${peakUvIntensity.round(1)}",
            textVariant = "${(25 * peakUvIntensity).roundToInt()} mW/m²",
            icon = R.drawable.ic_beach_access,
            desc = "Peak UV Index"
        )
    }
}