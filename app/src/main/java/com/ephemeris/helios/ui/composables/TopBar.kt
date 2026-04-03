package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.ephemeris.helios.R
import com.ephemeris.helios.utils.LocationService
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentTime: ZonedDateTime,
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
                val formattedLat = String.format(
                    LocalLocale.current.platformLocale,
                    "%.2f",
                    coordinates.latitude
                )
                val formattedLon = String.format(
                    LocalLocale.current.platformLocale,
                    "%.2f",
                    coordinates.longitude
                )
                val formattedTimeZone = currentTime.format(DateTimeFormatter.ofPattern("z"))
//                val formattedOffset = "UTC${currentTime.offset}"
                val formattedAlt = "${coordinates.altitude.roundToInt()}m"

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Hamilton St., London",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$formattedLat, $formattedLon · $formattedTimeZone · $formattedAlt",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {}
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_page_info),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = "Settings"
                    )
                }
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
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    )

    LocationBottomSheet(
        currentTime = currentTime,
        coordinates = coordinates,
        onSaveCoordinates = onSaveCoordinates,
        onLocationStatusChange = { locationStatus = it },
        showBottomSheet = showBottomSheet,
        onShowBottomSheetChange = { showBottomSheet = it }
    )
}