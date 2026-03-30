package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.utils.Charts

@Composable
fun rememberChartIconDrawer(chartType: Charts): DrawScope.(iconSize: Float, isIndicator: Boolean) -> Unit {
    // 1. Resolve all painters once
    val sunFilled = painterResource(id = R.drawable.ic_brightness_empty_filled)
    val sunOutline = painterResource(id = R.drawable.ic_brightness_empty_200_high_emphasis)
    val moonPainter = painterResource(id = R.drawable.ic_nightlight_filled)
    val indicatorPainter = painterResource(id = R.drawable.ic_circle_filled)

    // 2. Resolve colors and theme status
    val isLightMode = !isSystemInDarkTheme()
    val colors = LocalCustomColors.current
    val overlayColor = MaterialColors.Orange900

    // 3. Return a reusable DrawScope extension lambda
    return remember(chartType, isLightMode, colors) {
        { iconSize, isIndicator ->
            val sizeObj = Size(iconSize, iconSize)

            if (isIndicator) {
                // Draw the dim night indicator
                with(indicatorPainter) {
                    draw(
                        size = sizeObj,
                        colorFilter = ColorFilter.tint(colors.sun.copy(alpha = 0.5f))
                    )
                }
            } else {
                // Draw the main chart icon
                when (chartType) {
                    is Charts.Sun -> {
                        // Base filled sun
                        with(sunFilled) {
                            draw(size = sizeObj, colorFilter = ColorFilter.tint(colors.sun))
                        }
                        // Bonus: Outline overlay for light mode!
                        if (isLightMode) {
                            with(sunOutline) {
                                draw(size = sizeObj, colorFilter = ColorFilter.tint(overlayColor))
                            }
                        }
                    }

                    is Charts.Moon -> {
                        with(moonPainter) {
                            draw(size = sizeObj, colorFilter = ColorFilter.tint(colors.moon))
                        }
                    }

                    else -> {
                        with(indicatorPainter) {
                            draw(size = sizeObj, colorFilter = ColorFilter.tint(colors.sun))
                        }
                    }
                }
            }
        }
    }
}