package com.ephemeris.helios.ui.composables.maps

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
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
import com.ephemeris.helios.utils.formatHour
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay
import kotlin.collections.associateWith
import kotlin.math.cos
import kotlin.math.pow
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenAzimuthMap(
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
    val radiusMeters = 3200.0 * 2.0.pow(13.0 - currentZoom) * mercatorScale  // Compass ring size
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
    val currentSunDist = radiusMeters * cos(Math.toRadians(currentSunElevation))
    val currentSunPoint = SphericalUtil.computeOffset(drawCenter, currentSunDist, currentSolarPosition.azimuth)

    val currentSunEdgePoint = SphericalUtil.computeOffset(drawCenter, radiusMeters, currentSolarPosition.azimuth)

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

    val currentMoonEdgePoint = SphericalUtil.computeOffset(drawCenter, radiusMeters, currentLunarPosition.azimuth)

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
        // --- PRE-COMPUTE HOURLY ICON BITMAPS FOR PERFORMANCE ---
        val sunHourlyIcons = remember(colors.sun) {
            (0..24).associateWith { hour ->
                bitmapDescriptorForHourlyMark(formatHour(hour, true, context), colors.sun)
            }
        }

        val moonHourlyIcons = remember(colors.moon) {
            (0..24).associateWith { hour ->
                bitmapDescriptorForHourlyMark(formatHour(hour, true, context), colors.moon)
            }
        }

        // --- PRE-COMPUTE EDGE DOTS ---
        val sunriseDotIcon = remember { bitmapDescriptorForCenterDot(Color(0xFFFFB300)) }
        val sunsetDotIcon = remember { bitmapDescriptorForCenterDot(Color(0xFFFF5252)) }
        val moonriseDotIcon = remember { bitmapDescriptorForCenterDot(MaterialColors.Blue500) }
        val moonsetDotIcon = remember { bitmapDescriptorForCenterDot(MaterialColors.Blue700) }
        val sunEdgeDotIcon = remember(colors.sunPath) { bitmapDescriptorForCenterDot(colors.sunPath) }
        val moonEdgeDotIcon = remember(colors.moonPath) { bitmapDescriptorForCenterDot(colors.moonPath) }

        // 1a. The Outer Compass Ring
        Circle(
            center = drawCenter,
            radius = radiusMeters,
            fillColor = Color.Transparent,
            strokeColor = Color.Gray.copy(alpha = 0.4f),
            strokeWidth = 4f
        )

        // 1b. Translucent Dark Gray Band for Compass Directions
        Circle(
            center = drawCenter,
            radius = textRadiusMeters,
            fillColor = Color.Transparent,
            strokeColor = if (isLightMode) Color.DarkGray.copy(alpha = 0.5f) else Color.DarkGray.copy(alpha = 0.5f),
            strokeWidth = 55f
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

        // 3a. Curved Sun Path (Day and Night Segments) & Yellow Perimeter
        if (sunChartArrays != null) {
            val elevations = sunChartArrays.yDataSets[Charts.Sun.Daily.Elevation]
            val azimuths = sunChartArrays.xDataSets[Charts.Sun.Daily.Trajectory]

            if (elevations != null && azimuths != null && elevations.isNotEmpty()) {
                val daySegments = mutableListOf<List<LatLng>>()
                val nightSegments = mutableListOf<List<LatLng>>()
                var currentSegment = mutableListOf<LatLng>()
                var wasAbove = elevations[0] >= SolarEphemeris.ALT_SUNRISE_SUNSET.toFloat()

                for (i in elevations.indices) {
                    val el = elevations[i]
                    val isAbove = el >= SolarEphemeris.ALT_SUNRISE_SUNSET.toFloat()
                    val dist = radiusMeters * cos(Math.toRadians(el.toDouble()))
                    val pt = SphericalUtil.computeOffset(drawCenter, dist, azimuths[i].toDouble())

                    if (isAbove == wasAbove) {
                        currentSegment.add(pt)
                    } else {
                        // The moment we cross the horizon, add the exact crossover point to connect BOTH lists to they connect perfectly!
                        currentSegment.add(pt)
                        if (wasAbove) {
                            // Transition to Night
                            daySegments.add(currentSegment)
                        } else {
                            // Transition to Day
                            nightSegments.add(currentSegment)
                        }
                        currentSegment = mutableListOf(pt)
                        wasAbove = isAbove
                    }

                    // --- HOURLY MARKS ---
                    // Since points are every 1 minute, an exact hour is every 60 indices
                    if (i % 60 == 0 && isAbove) {
                        val hourInt = (i / 60)
                        val iconData = sunHourlyIcons[hourInt]
                        if (iconData != null) {
                            val markState = rememberUpdatedMarkerState(position = pt)
                            markState.position = pt
                            Marker(state = markState, icon = iconData.first, anchor = iconData.second)
                        }
                    }
                }
                // Catch any streak that was still going at midnight
                if (currentSegment.isNotEmpty()) {
                    if (wasAbove) {
                        daySegments.add(currentSegment)
                    } else {
                        nightSegments.add(currentSegment)
                    }
                }
                daySegments.forEach { pathPoints ->
                    Polyline(
                        points = pathPoints,
                        color = colors.sunPath.copy(alpha = 0.4f), // Slightly transparent to look like a track
                        width = 6f
                    )
                }
                nightSegments.forEach { pathPoints ->
                    Polyline(
                        points = pathPoints,
                        color = Color.Gray.copy(alpha = 0.4f),
                        width = 6f,
                        pattern = listOf(Dot(), Gap(6f))
                    )
                }

                // --- 2. THE FLAWLESS GEOMETRIC PERIMETER ---
                // Completely bypasses the time-array to draw a pure mathematical arc!
                val maxElevation = elevations.maxOrNull() ?: -90f
                val minElevation = elevations.minOrNull() ?: 90f

                val perimeterPoints = mutableListOf<LatLng>()

                if (minElevation >= SolarEphemeris.ALT_SUNRISE_SUNSET.toFloat()) {
                    // POLAR DAY: Draw a full 360-degree circle
                    for (i in 0..360 step 2) {
                        perimeterPoints.add(SphericalUtil.computeOffset(drawCenter, radiusMeters, i.toDouble()))
                    }
                } else if (maxElevation < SolarEphemeris.ALT_SUNRISE_SUNSET.toFloat()) {
                    // POLAR NIGHT: Do nothing (the list remains empty)
                } else {
                    // STANDARD DAY: Draw an arc from Sunrise to Sunset
                    val startAz = solarEvents.sunriseAzimuth
                    val endAz = solarEvents.sunsetAzimuth
                    val noonAz = solarEvents.solarNoonAzimuth

                    if (startAz != null && endAz != null) {
                        // Math helper: Normalizes angles to a clean 0..360 range
                        fun normAngle(a: Double) = (a % 360.0 + 360.0) % 360.0

                        val start = normAngle(startAz)
                        val end = normAngle(endAz)
                        val noon = normAngle(noonAz)

                        // --- THE DECLINATION FIX ---
                        // If Solar Noon is in the southern hemisphere of the sky (90° to 270°),
                        // the sun travels Clockwise (East -> South -> West).
                        // If it's in the northern sky (270° to 90°), it travels Counter-Clockwise (East -> North -> West).
                        val isClockwise = noon in 90.0..270.0

                        var currentAz = start
                        val step = if (isClockwise) 1.0 else -1.0

                        // Directional distance helpers
                        fun cwDist(a: Double, b: Double) = normAngle(b - a)
                        fun ccwDist(a: Double, b: Double) = normAngle(a - b)

                        // Step along the horizon every 2 degrees until we hit sunset
                        var safetyCounter = 0
                        while (safetyCounter < 360) {
                            perimeterPoints.add(SphericalUtil.computeOffset(drawCenter, radiusMeters, currentAz))

                            val remainingDist = if (isClockwise) cwDist(end, currentAz) else ccwDist(currentAz, end)
                            if (remainingDist <= abs(step)) break // We reached sunset!

                            currentAz = normAngle(currentAz + step)
                            safetyCounter++
                        }
                        // Snap the final point exactly to the sunset coordinate
                        perimeterPoints.add(SphericalUtil.computeOffset(drawCenter, radiusMeters, end))
                    }
                }

                if (perimeterPoints.isNotEmpty()) {
                    Polyline(
                        points = perimeterPoints,
                        color = colors.sun,
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
                var wasAbove = elevations[0] >= LunarEphemeris.ALT_MOONRISE_MOONSET.toFloat()

                for (i in elevations.indices) {
                    val el = elevations[i]
                    val isAbove = el >= LunarEphemeris.ALT_MOONRISE_MOONSET.toFloat()
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

                    // --- HOURLY MARKS ---
                    // Since points are every 1 minute, an exact hour is every 60 indices
                    if (i % 60 == 0 && isAbove) {
                        val hourInt = (i / 60)
                        val iconData = moonHourlyIcons[hourInt]
                        if (iconData != null) {
                            val markState = rememberUpdatedMarkerState(position = pt)
                            markState.position = pt
                            Marker(state = markState, icon = iconData.first, anchor = iconData.second)
                        }
                    }
                }
                // Catch any streak that was still going on at midnight
                if (currentSegment.isNotEmpty()) {
                    if (wasAbove) daySegments.add(currentSegment) else nightSegments.add(currentSegment)
                }
                daySegments.forEach { pathPoints ->
                    Polyline(
                        points = pathPoints,
                        color = colors.moonPath.copy(alpha = 0.4f), // Slightly transparent to look like a track
                        width = 6f
                    )
                }
                nightSegments.forEach { pathPoints ->
                    Polyline(
                        points = pathPoints,
                        color = Color.Gray.copy(alpha = 0.4f),
                        width = 6f,
                        pattern = listOf(Dot(), Gap(6f))
                    )
                }
            }
        }

        // 4a. Current Sun Line (Solid)
        if (currentSunElevation >= SolarEphemeris.ALT_SUNRISE_SUNSET) {
            Polyline(
                points = listOf(drawCenter, currentSunEdgePoint),
                color = colors.sunPath,
                width = 8f
            )
        }

        // 4b. Current Moon Line (Solid)
        if (currentMoonElevation >= LunarEphemeris.ALT_MOONRISE_MOONSET) {
            Polyline(
                points = listOf(drawCenter, currentMoonEdgePoint),
                color = colors.moonPath,
                width = 8f
            )
        }

        // 5. Compass Direction Text Band (Full 16-Point Compass)
        val directions = listOf(
            0.0 to "N", 22.5 to "NNE", 45.0 to "NE", 67.5 to "ENE",
            90.0 to "E", 112.5 to "ESE", 135.0 to "SE", 157.5 to "SSE",
            180.0 to "S", 202.5 to "SSW", 225.0 to "SW", 247.5 to "WSW",
            270.0 to "W", 292.5 to "WNW", 315.0 to "NW", 337.5 to "NNW"
        )
        val mapBearing = cameraPositionState.position.bearing
        directions.forEach { (azimuth, label) ->
            val point = SphericalUtil.computeOffset(drawCenter, textRadiusMeters, azimuth)

            // --- DYNAMIC TANGENT MATH ---
            // 1. Find the screen-relative angle of this point
            var screenAngle = (azimuth - mapBearing).toFloat()
            screenAngle = (screenAngle % 360f + 360f) % 360f

            // 2. Adjust rotation so the text stays right-side up
            val textRotation = when {
                screenAngle > 90f && screenAngle < 270f -> screenAngle - 180f
                screenAngle >= 270f -> screenAngle - 360f
                else -> screenAngle
            }

            // --- DIMMING SECONDARY DIRECTIONS ---
            // 3-letter strings (e.g. "WSW") get a 50% opacity fade
            val textColor = when (label.length) {
                1 -> MaterialColors.Red400.copy(alpha = 0.95f)
                2 -> MaterialColors.LightBlueA100.copy(alpha = 0.95f)
                else -> MaterialColors.Gray50.copy(alpha = 0.85f)
            }

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
                anchor = Offset(0.5f, 0.5f),
                rotation = textRotation
            )
        }

        // 6. The User Center Dot
        val centerColor = if (effectiveIsDarkTheme) Color.White else Color.DarkGray
        val centerDotIcon = remember(centerColor) { bitmapDescriptorForCenterDot(centerColor) }
        val centerState = rememberUpdatedMarkerState(position = drawCenter)
        centerState.position = drawCenter
        Marker(state = centerState, icon = centerDotIcon, anchor = Offset(0.5f, 0.5f))

        // --- NEW: ALL PERIMETER DOTS ---
        if (sunrisePoint != null) {
            val state = rememberUpdatedMarkerState(position = sunrisePoint)
            state.position = sunrisePoint
            Marker(state = state, icon = sunriseDotIcon, anchor = Offset(0.5f, 0.5f))
        }
        if (sunsetPoint != null) {
            val state = rememberUpdatedMarkerState(position = sunsetPoint)
            state.position = sunsetPoint
            Marker(state = state, icon = sunsetDotIcon, anchor = Offset(0.5f, 0.5f))
        }
        if (moonrisePoint != null) {
            val state = rememberUpdatedMarkerState(position = moonrisePoint)
            state.position = moonrisePoint
            Marker(state = state, icon = moonriseDotIcon, anchor = Offset(0.5f, 0.5f))
        }
        if (moonsetPoint != null) {
            val state = rememberUpdatedMarkerState(position = moonsetPoint)
            state.position = moonsetPoint
            Marker(state = state, icon = moonsetDotIcon, anchor = Offset(0.5f, 0.5f))
        }

        val sunEdgeState = rememberUpdatedMarkerState(position = currentSunEdgePoint)
        sunEdgeState.position = currentSunEdgePoint
        Marker(state = sunEdgeState, icon = sunEdgeDotIcon, anchor = Offset(0.5f, 0.5f), zIndex = 1f)

        val moonEdgeState = rememberUpdatedMarkerState(position = currentMoonEdgePoint)
        moonEdgeState.position = currentMoonEdgePoint
        Marker(state = moonEdgeState, icon = moonEdgeDotIcon, anchor = Offset(0.5f, 0.5f), zIndex = 1f)

        // 7a. The Sun Icon Marker
        val isSunAbove = currentSunElevation >= SolarEphemeris.ALT_SUNRISE_SUNSET
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
            snippet = "Azimuth: ${currentSolarPosition.azimuth.toInt()}°",
            zIndex = 2f
        )

        // 7b. The Moon Icon Marker
        val isMoonAbove = currentMoonElevation >= LunarEphemeris.ALT_MOONRISE_MOONSET
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
            snippet = "Azimuth: ${currentLunarPosition.azimuth.toInt()}°",
            zIndex = 2f
        )
    }
}

