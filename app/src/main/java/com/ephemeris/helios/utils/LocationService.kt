package com.ephemeris.helios.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigationevent.NavigationEventDispatcher
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

    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply {
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
        }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            with(p0.locations.last()) {
                coordinates = Coordinates(latitude, longitude)
            }
            endLocationRequest()
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

    fun requestCurrentLocation(): StartMonitoringResult {
        // Check if location is enabled
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isLocationEnabled) return StartMonitoringResult.GPSDisabled

        // Check if permission is granted
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) return StartMonitoringResult.PermissionDenied

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        monitoringStatus = MonitoringStatus.Monitoring
        return StartMonitoringResult.Started
    }

    fun endLocationRequest() {
        if (monitoringStatus == MonitoringStatus.NotMonitoring) return
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        monitoringStatus = MonitoringStatus.NotMonitoring
    }

    fun pauseLocationRequest() {
        if (monitoringStatus != MonitoringStatus.Monitoring) return
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        monitoringStatus = MonitoringStatus.Paused
    }

    fun resumeLocationRequest() {
        if (monitoringStatus != MonitoringStatus.Paused) return
        requestCurrentLocation()
    }
}