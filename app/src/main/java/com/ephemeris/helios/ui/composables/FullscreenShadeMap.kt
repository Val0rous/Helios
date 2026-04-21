package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.ephemeris.helios.BuildConfig
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.location.Coordinates
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.mapbox.bindgen.Value
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.literal
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.hillshadeLayer
import com.mapbox.maps.extension.style.light.generated.ambientLight
import com.mapbox.maps.extension.style.light.generated.directionalLight
import com.mapbox.maps.extension.style.light.setLight
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.terrain.generated.setTerrain
import com.mapbox.maps.extension.style.terrain.generated.terrain
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@OptIn(MapboxExperimental::class)
@Suppress("MapboxMapComposable")
@Composable
fun FullscreenShadeMap(
    location: Coordinates,
    currentSolarPosition: SolarEphemeris.SolarPosition,
    onMapCenterSettled: (Coordinates) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()

    // READ DIRECTLY FROM BUILD CONFIG
    // The Secrets Plugin pulls this from secrets.properties during compilation!
    MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN

    // Initial Camera Setup
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(location.longitude, location.latitude))
            zoom(16.0) // 16 is perfect for seeing physical building footprints
            pitch(60.0) // Tilt down to 60 degrees for the 3D effect
            bearing(0.0)
        }
    }

    // --- TWO-WAY BINDING: MAP TO VIEWMODEL ---
    // Because this watches the center state, delay(500) will constantly reset
    // while the user drags. It only fires when they truly stop moving!
    LaunchedEffect(mapViewportState.cameraState?.center) {
        delay(500)
        val center = mapViewportState.cameraState?.center ?: return@LaunchedEffect
        val newTarget = LatLng(center.latitude(), center.longitude())
        val oldTarget = LatLng(location.latitude, location.longitude)

        if (SphericalUtil.computeDistanceBetween(oldTarget, newTarget) > 10.0) {
            onMapCenterSettled(
                location.copy(
                    latitude = newTarget.latitude,
                    longitude = newTarget.longitude,
                    locationName = null, // Forces Geocoding lookup
                    timezoneId = null    // Forces Timezone lookup
                )
            )
        }
    }

    // --- TWO-WAY BINDING: VIEWMODEL TO MAP ---
    // Instantly snap the camera if the user hits the TopBar GPS button
    LaunchedEffect(location.latitude, location.longitude) {
        val center = mapViewportState.cameraState?.center
        val currentTarget = if (center != null) LatLng(center.latitude(), center.longitude()) else null
        val newTarget = LatLng(location.latitude, location.longitude)

        if (currentTarget == null || SphericalUtil.computeDistanceBetween(currentTarget, newTarget) > 10.0) {
            mapViewportState.setCameraOptions {
                center(Point.fromLngLat(location.longitude, location.latitude))
            }
        }
    }


    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = modifier.fillMaxSize(),
            mapViewportState = mapViewportState
        ) {
            // 1. Load the Map Style and Build the 3D Extrusions
            MapEffect(isDarkTheme) { mapView ->
                // Upgraded to OUTDOORS for rich terrain, parks, and water colors!
                val styleUri = if (isDarkTheme) Style.DARK else Style.OUTDOORS

                mapView.mapboxMap.loadStyle(styleUri) { style ->
                    // --- ENABLE 3D MOUNTAINS ---
                    // 1. Download the Digital Elevation Model (DEM)
                    style.addSource(
                        rasterDemSource("mapbox-dem") {
                            url("mapbox://mapbox.mapbox-terrain-dem-v1")
                            tileSize(512)
                            maxzoom(14)
                        }
                    )

                    // 2. Apply the elevation data to the map's ground plane
                    style.setTerrain(
                        terrain("mapbox-dem") {
                            // Optional: You can increase this to e.g., 1.5 to make mountains look taller!
                            exaggeration(1.2) // Slight boost to make mountains look more realistic
                        }
                    )

                    // --- ADD MOUNTAIN HILLSHADING ---
                    if (style.styleLayerExists("mountain-shading")) {
                        style.removeStyleLayer("mountain-shading")
                    }

                    val hillshade = hillshadeLayer("mountain-shading", "mapbox-dem") {
                        hillshadeExaggeration(0.8) // High contrast for beautiful sunrise/sunset
                        if (isDarkTheme) {
                            hillshadeShadowColor("#0D0D0D")
                            hillshadeHighlightColor("#333333")
                        } else {
                            hillshadeShadowColor("#333333")
                            hillshadeHighlightColor("#FFFFFF")
                        }
                    }
                    // Add the shading immediately so it sits under the buildings
                    style.addLayer(hillshade)

                    // Safety check: Don't stack layers if MapEffect recomposes
                    if (style.styleLayerExists("3d-buildings")) {
                        style.removeStyleLayer("3d-buildings")
                    }

                    // Extrude the buildings natively on the GPU
                    val buildingLayer = fillExtrusionLayer("3d-buildings", "composite") {
                        sourceLayer("building")
                        // Only extrude features marked as buildings
                        filter(eq(get("extrude"), literal("true")))
                        // Set heights
                        fillExtrusionHeight(get("height"))
                        fillExtrusionBase(get("min_height"))
                        // Shade the buildings nicely based on theme
                        fillExtrusionColor(if (isDarkTheme) "#2A2A2A" else "#E8E8E8")
                        fillExtrusionOpacity(0.9)
                    }

                    style.addLayer(buildingLayer)
                }
            }

            // 2. The Real-Time Ephemeris Engine Link!
            // This watches your Time Machine slider. Every time currentSolarPosition changes,
            // it instantly recalculates the shadows without reloading the map.
            MapEffect(currentSolarPosition) { mapView ->
                mapView.mapboxMap.getStyle { style ->
                    val altitude = currentSolarPosition.altitude

                    // --- THE SHADOW CLIPPING CEILING ---
                    // 84.5 degrees is the absolute bleeding edge of the CSM bounding box.
                    // Tangent(84.5) = 10.4x shadow multiplier. Any higher and it shatters.
                    val polarAngle = (90.0 - altitude).coerceIn(0.0, 84.5)

                    // --- LAMBERT'S COSINE LAW & LOGARITHMIC PERCEPTION FIX ---
                    // We map altitude to a 0.0-1.0 ratio, then apply a fractional power curve (0.35).
                    // Example: At just 5 degrees (0.055 ratio), 0.055^0.35 = 0.36!
                    // This keeps the mathematical light energy drastically higher than linear fading.

                    // 1. Direct Sun Intensity:
                    // Uncapped at noon (1.5 max brightness!), and stays at a powerful 0.65
                    // all the way down at the absolute horizon line before snapping to 0.
                    val directIntensity = if (altitude > 0.0) {
                        val normalizedAlt = (altitude / 90.0).coerceIn(0.0, 1.0)

                        // At noon (1.0), boost is 0. Base is perfectly exposed at 0.65.
                        // At sunrise (0.0), boost approaches 1.85. Total is 2.5 to cut through glancing angles!
                        val boost = 1.85 * (1.0 - sqrt(normalizedAlt))
                        (0.65 + boost)
                    } else 0.0

                    // 2. Ambient Sky Intensity (The Ground):
                    // Ambient stays completely flat and comfortable during the day to keep shadows visible
                    val ambientIntensity = when {
                        altitude >= 0.0 -> 0.35
                        else -> 0.02 + ((altitude + 18.0) / 18.0 * 0.33).coerceIn(0.0, 0.33)
                    }

                    style.setLight(
                        ambientLight {
                            intensity(ambientIntensity)
                        },
                        directionalLight {
                            // Direction takes an array of exactly two values: [Azimuth, Polar Angle]
                            direction(listOf(currentSolarPosition.azimuth, polarAngle))
                            intensity(directIntensity)
                            castShadows(true) // Turn on the GPU ray-tracing!
                        }
                    )

                    // --- ELEGANT SUBTLE TINTING (LERP) ---
                    if (style.styleLayerExists("3d-buildings")) {
                        val buildingColor = getSubtleBuildingTint(altitude, isDarkTheme)
                        val hexColor = String.format("#%06X", 0xFFFFFF and buildingColor.toArgb())

                        style.setStyleLayerProperty(
                            "3d-buildings",
                            "fill-extrusion-color",
                            Value.valueOf(hexColor)
                        )
                    }

                    // --- DYNAMIC MOUNTAIN SHADOWS ---
                    if (style.styleLayerExists("mountain-shading")) {
                        // Lock the illumination angle to the sun's azimuth so shadows rotate with the Time Machine!
                        style.setStyleLayerProperty(
                            "mountain-shading",
                            "hillshade-illumination-direction",
                            Value.valueOf(currentSolarPosition.azimuth)
                        )

                        // Optional polish: Fade out the intense mountain shading after sunset so it doesn't look weirdly lit
                        val shadingIntensity = if (altitude > -2.0) 0.8 else 0.1
                        style.setStyleLayerProperty(
                            "mountain-shading",
                            "hillshade-exaggeration",
                            Value.valueOf(shadingIntensity)
                        )
                    }
                }
            }
        }
    }
}

