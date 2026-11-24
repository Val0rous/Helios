package com.ephemeris.helios.ui.composables.cards

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
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
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.formatDuration
import java.time.ZoneOffset
import java.time.ZonedDateTime

enum class TwilightStageKind(@param:StringRes val labelRes: Int) {
    CIVIL(R.string.civil),
    NAUTICAL(R.string.nautical),
    ASTRO(R.string.astro),
}

data class TwilightStageWindow(
    val kind: TwilightStageKind,
    val startHour: Double?,
    val endHour: Double?,
    val durationHours: Double,
    val isActive: Boolean,
    val direction: SolarEphemeris.PhaseDirection = SolarEphemeris.PhaseDirection.CONTINUOUS,
    val isAllDay: Boolean = false,
)

private fun normalizeTwilightHour(hour: Double): Double {
    val mod = hour % 24.0
    return if (mod < 0.0) mod + 24.0 else mod
}

/**
 * Half-open `[start, end)` check so adjacent twilight stages (e.g. Nautical ending at the exact
 * instant Civil begins) never light up simultaneously on a boundary second.
 */
private fun isHourInHalfOpenInterval(currentHour: Double, startHour: Double, endHour: Double): Boolean {
    val cur = normalizeTwilightHour(currentHour)
    if (endHour >= 24.0 && startHour < 24.0) {
        val start = normalizeTwilightHour(startHour)
        val extraAfterMidnight = endHour - 24.0
        return cur >= start || (extraAfterMidnight > 0.0 && cur < extraAfterMidnight)
    }
    val start = normalizeTwilightHour(startHour)
    val end = normalizeTwilightHour(endHour)
    return if (start <= end) {
        (cur in start..<end)
    } else {
        cur >= start || cur < end
    }
}

/**
 * Resolves a twilight stage into 0, 1, 2, or 3 unified [TwilightStageWindow]s.
 *
 * - Prefers exact scanned [phaseIntervals] from [SolarEphemeris.DailyEvents] when supplied,
 *   natively handling continuous overnight/midday bands (1 unified window), absent stages (0),
 *   standard Dawn+Dusk (2, ordered Rising -> Left, Setting -> Right), and midnight-split 3-interval days.
 * - Falls back to boundary-crossing resolution when [phaseIntervals] is null.
 */
