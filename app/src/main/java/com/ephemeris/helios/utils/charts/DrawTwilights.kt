package com.ephemeris.helios.utils.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import com.ephemeris.helios.ui.theme.CustomColorScheme
import com.ephemeris.helios.utils.Charts

fun DrawScope.drawDayNightHorizontalTwilights(
    fillPath: Path,
    colors: CustomColorScheme,
    params: ChartData,
    zeroYPixel: Float,
    mapY: (Float) -> Float,
    chartType: Charts
) {
    val dayFill = colors.dayBackground
    val civilTwilightFill = colors.civilTwilight
    val nauticalTwilightFill = colors.nauticalTwilight
    val astroTwilightFill = colors.astronomicalTwilight
    val nightFill = colors.nightBackground // TODO: add "night" for moon and planets

    val showTwilights = when (chartType) {
        Charts.Sun.Daily.Trajectory -> true
        else -> false
    }

    val nightY = if (showTwilights) mapY(-18f) else zeroYPixel

    clipPath(fillPath) {

        // Day area: Removed 'right = currentXPx' so sunset is always visible by default
        clipRect(bottom = zeroYPixel) {
            drawRect(
                color = dayFill,
                topLeft = Offset(0f, 0f),
                size = Size(params.width, zeroYPixel)
            )
        }

        if (showTwilights) {
            clipRect(top = zeroYPixel, bottom = mapY(-6f)) {
                drawRect(
                    color = civilTwilightFill,
                    topLeft = Offset(0f, zeroYPixel),
                    size = Size(params.width, mapY(-6f) - zeroYPixel)
                )
            }
            clipRect(top = mapY(-6f), bottom = mapY(-12f)) {
                drawRect(
                    color = nauticalTwilightFill,
                    topLeft = Offset(0f, mapY(-6f)),
                    size = Size(params.width, mapY(-12f) - mapY(-6f))
                )
            }
            clipRect(top = mapY(-12f), bottom = mapY(-18f)) {
                drawRect(
                    color = astroTwilightFill,
                    topLeft = Offset(0f, mapY(-12f)),
                    size = Size(params.width, mapY(-18f) - mapY(-12f))
                )
            }
        }
        clipRect(top = nightY) {
            drawRect(
                color = nightFill,
                topLeft = Offset(0f, nightY),
                size = Size(params.width, params.height - nightY)
            )
        }
    }
}