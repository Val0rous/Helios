package com.ephemeris.helios.ui.composables

import android.icu.util.TimeZone as IcuTimeZone
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ephemeris.helios.ui.theme.GoogleSansFlexRounded
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.formatTimeZone
import com.ephemeris.helios.utils.formatUtcOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

private enum class TimeZonePickerTab(val label: String) {
    ZONES("Time Zones"),
    CUSTOM_OFFSET("Custom Offset"),
}

private data class TimeZoneOption(
    val zoneId: ZoneId,
    val primaryLabel: String,
    val secondaryLabel: String,
    val utcOffset: String,
    val offsetSeconds: Int,
    val searchKey: String,
)

private fun buildCanonicalTimeZoneOptions(
    instant: Instant,
    locale: Locale,
): List<TimeZoneOption> {
    val canonicalIds = buildSet {
        add("UTC")
        for (id in ZoneId.getAvailableZoneIds()) {
            if ('/' !in id) continue
            if (id.startsWith("Etc/") || id.startsWith("SystemV/")) continue
            val canonical = runCatching { IcuTimeZone.getCanonicalID(id) }.getOrNull()
            if (canonical == id) {
                add(id)
            }
        }
    }

    return canonicalIds.mapNotNull { id ->
        val zoneId = runCatching { ZoneId.of(id) }.getOrNull() ?: return@mapNotNull null
        val zoned = instant.atZone(zoneId)
        val formatted = zoned.formatTimeZone(locale)
        val primary = formatted.cityName ?: formatted.fullName
        val secondary = buildString {
            if (formatted.cityName != null) {
                append(formatted.fullName)
            }
            if (formatted.abbreviation != null && formatted.abbreviation != formatted.utcOffset) {
                if (isNotEmpty()) append(" · ")
                append(formatted.abbreviation)
            }
        }
        val asciiOffset = formatted.utcOffset.replace('\u2212', '-')
        val region = id.substringBefore('/', "").replace('_', ' ')
        val searchKey = buildString {
            append(primary)
            append(' ')
            append(region)
            append(' ')
            append(id.replace('_', ' '))
            append(' ')
            append(formatted.fullName)
            append(' ')
            if (formatted.abbreviation != null) append(formatted.abbreviation).append(' ')
            append(formatted.utcOffset)
            append(' ')
            append(asciiOffset)
        }.lowercase(locale)

        TimeZoneOption(
            zoneId = zoneId,
            primaryLabel = primary,
            secondaryLabel = secondary,
            utcOffset = formatted.utcOffset,
            offsetSeconds = zoned.offset.totalSeconds,
            searchKey = searchKey,
        )
    }.sortedWith(compareBy<TimeZoneOption> { it.offsetSeconds }.thenBy { it.primaryLabel })
}

private fun parseCustomOffset(
    isPositive: Boolean,
    hoursText: String,
    minutesText: String,
): ZoneOffset? {
    val hours = hoursText.toIntOrNull() ?: return null
    val minutes = if (minutesText.isBlank()) 0 else minutesText.toIntOrNull() ?: return null
    if (hours !in 0..18) return null
    if (minutes !in 0..59) return null
    if (hours == 18 && minutes > 0) return null
    val totalSeconds = (hours * 3600 + minutes * 60) * if (isPositive) 1 else -1
    return runCatching { ZoneOffset.ofTotalSeconds(totalSeconds) }.getOrNull()
}

