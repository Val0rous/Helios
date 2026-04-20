package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ephemeris.helios.utils.location.Coordinates
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenMapView(
    location: Coordinates,
    modifier: Modifier = Modifier
) {
    val effectiveIsDarkTheme = isSystemInDarkTheme()

    val coordinates = LatLng(location.latitude, location.longitude)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(coordinates, 10f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            compassEnabled = true,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = true,
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

    }
}