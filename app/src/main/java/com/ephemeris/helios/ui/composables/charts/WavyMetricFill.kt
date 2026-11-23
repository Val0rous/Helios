package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * Full-bleed static wavy liquid fill container inspired by Google Weather's Humidity tile.
 *
 * Draws a symmetric cubic-Bézier wave centered on the middle lobe rising from the bottom edge
 * (or descending from the top edge when [fromTop] is `true`) to [fraction] (`0f..1f`), with
 * smooth amplitude tapering at both `0%..5%` and `95%..100%` and foreground [content] rendered
 * cleanly above it.
 */
@Composable
fun WavyMetricFill(
    fraction: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    fromTop: Boolean = false,
    waveAmplitude: Dp = 3.dp,
    waveCycles: Float = 4.5f,
    waveRoundness: Float = 0.52f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val clamped = fraction.coerceIn(0f, 1f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (clamped <= 0f) return@Canvas

            if (clamped >= 1f) {
                drawRect(color = fillColor)
                return@Canvas
            }

            val width = size.width
            val height = size.height
            if ((width <= 0f) || (height <= 0f)) return@Canvas

            // Taper wave amplitude smoothly near both 0% and 100% so fills rise cleanly out of
            // the origin edge and flatten seamlessly into the opposite edge without popping.
            val edgeDistance = minOf(clamped, 1f - clamped)
            val ampPx = waveAmplitude.toPx() * (edgeDistance / 0.05f).coerceIn(0f, 1f)
            // Keep wave crests gracefully inside the card bounds for both tiny and near-full fills.
            val baseY = if (fromTop) {
                ((height - (2f * ampPx)) * clamped) + ampPx
            } else {
                ((height - (2f * ampPx)) * (1f - clamped)) + ampPx
            }
            val crestY = if (fromTop) {
                (baseY + ampPx).coerceIn(0f, height)
            } else {
                (baseY - ampPx).coerceIn(0f, height)
            }
            val troughY = if (fromTop) {
                (baseY - ampPx).coerceIn(0f, height)
            } else {
                (baseY + ampPx).coerceIn(0f, height)
            }

            val halfWave = width / (waveCycles * 2f)
            val centerX = width / 2f
            // Center lobe sits at index 0 (dead-center); even indices are crests, odd indices are troughs.
            val halfWavesFromCenter = ceil(centerX / halfWave).toInt() + 1
            val startIndex = -halfWavesFromCenter
            val controlOffset = halfWave * waveRoundness
            val anchorY = if (fromTop) 0f else height

            val wavePath = Path().apply {
                val startX = centerX + (startIndex * halfWave)
                var prevX = startX
                var prevY = if ((startIndex % 2) == 0) crestY else troughY
                moveTo(prevX, prevY)

                for (idx in (startIndex + 1)..halfWavesFromCenter) {
                    val nextX = centerX + (idx * halfWave)
                    val nextY = if ((idx % 2) == 0) crestY else troughY
                    cubicTo(
                        x1 = prevX + controlOffset,
                        y1 = prevY,
                        x2 = nextX - controlOffset,
                        y2 = nextY,
                        x3 = nextX,
                        y3 = nextY,
                    )
                    prevX = nextX
                    prevY = nextY
                }

                lineTo(prevX, anchorY)
                lineTo(startX, anchorY)
                close()
            }

            clipRect {
                drawPath(path = wavePath, color = fillColor)
            }
        }

        content()
    }
}
