package com.ephemeris.helios.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

enum class MonitoringStatus { Monitoring, Paused, NotMonitoring }

enum class StartMonitoringResult { Started, GPSDisabled, PermissionDenied }

class LocationService(private val context: Context) {
    private var monitoringStatus by mutableStateOf(MonitoringStatus.NotMonitoring)
    var coordinates: Coordinates? by mutableStateOf(null)
        private set

    private val fusedLocationProviderClient = LocationServices
        .getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest
        .Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
        .apply { setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL) }
        .build()

    private var isTracking = false
    private var currentIntervalMillis = 10000L
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            with(p0.locations.last()) {
                coordinates = Coordinates(
                    latitude,
                    longitude,
                    altitude,
                )
            }
        }
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    private fun checkPrerequisites(): StartMonitoringResult? {
        // Check if location is enabled
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isLocationEnabled) return StartMonitoringResult.GPSDisabled

        // Check if permission is granted
        val finePermissionGranted = ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarsePermissionGranted = ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!(finePermissionGranted || coarsePermissionGranted)) return StartMonitoringResult.PermissionDenied

        return null // All good
    }


    // One-Off Fast Request
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun requestCurrentLocation(): StartMonitoringResult {
        checkPrerequisites()?.let { return it }
//        val permissionGranted = ContextCompat.checkSelfPermission(
//            context,
//            Manifest.permission.ACCESS_COARSE_LOCATION
//        ) == PackageManager.PERMISSION_GRANTED

        // Use getCurrentLocation for a fast, one-off ping instead of setting up a tracking loop
//        fusedLocationProviderClient.requestLocationUpdates(
//            locationRequest,
//            locationCallback,
//            Looper.getMainLooper()
//        )

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .build()
        fusedLocationProviderClient.getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    coordinates = Coordinates(location.latitude, location.longitude, location.altitude.round(1))
                }
            }
//        monitoringStatus = MonitoringStatus.Monitoring
        return StartMonitoringResult.Started
    }

    // Continuous Tracking
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startLocationTracking(intervalMillis: Long = 10000L): StartMonitoringResult {
        checkPrerequisites()?.let { return it }

        currentIntervalMillis = intervalMillis
        val request = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, currentIntervalMillis)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .build()

        fusedLocationProviderClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        isTracking = true
        monitoringStatus = MonitoringStatus.Monitoring
        return StartMonitoringResult.Started
    }

    fun stopLocationTracking() {
        if (monitoringStatus == MonitoringStatus.NotMonitoring) return
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        monitoringStatus = MonitoringStatus.NotMonitoring
    }

    // --- LIFECYCLE MANAGEMENT ---
    fun pauseLocationRequest() {
        if (monitoringStatus != MonitoringStatus.Monitoring) return
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        monitoringStatus = MonitoringStatus.Paused
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun resumeLocationRequest() {
        if (monitoringStatus != MonitoringStatus.Paused) return
        startLocationTracking(currentIntervalMillis)
    }
}