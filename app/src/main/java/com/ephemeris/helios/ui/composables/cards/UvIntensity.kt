package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.DecalobeShape
import com.ephemeris.helios.ui.composables.charts.UvCircleIndicator
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.round

@Composable
fun UvIntensity(
    currentUvIntensity: Double,
    @Suppress("UNUSED_PARAMETER") peakUvIntensity: Double,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val uvFloat = currentUvIntensity.round(1).toFloat()
    val isZero = uvFloat < 0.05f

    // Standard WHO text descriptions
    val description = when {
        isZero -> "None"
        uvFloat < 3f -> "Low"
        uvFloat < 6f -> "Moderate"
        uvFloat < 8f -> "High"
        uvFloat < 11f -> "Very High"
        else -> "Extreme"
    }

    // Discrete vertical fill color shared with the UV chart slices, capped at standard metric
    // opacity in dark mode so foreground text on the tile stays high-contrast while the chart pops.
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val fills = LocalCustomColors.current.fills
    val rawFillColor = when {
        isZero -> Color.Transparent
        uvFloat < 2f -> fills.uvDarkGreen
        uvFloat < 3f -> fills.uvGreen
        uvFloat < 5f -> fills.uvYellow
        uvFloat < 6f -> fills.uvAmber
        uvFloat < 8f -> fills.uvOrange
        uvFloat < 10f -> fills.uvRed
        uvFloat < 11f -> fills.uvDarkRed
        else -> fills.uvPurple
    }
    val fillColor = if (isDarkSurface && rawFillColor != Color.Transparent) {
        rawFillColor.copy(alpha = 0.45f)
    } else {
        rawFillColor
    }

    // Always 1 decimal place (e.g. "9.0"), except plain "0" when there is no UV
    val valueText = if (isZero) {
        "0"
    } else {
        String.format(locale, "%.1f", currentUvIntensity)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (fillColor != Color.Transparent) Modifier.background(fillColor) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        // 1. Semicircle 5-circle UV tier indicator nestled inside the bottom 5 lobes
        UvCircleIndicator(
            currentUv = uvFloat,
            borderColor = surfaceColor,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Centered title, big value, and description inside the semicircle bowl
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 31.dp),
            ) {
                HeaderEntry(
                    text = stringResource(R.string.uv_intensity),
                    icon = R.drawable.ic_beach_access_filled,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    iconSize = 18.dp,
                    textSize = 14.5.sp,
                    spacing = 4.dp,
                )
            }

            Text(
                text = valueText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum",
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 10.dp),
            )

            Text(
                text = description,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
            )

            /*
            Text(
                text = "${(25 * currentUvIntensity).roundToInt()} mW/m²",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
            */
        }
    }

    /*
    CustomHorizontalDivider()
    Row(Modifier.padding(horizontal = 12.dp)) {
        TextEntry(
            text = "UVI ${peakUvIntensity.round(1)}",
            textVariant = "${(25 * peakUvIntensity).roundToInt()} mW/m²",
            icon = R.drawable.ic_beach_access,
            desc = "Peak UV Index"
        )
    }
    */
}

@Preview(showBackground = true)
@Composable
private fun UvIntensityDecalobePreview() {
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
                    shape = DecalobeShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    UvIntensity(currentUvIntensity = 0.0, peakUvIntensity = 6.2)
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = DecalobeShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    UvIntensity(currentUvIntensity = 2.4, peakUvIntensity = 6.2)
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = DecalobeShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    UvIntensity(currentUvIntensity = 5.3, peakUvIntensity = 8.1)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = DecalobeShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    UvIntensity(currentUvIntensity = 9.0, peakUvIntensity = 10.2)
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = DecalobeShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    UvIntensity(currentUvIntensity = 10.4, peakUvIntensity = 10.8)
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = DecalobeShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    UvIntensity(currentUvIntensity = 11.6, peakUvIntensity = 11.6)
                }
            }
        }
    }
}
