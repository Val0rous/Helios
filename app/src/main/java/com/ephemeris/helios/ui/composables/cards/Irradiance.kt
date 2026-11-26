package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.IrradianceDial
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.MetricModePill
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.datastore.IrradianceMode
import com.ephemeris.helios.utils.printSignificant

@Composable
fun Irradiance(
    currentIrradiance: Double,
    @Suppress("UNUSED_PARAMETER") peakIrradiance: Double,
    mode: IrradianceMode = IrradianceMode.DIRECT,
    onToggleMode: () -> Unit = {},
) {
    val irradianceFloat = currentIrradiance.toFloat()

    val maxIrradiance = when (mode) {
        IrradianceMode.DIRECT -> 950f
        IrradianceMode.GLOBAL -> 1050f
        IrradianceMode.DIFFUSE -> 120f
    }

    val description = when (mode) {
        IrradianceMode.DIFFUSE -> when {
            irradianceFloat < 0.001f -> "None"
            irradianceFloat < 0.5f -> "Trace"
            irradianceFloat < 4f -> "Twilight"
            irradianceFloat < 10f -> "Faint"
            irradianceFloat < 30f -> "Low"
            irradianceFloat < 60f -> "Moderate"
            irradianceFloat < 90f -> "Bright"
            else -> "Strong"
        }
        IrradianceMode.GLOBAL -> when {
            irradianceFloat < 0.001f -> "None"
            irradianceFloat < 0.5f -> "Trace"
            irradianceFloat < 4f -> "Twilight"
            irradianceFloat < 100f -> "Very Weak"
            irradianceFloat < 250f -> "Weak"
            irradianceFloat < 500f -> "Moderate"
            irradianceFloat < 800f -> "Strong"
            irradianceFloat < 1000f -> "Very Strong"
            else -> "Extreme"
        }
        IrradianceMode.DIRECT -> when {
            irradianceFloat < 0.001f -> "None"
            irradianceFloat < 10f -> "Trace"
            irradianceFloat < 100f -> "Very Weak"
            irradianceFloat < 250f -> "Weak"
            irradianceFloat < 500f -> "Moderate"
            irradianceFloat < 800f -> "Strong"
            irradianceFloat < 1000f -> "Very Strong"
            else -> "Extreme"
        }
    }

    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentColor = LocalCustomColors.current.irrTitle

    IrradianceDial(
        currentIrradiance = irradianceFloat,
        maxIrradiance = maxIrradiance,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        strokeWidth = 9.dp,
    ) {
        IrradianceInnerContent(
            title = stringResource(R.string.irradiance),
            valueText = currentIrradiance.printSignificant(),
            description = description,
        )
        MetricModePill(
            label = mode.label,
            onClick = onToggleMode,
            containerColor = accentColor.copy(alpha = if (isDarkSurface) 0.22f else 0.14f),
            contentColor = accentColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        )
    }
//    CustomHorizontalDivider()
//    Row(Modifier.padding(horizontal = 12.dp)) {
//        TextEntry(text = peakIrradiance.printSignificant(), textVariant = "W/m²", icon = R.drawable.ic_bolt, desc = "Peak Irradiance")
//    }
}

@Composable
private fun IrradianceInnerContent(
    title: String,
    valueText: String,
    description: String,
) {
    val titleColor = LocalCustomColors.current.irrTitle
    val valueStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Black,
        fontFeatureSettings = "tnum",
        color = MaterialTheme.colorScheme.onSurface,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
    ) {
        HeaderEntry(
            text = title,
            textColor = titleColor,
//            textVariant = "(Direct)",
            icon = R.drawable.ic_bolt_filled,
            iconTint = titleColor,
            horizontalAlignment = Alignment.CenterHorizontally,
            iconSize = 19.dp,
            textSize = 15.sp,
            spacing = 5.dp,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = valueText,
                style = valueStyle,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = "W/m²",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
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
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IrradianceCircularTilePreview() {
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
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Irradiance(
                            currentIrradiance = 640.0,
                            peakIrradiance = 850.0,
                            mode = IrradianceMode.DIRECT,
                        )
                    }
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Irradiance(
                            currentIrradiance = 1050.0,
                            peakIrradiance = 1050.0,
                            mode = IrradianceMode.GLOBAL,
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Irradiance(
                            currentIrradiance = 0.0,
                            peakIrradiance = 850.0,
                            mode = IrradianceMode.DIRECT,
                        )
                    }
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Irradiance(
                            currentIrradiance = 86.4,
                            peakIrradiance = 92.0,
                            mode = IrradianceMode.DIFFUSE,
                        )
                    }
                }
            }
        }
    }
}
