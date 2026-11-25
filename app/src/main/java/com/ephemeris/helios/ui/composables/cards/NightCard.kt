package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
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
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.EphemerisType
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.formatAzimuth
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.location.Coordinates
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Returns `true` when the Sun's minimum altitude at Solar Midnight stays at or above the
 * apparent sunrise/sunset threshold (Midnight Sun — the mirror image of Polar Night on [SolarNoon]).
 */
internal fun isSolarMidnightAboveHorizon(
    altitude: Double?,
    horizonThresholdDeg: Double,
): Boolean = altitude != null && !altitude.isNaN() && altitude >= horizonThresholdDeg

private fun formatSignedAltitude(
    altitude: Double?,
    locale: Locale,
): String? {
    if (altitude == null || altitude.isNaN()) return null
    val rounded = (altitude * 10.0).roundToInt() / 10.0
    val sign = if (rounded < 0.0) "\u2212" else ""
    return "$sign${String.format(locale, "%.1f", abs(rounded))}°"
}

/**
 * Full-bleed square card presenting both the astronomical dark-sky duration (`Sun < -18°`)
 * and Solar Midnight details (time, minimum altitude, and azimuth).
 *
 * Handles 0 (EoT slow drift / 23h DST), 1 (standard), and 2 (EoT fast drift / 25h DST)
 * solar midnights in a single calendar day. Mirrors [PlutoTime] and [LightPhaseCard] outer
 * padding and stacked tonal pill geometry so adjacent square cards align horizontally.
 */
