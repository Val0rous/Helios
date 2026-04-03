package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.utils.Coordinates
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapCard(
    location: Coordinates,
    modifier: Modifier = Modifier
) {
    val effectiveIsDarkTheme = isSystemInDarkTheme()

    val coordinates = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(coordinates, 15f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))    // Was 28.dp
            .height(218.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (true) {
            GoogleMap(
                modifier = Modifier.fillMaxWidth(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    compassEnabled = true,
                    mapToolbarEnabled = true,
                    myLocationButtonEnabled = true,
                    scrollGesturesEnabled = true,
                    tiltGesturesEnabled = false,
                    rotationGesturesEnabled = true,
                    zoomControlsEnabled = false,
                    zoomGesturesEnabled = true
                ),
                googleMapOptionsFactory = {
                    GoogleMapOptions().mapColorScheme(
                        if (effectiveIsDarkTheme) {
                            MapColorScheme.DARK
                        } else {
                            MapColorScheme.LIGHT
                        }
                    )
                }
            ) {
                Marker(
                    state = remember { MarkerState(position = coordinates) },
                    title = "", // Optional title
                    snippet = "Latitude: ${location.latitude}, Longitude: ${location.longitude}",  // Optional snippet
                )
            }
        } else {
            CircularProgressIndicator()
        }
    }
}