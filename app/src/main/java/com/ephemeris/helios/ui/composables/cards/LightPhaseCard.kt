package com.ephemeris.helios.ui.composables.cards

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.GoogleSansFlexRounded
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.formatDuration
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

data class LightPhaseInterval(
    val startHour: Double?,
    val endHour: Double?,
    val durationHours: Double,
    val isActive: Boolean,
    val isAllDay: Boolean = false,
)

private fun normalizeHour24(hour: Double): Double {
    val mod = hour % 24.0
    return if (mod < 0.0) mod + 24.0 else mod
}

private fun Double?.toRoundedLocalTime(): LocalTime? {
    if (this == null || this.isNaN()) return null
    val hNorm = normalizeHour24(this)
    val h = hNorm.toInt()
    val m = ((hNorm - h) * 60.0).roundToInt()
    val finalH = if (m == 60) (h + 1) % 24 else h % 24
    val finalM = if (m == 60) 0 else m
    return LocalTime.of(finalH, finalM)
}

/**
 * Formats a `(startHour, endHour)` interval using the user's locale short time format,
 * collapsing duplicate AM/PM indicators when both endpoints fall in the same half of the day
 * (e.g., `6:12 – 7:10 AM`), while preserving both indicators across noon or midnight
 * (e.g., `10:30 AM – 12:10 PM`, `10:15 PM – 2:45 AM`).
 */
internal fun formatDecimalHourRange(
    startHour: Double?,
    endHour: Double?,
    locale: Locale = Locale.getDefault(),
): Pair<String, String> {
    val startTime = startHour.toRoundedLocalTime()
    val endTime = endHour.toRoundedLocalTime()
    if (startTime == null || endTime == null) {
        return startHour.formatDecimalHours() to endHour.formatDecimalHours()
    }

    val fullFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    val fullStart = startTime.format(fullFormatter).replace("\u202F", " ")
    val fullEnd = endTime.format(fullFormatter).replace("\u202F", " ")

    val pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        null,
        FormatStyle.SHORT,
        IsoChronology.INSTANCE,
        locale,
    )

    // Inspect unquoted pattern tokens to locate any AM/PM ('a') marker
    var amPmIndex = -1
    var hourIndex = -1
    var inQuote = false
    val strippedBuilder = StringBuilder(pattern.length)
    for (i in pattern.indices) {
        val c = pattern[i]
        if (c == '\'') {
            inQuote = !inQuote
            strippedBuilder.append(c)
        } else if (!inQuote && c == 'a') {
            if (amPmIndex == -1) amPmIndex = i
        } else {
            if (!inQuote && hourIndex == -1 && (c == 'h' || c == 'K' || c == 'H' || c == 'k')) {
                hourIndex = i
            }
            strippedBuilder.append(c)
        }
    }

    if (amPmIndex == -1) {
        return fullStart to fullEnd
    }

    val sameMeridiem = (startTime.hour < 12) == (endTime.hour < 12)
    if (!sameMeridiem) {
        return fullStart to fullEnd
    }

    val strippedPattern = strippedBuilder
        .toString()
        .replace('\u202F', ' ')
        .replace('\u00A0', ' ')
        .trim()
    val shortFormatter = runCatching {
        DateTimeFormatter.ofPattern(strippedPattern, locale)
    }.getOrNull() ?: return fullStart to fullEnd

    val isAmPmPrefix = hourIndex != -1 && amPmIndex < hourIndex
    return if (isAmPmPrefix) {
        fullStart to endTime.format(shortFormatter).replace("\u202F", " ").trim()
    } else {
        startTime.format(shortFormatter).replace("\u202F", " ").trim() to fullEnd
    }
}

