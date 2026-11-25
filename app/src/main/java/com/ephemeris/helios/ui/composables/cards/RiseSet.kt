package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.DaylightPill
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.EphemerisType
import com.ephemeris.helios.utils.calc.RiseSetEvent
import com.ephemeris.helios.utils.formatAzimuth
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.formatDuration
import java.util.Locale

@Composable
fun SunriseSunset(
    events: List<RiseSetEvent>,
    dayLengthHours: Double,
    modifier: Modifier = Modifier,
    totalDayHours: Double = 24.0,
    showItemIcons: Boolean = true,
) {
    RiseSet(
        events = events,
        durationHours = dayLengthHours,
        type = EphemerisType.SUN,
        modifier = modifier,
        totalDayHours = totalDayHours,
        isUpAllDay = events.isEmpty() && (dayLengthHours >= (totalDayHours * 0.5)),
        showItemIcons = showItemIcons,
    )
}

@Composable
fun MoonriseMoonset(
    events: List<RiseSetEvent>,
    moonDurationHours: Double,
    isMoonUpAllDay: Boolean,
    modifier: Modifier = Modifier,
    totalDayHours: Double = 24.0,
    showItemIcons: Boolean = true,
) {
    RiseSet(
        events = events,
        durationHours = moonDurationHours,
        type = EphemerisType.MOON,
        modifier = modifier,
        totalDayHours = totalDayHours,
        isUpAllDay = isMoonUpAllDay,
        showItemIcons = showItemIcons,
    )
}

