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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.AnchoredMetricValue
import com.ephemeris.helios.ui.composables.charts.IrradianceDial
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.printSignificant

@Composable
fun Irradiance(
    currentIrradiance: Double,
    @Suppress("UNUSED_PARAMETER") peakIrradiance: Double,
) {
    val irradianceFloat = currentIrradiance.toFloat()

    val description = when {
        irradianceFloat < 0.001f -> "None"
        irradianceFloat < 10f -> "Trace"
        irradianceFloat < 100f -> "Very Weak"
        irradianceFloat < 250f -> "Weak"
        irradianceFloat < 500f -> "Moderate"
        irradianceFloat < 800f -> "Strong"
        irradianceFloat < 1000f -> "Very Strong"
        else -> "Extreme"
    }

    IrradianceDial(
        currentIrradiance = irradianceFloat,
        maxIrradiance = 950f,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        strokeWidth = 10.dp,
    ) {
        IrradianceInnerContent(
            title = stringResource(R.string.irradiance),
            valueText = currentIrradiance.printSignificant(),
            description = description,
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
    val valueStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = 22.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Black,
        fontFeatureSettings = "tnum",
        color = MaterialTheme.colorScheme.onSurface,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp),
    ) {
        HeaderEntry(
            text = title,
//            textVariant = "(Direct)",
            icon = R.drawable.ic_bolt_filled,
            horizontalAlignment = Alignment.CenterHorizontally,
        )
        AnchoredMetricValue(
            value = {
                Text(
                    text = valueText,
                    style = valueStyle,
                    maxLines = 1,
                    softWrap = false,
                )
            },
            anchor = {
                Text(
                    text = "0",
                    style = valueStyle,
                    maxLines = 1,
                    softWrap = false,
                )
            },
            unit = {
                Text(
                    text = "W/m²",
                    style = TextStyle(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
            },
            unitSpacing = 4.dp,
        )
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
                        Irradiance(currentIrradiance = 0.003, peakIrradiance = 850.0)
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
                        Irradiance(currentIrradiance = 640.0, peakIrradiance = 850.0)
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
                        Irradiance(currentIrradiance = 0.0, peakIrradiance = 850.0)
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
                        Irradiance(currentIrradiance = 640.0, peakIrradiance = 850.0)
                    }
                }
            }
        }
    }
}