internal fun isHourInInterval(currentHour: Double, startHour: Double, endHour: Double): Boolean {
    val cur = normalizeHour24(currentHour)
    if (endHour >= 24.0 && startHour < 24.0) {
        val start = normalizeHour24(startHour)
        val extraAfterMidnight = endHour - 24.0
        return cur >= start || (extraAfterMidnight > 0.0 && cur <= extraAfterMidnight)
    }
    val start = normalizeHour24(startHour)
    val end = normalizeHour24(endHour)
    return if (start <= end) {
        (cur in start..end)
    } else {
        // Overnight interval wrapping across midnight (e.g., 22:15 – 02:45 or 23:10 – 00:40)
        cur >= start || cur <= end
    }
}

/**
 * Converts exact [SolarEphemeris.PhaseInterval]s (0, 1, 2, or 3 intervals per calendar day)
 * into display [LightPhaseInterval]s with active-time detection at [currentTime].
 */
fun resolveLightPhaseIntervals(
    phaseIntervals: List<SolarEphemeris.PhaseInterval>,
    currentTime: ZonedDateTime,
): List<LightPhaseInterval> {
    val currentHour = currentTime.hour +
        currentTime.minute / 60.0 +
        currentTime.second / 3600.0
    return phaseIntervals.map { phase ->
        val isActive = when {
            phase.isAllDay -> true
            phase.startHour != null && phase.endHour != null ->
                isHourInInterval(currentHour, phase.startHour, phase.endHour)
            else -> false
        }
        LightPhaseInterval(
            startHour = phase.startHour,
            endHour = phase.endHour,
            durationHours = phase.durationHours,
            isActive = isActive,
            isAllDay = phase.isAllDay,
        )
    }
}

/**
 * Resolves the solar phase boundary crossings into 0, 1, or 2 (or more if custom) discrete
 * display intervals, collapsing continuous midday (e.g. Scandinavian winter) and continuous
 * overnight (e.g. Arctic summer) windows into a single coherent interval and detecting which
 * specific interval is currently active at [currentTime].
 */
