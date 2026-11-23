package com.ephemeris.helios.utils.location

import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.tan

object MapboxElevationEngine {

    suspend fun getElevation(lat: Double, lon: Double, accessToken: String): Double? = withContext(Dispatchers.IO) {
        fetchFromMapbox(lat, lon, accessToken) ?: fetchFromOpenMeteo(lat, lon)
    }

    private fun fetchFromMapbox(lat: Double, lon: Double, accessToken: String): Double? {
        // Skip Mapbox Terrain-RGB if no valid public token is configured
        if (!accessToken.startsWith("pk.") || accessToken.length < 20) return null

        var connection: HttpURLConnection? = null
        return try {
            // 1. Math to find the specific tile at Zoom 14 (Mapbox's highest terrain resolution)
            val zoom = 14.0
            val n = 2.0.pow(zoom)

            val clampedLat = lat.coerceIn(-85.0511, 85.0511)
            val xExact = ((lon + 180.0) / 360.0) * n
            val tileX = floor(xExact).toInt().coerceIn(0, n.toInt() - 1)

            val latRad = clampedLat * PI / 180.0
            val yExact = (1.0 - asinh(tan(latRad)) / PI) / 2.0 * n
            val tileY = floor(yExact).toInt().coerceIn(0, n.toInt() - 1)

            // 2. Math to find the exact X/Y pixel inside that 256x256 image
            val pixelX = floor((xExact - tileX) * 256.0).toInt().coerceIn(0, 255)
            val pixelY = floor((yExact - tileY) * 256.0).toInt().coerceIn(0, 255)

            // 3. Download the specific tile from Mapbox's terrain database
            val urlString = "https://api.mapbox.com/v4/mapbox.terrain-rgb/14/$tileX/$tileY.pngraw?access_token=$accessToken"
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            // 4. Decode the image and pluck the color of our specific pixel
            connection.inputStream.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: return null
                try {
                    val pixelColor = bitmap.getPixel(pixelX, pixelY)

                    val r = Color.red(pixelColor)
                    val g = Color.green(pixelColor)
                    val b = Color.blue(pixelColor)

                    // 5. Mapbox's official RGB-to-Elevation formula
                    -10000.0 + ((r * 256.0 * 256.0 + g * 256.0 + b) * 0.1)
                } finally {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Free, zero-key Copernicus 90m DEM elevation fallback via Open-Meteo.
     */
    private fun fetchFromOpenMeteo(lat: Double, lon: Double): Double? {
        var connection: HttpURLConnection? = null
        return try {
            val urlString = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/elevation?latitude=%.6f&longitude=%.6f",
                lat,
                lon,
            )
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val responseString = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseString)
            val elevationArray = json.optJSONArray("elevation") ?: return null
            if (elevationArray.length() == 0 || elevationArray.isNull(0)) return null
            val elevation = elevationArray.optDouble(0, Double.NaN)
            elevation.takeIf { it.isFinite() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }
}