internal fun resolveTwilightStageWindows(
    kind: TwilightStageKind,
    phaseIntervals: List<SolarEphemeris.PhaseInterval>?,
    dawnLower: Double?,
    dawnUpper: Double?,
    duskUpper: Double?,
    duskLower: Double?,
    morningDuration: Double,
    eveningDuration: Double,
    currentTime: ZonedDateTime,
): List<TwilightStageWindow> {
    val currentHour = currentTime.hour +
        currentTime.minute / 60.0 +
        currentTime.second / 3600.0

    if (phaseIntervals != null) {
        val windows = phaseIntervals.map { phase ->
            val isActive = when {
                phase.isAllDay -> true
                phase.startHour != null && phase.endHour != null ->
                    isHourInHalfOpenInterval(currentHour, phase.startHour, phase.endHour)
                else -> false
            }
            TwilightStageWindow(
                kind = kind,
                startHour = phase.startHour,
                endHour = phase.endHour,
                durationHours = phase.durationHours,
                isActive = isActive,
                direction = phase.direction,
                isAllDay = phase.isAllDay,
            )
        }
        // When there are 2 windows and one is Dawn (RISING) and one is Dusk (SETTING),
        // always keep Dawn on the left and Dusk on the right (even if Dusk occurs after 00:00).
        if (windows.size == 2) {
            val rising = windows.firstOrNull { it.direction == SolarEphemeris.PhaseDirection.RISING }
            val setting = windows.firstOrNull { it.direction == SolarEphemeris.PhaseDirection.SETTING }
            if (rising != null && setting != null) {
                return listOf(rising, setting)
            }
        }
        return windows
    }

    // Legacy fallback when only 4 crossings are provided
    val totalDuration = morningDuration + eveningDuration

    // Case 1: Standard day — 2 separate windows (Dawn and Dusk)
    if (dawnLower != null && dawnUpper != null && duskUpper != null && duskLower != null) {
        return listOf(
            TwilightStageWindow(
                kind = kind,
                startHour = dawnLower,
                endHour = dawnUpper,
                durationHours = morningDuration,
                isActive = isHourInHalfOpenInterval(currentHour, dawnLower, dawnUpper),
                direction = SolarEphemeris.PhaseDirection.RISING,
            ),
            TwilightStageWindow(
                kind = kind,
                startHour = duskUpper,
                endHour = duskLower,
                durationHours = eveningDuration,
                isActive = isHourInHalfOpenInterval(currentHour, duskUpper, duskLower),
                direction = SolarEphemeris.PhaseDirection.SETTING,
            ),
        )
    }

    // Case 2: Peaks inside this twilight stage (polar winter) — 1 continuous midday window
    if (dawnLower != null && duskLower != null && dawnUpper == null && duskUpper == null) {
        return listOf(
            TwilightStageWindow(
                kind = kind,
                startHour = dawnLower,
                endHour = duskLower,
                durationHours = totalDuration,
                isActive = isHourInHalfOpenInterval(currentHour, dawnLower, duskLower),
                direction = SolarEphemeris.PhaseDirection.CONTINUOUS,
            ),
        )
    }

    // Case 3: Bottoms out inside this twilight stage (Scandinavia summer) — 1 continuous overnight window
    if (dawnUpper != null && duskUpper != null && dawnLower == null && duskLower == null) {
        return listOf(
            TwilightStageWindow(
                kind = kind,
                startHour = duskUpper,
                endHour = dawnUpper,
                durationHours = totalDuration,
                isActive = isHourInHalfOpenInterval(currentHour, duskUpper, dawnUpper),
                direction = SolarEphemeris.PhaseDirection.CONTINUOUS,
            ),
        )
    }

    // Case 4: No crossings at all — either all-day or absent
    if (dawnLower == null && dawnUpper == null && duskUpper == null && duskLower == null) {
        return if (totalDuration > 0.0) {
            listOf(
                TwilightStageWindow(
                    kind = kind,
                    startHour = null,
                    endHour = null,
                    durationHours = totalDuration,
                    isActive = true,
                    direction = SolarEphemeris.PhaseDirection.CONTINUOUS,
                    isAllDay = true,
                ),
            )
        } else {
            emptyList()
        }
    }

    // Case 5: Partial polar transition day
    return buildList {
        when {
            dawnLower != null && dawnUpper != null -> add(
                TwilightStageWindow(
                    kind = kind,
                    startHour = dawnLower,
                    endHour = dawnUpper,
                    durationHours = morningDuration.takeIf { it > 0.0 }
                        ?: normalizeTwilightHour(dawnUpper - dawnLower),
                    isActive = isHourInHalfOpenInterval(currentHour, dawnLower, dawnUpper),
                    direction = SolarEphemeris.PhaseDirection.RISING,
                ),
            )
            dawnLower != null -> add(
                TwilightStageWindow(
                    kind = kind,
                    startHour = dawnLower,
                    endHour = 24.0,
                    durationHours = (24.0 - normalizeTwilightHour(dawnLower)).coerceAtLeast(0.0),
                    isActive = isHourInHalfOpenInterval(currentHour, dawnLower, 24.0),
                    direction = SolarEphemeris.PhaseDirection.RISING,
                ),
            )
            dawnUpper != null -> add(
                TwilightStageWindow(
                    kind = kind,
                    startHour = 0.0,
                    endHour = dawnUpper,
                    durationHours = normalizeTwilightHour(dawnUpper),
                    isActive = isHourInHalfOpenInterval(currentHour, 0.0, dawnUpper),
                    direction = SolarEphemeris.PhaseDirection.RISING,
                ),
            )
        }
        when {
            duskUpper != null && duskLower != null -> add(
                TwilightStageWindow(
                    kind = kind,
                    startHour = duskUpper,
                    endHour = duskLower,
                    durationHours = eveningDuration.takeIf { it > 0.0 }
                        ?: normalizeTwilightHour(duskLower - duskUpper),
                    isActive = isHourInHalfOpenInterval(currentHour, duskUpper, duskLower),
                    direction = SolarEphemeris.PhaseDirection.SETTING,
                ),
            )
            duskUpper != null -> add(
                TwilightStageWindow(
                    kind = kind,
                    startHour = duskUpper,
                    endHour = 24.0,
                    durationHours = (24.0 - normalizeTwilightHour(duskUpper)).coerceAtLeast(0.0),
                    isActive = isHourInHalfOpenInterval(currentHour, duskUpper, 24.0),
                    direction = SolarEphemeris.PhaseDirection.SETTING,
                ),
            )
            duskLower != null -> add(
                TwilightStageWindow(
                    kind = kind,
                    startHour = 0.0,
                    endHour = duskLower,
                    durationHours = normalizeTwilightHour(duskLower),
                    isActive = isHourInHalfOpenInterval(currentHour, 0.0, duskLower),
                    direction = SolarEphemeris.PhaseDirection.SETTING,
                ),
            )
        }
    }
}