fun resolveLightPhaseIntervals(
    dawnLower: Double?,
    dawnUpper: Double?,
    duskUpper: Double?,
    duskLower: Double?,
    morningDuration: Double,
    eveningDuration: Double,
    currentTime: ZonedDateTime,
): List<LightPhaseInterval> {
    val currentHour = currentTime.hour +
        currentTime.minute / 60.0 +
        currentTime.second / 3600.0
    val totalDuration = morningDuration + eveningDuration

    // Case 1: Standard day — two separate intervals (morning and evening)
    if (dawnLower != null && dawnUpper != null && duskUpper != null && duskLower != null) {
        return listOf(
            LightPhaseInterval(
                startHour = dawnLower,
                endHour = dawnUpper,
                durationHours = morningDuration,
                isActive = isHourInInterval(currentHour, dawnLower, dawnUpper),
            ),
            LightPhaseInterval(
                startHour = duskUpper,
                endHour = duskLower,
                durationHours = eveningDuration,
                isActive = isHourInInterval(currentHour, duskUpper, duskLower),
            ),
        )
    }

    // Case 2: Peaks inside the band (e.g., Scandinavian winter) — one continuous midday interval
    if (dawnLower != null && duskLower != null && dawnUpper == null && duskUpper == null) {
        return listOf(
            LightPhaseInterval(
                startHour = dawnLower,
                endHour = duskLower,
                durationHours = totalDuration,
                isActive = isHourInInterval(currentHour, dawnLower, duskLower),
            ),
        )
    }

    // Case 3: Bottoms out inside the band (e.g., Arctic summer) — one continuous overnight interval
    if (dawnUpper != null && duskUpper != null && dawnLower == null && duskLower == null) {
        return listOf(
            LightPhaseInterval(
                startHour = duskUpper,
                endHour = dawnUpper,
                durationHours = totalDuration,
                isActive = isHourInInterval(currentHour, duskUpper, dawnUpper),
            ),
        )
    }

    // Case 4: No crossings at all — either all day inside the band or absent all day
    if (dawnLower == null && dawnUpper == null && duskUpper == null && duskLower == null) {
        return if (totalDuration > 0.0) {
            listOf(
                LightPhaseInterval(
                    startHour = null,
                    endHour = null,
                    durationHours = totalDuration,
                    isActive = true,
                    isAllDay = true,
                ),
            )
        } else {
            emptyList()
        }
    }

    // Case 5: Partial / polar transition day (1–3 crossings present)
    val intervals = mutableListOf<LightPhaseInterval>()
    when {
        dawnLower != null && dawnUpper != null -> {
            intervals += LightPhaseInterval(
                startHour = dawnLower,
                endHour = dawnUpper,
                durationHours = morningDuration.takeIf { it > 0.0 }
                    ?: normalizeHour24(dawnUpper - dawnLower),
                isActive = isHourInInterval(currentHour, dawnLower, dawnUpper),
            )
        }
        dawnLower != null -> {
            intervals += LightPhaseInterval(
                startHour = dawnLower,
                endHour = 24.0,
                durationHours = (24.0 - normalizeHour24(dawnLower)).coerceAtLeast(0.0),
                isActive = currentHour >= normalizeHour24(dawnLower),
            )
        }
        dawnUpper != null -> {
            intervals += LightPhaseInterval(
                startHour = 0.0,
                endHour = dawnUpper,
                durationHours = normalizeHour24(dawnUpper),
                isActive = currentHour <= normalizeHour24(dawnUpper),
            )
        }
    }

    when {
        duskUpper != null && duskLower != null -> {
            intervals += LightPhaseInterval(
                startHour = duskUpper,
                endHour = duskLower,
                durationHours = eveningDuration.takeIf { it > 0.0 }
                    ?: normalizeHour24(duskLower - duskUpper),
                isActive = isHourInInterval(currentHour, duskUpper, duskLower),
            )
        }
        duskUpper != null -> {
            intervals += LightPhaseInterval(
                startHour = duskUpper,
                endHour = 24.0,
                durationHours = (24.0 - normalizeHour24(duskUpper)).coerceAtLeast(0.0),
                isActive = currentHour >= normalizeHour24(duskUpper),
            )
        }
        duskLower != null -> {
            intervals += LightPhaseInterval(
                startHour = 0.0,
                endHour = duskLower,
                durationHours = normalizeHour24(duskLower),
                isActive = currentHour <= normalizeHour24(duskLower),
            )
        }
    }

    return intervals
}

@Composable
fun GoldenHour(
    dawnLower: Double?,
    dawnUpper: Double?,
    duskUpper: Double?,
    duskLower: Double?,
    morningDuration: Double,
    eveningDuration: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier,
    phaseIntervals: List<SolarEphemeris.PhaseInterval>? = null,
) {
    LightPhaseCard(
        titleRes = R.string.golden_hour,
        iconRes = R.drawable.ic_wb_twilight_filled,
        tintSeed = Color(0xFFFFB300),
        lightAccent = Color(0xFF8C5200),
        darkAccent = Color(0xFFFFCA28),
        intervals = phaseIntervals?.let { resolveLightPhaseIntervals(it, currentTime) }
            ?: resolveLightPhaseIntervals(
                dawnLower = dawnLower,
                dawnUpper = dawnUpper,
                duskUpper = duskUpper,
                duskLower = duskLower,
                morningDuration = morningDuration,
                eveningDuration = eveningDuration,
                currentTime = currentTime,
            ),
        modifier = modifier,
    )
}

@Composable
fun BlueHour(
    dawnLower: Double?,
    dawnUpper: Double?,
    duskUpper: Double?,
    duskLower: Double?,
    morningDuration: Double,
    eveningDuration: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier,
    phaseIntervals: List<SolarEphemeris.PhaseInterval>? = null,
) {
    LightPhaseCard(
        titleRes = R.string.blue_hour,
        iconRes = R.drawable.ic_partly_cloudy_night_filled,
        tintSeed = Color(0xFF1E88E5),
        lightAccent = Color(0xFF0D47A1),
        darkAccent = Color(0xFF64B5F6),
        intervals = phaseIntervals?.let { resolveLightPhaseIntervals(it, currentTime) }
            ?: resolveLightPhaseIntervals(
                dawnLower = dawnLower,
                dawnUpper = dawnUpper,
                duskUpper = duskUpper,
                duskLower = duskLower,
                morningDuration = morningDuration,
                eveningDuration = eveningDuration,
                currentTime = currentTime,
            ),
        modifier = modifier,
    )
}

