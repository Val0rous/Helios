package com.ephemeris.helios.ui.composables.charts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

@Composable
fun rememberChartIconDrawer(chartType: Charts): DrawScope.(iconSize: Float, isBelow: Boolean) -> Unit {
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
        { iconSize, isBelow ->
            val sizeObj = Size(iconSize, iconSize)
            // Draw the main chart icon
            when (chartType) {
                is Charts.Sun -> {
                    val color = if (isBelow) colors.sun.copy(alpha = 0.5f) else colors.sun
                    // Base filled sun
                    with(sunFilled) {
                        draw(size = sizeObj, colorFilter = ColorFilter.tint(color))
                    }
                    // Bonus: Outline overlay for light mode!
                    if (isLightMode && !isBelow) {
                        with(sunOutline) {
                            draw(size = sizeObj, colorFilter = ColorFilter.tint(overlayColor))
                        }
                    }
                }

                is Charts.Moon -> {
                    val color = if (isBelow) colors.moon.copy(alpha = 0.5f) else colors.moon
                    rotate(degrees = -35f, pivot = Offset(iconSize / 2f, iconSize / 2f)) {
                        with(moonPainter) {
                            draw(size = sizeObj, colorFilter = ColorFilter.tint(color))
                        }
                    }
                }

                else -> {
                    val color = if (isBelow) Color.Red else colors.sun// TODO
                    with(indicatorPainter) {
                        draw(size = sizeObj, colorFilter = ColorFilter.tint(color))
                    }
                }
            }
        }
    }
}


fun DrawScope.paintIcon(
    currentXPx: Float,
    currentY: Float,
    currentYPx: Float,
    zeroYPixel: Float,
    chartType: Charts,
    drawChartIcon: DrawScope.(Float, Boolean) -> Unit
) {
    val className = chartType.javaClass.simpleName
    val isUp = when {
        className.contains("Elevation") || className.contains("Trajectory") -> {
            when (chartType) {
                is Charts.Sun -> currentY >= SolarEphemeris.ALT_SUNRISE_SUNSET.toFloat()
                is Charts.Moon -> currentY >= LunarEphemeris.ALT_MOONRISE_MOONSET.toFloat()
                else -> currentY >= 0f
            }
        }
        className.contains("ColorTemperature") -> currentY > 2000f
        className.contains("AirMass") -> currentY > 1f
        else -> currentY > 0f
    }

    if (isUp) {
        val iconSize = 24.dp.toPx()

        clipRect(bottom = (zeroYPixel - 2f)) {
            translate(
                left = currentXPx - iconSize / 2,
                top = currentYPx - iconSize / 2
            ) {
                drawChartIcon(iconSize, false)
            }
        }
    } else {
        val iconSize = 12.dp.toPx()
        translate(
            left = currentXPx - iconSize / 2,
            top = currentYPx - iconSize / 2
        ) {
            drawChartIcon(iconSize, true)
        }
    }
}

@Composable
fun rememberIconBitmapDescriptor(
    isAbove: Boolean,
    drawer: DrawScope.(Float, Boolean) -> Unit
): BitmapDescriptor {
    val density = LocalDensity.current

    // Only re-draw the bitmap if the elevation state, screen density, or theme colors (drawer) change
    return remember(isAbove, density, drawer) {
        val sizeDp = if (isAbove) 24f else 12f
        val sizePx = with(density) { sizeDp.dp.toPx() }
        val intSize = sizePx.toInt().coerceAtLeast(1)

        // 1. Create a blank Compose ImageBitmap
        val imageBitmap = ImageBitmap(intSize, intSize)
        val canvas = androidx.compose.ui.graphics.Canvas(imageBitmap)

        // 2. Open a Compose DrawScope and execute the lambda from ChartIconDrawer!
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(sizePx, sizePx)
        ) {
            drawer(sizePx, !isAbove) // Pass the size and the 'isBelow' boolean
        }

        // 3. Convert to an Android Bitmap for Google Maps
        BitmapDescriptorFactory.fromBitmap(imageBitmap.asAndroidBitmap())
    }
}