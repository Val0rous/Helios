package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.GoogleSansFlexRounded
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.EphemerisType
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.formatAzimuth
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.location.Coordinates
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Resolves the rise/set altitude threshold (in degrees) below which a culmination peak
 * never rises above the horizon.
 *
 * When [followApparentHorizonSetting] is `true` and [coordinates] has `useHorizonDip = true`
 * (controlled by `AppSettings.useHorizonDip`), the threshold lowers by the observer's
 * elevation horizon dip (e.g., on a mountain peak).
 */
internal fun resolveCulminationHorizonThreshold(
    type: EphemerisType,
    coordinates: Coordinates?,
    followApparentHorizonSetting: Boolean = true,
): Double {
    val baseThreshold = when (type) {
        EphemerisType.SUN -> SolarEphemeris.ALT_SUNRISE_SUNSET
        EphemerisType.MOON -> LunarEphemeris.ALT_MOONRISE_MOONSET
        else -> SolarEphemeris.ALT_SUNRISE_SUNSET
    }
    if (!followApparentHorizonSetting || (coordinates == null)) {
        return baseThreshold
    }
    return when (type) {
        EphemerisType.SUN -> coordinates.sunApparentHorizonAlt
        EphemerisType.MOON -> coordinates.moonApparentHorizonAlt
        else -> baseThreshold - coordinates.horizonDipDeg
    }
}

internal fun isCulminationBelowHorizon(
    altitude: Double?,
    horizonThresholdDeg: Double,
): Boolean = altitude != null && !altitude.isNaN() && altitude < horizonThresholdDeg

/**
 * Returns `true` while [currentTime] coincides with the culmination instant (within ±30 seconds
 * or matching the exact displayed minute of [eventHour]).
 */
internal fun isCulminationActive(
    eventHour: Double?,
    currentTime: ZonedDateTime?,
): Boolean {
    if (eventHour == null || eventHour.isNaN() || currentTime == null) return false
    val normEvent = ((eventHour % 24.0) + 24.0) % 24.0
    val eventRoundedTotalMinutes = (normEvent * 60.0).roundToInt() % (24 * 60)
    val currentTotalMinutes = currentTime.hour * 60 + currentTime.minute
    if (currentTotalMinutes == eventRoundedTotalMinutes) return true

    val currentHour = currentTime.hour +
        currentTime.minute / 60.0 +
        currentTime.second / 3600.0
    val rawDiff = abs(currentHour - normEvent)
    val circularDiff = minOf(rawDiff, 24.0 - rawDiff)
    return circularDiff <= (30.0 / 3600.0)
}

@Composable
fun SolarNoon(
    noonTime: Double?,
    noonAzimuth: Double?,
    noonAltitude: Double?,
    coordinates: Coordinates?,
    currentTime: ZonedDateTime? = null,
    modifier: Modifier = Modifier,
    followApparentHorizonSetting: Boolean = true,
) {
    Culmination(
        time = noonTime,
        azimuth = noonAzimuth,
        altitude = noonAltitude,
        type = EphemerisType.SUN,
        modifier = modifier,
        coordinates = coordinates,
        currentTime = currentTime,
        followApparentHorizonSetting = followApparentHorizonSetting,
    )
}

@Composable
fun LunarCulmination(
    culminationTime: Double?,
    culminationAzimuth: Double?,
    culminationAltitude: Double?,
    coordinates: Coordinates?,
    currentTime: ZonedDateTime? = null,
    modifier: Modifier = Modifier,
    followApparentHorizonSetting: Boolean = true,
) {
    Culmination(
        time = culminationTime,
        azimuth = culminationAzimuth,
        altitude = culminationAltitude,
        type = EphemerisType.MOON,
        modifier = modifier,
        coordinates = coordinates,
        currentTime = currentTime,
        followApparentHorizonSetting = followApparentHorizonSetting,
    )
}