@Composable
fun Night(
    durationHours: Double,
    midnightTime: Double?,
    midnightAzimuth: Double?,
    midnightAltitude: Double?,
    modifier: Modifier = Modifier,
    midnightEvents: List<SolarEphemeris.SolarMidnightEvent> = emptyList(),
    coordinates: Coordinates? = null,
    currentTime: ZonedDateTime? = null,
    currentAltitude: Double? = null,
    followApparentHorizonSetting: Boolean = true,
    onMidnightClick: ((Double) -> Unit)? = null,
) {
    val locale = LocalLocale.current.platformLocale
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val customColors = LocalCustomColors.current

    val tintSeed = Color(0xFF5E5791)
    val accentColor = customColors.nightPrimary
    val onBadgeColor = customColors.nightOnPrimary
    val cardTint = tintSeed.copy(alpha = if (isDarkSurface) 0.16f else 0.14f)
    val aboveHorizonRed = if (isDarkSurface) Color(0xFFFF8A80) else Color(0xFFC62828)

    val horizonThresholdDeg = resolveCulminationHorizonThreshold(
        type = EphemerisType.SUN,
        coordinates = coordinates,
        followApparentHorizonSetting = followApparentHorizonSetting,
    )

    val resolvedMidnights: List<SolarEphemeris.SolarMidnightEvent> = when {
        midnightEvents.isNotEmpty() -> midnightEvents
        midnightTime != null && !midnightTime.isNaN() -> listOf(
            SolarEphemeris.SolarMidnightEvent(
                time = midnightTime,
                elapsedHours = midnightTime,
                altitude = midnightAltitude ?: Double.NaN,
                azimuth = midnightAzimuth ?: Double.NaN,
            ),
        )
        else -> emptyList()
    }

    val hasDarkSky = !durationHours.isNaN() &&
        durationHours > 0.0 &&
        (durationHours * 3600.0).roundToInt() > 0
    val formattedDuration = if (durationHours.isNaN()) "--" else durationHours.formatDuration(showSeconds = true)
    val isDarkSkyActive = hasDarkSky &&
        currentAltitude != null &&
        !currentAltitude.isNaN() &&
        currentAltitude < SolarEphemeris.ALT_ASTRONOMICAL_TWILIGHT

    val isAnyMidnightActive = resolvedMidnights.any { isCulminationActive(it.time, currentTime) }

    val trimLineHeight = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(cardTint)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeaderEntry(
            text = stringResource(R.string.night),
            textColor = accentColor,
            icon = R.drawable.ic_stars_2_filled,
            iconTint = accentColor,
            horizontalAlignment = Alignment.CenterHorizontally,
            iconSize = 20.dp,
            textSize = 15.5.sp,
            spacing = 6.dp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // Top tonal pill: Astronomical Dark Sky duration
            val darkSkyPillColor = if (isDarkSkyActive) {
                tintSeed.copy(alpha = if (isDarkSurface) 0.32f else 0.24f)
            } else {
                surfaceColor.copy(alpha = if (isDarkSurface) 0.44f else 0.66f)
            }
            val darkSkyBorder = if (isDarkSkyActive) {
                BorderStroke(1.5.dp, accentColor.copy(alpha = if (isDarkSurface) 0.85f else 0.72f))
            } else {
                null
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = darkSkyPillColor,
                border = darkSkyBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.43f),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.dark_sky),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                lineHeightStyle = trimLineHeight,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp,
                                color = if (isDarkSkyActive) {
                                    accentColor
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )

                        Text(
                            text = formattedDuration,
                            style = MaterialTheme.typography.titleSmall.copy(
                                lineHeight = 1.05.em,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = "tnum",
                                letterSpacing = (-0.2).sp,
                                color = if (hasDarkSky) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
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

                    if (isDarkSkyActive) {
                        NightNowBadge(
                            accentColor = accentColor,
                            onBadgeColor = onBadgeColor,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 5.dp),
                        )
                    }
                }
            }

            // Bottom tonal pill: 0, 1, or 2 Solar Midnight events (tapping jumps TimeMachine)
            val midnightPillColor = if (isAnyMidnightActive) {
                tintSeed.copy(alpha = if (isDarkSurface) 0.32f else 0.24f)
            } else {
                surfaceColor.copy(alpha = if (isDarkSurface) 0.44f else 0.66f)
            }
            val midnightBorder = if (isAnyMidnightActive) {
                BorderStroke(1.5.dp, accentColor.copy(alpha = if (isDarkSurface) 0.85f else 0.72f))
            } else {
                null
            }
            val singleEvent = resolvedMidnights.singleOrNull()
            val singlePillClick: (() -> Unit)? = if (singleEvent != null && onMidnightClick != null) {
                { onMidnightClick(singleEvent.elapsedHours) }
            } else {
                null
            }

            val midnightPillContent: @Composable () -> Unit = {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            if (resolvedMidnights.size >= 2) 2.dp else 1.dp,
                            Alignment.CenterVertically,
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = if (resolvedMidnights.size >= 2) 7.dp else 8.dp,
                                vertical = if (resolvedMidnights.size >= 2) 3.dp else 4.dp,
                            ),
                    ) {
                        Text(
                            text = stringResource(R.string.solar_midnight),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 10.5.sp,
                                lineHeight = 12.5.sp,
                                lineHeightStyle = trimLineHeight,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp,
                                color = if (isAnyMidnightActive) {
                                    accentColor
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )

                        when {
                            resolvedMidnights.isEmpty() -> {
                                Text(
                                    text = "None today",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 13.5.sp,
                                        lineHeight = 16.sp,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }

                            resolvedMidnights.size == 1 -> {
                                val event = resolvedMidnights.first()
                                val isEventAboveHorizon = isSolarMidnightAboveHorizon(
                                    event.altitude,
                                    horizonThresholdDeg,
                                )
                                val formattedAlt = formatSignedAltitude(event.altitude, locale)
                                val formattedAz = event.azimuth.formatAzimuth(locale)

                                Text(
                                    text = event.time.formatDecimalHours(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        lineHeight = 1.08.em,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                        letterSpacing = (-0.25).sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    autoSize = TextAutoSize.StepBased(
                                        minFontSize = 12.sp,
                                        maxFontSize = 15.5.sp,
                                        stepSize = 0.5.sp,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                if (formattedAlt != null || formattedAz != null) {
                                    val metadataColor = if (isAnyMidnightActive) {
                                        accentColor
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    val altitudeColor = if (isEventAboveHorizon) {
                                        aboveHorizonRed
                                    } else {
                                        metadataColor
                                    }
                                    val separatorColor = metadataColor.copy(alpha = 0.55f)
                                    val metadataText = buildAnnotatedString {
                                        formattedAlt?.let { altText ->
                                            withStyle(
                                                SpanStyle(
                                                    color = altitudeColor,
                                                    fontWeight = if (isEventAboveHorizon) {
                                                        FontWeight.Bold
                                                    } else {
                                                        FontWeight.SemiBold
                                                    },
                                                ),
                                            ) {
                                                append(altText)
                                            }
                                        }
                                        if (formattedAlt != null && formattedAz != null) {
                                            withStyle(SpanStyle(color = separatorColor)) {
                                                append(" · ")
                                            }
                                        }
                                        formattedAz?.let { azText ->
                                            append(azText)
                                        }
                                    }

                                    Text(
                                        text = metadataText,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            lineHeight = 13.5.sp,
                                            lineHeightStyle = trimLineHeight,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            fontWeight = FontWeight.SemiBold,
                                            fontFeatureSettings = "tnum",
                                            color = metadataColor,
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

                            else -> {
                                // Two solar midnights in one calendar day (fast September/March EoT
                                // or 25h DST fall-back): each row is individually tappable.
                                CompositionLocalProvider(
                                    LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(1.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                    ) {
                                        resolvedMidnights.take(2).forEach { event ->
                                            val isRowActive = isCulminationActive(event.time, currentTime)
                                            val isRowAboveHorizon = isSolarMidnightAboveHorizon(
                                                event.altitude,
                                                horizonThresholdDeg,
                                            )
                                            val formattedAlt = formatSignedAltitude(event.altitude, locale)
                                            val formattedAz = event.azimuth.formatAzimuth(locale)
                                            val metaColor = if (isRowActive) {
                                                accentColor
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                            val altColor = if (isRowAboveHorizon) aboveHorizonRed else metaColor
                                            val rowMeta = buildAnnotatedString {
                                                formattedAlt?.let { altText ->
                                                    withStyle(
                                                        SpanStyle(
                                                            color = altColor,
                                                            fontWeight = if (isRowAboveHorizon) {
                                                                FontWeight.Bold
                                                            } else {
                                                                FontWeight.SemiBold
                                                            },
                                                        ),
                                                    ) {
                                                        append(altText)
                                                    }
                                                }
                                                if (formattedAlt != null && formattedAz != null) {
                                                    withStyle(SpanStyle(color = metaColor.copy(alpha = 0.55f))) {
                                                        append(" · ")
                                                    }
                                                }
                                                formattedAz?.let { append(it) }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .then(
                                                        if (onMidnightClick != null) {
                                                            Modifier.clickable { onMidnightClick(event.elapsedHours) }
                                                        } else {
                                                            Modifier
                                                        },
                                                    )
                                                    .padding(horizontal = 2.dp),
                                            ) {
                                                Text(
                                                    text = event.time.formatDecimalHours(),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        lineHeight = 13.sp,
                                                        lineHeightStyle = trimLineHeight,
                                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                        fontWeight = FontWeight.Bold,
                                                        fontFeatureSettings = "tnum",
                                                        letterSpacing = (-0.2).sp,
                                                        color = if (isRowActive) {
                                                            accentColor
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        },
                                                    ),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    autoSize = TextAutoSize.StepBased(
                                                        minFontSize = 9.5.sp,
                                                        maxFontSize = 11.5.sp,
                                                        stepSize = 0.5.sp,
                                                    ),
                                                )
                                                if (rowMeta.isNotEmpty()) {
                                                    Text(
                                                        text = rowMeta,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            lineHeight = 11.5.sp,
                                                            lineHeightStyle = trimLineHeight,
                                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontFeatureSettings = "tnum",
                                                            color = metaColor,
                                                        ),
                                                        textAlign = TextAlign.End,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        autoSize = TextAutoSize.StepBased(
                                                            minFontSize = 8.5.sp,
                                                            maxFontSize = 9.5.sp,
                                                            stepSize = 0.5.sp,
                                                        ),
                                                        modifier = Modifier.padding(start = 3.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isAnyMidnightActive) {
                        NightNowBadge(
                            accentColor = accentColor,
                            onBadgeColor = onBadgeColor,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 5.dp),
                        )
                    }
                }
            }

            if (singlePillClick != null) {
                Surface(
                    onClick = singlePillClick,
                    shape = RoundedCornerShape(14.dp),
                    color = midnightPillColor,
                    border = midnightBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.57f),
                    content = midnightPillContent,
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = midnightPillColor,
                    border = midnightBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.57f),
                    content = midnightPillContent,
                )
            }
        }
    }
}

@Composable
private fun NightNowBadge(
    accentColor: Color,
    onBadgeColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = accentColor,
        modifier = modifier,
    ) {
        Text(
            text = "Now",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.5.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Bold,
                color = onBadgeColor,
            ),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 1.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun NightPreviewLight() {
    val activeMidnightTime = ZonedDateTime.of(2026, 11, 24, 0, 48, 0, 0, ZoneOffset.UTC)
    val daytimeTime = ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, ZoneOffset.UTC)
    HeliosTheme(darkTheme = false, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(Color(0xFFECEEF4))
                .padding(16.dp),
        ) {
            // Row 1: Night paired directly with PlutoTime (as on Sun.kt)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Night(
                        durationHours = 8.404,
                        midnightTime = 0.80,
                        midnightAzimuth = 0.0,
                        midnightAltitude = -32.4,
                        currentTime = daytimeTime,
                        currentAltitude = 45.0,
                        onMidnightClick = {},
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    PlutoTime(
                        dawnLower = 6.23,
                        dawnUpper = 6.32,
                        duskUpper = 19.30,
                        duskLower = 19.38,
                        morningDuration = 0.083,
                        eveningDuration = 0.083,
                        currentTime = daytimeTime,
                    )
                }
            }

            // Row 2: Midnight Sun (altitude above sunrise threshold -> red) + Double Solar Midnight (fast EoT)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Night(
                        durationHours = 0.0,
                        midnightTime = 0.80,
                        midnightAzimuth = 0.0,
                        midnightAltitude = 4.8,
                        currentTime = activeMidnightTime,
                        currentAltitude = 4.8,
                        onMidnightClick = {},
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Night(
                        durationHours = 9.15,
                        midnightTime = 0.004,
                        midnightAzimuth = 0.0,
                        midnightAltitude = -37.2,
                        midnightEvents = listOf(
                            SolarEphemeris.SolarMidnightEvent(
                                time = 0.004,
                                elapsedHours = 0.004,
                                altitude = -37.2,
                                azimuth = 0.0,
                            ),
                            SolarEphemeris.SolarMidnightEvent(
                                time = 23.988,
                                elapsedHours = 23.988,
                                altitude = -37.6,
                                azimuth = 0.0,
                            ),
                        ),
                        currentTime = daytimeTime,
                        currentAltitude = 35.0,
                        onMidnightClick = {},
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun NightPreviewDark() {
    val activeMidnightTime = ZonedDateTime.of(2026, 12, 21, 0, 52, 0, 0, ZoneOffset.UTC)
    val lateEveningTime = ZonedDateTime.of(2026, 12, 21, 22, 15, 0, 0, ZoneOffset.UTC)
    HeliosTheme(darkTheme = true, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(Color(0xFF0E1015))
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Night(
                        durationHours = 12.35,
                        midnightTime = null,
                        midnightAzimuth = 0.0,
                        midnightAltitude = -61.8,
                        currentTime = lateEveningTime,
                        currentAltitude = -34.0,
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Night(
                        durationHours = 24.0,
                        midnightTime = 0.87,
                        midnightAzimuth = 0.0,
                        midnightAltitude = -24.1,
                        currentTime = activeMidnightTime,
                        currentAltitude = -24.1,
                        onMidnightClick = {},
                    )
                }
            }
        }
    }
}
