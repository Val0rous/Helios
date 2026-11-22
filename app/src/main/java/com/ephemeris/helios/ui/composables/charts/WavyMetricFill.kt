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
 * Draws a symmetric cubic-Bézier wave centered on the middle peak (`waveCycles = 4.7f` gives
 * 5 full peaks with the outer two valleys softly cropped at the card edges) rising from the
 * bottom edge to [fraction] (`0f..1f`), with foreground [content] rendered cleanly above it.
 */
@Composable
fun WavyMetricFill(
    fraction: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
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

            val ampPx = waveAmplitude.toPx()
            // Keep wave crests gracefully inside the card bounds for both tiny and near-full fills.
            val baseY = ((height - (2f * ampPx)) * (1f - clamped)) + ampPx
            val peakY = (baseY - ampPx).coerceIn(0f, height)
            val valleyY = (baseY + ampPx).coerceIn(0f, height)

            val halfWave = width / (waveCycles * 2f)
            val centerX = width / 2f
            // Peak #3 sits at index 0 (dead-center); even indices are peaks, odd indices are valleys.
            val halfWavesFromCenter = ceil(centerX / halfWave).toInt() + 1
            val startIndex = -halfWavesFromCenter
            val controlOffset = halfWave * waveRoundness

            val wavePath = Path().apply {
                val startX = centerX + (startIndex * halfWave)
                var prevX = startX
                var prevY = if ((startIndex % 2) == 0) peakY else valleyY
                moveTo(prevX, prevY)

                for (idx in (startIndex + 1)..halfWavesFromCenter) {
                    val nextX = centerX + (idx * halfWave)
                    val nextY = if ((idx % 2) == 0) peakY else valleyY
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

                lineTo(prevX, height)
                lineTo(startX, height)
                close()
            }

            clipRect {
                drawPath(path = wavePath, color = fillColor)
            }
        }

        content()
    }
}
