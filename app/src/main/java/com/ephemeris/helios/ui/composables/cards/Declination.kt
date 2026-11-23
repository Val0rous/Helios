package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.DeclinationPill
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.calc.SeasonalEphemeris
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val SEASONAL_EVENT_HALF_WINDOW_SECONDS = 12L * 3600L

private fun isWithin24hOf(time: ZonedDateTime, event: ZonedDateTime): Boolean =
    abs(ChronoUnit.SECONDS.between(time.toInstant(), event.toInstant())) <= SEASONAL_EVENT_HALF_WINDOW_SECONDS

@Composable
fun Declination(
    currentDeclinationDeg: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier,
    seasonalEvents: SeasonalEphemeris.SeasonalEvents? = null,
) {
    val locale = LocalLocale.current.platformLocale
    val resolvedEvents = seasonalEvents ?: remember(currentTime.year, currentTime.zone) {
        SeasonalEphemeris.getSeasonalEvents(currentTime.year, currentTime.zone)
    }

    val dec = currentDeclinationDeg
    val description = when {
        isWithin24hOf(currentTime, resolvedEvents.marchEquinox) -> "March Equinox"
        isWithin24hOf(currentTime, resolvedEvents.juneSolstice) -> "June Solstice"
        isWithin24hOf(currentTime, resolvedEvents.septemberEquinox) -> "September Equinox"
        isWithin24hOf(currentTime, resolvedEvents.decemberSolstice) -> "December Solstice"
        dec >= 22.0 -> "Peak North"
        dec >= 17.5 -> "High North"
        dec >= 11.5 -> "Mid North"
        dec >= 5.0 -> "Low North"
        dec > 0.0 -> "North of Equator"
        dec <= -22.0 -> "Peak South"
        dec <= -17.5 -> "High South"
        dec <= -11.5 -> "Mid South"
        dec <= -5.0 -> "Low South"
        dec < 0.0 -> "South of Equator"
        else -> "Celestial Equator"
    }

    val hemisphere = when {
        abs(dec) < 0.005 -> ""
        dec > 0.0 -> "N"
        else -> "S"
    }
    val formattedValue = "${String.format(locale, "%.2f", abs(dec))}°"

    DeclinationPill(
        declination = dec.toFloat(),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            ) {
                HeaderEntry(
                    text = stringResource(R.string.declination),
                    icon = R.drawable.ic_globe,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    iconSize = 20.dp,
                    textSize = 16.sp,
                    spacing = 8.dp,
                )
            }

            val valueStyle = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                fontFeatureSettings = "tnum",
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text(
                    text = formattedValue,
                    style = valueStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
                if (hemisphere.isNotEmpty()) {
                    Text(
                        text = hemisphere,
                        style = valueStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }

            Text(
                text = description,
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
}

@Preview(showBackground = true)
@Composable
private fun DeclinationWavyTilePreview() {
    val juneSolstice = ZonedDateTime.of(2026, 6, 21, 8, 24, 0, 0, ZoneOffset.UTC)
    val midSpring = ZonedDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC)
    val marchEquinox = ZonedDateTime.of(2026, 3, 20, 14, 46, 0, 0, ZoneOffset.UTC)
    val lateAutumn = ZonedDateTime.of(2026, 11, 22, 12, 0, 0, 0, ZoneOffset.UTC)

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
                    Declination(
                        currentDeclinationDeg = 23.436,
                        currentTime = juneSolstice,
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Declination(
                        currentDeclinationDeg = 15.12,
                        currentTime = midSpring,
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
                    Declination(
                        currentDeclinationDeg = 0.0,
                        currentTime = marchEquinox,
                    )
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Declination(
                        currentDeclinationDeg = -20.18,
                        currentTime = lateAutumn,
                    )
                }
            }
        }
    }
}
