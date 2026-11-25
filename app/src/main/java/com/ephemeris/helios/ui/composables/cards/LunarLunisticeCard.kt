package com.ephemeris.helios.ui.composables.cards

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.EphemerisType
import com.ephemeris.helios.utils.calc.LunarLunisticeEphemeris.LunisticeEvent
import com.ephemeris.helios.utils.calc.LunarLunisticeEphemeris.StandstillCyclePhase
import com.ephemeris.helios.utils.calc.LunarLunisticeEphemeris.YearlyLunistices
import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.location.Coordinates
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private fun formatCycleStatus(lunistices: YearlyLunistices, locale: Locale): Pair<String, Boolean> {
    fun fmt(v: Double): String = "${String.format(locale, "%.1f", abs(v))}°"
    val peakAbs = max(abs(lunistices.maxNorth.declinationDeg), abs(lunistices.maxSouth.declinationDeg))
    val minAbs = min(abs(lunistices.minNorth.declinationDeg), abs(lunistices.minSouth.declinationDeg))
    val range = "${fmt(lunistices.cycleStartAbsDecDeg)} → ${fmt(lunistices.cycleEndAbsDecDeg)}"

    return when (lunistices.cyclePhase) {
        StandstillCyclePhase.MAJOR_STANDSTILL ->
            "Major Standstill · 18.6y peak (±${fmt(peakAbs)})" to true
        StandstillCyclePhase.MINOR_STANDSTILL ->
            "Minor Standstill · 18.6y low (±${fmt(minAbs)})" to true
        StandstillCyclePhase.NEAR_MAJOR_WIDENING ->
            "Approaching Major · $range" to false
        StandstillCyclePhase.NEAR_MAJOR_NARROWING ->
            "Post-Major · $range" to false
        StandstillCyclePhase.NEAR_MINOR_NARROWING ->
            "Approaching Minor · $range" to false
        StandstillCyclePhase.NEAR_MINOR_WIDENING ->
            "Post-Minor · $range" to false
        StandstillCyclePhase.WIDENING ->
            "18.6y cycle · Widening $range" to false
        StandstillCyclePhase.NARROWING ->
            "18.6y cycle · Narrowing $range" to false
    }
}

private fun formatSignedDeclination(declinationDeg: Double, locale: Locale): String {
    val rounded = (declinationDeg * 10.0).roundToInt() / 10.0
    val sign = if (rounded < 0.0) "\u2212" else "+"
    return "$sign${String.format(locale, "%.1f", abs(rounded))}°"
}

private fun formatAltitudeDeg(altitudeDeg: Double?, locale: Locale): String {
    if (altitudeDeg == null || altitudeDeg.isNaN()) return "--°"
    val rounded = (altitudeDeg * 10.0).roundToInt() / 10.0
    val sign = if (rounded < 0.0) "\u2212" else ""
    return "$sign${String.format(locale, "%.1f", abs(rounded))}°"
}

