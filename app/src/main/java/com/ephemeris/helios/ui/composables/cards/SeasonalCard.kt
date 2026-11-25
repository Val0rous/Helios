package com.ephemeris.helios.ui.composables.cards

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.EphemerisType
import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.location.Coordinates
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class SeasonalMilestone(
    @param:StringRes val labelRes: Int,
    val dateTime: ZonedDateTime,
    val daylightHours: Double,
    val maxSolarAltitude: Double,
)

@Composable
fun SeasonalCard(
    marchEquinox: ZonedDateTime,
    marchDaylight: Double,
    marchSunAngle: Double,
    juneSolstice: ZonedDateTime,
    juneDaylight: Double,
    juneSunAngle: Double,
    septemberEquinox: ZonedDateTime,
    septemberDaylight: Double,
    septemberSunAngle: Double,
    decemberSolstice: ZonedDateTime,
    decemberDaylight: Double,
    decemberSunAngle: Double,
    modifier: Modifier = Modifier,
    currentTime: ZonedDateTime? = null,
    coordinates: Coordinates? = null,
    followApparentHorizonSetting: Boolean = true,
    onEventClick: ((ZonedDateTime) -> Unit)? = null,
) {
    val locale = LocalLocale.current.platformLocale
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val accentColor = MaterialTheme.colorScheme.primary
    val cardTint = accentColor.copy(alpha = if (isDarkSurface) 0.15f else 0.12f)
    val year = marchEquinox.year

    val horizonThresholdDeg = resolveCulminationHorizonThreshold(
        type = EphemerisType.SUN,
        coordinates = coordinates,
        followApparentHorizonSetting = followApparentHorizonSetting,
    )

    val marchEvent = SeasonalMilestone(R.string.equinox, marchEquinox, marchDaylight, marchSunAngle)
    val juneEvent = SeasonalMilestone(R.string.solstice, juneSolstice, juneDaylight, juneSunAngle)
    val septEvent = SeasonalMilestone(R.string.equinox, septemberEquinox, septemberDaylight, septemberSunAngle)
    val decEvent = SeasonalMilestone(R.string.solstice, decemberSolstice, decemberDaylight, decemberSunAngle)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(228.dp)
                .background(cardTint)
                .padding(horizontal = 11.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header: leading calendar icon + title + compact reference year chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar_month_filled),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    text = stringResource(R.string.equinoxes_and_solstices),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.5.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = if (isDarkSurface) 0.18f else 0.13f),
                ) {
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                            color = accentColor,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2x2 grid: chronological left-to-right, top-to-bottom (Mar -> Jun -> Sep -> Dec),
            // keeping Equinoxes stacked in column 1 and Solstices stacked in column 2.
            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    SeasonalMilestonePill(
                        milestone = marchEvent,
                        locale = locale,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        horizonThresholdDeg = horizonThresholdDeg,
                        isActive = currentTime?.toLocalDate() == marchEvent.dateTime.toLocalDate(),
                        onClick = onEventClick?.let { callback -> { callback(marchEvent.dateTime) } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                    SeasonalMilestonePill(
                        milestone = juneEvent,
                        locale = locale,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        horizonThresholdDeg = horizonThresholdDeg,
                        isActive = currentTime?.toLocalDate() == juneEvent.dateTime.toLocalDate(),
                        onClick = onEventClick?.let { callback -> { callback(juneEvent.dateTime) } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    SeasonalMilestonePill(
                        milestone = septEvent,
                        locale = locale,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        horizonThresholdDeg = horizonThresholdDeg,
                        isActive = currentTime?.toLocalDate() == septEvent.dateTime.toLocalDate(),
                        onClick = onEventClick?.let { callback -> { callback(septEvent.dateTime) } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                    SeasonalMilestonePill(
                        milestone = decEvent,
                        locale = locale,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        horizonThresholdDeg = horizonThresholdDeg,
                        isActive = currentTime?.toLocalDate() == decEvent.dateTime.toLocalDate(),
                        onClick = onEventClick?.let { callback -> { callback(decEvent.dateTime) } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonalMilestonePill(
    milestone: SeasonalMilestone,
    locale: Locale,
    accentColor: Color,
    surfaceColor: Color,
    isDarkSurface: Boolean,
    horizonThresholdDeg: Double,
    isActive: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val datePattern = remember(locale) {
        DateFormat.getBestDateTimePattern(locale, "MMMd")
    }
    val dateFormatter = remember(datePattern, locale) {
        DateTimeFormatter.ofPattern(datePattern, locale)
    }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }

    val formattedDate = milestone.dateTime.format(dateFormatter)
    val formattedTime = milestone.dateTime.toLocalTime()
        .format(timeFormatter)
        .replace("\u202F", " ")
    val formattedDaylight = milestone.daylightHours.formatDuration()

    val isBelowHorizon = isCulminationBelowHorizon(milestone.maxSolarAltitude, horizonThresholdDeg)
    val formattedAltitude = if (!milestone.maxSolarAltitude.isNaN()) {
        val rounded = (milestone.maxSolarAltitude * 10.0).roundToInt() / 10.0
        val sign = if (rounded < 0.0) "\u2212" else ""
        "$sign${String.format(locale, "%.1f", abs(rounded))}°"
    } else {
        "--°"
    }

    val containerColor = if (isActive) {
        accentColor.copy(alpha = if (isDarkSurface) 0.28f else 0.22f)
    } else {
        surfaceColor.copy(alpha = if (isDarkSurface) 0.44f else 0.66f)
    }
    val border = if (isActive) {
        BorderStroke(1.5.dp, accentColor.copy(alpha = if (isDarkSurface) 0.85f else 0.72f))
    } else {
        null
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val belowHorizonRed = if (isDarkSurface) Color(0xFFFF8A80) else Color(0xFFC62828)
    val trimLineHeight = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    )

    val dateTimeText = buildAnnotatedString {
        append(formattedDate)
        withStyle(
            SpanStyle(
                color = onSurfaceVariant.copy(alpha = 0.55f),
                fontWeight = FontWeight.Medium,
            ),
        ) {
            append(" · ")
        }
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(formattedTime)
        }
    }

    val metricsColor = if (isActive) accentColor else onSurfaceVariant
    val altitudeColor = if (isBelowHorizon) belowHorizonRed else metricsColor
    val metricsText = buildAnnotatedString {
        append(formattedDaylight)
        withStyle(SpanStyle(color = metricsColor.copy(alpha = 0.55f))) {
            append(" · ")
        }
        withStyle(
            SpanStyle(
                color = altitudeColor,
                fontWeight = if (isBelowHorizon) FontWeight.Bold else FontWeight.SemiBold,
            ),
        ) {
            append(formattedAltitude)
        }
    }

    val pillContent: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = stringResource(milestone.labelRes),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 12.sp,
                    lineHeightStyle = trimLineHeight,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    color = accentColor.copy(alpha = if (isActive) 1f else 0.85f),
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = dateTimeText,
                style = MaterialTheme.typography.titleMedium.copy(
                    lineHeight = 1.08.em,
                    lineHeightStyle = trimLineHeight,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    letterSpacing = (-0.2).sp,
                    color = onSurface,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 11.sp,
                    maxFontSize = 14.5.sp,
                    stepSize = 0.5.sp,
                ),
            )
            Text(
                text = metricsText,
                style = MaterialTheme.typography.labelMedium.copy(
                    lineHeight = 14.sp,
                    lineHeightStyle = trimLineHeight,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                    color = metricsColor,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 9.5.sp,
                    maxFontSize = 11.5.sp,
                    stepSize = 0.5.sp,
                ),
            )
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            border = border,
            modifier = modifier,
            content = pillContent,
        )
    } else {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            border = border,
            modifier = modifier,
            content = pillContent,
        )
    }
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun SeasonalPreviewLight() {
    val marchEq = ZonedDateTime.of(2026, 3, 20, 15, 46, 0, 0, ZoneOffset.ofHours(1))
    val juneSol = ZonedDateTime.of(2026, 6, 21, 10, 24, 0, 0, ZoneOffset.ofHours(2))
    val septEq = ZonedDateTime.of(2026, 9, 23, 2, 5, 0, 0, ZoneOffset.ofHours(2))
    val decSol = ZonedDateTime.of(2026, 12, 21, 21, 50, 0, 0, ZoneOffset.ofHours(1))

    HeliosTheme(darkTheme = false, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(Color(0xFFECEEF4))
                .padding(vertical = 16.dp),
        ) {
            // Mid-latitude (Rome)
            SeasonalCard(
                marchEquinox = marchEq,
                marchDaylight = 12.15,
                marchSunAngle = 48.1,
                juneSolstice = juneSol,
                juneDaylight = 15.24,
                juneSunAngle = 71.5,
                septemberEquinox = septEq,
                septemberDaylight = 12.14,
                septemberSunAngle = 48.0,
                decemberSolstice = decSol,
                decemberDaylight = 9.12,
                decemberSunAngle = 24.7,
                currentTime = marchEq,
                onEventClick = {},
            )

            // Arctic (Svalbard — Polar Night December Solstice altitude in red)
            SeasonalCard(
                marchEquinox = marchEq,
                marchDaylight = 12.42,
                marchSunAngle = 11.8,
                juneSolstice = juneSol,
                juneDaylight = 24.0,
                juneSunAngle = 35.2,
                septemberEquinox = septEq,
                septemberDaylight = 12.40,
                septemberSunAngle = 11.8,
                decemberSolstice = decSol,
                decemberDaylight = 0.0,
                decemberSunAngle = -11.6,
                currentTime = decSol,
                onEventClick = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun SeasonalPreviewDark() {
    val marchEq = ZonedDateTime.of(2026, 3, 20, 15, 46, 0, 0, ZoneOffset.ofHours(1))
    val juneSol = ZonedDateTime.of(2026, 6, 21, 10, 24, 0, 0, ZoneOffset.ofHours(2))
    val septEq = ZonedDateTime.of(2026, 9, 23, 2, 5, 0, 0, ZoneOffset.ofHours(2))
    val decSol = ZonedDateTime.of(2026, 12, 21, 21, 50, 0, 0, ZoneOffset.ofHours(1))

    HeliosTheme(darkTheme = true, dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(Color(0xFF0E1015))
                .padding(vertical = 16.dp),
        ) {
            SeasonalCard(
                marchEquinox = marchEq,
                marchDaylight = 12.15,
                marchSunAngle = 48.1,
                juneSolstice = juneSol,
                juneDaylight = 15.24,
                juneSunAngle = 71.5,
                septemberEquinox = septEq,
                septemberDaylight = 12.14,
                septemberSunAngle = 48.0,
                decemberSolstice = decSol,
                decemberDaylight = 9.12,
                decemberSunAngle = 24.7,
                currentTime = juneSol,
                onEventClick = {},
            )
        }
    }
}
