package com.ephemeris.helios.ui.composables.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.ui.composables.charts.MoonPhaseIcon
import com.ephemeris.helios.ui.composables.charts.rememberChartIconDrawer
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.calc.RiseSetEvent
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.formatDirection
import com.ephemeris.helios.utils.formatDuration
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CelestialBodyTelemetry {
    SUN,
    MOON,
}

internal val ExpandedTelemetryRowHeight = 54.dp
internal val CollapsedTelemetryRowHeight = 38.dp

internal val CompactLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun formatTelemetryAltitude(altitudeDeg: Double, locale: Locale): String {
    if (altitudeDeg.isNaN()) return "--°"
    val rounded = (altitudeDeg * 10.0).roundToInt() / 10.0
    val safe = if (abs(rounded) < 0.05) 0.0 else rounded
    val sign = if (safe < 0.0) "\u2212" else ""
    return "$sign${String.format(locale, "%.1f", abs(safe))}°"
}

internal fun formatTelemetryAzimuthParts(azimuthDeg: Double?, locale: Locale): Pair<String, String> {
    if ((azimuthDeg == null) || azimuthDeg.isNaN()) return "--°" to ""
    val normalized = ((azimuthDeg % 360.0) + 360.0) % 360.0
    val roundedTenths = (normalized * 10.0).roundToInt() % 3600
    val wrappedDeg = roundedTenths / 10.0
    val degText = String.format(locale, "%.1f°", wrappedDeg)
    val cardinal = wrappedDeg.formatDirection()
    return degText to cardinal
}

@Composable
private fun resolvePhasePillColors(
    body: CelestialBodyTelemetry,
    phase: String,
    isDarkSurface: Boolean,
): Pair<Color, Color> {
    val customColors = LocalCustomColors.current
    val normalized = phase.lowercase(Locale.ROOT)

    val (targetBg, targetFg) = when (body) {
        CelestialBodyTelemetry.SUN -> when {
            normalized.contains("daylight") -> {
                val accent = customColors.sunPath
                accent.copy(alpha = if (isDarkSurface) 0.22f else 0.14f) to accent
            }
            normalized.contains("civil") -> {
                val accent = if (isDarkSurface) Color(0xFFD1C4E9) else Color(0xFF673AB7)
                accent.copy(alpha = if (isDarkSurface) 0.20f else 0.13f) to accent
            }
            normalized.contains("nautical") -> {
                val accent = if (isDarkSurface) Color(0xFFB39DDB) else Color(0xFF4527A0)
                accent.copy(alpha = if (isDarkSurface) 0.22f else 0.14f) to accent
            }
            normalized.contains("astronomical") || normalized.contains("astro") -> {
                val accent = if (isDarkSurface) Color(0xFFB388FF) else Color(0xFF311B92)
                accent.copy(alpha = if (isDarkSurface) 0.22f else 0.14f) to accent
            }
            normalized.contains("night") -> {
                if (isDarkSurface) {
                    Color(0xFF463F77).copy(alpha = 0.50f) to Color(0xFFE5DEFF)
                } else {
                    Color(0xFFE5DEFF).copy(alpha = 0.85f) to Color(0xFF3E366E)
                }
            }
            else -> {
                MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            }
        }

        CelestialBodyTelemetry.MOON -> {
            val accent = customColors.moonPath
            accent.copy(alpha = if (isDarkSurface) 0.20f else 0.13f) to accent
        }
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 220),
        label = "phasePillBg",
    )
    val animatedFg by animateColorAsState(
        targetValue = targetFg,
        animationSpec = tween(durationMillis = 220),
        label = "phasePillFg",
    )
    return animatedBg to animatedFg
}

/**
 * Material 3 Expressive telemetry strip shown directly beneath daily solar/lunar chart cards
 * and in the Maps bottom summary.
 *
 * Uses deterministic weighted slots and a fixed height so variable-length phase strings
 * (e.g., `"Night"` vs. `"Astronomical Twilight"`) never shift Altitude or Direction horizontally
 * or cause vertical card jumps.
 */