@Composable
fun LunarLunisticeCard(
    lunistices: YearlyLunistices,
    modifier: Modifier = Modifier,
    currentTime: ZonedDateTime? = null,
    coordinates: Coordinates? = null,
    followApparentHorizonSetting: Boolean = true,
    initialExpanded: Boolean = false,
    onEventClick: ((ZonedDateTime) -> Unit)? = null,
) {
    val locale = LocalLocale.current.platformLocale
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val accentColor = LocalCustomColors.current.nightPrimary
    val tintSeed = Color(0xFF5E5791)
    val cardTint = tintSeed.copy(alpha = if (isDarkSurface) 0.15f else 0.12f)

    var isExpanded by rememberSaveable { mutableStateOf(initialExpanded) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "lunisticeChevronRotation",
    )

    val horizonThresholdDeg = resolveCulminationHorizonThreshold(
        type = EphemerisType.MOON,
        coordinates = coordinates,
        followApparentHorizonSetting = followApparentHorizonSetting,
    )

    val datePattern = remember(locale) {
        DateFormat.getBestDateTimePattern(locale, "MMMd")
    }
    val dateFormatter = remember(datePattern, locale) {
        DateTimeFormatter.ofPattern(datePattern, locale)
    }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }

    val (cycleLabel, isStandoutStandstill) = formatCycleStatus(lunistices, locale)
    val hasFullList = lunistices.allLunistices.isNotEmpty()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    text = stringResource(R.string.lunar_lunistices),
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
                        text = lunistices.year.toString(),
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

            Spacer(modifier = Modifier.height(6.dp))

            // 18.6-year nodal cycle standstill callout / annual drift pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isStandoutStandstill) {
                    accentColor.copy(alpha = if (isDarkSurface) 0.22f else 0.15f)
                } else {
                    surfaceColor.copy(alpha = if (isDarkSurface) 0.28f else 0.45f)
                },
                border = if (isStandoutStandstill) {
                    BorderStroke(1.dp, accentColor.copy(alpha = if (isDarkSurface) 0.55f else 0.38f))
                } else {
                    null
                },
            ) {
                Text(
                    text = cycleLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        fontWeight = if (isStandoutStandstill) FontWeight.Bold else FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                        color = if (isStandoutStandstill) {
                            accentColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2x2 grid: Row 1 = Extreme North & South of the year; Row 2 = Shallowest North & South
            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                ) {
                    LunisticeMilestonePill(
                        labelRes = R.string.max_north,
                        event = lunistices.maxNorth,
                        locale = locale,
                        dateFormatter = dateFormatter,
                        timeFormatter = timeFormatter,
                        accentColor = accentColor,
                        tintSeed = tintSeed,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        horizonThresholdDeg = horizonThresholdDeg,
                        isActive = currentTime?.toLocalDate() == lunistices.maxNorth.dateTime.toLocalDate(),
                        onClick = onEventClick?.let { callback -> { callback(lunistices.maxNorth.dateTime) } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                    LunisticeMilestonePill(
                        labelRes = R.string.max_south,
                        event = lunistices.maxSouth,
                        locale = locale,
                        dateFormatter = dateFormatter,
                        timeFormatter = timeFormatter,
                        accentColor = accentColor,
                        tintSeed = tintSeed,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        horizonThresholdDeg = horizonThresholdDeg,
                        isActive = currentTime?.toLocalDate() == lunistices.maxSouth.dateTime.toLocalDate(),
                        onClick = onEventClick?.let { callback -> { callback(lunistices.maxSouth.dateTime) } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                ) {
                    LunisticeMilestonePill(
                        labelRes = R.string.min_north,
                        event = lunistices.minNorth,
                        locale = locale,
                        dateFormatter = dateFormatter,
                        timeFormatter = timeFormatter,
                        accentColor = accentColor,
                        tintSeed = tintSeed,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        horizonThresholdDeg = horizonThresholdDeg,
                        isActive = currentTime?.toLocalDate() == lunistices.minNorth.dateTime.toLocalDate(),
                        onClick = onEventClick?.let { callback -> { callback(lunistices.minNorth.dateTime) } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                    LunisticeMilestonePill(
                        labelRes = R.string.min_south,
                        event = lunistices.minSouth,
                        locale = locale,
                        dateFormatter = dateFormatter,
                        timeFormatter = timeFormatter,
                        accentColor = accentColor,
                        tintSeed = tintSeed,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        horizonThresholdDeg = horizonThresholdDeg,
                        isActive = currentTime?.toLocalDate() == lunistices.minSouth.dateTime.toLocalDate(),
                        onClick = onEventClick?.let { callback -> { callback(lunistices.minSouth.dateTime) } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                }
            }

            if (hasFullList) {
                Spacer(modifier = Modifier.height(8.dp))

                // Expand / collapse toggle right beneath the 2x2 extremes grid so collapsing never requires scrolling down
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Surface(
                        onClick = { isExpanded = !isExpanded },
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = if (isDarkSurface) 0.16f else 0.11f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = if (isExpanded) {
                                    stringResource(R.string.show_less)
                                } else {
                                    stringResource(R.string.show_all_lunistices, lunistices.totalLunisticeCount)
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.5.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum",
                                    color = accentColor,
                                ),
                                maxLines = 1,
                                softWrap = false,
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(chevronRotation),
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        for (event in lunistices.allLunistices) {
                            val extremumBadge = when (event.dateTime) {
                                lunistices.maxNorth.dateTime,
                                lunistices.maxSouth.dateTime,
                                -> "Max"
                                lunistices.minNorth.dateTime,
                                lunistices.minSouth.dateTime,
                                -> "Min"
                                else -> null
                            }
                            LunisticeTimelineRow(
                                event = event,
                                extremumBadge = extremumBadge,
                                locale = locale,
                                dateFormatter = dateFormatter,
                                timeFormatter = timeFormatter,
                                accentColor = accentColor,
                                tintSeed = tintSeed,
                                surfaceColor = surfaceColor,
                                isDarkSurface = isDarkSurface,
                                horizonThresholdDeg = horizonThresholdDeg,
                                isActive = currentTime?.toLocalDate() == event.dateTime.toLocalDate(),
                                onClick = onEventClick?.let { callback -> { callback(event.dateTime) } },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LunisticeTimelineRow(
    event: LunisticeEvent,
    extremumBadge: String?,
    locale: Locale,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    accentColor: Color,
    tintSeed: Color,
    surfaceColor: Color,
    isDarkSurface: Boolean,
    horizonThresholdDeg: Double,
    isActive: Boolean,
    onClick: (() -> Unit)?,
) {
    val formattedDate = event.dateTime.format(dateFormatter)
    val formattedTime = event.dateTime.toLocalTime()
        .format(timeFormatter)
        .replace("\u202F", " ")
    val formattedDec = formatSignedDeclination(event.declinationDeg, locale)
    val formattedUptime = event.moonUptimeHours.formatDuration()
    val isBelowHorizon = isCulminationBelowHorizon(event.maxAltitudeDeg, horizonThresholdDeg)
    val formattedAltitude = formatAltitudeDeg(event.maxAltitudeDeg, locale)

    val containerColor = if (isActive) {
        tintSeed.copy(alpha = if (isDarkSurface) 0.30f else 0.24f)
    } else {
        surfaceColor.copy(alpha = if (isDarkSurface) 0.38f else 0.58f)
    }
    val border = if (isActive) {
        BorderStroke(1.5.dp, accentColor.copy(alpha = if (isDarkSurface) 0.85f else 0.72f))
    } else {
        null
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val belowHorizonRed = if (isDarkSurface) Color(0xFFFF8A80) else Color(0xFFC62828)
    val metricsColor = if (isActive) accentColor else onSurfaceVariant
    val altitudeColor = if (isBelowHorizon) belowHorizonRed else metricsColor

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
        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
            append(formattedTime)
        }
    }

    val metricsText = buildAnnotatedString {
        append(formattedUptime)
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

    val rowContent: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 7.dp),
        ) {
            // Fixed-width N/S + declination chip for crisp vertical column alignment
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = accentColor.copy(
                    alpha = when {
                        event.isNorth -> if (isDarkSurface) 0.22f else 0.15f
                        else -> if (isDarkSurface) 0.12f else 0.08f
                    },
                ),
            ) {
                Text(
                    text = "${if (event.isNorth) "N" else "S"} $formattedDec",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                        color = accentColor,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .width(62.dp)
                        .padding(horizontal = 4.dp, vertical = 2.5.dp),
                )
            }

            // Date & time + optional Max/Min tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = dateTimeText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                        letterSpacing = (-0.15).sp,
                        color = onSurface,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 10.sp,
                        maxFontSize = 12.5.sp,
                        stepSize = 0.5.sp,
                    ),
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (extremumBadge != null) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = accentColor.copy(alpha = if (isDarkSurface) 0.24f else 0.16f),
                    ) {
                        Text(
                            text = extremumBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }

            // Uptime + max altitude
            Text(
                text = metricsText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                    color = metricsColor,
                ),
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
            )
        }
    }

    if (onClick != null) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(11.dp),
                color = containerColor,
                border = border,
                modifier = Modifier.fillMaxWidth(),
                content = rowContent,
            )
        }
    } else {
        Surface(
            shape = RoundedCornerShape(11.dp),
            color = containerColor,
            border = border,
            modifier = Modifier.fillMaxWidth(),
            content = rowContent,
        )
    }
}

@Composable
private fun LunisticeMilestonePill(
    @StringRes labelRes: Int,
    event: LunisticeEvent,
    locale: Locale,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    accentColor: Color,
    tintSeed: Color,
    surfaceColor: Color,
    isDarkSurface: Boolean,
    horizonThresholdDeg: Double,
    isActive: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val formattedDate = event.dateTime.format(dateFormatter)
    val formattedTime = event.dateTime.toLocalTime()
        .format(timeFormatter)
        .replace("\u202F", " ")
    val formattedUptime = event.moonUptimeHours.formatDuration()
    val formattedDec = formatSignedDeclination(event.declinationDeg, locale)
    val isBelowHorizon = isCulminationBelowHorizon(event.maxAltitudeDeg, horizonThresholdDeg)
    val formattedAltitude = formatAltitudeDeg(event.maxAltitudeDeg, locale)

    val containerColor = if (isActive) {
        tintSeed.copy(alpha = if (isDarkSurface) 0.30f else 0.24f)
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

    val labelColor = accentColor.copy(alpha = if (isActive) 1f else 0.88f)
    val eyebrowText = buildAnnotatedString {
        append(stringResource(labelRes))
        withStyle(SpanStyle(color = labelColor.copy(alpha = 0.55f))) {
            append(" · ")
        }
        append(formattedDec)
    }

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
        append(formattedUptime)
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
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            Text(
                text = eyebrowText,
                style = MaterialTheme.typography.labelSmall.copy(
                    lineHeight = 12.sp,
                    lineHeightStyle = trimLineHeight,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    letterSpacing = 0.2.sp,
                    color = labelColor,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 9.sp,
                    maxFontSize = 10.5.sp,
                    stepSize = 0.5.sp,
                ),
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

private fun sampleMajor2025(): YearlyLunistices {
    val e1 = LunisticeEvent(ZonedDateTime.of(2025, 1, 11, 22, 18, 0, 0, ZoneOffset.ofHours(1)), 28.51, 16.33, 76.0)
    val e2 = LunisticeEvent(ZonedDateTime.of(2025, 1, 26, 9, 44, 0, 0, ZoneOffset.ofHours(1)), -28.54, 7.88, 19.1)
    val maxN = LunisticeEvent(ZonedDateTime.of(2025, 3, 7, 15, 22, 0, 0, ZoneOffset.ofHours(1)), 28.72, 16.45, 76.2)
    val maxS = LunisticeEvent(ZonedDateTime.of(2025, 3, 22, 11, 48, 0, 0, ZoneOffset.ofHours(1)), -28.72, 7.82, 18.9)
    val minN = LunisticeEvent(ZonedDateTime.of(2025, 12, 5, 17, 10, 0, 0, ZoneOffset.ofHours(1)), 28.25, 16.22, 75.7)
    val minS = LunisticeEvent(ZonedDateTime.of(2025, 12, 19, 22, 35, 0, 0, ZoneOffset.ofHours(1)), -28.18, 7.95, 19.5)
    return YearlyLunistices(
        year = 2025,
        maxNorth = maxN,
        maxSouth = maxS,
        minNorth = minN,
        minSouth = minS,
        cyclePhase = StandstillCyclePhase.MAJOR_STANDSTILL,
        cycleStartAbsDecDeg = 28.5,
        cycleEndAbsDecDeg = 28.2,
        totalLunisticeCount = 27,
        allLunistices = listOf(e1, e2, maxN, maxS, minN, minS),
    )
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun LunarLunisticePreviewLight() {
    val major2025 = sampleMajor2025()
    HeliosTheme(darkTheme = false, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(Color(0xFFECEEF4))
                .padding(vertical = 16.dp),
        ) {
            LunarLunisticeCard(
                lunistices = major2025,
                currentTime = major2025.maxNorth.dateTime,
                onEventClick = {},
            )
            LunarLunisticeCard(
                lunistices = major2025,
                currentTime = major2025.maxNorth.dateTime,
                initialExpanded = true,
                onEventClick = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun LunarLunisticePreviewDark() {
    val major2025 = sampleMajor2025()
    HeliosTheme(darkTheme = true, dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(Color(0xFF0E1015))
                .padding(vertical = 16.dp),
        ) {
            LunarLunisticeCard(
                lunistices = major2025,
                currentTime = major2025.maxSouth.dateTime,
                initialExpanded = true,
                onEventClick = {},
            )
        }
    }
}
