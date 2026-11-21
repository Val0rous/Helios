package com.ephemeris.helios.utils.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ephemeris.helios.ui.theme.CustomColorScheme

private val UV_THRESHOLDS_ASC = floatArrayOf(0.01f, 2f, 3f, 5f, 6f, 8f, 10f, 11f)

fun buildUVSliceBands(
    params: ChartData,
    mapX: (Float) -> Float,
    colors: CustomColorScheme,
): List<HorizontalColorBand> {
    return buildHorizontalColorBands(
        xValues = params.xValues,
        yValues = params.yValues,
        sortedThresholdsAsc = UV_THRESHOLDS_ASC,
        mapX = mapX,
    ) { uv ->
        when {
            uv < 0.01f -> Color.Transparent // Nighttime/Zero UV
            uv < 2f -> colors.uvDarkGreen
            uv < 3f -> colors.uvGreen
            uv < 5f -> colors.uvYellow
            uv < 6f -> colors.uvAmber
            uv < 8f -> colors.uvOrange
            uv < 10f -> colors.uvRed
            uv < 11f -> colors.uvDarkRed
            else -> colors.uvPurple
        }
    }
}

fun DrawScope.drawUVSlices(
    bands: List<HorizontalColorBand>,
    zeroYPixel: Float,
) {
    for (i in bands.indices) {
        val band = bands[i]
        drawRect(
            color = band.color,
            topLeft = Offset(band.startPx, 0f), // Starts at very top of canvas
            size = Size(band.endPx - band.startPx, zeroYPixel), // Extends down to horizon line
        )
    }
}
