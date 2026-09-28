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
    chartType: Charts,
    isApparentElevation: Boolean = false
) {
    val dayFill = colors.dayBackground
    val civilTwilightFill = colors.civilTwilight
    val nauticalTwilightFill = colors.nauticalTwilight
    val astroTwilightFill = colors.astronomicalTwilight
    val nightFill = colors.nightBackground // TODO: add "night" for moon and planets

    val className = chartType.javaClass.simpleName
    val isElevationOrTrajectory = className.contains("Elevation") || className.contains("Trajectory")
    val showTwilights = when (chartType) {
        Charts.Sun.Daily.Trajectory -> true
        else -> false
    }

    fun mapAlt(alt: Float) = mapY(if (isApparentElevation && isElevationOrTrajectory) transformApparentElevation(alt) else alt)

    val nightY = if (showTwilights) mapAlt(-18f) else zeroYPixel

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
            clipRect(top = zeroYPixel, bottom = mapAlt(-6f)) {
                drawRect(
                    color = civilTwilightFill,
                    topLeft = Offset(0f, zeroYPixel),
                    size = Size(params.width, mapAlt(-6f) - zeroYPixel)
                )
            }
            clipRect(top = mapAlt(-6f), bottom = mapAlt(-12f)) {
                drawRect(
                    color = nauticalTwilightFill,
                    topLeft = Offset(0f, mapAlt(-6f)),
                    size = Size(params.width, mapAlt(-12f) - mapAlt(-6f))
                )
            }
            clipRect(top = mapAlt(-12f), bottom = mapAlt(-18f)) {
                drawRect(
                    color = astroTwilightFill,
                    topLeft = Offset(0f, mapAlt(-12f)),
                    size = Size(params.width, mapAlt(-18f) - mapAlt(-12f))
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