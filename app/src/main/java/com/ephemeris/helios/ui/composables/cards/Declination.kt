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
import com.ephemeris.helios.ui.composables.charts.MAX_LUNAR_DECLINATION_DEG
import com.ephemeris.helios.ui.composables.charts.MAX_SOLAR_DECLINATION_DEG
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.MetricModePill
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SeasonalEphemeris
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val SEASONAL_EVENT_HALF_WINDOW_SECONDS = 12L * 3600L

private fun isWithin24hOf(time: ZonedDateTime, event: ZonedDateTime): Boolean =
    abs(ChronoUnit.SECONDS.between(time.toInstant(), event.toInstant())) <= SEASONAL_EVENT_HALF_WINDOW_SECONDS

/**
 * Returns `true` when [currentTime] falls within `±6h` of a monthly North Lunistice
 * (strict local maximum of lunar declination during the 27.3-day tropical month).
 */
private fun isNearNorthLunistice(dec: Double, currentTime: ZonedDateTime): Boolean {
    if (dec < 17.5) return false
    val risingBefore = LunarEphemeris.calculateDeclinationDeg(currentTime.minusHours(6).minusMinutes(15)) <=
        LunarEphemeris.calculateDeclinationDeg(currentTime.minusHours(6).plusMinutes(15))
    val fallingAfter = LunarEphemeris.calculateDeclinationDeg(currentTime.plusHours(6).minusMinutes(15)) >=
        LunarEphemeris.calculateDeclinationDeg(currentTime.plusHours(6).plusMinutes(15))
    return risingBefore && fallingAfter
}

/**
 * Returns `true` when [currentTime] falls within `±6h` of a monthly South Lunistice
 * (strict local minimum of lunar declination during the 27.3-day tropical month).
 */
private fun isNearSouthLunistice(dec: Double, currentTime: ZonedDateTime): Boolean {
    if (dec > -17.5) return false
    val fallingBefore = LunarEphemeris.calculateDeclinationDeg(currentTime.minusHours(6).minusMinutes(15)) >=
        LunarEphemeris.calculateDeclinationDeg(currentTime.minusHours(6).plusMinutes(15))
    val risingAfter = LunarEphemeris.calculateDeclinationDeg(currentTime.plusHours(6).minusMinutes(15)) <=
        LunarEphemeris.calculateDeclinationDeg(currentTime.plusHours(6).plusMinutes(15))
    return fallingBefore && risingAfter
}

@Composable
fun Declination(
    currentDeclinationDeg: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier,
    seasonalEvents: SeasonalEphemeris.SeasonalEvents? = null,
    isLunar: Boolean = false,
    geocentricDeclinationDeg: Double = currentDeclinationDeg,
    modeLabel: String? = null,
    onToggleMode: (() -> Unit)? = null,
) {
    val locale = LocalLocale.current.platformLocale
    val dec = currentDeclinationDeg

    val description = if (isLunar) {
        when {
            isNearNorthLunistice(geocentricDeclinationDeg, currentTime) -> "North Lunistice"
            isNearSouthLunistice(geocentricDeclinationDeg, currentTime) -> "South Lunistice"
            abs(dec) < 0.25 -> "Celestial Equator"
            dec >= 25.0 -> "Extreme North"
            dec >= 20.0 -> "High North"
            dec >= 13.0 -> "Mid North"
            dec >= 5.0 -> "Low North"
            dec > 0.0 -> "North of Equator"
            dec <= -25.0 -> "Extreme South"
            dec <= -20.0 -> "High South"
            dec <= -13.0 -> "Mid South"
            dec <= -5.0 -> "Low South"
            else -> "South of Equator"
        }
    } else {
        val resolvedEvents = seasonalEvents ?: remember(currentTime.year, currentTime.zone) {
            SeasonalEphemeris.getSeasonalEvents(currentTime.year, currentTime.zone)
        }
        when {
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
    }

    val hemisphere = when {
        abs(dec) < 0.005 -> ""
        dec > 0.0 -> "N"
        else -> "S"
    }
    val formattedValue = "${String.format(locale, "%.2f", abs(dec))}°"

    DeclinationPill(
        declination = dec.toFloat(),
        maxDeclinationDeg = if (isLunar) MAX_LUNAR_DECLINATION_DEG else MAX_SOLAR_DECLINATION_DEG,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                        .weight(1f, fill = false)
                        .padding(end = if (modeLabel != null) 4.dp else 0.dp),
                )
                if (modeLabel != null && onToggleMode != null) {
                    MetricModePill(
                        label = modeLabel,
                        onClick = onToggleMode,
                    )
                }
            }
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
                        currentDeclinationDeg = 27.42,
                        currentTime = midSpring,
                        isLunar = true,
                        modeLabel = "Topo",
                        onToggleMode = {},
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
                        isLunar = true,
                        modeLabel = "Geo",
                        onToggleMode = {},
                    )
                }
            }
        }
    }
}