@Composable
fun PinkHour(
    dawnLower: Double?,
    dawnUpper: Double?,
    duskUpper: Double?,
    duskLower: Double?,
    morningDuration: Double,
    eveningDuration: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier,
    phaseIntervals: List<SolarEphemeris.PhaseInterval>? = null,
) {
    LightPhaseCard(
        titleRes = R.string.pink_hour,
        iconRes = R.drawable.ic_filter_drama_filled,
        tintSeed = Color(0xFFEC407A),
        lightAccent = Color(0xFFAD1457),
        darkAccent = Color(0xFFF48FB1),
        intervals = phaseIntervals?.let { resolveLightPhaseIntervals(it, currentTime) }
            ?: resolveLightPhaseIntervals(
                dawnLower = dawnLower,
                dawnUpper = dawnUpper,
                duskUpper = duskUpper,
                duskLower = duskLower,
                morningDuration = morningDuration,
                eveningDuration = eveningDuration,
                currentTime = currentTime,
            ),
        modifier = modifier,
    )
}

@Composable
fun Alpenglow(
    dawnLower: Double?,
    dawnUpper: Double?,
    duskUpper: Double?,
    duskLower: Double?,
    morningDuration: Double,
    eveningDuration: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier,
    phaseIntervals: List<SolarEphemeris.PhaseInterval>? = null,
) {
    LightPhaseCard(
        titleRes = R.string.alpenglow,
        iconRes = R.drawable.ic_landscape_filled,
        tintSeed = Color(0xFFE53935),
        lightAccent = Color(0xFFB71C1C),
        darkAccent = Color(0xFFFF8A80),
        intervals = phaseIntervals?.let { resolveLightPhaseIntervals(it, currentTime) }
            ?: resolveLightPhaseIntervals(
                dawnLower = dawnLower,
                dawnUpper = dawnUpper,
                duskUpper = duskUpper,
                duskLower = duskLower,
                morningDuration = morningDuration,
                eveningDuration = eveningDuration,
                currentTime = currentTime,
            ),
        modifier = modifier,
    )
}

