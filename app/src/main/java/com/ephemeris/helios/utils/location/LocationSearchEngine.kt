package com.ephemeris.helios.utils.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import java.util.Locale

class LocationSearchEngine(private val context: Context) {

    fun getCoordinatesFromSearch(searchQuery: String, onCoordinatesFound: (Double, Double) -> Unit) {
        val geocoder = Geocoder(context, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ Asynchronous Listener
            geocoder.getFromLocationName(
                searchQuery,
                1 // Limit to the top 1 result
            ) { addresses ->
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    onCoordinatesFound(address.latitude, address.longitude)
                }
            }
        } else {
            // Older Android versions (Remember to run this on a background thread!)
            try {
                val addresses = geocoder.getFromLocationName(searchQuery, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    onCoordinatesFound(address.latitude, address.longitude)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}