package com.ephemeris.helios.utils.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NativeGeocodingEngine(context: Context) {

    // 1. Single shared engine for the whole app
    private val geocoder = Geocoder(context, Locale.getDefault())

    // Cache purely for Reverse Geocoding (GPS tracking)
    private var lastFetchLocation: Location? = null
    private var cachedAddress: String? = null
    private val DISTANCE_THRESHOLD_METERS = 75f

    // ========================================================================
    // FEATURE 1: REVERSE GEOCODING (Coordinates -> Street Address)
    // ========================================================================
    suspend fun getStreetAddress(location: Location): String? = withContext(Dispatchers.IO) {

        // Check the 75-meter cache first
        if (lastFetchLocation != null && cachedAddress != null) {
            if (location.distanceTo(lastFetchLocation!!) < DISTANCE_THRESHOLD_METERS) {
                return@withContext cachedAddress // Return cached string instantly
            }
        }

        // Cache busted. Fetch new data using our helper function.
        val addressList = fetchFromLocationAPI(location.latitude, location.longitude)

        // Extract the street and city safely
        val result = addressList?.firstOrNull()?.let { address ->
            val street = address.thoroughfare
            val neighborhood = address.subLocality
            val city = address.locality
            val state = address.adminArea
            val country = address.countryName

            // Build a smart, privacy-friendly string
            when {
                // 1. Street-level ("Hamilton St, London")
                street != null && city != null -> "$street, $city"
                // 2. Neighborhood-level ("Trastevere, Rome")
                neighborhood != null && city != null && neighborhood != city -> "$neighborhood, $city"
                // 3. City & State ("Perth, Western Australia")
                city != null && state != null -> "$city, $state"
                // 4. City & Country (fallback only if State data is missing in that region)
                city != null && country != null -> "$city, $country"
                // 5. Ultimate Fallback (Wilderness or ocean edges)
                else -> city ?: state ?: country ?: ""
            }
        }

        // Only update the cache if we got a valid, non-blank result back
        if (!result.isNullOrBlank()) {
            lastFetchLocation = location
            cachedAddress = result
        }

        return@withContext result
    }

    // ========================================================================
    // FEATURE 2: FORWARD GEOCODING (Search Bar -> Coordinates)
    // ========================================================================
    suspend fun getCoordinates(query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        // No cache needed here, just fetch directly
        val addressList = fetchFromLocationNameAPI(query)

        // Extract the latitude and longitude into a Kotlin Pair
        return@withContext addressList?.firstOrNull()?.let {
            Pair(it.latitude, it.longitude)
        }
    }

    // ========================================================================
    // OS COMPATIBILITY WRAPPERS
    // ========================================================================

    @Suppress("DEPRECATION")
    private suspend fun fetchFromLocationAPI(lat: Double, lon: Double): List<Address>? =
        suspendCancellableCoroutine { continuation ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ Explicit Listener
                    geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        // Catch the offline error and resume safely
                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    })
                } else {
                    // Android 12 and below (Must use the deprecated synchronous method)
                    if (continuation.isActive) {
                        continuation.resume(geocoder.getFromLocation(lat, lon, 1))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (continuation.isActive) continuation.resume(null) // Prevent app crash if network fails
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun fetchFromLocationNameAPI(query: String): List<Address>? =
        suspendCancellableCoroutine { continuation ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    })
                } else {
                    // Android 12 and below (Must use the deprecated synchronous method)
                    if (continuation.isActive) {
                        continuation.resume(geocoder.getFromLocationName(query, 1))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (continuation.isActive) continuation.resume(null)
            }
        }
}

//// In your ViewModel
//fun onSearchButtonClicked(cityName: String) {
//    viewModelScope.launch {
//        val coords = geocodingEngine.getCoordinates(cityName)
//        if (coords != null) {
//            val (lat, lon) = coords
//            // Now pass these to Open-Meteo!
//            fetchWeather(lat, lon)
//        }
//    }
//}