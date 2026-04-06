package com.ephemeris.helios.utils.location

import Geoid
import android.content.Context
import android.location.Location
import java.io.File

object AltitudeCorrector {

    private var geoid: Geoid? = null

    fun initialize(context: Context) {
        // Prevent re-initialization if called multiple times
        if (geoid != null) return

        // CRITICAL: Grab the Application Context to prevent memory leaks
        val appContext = context.applicationContext

        // 1. Define where the file will live on the phone's internal storage
        val geoidDir = File(appContext.filesDir, "geoids")
        if (!geoidDir.exists()) geoidDir.mkdirs()

        val geoidFile = File(geoidDir, "egm96-5.pgm")

        // 2. If the file isn't there yet, copy it from the bundled APK assets
        if (!geoidFile.exists()) {
            context.assets.open("geoids/egm96-5.pgm").use { inputStream ->
                geoidFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        // 3. Initialize the GeographicLib Geoid engine with the copied file
        try {
            geoid = Geoid("egm96-5", context.filesDir.absolutePath, true, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTrueSeaLevelAltitude(lat: Double, lon: Double, gpsAltitude: Double): Double {
        // 4. Calculate the Geoid Undulation (the difference between MSL and the Ellipsoid)
        val undulation = geoid?.computeGeoidHeight(lat, lon) ?: 0.0

        // 5. Apply the mathematical correction: Orthometric = Ellipsoid - Undulation
        return gpsAltitude - undulation
    }

    fun getRealAltitude(location: Location): Double {
//    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
//        && location.hasMslAltitude()) {
//        // 1. Android 14+ natively calculates the Geoid correction for you
//            return location.mslAltitudeMeters
//    }
        // 2. Fallback for older Android versions
//        val ellipsoidAltitude = location.altitude
        return getTrueSeaLevelAltitude(location.latitude, location.longitude, location.altitude)
    }
}