@Composable
fun TwilightCard(
    sunrise: Double?,
    sunset: Double?,
    dawnCivil: Double?,
    duskCivil: Double?,
    dawnNautical: Double?,
    duskNautical: Double?,
    dawnAstronomical: Double?,
    duskAstronomical: Double?,
    civilMorningDuration: Double,
    civilEveningDuration: Double,
    nauticalMorningDuration: Double,
    nauticalEveningDuration: Double,
    astroMorningDuration: Double,
    astroEveningDuration: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier,
    civilIntervals: List<SolarEphemeris.PhaseInterval>? = null,
    nauticalIntervals: List<SolarEphemeris.PhaseInterval>? = null,
    astroIntervals: List<SolarEphemeris.PhaseInterval>? = null,
) {
    val civilWindows = resolveTwilightStageWindows(
        kind = TwilightStageKind.CIVIL,
        phaseIntervals = civilIntervals,
        dawnLower = dawnCivil,
        dawnUpper = sunrise,
        duskUpper = sunset,
        duskLower = duskCivil,
        morningDuration = civilMorningDuration,
        eveningDuration = civilEveningDuration,
        currentTime = currentTime,
    )
    val nauticalWindows = resolveTwilightStageWindows(
        kind = TwilightStageKind.NAUTICAL,
        phaseIntervals = nauticalIntervals,
        dawnLower = dawnNautical,
        dawnUpper = dawnCivil,
        duskUpper = duskCivil,
        duskLower = duskNautical,
        morningDuration = nauticalMorningDuration,
        eveningDuration = nauticalEveningDuration,
        currentTime = currentTime,
    )
    val astroWindows = resolveTwilightStageWindows(
        kind = TwilightStageKind.ASTRO,
        phaseIntervals = astroIntervals,
        dawnLower = dawnAstronomical,
        dawnUpper = dawnNautical,
        duskUpper = duskNautical,
        duskLower = duskAstronomical,
        morningDuration = astroMorningDuration,
        eveningDuration = astroEveningDuration,
        currentTime = currentTime,
    )

    val allAbsent = civilWindows.isEmpty() && nauticalWindows.isEmpty() && astroWindows.isEmpty()

    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f

    // Twilight indigo-violet palette (distinct from Blue Hour cobalt and Pink Hour rose)
    val tintSeed = Color(0xFF5E35B1)
    val accentColor = if (isDarkSurface) Color(0xFFB39DDB) else Color(0xFF4527A0)
    val cardTint = tintSeed.copy(alpha = if (isDarkSurface) 0.16f else 0.12f)

    // Constant height so missing stages or polar midnight sun / polar night never resize the card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(262.dp)
                .background(cardTint)
                .padding(horizontal = 11.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderEntry(
                text = stringResource(R.string.twilights),
                textColor = accentColor,
                icon = R.drawable.ic_routine_filled,
                iconTint = accentColor,
                horizontalAlignment = Alignment.CenterHorizontally,
                iconSize = 20.dp,
                textSize = 15.5.sp,
                spacing = 6.dp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (allAbsent) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = surfaceColor.copy(alpha = if (isDarkSurface) 0.40f else 0.64f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = "No twilight today",
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
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    TwilightStageRow(
                        kind = TwilightStageKind.CIVIL,
                        windows = civilWindows,
                        tintSeed = tintSeed,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    TwilightStageRow(
                        kind = TwilightStageKind.NAUTICAL,
                        windows = nauticalWindows,
                        tintSeed = tintSeed,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    TwilightStageRow(
                        kind = TwilightStageKind.ASTRO,
                        windows = astroWindows,
                        tintSeed = tintSeed,
                        accentColor = accentColor,
                        surfaceColor = surfaceColor,
                        isDarkSurface = isDarkSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

private fun stageDotColor(kind: TwilightStageKind, isDarkSurface: Boolean): Color = when (kind) {
    TwilightStageKind.CIVIL -> if (isDarkSurface) Color(0xFFD1C4E9) else Color(0xFF7E57C2)
    TwilightStageKind.NAUTICAL -> if (isDarkSurface) Color(0xFFB39DDB) else Color(0xFF5E35B1)
    TwilightStageKind.ASTRO -> if (isDarkSurface) Color(0xFF7E57C2) else Color(0xFF311B92)
}

@Composable
private fun TwilightStageRow(
    kind: TwilightStageKind,
    windows: List<TwilightStageWindow>,
    tintSeed: Color,
    accentColor: Color,
    surfaceColor: Color,
    isDarkSurface: Boolean,
    modifier: Modifier = Modifier,
) {
    val dotColor = stageDotColor(kind, isDarkSurface)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val visibleWindows = windows.take(3)
    val totalDuration = visibleWindows.sumOf { it.durationHours }
    val anyActive = visibleWindows.any { it.isActive }
    val isAbsent = visibleWindows.isEmpty()

    // Unified default layout for every twilight stage:
    // Top strip: stage dot + name on left, total stage duration on right
    // Bottom slot strip: 1, 2, or 3 interval pills (or 1 muted "None today" pill when absent)
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(5.5.dp)
                    .clip(CircleShape)
                    .background(if (isAbsent) dotColor.copy(alpha = 0.45f) else dotColor),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(kind.labelRes),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        anyActive -> accentColor
                        isAbsent -> onSurfaceVariant.copy(alpha = 0.72f)
                        else -> onSurfaceVariant
                    },
                ),
                maxLines = 1,
                softWrap = false,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = totalDuration.formatDuration(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                    color = when {
                        anyActive -> accentColor
                        isAbsent -> onSurfaceVariant.copy(alpha = 0.6f)
                        else -> onSurfaceVariant
                    },
                ),
                maxLines = 1,
                softWrap = false,
            )
        }

        when {
            isAbsent -> {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = surfaceColor.copy(alpha = if (isDarkSurface) 0.34f else 0.56f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = "None today",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = onSurfaceVariant.copy(alpha = 0.72f),
                            ),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            visibleWindows.size == 1 -> {
                TwilightSlotChip(
                    stage = visibleWindows.first(),
                    slotCount = 1,
                    tintSeed = tintSeed,
                    accentColor = accentColor,
                    surfaceColor = surfaceColor,
                    isDarkSurface = isDarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            else -> {
                val gap = if (visibleWindows.size == 2) 6.dp else 4.dp
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    visibleWindows.forEach { window ->
                        TwilightSlotChip(
                            stage = window,
                            slotCount = visibleWindows.size,
                            tintSeed = tintSeed,
                            accentColor = accentColor,
                            surfaceColor = surfaceColor,
                            isDarkSurface = isDarkSurface,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TwilightSlotChip(
    stage: TwilightStageWindow,
    slotCount: Int,
    tintSeed: Color,
    accentColor: Color,
    surfaceColor: Color,
    isDarkSurface: Boolean,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val containerColor = if (stage.isActive) {
        tintSeed.copy(alpha = if (isDarkSurface) 0.34f else 0.28f)
    } else {
        surfaceColor.copy(alpha = if (isDarkSurface) 0.44f else 0.68f)
    }
    val border = if (stage.isActive) {
        BorderStroke(1.5.dp, accentColor.copy(alpha = if (isDarkSurface) 0.85f else 0.72f))
    } else {
        null
    }
    val onBadgeColor = if (isDarkSurface) Color(0xFF1A1A1A) else Color.White

    val isTriple = slotCount >= 3
    val cornerRadius = if (isTriple) 10.dp else 11.dp
    val horizPad = when (slotCount) {
        1 -> 10.dp
        2 -> 8.dp
        else -> 4.dp
    }
    val maxRangeSize = when (slotCount) {
        1 -> 15.sp
        2 -> 14.sp
        else -> 11.sp
    }
    val minRangeSize = if (isTriple) 8.sp else 10.sp
    val durationSize = if (isTriple) 9.5.sp else 11.sp
    val nowFontSize = if (isTriple) 7.5.sp else 8.5.sp

    val rangeText = if (stage.isAllDay) {
        buildAnnotatedString { append("All day") }
    } else {
        val (startStr, endStr) = formatDecimalHourRange(stage.startHour, stage.endHour, locale)
        buildAnnotatedString {
            append(startStr)
            withStyle(
                SpanStyle(
                    color = onSurfaceVariant.copy(alpha = if (stage.isActive) 0.85f else 0.65f),
                    fontWeight = FontWeight.Medium,
                ),
            ) {
                append(if (isTriple) "–" else "\u2009–\u2009")
            }
            append(endStr)
        }
    }

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = border,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizPad, vertical = 3.dp),
        ) {
            // Time range stays identical weight & size whether active or inactive
            Text(
                text = rangeText,
                style = MaterialTheme.typography.titleSmall.copy(
                    lineHeight = 1.15.em,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    letterSpacing = if (isTriple) (-0.3).sp else (-0.2).sp,
                    color = onSurface,
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = minRangeSize,
                    maxFontSize = maxRangeSize,
                    stepSize = 0.5.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Duration pinned at horizontal center; "Now" badge docked in bottom-right corner without shifting it
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stage.durationHours.formatDuration(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = durationSize,
                        lineHeight = durationSize * 1.25f,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                        color = if (stage.isActive) accentColor else onSurfaceVariant,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.align(Alignment.Center),
                )
                if (stage.isActive) {
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
                                horizontal = if (isTriple) 3.5.dp else 4.5.dp,
                                vertical = 0.5.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun TwilightCardStandardAndScandiPreview() {
    val activeCivilDawn = ZonedDateTime.of(2026, 6, 15, 5, 50, 0, 0, ZoneOffset.UTC)
    val scandiOvernight = ZonedDateTime.of(2026, 6, 21, 0, 30, 0, 0, ZoneOffset.UTC)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .background(Color(0xFFECEEF4))
            .padding(vertical = 12.dp),
    ) {
        // 1. Standard mid-latitude day (Light, Civil Dawn active with bottom-right Now pill)
        HeliosTheme(darkTheme = false, dynamicColor = false) {
            TwilightCard(
                sunrise = 6.2,
                sunset = 20.633,
                dawnCivil = 5.65,
                duskCivil = 21.183,
                dawnNautical = 5.017,
                duskNautical = 21.817,
                dawnAstronomical = 4.3,
                duskAstronomical = 22.533,
                civilMorningDuration = 0.55,
                civilEveningDuration = 0.55,
                nauticalMorningDuration = 0.633,
                nauticalEveningDuration = 0.633,
                astroMorningDuration = 0.717,
                astroEveningDuration = 0.717,
                currentTime = activeCivilDawn,
            )
        }

        // 2. Scandinavia summer (Dark): Civil 2-way, Nautical 1 unified overnight pill (active), Astro muted None
        HeliosTheme(darkTheme = true, dynamicColor = false) {
            TwilightCard(
                sunrise = 3.75,
                sunset = 22.25,
                dawnCivil = 2.583,
                duskCivil = 23.417,
                dawnNautical = null,
                duskNautical = null,
                dawnAstronomical = null,
                duskAstronomical = null,
                civilMorningDuration = 1.167,
                civilEveningDuration = 1.167,
                nauticalMorningDuration = 1.583,
                nauticalEveningDuration = 1.583,
                astroMorningDuration = 0.0,
                astroEveningDuration = 0.0,
                currentTime = scandiOvernight,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun TwilightCardPolarEdgeCasesPreview() {
    val bergenTripleTime = ZonedDateTime.of(2026, 5, 20, 0, 20, 0, 0, ZoneOffset.UTC)
    val midnightSunTime = ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, ZoneOffset.UTC)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .background(Color(0xFFECEEF4))
            .padding(vertical = 12.dp),
    ) {
        // 3. Bergen / Reykjavik edge case (Light): 3 Nautical intervals + 1 unified overnight Astro
        HeliosTheme(darkTheme = false, dynamicColor = false) {
            TwilightCard(
                sunrise = 4.917,
                sunset = 22.25,
                dawnCivil = 3.667,
                duskCivil = 23.5,
                dawnNautical = 2.417,
                duskNautical = 0.75,
                dawnAstronomical = null,
                duskAstronomical = null,
                civilMorningDuration = 1.25,
                civilEveningDuration = 1.25,
                nauticalMorningDuration = 1.25,
                nauticalEveningDuration = 1.25,
                astroMorningDuration = 0.833,
                astroEveningDuration = 0.833,
                currentTime = bergenTripleTime,
                civilIntervals = listOf(
                    SolarEphemeris.PhaseInterval(3.667, 4.917, 1.25, SolarEphemeris.PhaseDirection.RISING),
                    SolarEphemeris.PhaseInterval(22.25, 23.5, 1.25, SolarEphemeris.PhaseDirection.SETTING),
                ),
                nauticalIntervals = listOf(
                    SolarEphemeris.PhaseInterval(0.0, 0.75, 0.75, SolarEphemeris.PhaseDirection.SETTING),
                    SolarEphemeris.PhaseInterval(2.417, 3.667, 1.25, SolarEphemeris.PhaseDirection.RISING),
                    SolarEphemeris.PhaseInterval(23.5, 24.0, 0.5, SolarEphemeris.PhaseDirection.SETTING),
                ),
                astroIntervals = listOf(
                    SolarEphemeris.PhaseInterval(0.75, 2.417, 1.667, SolarEphemeris.PhaseDirection.CONTINUOUS),
                ),
            )
        }

        // 4. Midnight Sun / Deep Polar Night (Dark): exact same card height
        HeliosTheme(darkTheme = true, dynamicColor = false) {
            TwilightCard(
                sunrise = null,
                sunset = null,
                dawnCivil = null,
                duskCivil = null,
                dawnNautical = null,
                duskNautical = null,
                dawnAstronomical = null,
                duskAstronomical = null,
                civilMorningDuration = 0.0,
                civilEveningDuration = 0.0,
                nauticalMorningDuration = 0.0,
                nauticalEveningDuration = 0.0,
                astroMorningDuration = 0.0,
                astroEveningDuration = 0.0,
                currentTime = midnightSunTime,
                civilIntervals = emptyList(),
                nauticalIntervals = emptyList(),
                astroIntervals = emptyList(),
            )
        }
    }
}
