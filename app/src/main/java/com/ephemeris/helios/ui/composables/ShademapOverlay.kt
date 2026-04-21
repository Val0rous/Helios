package com.ephemeris.helios.ui.composables

import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import com.ephemeris.helios.R
import com.ephemeris.helios.utils.calc.SolarEphemeris
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

@Composable
fun ShademapOverlay(
    currentSolarPosition: SolarEphemeris.SolarPosition,
    elevationTile: ImageBitmap?, // The Mapbox RGB tile we will fetch later
    modifier: Modifier = Modifier
) {

    // Version Guard: Only proceed if the device is running Android 13 (API 33) or higher
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current

    // 1. READ THE SHADER FROM THE RAW FILE (DRY & Memory Safe)
    val shaderCode = remember {
        with (context) {
            resources
                .openRawResource(R.raw.shadow_shader)
                .bufferedReader()
                .use { it.readText() }
        }
    }

    // 2. COMPILE THE SHADER ON THE GPU
    val runtimeShader = remember(shaderCode) { RuntimeShader(shaderCode) }

    // 3. DRAW THE FULL-SCREEN OVERLAY
    Canvas(modifier = modifier.fillMaxSize()) {
        // If we haven't fetched the elevation tile yet, don't draw anything
        if (elevationTile == null) return@Canvas

        val androidBitmap = elevationTile.asAndroidBitmap()
        val bitmapShader = BitmapShader(
            androidBitmap,
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )

        // Calculate the Sun's 2D screen direction based on its azimuth
        val sunAzimuthRads = Math.toRadians(currentSolarPosition.azimuth)
        val sunDirX = sin(sunAzimuthRads).toFloat()
        val sunDirY = -cos(sunAzimuthRads).toFloat() // Y is inverted on Android canvases

        // Calculate how fast the ray rises into the sky per step
        // A lower altitude means it rises slower, casting longer shadows!
        val altitudeRisePerStep = tan(Math.toRadians(currentSolarPosition.altitude)).toFloat() * 10f

        // Feed our Kotlin variables into the AGSL GPU Shader
        runtimeShader.setInputBuffer("elevationMap", bitmapShader)
        runtimeShader.setFloatUniform("sunDir", sunDirX, sunDirY)
        runtimeShader.setFloatUniform("sunAltitude", altitudeRisePerStep)
        runtimeShader.setFloatUniform("stepSize", 5f) // Check for mountains every 5 pixels

        // Paint the shadow mask directly over the screen!
        drawRect(brush = ShaderBrush(runtimeShader))
    }
}