package com.ephemeris.helios.ui.composables

import android.content.Context
import android.graphics.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.ephemeris.helios.ui.composables.cards.ChartArrays
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.Polyline
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenMap(
    location: Coordinates,
    currentSolarPosition: SolarEphemeris.SolarPosition,
    currentLunarPosition: LunarEphemeris.LunarPosition,
    solarEvents: SolarEphemeris.DailyEvents,
    lunarEvents: LunarEphemeris.LunarDailyEvents,
    modifier: Modifier = Modifier,
    sunChartArrays: ChartArrays?,
    moonChartArrays: ChartArrays?,
    onMapCenterSettled: (Coordinates) -> Unit
) {
    val context = LocalContext.current
    val effectiveIsDarkTheme = isSystemInDarkTheme()
    val isLightMode = !effectiveIsDarkTheme
    val colors = LocalCustomColors.current

    val centerCoordinates = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerCoordinates, 10f)
    }

    // --- TWO-WAY BINDING: MAP TO VIEWMODEL ---
    // Watch the user panning the map. When they stop, wait 500ms and update the ViewModel
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            delay(500) // The Debounce
            val newTarget = cameraPositionState.position.target
            val oldTarget = LatLng(location.latitude, location.longitude)

            // Only trigger a heavy re-calculation if they actually panned away (e.g. > 10 meters)
            if (SphericalUtil.computeDistanceBetween(oldTarget, newTarget) > 10.0) {
                onMapCenterSettled(
                    location.copy(
                        latitude = newTarget.latitude,
                        longitude = newTarget.longitude,
                        locationName = null, // Null forces ViewModel to fetch new Geocoded Address!
                        timezoneId = null    // Null forces ViewModel to fetch new Timezone!
                    )
                )
            }
        }
    }

    // --- TWO-WAY BINDING: VIEWMODEL TO MAP ---
    // If the global location updates externally (e.g. user hits the GPS button in the TopBar),
    // animate the camera back to the new GPS location.
    LaunchedEffect(location.latitude, location.longitude) {
        val currentTarget = cameraPositionState.position.target
        val newTarget = LatLng(location.latitude, location.longitude)
        if (SphericalUtil.computeDistanceBetween(currentTarget, newTarget) > 10.0) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(newTarget))
        }
    }

    // --- DYNAMIC DRAW CENTER ---
    // Use the camera target as the center for all drawing, so elements never leave the screen
    val drawCenter = cameraPositionState.position.target
    val currentZoom = cameraPositionState.position.zoom.takeIf { it > 0f }?.toDouble() ?: 13.0

    val mercatorScale = cos(Math.toRadians(drawCenter.latitude))
    val radiusMeters = 3000.0 * 2.0.pow(13.0 - currentZoom) * mercatorScale  // Compass ring size
    val textRadiusMeters = radiusMeters * 1.08  // Places text 8% outside the main circle
    // --- SPHERICAL UTILITY MAGIC ---
    // Calculate exactly where the lines should end on the edge of the 2km circle
    // Note: Ensure your azimuth properties match your actual data class variable names!
    val sunrisePoint = solarEvents.sunriseAzimuth?.let { azimuth ->
        SphericalUtil.computeOffset(drawCenter, radiusMeters, azimuth)
    }
    val sunsetPoint = solarEvents.sunsetAzimuth?.let { azimuth ->
        SphericalUtil.computeOffset(drawCenter, radiusMeters, azimuth)
    }

    // PROJECTED SUN POSITION: Maps altitude to the circle's radius!
    val currentSunElevation = currentSolarPosition.altitude
    // Todo: make it pull from official sunrise/sunset boundary
    val currentSunDist = radiusMeters * cos(Math.toRadians(currentSunElevation))
    val currentSunPoint = SphericalUtil.computeOffset(drawCenter, currentSunDist, currentSolarPosition.azimuth)

    val moonrisePoint = lunarEvents.moonriseAzimuth?.let { azimuth ->
        SphericalUtil.computeOffset(drawCenter, radiusMeters, azimuth)
    }
    val moonsetPoint = lunarEvents.moonsetAzimuth?.let { azimuth ->
        SphericalUtil.computeOffset(drawCenter, radiusMeters, azimuth)
    }

    // Projected Moon Position
    val currentMoonElevation = currentLunarPosition.altitude
    val currentMoonDist = radiusMeters * cos(Math.toRadians(currentMoonElevation))
    val currentMoonPoint = SphericalUtil.computeOffset(drawCenter, currentMoonDist, currentLunarPosition.azimuth)

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
            center = drawCenter,
            radius = radiusMeters,
            fillColor = Color.Transparent,
            strokeColor = Color.Gray.copy(alpha = 0.4f),
            strokeWidth = 4f
        )

        // 2a. Sunrise Line
        if (sunrisePoint != null) {
            Polyline(
                points = listOf(drawCenter, sunrisePoint),
                color = Color(0xFFFFB300), // Amber/Orange
                width = 6f,
//            pattern = listOf(Dash(20f), Gap(20f))
            )
        }

        // 2b. Sunset Line
        if (sunsetPoint != null) {
            Polyline(
                points = listOf(drawCenter, sunsetPoint),
                color = Color(0xFFFF5252), // Red/Orange
                width = 6f,
//            pattern = listOf(Dash(20f), Gap(20f))
            )
        }

        // 2c. Moonrise Line
        if (moonrisePoint != null) {
            Polyline(
                points = listOf(drawCenter, moonrisePoint),
                color = MaterialColors.Blue500,
                width = 6f,
            )
        }

        // 2d. Moonset Line
        if (moonsetPoint != null) {
            Polyline(
                points = listOf(drawCenter, moonsetPoint),
                color = MaterialColors.Blue700,
                width = 6f,
            )
        }

        // 3a. Curved Sun Path (Day and Night Segments)
        if (sunChartArrays != null) {
            val elevations = sunChartArrays.yDataSets[Charts.Sun.Daily.Elevation]
            val azimuths = sunChartArrays.xDataSets[Charts.Sun.Daily.Trajectory]

            if (elevations != null && azimuths != null && elevations.isNotEmpty()) {
                val daySegments = mutableListOf<List<LatLng>>()
                val nightSegments = mutableListOf<List<LatLng>>()
                var currentSegment = mutableListOf<LatLng>()
                var wasAbove = elevations[0] >= 0f

                for (i in elevations.indices) {
                    val el = elevations[i]
                    val isAbove = el >= 0f
                    val dist = radiusMeters * cos(Math.toRadians(el.toDouble()))
                    val pt = SphericalUtil.computeOffset(drawCenter, dist, azimuths[i].toDouble())
                    if (isAbove == wasAbove) {
                        currentSegment.add(pt)
                    } else {
                        // The moment we cross the horizon, add the exact crossover point to connect BOTH lists to they connect perfectly!
                        currentSegment.add(pt)
                        if (wasAbove) daySegments.add(currentSegment) else nightSegments.add(currentSegment)
                        currentSegment = mutableListOf(pt)
                        wasAbove = isAbove
                    }
                }
                // Catch any streak that was still going at midnight
                if (currentSegment.isNotEmpty()) {
                    if (wasAbove) daySegments.add(currentSegment) else nightSegments.add(currentSegment)
                }
                daySegments.forEach { pathPoints ->
                    Polyline(
                        points = pathPoints,
                        color = colors.sun.copy(alpha = 0.4f), // Slightly transparent to look like a track
                        width = 6f
                    )
                }
                nightSegments.forEach { pathPoints ->
                    Polyline(
                        points = pathPoints,
                        color = Color.Gray.copy(alpha = 0.4f),
                        width = 6f
                    )
                }
            }
        }

        // 3b. Curved Moon Path
        if (moonChartArrays != null) {
            val elevations = moonChartArrays.yDataSets[Charts.Moon.Daily.Elevation]
            val azimuths = moonChartArrays.xDataSets[Charts.Moon.Daily.Trajectory]

            if (elevations != null && azimuths != null && elevations.isNotEmpty()) {
                val daySegments = mutableListOf<List<LatLng>>()
                val nightSegments = mutableListOf<List<LatLng>>()
                var currentSegment = mutableListOf<LatLng>()
                var wasAbove = elevations[0] >= 0f

                for (i in elevations.indices) {
                    val el = elevations[i]
                    val isAbove = el >= 0f
                    val dist = radiusMeters * cos(Math.toRadians(el.toDouble()))
                    val pt = SphericalUtil.computeOffset(drawCenter, dist, azimuths[i].toDouble())

                    if (isAbove == wasAbove) {
                        currentSegment.add(pt)
                    } else {
                        currentSegment.add(pt)
                        if (wasAbove) daySegments.add(currentSegment) else nightSegments.add(currentSegment)
                        currentSegment = mutableListOf(pt)
                        wasAbove = isAbove
                    }
                }
                // Catch any streak that was still going on at midnight
                if (currentSegment.isNotEmpty()) {
                    if (wasAbove) daySegments.add(currentSegment) else nightSegments.add(currentSegment)
                }
                daySegments.forEach { pathPoints ->
                    Polyline(
                        points = pathPoints,
                        color = colors.moon.copy(alpha = 0.4f), // Slightly transparent to look like a track
                        width = 6f
                    )
                }
                nightSegments.forEach { pathPoints ->
                    Polyline(
                        points = pathPoints,
                        color = Color.Gray.copy(alpha = 0.4f),
                        width = 6f
                    )
                }
            }
        }

        // 4a. Current Sun Line (Solid)
        Polyline(
            points = listOf(drawCenter, currentSunPoint),
            color = colors.sun,
            width = 8f
        )

        // 4b. Current Moon Line (Solid)
        Polyline(
            points = listOf(drawCenter, currentMoonPoint),
            color = colors.moon,
            width = 8f
        )

        // 5. Compass Direction Text Band (N, NE, E, etc.)
        val directions = listOf(
            0.0 to "N", 45.0 to "NE", 90.0 to "E", 135.0 to "SE",
            180.0 to "S", 225.0 to "SW", 270.0 to "W", 315.0 to "NW"
        )
        val textColor = if (effectiveIsDarkTheme) Color.LightGray else Color.DarkGray
        directions.forEach { (azimuth, label) ->
            val point = SphericalUtil.computeOffset(drawCenter, textRadiusMeters, azimuth)

            // Cache the text bitmap so we don't redraw it every frame
            val textIcon = remember(label, textColor) {
                bitmapDescriptorFromText(label, textColor)
            }

            // Explicitly assign the dynamically calculated position to the marker state every frame
            val markerState = rememberUpdatedMarkerState(position = point)
            markerState.position = point

            // Note: We DO use `remember` here because the compass labels never move relative to the center
            Marker(
                state = markerState,
                icon = textIcon,
                anchor = Offset(0.5f, 0.5f)
            )
        }

        // 6a. The Sun Icon Marker
        val isSunAbove = currentSunElevation >= 0.0
        val sunIcon = remember(colors.sun, isSunAbove, isLightMode) {
            bitmapDescriptorForCelestialBody(context, true, isSunAbove, isLightMode, colors.sun)
        }
        val sunMarkerState = rememberUpdatedMarkerState(position = currentSunPoint)
        sunMarkerState.position = currentSunPoint

        Marker(
            state = sunMarkerState,
            icon = sunIcon,
            anchor = Offset(0.5f, 0.5f), // Centers the icon perfectly on the line end
            title = "Sun",
            snippet = "Azimuth: ${currentSolarPosition.azimuth.toInt()}°"
        )

        // 6b. The Moon Icon Marker
        val isMoonAbove = currentMoonElevation >= 0.0
        val moonIcon = remember(colors.moon, isMoonAbove, isLightMode) {
            bitmapDescriptorForCelestialBody(context, false, isMoonAbove, isLightMode, colors.moon)
        }
        val moonMarkerState = rememberUpdatedMarkerState(position = currentMoonPoint)
        moonMarkerState.position = currentMoonPoint

        Marker(
            state = moonMarkerState,
            icon = moonIcon,
            anchor = Offset(0.5f, 0.5f),
            title = "Moon",
            snippet = "Azimuth: ${currentLunarPosition.azimuth.toInt()}°"
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

fun bitmapDescriptorForCelestialBody(
    context: Context,
    isSun: Boolean,
    isAboveHorizon: Boolean,
    isLightMode: Boolean,
    bodyColor: Color
): BitmapDescriptor? {
    val sizeDp = if (isAboveHorizon) 24f else 12f
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()

    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

    if (!isAboveHorizon) {
        // Dim Indicator for Nighttime
        val indicator = ContextCompat.getDrawable(context, R.drawable.ic_circle_filled) ?: return null
        indicator.setTint(android.graphics.Color.argb(
            (0.5f * 255).toInt(),
            (bodyColor.red * 255).toInt(),
            (bodyColor.green * 255).toInt(),
            (bodyColor.blue * 255).toInt()
        ))
        indicator.setBounds(0, 0, sizePx, sizePx)
        indicator.draw(canvas)
    } else {
        if (isSun) {
            val filled = ContextCompat.getDrawable(context, R.drawable.ic_brightness_empty_filled) ?: return null
            filled.setTint(android.graphics.Color.argb(
                (bodyColor.alpha * 255).toInt(), (bodyColor.red * 255).toInt(),
                (bodyColor.green * 255).toInt(), (bodyColor.blue * 255).toInt()
            ))
            filled.setBounds(0, 0, sizePx, sizePx)
            filled.draw(canvas)

            if (isLightMode) {
                val outline = ContextCompat.getDrawable(context, R.drawable.ic_brightness_empty_200_high_emphasis) ?: return null
                // Overlay color matching MaterialColors.Orange900
                val overlayColor = android.graphics.Color.parseColor("#E65100")
                outline.setTint(overlayColor)
                outline.setBounds(0, 0, sizePx, sizePx)
                outline.draw(canvas)
            }
        } else {
            val moon = ContextCompat.getDrawable(context, R.drawable.ic_nightlight_filled) ?: return null
            moon.setTint(android.graphics.Color.argb(
                (bodyColor.alpha * 255).toInt(), (bodyColor.red * 255).toInt(),
                (bodyColor.green * 255).toInt(), (bodyColor.blue * 255).toInt()
            ))
            canvas.save()
            // Match the -35f rotation from ChartIconDrawer
            canvas.rotate(-35f, sizePx / 2f, sizePx / 2f)
            moon.setBounds(0, 0, sizePx, sizePx)
            moon.draw(canvas)
            canvas.restore()
        }
    }

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