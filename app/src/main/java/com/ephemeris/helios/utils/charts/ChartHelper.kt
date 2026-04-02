package com.ephemeris.helios.utils.charts

import kotlin.math.log10


fun mapX(
    x: Float,
    minX: Float,
    maxX: Float,
    width: Float
): Float {
    return ((x - minX) / (maxX - minX)) * width
}

// Canvas Y=0 is at the top, so we invert the Y mapping
fun mapY(
    y: Float,
    minY: Float,
    maxY: Float,
    height: Float,
    drawHeight: Float,
    verticalPaddingPx: Float,
    isLogScale: Boolean = false
): Float {
    return if (isLogScale) {
        // log10(y + 1) safely handles 0 values without throwing negative infinity
        val logY = log10(y.coerceAtLeast(0f) + 1f)
        val logMin = log10(minY.coerceAtLeast(0f) + 1f)
        val logMax = log10(maxY.coerceAtLeast(0f) + 1f)

        val fraction = if (logMax == logMin) 0f else (logY - logMin) / (logMax - logMin)
        height - verticalPaddingPx - (fraction * drawHeight)
    } else {
        val fraction = if (maxY == minY) 0f else (y - minY) / (maxY - minY)
        height - verticalPaddingPx - (fraction * drawHeight)
    }
}