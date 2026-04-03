package com.ephemeris.helios.ui.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.cards.MapCard
import com.ephemeris.helios.ui.composables.entries.TextEntryLocation
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.LocationStatus
import com.ephemeris.helios.utils.formatLatitude
import com.ephemeris.helios.utils.formatLongitude
import com.ephemeris.helios.utils.round
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationBottomSheet(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    onSaveCoordinates: (Coordinates) -> Unit,
    onLocationStatusChange: (LocationStatus) -> Unit,
    showBottomSheet: Boolean,
    onShowBottomSheetChange: (Boolean) -> Unit,
    isTracking: Boolean,
    onToggleTracking: (Boolean) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                onShowBottomSheetChange(false)
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
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + slideInVertically(
                            initialOffsetY = { 90 }))
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
                                        onLocationStatusChange(LocationStatus.DISABLED)
                                        onSaveCoordinates(Coordinates(latDouble!!, lonDouble!!, altDouble!!,))
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

                            // Smooth color transitions for the switch surface
                            val surfaceColor by animateColorAsState(
                                targetValue = if (isTracking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                label = "surfaceColor"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isTracking) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                label = "contentColor"
                            )
                            val iconTint by animateColorAsState(
                                targetValue = if (isTracking) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                label = "iconTint"
                            )

                            Surface(
                                color = surfaceColor,
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .toggleable(
                                            value = isTracking,
                                            onValueChange = { onToggleTracking(it) },
                                            role = Role.Switch
                                        )
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_my_location),
                                            contentDescription = null,
                                            tint = iconTint
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = "Continuous Tracking",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = contentColor
                                        )
                                    }

                                    // Custom icons for the internal switch thumb
                                    val switchIcon: (@Composable () -> Unit)? = if (isTracking) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize)
                                            )
                                        }
                                    } else {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize)
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isTracking,
                                        onCheckedChange = null,  // Handled implicitly by the toggleable Row
                                        thumbContent = switchIcon
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("Location Details", style = MaterialTheme.typography.titleLarge)

                            Spacer(modifier = Modifier.height(16.dp))

                            MapCard(
                                location = coordinates,
                                modifier = Modifier
                                    .padding(horizontal = 0.dp)
                                    .padding(top = 4.dp, bottom = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            TextEntryLocation(
                                label = "Latitude",
                                value = "${coordinates.latitude.round(6)}°",
                                extra = coordinates.latitude.formatLatitude()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TextEntryLocation(
                                label = "Longitude",
                                value = "${coordinates.longitude.round(6)}°",
                                extra = coordinates.longitude.formatLongitude()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TextEntryLocation(
                                label = "Altitude",
                                value = "${coordinates.altitude} m"
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TextEntryLocation(
                                label = "Time Zone",
                                value = "GMT${currentTime.offset}",
                                extra = currentTime.zone.id
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