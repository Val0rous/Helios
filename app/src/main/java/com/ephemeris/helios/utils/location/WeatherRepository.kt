//package com.ephemeris.helios.utils.location
//
//import android.location.Location
//import java.time.Instant
//
//class WeatherRepository {
//
//    // Cache variables stored in memory
//    private var lastFetchTime: Long = 0
//    private var lastFetchLocation: Location? = null
//    private var cachedWeatherData: WeatherResponse? = null
//
//    // Thresholds
//    private val TIME_THRESHOLD_MS = 45 * 60 * 1000L // 45 minutes
//    private val DISTANCE_THRESHOLD_METERS = 5000f // 5 kilometers
//
//    suspend fun getUVAndWeather(currentLocation: Location): WeatherResponse {
//        val currentTime = Instant.now().toEpochMilli()
//
//        // 1. Check if we have cached data to compare against
//        if (lastFetchLocation != null && cachedWeatherData != null) {
//
//            val timeElapsed = currentTime - lastFetchTime
//            val distanceMoved = currentLocation.distanceTo(lastFetchLocation!!)
//
//            // 2. The Golden Rule: Only fetch if time OR distance thresholds are broken
//            if (timeElapsed < TIME_THRESHOLD_MS && distanceMoved < DISTANCE_THRESHOLD_METERS) {
//                println("Serving from Cache! Saved an API call.")
//                return cachedWeatherData!!
//            }
//        }
//
//        // 3. If we reach here, the cache is invalid. Make the real network call.
//        println("Making actual Open-Meteo API Network Call...")
//        val newData = makeNetworkCallToOpenMeteo(currentLocation)
//
//        // 4. Update the cache with the fresh data and current coordinates/time
//        lastFetchTime = currentTime
//        lastFetchLocation = currentLocation
//        cachedWeatherData = newData
//
//        return newData
//    }
//
//    private suspend fun makeNetworkCallToOpenMeteo(loc: Location): WeatherResponse {
//        // Your Retrofit or HTTP Client code goes here
//        return WeatherResponse()
//    }
//}