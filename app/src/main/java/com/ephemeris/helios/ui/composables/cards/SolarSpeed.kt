package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.theme.LocalCustomColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.pow

private const val MAX_SOLAR_SPEED_DEG_PER_MIN = 0.25f

@Composable
fun SolarSpeed(
    currentSpeedDegPerMin: Double, // Vertical rate: +/- degrees per minute
    modifier: Modifier = Modifier,
    solarNoonAzimuth: Double = 180.0,
) {
    val locale = LocalLocale.current.platformLocale
    val customColors = LocalCustomColors.current
    val speedFloat = currentSpeedDegPerMin.toFloat()
    val absSpeed = abs(speedFloat)

    // Symmetric progression across the full 0..±0.25°/min vertical velocity range
    val description = when {
        absSpeed < 0.015f -> "Level"
        speedFloat > 0f -> when {
            speedFloat >= 0.21f -> "Steep Ascent"
            speedFloat >= 0.15f -> "Rapid Ascent"
            speedFloat >= 0.09f -> "Steady Ascent"
            speedFloat >= 0.04f -> "Gentle Ascent"
            else -> "Slow Ascent"
        }
        else -> when {
            speedFloat <= -0.21f -> "Steep Descent"
            speedFloat <= -0.15f -> "Rapid Descent"
            speedFloat <= -0.09f -> "Steady Descent"
            speedFloat <= -0.04f -> "Gentle Descent"
            else -> "Slow Descent"
        }
    }

    // Align horizontal travel with observer's sky view:
    // - Sun culminating in the South (90°..270°): moves Left -> Right (East on left, West on right).
    // - Sun culminating in the North (270°..90°): moves Right -> Left (East on right, West on left).
    // Vertical tilt follows arcsin(dh/dt / 0.25°/min) so the arrow inclination matches the physical path angle.
    val normalizedNoonAz = ((solarNoonAzimuth % 360.0) + 360.0) % 360.0
    val movesLeftToRight = (normalizedNoonAz in 90.0..270.0)
    val speedRatio = (speedFloat / MAX_SOLAR_SPEED_DEG_PER_MIN).coerceIn(-1f, 1f).toDouble()
    val tiltDeg = Math.toDegrees(asin(speedRatio)).toFloat()
    val arrowAngleDeg = if (movesLeftToRight) -tiltDeg else 180f + tiltDeg

    // Speed-proportional arrow tint anchored to the universal ±0.25°/min equatorial ceiling
    val neutralArrowColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val targetColor = if (speedFloat >= 0f) customColors.warmWave else customColors.coolWave
    val tintStrength = (absSpeed / MAX_SOLAR_SPEED_DEG_PER_MIN).coerceIn(0f, 1f).pow(0.8f)
    val arrowColor = lerp(neutralArrowColor, targetColor, tintStrength)

    val isZero = absSpeed < 0.0005f
    val sign = if (speedFloat > 0f) "+" else "\u2212" // True Unicode minus matching '+' advance width
    val formattedMagnitude = "${String.format(locale, "%.3f", absSpeed)}°"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        VelocityArrowBackground(
            angleDeg = arrowAngleDeg,
            color = arrowColor,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
            ) {
                HeaderEntry(
                    text = stringResource(R.string.velocity),
                    icon = R.drawable.ic_speed_filled,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    iconSize = 20.dp,
                    textSize = 16.sp,
                    spacing = 6.dp,
                )
            }

            val valueStyle = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 25.5.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                fontFeatureSettings = "tnum",
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.align(Alignment.Center),
            ) {
                // Reserve exact '+'/'−' advance width so "0.000°" never shifts horizontally
                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier.alignByBaseline(),
                ) {
                    Text(
                        text = "+$formattedMagnitude",
                        style = valueStyle,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .alpha(0f)
                            .clearAndSetSemantics {},
                    )
                    Text(
                        text = if (isZero) formattedMagnitude else "$sign$formattedMagnitude",
                        style = valueStyle,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Text(
                    text = "/min",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
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
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 38.dp),
            )
        }
    }
}

/**
 * Draws a large, plush Material 3 directional arrow built from analytically tangent circular arcs
 * (matching Google Weather's Wind tile silhouette), rotated by [angleDeg] (`0°` = right,
 * `-90°` = up, `+90°` = down, `180°` = left).
 */
