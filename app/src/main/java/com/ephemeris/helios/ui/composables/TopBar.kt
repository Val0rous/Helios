package com.ephemeris.helios.ui.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.ephemeris.helios.utils.LocationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(onLocationClick: () -> Unit) {
    val locationStatus = LocationStatus.CURRENT // Todo: make it store user setting
    val color = if (locationStatus != LocationStatus.DISABLED) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    TopAppBar(
        title = { Text("Location") },
        actions = {
            IconButton(onClick = onLocationClick) {
                Icon(
                    painter = painterResource(id = locationStatus.icon),
                    tint = color,
                    contentDescription = locationStatus.desc
                )
            }
        }
    )
}