@Composable
fun LightPhaseCard(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    tintSeed: Color,
    lightAccent: Color,
    darkAccent: Color,
    intervals: List<LightPhaseInterval>,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val accentColor = if (isDarkSurface) darkAccent else lightAccent
    val cardTint = tintSeed.copy(alpha = if (isDarkSurface) 0.15f else 0.14f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cardTint)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeaderEntry(
            text = stringResource(titleRes),
            textColor = accentColor,
            icon = iconRes,
            iconTint = accentColor,
            horizontalAlignment = Alignment.CenterHorizontally,
            iconSize = 20.dp,
            textSize = 15.5.sp,
            spacing = 6.dp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                intervals.isEmpty() -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = surfaceColor.copy(alpha = if (isDarkSurface) 0.40f else 0.62f),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = "None today",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = "0h",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = GoogleSansFlexRounded,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFeatureSettings = "tnum",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                ),
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }

                intervals.size == 1 -> {
                    LightPhaseIntervalChip(
                        interval = intervals.first(),
                        tintSeed = tintSeed,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        density = IntervalDensity.SINGLE,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                intervals.size == 2 -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        intervals.forEach { interval ->
                            LightPhaseIntervalChip(
                                interval = interval,
                                tintSeed = tintSeed,
                                accentColor = accentColor,
                                surfaceColor = surfaceColor,
                                isDarkSurface = isDarkSurface,
                                density = IntervalDensity.DOUBLE,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        }
                    }
                }

                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        intervals.take(3).forEach { interval ->
                            LightPhaseIntervalChip(
                                interval = interval,
                                tintSeed = tintSeed,
                                accentColor = accentColor,
                                surfaceColor = surfaceColor,
                                isDarkSurface = isDarkSurface,
                                density = IntervalDensity.COMPACT,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal enum class IntervalDensity {
    SINGLE,
    DOUBLE,
    COMPACT,
}

@Composable
internal fun LightPhaseIntervalChip(
    interval: LightPhaseInterval,
    tintSeed: Color,
    accentColor: Color,
    surfaceColor: Color,
    isDarkSurface: Boolean,
    density: IntervalDensity,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val containerColor = if (interval.isActive) {
        tintSeed.copy(alpha = if (isDarkSurface) 0.30f else 0.34f)
    } else {
        surfaceColor.copy(alpha = if (isDarkSurface) 0.44f else 0.66f)
    }
    val border = if (interval.isActive) {
        BorderStroke(1.5.dp, accentColor.copy(alpha = if (isDarkSurface) 0.85f else 0.72f))
    } else {
        null
    }

    val cornerRadius = when (density) {
        IntervalDensity.SINGLE -> 16.dp
        IntervalDensity.DOUBLE -> 14.dp
        IntervalDensity.COMPACT -> 10.dp
    }
    val verticalPadding = when (density) {
        IntervalDensity.SINGLE -> 14.dp
        IntervalDensity.DOUBLE -> 6.dp
        IntervalDensity.COMPACT -> 3.dp
    }
    val maxRangeFontSize = when (density) {
        IntervalDensity.SINGLE -> 17.5.sp
        IntervalDensity.DOUBLE -> 15.5.sp
        IntervalDensity.COMPACT -> 12.5.sp
    }
    val durationFontSize = when (density) {
        IntervalDensity.SINGLE -> 13.sp
        IntervalDensity.DOUBLE -> 12.sp
        IntervalDensity.COMPACT -> 10.sp
    }
    val nowFontSize = when (density) {
        IntervalDensity.SINGLE -> 10.sp
        IntervalDensity.DOUBLE -> 9.5.sp
        IntervalDensity.COMPACT -> 8.sp
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val durationText = interval.durationHours.formatDuration()

    val rangeText = if (interval.isAllDay) {
        buildAnnotatedString { append("All day") }
    } else {
        val (startStr, endStr) = formatDecimalHourRange(interval.startHour, interval.endHour, locale)
        val sameMinute = interval.startHour.toRoundedLocalTime() != null &&
            interval.startHour.toRoundedLocalTime() == interval.endHour.toRoundedLocalTime() &&
            interval.durationHours < 1.0
        if (sameMinute) {
            buildAnnotatedString { append(interval.startHour.formatDecimalHours()) }
        } else {
            buildAnnotatedString {
                append(startStr)
                withStyle(
                    SpanStyle(
                        color = onSurfaceVariant.copy(alpha = if (interval.isActive) 0.85f else 0.65f),
                        fontWeight = FontWeight.Medium,
                    ),
                ) {
                    append("\u2009–\u2009")
                }
                append(endStr)
            }
        }
    }

    val onBadgeColor = if (isDarkSurface) Color(0xFF1A1A1A) else Color.White

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = border,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = if (density == IntervalDensity.SINGLE) 4.dp else 1.dp,
                alignment = Alignment.CenterVertically,
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = verticalPadding),
        ) {
            // Identical weight & sizing regardless of active state so highlighting never shifts or shrinks times
            Text(
                text = rangeText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansFlexRounded,
                    lineHeight = 1.15.em,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    letterSpacing = (-0.2).sp,
                    color = onSurface,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 10.sp,
                    maxFontSize = maxRangeFontSize,
                    stepSize = 0.5.sp,
                ),
            )

            // Duration stays pinned at exact horizontal center; active "Now" pill docks at end without shifting it
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = GoogleSansFlexRounded,
                        fontSize = durationFontSize,
                        lineHeight = durationFontSize * 1.25f,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                        color = if (interval.isActive) accentColor else onSurfaceVariant,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.align(Alignment.Center),
                )

                if (interval.isActive) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Text(
                            text = "Now",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = nowFontSize,
                                lineHeight = nowFontSize * 1.15f,
                                fontWeight = FontWeight.Bold,
                                color = onBadgeColor,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(
                                horizontal = if (density == IntervalDensity.COMPACT) 4.dp else 5.dp,
                                vertical = 1.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPhaseGridLightPreview() {
    val morningActiveTime = ZonedDateTime.of(2026, 6, 15, 6, 35, 0, 0, ZoneOffset.UTC)
    val polarWinterTime = ZonedDateTime.of(2026, 12, 21, 11, 20, 0, 0, ZoneOffset.UTC)
    val eveningActiveTime = ZonedDateTime.of(2026, 6, 15, 20, 48, 0, 0, ZoneOffset.UTC)

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
                    GoldenHour(
                        dawnLower = 6.2,
                        dawnUpper = 7.167,
                        duskUpper = 19.667,
                        duskLower = 20.633,
                        morningDuration = 0.967,
                        eveningDuration = 0.967,
                        currentTime = morningActiveTime,
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    BlueHour(
                        dawnLower = 5.65,
                        dawnUpper = 6.2,
                        duskUpper = 20.633,
                        duskLower = 21.183,
                        morningDuration = 0.55,
                        eveningDuration = 0.55,
                        currentTime = eveningActiveTime,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    PinkHour(
                        dawnLower = 10.5,
                        dawnUpper = null,
                        duskUpper = null,
                        duskLower = 12.167,
                        morningDuration = 0.833,
                        eveningDuration = 0.833,
                        currentTime = polarWinterTime,
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Alpenglow(
                        dawnLower = 5.9,
                        dawnUpper = 6.25,
                        duskUpper = 20.55,
                        duskLower = 20.9,
                        morningDuration = 0.35,
                        eveningDuration = 0.35,
                        currentTime = eveningActiveTime,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPhaseGridDarkPreview() {
    val morningActiveTime = ZonedDateTime.of(2026, 6, 15, 6, 35, 0, 0, ZoneOffset.UTC)
    val middayInactiveTime = ZonedDateTime.of(2026, 6, 15, 13, 0, 0, 0, ZoneOffset.UTC)
    val eveningActiveTime = ZonedDateTime.of(2026, 6, 15, 20, 48, 0, 0, ZoneOffset.UTC)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .background(Color(0xFF141416))
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    GoldenHour(
                        dawnLower = 6.2,
                        dawnUpper = 7.167,
                        duskUpper = 19.667,
                        duskLower = 20.633,
                        morningDuration = 0.967,
                        eveningDuration = 0.967,
                        currentTime = morningActiveTime,
                    )
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    BlueHour(
                        dawnLower = 5.65,
                        dawnUpper = 6.2,
                        duskUpper = 20.633,
                        duskLower = 21.183,
                        morningDuration = 0.55,
                        eveningDuration = 0.55,
                        currentTime = eveningActiveTime,
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
                    PinkHour(
                        dawnLower = 5.95,
                        dawnUpper = 6.3,
                        duskUpper = 20.45,
                        duskLower = 20.8,
                        morningDuration = 0.35,
                        eveningDuration = 0.35,
                        currentTime = middayInactiveTime,
                    )
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Alpenglow(
                        dawnLower = null,
                        dawnUpper = null,
                        duskUpper = null,
                        duskLower = null,
                        morningDuration = 0.0,
                        eveningDuration = 0.0,
                        currentTime = middayInactiveTime,
                    )
                }
            }
        }
    }
}
