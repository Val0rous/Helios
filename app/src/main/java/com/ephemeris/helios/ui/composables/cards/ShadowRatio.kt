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
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.ShadowDial
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.calc.MAX_SHADOW_RATIO
import com.ephemeris.helios.utils.printSignificant
import kotlin.math.roundToInt

@Composable
fun ShadowRatio(
    currentShadowRatio: Double,
    @Suppress("UNUSED_PARAMETER") peakShadowRatio: Double,
) {
    val isNight = currentShadowRatio <= 0.0
    val isInfinite = !isNight && (currentShadowRatio >= MAX_SHADOW_RATIO)

    val description = when {
        isNight -> "N/A"
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

    ShadowDial(
        currentShadow = currentShadowRatio.toFloat(),
        isNight = isNight,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        strokeWidth = 9.dp,
    ) {
        ShadowRatioInnerContent(
            title = stringResource(R.string.shadows),
            valueText = when {
                isNight -> "--"
                isInfinite -> "∞"
                else -> currentShadowRatio.printSignificant()
            },
            isNight = isNight,
            isInfinite = isInfinite,
            description = description,
        )
    }

//    CustomHorizontalDivider()
//    Row(Modifier.padding(horizontal = 12.dp)) {
//        val peakText = if (peakShadowRatio <= 0.0 || peakShadowRatio >= MAX_SHADOW_RATIO) {
//            "∞"
//        } else {
//            "${peakShadowRatio.roundToSignificant()} : 1"
//        }
//        TextEntry(
//            text = peakText,
//            icon = R.drawable.ic_ev_shadow,
//            desc = "Peak Shadow Ratio"
//        )
//    }
}

@Composable
private fun ShadowRatioInnerContent(
    title: String,
    valueText: String,
    isNight: Boolean,
    isInfinite: Boolean,
    description: String,
) {
    val valueColor = if (isNight) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val valueStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Black,
        fontFeatureSettings = "tnum",
        color = valueColor,
    )
    val infinityStyle = MaterialTheme.typography.headlineMedium.copy(
        fontSize = 38.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp),
    ) {
        val titleColor = LocalCustomColors.current.shadowDial
        HeaderEntry(
            text = title,
            textColor = titleColor,
            icon = R.drawable.ic_ev_shadow_filled,
            iconTint = titleColor,
            horizontalAlignment = Alignment.CenterHorizontally,
            iconSize = 19.dp,
            textSize = 15.sp,
            spacing = 5.dp,
        )
        if (isNight) {
            Text(
                text = valueText,
                style = valueStyle,
                maxLines = 1,
                softWrap = false,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = valueText,
                    style = if (isInfinite) infinityStyle else valueStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = if (isInfinite) {
                        // Keep layout box height & baseline identical to 28.sp digits so
                        // header, ": 1", and description never shift vertically while
                        // visually centering the taller ∞ glyph against digit cap-height.
                        Modifier
                            .alignByBaseline()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val targetHeight = 32.dp.roundToPx()
                                val targetBaseline = 25.5.dp.roundToPx()
                                val digitCenterY = targetBaseline - 28.sp.toPx() * 0.36f
                                val glyphCenterY = placeable[FirstBaseline] - 38.sp.toPx() * 0.30f
                                val offsetY = (digitCenterY - glyphCenterY).roundToInt()
                                layout(
                                    width = placeable.width,
                                    height = targetHeight,
                                    alignmentLines = mapOf(FirstBaseline to targetBaseline),
                                ) {
                                    placeable.placeRelative(0, offsetY)
                                }
                            }
                    } else {
                        Modifier.alignByBaseline()
                    },
                )
                Text(
                    text = ": 1",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
//        Text(
//            text = "to 1",
//            style = MaterialTheme.typography.labelLarge,
//            fontWeight = FontWeight.Black,
//            color = MaterialTheme.colorScheme.onSurfaceVariant,
//        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShadowRatioCircularTilePreview() {
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
                        ShadowRatio(currentShadowRatio = 0.655, peakShadowRatio = 2.5)
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
                        ShadowRatio(currentShadowRatio = MAX_SHADOW_RATIO, peakShadowRatio = 2.5)
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
                        ShadowRatio(currentShadowRatio = 0.0, peakShadowRatio = 2.5)
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
                        ShadowRatio(currentShadowRatio = 0.655, peakShadowRatio = 2.5)
                    }
                }
            }
        }
    }
}
