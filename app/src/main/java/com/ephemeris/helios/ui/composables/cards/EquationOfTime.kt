package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
// import com.ephemeris.helios.ui.composables.charts.AnalemmaIndicator
import com.ephemeris.helios.ui.composables.charts.EOT_FAST_COLOR
import com.ephemeris.helios.ui.composables.charts.EOT_MAX_MINUTES
import com.ephemeris.helios.ui.composables.charts.EOT_MIN_MINUTES
import com.ephemeris.helios.ui.composables.charts.EOT_SLOW_COLOR
import com.ephemeris.helios.ui.composables.charts.EquationOfTimeBar
import com.ephemeris.helios.ui.composables.charts.colorForEot
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EquationOfTime(
    currentEotMinutes: Double,
    @Suppress("UNUSED_PARAMETER") currentDeclinationDeg: Double,
    @Suppress("UNUSED_PARAMETER") currentTime: ZonedDateTime,
    modifier: Modifier = Modifier,
) {
    // 1. Determine the status description
    // Positive EoT = Solar Time is ahead of Mean Time (Sundial is Fast)
    // Negative EoT = Solar Time is behind Mean Time (Sundial is Slow)
    val eotFloat = currentEotMinutes.toFloat()
    val absEot = abs(eotFloat)

    // The Equation of Time ranges from ~-14.3m (Feb) to ~+16.4m (Nov)
    val status = when {
        absEot <= 0.5f -> "In Sync"
        eotFloat > 0f -> when {
            eotFloat >= 14.5f -> "Peak Fast"
            eotFloat > 10.0f -> "Very Fast"
            eotFloat > 5.0f -> "Moderately Fast"
            eotFloat > 1.5f -> "Slightly Fast"
            else -> "Barely Fast"
        }
        else -> when {
            eotFloat <= -13.5f -> "Peak Slow"
            eotFloat < -10.0f -> "Very Slow"
            eotFloat < -5.0f -> "Moderately Slow"
            eotFloat < -1.5f -> "Slightly Slow"
            else -> "Barely Slow"
        }
    }

    // 2. Flat Material-compliant tonal tint interpolated from red (fast) -> neutral -> blue (slow)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val neutralColor = lerp(surfaceColor, MaterialTheme.colorScheme.onSurfaceVariant, 0.38f)
    val activeColor = colorForEot(eotFloat, neutralColor)
    val maxTintAlpha = if (isDarkSurface) 0.28f else 0.22f
    val tintModifier = when {
        eotFloat > 0f -> {
            val strength = (eotFloat / EOT_MAX_MINUTES).coerceIn(0f, 1f)
            Modifier.background(EOT_FAST_COLOR.copy(alpha = maxTintAlpha * strength))
        }
        eotFloat < 0f -> {
            val strength = (-eotFloat / -EOT_MIN_MINUTES).coerceIn(0f, 1f)
            Modifier.background(EOT_SLOW_COLOR.copy(alpha = maxTintAlpha * strength))
        }
        else -> Modifier
    }

    // 3. Format the time delta (e.g., "+14m 22s")
    val absMinutes = abs(currentEotMinutes)
    var mins = absMinutes.toInt()
    var secs = ((absMinutes - mins) * 60.0).roundToInt()

    // Handle edge case where rounding pushes seconds to 60
    if (secs == 60) {
        mins += 1
        secs = 0
    }
    val sign = when {
        mins == 0 && secs == 0 -> ""
        currentEotMinutes >= 0.0 -> "+"
        else -> "\u2212" // True Unicode minus matching '+' advance width
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(tintModifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            val titleColor = MaterialTheme.colorScheme.onSurface
            HeaderEntry(
                text = stringResource(R.string.eq_of_time),
                textColor = titleColor,
                icon = R.drawable.ic_timer_filled,
                iconTint = titleColor,
                horizontalAlignment = Alignment.CenterHorizontally,
                iconSize = 20.dp,
                textSize = 16.sp,
                spacing = 8.dp,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart),
        ) {
            val valueStyle = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                fontFeatureSettings = "tnum",
                color = MaterialTheme.colorScheme.onSurface,
            )
            val unitSpan = SpanStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = buildAnnotatedString {
                    append("$sign$mins")
                    withStyle(unitSpan) { append("\u2009m ") }
                    append("$secs")
                    withStyle(unitSpan) { append("\u2009s") }
                },
                style = valueStyle,
                maxLines = 1,
                softWrap = false,
            )

            EquationOfTimeBar(
                eotMinutes = eotFloat,
                activeColor = activeColor,
                borderColor = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
            )
        }

        /*
        Text(
            text = "vs. Mean Time",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        AnalemmaIndicator(
            currentEot = currentEotMinutes.toFloat(),
            currentDeclination = currentDeclinationDeg.toFloat(),
            currentTime = currentTime,
            modifier = Modifier.width(48.dp).height(72.dp)
        )
        */

        Text(
            text = status,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EquationOfTimeTilePreview() {
    val sampleTime = ZonedDateTime.of(2026, 11, 3, 12, 0, 0, 0, ZoneOffset.UTC)

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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    EquationOfTime(
                        currentEotMinutes = 16.36,
                        currentDeclinationDeg = -15.0,
                        currentTime = sampleTime,
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    EquationOfTime(
                        currentEotMinutes = 0.25,
                        currentDeclinationDeg = 10.0,
                        currentTime = sampleTime,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    EquationOfTime(
                        currentEotMinutes = -7.45,
                        currentDeclinationDeg = -20.0,
                        currentTime = sampleTime,
                    )
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    EquationOfTime(
                        currentEotMinutes = -14.25,
                        currentDeclinationDeg = -14.0,
                        currentTime = sampleTime,
                    )
                }
            }
        }
    }
}