@Composable
fun Culmination(
    time: Double?,
    azimuth: Double?,
    altitude: Double?,
    type: EphemerisType,
    modifier: Modifier = Modifier,
    coordinates: Coordinates? = null,
    currentTime: ZonedDateTime? = null,
    followApparentHorizonSetting: Boolean = true,
) {
    val locale = LocalLocale.current.platformLocale
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val customColors = LocalCustomColors.current

    val horizonThresholdDeg = resolveCulminationHorizonThreshold(
        type = type,
        coordinates = coordinates,
        followApparentHorizonSetting = followApparentHorizonSetting,
    )
    val hasEvent = time != null && !time.isNaN() && altitude != null && !altitude.isNaN()
    val isBelowHorizon = hasEvent && isCulminationBelowHorizon(altitude, horizonThresholdDeg)
    val isActive = hasEvent && isCulminationActive(time, currentTime)

    val belowHorizonSeed = Color(0xFFD32F2F)
    val belowHorizonTextColor = if (isDarkSurface) Color(0xFFFF8A80) else Color(0xFFC62828)
    val cardTint: Color? = if (isBelowHorizon) {
        belowHorizonSeed.copy(alpha = if (isDarkSurface) 0.17f else 0.11f)
    } else {
        null
    }
    val effectiveBackgroundColor = cardTint?.compositeOver(surfaceColor) ?: surfaceColor

    val headerAccent = if (isBelowHorizon) belowHorizonTextColor else MaterialTheme.colorScheme.primary
    val gaugeAccent = when {
        isBelowHorizon -> belowHorizonTextColor
        type == EphemerisType.SUN -> customColors.sunPath
        type == EphemerisType.MOON -> customColors.moonPath
        else -> MaterialTheme.colorScheme.primary
    }
    val activeAccent = when {
        isBelowHorizon -> belowHorizonTextColor
        type == EphemerisType.SUN -> if (isDarkSurface) Color(0xFFFFCA28) else Color(0xFF8C5200)
        type == EphemerisType.MOON -> if (isDarkSurface) Color(0xFF82B1FF) else Color(0xFF1565C0)
        else -> MaterialTheme.colorScheme.primary
    }
    val activeSeed = when {
        isBelowHorizon -> belowHorizonSeed
        type == EphemerisType.SUN -> Color(0xFFFFB300)
        type == EphemerisType.MOON -> Color(0xFF2979FF)
        else -> MaterialTheme.colorScheme.primary
    }
    val onBadgeColor = if (isDarkSurface) Color(0xFF1A1A1A) else Color.White

    val titleRes = when (type) {
        EphemerisType.SUN -> R.string.solar_noon
        EphemerisType.MOON -> R.string.culmination
        else -> R.string.culmination
    }
    val iconRes = when (type) {
        EphemerisType.SUN -> R.drawable.ic_brightness_7_filled
        EphemerisType.MOON -> R.drawable.ic_brightness_4_filled
        else -> R.drawable.ic_brightness_6_filled
    }

    val formattedAltitude = if (hasEvent) {
        val rounded = (altitude * 10.0).roundToInt() / 10.0
        val sign = if (rounded < 0.0) "\u2212" else ""
        "$sign${String.format(locale, "%.1f", abs(rounded))}°"
    } else {
        "--°"
    }

    val formattedTime = if (hasEvent) time.formatDecimalHours() else null
    val formattedAzimuth = if (hasEvent) azimuth.formatAzimuth(locale) else null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .then(if (cardTint != null) Modifier.background(cardTint) else Modifier)
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        HeaderEntry(
            text = stringResource(titleRes),
            textColor = headerAccent,
            icon = iconRes,
            iconTint = headerAccent,
            horizontalAlignment = Alignment.CenterHorizontally,
            iconSize = 20.dp,
            textSize = 15.5.sp,
            spacing = 6.dp,
        )

        // Hero altitude + uniform horizon-to-zenith slider centered in the middle region
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Text(
                text = formattedAltitude,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 27.5.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum",
                    letterSpacing = (-0.5).sp,
                    color = when {
                        !hasEvent -> MaterialTheme.colorScheme.onSurfaceVariant
                        isBelowHorizon -> belowHorizonTextColor
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )

            CulminationHorizonBar(
                altitudeDeg = if (hasEvent) altitude else null,
                horizonThresholdDeg = horizonThresholdDeg,
                isBelowHorizon = isBelowHorizon,
                activeColor = gaugeAccent,
                trackColor = if (isBelowHorizon) {
                    belowHorizonSeed.copy(alpha = if (isDarkSurface) 0.24f else 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                horizonTickColor = if (isBelowHorizon) {
                    belowHorizonTextColor.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                },
                borderColor = effectiveBackgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
            )
        }

        // Bottom tonal pill: plush padding, Time prominent on top, Azimuth below, active "Now" highlight
        val pillColor = when {
            isActive -> activeSeed.copy(alpha = if (isDarkSurface) 0.30f else 0.22f)
            isBelowHorizon -> surfaceColor.copy(alpha = if (isDarkSurface) 0.46f else 0.72f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val pillBorder = if (isActive) {
            BorderStroke(1.5.dp, activeAccent.copy(alpha = if (isDarkSurface) 0.85f else 0.72f))
        } else {
            null
        }

        Surface(
            shape = RoundedCornerShape(13.dp),
            color = pillColor,
            border = pillBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                if (formattedTime != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Time stays identical size, weight & position whether active or inactive
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = GoogleSansFlexRounded,
                                lineHeight = 1.12.em,
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
                        // Azimuth pinned at exact center; "Now" pill docks bottom-right without shifting it
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (formattedAzimuth != null) {
                                Text(
                                    text = formattedAzimuth,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontSize = 11.5.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum",
                                        color = if (isActive) {
                                            activeAccent
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                            if (isActive) {
                                Surface(
                                    shape = CircleShape,
                                    color = activeAccent,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                ) {
                                    Text(
                                        text = "Now",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            lineHeight = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = onBadgeColor,
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "None today",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/**
 * Horizon-to-zenith altitude slider (`-24°`..`90°`) matching the exact geometry of
 * `EquationOfTimeBar` and `ColorTempBar` (`8.dp` track, `2.5.dp` cutout border,
 * `8.dp`/`5.5.dp` thumb radii, `18.dp` container height), with a vertical reference tick
 * at the effective rise/set horizon threshold and a flush square inner edge against the tick.
 */
@Composable
private fun CulminationHorizonBar(
    altitudeDeg: Double?,
    horizonThresholdDeg: Double,
    isBelowHorizon: Boolean,
    activeColor: Color,
    trackColor: Color,
    horizonTickColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    val activeSegmentPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val borderWidth = 2.5.dp.toPx()
        val trackHeight = 8.dp.toPx()
        val outerTrackHeight = trackHeight + borderWidth * 2f
        val thumbOuterRadius = 8.dp.toPx()
        val thumbInnerRadius = 5.5.dp.toPx()

        val usableWidth = size.width
        val centerY = size.height / 2f
        val trackTop = centerY - trackHeight / 2f
        val outerTrackTop = centerY - outerTrackHeight / 2f

        val thumbTravel = (usableWidth - thumbOuterRadius * 2f).coerceAtLeast(1f)
        val travelStartX = thumbOuterRadius
        val travelEndX = usableWidth - thumbOuterRadius

        // 22% below-horizon zone on the left, 78% above-horizon (threshold..90°) on the right
        val horizonFraction = 0.22f
        val horizonX = travelStartX + thumbTravel * horizonFraction
        val minBelowDeg = horizonThresholdDeg - 24.0

        // 1. Outer cutout capsule border matching EquationOfTimeBar & ColorTempBar
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(0f, outerTrackTop),
            size = Size(usableWidth, outerTrackHeight),
            cornerRadius = CornerRadius(outerTrackHeight / 2f),
        )

        // 2. Inner track capsule
        val innerTrackWidth = (usableWidth - borderWidth * 2f).coerceAtLeast(0f)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(borderWidth, trackTop),
            size = Size(innerTrackWidth, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f),
        )

        // 3. Active filled segment from horizon tick (flush square end) to culmination dot (rounded outer end)
        val dotX = if (altitudeDeg != null) {
            if (altitudeDeg >= horizonThresholdDeg) {
                val aboveSpan = (90.0 - horizonThresholdDeg).coerceAtLeast(1.0)
                val t = ((altitudeDeg - horizonThresholdDeg) / aboveSpan).toFloat().coerceIn(0f, 1f)
                horizonX + (travelEndX - horizonX) * t
            } else {
                val belowSpan = (horizonThresholdDeg - minBelowDeg).coerceAtLeast(1.0)
                val t = ((horizonThresholdDeg - altitudeDeg) / belowSpan).toFloat().coerceIn(0f, 1f)
                horizonX - (horizonX - travelStartX) * t
            }
        } else {
            null
        }

        if (dotX != null) {
            val segLeft = minOf(horizonX, dotX)
            val segRight = maxOf(horizonX, dotX)
            if (segRight - segLeft > 0.5f) {
                val outerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                val isAbove = dotX >= horizonX
                activeSegmentPath.reset()
                activeSegmentPath.addRoundRect(
                    RoundRect(
                        left = segLeft,
                        top = trackTop,
                        right = segRight,
                        bottom = trackTop + trackHeight,
                        topLeftCornerRadius = if (isAbove) CornerRadius.Zero else outerRadius,
                        bottomLeftCornerRadius = if (isAbove) CornerRadius.Zero else outerRadius,
                        topRightCornerRadius = if (isAbove) outerRadius else CornerRadius.Zero,
                        bottomRightCornerRadius = if (isAbove) outerRadius else CornerRadius.Zero,
                    ),
                )
                drawPath(
                    path = activeSegmentPath,
                    color = activeColor.copy(alpha = if (isBelowHorizon) 0.78f else 0.72f),
                )
            }
        }

        // 4. Vertical horizon reference tick (drawn beneath the thumb halo so near-horizon dots stay clean)
        val tickHalfH = 7.dp.toPx()
        drawLine(
            color = if (altitudeDeg != null) horizonTickColor else horizonTickColor.copy(alpha = 0.5f),
            start = Offset(horizonX, centerY - tickHalfH),
            end = Offset(horizonX, centerY + tickHalfH),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // 5. Indicator thumb with matching outer cutout ring & inner dot
        if (dotX != null) {
            val thumbCenter = Offset(dotX, centerY)
            drawCircle(
                color = borderColor,
                radius = thumbOuterRadius,
                center = thumbCenter,
            )
            drawCircle(
                color = activeColor,
                radius = thumbInnerRadius,
                center = thumbCenter,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun CulminationPreviewLight() {
    val rome = Coordinates(41.9, 12.5, altitude = 21.0, useHorizonDip = false)
    val svalbard = Coordinates(78.22, 15.63, altitude = 10.0, useHorizonDip = false)
    val mountainPeak = Coordinates(70.0, 20.0, altitude = 4000.0, useHorizonDip = true)
    val activeNoonTime = ZonedDateTime.of(2026, 6, 21, 13, 9, 0, 0, ZoneOffset.UTC)
    val inactiveTime = ZonedDateTime.of(2026, 6, 21, 9, 0, 0, 0, ZoneOffset.UTC)

    HeliosTheme(darkTheme = false, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(Color(0xFFECEEF4))
                .padding(16.dp),
        ) {
            // Row 1: Active Solar Noon ("Now") + Polar Night Solar Noon (below horizon -> red)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarNoon(
                        noonTime = 13.15,
                        noonAzimuth = 180.0,
                        noonAltitude = 71.5,
                        coordinates = rome,
                        currentTime = activeNoonTime,
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarNoon(
                        noonTime = 11.97,
                        noonAzimuth = 180.0,
                        noonAltitude = -11.6,
                        coordinates = svalbard,
                        currentTime = inactiveTime,
                    )
                }
            }

            // Row 2: Mountain peak (-1.5° visible above -2.86° dip) + Active Moon Culmination
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarNoon(
                        noonTime = 12.18,
                        noonAzimuth = 180.0,
                        noonAltitude = -1.5,
                        coordinates = mountainPeak,
                        currentTime = inactiveTime,
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    LunarCulmination(
                        culminationTime = 21.45,
                        culminationAzimuth = 178.4,
                        culminationAltitude = 46.8,
                        coordinates = rome,
                        currentTime = ZonedDateTime.of(2026, 6, 21, 21, 27, 0, 0, ZoneOffset.UTC),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 392)
@Composable
private fun CulminationPreviewDark() {
    val rome = Coordinates(41.9, 12.5, altitude = 21.0, useHorizonDip = false)
    val svalbard = Coordinates(78.22, 15.63, altitude = 10.0, useHorizonDip = false)
    val inactiveTime = ZonedDateTime.of(2026, 12, 21, 8, 0, 0, 0, ZoneOffset.UTC)

    HeliosTheme(darkTheme = true, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(Color(0xFF121318))
                .padding(16.dp),
        ) {
            // Row 1: Active Moon below horizon (red + "Now") + Moon skipped culmination day (None today)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    LunarCulmination(
                        culminationTime = 3.25,
                        culminationAzimuth = 181.2,
                        culminationAltitude = -4.2,
                        coordinates = svalbard,
                        currentTime = ZonedDateTime.of(2026, 12, 21, 3, 15, 0, 0, ZoneOffset.UTC),
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    LunarCulmination(
                        culminationTime = null,
                        culminationAzimuth = null,
                        culminationAltitude = null,
                        coordinates = rome,
                        currentTime = inactiveTime,
                    )
                }
            }

            // Row 2: Winter Solar Noon (inactive) + Polar Night Solar Noon (inactive)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarNoon(
                        noonTime = 12.12,
                        noonAzimuth = 180.0,
                        noonAltitude = 24.7,
                        coordinates = rome,
                        currentTime = inactiveTime,
                    )
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarNoon(
                        noonTime = 11.97,
                        noonAzimuth = 359.96,
                        noonAltitude = -2.3,
                        coordinates = svalbard,
                        currentTime = inactiveTime,
                    )
                }
            }
        }
    }
}
