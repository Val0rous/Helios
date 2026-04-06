//package com.ephemeris.helios.utils.location
//
//import android.content.Context
//import android.location.Geocoder
//import android.os.Build
//import android.location.Location
//import java.util.Locale
//
//class AddressRepository(private val context: Context) {
//
//    private var lastFetchLocation: Location? = null
//    private var cachedAddress: String = "Locating..."
//
//    // Tighter threshold for street changes (e.g., 75 meters)
//    private val DISTANCE_THRESHOLD_METERS = 75f
//
//    fun getStreetAddress(currentLocation: Location, onAddressFound: (String) -> Unit) {
//
//        // 1. Check our tight 75m cache
//        if (lastFetchLocation != null) {
//            val distanceMoved = currentLocation.distanceTo(lastFetchLocation!!)
//            if (distanceMoved < DISTANCE_THRESHOLD_METERS) {
//                onAddressFound(cachedAddress)
//                return
//            }
//        }
//
//        // 2. Cache busted. Time to fetch a new street name.
//        val geocoder = Geocoder(context, Locale.getDefault())
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            // 3. Android 13+ (API 33) uses the new asynchronous listener
//            geocoder.getFromLocation(
//                currentLocation.latitude,
//                currentLocation.longitude,
//                1 // We only want the top result
//            ) { addresses ->
//                if (addresses.isNotEmpty()) {
//                    val address = addresses[0]
//                    // You can extract thoroughfare (street), subLocality (district), locality (city)
//                    val formattedName = "${address.thoroughfare ?: ""}, ${address.locality ?: ""}".trim(', ', ' ')
//
//                    updateCache(currentLocation, formattedName)
//                    onAddressFound(formattedName)
//                }
//            }
//        } else {
//            // 4. Older Android versions require standard synchronous execution
//            // Note: In a real app, wrap this in a Coroutine (Dispatchers.IO)
//            // because this older method can block the main thread!
//            try {
//                val addresses = geocoder.getFromLocation(currentLocation.latitude, currentLocation.longitude, 1)
//                if (!addresses.isNullOrEmpty()) {
//                    val address = addresses[0]
//                    val formattedName = "${address.thoroughfare ?: ""}, ${address.locality ?: ""}".trim(', ', ' ')
//
//                    updateCache(currentLocation, formattedName)
//                    onAddressFound(formattedName)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    private fun updateCache(loc: Location, name: String) {
//        lastFetchLocation = loc
//        cachedAddress = name
//    }
//}