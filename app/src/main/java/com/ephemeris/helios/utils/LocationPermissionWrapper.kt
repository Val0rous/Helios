package com.ephemeris.helios.utils

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun LocationPermissionWrapper(
    locationService: LocationService,
    snackbarHostState: SnackbarHostState,
    isContinuousGPSTrackingEnabled: Boolean,
    content: @Composable (
        requestOneOffLocation: () -> Unit,
        startContinuousTracking: (Long) -> Unit,
        stopTracking: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val showLocationDisabledAlert = remember { mutableStateOf(false) }
    val showPermissionDeniedAlert = remember { mutableStateOf(false) }
    val showPermissionPermanentlyDeniedSnackbar = remember { mutableStateOf(false) }

    // Automatic Lifecycle Handling (Replaces onPause/onResume in MainActivity)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) locationService.pauseLocationRequest()
            if (event == Lifecycle.Event.ON_RESUME) locationService.resumeLocationRequest()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationPermission = rememberPermission(Manifest.permission.ACCESS_FINE_LOCATION) { status ->
        when (status) {
            PermissionStatus.Granted -> {
                val res = locationService.requestCurrentLocation()
                showLocationDisabledAlert.value = res == StartMonitoringResult.GPSDisabled
            }
            PermissionStatus.Denied -> showPermissionDeniedAlert.value = true
            PermissionStatus.PermanentlyDenied -> showPermissionPermanentlyDeniedSnackbar.value = true
            PermissionStatus.Unknown -> {}
        }
    }

    fun getGPSLocation(isTrackingEnabled: Boolean, interval: Long = 0L) {
        if (locationPermission.status.isGranted) {
            val res = if (isTrackingEnabled) {
                locationService.startLocationTracking(interval)
            } else {
                locationService.requestCurrentLocation()
            }
            showLocationDisabledAlert.value = res == StartMonitoringResult.GPSDisabled
        } else {
            locationPermission.launchPermissionRequest()
        }
    }

    // Exposed Functions to UI
    val requestOneOffLocation = { getGPSLocation(false) }

    val startContinuousTracking = { interval: Long ->
        getGPSLocation(true, interval)
    }

    val stopTracking = { locationService.stopLocationTracking() }

    // Render Dialogs Invisibly
    if (showLocationDisabledAlert.value) {
        AlertDialog(
            title = { Text("Location disabled") },
            text = { Text("Location must be enabled to get your current location in the app.") },
            confirmButton = {
                TextButton(onClick = {
                    locationService.openLocationSettings()
                    showLocationDisabledAlert.value = false
                }) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDisabledAlert.value = false }) { Text("Dismiss") }
            },
            onDismissRequest = { showLocationDisabledAlert.value = false }
        )
    }

    if (showPermissionDeniedAlert.value) {
        AlertDialog(
            title = { Text("Location permission denied") },
            text = { Text("Location permission is required to get your current location in the app.") },
            confirmButton = {
                TextButton(onClick = {
                    locationPermission.launchPermissionRequest()
                    showPermissionDeniedAlert.value = false
                }) { Text("Grant") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedAlert.value = false }) { Text("Dismiss") }
            },
            onDismissRequest = { showPermissionDeniedAlert.value = false }
        )
    }

    if (showPermissionPermanentlyDeniedSnackbar.value) {
        LaunchedEffect(snackbarHostState) {
            val res = snackbarHostState.showSnackbar("Location permission is required.", "Go to Settings", duration = SnackbarDuration.Long)
            if (res == SnackbarResult.ActionPerformed) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
            showPermissionPermanentlyDeniedSnackbar.value = false
        }
    }

    // Render the actual app UI
    content(requestOneOffLocation, startContinuousTracking, stopTracking)
}