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
    val fills = colors.fills
    return buildHorizontalColorBands(
        xValues = params.xValues,
        yValues = params.yValues,
        sortedThresholdsAsc = UV_THRESHOLDS_ASC,
        mapX = mapX,
    ) { uv ->
        when {
            uv < 0.01f -> Color.Transparent // Nighttime/Zero UV
            uv < 2f -> fills.uvDarkGreen
            uv < 3f -> fills.uvGreen
            uv < 5f -> fills.uvYellow
            uv < 6f -> fills.uvAmber
            uv < 8f -> fills.uvOrange
            uv < 10f -> fills.uvRed
            uv < 11f -> fills.uvDarkRed
            else -> fills.uvPurple
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
