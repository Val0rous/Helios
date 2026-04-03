package com.ephemeris.helios.ui.composables

import android.Manifest
import android.R.attr.data
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.LocationStatus
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.cards.MapCard
import com.ephemeris.helios.utils.LocationService
import com.ephemeris.helios.utils.PermissionStatus
import com.ephemeris.helios.utils.StartMonitoringResult
import com.ephemeris.helios.utils.rememberPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    coordinates: Coordinates,
    onSaveCoordinates: (Coordinates) -> Unit,
    onLocationClick: () -> Unit,
    locationService: LocationService
) {
    var isLocationLoaded by remember { mutableStateOf(false) }
    var isGps by remember { mutableStateOf(false) }
    var locationStatus by remember { mutableStateOf(LocationStatus.DISABLED) } // Todo: make it store user setting

    LaunchedEffect(key1 = locationService.coordinates) {
        if (locationService.coordinates != null) {
            val location = locationService.coordinates?.let {
                Coordinates(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    altitude = it.altitude
                )
            }
            if (location != null) {
                locationStatus = LocationStatus.CURRENT
                onSaveCoordinates(location)
            }
        }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val color = if (locationStatus != LocationStatus.DISABLED) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    TopAppBar(
        title = {
            TextButton(onClick = { showBottomSheet = true }) {
                val formattedLat = String.format(LocalLocale.current.platformLocale, "%.4f", coordinates.latitude)
                val formattedLon = String.format(LocalLocale.current.platformLocale, "%.4f", coordinates.longitude)
                Text("$formattedLat, $formattedLon")
            }
        },
        actions = {
            IconButton(
                onClick = {
                    locationStatus = LocationStatus.SEARCHING
                    onLocationClick()
                }
            ) {
                Icon(
                    painter = painterResource(id = locationStatus.icon),
                    tint = color,
                    contentDescription = stringResource(locationStatus.desc)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                isEditing = false // Reset edit state when closed
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp) // Extra padding for system navigation bar
            ) {
                AnimatedContent(
                    targetState = isEditing,
                    transitionSpec = {
                        // Smooth slide-and-fade transition
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + slideInVertically(initialOffsetY = { 90 }))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "EditViewTransition"
                ) { targetIsEditing ->
                    if (targetIsEditing) {
                        // --- EDIT MODE ---
                        // Local states to hold the text input before saving
                        var latInput by remember { mutableStateOf(coordinates.latitude.toString()) }
                        var lonInput by remember { mutableStateOf(coordinates.longitude.toString()) }
                        var altInput by remember { mutableStateOf(coordinates.altitude.toString()) }

                        // 1. Validation Logic
                        val latDouble = latInput.toDoubleOrNull()
                        val lonDouble = lonInput.toDoubleOrNull()
                        val altDouble = altInput.toDoubleOrNull()

                        val isLatError = latDouble == null || latDouble !in -90.0..90.0
                        val isLonError = lonDouble == null || lonDouble !in -180.0..180.0
                        val isAltError = altDouble == null || altDouble !in -500.0..20000.0
                        val isSaveEnabled = !isLatError && !isLonError && !isAltError

                        // 2. Sanitization Regex: Allows optional minus sign, numbers, and one optional decimal point
                        val decimalRegex = Regex("^-?[0-9]*\\.?[0-9]*$")
                        Column {

                            Text("Edit Coordinates", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = latInput,
                                onValueChange = { if (it.matches(decimalRegex)) latInput = it },
                                label = { Text("Latitude (Decimal Degrees)") },
                                isError = isLatError,
                                supportingText = { if (isLatError) Text("Must be between -90° and 90°") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = lonInput,
                                onValueChange = { if (it.matches(decimalRegex)) lonInput = it },
                                label = { Text("Longitude (Decimal Degrees)") },
                                isError = isLonError,
                                supportingText = { if (isLonError) Text("Must be between -180° and 180°") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = altInput,
                                onValueChange = { if (it.matches(decimalRegex)) altInput = it },
                                label = { Text("Altitude (Meters)") },
                                isError = isAltError,
                                supportingText = { if (isAltError) Text("Must be between -500m and 20,000m") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isEditing = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        // Trigger the save callback
                                        // We know these aren't null because the button is enabled
                                        onSaveCoordinates(
                                            Coordinates(
                                                latDouble!!,
                                                lonDouble!!,
                                                altDouble!!
                                            )
                                        )
                                        // Return to view mode
                                        isEditing = false
                                    },
                                    enabled = isSaveEnabled
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    } else {
                        // --- VIEW MODE ---
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Location Details", style = MaterialTheme.typography.titleLarge)

                            Spacer(modifier = Modifier.height(16.dp))

                            MapCard(
                                location = coordinates,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 6.dp, bottom = 1.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Latitude: ${coordinates.latitude}°",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Longitude: ${coordinates.longitude}°",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Altitude: ${coordinates.altitude} m",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Edit Coordinates")
                            }
                        }
                    }
                }
            }
        }
    }
}