fun bitmapDescriptorFromVector(
    context: Context,
    vectorResId: Int,
    tintColor: Color
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

    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

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
                val overlayColor = "#E65100".toColorInt()
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
            canvas.withRotation(-35f, sizePx / 2f, sizePx / 2f) {
                // Match the -35f rotation from ChartIconDrawer
                moon.setBounds(0, 0, sizePx, sizePx)
                moon.draw(this)
            }
        }
    }

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun bitmapDescriptorFromText(
    text: String,
    textColor: Color
): BitmapDescriptor {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            (textColor.alpha * 255).toInt(),
            (textColor.red * 255).toInt(),
            (textColor.green * 255).toInt(),
            (textColor.blue * 255).toInt()
        )
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    val padding = 10f
    val baseline = -paint.ascent() + padding
    val width = (paint.measureText(text) + padding * 2).toInt()
    val height = (baseline + paint.descent() + padding).toInt()

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    // Ensure the background is totally transparent
    canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    canvas.drawText(text, width / 2f, baseline, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun bitmapDescriptorForHourlyMark(
    text: String,
    textColor: Color
): Pair<BitmapDescriptor, Offset> {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(
            (textColor.alpha * 255).toInt(), (textColor.red * 255).toInt(),
            (textColor.green * 255).toInt(), (textColor.blue * 255).toInt()
        )
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    val dotRadius = 6f
    val gap = 4f
    val textHeight = -paint.ascent() + paint.descent()
    val textWidth = paint.measureText(text)

    // Calculate canvas size to comfortably fit text and the dot beneath it
    val width = textWidth.coerceAtLeast(dotRadius * 2).toInt() + 10
    val height = (textHeight + gap + dotRadius * 2).toInt() + 10

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    val centerX = width / 2f
    val textY = -paint.ascent() + 2f // Top padding
    val dotY = textY + paint.descent() + gap + dotRadius

    canvas.drawText(text, centerX, textY, paint)
    canvas.drawCircle(centerX, dotY, dotRadius, paint)

    // Dynamically calculate the anchor so the DOT sits perfectly on the map coordinate
    val anchorX = 0.5f
    val anchorY = dotY / height.toFloat()

    return Pair(BitmapDescriptorFactory.fromBitmap(bitmap), Offset(anchorX, anchorY))
}

fun bitmapDescriptorForCenterDot(color: Color): BitmapDescriptor {
    val radius = 12f // 12px gives a clean, small but visible dot
    val bitmap = createBitmap((radius * 2).toInt(), (radius * 2).toInt())
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(), (color.red * 255).toInt(),
            (color.green * 255).toInt(), (color.blue * 255).toInt()
        )
    }
    canvas.drawCircle(radius, radius, radius, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}