@Composable
fun RiseSet(
    events: List<RiseSetEvent>,
    durationHours: Double,
    type: EphemerisType,
    modifier: Modifier = Modifier,
    totalDayHours: Double = 24.0,
    isUpAllDay: Boolean = events.isEmpty() && (durationHours >= (totalDayHours * 0.5)),
    showItemIcons: Boolean = true,
) {
    val locale = LocalLocale.current.platformLocale
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val customColors = LocalCustomColors.current
    val isLunar = type == EphemerisType.MOON

    val headerIconRes = when (type) {
        EphemerisType.SUN -> R.drawable.ic_wb_sunny_filled
        EphemerisType.MOON -> R.drawable.ic_moon_stars_filled
        else -> R.drawable.ic_routine_filled
    }

    val durationLabelRes = when (type) {
        EphemerisType.SUN -> R.string.day_length
        EphemerisType.MOON -> R.string.duration
        else -> R.string.uptime
    }

    val riseIconTint = when (type) {
        EphemerisType.SUN -> customColors.sunPath
        EphemerisType.MOON -> customColors.moonPath
        else -> MaterialTheme.colorScheme.primary
    }
    val setIconTint = MaterialTheme.colorScheme.onSurfaceVariant

    // Consistent semi-translucent tonal glass pill: always visible against plain surface (0h / 2h)
    // while letting background waves shine clearly through at partial and full daylight levels.
    val durationPillColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
        alpha = if (isDarkSurface) 0.56f else 0.58f,
    )
    val durationPillBorder = BorderStroke(
        width = 0.75.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = if (isDarkSurface) 0.11f else 0.08f,
        ),
    )

    val formattedDuration = durationHours.formatDuration(showSeconds = true)

    DaylightPill(
        durationHours = durationHours,
        totalDayHours = totalDayHours,
        isLunar = isLunar,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp, vertical = 11.dp),
        ) {
            HeaderEntry(
                text = stringResource(R.string.rise_set),
                icon = headerIconRes,
                horizontalAlignment = Alignment.CenterHorizontally,
                iconSize = 20.dp,
                textSize = 15.5.sp,
                spacing = 6.dp,
            )

            // Middle events region: constant square card size across 0, 1, 2, or 3 events
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (events.size) {
                    0 -> NoCrossingContent(
                        type = type,
                        isUpAllDay = isUpAllDay,
                    )

                    1 -> SingleEventContent(
                        event = events[0],
                        type = type,
                        locale = locale,
                        showItemIcon = showItemIcons,
                        riseIconTint = riseIconTint,
                        setIconTint = setIconTint,
                    )

                    2 -> TwoEventsContent(
                        events = events,
                        locale = locale,
                        showItemIcons = showItemIcons,
                        riseIconTint = riseIconTint,
                        setIconTint = setIconTint,
                    )

                    else -> MultiEventsContent(
                        events = events.take(3),
                        locale = locale,
                        showItemIcons = showItemIcons,
                        riseIconTint = riseIconTint,
                        setIconTint = setIconTint,
                    )
                }
            }

            // Bottom Day Length / Moon Duration translucent box matching Culmination's 46.dp pill
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = durationPillColor,
                border = durationPillBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(durationLabelRes),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.titleSmall.copy(
                            lineHeight = 1.05.em,
                            fontWeight = FontWeight.ExtraBold,
                            fontFeatureSettings = "tnum",
                            letterSpacing = (-0.2).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 11.5.sp,
                            maxFontSize = 15.5.sp,
                            stepSize = 0.5.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NoCrossingContent(
    type: EphemerisType,
    isUpAllDay: Boolean,
) {
    val primaryLabel = when (type) {
        EphemerisType.SUN -> if (isUpAllDay) "Midnight Sun" else "Polar Night"
        else -> if (isUpAllDay) "Always Up" else "Always Down"
    }
    val secondaryLabel = if (isUpAllDay) "Above horizon" else "Below horizon"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = primaryLabel,
            style = MaterialTheme.typography.titleMedium.copy(
                lineHeight = 1.15.em,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 13.sp,
                maxFontSize = 16.sp,
                stepSize = 0.5.sp,
            ),
        )
        Text(
            text = secondaryLabel,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun SingleEventContent(
    event: RiseSetEvent,
    type: EphemerisType,
    locale: Locale,
    showItemIcon: Boolean,
    riseIconTint: Color,
    setIconTint: Color,
) {
    val formattedTime = event.timeHour.formatDecimalHours()
    val formattedAzimuth = event.azimuth.formatAzimuth(locale)
    val missingLabel = when (type) {
        EphemerisType.SUN -> if (event.isRise) "No sunset" else "No sunrise"
        EphemerisType.MOON -> if (event.isRise) "No moonset" else "No moonrise"
        else -> if (event.isRise) "No set" else "No rise"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
    ) {
        if (showItemIcon) {
            DirectionArrowIcon(
                isRise = event.isRise,
                tint = if (event.isRise) riseIconTint else setIconTint,
                sizeDp = 22,
            )
        }
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.titleLarge.copy(
                    lineHeight = 1.08.em,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    letterSpacing = (-0.3).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 14.sp,
                    maxFontSize = 18.sp,
                    stepSize = 0.5.sp,
                ),
            )
            formattedAzimuth?.let { azimuthText ->
                Text(
                    text = azimuthText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        lineHeight = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Text(
                text = missingLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun TwoEventsContent(
    events: List<RiseSetEvent>,
    locale: Locale,
    showItemIcons: Boolean,
    riseIconTint: Color,
    setIconTint: Color,
) {
    val trimLineHeight = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    )
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
    ) {
        events.forEach { event ->
            val formattedTime = event.timeHour.formatDecimalHours()
            val formattedAzimuth = event.azimuth.formatAzimuth(locale)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (showItemIcons) {
                    DirectionArrowIcon(
                        isRise = event.isRise,
                        tint = if (event.isRise) riseIconTint else setIconTint,
                        sizeDp = 20,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.titleMedium.copy(
                            lineHeight = 17.sp,
                            lineHeightStyle = trimLineHeight,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                            letterSpacing = (-0.25).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 12.5.sp,
                            maxFontSize = 15.5.sp,
                            stepSize = 0.5.sp,
                        ),
                    )
                    formattedAzimuth?.let { azimuthText ->
                        Text(
                            text = azimuthText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 13.sp,
                                lineHeightStyle = trimLineHeight,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                            ),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact 3-event layout for arctic / polar or near-midnight lunar days with three horizon
 * crossings (e.g., Set -> Rise -> Set or Rise -> Set -> Rise). Each crossing stays on a single
 * horizontal row so all three fit comfortably inside the fixed square tile.
 */
@Composable
private fun MultiEventsContent(
    events: List<RiseSetEvent>,
    locale: Locale,
    showItemIcons: Boolean,
    riseIconTint: Color,
    setIconTint: Color,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxWidth(),
    ) {
        events.forEach { event ->
            val formattedTime = event.timeHour.formatDecimalHours()
            val formattedAzimuth = event.azimuth.formatAzimuth(locale)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    if (showItemIcons) {
                        DirectionArrowIcon(
                            isRise = event.isRise,
                            tint = if (event.isRise) riseIconTint else setIconTint,
                            sizeDp = 15,
                        )
                    }
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 1.1.em,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                            letterSpacing = (-0.2).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 10.sp,
                            maxFontSize = 12.5.sp,
                            stepSize = 0.5.sp,
                        ),
                    )
                }
                formattedAzimuth?.let { azimuthText ->
                    Text(
                        text = azimuthText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            lineHeight = 1.1.em,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        ),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 9.sp,
                            maxFontSize = 11.sp,
                            stepSize = 0.5.sp,
                        ),
                        modifier = Modifier.padding(start = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectionArrowIcon(
    isRise: Boolean,
    tint: Color,
    sizeDp: Int,
) {
    Icon(
        painter = painterResource(
            id = if (isRise) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward,
        ),
        contentDescription = stringResource(if (isRise) R.string.rise else R.string.set),
        tint = tint,
        modifier = Modifier.size(sizeDp.dp),
    )
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun RiseSetPreviewLight() {
    HeliosTheme(darkTheme = false, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(Color(0xFFECEEF4))
                .padding(16.dp),
        ) {
            // Row 1: Standard Summer Sun (2 events, ~15h wave) + Short 3.5h Winter Sun (wave halfway through bottom box)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SunriseSunset(
                        events = listOf(
                            RiseSetEvent(timeHour = 5.58, azimuth = 59.2, isRise = true),
                            RiseSetEvent(timeHour = 20.81, azimuth = 300.8, isRise = false),
                        ),
                        dayLengthHours = 15.238,
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SunriseSunset(
                        events = listOf(
                            RiseSetEvent(timeHour = 10.25, azimuth = 158.4, isRise = true),
                            RiseSetEvent(timeHour = 13.75, azimuth = 201.6, isRise = false),
                        ),
                        dayLengthHours = 3.50,
                    )
                }
            }

            // Row 2: Arctic 3-event Sun (~22h wave) + Moon 2 events (Set -> Rise, ~11h wave)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SunriseSunset(
                        events = listOf(
                            RiseSetEvent(timeHour = 0.21, azimuth = 356.4, isRise = false),
                            RiseSetEvent(timeHour = 1.82, azimuth = 4.2, isRise = true),
                            RiseSetEvent(timeHour = 23.91, azimuth = 355.8, isRise = false),
                        ),
                        dayLengthHours = 22.30,
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    MoonriseMoonset(
                        events = listOf(
                            RiseSetEvent(timeHour = 3.45, azimuth = 248.6, isRise = false),
                            RiseSetEvent(timeHour = 16.72, azimuth = 112.4, isRise = true),
                        ),
                        moonDurationHours = 10.73,
                        isMoonUpAllDay = false,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun RiseSetPreviewDark() {
    HeliosTheme(darkTheme = true, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(Color(0xFF121318))
                .padding(16.dp),
        ) {
            // Row 1: Polar Night (0h) + Midnight Sun (24h)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SunriseSunset(
                        events = emptyList(),
                        dayLengthHours = 0.0,
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SunriseSunset(
                        events = emptyList(),
                        dayLengthHours = 24.0,
                    )
                }
            }

            // Row 2: Moon 3-event arctic day + Moon 1-event (2h duration, partial wave behind pill)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    MoonriseMoonset(
                        events = listOf(
                            RiseSetEvent(timeHour = 0.18, azimuth = 351.2, isRise = true),
                            RiseSetEvent(timeHour = 10.42, azimuth = 228.5, isRise = false),
                            RiseSetEvent(timeHour = 23.86, azimuth = 349.8, isRise = true),
                        ),
                        moonDurationHours = 10.38,
                        isMoonUpAllDay = false,
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    MoonriseMoonset(
                        events = listOf(
                            RiseSetEvent(timeHour = 22.0, azimuth = 124.7, isRise = true),
                        ),
                        moonDurationHours = 2.0,
                        isMoonUpAllDay = false,
                    )
                }
            }
        }
    }
}
