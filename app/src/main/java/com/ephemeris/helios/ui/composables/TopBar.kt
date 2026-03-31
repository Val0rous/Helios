package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.LocationStatus
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    coordinates: Coordinates,
    onSaveCoordinates: (Coordinates) -> Unit,
    onLocationClick: () -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    val locationStatus = LocationStatus.CURRENT // Todo: make it store user setting
    val color = if (locationStatus != LocationStatus.DISABLED) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    TopAppBar(
        title = {
            TextButton(onClick = { showBottomSheet = true }) {
                val formattedLat = String.format(Locale.getDefault(), "%.4f", coordinates.latitude)
                val formattedLon = String.format(Locale.getDefault(), "%.4f", coordinates.longitude)
                Text("$formattedLat, $formattedLon")
            }
        },
        actions = {
            IconButton(onClick = onLocationClick) {
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
                if (isEditing) {
                    // --- EDIT MODE ---
                    // Local states to hold the text input before saving
                    var latInput by remember { mutableStateOf(coordinates.latitude.toString()) }
                    var lonInput by remember { mutableStateOf(coordinates.longitude.toString()) }
                    var altInput by remember { mutableStateOf(coordinates.altitude.toString()) }

                    Text("Edit Coordinates", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text("Latitude (Decimal Degrees)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lonInput,
                        onValueChange = { lonInput = it },
                        label = { Text("Longitude (Decimal Degrees)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = altInput,
                        onValueChange = { altInput = it },
                        label = { Text("Altitude (Meters)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        Button(onClick = {
                            // Safely parse strings back to doubles, falling back to old values if invalid
                            val newLat = latInput.toDoubleOrNull() ?: coordinates.latitude
                            val newLon = lonInput.toDoubleOrNull() ?: coordinates.longitude
                            val newAlt = altInput.toDoubleOrNull() ?: 0.0

                            // Trigger the save callback
                            onSaveCoordinates(Coordinates(newLat, newLon, newAlt))
                            // Return to view mode
                            isEditing = false
                        }) {
                            Text("Save")
                        }
                    }
                } else {
                    // --- VIEW MODE ---
                    Text("Location Details", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Latitude: ${coordinates.latitude}°", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Longitude: ${coordinates.longitude}°", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Altitude: ${coordinates.altitude} m", style = MaterialTheme.typography.bodyLarge)

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