@Composable
fun TimeZonePickerDialog(
    currentTime: ZonedDateTime,
    defaultTimezoneId: String?,
    customTimezoneId: String?,
    onSelectTimezone: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        TimeZonePickerDialogContent(
            currentTime = currentTime,
            defaultTimezoneId = defaultTimezoneId,
            customTimezoneId = customTimezoneId,
            onSelectTimezone = { tzId ->
                onSelectTimezone(tzId)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun TimeZonePickerDialogContent(
    currentTime: ZonedDateTime,
    defaultTimezoneId: String?,
    customTimezoneId: String?,
    onSelectTimezone: (String?) -> Unit,
    onDismiss: () -> Unit,
    initialTabOverride: TimeZonePickerTab? = null,
    preloadedOptions: List<TimeZoneOption>? = null,
) {
    val locale = LocalLocale.current.platformLocale
    val instant = remember(currentTime) { currentTime.toInstant() }
    val isCustomActive = !customTimezoneId.isNullOrBlank()

    val defaultZone = remember(defaultTimezoneId) {
        defaultTimezoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
    }
    val defaultFormatted = remember(defaultZone, instant, locale) {
        instant.atZone(defaultZone).formatTimeZone(locale)
    }
    val defaultSummary = remember(defaultFormatted) {
        val name = defaultFormatted.cityName ?: defaultFormatted.fullName
        "$name (${defaultFormatted.utcOffset})"
    }

    val initialTab = initialTabOverride ?: remember(currentTime.zone) {
        if (currentTime.zone is ZoneOffset && currentTime.offset.totalSeconds != 0) {
            TimeZonePickerTab.CUSTOM_OFFSET
        } else {
            TimeZonePickerTab.ZONES
        }
    }
    var selectedTab by remember { mutableStateOf(initialTab) }

    val options by produceState(
        initialValue = preloadedOptions,
        key1 = instant,
        key2 = locale,
    ) {
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                buildCanonicalTimeZoneOptions(instant, locale)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .heightIn(max = 620.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Time Zone",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Default zone info + Reset button when overridden
            if (isCustomActive) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom override active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Default: $defaultSummary",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = GoogleSansFlexRounded,
                                    fontFeatureSettings = "tnum",
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        val resetContentColor = if (isDarkSurface) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        TextButton(
                            onClick = { onSelectTimezone(null) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = resetContentColor,
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }
            } else {
                Text(
                    text = "Location default: $defaultSummary",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = GoogleSansFlexRounded,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-tab segmented switcher
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TimeZonePickerTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable(role = Role.Tab) { selectedTab = tab },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(150)).togetherWith(fadeOut(tween(100)))
                },
                label = "TimeZoneTabTransition",
            ) { tab ->
                when (tab) {
                    TimeZonePickerTab.ZONES -> {
                        TimeZoneListTab(
                            activeZoneId = currentTime.zone,
                            defaultZoneId = defaultZone,
                            options = options,
                            locale = locale,
                            onSelectZone = { zoneId ->
                                if (zoneId == defaultZone) {
                                    onSelectTimezone(null)
                                } else {
                                    onSelectTimezone(zoneId.id)
                                }
                            },
                        )
                    }

                    TimeZonePickerTab.CUSTOM_OFFSET -> {
                        CustomOffsetTab(
                            referenceInstant = instant,
                            initialOffset = currentTime.offset,
                            locale = locale,
                            onApplyOffset = { offset ->
                                onSelectTimezone(offset.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeZoneListTab(
    activeZoneId: ZoneId,
    defaultZoneId: ZoneId,
    options: List<TimeZoneOption>?,
    locale: Locale,
    onSelectZone: (ZoneId) -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val filteredOptions = remember(options, query, locale) {
        val list = options ?: return@remember emptyList()
        val trimmed = query.trim().lowercase(locale)
        if (trimmed.isEmpty()) return@remember list
        val tokens = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        list.filter { option ->
            tokens.all { token -> option.searchKey.contains(token) }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search city, zone, or UTC+2") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (options == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        } else if (filteredOptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No matching time zones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    items = filteredOptions,
                    key = { it.zoneId.id },
                ) { option ->
                    val isSelected = option.zoneId == activeZoneId
                    val isDefault = option.zoneId == defaultZoneId
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(role = Role.Button) { onSelectZone(option.zoneId) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = option.primaryLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (isDefault) {
                                        Text(
                                            text = "Default",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                if (option.secondaryLabel.isNotEmpty()) {
                                    Text(
                                        text = option.secondaryLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ) {
                                Text(
                                    text = option.utcOffset,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomOffsetTab(
    referenceInstant: Instant,
    initialOffset: ZoneOffset,
    locale: Locale,
    onApplyOffset: (ZoneOffset) -> Unit,
) {
    val initialTotalMinutes = initialOffset.totalSeconds / 60
    var isPositive by remember(initialOffset) { mutableStateOf(initialTotalMinutes >= 0) }
    var hoursText by remember(initialOffset) { mutableStateOf((abs(initialTotalMinutes) / 60).toString()) }
    var minutesText by remember(initialOffset) { mutableStateOf((abs(initialTotalMinutes) % 60).toString()) }

    val parsedOffset = parseCustomOffset(isPositive, hoursText, minutesText)
    val isError = parsedOffset == null

    val hoursRegex = remember { Regex("^[0-9]{0,2}$") }
    val minutesRegex = remember { Regex("^[0-9]{0,2}$") }

    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }

    fun applyTotalMinutes(clampedMinutes: Int) {
        val clamped = clampedMinutes.coerceIn(-18 * 60, 18 * 60)
        isPositive = clamped >= 0
        hoursText = (abs(clamped) / 60).toString()
        minutesText = (abs(clamped) % 60).toString()
    }

    fun nudgeMinutes(deltaMinutes: Int) {
        val currentMinutes = (parsedOffset?.totalSeconds ?: 0) / 60
        applyTotalMinutes(currentMinutes + deltaMinutes)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Live Offset & Local Time Preview Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = parsedOffset?.formatUtcOffset() ?: "Invalid Offset",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                Spacer(modifier = Modifier.height(4.dp))
                val previewLocalTime = parsedOffset?.let { offset ->
                    referenceInstant.atZone(offset).format(timeFormatter).replace("\u202F", " ")
                }
                Text(
                    text = if (previewLocalTime != null) {
                        "Local time: $previewLocalTime · Fixed offset (no DST)"
                    } else {
                        "Valid range: UTC\u221218:00 to UTC+18:00"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = GoogleSansFlexRounded,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "UTC Offset",
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(50.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { isPositive = !isPositive },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isPositive) "+" else "\u2212",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = hoursText,
                onValueChange = { if (it.matches(hoursRegex)) hoursText = it },
                label = { Text("Hours") },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                ),
                modifier = Modifier.weight(1f),
            )

            OutlinedTextField(
                value = minutesText,
                onValueChange = { if (it.matches(minutesRegex)) minutesText = it },
                label = { Text("Minutes") },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                ),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick offset step buttons for effortless experimentation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val steps = listOf(
                "\u22121h" to { nudgeMinutes(-60) },
                "\u221215m" to { nudgeMinutes(-15) },
                "UTC+0" to { applyTotalMinutes(0) },
                "+15m" to { nudgeMinutes(15) },
                "+1h" to { nudgeMinutes(60) },
            )
            steps.forEach { (label, action) ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = action),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                            ),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { parsedOffset?.let(onApplyOffset) },
            enabled = !isError,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Apply Offset")
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun TimeZonePickerZonesPreview() {
    val sampleInstant = ZonedDateTime.of(2026, 7, 15, 14, 30, 0, 0, ZoneId.of("Europe/Rome"))
    val sampleOptions = listOf(
        TimeZoneOption(ZoneId.of("America/Argentina/Buenos_Aires"), "Buenos Aires", "Argentina Standard Time", "UTC\u22123", -10800, "buenos aires"),
        TimeZoneOption(ZoneId.of("UTC"), "Coordinated Universal Time", "UTC", "UTC+0", 0, "utc"),
        TimeZoneOption(ZoneId.of("Europe/Rome"), "Rome", "Central European Summer Time · CEST", "UTC+2", 7200, "rome"),
        TimeZoneOption(ZoneId.of("Asia/Tokyo"), "Tokyo", "Japan Standard Time · JST", "UTC+9", 32400, "tokyo"),
    )
    HeliosTheme {
        Box(modifier = Modifier.padding(vertical = 12.dp)) {
            TimeZonePickerDialogContent(
                currentTime = sampleInstant.withZoneSameInstant(ZoneId.of("Asia/Tokyo")),
                defaultTimezoneId = "Europe/Rome",
                customTimezoneId = "Asia/Tokyo",
                onSelectTimezone = {},
                onDismiss = {},
                initialTabOverride = TimeZonePickerTab.ZONES,
                preloadedOptions = sampleOptions,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun TimeZonePickerCustomOffsetPreview() {
    val sampleInstant = ZonedDateTime.of(2026, 7, 15, 14, 30, 0, 0, ZoneOffset.ofHoursMinutes(5, 45))
    HeliosTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(vertical = 12.dp)) {
            TimeZonePickerDialogContent(
                currentTime = sampleInstant,
                defaultTimezoneId = "Europe/Rome",
                customTimezoneId = "+05:45",
                onSelectTimezone = {},
                onDismiss = {},
                initialTabOverride = TimeZonePickerTab.CUSTOM_OFFSET,
                preloadedOptions = emptyList(),
            )
        }
    }
}