@Composable
private fun VelocityArrowBackground(
    angleDeg: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val circleRadius = size.minDimension / 2f
        if (circleRadius <= 0f) return@Canvas

        val center = Offset(size.width / 2f, size.height / 2f)

        // All 3 outer circular lobes reach 86% of circleRadius with big pillowy corner arcs.
        // The rear indent is a complementary rounded V-corner (tighter concave arc connected by
        // two straight internal tangent lines to the rear wings), matching Google Weather's arrow.
        val noseX = 0.56f * circleRadius
        val noseR = 0.30f * circleRadius
        val wingX = -0.34f * circleRadius
        val wingY = 0.47f * circleRadius
        val wingR = 0.28f * circleRadius
        val waistX = -0.46f * circleRadius
        val waistR = 0.16f * circleRadius
        val waistCenterX = waistX - waistR

        // Exact external tangent angle between top wing circle and nose circle
        val flankDx = (noseX - wingX).toDouble()
        val flankDy = wingY.toDouble()
        val flankDist = hypot(flankDx, flankDy)
        val topFlankDeg = Math.toDegrees(
            atan2(flankDy, flankDx) - (PI / 2.0) -
                asin(((noseR - wingR) / flankDist).coerceIn(-1.0, 1.0)),
        ).toFloat()
        val bottomFlankDeg = -topFlankDeg

        // Exact internal common tangent angle between the concave waist circle and each rear wing
        val tailDx = (wingX - waistCenterX).toDouble()
        val tailDy = wingY.toDouble()
        val tailDist = hypot(tailDx, tailDy)
        val tailBeta = atan2(tailDy, tailDx)
        val tailOffset = acos(((wingR + waistR) / tailDist).coerceIn(-1.0, 1.0))
        val waistNormalDeg = Math.toDegrees(tailBeta - tailOffset).toFloat()

        fun circleRect(cx: Float, cy: Float, r: Float): Rect =
            Rect(center.x + cx - r, center.y + cy - r, center.x + cx + r, center.y + cy + r)

        val arrowPath = Path().apply {
            // 1. Nose circular arc (clockwise from top flank to bottom flank)
            arcTo(
                rect = circleRect(noseX, 0f, noseR),
                startAngleDegrees = topFlankDeg,
                sweepAngleDegrees = bottomFlankDeg - topFlankDeg,
                forceMoveTo = true,
            )
            // 2. Bottom flank (implicit straight tangent) + bottom wing arc
            arcTo(
                rect = circleRect(wingX, wingY, wingR),
                startAngleDegrees = bottomFlankDeg,
                sweepAngleDegrees = (180f + waistNormalDeg) - bottomFlankDeg,
                forceMoveTo = false,
            )
            // 3. Bottom straight slanted tail edge (implicit internal tangent) + rounded concave V-apex
            arcTo(
                rect = circleRect(waistCenterX, 0f, waistR),
                startAngleDegrees = waistNormalDeg,
                sweepAngleDegrees = -2f * waistNormalDeg,
                forceMoveTo = false,
            )
            // 4. Top straight slanted tail edge (implicit internal tangent) + top wing arc
            arcTo(
                rect = circleRect(wingX, -wingY, wingR),
                startAngleDegrees = 180f - waistNormalDeg,
                sweepAngleDegrees = (360f + topFlankDeg) - (180f - waistNormalDeg),
                forceMoveTo = false,
            )
            close()
        }

        rotate(degrees = angleDeg, pivot = center) {
            drawPath(path = arrowPath, color = color)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SolarSpeedCircularTilePreview() {
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
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarSpeed(
                        currentSpeedDegPerMin = 0.175,
                        solarNoonAzimuth = 180.0,
                    )
                }
            }
            HeliosTheme(darkTheme = false, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarSpeed(
                        currentSpeedDegPerMin = 0.250,
                        solarNoonAzimuth = 180.0,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarSpeed(
                        currentSpeedDegPerMin = 0.0,
                        solarNoonAzimuth = 180.0,
                    )
                }
            }
            HeliosTheme(darkTheme = true, dynamicColor = false) {
                Card(
                    modifier = Modifier.size(168.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SolarSpeed(
                        currentSpeedDegPerMin = -0.125,
                        solarNoonAzimuth = 0.0,
                    )
                }
            }
        }
    }
}
