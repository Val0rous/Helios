package com.ephemeris.helios.ui.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.GoogleSansFlexRounded
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.LocationStatus
import com.ephemeris.helios.utils.calc.AtmosphericConditions
import com.ephemeris.helios.utils.calc.GroundSurface
import com.ephemeris.helios.utils.datastore.CoordinateFormat
import com.ephemeris.helios.utils.formatShortLatitude
import com.ephemeris.helios.utils.formatShortLongitude
import com.ephemeris.helios.utils.formatTimeZone
import com.ephemeris.helios.utils.location.Coordinates
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentTime: ZonedDateTime,
    coordinates: Coordinates?,
    locationStatus: LocationStatus,
    coordinateFormat: CoordinateFormat,
    onToggleCoordinateFormat: () -> Unit,
    customTimezoneId: String?,
    onSelectCustomTimezone: (String?) -> Unit,
    onSaveCoordinates: (Coordinates, Boolean, Boolean) -> Unit,
    onLocationClick: () -> Unit,
    isTracking: Boolean,
    onToggleTracking: (Boolean) -> Unit,
    atmosphericConditions: AtmosphericConditions = AtmosphericConditions.DEFAULT,
    onAtmosphericConditionsChange: (AtmosphericConditions) -> Unit = {},
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showWeatherSheet by remember { mutableStateOf(false) }
    val locale = LocalLocale.current.platformLocale

    val isThinking = locationStatus == LocationStatus.SEARCHING || isTracking
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    TopAppBar(
        title = {
            if (coordinates != null) {
                val isDecimal = coordinateFormat == CoordinateFormat.DECIMAL
                val formattedLatitude = coordinates.latitude.formatShortLatitude(
                    isDecimal = isDecimal,
                    locale = locale,
                )
                val formattedLongitude = coordinates.longitude.formatShortLongitude(
                    isDecimal = isDecimal,
                    locale = locale,
                )
                val formattedTimeZone = remember(currentTime.zone, currentTime.offset, locale) {
                    currentTime.formatTimeZone(locale)
                }
                val formattedAlt = "${coordinates.altitude.roundToInt()}m"

                Surface(
                    onClick = { showBottomSheet = true },
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
                    ) {
                        LocationStatusTonalIconButton(
                            locationStatus = locationStatus,
                            isThinking = isThinking,
                            isTracking = isTracking,
                            pulseAlpha = alpha,
                            onClick = onLocationClick,
                            onLongClick = { onToggleTracking(!isTracking) },
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = coordinates.locationName ?: "Locating...",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "$formattedLatitude, $formattedLongitude · ${formattedTimeZone.utcOffset} · $formattedAlt",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = GoogleSansFlexRounded,
                                    fontFeatureSettings = "tnum",
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            } else {
                LocationStatusTonalIconButton(
                    locationStatus = locationStatus,
                    isThinking = isThinking,
                    isTracking = isTracking,
                    pulseAlpha = alpha,
                    onClick = onLocationClick,
                    onLongClick = { onToggleTracking(!isTracking) },
                )
            }
        },
        actions = {
            WeatherConditionIconButton(
                conditions = atmosphericConditions,
                onClick = { showWeatherSheet = true },
                onLongClick = {
                    if (!atmosphericConditions.isDefault) {
                        onAtmosphericConditionsChange(AtmosphericConditions.DEFAULT)
                    }
                },
            )
            IconButton(
                onClick = {}
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_page_info),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = "Settings",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )

    if (coordinates != null) {
        LocationBottomSheet(
            currentTime = currentTime,
            coordinates = coordinates,
            coordinateFormat = coordinateFormat,
            onToggleCoordinateFormat = onToggleCoordinateFormat,
            customTimezoneId = customTimezoneId,
            onSelectCustomTimezone = onSelectCustomTimezone,
            onSaveCoordinates = onSaveCoordinates,
            showBottomSheet = showBottomSheet,
            onShowBottomSheetChange = { showBottomSheet = it },
            isTracking = isTracking,
            onToggleTracking = onToggleTracking,
        )
    }

    WeatherBottomSheet(
        conditions = atmosphericConditions,
        onConditionsChange = onAtmosphericConditionsChange,
        showBottomSheet = showWeatherSheet,
        onShowBottomSheetChange = { showWeatherSheet = it },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeatherConditionIconButton(
    conditions: AtmosphericConditions,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isModified = !conditions.isDefault
    val containerColor by animateColorAsState(
        targetValue = if (isModified) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        label = "weatherButtonContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isModified) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "weatherButtonContent",
    )

    Box(
        modifier = Modifier.minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClickLabel = "Reset weather and ground to default",
                    onLongClick = {
                        if (isModified) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    },
                ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = conditions.weatherIconRes,
                    transitionSpec = {
                        fadeIn(tween(150)).togetherWith(fadeOut(tween(120)))
                    },
                    label = "topBarWeatherIcon",
                ) { iconRes ->
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Weather & ground: ${conditions.skyConditionLabel}",
                    )
                }

                // Subtle accent dot when a non-default ground surface is active
                if (conditions.hasCustomGround) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationStatusTonalIconButton(
    locationStatus: LocationStatus,
    isThinking: Boolean,
    isTracking: Boolean,
    pulseAlpha: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val containerColor by animateColorAsState(
        targetValue = when {
            isTracking -> MaterialTheme.colorScheme.primaryContainer
            locationStatus != LocationStatus.DISABLED -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "locationButtonContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isTracking -> MaterialTheme.colorScheme.onPrimaryContainer
            locationStatus != LocationStatus.DISABLED -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "locationButtonContent",
    )

    Box(
        modifier = Modifier.minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClickLabel = if (isTracking) {
                        "Disable continuous tracking"
                    } else {
                        "Enable continuous tracking"
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = locationStatus.icon),
                    contentDescription = stringResource(locationStatus.desc),
                )
                if (isThinking) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_my_location),
                        tint = contentColor.copy(alpha = pulseAlpha),
                        contentDescription = null,
                    )
                    if (!isTracking) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = contentColor,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TopBarPreview() {
    HeliosTheme {
        TopBar(
            currentTime = ZonedDateTime.of(2026, 7, 15, 14, 30, 0, 0, ZoneId.of("Europe/Rome")),
            coordinates = Coordinates(41.902782, 12.496366, 37.0, "Rome"),
            locationStatus = LocationStatus.CURRENT,
            coordinateFormat = CoordinateFormat.DECIMAL,
            onToggleCoordinateFormat = {},
            customTimezoneId = null,
            onSelectCustomTimezone = {},
            onSaveCoordinates = { _, _, _ -> },
            onLocationClick = {},
            isTracking = false,
            onToggleTracking = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopBarDmsPreview() {
    HeliosTheme {
        TopBar(
            currentTime = ZonedDateTime.of(2026, 7, 15, 14, 30, 0, 0, ZoneId.of("Europe/Rome")),
            coordinates = Coordinates(33.0, 12.0, 37.0, "Rome"),
            locationStatus = LocationStatus.CURRENT,
            coordinateFormat = CoordinateFormat.DMS,
            onToggleCoordinateFormat = {},
            customTimezoneId = null,
            onSelectCustomTimezone = {},
            onSaveCoordinates = { _, _, _ -> },
            onLocationClick = {},
            isTracking = false,
            onToggleTracking = {},
            atmosphericConditions = AtmosphericConditions(
                cloudCoverPercent = 40,
                groundSurface = GroundSurface.BEACH,
            ),
        )
    }
}
