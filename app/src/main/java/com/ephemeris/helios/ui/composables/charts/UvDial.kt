package com.ephemeris.helios.ui.composables.charts

import android.graphics.CornerPathEffect
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.LocalCustomColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun UvDial(
    currentUv: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
) {
    val colors = LocalCustomColors.current
    val isLightMode = !isSystemInDarkTheme()

    // 1. Calculate height fraction (capped at 11+ for the visual scale)
    val fraction = (currentUv / 11f).coerceIn(0f, 1f)

    // 2. Exact Color Map pulled directly from UVSlicing.kt
    // Leaving native alphas untouched per your request!
    val rawFillColor = when {
        currentUv < 0.01f -> MaterialTheme.colorScheme.onSurfaceVariant // Nighttime/Zero UV
        currentUv < 2f -> colors.uvDarkGreen
        currentUv < 3f -> colors.uvGreen
        currentUv < 5f -> colors.uvYellow
        currentUv < 6f -> colors.uvAmber
        currentUv < 8f -> colors.uvOrange
        currentUv < 10f -> colors.uvRed
        currentUv < 11f -> colors.uvDarkRed
        else -> colors.uvPurple
    }

    // --- ACCESSIBILITY TRANSFORM ---
    val fillColor = if (isLightMode && rawFillColor.luminance() > 0.4f) {
        Color(
            red = rawFillColor.red * 0.75f,
            green = rawFillColor.green * 0.75f,
            blue = rawFillColor.blue * 0.75f,
            alpha = rawFillColor.alpha
        )
    } else rawFillColor

    // --- Faint Background Match ---
    // Washes out the active color to 15% opacity for the unfilled background
    val trackColor = if (fillColor == Color.Transparent) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    } else {
        fillColor.copy(alpha = 0.15f)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Center mathematically perfectly to align with outer labels
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Scale the badge to 80% so the left-side indicator has room to draw
        val diameter = min(size.width, size.height) * 0.8f

        // --- SUNBURST GEOMETRY MATH ---
        val points = 24 // A 12-pointed scalloped sun badge
        val cornerRadius = diameter * 0.04f
        // Subtract cornerRadius from base radius so the thick stroke fits inside the Canvas bounds
        val baseRadius = (diameter / 2f) - cornerRadius
        val innerRadius = baseRadius * 0.88f

        val sunburstPath = Path().apply {
            for (i in 0 until points) {
                val angle = Math.toRadians((i * 360f / points).toDouble() - 90.0)
                val r = if (i % 2 == 0) baseRadius else innerRadius
                val x = cx + r * cos(angle).toFloat()
                val y = cy + r * sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

        // --- THE FIX: Android Native Canvas Paint ---
        // By using FILL_AND_STROKE, the shape is rasterized as a single uniform body.
        // This completely eliminates the "outlined shadow" overlap artifact!
        val androidSunburst = sunburstPath.asAndroidPath()

        val trackPaint = Paint().apply {
            color = trackColor.toArgb()
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = cornerRadius * 2f
            strokeJoin = Paint.Join.ROUND
            pathEffect = CornerPathEffect(cornerRadius)
            isAntiAlias = true
        }

        val fillPaint = Paint().apply {
            color = fillColor.toArgb()
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = cornerRadius * 2f
            strokeJoin = Paint.Join.ROUND
            pathEffect = CornerPathEffect(cornerRadius)
            isAntiAlias = true
        }

        // Draw Background Track uniformly
        drawContext.canvas.nativeCanvas.drawPath(androidSunburst, trackPaint)

        // 4. Calculate exactly where the continuous decimal fill line should be
        val actualRadius = baseRadius + cornerRadius
        val bottomY = cy + actualRadius
        val topY = cy - actualRadius
        val fillHeightY = bottomY - (bottomY - topY) * fraction

        // 5. Draw the Active Color Fill (Clipped perfectly to the height)
        clipRect(
            left = 0f,
            top = fillHeightY,
            right = size.width,
            bottom = size.height
        ) {
            drawContext.canvas.nativeCanvas.drawPath(androidSunburst, fillPaint)
        }

        // 6. Draw the Left-Side Indicator Triangle
        val triangleWidth = 6.dp.toPx()
        val triangleHeight = 8.dp.toPx()

        // Tip touches just slightly outside the left edge of the sunburst
        val tipX = cx - actualRadius - 2.dp.toPx()

        val trianglePath = Path().apply {
            moveTo(tipX, fillHeightY) // Tip pointing right
            lineTo(tipX - triangleWidth, fillHeightY - triangleHeight / 2f) // Top left
            lineTo(tipX - triangleWidth, fillHeightY + triangleHeight / 2f) // Bottom left
            close()
        }

        val markerColor = Color.White.copy(alpha = 0.85f)
        val androidTriangle = trianglePath.asAndroidPath()

        val trianglePaint = Paint().apply {
            color = markerColor.toArgb()
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2.dp.toPx()
            strokeJoin = Paint.Join.ROUND
            pathEffect = CornerPathEffect(2.dp.toPx())
            isAntiAlias = true
        }

        drawContext.canvas.nativeCanvas.drawPath(androidTriangle, trianglePaint)
    }
}