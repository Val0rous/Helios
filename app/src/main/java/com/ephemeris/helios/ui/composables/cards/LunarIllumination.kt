package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.ui.composables.charts.drawMoonPhaseIcon
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.calc.MoonMetrics

@Composable
fun LunarIllumination(
    illuminationPercent: Double,
    phase: MoonMetrics.LunarPhase,
    ageDays: Double,
    moonPhase: MoonPhaseAppearance,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val customColors = LocalCustomColors.current
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val moonPath = remember { Path() }

    // Tinted like the Moon icon while preserving high legibility for centered text in both themes
    val discFillColor = if (isDarkSurface) {
        customColors.moon.copy(alpha = 0.28f)
    } else {
        lerp(Color.White, customColors.moon, 0.25f)
    }
    val discUnlitColor = if (isDarkSurface) {
        customColors.moonUnlit.copy(alpha = 0.35f)
    } else {
        customColors.moonUnlit.copy(alpha = 0.28f)
    }
    val discBorderColor = customColors.moonBorder.copy(alpha = if (isDarkSurface) 0.85f else 0.9f)

    val formattedPercent = String.format(locale, "%.2f", illuminationPercent.coerceIn(0.0, 100.0))
    val formattedAge = String.format(locale, "Day %.2f", ageDays.coerceAtLeast(0.0))

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Big Moon phase disc matching ShadowDial's outer circular footprint
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMoonPhaseIcon(
                iconSize = size.minDimension,
                phase = moonPhase,
                fillColor = discFillColor,
                borderColor = discBorderColor,
                backgroundColor = discUnlitColor,
                isDimmed = false,
                reusablePath = moonPath,
                outerRadiusRatio = 0.5f,
                strokeWidthPx = 4.dp.toPx(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp),
            ) {
                val titleColor = customColors.moonPath
                HeaderEntry(
                    text = stringResource(R.string.illumination),
                    textColor = titleColor,
                    icon = R.drawable.ic_tonality_filled,
                    iconTint = titleColor,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    iconSize = 17.dp,
                    textSize = 14.sp,
                    spacing = 4.dp,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 8.dp),
            ) {
                Text(
                    text = formattedPercent,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum",
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 17.dp),
            ) {
                Text(
                    text = phase.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = formattedAge,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LunarIlluminationCircularTilePreview() {
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
                    LunarIllumination(
                        illuminationPercent = 18.45,
                        phase = MoonMetrics.LunarPhase.WAXING_CRESCENT,
                        ageDays = 4.24,
                        moonPhase = MoonPhaseAppearance(0.1845f, isWaxing = true),
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    LunarIllumination(
                        illuminationPercent = 100.00,
                        phase = MoonMetrics.LunarPhase.FULL_MOON,
                        ageDays = 14.82,
                        moonPhase = MoonPhaseAppearance(1.0f, isWaxing = true),
                    )
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
                    LunarIllumination(
                        illuminationPercent = 50.00,
                        phase = MoonMetrics.LunarPhase.FIRST_QUARTER,
                        ageDays = 7.43,
                        moonPhase = MoonPhaseAppearance(0.5f, isWaxing = true),
                    )
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    LunarIllumination(
                        illuminationPercent = 74.28,
                        phase = MoonMetrics.LunarPhase.WANING_GIBBOUS,
                        ageDays = 19.65,
                        moonPhase = MoonPhaseAppearance(0.7428f, isWaxing = false),
                    )
                }
            }
        }
    }
}
