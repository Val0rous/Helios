package com.ephemeris.helios.ui.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.cards.MapCard
import com.ephemeris.helios.ui.composables.entries.MetricModePill
import com.ephemeris.helios.ui.theme.GoogleSansFlexRounded
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.datastore.CoordinateFormat
import com.ephemeris.helios.utils.dmsToDecimal
import com.ephemeris.helios.utils.formatDecimalDegrees
import com.ephemeris.helios.utils.formatLatitude
import com.ephemeris.helios.utils.formatLongitude
import com.ephemeris.helios.utils.formatTimeZone
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.printRounded
import com.ephemeris.helios.utils.toDmsCoordinate
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationBottomSheet(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    coordinateFormat: CoordinateFormat,
    onToggleCoordinateFormat: () -> Unit,
    customTimezoneId: String?,
    onSelectCustomTimezone: (String?) -> Unit,
    onSaveCoordinates: (Coordinates, Boolean, Boolean) -> Unit,
    showBottomSheet: Boolean,
    onShowBottomSheetChange: (Boolean) -> Unit,
    isTracking: Boolean,
    onToggleTracking: (Boolean) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var showTimezoneDialog by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = {
                onShowBottomSheetChange(false)
                isEditing = false // Reset edit state when closed
            },
            sheetState = sheetState,
            // Disable ModalBottomSheet's built-in predictive-back scaling handler;
            // handle back cleanly below (Edit -> Details, or Details -> hide sheet).
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        ) {
            val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
            NavigationBackHandler(
                state = navigationState,
                isBackEnabled = true,
                onBackCompleted = {
                    if (isEditing) {
                        isEditing = false
                    } else {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onShowBottomSheetChange(false)
                                isEditing = false
                            }
                        }
                    }
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp), // Extra padding for system navigation bar
            ) {
                AnimatedContent(
                    targetState = isEditing,
                    transitionSpec = {
                        // Smooth slide-and-fade transition
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + slideInVertically(
                            initialOffsetY = { 90 }
                        ))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "EditViewTransition",
                ) { targetIsEditing ->
                    if (targetIsEditing) {
                        EditCoordinatesContent(
                            coordinates = coordinates,
                            coordinateFormat = coordinateFormat,
                            onToggleCoordinateFormat = onToggleCoordinateFormat,
                            onBack = { isEditing = false },
                            onSave = { newCoords, autoElevation ->
                                onSaveCoordinates(newCoords, false, autoElevation)
                                isEditing = false
                            },
                        )
                    } else {
                        LocationDetailsContent(
                            currentTime = currentTime,
                            coordinates = coordinates,
                            isTracking = isTracking,
                            onToggleTracking = onToggleTracking,
                            coordinateFormat = coordinateFormat,
                            onToggleCoordinateFormat = onToggleCoordinateFormat,
                            isCustomTimezone = !customTimezoneId.isNullOrBlank(),
                            onTimeZoneClick = { showTimezoneDialog = true },
                            onOpenLocationList = {},
                            onEditCoordinates = { isEditing = true },
                        )
                    }
                }
            }
        }
    }

    if (showTimezoneDialog) {
        TimeZonePickerDialog(
            currentTime = currentTime,
            defaultTimezoneId = coordinates.timezoneId,
            customTimezoneId = customTimezoneId,
            onSelectTimezone = onSelectCustomTimezone,
            onDismiss = { showTimezoneDialog = false },
        )
    }
}

private fun formatDecimalForInput(value: Double, maxDecimals: Int = 6): String {
    val bd = BigDecimal.valueOf(value).setScale(maxDecimals, RoundingMode.HALF_UP)
    if (bd.signum() == 0) return "0"
    return bd.stripTrailingZeros().toPlainString()
}

