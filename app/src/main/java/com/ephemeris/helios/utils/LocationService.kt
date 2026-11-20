package com.ephemeris.helios.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class MonitoringStatus { Monitoring, Paused, NotMonitoring }

enum class StartMonitoringResult { Started, GPSDisabled, PermissionDenied }

sealed interface GpsEvent {
    data class Fix(val location: Location) : GpsEvent
    data object Failed : GpsEvent
}

class LocationService(
    private val context: Context,
) {
    private var monitoringStatus = MonitoringStatus.NotMonitoring

    private val _gpsEvents = MutableSharedFlow<GpsEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val gpsEvents: SharedFlow<GpsEvent> = _gpsEvents.asSharedFlow()

    private val fusedLocationProviderClient = LocationServices
        .getFusedLocationProviderClient(context)

    private var currentIntervalMillis = 10000L

    private fun dispatchLocation(location: Location) {
        _gpsEvents.tryEmit(GpsEvent.Fix(location))
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            p0.locations.lastOrNull()?.let { dispatchLocation(it) }
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
        val finePermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarsePermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!(finePermissionGranted || coarsePermissionGranted)) return StartMonitoringResult.PermissionDenied

        return null // All good
    }

    // One-Off Fast Request (Hybrid Waterfall Approach)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun requestCurrentLocation(): StartMonitoringResult {
        checkPrerequisites()?.let {
            _gpsEvents.tryEmit(GpsEvent.Failed)
            return it
        }

        // Step 1: Try the blazing-fast Fused Location API first
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationProviderClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                // Success! We got a fast, network-assisted location.
                dispatchLocation(location)
            } else {
                // Fused Location failed or we are offline. Trigger the hardware fallback!
                triggerHardwareGpsFallback()
            }
        }.addOnFailureListener {
            // Something crashed internally with Play Services. Trigger fallback!
            triggerHardwareGpsFallback()
        }

        return StartMonitoringResult.Started
    }

    // Step 2: The Offline/Hardware Fallback Helper
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun triggerHardwareGpsFallback() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Tell the user we are attempting a hard offline lock
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Offline: Searching for satellites...", Toast.LENGTH_LONG).show()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+: Force the hardware antenna to scan the sky
            locationManager.getCurrentLocation(
                LocationManager.GPS_PROVIDER,
                null,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                if (location != null) {
                    dispatchLocation(location)
                } else {
                    _gpsEvents.tryEmit(GpsEvent.Failed)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "GPS Failed. Are you indoors?", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            // Legacy Android hardware request
            @Suppress("DEPRECATION")
            locationManager.requestSingleUpdate(
                LocationManager.GPS_PROVIDER,
                { location ->
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Satellite lock acquired!", Toast.LENGTH_SHORT).show()
                    }
                    dispatchLocation(location)
                },
                Looper.getMainLooper()
            )
        }
    }

    // Continuous Tracking
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startLocationTracking(intervalMillis: Long = 10000L): StartMonitoringResult {
        checkPrerequisites()?.let {
            _gpsEvents.tryEmit(GpsEvent.Failed)
            return it
        }

        currentIntervalMillis = intervalMillis
        val request = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, currentIntervalMillis)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .build()

        fusedLocationProviderClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
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
