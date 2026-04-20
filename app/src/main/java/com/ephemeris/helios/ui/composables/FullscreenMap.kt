package com.ephemeris.helios.ui.composables

import android.content.Context
import android.graphics.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ephemeris.helios.utils.location.Coordinates
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.core.graphics.createBitmap
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenMap(
    location: Coordinates,
    currentSolarPosition: SolarEphemeris.SolarPosition,
    solarEvents: SolarEphemeris.DailyEvents,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveIsDarkTheme = isSystemInDarkTheme()
    val colors = LocalCustomColors.current

    val centerCoordinates = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerCoordinates, 10f)
    }

    val currentZoom = cameraPositionState.position.zoom.takeIf { it > 0f }?.toDouble() ?: 13.0
    val radiusMeters = 2500.0 * 2.0.pow(13.0 - currentZoom)  // Compass ring size
    val textRadiusMeters = radiusMeters * 1.08  // Places text 8% outside the main circle
    // --- SPHERICAL UTILITY MAGIC ---
    // Calculate exactly where the lines should end on the edge of the 2km circle
    // Note: Ensure your azimuth properties match your actual data class variable names!
    val sunrisePoint = SphericalUtil.computeOffset(centerCoordinates, radiusMeters, solarEvents.sunriseAzimuth!!)
    val sunsetPoint = SphericalUtil.computeOffset(centerCoordinates, radiusMeters, solarEvents.sunsetAzimuth!!)
    val currentSunPoint = SphericalUtil.computeOffset(centerCoordinates, radiusMeters, currentSolarPosition.azimuth)


    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            compassEnabled = true,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = true,
            rotationGesturesEnabled = true,
            zoomControlsEnabled = false,
            zoomGesturesEnabled = true
        ),
        googleMapOptionsFactory = {
            GoogleMapOptions().mapColorScheme(
                if (effectiveIsDarkTheme) {
                    MapColorScheme.DARK
                } else {
                    MapColorScheme.LIGHT
                }
            )
        }
    ) {
        // 1. The Outer Compass Ring
        Circle(
            center = centerCoordinates,
            radius = radiusMeters,
            fillColor = Color.Transparent,
            strokeColor = Color.Gray.copy(alpha = 0.4f),
            strokeWidth = 4f
        )

        // 2. Sunrise Line
        Polyline(
            points = listOf(centerCoordinates, sunrisePoint),
            color = Color(0xFFFFB300), // Amber/Orange
            width = 6f,
//            pattern = listOf(Dash(20f), Gap(20f))
        )

        // 3. Sunset Line
        Polyline(
            points = listOf(centerCoordinates, sunsetPoint),
            color = Color(0xFFFF5252), // Red/Orange
            width = 6f,
//            pattern = listOf(Dash(20f), Gap(20f))
        )

        // 4. Current Sun Line (Solid)
        Polyline(
            points = listOf(centerCoordinates, currentSunPoint),
            color = colors.sun,
            width = 8f
        )

        // 5. Compass Direction Text Band (N, NE, E, etc.)
        val directions = listOf(
            0.0 to "N", 45.0 to "NE", 90.0 to "E", 135.0 to "SE",
            180.0 to "S", 225.0 to "SW", 270.0 to "W", 315.0 to "NW"
        )
        val textColor = if (effectiveIsDarkTheme) Color.LightGray else Color.DarkGray
        directions.forEach { (azimuth, label) ->
            val point = SphericalUtil.computeOffset(centerCoordinates, textRadiusMeters, azimuth)

            // Cache the text bitmap so we don't redraw it every frame
            val textIcon = remember(label, textColor) {
                bitmapDescriptorFromText(label, textColor)
            }

            // Note: We DO use `remember` here because the compass labels never move relative to the center
            Marker(
                state = remember { MarkerState(position = point) },
                icon = textIcon,
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
            )
        }

        // 6. The Sun Icon Marker
        val sunIcon = remember(colors.sun) {
            bitmapDescriptorFromVector(context, R.drawable.ic_brightness_empty_filled, colors.sun)
        }
        val sunMarkerState = rememberUpdatedMarkerState(position = currentSunPoint)
        sunMarkerState.position = currentSunPoint

        Marker(
            state = sunMarkerState,
            icon = sunIcon,
            anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f), // Centers the icon perfectly on the line end
            title = "Sun",
            snippet = "Azimuth: ${currentSolarPosition.azimuth.toInt()}°"
        )
    }
}

fun bitmapDescriptorFromVector(
    context: Context,
    vectorResId: Int,
    tintColor: androidx.compose.ui.graphics.Color
): BitmapDescriptor? {
    val drawable = ContextCompat.getDrawable(context, vectorResId) ?: return null

    // Apply your theme color to the vector
    drawable.setTint(android.graphics.Color.argb(
        (tintColor.alpha * 255).toInt(),
        (tintColor.red * 255).toInt(),
        (tintColor.green * 255).toInt(),
        (tintColor.blue * 255).toInt()
    ))

    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
    val canvas = Canvas(bitmap)
    drawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun bitmapDescriptorFromText(
    text: String,
    textColor: Color
): BitmapDescriptor {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            (textColor.alpha * 255).toInt(),
            (textColor.red * 255).toInt(),
            (textColor.green * 255).toInt(),
            (textColor.blue * 255).toInt()
        )
        textSize = 40f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val padding = 10f
    val baseline = -paint.ascent() + padding
    val width = (paint.measureText(text) + padding * 2).toInt()
    val height = (baseline + paint.descent() + padding).toInt()

    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Ensure the background is totally transparent
    canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
    canvas.drawText(text, width / 2f, baseline, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}