// --- HELPER FUNCTION ---
// Beautifully mixes 15-25% of vibrant sunset colors into the base building gray,
// then smoothly interpolates between phases so the colors never snap.
fun getSubtleBuildingTint(altitude: Double, isDarkTheme: Boolean): Color {
    val base = if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE8E8E8)

    // Mix subtle hints of color into the base
    val warmBase = lerp(base, Color(0xFFFFE0B2), if (isDarkTheme) 0.10f else 0.15f)
    val goldenBase = lerp(base, Color(0xFFFFB300), if (isDarkTheme) 0.15f else 0.25f)
    val reddishBase = lerp(base, Color(0xFFFF5252), if (isDarkTheme) 0.15f else 0.20f)
    val twilightBase = Color(0xFF283559) // Dark navy/blue
    val nightBase = Color(0xFF12121A) // Almost pitch black

    return when {
        altitude >= 15.0 -> base
        altitude in 10.0..15.0 -> lerp(warmBase, base, ((altitude - 10.0) / 5.0).toFloat())
        altitude in 5.0..10.0 -> lerp(goldenBase, warmBase, ((altitude - 5.0) / 5.0).toFloat())
        altitude in 0.0..5.0 -> lerp(reddishBase, goldenBase, (altitude / 5.0).toFloat())
        altitude in -6.0..0.0 -> lerp(twilightBase, reddishBase, ((altitude + 6.0) / 6.0).toFloat())
        altitude in -12.0..-6.0 -> lerp(nightBase, twilightBase, ((altitude + 12.0) / 6.0).toFloat())
        else -> nightBase
    }
}