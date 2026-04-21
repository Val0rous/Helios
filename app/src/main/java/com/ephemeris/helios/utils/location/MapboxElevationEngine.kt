package com.ephemeris.helios.utils.location

import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.tan

object MapboxElevationEngine {

    suspend fun getElevation(lat: Double, lon: Double, accessToken: String): Double? = withContext(Dispatchers.IO) {
        try {
            // 1. Math to find the specific tile at Zoom 14 (Mapbox's highest terrain resolution)
            val zoom = 14.0
            val n = Math.pow(2.0, zoom)

            val xExact = ((lon + 180.0) / 360.0) * n
            val tileX = floor(xExact).toInt()

            val latRad = lat * PI / 180.0
            val yExact = (1.0 - asinh(tan(latRad)) / PI) / 2.0 * n
            val tileY = floor(yExact).toInt()

            // 2. Math to find the exact X/Y pixel inside that 256x256 image!
            val pixelX = floor((xExact - tileX) * 256.0).toInt().coerceIn(0, 255)
            val pixelY = floor((yExact - tileY) * 256.0).toInt().coerceIn(0, 255)

            // 3. Download the specific tile from Mapbox's terrain database
            val urlString = "https://api.mapbox.com/v4/mapbox.terrain-rgb/14/$tileX/$tileY.pngraw?access_token=$accessToken"
            val connection = URL(urlString).openConnection()
            connection.connectTimeout = 3000 // Don't hang the app if offline
            connection.readTimeout = 3000

            // 4. Decode the image and pluck the color of our specific pixel
            connection.inputStream.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: return@withContext null
                val pixelColor = bitmap.getPixel(pixelX, pixelY)

                val r = Color.red(pixelColor)
                val g = Color.green(pixelColor)
                val b = Color.blue(pixelColor)

                // 5. Mapbox's official RGB-to-Elevation formula
                val altitude = -10000.0 + ((r * 256.0 * 256.0 + g * 256.0 + b) * 0.1)

                // Free up memory instantly
                bitmap.recycle()

                return@withContext altitude
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}