private fun parseDmsInput(
    degText: String,
    minText: String,
    secText: String,
    isPositive: Boolean,
    maxDegrees: Int,
): Double? {
    val deg = degText.toIntOrNull() ?: return null
    val min = if (minText.isBlank()) 0 else minText.toIntOrNull() ?: return null
    val sec = if (secText.isBlank()) 0.0 else secText.toDoubleOrNull() ?: return null
    if (deg !in 0..maxDegrees) return null
    if (min !in 0..59) return null
    if (sec < 0.0 || sec >= 60.0) return null
    if (deg == maxDegrees && (min > 0 || sec > 0.0)) return null
    return dmsToDecimal(deg, min, sec, isPositive)
}

@Composable
internal fun EditCoordinatesContent(
    coordinates: Coordinates,
    coordinateFormat: CoordinateFormat,
    onToggleCoordinateFormat: () -> Unit,
    onBack: () -> Unit,
    onSave: (Coordinates, Boolean) -> Unit,
) {
    val initialLatDms = remember(coordinates) { coordinates.latitude.toDmsCoordinate(secondsDecimals = 2) }
    val initialLonDms = remember(coordinates) { coordinates.longitude.toDmsCoordinate(secondsDecimals = 2) }

    var latInput by remember(coordinates) { mutableStateOf(formatDecimalForInput(coordinates.latitude)) }
    var lonInput by remember(coordinates) { mutableStateOf(formatDecimalForInput(coordinates.longitude)) }

    var latDeg by remember(coordinates) { mutableStateOf(initialLatDms.degrees.toString()) }
    var latMin by remember(coordinates) { mutableStateOf(initialLatDms.minutes.toString()) }
    var latSec by remember(coordinates) { mutableStateOf(initialLatDms.seconds.printRounded(decimals = 2)) }
    var latIsNorth by remember(coordinates) { mutableStateOf(initialLatDms.isPositive) }

    var lonDeg by remember(coordinates) { mutableStateOf(initialLonDms.degrees.toString()) }
    var lonMin by remember(coordinates) { mutableStateOf(initialLonDms.minutes.toString()) }
    var lonSec by remember(coordinates) { mutableStateOf(initialLonDms.seconds.printRounded(decimals = 2)) }
    var lonIsEast by remember(coordinates) { mutableStateOf(initialLonDms.isPositive) }

    val initialAltText = remember(coordinates) { coordinates.altitude.printRounded(decimals = 1) }
    var altInput by remember(coordinates) { mutableStateOf(initialAltText) }

    var lastSyncedFormat by remember { mutableStateOf(coordinateFormat) }
    LaunchedEffect(coordinateFormat) {
        if (coordinateFormat != lastSyncedFormat) {
            if (coordinateFormat == CoordinateFormat.DMS) {
                latInput.toDoubleOrNull()?.takeIf { it in -90.0..90.0 }?.let { dec ->
                    val dms = dec.toDmsCoordinate(secondsDecimals = 2)
                    latDeg = dms.degrees.toString()
                    latMin = dms.minutes.toString()
                    latSec = dms.seconds.printRounded(decimals = 2)
                    latIsNorth = dms.isPositive
                }
                lonInput.toDoubleOrNull()?.takeIf { it in -180.0..180.0 }?.let { dec ->
                    val dms = dec.toDmsCoordinate(secondsDecimals = 2)
                    lonDeg = dms.degrees.toString()
                    lonMin = dms.minutes.toString()
                    lonSec = dms.seconds.printRounded(decimals = 2)
                    lonIsEast = dms.isPositive
                }
            } else {
                parseDmsInput(latDeg, latMin, latSec, latIsNorth, maxDegrees = 90)?.let { dec ->
                    latInput = formatDecimalForInput(dec)
                }
                parseDmsInput(lonDeg, lonMin, lonSec, lonIsEast, maxDegrees = 180)?.let { dec ->
                    lonInput = formatDecimalForInput(dec)
                }
            }
            lastSyncedFormat = coordinateFormat
        }
    }

    val isDms = coordinateFormat == CoordinateFormat.DMS

    val resolvedLat = if (isDms) {
        parseDmsInput(latDeg, latMin, latSec, latIsNorth, maxDegrees = 90)
    } else {
        latInput.toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
    }
    val resolvedLon = if (isDms) {
        parseDmsInput(lonDeg, lonMin, lonSec, lonIsEast, maxDegrees = 180)
    } else {
        lonInput.toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
    }
    val altDouble = if (altInput.isBlank()) null else altInput.toDoubleOrNull()

    val isLatError = resolvedLat == null
    val isLonError = resolvedLon == null
    val isAltError = altInput.isNotBlank() && (altDouble == null || altDouble !in -500.0..20000.0)
    val isSaveEnabled = !isLatError && !isLonError && !isAltError

    val signedDecimalRegex = remember { Regex("^-?[0-9]*\\.?[0-9]*$") }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header with Back button beside title + persisted Decimal / DMS pill switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Edit Coordinates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            MetricModePill(
                label = coordinateFormat.label,
                onClick = onToggleCoordinateFormat,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        val numericInputStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = GoogleSansFlexRounded,
            fontWeight = FontWeight.Medium,
            fontFeatureSettings = "tnum",
        )

        AnimatedContent(
            targetState = isDms,
            transitionSpec = {
                fadeIn(tween(150)).togetherWith(fadeOut(tween(120)))
                    .using(SizeTransform(clip = false))
            },
            label = "EditCoordinateFormatTransition",
        ) { targetDms ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (targetDms) {
                    DmsCoordinateFieldGroup(
                        title = "Latitude",
                        deg = latDeg,
                        onDegChange = { latDeg = it },
                        min = latMin,
                        onMinChange = { latMin = it },
                        sec = latSec,
                        onSecChange = { latSec = it },
                        isPositive = latIsNorth,
                        positiveLabel = "N",
                        negativeLabel = "S",
                        onToggleDirection = { latIsNorth = !latIsNorth },
                        isError = isLatError,
                        errorText = "Must be within 0°–90°, 0′–59′, 0″–59.99″",
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DmsCoordinateFieldGroup(
                        title = "Longitude",
                        deg = lonDeg,
                        onDegChange = { lonDeg = it },
                        min = lonMin,
                        onMinChange = { lonMin = it },
                        sec = lonSec,
                        onSecChange = { lonSec = it },
                        isPositive = lonIsEast,
                        positiveLabel = "E",
                        negativeLabel = "W",
                        onToggleDirection = { lonIsEast = !lonIsEast },
                        isError = isLonError,
                        errorText = "Must be within 0°–180°, 0′–59′, 0″–59.99″",
                    )
                } else {
                    SingleValueFieldGroup(
                        title = "Latitude",
                        value = latInput,
                        onValueChange = { raw ->
                            val normalized = raw.replace(',', '.')
                            if (normalized.matches(signedDecimalRegex)) latInput = normalized
                        },
                        fieldLabel = "Degrees °",
                        isError = isLatError,
                        errorText = "Must be between -90° and 90°",
                        textStyle = numericInputStyle,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SingleValueFieldGroup(
                        title = "Longitude",
                        value = lonInput,
                        onValueChange = { raw ->
                            val normalized = raw.replace(',', '.')
                            if (normalized.matches(signedDecimalRegex)) lonInput = normalized
                        },
                        fieldLabel = "Degrees °",
                        isError = isLonError,
                        errorText = "Must be between -180° and 180°",
                        textStyle = numericInputStyle,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SingleValueFieldGroup(
            title = "Altitude",
            trailingHint = "Leave blank for auto",
            value = altInput,
            onValueChange = { raw ->
                val normalized = raw.replace(',', '.')
                if (normalized.matches(signedDecimalRegex)) altInput = normalized
            },
            fieldLabel = "Meters (m)",
            placeholder = "Auto",
            isError = isAltError,
            errorText = "Must be between -500 m and 20,000 m",
            textStyle = numericInputStyle,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val altExplicitlyEdited = altInput.isNotBlank() && altInput != initialAltText
                onSave(
                    Coordinates(resolvedLat!!, resolvedLon!!, altDouble ?: coordinates.altitude),
                    !altExplicitlyEdited,
                )
            },
            enabled = isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}

@Composable
private fun SingleValueFieldGroup(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    fieldLabel: String,
    isError: Boolean,
    errorText: String,
    textStyle: TextStyle,
    trailingHint: String? = null,
    placeholder: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (trailingHint != null) {
                Text(
                    text = trailingHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(fieldLabel) },
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = textStyle,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
private fun DmsCoordinateFieldGroup(
    title: String,
    deg: String,
    onDegChange: (String) -> Unit,
    min: String,
    onMinChange: (String) -> Unit,
    sec: String,
    onSecChange: (String) -> Unit,
    isPositive: Boolean,
    positiveLabel: String,
    negativeLabel: String,
    onToggleDirection: () -> Unit,
    isError: Boolean,
    errorText: String,
) {
    val degRegex = remember { Regex("^[0-9]{0,3}$") }
    val minRegex = remember { Regex("^[0-9]{0,2}$") }
    val secRegex = remember { Regex("^[0-9]{0,2}(\\.[0-9]{0,2})?$") }
    val numericInputStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = deg,
                onValueChange = { if (it.matches(degRegex)) onDegChange(it) },
                label = { Text("Deg °") },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = numericInputStyle,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = min,
                onValueChange = { if (it.matches(minRegex)) onMinChange(it) },
                label = { Text("Min ′") },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = numericInputStyle,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = sec,
                onValueChange = { raw ->
                    val normalized = raw.replace(',', '.')
                    if (normalized.matches(secRegex)) onSecChange(normalized)
                },
                label = { Text("Sec ″") },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = numericInputStyle,
                modifier = Modifier.weight(1.25f),
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(50.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onToggleDirection),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isPositive) positiveLabel else negativeLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
        if (isError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Composable
internal fun LocationDetailsContent(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    isTracking: Boolean,
    onToggleTracking: (Boolean) -> Unit,
    coordinateFormat: CoordinateFormat,
    onToggleCoordinateFormat: () -> Unit,
    isCustomTimezone: Boolean,
    onTimeZoneClick: () -> Unit,
    onOpenLocationList: () -> Unit,
    onEditCoordinates: () -> Unit,
    mapContent: @Composable () -> Unit = {
        MapCard(
            location = coordinates,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    },
) {
    val locale = LocalLocale.current.platformLocale
    val tz = remember(currentTime.zone, currentTime.offset, locale) {
        currentTime.formatTimeZone(locale)
    }

    val surfaceColor by animateColorAsState(
        targetValue = if (isTracking) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "surfaceColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isTracking) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "contentColor",
    )
    val iconTint by animateColorAsState(
        targetValue = if (isTracking) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
        label = "iconTint",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Continuous Tracking Expressive Toggle
        Surface(
            color = surfaceColor,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isTracking,
                        onValueChange = onToggleTracking,
                        role = Role.Switch,
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_my_location),
                        contentDescription = null,
                        tint = iconTint,
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Continuous Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                    )
                }

                val switchIcon: @Composable () -> Unit = if (isTracking) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                }
                Switch(
                    checked = isTracking,
                    onCheckedChange = null,
                    thumbContent = switchIcon,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = coordinates.locationName ?: "Location Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(12.dp))

        mapContent()

        Spacer(modifier = Modifier.height(14.dp))

        // Connected Detail Tiles
        val detailLabelStyle = MaterialTheme.typography.labelLarge
        val detailValueStyle = MaterialTheme.typography.titleMedium.copy(
            fontFamily = GoogleSansFlexRounded,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum",
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Tile 1: Coordinates (Latitude + Longitude with Decimal / DMS tonal switch)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = 5.dp,
                    bottomEnd = 5.dp,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_explore),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Coordinates",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        MetricModePill(
                            label = coordinateFormat.label,
                            onClick = onToggleCoordinateFormat,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val useDmsFormat = coordinateFormat == CoordinateFormat.DMS
                    val latText = if (useDmsFormat) {
                        coordinates.latitude.formatLatitude()
                    } else {
                        coordinates.latitude.formatDecimalDegrees(decimals = 5, locale = locale)
                    }
                    val lonText = if (useDmsFormat) {
                        coordinates.longitude.formatLongitude()
                    } else {
                        coordinates.longitude.formatDecimalDegrees(decimals = 5, locale = locale)
                    }

                    AnimatedContent(
                        targetState = latText to lonText,
                        transitionSpec = {
                            fadeIn(tween(150)).togetherWith(fadeOut(tween(120)))
                        },
                        label = "CoordinateFormatTransition",
                    ) { (lat, lon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Latitude",
                                    style = detailLabelStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lat,
                                    style = detailValueStyle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            VerticalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Longitude",
                                    style = detailLabelStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lon,
                                    style = detailValueStyle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // Tile 2: Altitude
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_landscape_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Altitude",
                            style = detailLabelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "${coordinates.altitude.printRounded(decimals = 1)} m",
                        style = detailValueStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Tile 3: Time Zone
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(
                    topStart = 5.dp,
                    topEnd = 5.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_schedule),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tz.cityName ?: tz.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            color = if (isCustomTimezone) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (isCustomTimezone) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = onTimeZoneClick),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            ) {
                                Text(
                                    text = tz.utcOffset,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Change time zone",
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }

                    val secondaryTzLine = buildString {
                        if (tz.cityName != null && tz.fullName != tz.utcOffset) {
                            append(tz.fullName)
                        }
                        if (tz.abbreviation != null && tz.abbreviation != tz.utcOffset) {
                            if (isNotEmpty()) append(" · ")
                            append(tz.abbreviation)
                        }
                        if (isCustomTimezone) {
                            if (isNotEmpty()) append(" · ")
                            append("Custom")
                        }
                    }
                    if (secondaryTzLine.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = secondaryTzLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCustomTimezone) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 28.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onOpenLocationList,
                modifier = Modifier.weight(1f),
            ) {
                Text("Location List")
            }
            Button(
                onClick = onEditCoordinates,
                modifier = Modifier.weight(1f),
            ) {
                Text("Edit Coordinates")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationDetailsRomePreview() {
    HeliosTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                LocationDetailsContent(
                    currentTime = ZonedDateTime.of(2026, 7, 15, 14, 30, 0, 0, ZoneId.of("Europe/Rome")),
                    coordinates = Coordinates(41.902782, 12.496366, 37.0, "Rome"),
                    isTracking = false,
                    onToggleTracking = {},
                    coordinateFormat = CoordinateFormat.DECIMAL,
                    onToggleCoordinateFormat = {},
                    isCustomTimezone = false,
                    onTimeZoneClick = {},
                    onOpenLocationList = {},
                    onEditCoordinates = {},
                    mapContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Map Preview", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationDetailsArgentinaDmsPreview() {
    HeliosTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                LocationDetailsContent(
                    currentTime = ZonedDateTime.of(2026, 7, 15, 14, 30, 0, 0, ZoneId.of("America/Argentina/Buenos_Aires")),
                    coordinates = Coordinates(-34.603722, -58.381592, 25.0, "Buenos Aires"),
                    isTracking = true,
                    onToggleTracking = {},
                    coordinateFormat = CoordinateFormat.DMS,
                    onToggleCoordinateFormat = {},
                    isCustomTimezone = true,
                    onTimeZoneClick = {},
                    onOpenLocationList = {},
                    onEditCoordinates = {},
                    mapContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Map Preview", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun EditCoordinatesDecimalPreview() {
    HeliosTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                EditCoordinatesContent(
                    coordinates = Coordinates(41.902782, 12.496366, 37.0, "Rome"),
                    coordinateFormat = CoordinateFormat.DECIMAL,
                    onToggleCoordinateFormat = {},
                    onBack = {},
                    onSave = { _, _ -> },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun EditCoordinatesDmsPreview() {
    HeliosTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                EditCoordinatesContent(
                    coordinates = Coordinates(-34.603722, -58.381592, 25.0, "Buenos Aires"),
                    coordinateFormat = CoordinateFormat.DMS,
                    onToggleCoordinateFormat = {},
                    onBack = {},
                    onSave = { _, _ -> },
                )
            }
        }
    }
}
