package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Smooth 10-lobed scalloped cookie shape (decalobe) matching Google Weather's UV Index tile.
 */
object DecalobeShape : Shape {
    private const val LOBE_COUNT = 10
    private const val SAMPLES = 160
    private const val INNER_RATIO = 0.89f

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = min(size.width, size.height) / 2f
        val midRadius = maxRadius * ((1f + INNER_RATIO) / 2f)
        val amplitude = maxRadius * ((1f - INNER_RATIO) / 2f)

        val path = Path().apply {
            for (i in 0 until SAMPLES) {
                val theta = (2.0 * PI * i / SAMPLES) - (PI / 2.0)
                val r = midRadius + amplitude * cos(LOBE_COUNT * (theta + PI / 2.0)).toFloat()
                val x = cx + r * cos(theta).toFloat()
                val y = cy + r * sin(theta).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * 5-circle Google Weather-style UV Index tier indicator arranged along the bottom semicircle
 * nestled inside the lower 5 lobes of [DecalobeShape] (`Low`, `Moderate`, `High`, `Very High`, `Extreme`).
 *
 * Each circle has a crisp [borderColor] outer backing/cutout ring so both active and soft-tinted
 * inactive circles stay sharp over the flat vertical UV fill. The active tier pops with full chroma
 * and a slightly larger radius, shifting shades discretely at UV 2 (Low), UV 5 (Moderate), and
 * UV 10 (Very High).
 */
@Composable
fun UvCircleIndicator(
    currentUv: Float,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.surface,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkSurface = colorScheme.surface.luminance() < 0.5f

    // High-chroma popping circle colors (distinct from the softer chart/tile fill shades)
    val lowDarkGreen = if (isDarkSurface) Color(0xFF43A047) else Color(0xFF2E7D32)
    val lowBrightGreen = if (isDarkSurface) Color(0xFF76FF03) else Color(0xFF4CAF50)
    val modYellow = if (isDarkSurface) Color(0xFFFFEE58) else Color(0xFFFDD835)
    val modAmber = if (isDarkSurface) Color(0xFFFFB300) else Color(0xFFFFA000)
    val highOrange = if (isDarkSurface) Color(0xFFFF7043) else Color(0xFFF57C00)
    val veryHighRed = if (isDarkSurface) Color(0xFFFF5252) else Color(0xFFE53935)
    val veryHighDarkRed = if (isDarkSurface) Color(0xFFD32F2F) else Color(0xFFB71C1C)
    val extremePurple = if (isDarkSurface) Color(0xFFBA68C8) else Color(0xFF7B1FA2)

    val inactiveAlpha = if (isDarkSurface) 0.30f else 0.22f
    val activeIndex = when {
        currentUv < 0.05f -> -1
        currentUv < 3f -> 0
        currentUv < 6f -> 1
        currentUv < 8f -> 2
        currentUv < 11f -> 3
        else -> 4
    }

    val tierActiveColors = listOf(
        if (currentUv < 2f) lowDarkGreen else lowBrightGreen,
        if (currentUv < 5f) modYellow else modAmber,
        highOrange,
        if (currentUv < 10f) veryHighRed else veryHighDarkRed,
        extremePurple,
    )
    val tierBaseColors = listOf(
        lowBrightGreen,
        modYellow,
        highOrange,
        veryHighRed,
        extremePurple,
    )

    // Bottom 5 lobe angles of the 10-lobe DecalobeShape (left to right: 162°, 126°, 90°, 54°, 18°)
    val lobeAnglesDeg = doubleArrayOf(162.0, 126.0, 90.0, 54.0, 18.0)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = min(size.width, size.height) / 2f
        val arcRadius = maxRadius * 0.74f

        val borderThickness = 2.dp.toPx()
        val inactiveInnerRadius = 6.dp.toPx()
        val activeInnerRadius = 8.5.dp.toPx()

        for (i in lobeAnglesDeg.indices) {
            val isActive = i == activeIndex
            val angleRad = Math.toRadians(lobeAnglesDeg[i])
            val center = Offset(
                x = cx + arcRadius * cos(angleRad).toFloat(),
                y = cy + arcRadius * sin(angleRad).toFloat(),
            )
            val innerRadius = if (isActive) activeInnerRadius else inactiveInnerRadius
            val outerRadius = innerRadius + borderThickness
            val circleColor = if (isActive) {
                tierActiveColors[i]
            } else {
                tierBaseColors[i].copy(alpha = inactiveAlpha)
            }

            // Solid surface backing disc creates the crisp outer border and masks the vertical fill
            // so translucent pastel inactive dots stay clean and never muddy over the fill.
            drawCircle(
                color = borderColor,
                radius = outerRadius,
                center = center,
            )
            drawCircle(
                color = circleColor,
                radius = innerRadius,
                center = center,
            )
        }
    }
}
