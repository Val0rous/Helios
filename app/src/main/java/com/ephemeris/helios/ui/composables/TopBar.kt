package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.LocationStatus
import androidx.compose.ui.platform.LocalLocale
import com.ephemeris.helios.utils.LocationService

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
                    altitude = it.altitude,
                )
            }
            if (location != null) {
                locationStatus = LocationStatus.CURRENT
                onSaveCoordinates(location)
            }
        }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = locationStatus.icon),
                        tint = color,
                        contentDescription = stringResource(locationStatus.desc)
                    )
                    if (locationStatus == LocationStatus.SEARCHING) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    )

    LocationBottomSheet(
        coordinates = coordinates,
        onSaveCoordinates = onSaveCoordinates,
        onLocationStatusChange = { locationStatus = it },
        showBottomSheet = showBottomSheet,
        onShowBottomSheetChange = { showBottomSheet = it }
    )
}