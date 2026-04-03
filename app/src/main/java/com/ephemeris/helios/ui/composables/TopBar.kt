package com.ephemeris.helios.ui.composables

import android.icu.util.TimeZone
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.LocationStatus
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.utils.LocationService
import com.ephemeris.helios.utils.formatShortLatitude
import com.ephemeris.helios.utils.formatShortLongitude
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    onSaveCoordinates: (Coordinates) -> Unit,
    onLocationClick: () -> Unit,
    isTracking: Boolean,
    onToggleTracking: (Boolean) -> Unit,
    locationService: LocationService
) {
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

    // Force icon state if continuous tracking gets enabled elsewhere
    LaunchedEffect(isTracking) {
        if (isTracking && locationStatus != LocationStatus.CURRENT) {
            locationStatus = LocationStatus.SEARCHING
        }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    val color = if (locationStatus != LocationStatus.DISABLED || isTracking) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Animation setup for the tracking pulsing effect
    val isThinking = locationStatus == LocationStatus.SEARCHING || isTracking
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    TopAppBar(
        title = {
            TextButton(
                onClick = { showBottomSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                val formattedLatitude = coordinates.latitude.formatShortLatitude()
                val formattedLongitude = coordinates.longitude.formatShortLongitude()

                val formattedTimeZone = currentTime.format(DateTimeFormatter.ofPattern("z"))
                // 1. Abbreviations and Full Names
//                val shortName = currentTime.format(DateTimeFormatter.ofPattern("zzz", LocalLocale.current.platformLocale))
                // Output: "CEST"
                // Actual Output: "GMT+02:00"

                val instant = currentTime.toInstant()
                val isDstJava = currentTime.zone.rules.isDaylightSavings(instant)
                val legacyZone = TimeZone.getTimeZone(currentTime.zone.id)
                val shortishName = legacyZone.getDisplayName(
                    isDstJava, TimeZone.SHORT, LocalLocale.current.platformLocale
                )
                // GMT+2

                val fullName = currentTime.format(DateTimeFormatter.ofPattern("zzzz", LocalLocale.current.platformLocale))
                // Output: "Central European Summer Time"

                // 2. The Zone Rules & DST Flag
                val rules = currentTime.zone.rules

                val isDst = rules.isDaylightSavings(instant)
                // Output: true or false
                // This one works

                // 3. DST Start and End Transitions
                val previousTransition = rules.previousTransition(instant)
                // Output: 2026-03-29T01:00:00Z (When DST started)
                // This works

                val nextTransition = rules.nextTransition(instant)
                // Output: 2026-10-25T01:00:00Z (When DST ends)

                // Formatting the transitions into readable local time
                val formattedNextTransition = nextTransition?.instant?.let { transitionInstant ->
                    ZonedDateTime.ofInstant(transitionInstant, currentTime.zone)
                        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                }
                val formattedOffset = "UTC${currentTime.offset}"
                val shortName = currentTime.format(DateTimeFormatter.ofPattern("zzzz", Locale.ENGLISH)).filter { it.isUpperCase() }
                val formattedAlt = "${coordinates.altitude.roundToInt()}m"

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Hamilton St., London",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$formattedLatitude, $formattedLongitude · $formattedTimeZone · $shortName · $formattedAlt",
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = locationStatus.icon),
                            tint = color,
                            contentDescription = stringResource(locationStatus.desc)
                        )
                        if (isThinking) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_my_location),
                                tint = color.copy(alpha = alpha),
                                contentDescription = null
                            )
                            if (!isTracking) CircularProgressIndicator(strokeWidth = 2.dp)
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
        onShowBottomSheetChange = { showBottomSheet = it },
        isTracking = isTracking,
        onToggleTracking = onToggleTracking
    )
}