@Composable
fun ChartTelemetryRow(
    body: CelestialBodyTelemetry,
    altitude: Double,
    azimuth: Double,
    modifier: Modifier = Modifier,
    phase: String = "",
    moonPhase: MoonPhaseAppearance = MoonPhaseAppearance.Full,
    shape: Shape = RoundedCornerShape(
        topStart = 5.dp,
        topEnd = 5.dp,
        bottomStart = 12.dp,
        bottomEnd = 12.dp,
    ),
    showBodyIcon: Boolean = true,
    collapseFraction: Float = 0f,
    drawBottomDivider: Boolean = true,
) {
    val t = collapseFraction.coerceIn(0f, 1f)
    val locale = LocalLocale.current.platformLocale
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme
    val isDarkSurface = colorScheme.surface.luminance() < 0.5f

    val bodyAccent = when (body) {
        CelestialBodyTelemetry.SUN -> customColors.sunPath
        CelestialBodyTelemetry.MOON -> customColors.moonPath
    }
    val bodyIconContentDesc = when (body) {
        CelestialBodyTelemetry.SUN -> stringResource(R.string.sun)
        CelestialBodyTelemetry.MOON -> stringResource(R.string.moon)
    }

    val altitudeText = formatTelemetryAltitude(altitude, locale)
    val (azimuthDegText, azimuthCardinal) = formatTelemetryAzimuthParts(azimuth, locale)
    val showPhasePill = phase.isNotBlank()

    // Keep phase pill colors stably animated across both expanded and compact layouts
    val phaseColors = if (showPhasePill) {
        resolvePhasePillColors(
            body = body,
            phase = phase,
            isDarkSurface = isDarkSurface,
        )
    } else {
        null
    }

    // App label style (GoogleSansFlex, Title Case, tight tracking)
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 10.5.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        color = colorScheme.onSurfaceVariant,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CompactLineHeightStyle,
    )

    // App metric style (headlineSmall -> GoogleSansFlexRounded, tabular figures)
    val metricBaseStyle = MaterialTheme.typography.headlineSmall.copy(
        fontSize = 20.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = "tnum",
        letterSpacing = (-0.3).sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CompactLineHeightStyle,
    )

    val compactMetricStyle = MaterialTheme.typography.headlineSmall.copy(
        fontSize = 15.5.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
        letterSpacing = (-0.2).sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CompactLineHeightStyle,
    )

    val azimuthAnnotated = buildAnnotatedString {
        append(azimuthDegText)
        if (azimuthCardinal.isNotEmpty()) {
            append(" ")
            withStyle(
                SpanStyle(
                    color = bodyAccent.copy(alpha = 0.90f),
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                append(azimuthCardinal)
            }
        }
    }

    val rowHeight = lerpDp(ExpandedTelemetryRowHeight, CollapsedTelemetryRowHeight, t)
    val containerColor = lerpColor(colorScheme.surface, colorScheme.surfaceContainer, t)
    val horizontalPadding = lerpDp(10.dp, 0.dp, t)
    val verticalPadding = lerpDp(6.dp, 4.dp, t)
    val bodyBoxSize = lerpDp(32.dp, 22.dp, t)
    val sunIconSize = lerpDp(30.dp, 21.dp, t)
    val moonIconSize = lerpDp(28.dp, 20.dp, t)
    val bodyIconSpacer = lerpDp(10.dp, 8.dp, t)

    val expandedAlpha = (1f - t * 2.2f).coerceIn(0f, 1f)
    val compactAlpha = ((t - 0.38f) / 0.55f).coerceIn(0f, 1f)
    val bottomDividerAlpha = if (drawBottomDivider) {
        ((t - 0.35f) / 0.65f).coerceIn(0f, 1f) * 0.45f
    } else {
        0f
    }
    val dividerColor = colorScheme.outlineVariant

    Surface(
        shape = shape,
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .drawWithContent {
                drawContent()
                if (bottomDividerAlpha > 0.005f) {
                    val strokePx = 1.dp.toPx()
                    drawLine(
                        color = dividerColor.copy(alpha = bottomDividerAlpha),
                        start = Offset(0f, size.height - strokePx / 2f),
                        end = Offset(size.width, size.height - strokePx / 2f),
                        strokeWidth = strokePx,
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBodyIcon) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(bodyBoxSize)
                        .semantics { contentDescription = bodyIconContentDesc },
                ) {
                    when (body) {
                        CelestialBodyTelemetry.SUN -> {
                            val sunIconDrawer = rememberChartIconDrawer(Charts.Sun.Daily.Elevation)
                            Spacer(
                                modifier = Modifier
                                    .size(sunIconSize)
                                    .drawBehind {
                                        sunIconDrawer(size.minDimension, false)
                                    },
                            )
                        }
                        CelestialBodyTelemetry.MOON -> {
                            MoonPhaseIcon(
                                phase = moonPhase,
                                size = moonIconSize,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(bodyIconSpacer))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                // Expanded 2-line layout
                if (expandedAlpha > 0.01f) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = expandedAlpha
                                translationY = -4.dp.toPx() * t
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Slot 1: Altitude — Hero metric (bold, high-contrast onSurface)
                        Column(
                            modifier = Modifier
                                .weight(if (showPhasePill) 0.74f else 1.0f)
                                .fillMaxHeight()
                                .padding(vertical = 1.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(13.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.altitude),
                                    style = labelStyle,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                            Box(
                                contentAlignment = Alignment.BottomStart,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = altitudeText,
                                    style = metricBaseStyle.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorScheme.onSurface,
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    autoSize = TextAutoSize.StepBased(
                                        minFontSize = 14.sp,
                                        maxFontSize = 20.sp,
                                        stepSize = 0.5.sp,
                                    ),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Slot 2: Direction — Same metric size as Altitude, softer secondary tone + compass & cardinal
                        Column(
                            modifier = Modifier
                                .weight(if (showPhasePill) 1.24f else 1.0f)
                                .fillMaxHeight()
                                .padding(vertical = 1.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.height(13.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_explore_filled),
                                    contentDescription = null,
                                    tint = bodyAccent.copy(alpha = 0.85f),
                                    modifier = Modifier.size(10.5.dp),
                                )
                                Text(
                                    text = stringResource(R.string.direction),
                                    style = labelStyle,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                            Box(
                                contentAlignment = Alignment.BottomStart,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = azimuthAnnotated,
                                    style = metricBaseStyle.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurfaceVariant,
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    autoSize = TextAutoSize.StepBased(
                                        minFontSize = 12.sp,
                                        maxFontSize = 20.sp,
                                        stepSize = 0.5.sp,
                                    ),
                                )
                            }
                        }

                        // Slot 3: Phase — Fixed-width, fixed-height expressive tonal pill (zero layout shift)
                        if (phaseColors != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val (phaseBg, phaseFg) = phaseColors
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = phaseBg,
                                modifier = Modifier
                                    .weight(0.96f)
                                    .fillMaxHeight(),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = phase,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 11.5.sp,
                                            lineHeight = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.sp,
                                            color = phaseFg,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            lineHeightStyle = CompactLineHeightStyle,
                                        ),
                                        maxLines = 2,
                                        autoSize = TextAutoSize.StepBased(
                                            minFontSize = 9.sp,
                                            maxFontSize = 12.sp,
                                            stepSize = 0.5.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                // Compact 1-line layout (shown when chart header is collapsed)
                if (compactAlpha > 0.01f) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = compactAlpha
                                translationY = 4.dp.toPx() * (1f - t)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Slot 1: Inline Altitude
                        Row(
                            modifier = Modifier.weight(if (showPhasePill) 0.86f else 1.0f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.alt_short),
                                style = labelStyle,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = altitudeText,
                                style = compactMetricStyle.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colorScheme.onSurface,
                                ),
                                maxLines = 1,
                                softWrap = false,
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 12.sp,
                                    maxFontSize = 15.5.sp,
                                    stepSize = 0.5.sp,
                                ),
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Slot 2: Inline Direction + compass + cardinal
                        Row(
                            modifier = Modifier.weight(if (showPhasePill) 1.22f else 1.0f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_explore_filled),
                                contentDescription = null,
                                tint = bodyAccent.copy(alpha = 0.85f),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = azimuthAnnotated,
                                style = compactMetricStyle.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurfaceVariant,
                                ),
                                maxLines = 1,
                                softWrap = false,
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 11.sp,
                                    maxFontSize = 15.5.sp,
                                    stepSize = 0.5.sp,
                                ),
                            )
                        }

                        // Slot 3: Single-line Phase pill
                        if (phaseColors != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val (phaseBg, phaseFg) = phaseColors
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = phaseBg,
                                modifier = Modifier
                                    .weight(0.98f)
                                    .height(24.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp),
                                ) {
                                    Text(
                                        text = phase,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 11.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.sp,
                                            color = phaseFg,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            lineHeightStyle = CompactLineHeightStyle,
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        autoSize = TextAutoSize.StepBased(
                                            minFontSize = 8.5.sp,
                                            maxFontSize = 11.sp,
                                            stepSize = 0.5.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val ComboDurationStripHeight = 60.dp
private val RiseSetStripHeight = 40.dp

/**
 * Fixed-height dual-pill footer for [DailyComboPathCard] combining Day Length + Sun rise/set
 * events on the left and Moon Uptime + Moon rise/set events on the right.
 *
 * Uses a side-by-side split inside each pill at a constant [ComboDurationStripHeight] so height
 * never changes across 0 (Midnight Sun / Polar Night / Always Up / Always Down), 1, 2, or 3 daily
 * horizon crossings.
 */
@Composable
fun ComboDurationStrip(
    dayLength: Double,
    moonDuration: Double,
    modifier: Modifier = Modifier,
    sunEvents: List<RiseSetEvent> = emptyList(),
    moonEvents: List<RiseSetEvent> = emptyList(),
    isMoonUpAllDay: Boolean = false,
) {
    val customColors = LocalCustomColors.current
    val isSunUpAllDay = sunEvents.isEmpty() && dayLength >= 12.0

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(ComboDurationStripHeight),
    ) {
        ComboBodySummaryPill(
            body = CelestialBodyTelemetry.SUN,
            iconRes = R.drawable.ic_wb_sunny_filled,
            accentColor = customColors.sunPath,
            durationLabel = stringResource(R.string.day_length),
            durationValue = dayLength.formatDuration(showSeconds = true),
            events = sunEvents.take(3),
            isUpAllDay = isSunUpAllDay,
            shape = RoundedCornerShape(
                topStart = 5.dp,
                topEnd = 5.dp,
                bottomStart = 12.dp,
                bottomEnd = 5.dp,
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        ComboBodySummaryPill(
            body = CelestialBodyTelemetry.MOON,
            iconRes = R.drawable.ic_moon_stars_filled,
            accentColor = customColors.moonPath,
            durationLabel = stringResource(R.string.uptime),
            durationValue = moonDuration.formatDuration(showSeconds = true),
            events = moonEvents.take(3),
            isUpAllDay = isMoonUpAllDay,
            shape = RoundedCornerShape(
                topStart = 5.dp,
                topEnd = 5.dp,
                bottomStart = 5.dp,
                bottomEnd = 12.dp,
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun ComboBodySummaryPill(
    body: CelestialBodyTelemetry,
    iconRes: Int,
    accentColor: Color,
    durationLabel: String,
    durationValue: String,
    events: List<RiseSetEvent>,
    isUpAllDay: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            // Left half: icon + Day Length / Uptime label over tabular duration value
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .weight(1.08f)
                    .fillMaxHeight(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.5.dp),
                    )
                    Text(
                        text = durationLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 10.sp,
                            lineHeight = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = CompactLineHeightStyle,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 8.5.sp,
                            maxFontSize = 10.sp,
                            stepSize = 0.5.sp,
                        ),
                    )
                }
                Text(
                    text = durationValue,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                        letterSpacing = (-0.25).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = CompactLineHeightStyle,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 9.5.sp,
                        maxFontSize = 13.sp,
                        stepSize = 0.5.sp,
                    ),
                )
            }

            Spacer(modifier = Modifier.width(5.dp))
            VerticalDivider(
                thickness = 0.75.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 3.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))

            // Right half: 0, 1, 2, or 3 chronological rise/set crossings inside constant height
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight(),
            ) {
                when (events.size) {
                    0 -> {
                        val noCrossingLabel = when (body) {
                            CelestialBodyTelemetry.SUN -> if (isUpAllDay) "Midnight Sun" else "Polar Night"
                            CelestialBodyTelemetry.MOON -> if (isUpAllDay) "Always Up" else "Always Down"
                        }
                        Text(
                            text = noCrossingLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 10.5.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = CompactLineHeightStyle,
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }

                    1 -> {
                        val event = events[0]
                        val missingText = if (event.isRise) "No set" else "No rise"
                        val missingIconRes = if (event.isRise) {
                            R.drawable.ic_arrow_downward_alt
                        } else {
                            R.drawable.ic_arrow_upward_alt
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            ComboRiseSetTimeItem(
                                event = event,
                                riseTint = accentColor,
                                compact = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    painter = painterResource(missingIconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.size(11.dp),
                                )
                                Text(
                                    text = missingText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 10.5.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        lineHeightStyle = CompactLineHeightStyle,
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }

                    2 -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            ComboRiseSetTimeItem(
                                event = events[0],
                                riseTint = accentColor,
                                compact = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            ComboRiseSetTimeItem(
                                event = events[1],
                                riseTint = accentColor,
                                compact = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    else -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.5.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            events.forEach { event ->
                                ComboRiseSetTimeItem(
                                    event = event,
                                    riseTint = accentColor,
                                    compact = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComboRiseSetTimeItem(
    event: RiseSetEvent,
    riseTint: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val iconRes = if (event.isRise) R.drawable.ic_arrow_upward_alt else R.drawable.ic_arrow_downward_alt
    val iconTint = if (event.isRise) riseTint else MaterialTheme.colorScheme.onSurfaceVariant
    val timeText = event.timeHour.formatDecimalHours()
    val desc = "${stringResource(if (event.isRise) R.string.rise else R.string.set)} $timeText"
    val maxTextSize = if (compact) 10.5.sp else 11.5.sp
    val minTextSize = if (compact) 8.5.sp else 9.sp
    val lineHeight = if (compact) 11.5.sp else 13.sp
    val iconSize = if (compact) 10.5.dp else 11.5.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = desc
        },
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = maxTextSize,
                lineHeight = lineHeight,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onSurface,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = CompactLineHeightStyle,
            ),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(
                minFontSize = minTextSize,
                maxFontSize = maxTextSize,
                stepSize = 0.5.sp,
            ),
        )
    }
}

/**
 * Connected Rise & Set footer matching [ChartTelemetryRow], displaying 0, 1, 2, or 3 daily
 * horizon crossings with local time and azimuth/cardinal direction (used in the Maps bottom
 * summary pager).
 */
@Composable
fun RiseSetTelemetryStrip(
    body: CelestialBodyTelemetry,
    events: List<RiseSetEvent>,
    modifier: Modifier = Modifier,
    isUpAllDay: Boolean = false,
) {
    val customColors = LocalCustomColors.current
    val bodyAccent = when (body) {
        CelestialBodyTelemetry.SUN -> customColors.sunPath
        CelestialBodyTelemetry.MOON -> customColors.moonPath
    }
    val visibleEvents = events.take(3)

    when (visibleEvents.size) {
        0 -> {
            val primaryLabel = when (body) {
                CelestialBodyTelemetry.SUN -> if (isUpAllDay) "Midnight Sun" else "Polar Night"
                CelestialBodyTelemetry.MOON -> if (isUpAllDay) "Always Up" else "Always Down"
            }
            val secondaryLabel = if (isUpAllDay) "Above horizon" else "Below horizon"
            Surface(
                shape = RoundedCornerShape(
                    topStart = 5.dp,
                    topEnd = 5.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp,
                ),
                color = MaterialTheme.colorScheme.surface,
                modifier = modifier
                    .fillMaxWidth()
                    .height(RiseSetStripHeight),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = CompactLineHeightStyle,
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = CompactLineHeightStyle,
                        ),
                    )
                    Text(
                        text = secondaryLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = CompactLineHeightStyle,
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }

        1 -> {
            val event = visibleEvents[0]
            val missingLabel = when (body) {
                CelestialBodyTelemetry.SUN -> if (event.isRise) "No sunset" else "No sunrise"
                CelestialBodyTelemetry.MOON -> if (event.isRise) "No moonset" else "No moonrise"
            }
            val missingIconRes = if (event.isRise) R.drawable.ic_arrow_downward_alt else R.drawable.ic_arrow_upward_alt
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .height(RiseSetStripHeight),
            ) {
                RiseSetTelemetryPill(
                    body = body,
                    event = event,
                    bodyAccent = bodyAccent,
                    shape = RoundedCornerShape(
                        topStart = 5.dp,
                        topEnd = 5.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 5.dp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 5.dp,
                        topEnd = 5.dp,
                        bottomStart = 5.dp,
                        bottomEnd = 12.dp,
                    ),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                    ) {
                        Icon(
                            painter = painterResource(missingIconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = missingLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = CompactLineHeightStyle,
                            ),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        2 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .height(RiseSetStripHeight),
            ) {
                RiseSetTelemetryPill(
                    body = body,
                    event = visibleEvents[0],
                    bodyAccent = bodyAccent,
                    shape = RoundedCornerShape(
                        topStart = 5.dp,
                        topEnd = 5.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 5.dp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                RiseSetTelemetryPill(
                    body = body,
                    event = visibleEvents[1],
                    bodyAccent = bodyAccent,
                    shape = RoundedCornerShape(
                        topStart = 5.dp,
                        topEnd = 5.dp,
                        bottomStart = 5.dp,
                        bottomEnd = 12.dp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        else -> {
            // 3 horizon crossings in one calendar day (e.g., arctic / near-midnight lunar day)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .height(RiseSetStripHeight),
            ) {
                visibleEvents.forEachIndexed { index, event ->
                    val pillShape = when (index) {
                        0 -> RoundedCornerShape(
                            topStart = 5.dp,
                            topEnd = 5.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 5.dp,
                        )
                        visibleEvents.lastIndex -> RoundedCornerShape(
                            topStart = 5.dp,
                            topEnd = 5.dp,
                            bottomStart = 5.dp,
                            bottomEnd = 12.dp,
                        )
                        else -> RoundedCornerShape(5.dp)
                    }
                    CompactTripleRiseSetPill(
                        body = body,
                        event = event,
                        bodyAccent = bodyAccent,
                        shape = pillShape,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RiseSetTelemetryPill(
    body: CelestialBodyTelemetry,
    event: RiseSetEvent,
    bodyAccent: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val iconRes = if (event.isRise) R.drawable.ic_arrow_upward_alt else R.drawable.ic_arrow_downward_alt
    val iconTint = if (event.isRise) bodyAccent else MaterialTheme.colorScheme.onSurfaceVariant
    val cardinalTint = bodyAccent.copy(alpha = 0.90f)
    val label = when (body) {
        CelestialBodyTelemetry.SUN -> stringResource(if (event.isRise) R.string.sunrise else R.string.sunset)
        CelestialBodyTelemetry.MOON -> stringResource(if (event.isRise) R.string.moonrise else R.string.moonset)
    }
    val timeText = event.timeHour.formatDecimalHours()
    val (azimuthDegText, azimuthCardinal) = formatTelemetryAzimuthParts(event.azimuth, locale)
    val azimuthDescription = if (azimuthCardinal.isNotEmpty()) {
        "$azimuthDegText $azimuthCardinal"
    } else {
        azimuthDegText
    }

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .height(RiseSetStripHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label, $timeText, $azimuthDescription"
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                    letterSpacing = (-0.25).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = CompactLineHeightStyle,
                ),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 10.sp,
                    maxFontSize = 13.sp,
                    stepSize = 0.5.sp,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = buildAnnotatedString {
                    append(azimuthDegText)
                    if (azimuthCardinal.isNotEmpty()) {
                        append(" ")
                        withStyle(
                            SpanStyle(
                                color = cardinalTint,
                                fontWeight = FontWeight.Bold,
                            ),
                        ) {
                            append(azimuthCardinal)
                        }
                    }
                },
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                    letterSpacing = (-0.2).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = CompactLineHeightStyle,
                ),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 9.sp,
                    maxFontSize = 11.5.sp,
                    stepSize = 0.5.sp,
                ),
            )
        }
    }
}

@Composable
private fun CompactTripleRiseSetPill(
    body: CelestialBodyTelemetry,
    event: RiseSetEvent,
    bodyAccent: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val iconRes = if (event.isRise) R.drawable.ic_arrow_upward_alt else R.drawable.ic_arrow_downward_alt
    val iconTint = if (event.isRise) bodyAccent else MaterialTheme.colorScheme.onSurfaceVariant
    val cardinalTint = bodyAccent.copy(alpha = 0.90f)
    val label = when (body) {
        CelestialBodyTelemetry.SUN -> stringResource(if (event.isRise) R.string.sunrise else R.string.sunset)
        CelestialBodyTelemetry.MOON -> stringResource(if (event.isRise) R.string.moonrise else R.string.moonset)
    }
    val timeText = event.timeHour.formatDecimalHours()
    val (azimuthDegText, azimuthCardinal) = formatTelemetryAzimuthParts(event.azimuth, locale)
    val azimuthDescription = if (azimuthCardinal.isNotEmpty()) {
        "$azimuthDegText $azimuthCardinal"
    } else {
        azimuthDegText
    }

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .height(RiseSetStripHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label, $timeText, $azimuthDescription"
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                        letterSpacing = (-0.2).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = CompactLineHeightStyle,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 9.5.sp,
                        maxFontSize = 12.sp,
                        stepSize = 0.5.sp,
                    ),
                )
            }
            Text(
                text = buildAnnotatedString {
                    append(azimuthDegText)
                    if (azimuthCardinal.isNotEmpty()) {
                        append(" ")
                        withStyle(
                            SpanStyle(
                                color = cardinalTint,
                                fontWeight = FontWeight.Bold,
                            ),
                        ) {
                            append(azimuthCardinal)
                        }
                    }
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                    letterSpacing = (-0.15).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = CompactLineHeightStyle,
                ),
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 8.5.sp,
                    maxFontSize = 10.5.sp,
                    stepSize = 0.5.sp,
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SunTelemetryPhasesCompactLightPreview() {
    HeliosTheme(darkTheme = false, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
        ) {
            ChartTelemetryRow(
                body = CelestialBodyTelemetry.SUN,
                altitude = 54.3,
                azimuth = 178.6,
                phase = "Daylight",
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 5.dp, bottomEnd = 5.dp),
            )
            ChartTelemetryRow(
                body = CelestialBodyTelemetry.SUN,
                altitude = -3.4,
                azimuth = 287.4,
                phase = "Civil Twilight",
                shape = RoundedCornerShape(5.dp),
            )
            ChartTelemetryRow(
                body = CelestialBodyTelemetry.SUN,
                altitude = -9.1,
                azimuth = 296.8,
                phase = "Nautical Twilight",
                shape = RoundedCornerShape(5.dp),
            )
            ChartTelemetryRow(
                body = CelestialBodyTelemetry.SUN,
                altitude = -15.7,
                azimuth = 307.2,
                phase = "Astronomical Twilight",
                shape = RoundedCornerShape(5.dp),
            )
            ChartTelemetryRow(
                body = CelestialBodyTelemetry.SUN,
                altitude = -32.8,
                azimuth = 359.4,
                phase = "Night",
                shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun SunAndMoonTelemetryDarkPreview() {
    HeliosTheme(darkTheme = true, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
        ) {
            // Standard 2-event Sun + 2-event Moon
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ChartTelemetryRow(
                    body = CelestialBodyTelemetry.SUN,
                    altitude = 42.8,
                    azimuth = 168.4,
                    phase = "Daylight",
                    shape = RoundedCornerShape(5.dp),
                )
                ChartTelemetryRow(
                    body = CelestialBodyTelemetry.MOON,
                    altitude = -12.4,
                    azimuth = 287.4,
                    phase = "Waning Crescent",
                    moonPhase = MoonPhaseAppearance(illuminationFraction = 0.22f, isWaxing = false),
                    shape = RoundedCornerShape(5.dp),
                )
                ComboDurationStrip(
                    dayLength = 14.202,
                    moonDuration = 11.808,
                    sunEvents = listOf(
                        RiseSetEvent(timeHour = 5.58, azimuth = 59.2, isRise = true),
                        RiseSetEvent(timeHour = 19.78, azimuth = 300.8, isRise = false),
                    ),
                    moonEvents = listOf(
                        RiseSetEvent(timeHour = 3.45, azimuth = 248.6, isRise = false),
                        RiseSetEvent(timeHour = 16.72, azimuth = 112.4, isRise = true),
                    ),
                )
            }

            // Arctic 3-event Sun + 1-event Moon
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ChartTelemetryRow(
                    body = CelestialBodyTelemetry.SUN,
                    altitude = 12.1,
                    azimuth = 351.2,
                    phase = "Daylight",
                    shape = RoundedCornerShape(5.dp),
                )
                ChartTelemetryRow(
                    body = CelestialBodyTelemetry.MOON,
                    altitude = 38.9,
                    azimuth = 234.7,
                    phase = "Waxing Gibbous",
                    moonPhase = MoonPhaseAppearance(illuminationFraction = 0.78f, isWaxing = true),
                    shape = RoundedCornerShape(5.dp),
                )
                ComboDurationStrip(
                    dayLength = 22.30,
                    moonDuration = 2.0,
                    sunEvents = listOf(
                        RiseSetEvent(timeHour = 0.21, azimuth = 356.4, isRise = false),
                        RiseSetEvent(timeHour = 1.82, azimuth = 4.2, isRise = true),
                        RiseSetEvent(timeHour = 23.91, azimuth = 355.8, isRise = false),
                    ),
                    moonEvents = listOf(
                        RiseSetEvent(timeHour = 22.0, azimuth = 124.7, isRise = true),
                    ),
                )
            }

            // 0-event Midnight Sun + Always Down Moon
            ComboDurationStrip(
                dayLength = 24.0,
                moonDuration = 0.0,
                sunEvents = emptyList(),
                moonEvents = emptyList(),
                isMoonUpAllDay = false,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ComboTelemetryCompactLightPreview() {
    HeliosTheme(darkTheme = false, dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 5.dp, bottomEnd = 5.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {}
            ChartTelemetryRow(
                body = CelestialBodyTelemetry.SUN,
                altitude = -14.8,
                azimuth = 307.2,
                phase = "Astronomical Twilight",
                shape = RoundedCornerShape(5.dp),
            )
            ChartTelemetryRow(
                body = CelestialBodyTelemetry.MOON,
                altitude = 61.5,
                azimuth = 194.2,
                phase = "Waxing Crescent",
                moonPhase = MoonPhaseAppearance(illuminationFraction = 0.28f, isWaxing = true),
                shape = RoundedCornerShape(5.dp),
            )
            ComboDurationStrip(
                dayLength = 14.202,
                moonDuration = 11.808,
                sunEvents = listOf(
                    RiseSetEvent(timeHour = 5.58, azimuth = 59.2, isRise = true),
                    RiseSetEvent(timeHour = 19.78, azimuth = 300.8, isRise = false),
                ),
                moonEvents = listOf(
                    RiseSetEvent(timeHour = 0.18, azimuth = 351.2, isRise = true),
                    RiseSetEvent(timeHour = 10.42, azimuth = 228.5, isRise = false),
                    RiseSetEvent(timeHour = 23.86, azimuth = 349.8, isRise = true),
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CollapsedTelemetryRowPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeliosTheme(darkTheme = false, dynamicColor = false) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                ChartTelemetryRow(
                    body = CelestialBodyTelemetry.SUN,
                    altitude = 54.3,
                    azimuth = 178.6,
                    phase = "Daylight",
                    shape = RoundedCornerShape(0.dp),
                    collapseFraction = 1f,
                )
                ChartTelemetryRow(
                    body = CelestialBodyTelemetry.SUN,
                    altitude = -15.7,
                    azimuth = 307.2,
                    phase = "Astronomical Twilight",
                    shape = RoundedCornerShape(0.dp),
                    collapseFraction = 1f,
                )
                ChartTelemetryRow(
                    body = CelestialBodyTelemetry.MOON,
                    altitude = 38.2,
                    azimuth = 232.4,
                    phase = "Waning Gibbous",
                    moonPhase = MoonPhaseAppearance(illuminationFraction = 0.72f, isWaxing = false),
                    shape = RoundedCornerShape(0.dp),
                    collapseFraction = 1f,
                )
            }
        }
        HeliosTheme(darkTheme = true, dynamicColor = false) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                ChartTelemetryRow(
                    body = CelestialBodyTelemetry.SUN,
                    altitude = -22.4,
                    azimuth = 351.8,
                    phase = "Night",
                    shape = RoundedCornerShape(0.dp),
                    collapseFraction = 1f,
                )
                ChartTelemetryRow(
                    body = CelestialBodyTelemetry.MOON,
                    altitude = 61.5,
                    azimuth = 194.2,
                    phase = "Waxing Crescent",
                    moonPhase = MoonPhaseAppearance(illuminationFraction = 0.28f, isWaxing = true),
                    shape = RoundedCornerShape(0.dp),
                    collapseFraction = 1f,
                )
            }
        }
    }
}
