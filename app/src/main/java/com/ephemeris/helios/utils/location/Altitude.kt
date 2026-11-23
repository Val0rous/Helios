package com.ephemeris.helios.utils.location

import Geoid
import android.content.Context
import android.location.Location
import android.os.Build
import java.io.File

class AltitudeCorrector(context: Context) {

    // Hold only the Application Context so this singleton can never leak an Activity
    private val appContext: Context = context.applicationContext

    // Lazily copied and loaded on first fallback use (completely skipped on Android 14+ when MSL is available)
    private val geoid: Geoid? by lazy { loadGeoid() }

    private fun loadGeoid(): Geoid? = try {
        // 1. Define where the file will live on the phone's internal storage
        val geoidDir = File(appContext.filesDir, "geoids")
        if (!geoidDir.exists()) geoidDir.mkdirs()

        val geoidFile = File(geoidDir, "egm96-5.pgm")

        // 2. If the file isn't there yet, copy it atomically from the bundled APK assets
        if (!geoidFile.exists()) {
            val tempFile = File(geoidDir, "egm96-5.pgm.tmp")
            appContext.assets.open("geoids/egm96-5.pgm").use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (!tempFile.renameTo(geoidFile)) {
                tempFile.copyTo(geoidFile, overwrite = true)
                tempFile.delete()
            }
        }

        // 3. Initialize the GeographicLib Geoid engine with the copied file
        Geoid("egm96-5", geoidDir.absolutePath, true, false)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    fun getTrueSeaLevelAltitude(lat: Double, lon: Double, gpsAltitude: Double): Double {
        // 4. Calculate the Geoid Undulation (the difference between MSL and the Ellipsoid)
        val undulation = geoid?.computeGeoidHeight(lat, lon) ?: 0.0

        // 5. Apply the mathematical correction: Orthometric = Ellipsoid - Undulation
        return gpsAltitude - undulation
    }

    /**
     * Returns the orthometric (mean sea level) altitude in meters if the [location] carries either
     * a native Android 14+ MSL altitude or a valid ellipsoid altitude (`hasAltitude()`), or `null`
     * for fixes (such as indoor network-assisted fixes) that lack an altitude measurement.
     */
    fun getRealAltitudeOrNull(location: Location): Double? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            location.hasMslAltitude()
        ) {
            // 1. Android 14+ natively calculates the Geoid correction for you
            return location.mslAltitudeMeters
        }
        if (location.hasAltitude()) {
            // 2. Fallback for older Android versions (or fixes missing native MSL altitude)
            return getTrueSeaLevelAltitude(location.latitude, location.longitude, location.altitude)
        }
        return null
    }

    fun getRealAltitude(location: Location): Double = getRealAltitudeOrNull(location) ?: 0.0
}
