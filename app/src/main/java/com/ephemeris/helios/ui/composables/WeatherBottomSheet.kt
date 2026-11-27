package com.ephemeris.helios.ui.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.GoogleSansFlexRounded
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.calc.AtmosphericConditions
import com.ephemeris.helios.utils.calc.CloudType
import com.ephemeris.helios.utils.calc.GroundSurface
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val CLOUD_PRESETS = listOf(
    "Clear" to 0,
    "Partly" to 35,
    "Cloudy" to 70,
    "Overcast" to 100,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherBottomSheet(
    conditions: AtmosphericConditions,
    onConditionsChange: (AtmosphericConditions) -> Unit,
    showBottomSheet: Boolean,
    onShowBottomSheetChange: (Boolean) -> Unit,
) {
    if (showBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = { onShowBottomSheetChange(false) },
            sheetState = sheetState,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        ) {
            val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
            NavigationBackHandler(
                state = navigationState,
                isBackEnabled = true,
                onBackCompleted = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onShowBottomSheetChange(false)
                        }
                    }
                },
            )

            WeatherBottomSheetContent(
                conditions = conditions,
                onConditionsChange = onConditionsChange,
            )
        }
    }
}

@Composable
internal fun WeatherBottomSheetContent(
    conditions: AtmosphericConditions,
    onConditionsChange: (AtmosphericConditions) -> Unit,
) {
    var draft by remember { mutableStateOf(conditions) }
    LaunchedEffect(conditions) {
        draft = conditions
    }

    val commit: (AtmosphericConditions) -> Unit = { updated ->
        draft = updated
        onConditionsChange(updated)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 28.dp),
    ) {
        // Header row: dynamic weather icon + condition summary + Reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                val badgeContainer by animateColorAsState(
                    targetValue = if (draft.isDefault) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    label = "weatherHeaderBadgeContainer",
                )
                val badgeContent by animateColorAsState(
                    targetValue = if (draft.isDefault) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    label = "weatherHeaderBadgeContent",
                )
                Surface(
                    color = badgeContainer,
                    contentColor = badgeContent,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = draft.weatherIconFilledRes,
                            transitionSpec = {
                                fadeIn(tween(150)).togetherWith(fadeOut(tween(120)))
                            },
                            label = "weatherHeaderIcon",
                        ) { iconRes ->
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = draft.skyConditionLabel,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.skyConditionLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = if (draft.isDefault) {
                        "Clear sky · Default ground"
                    } else {
                        buildString {
                            if (draft.hasClouds) {
                                append("${draft.clampedCloudCover}% ${draft.cloudType.label.lowercase()} clouds")
                            } else {
                                append("0% clouds")
                            }
                            append(" · ")
                            append(draft.groundSurface.label)
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = GoogleSansFlexRounded,
                            fontFeatureSettings = "tnum",
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AnimatedVisibility(
                visible = !draft.isDefault,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(120)),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) {
                            commit(AtmosphericConditions.DEFAULT)
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset weather and ground",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connected M3 cards matching LocationBottomSheet
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Card 1: Cloud Cover Slider + Quick Presets
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cloud),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cloud Cover",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Surface(
                            color = if (draft.hasClouds) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (draft.hasClouds) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            shape = CircleShape,
                        ) {
                            Text(
                                text = "${draft.clampedCloudCover}%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = GoogleSansFlexRounded,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = draft.clampedCloudCover.toFloat(),
                        onValueChange = { raw ->
                            val pct = raw.roundToInt().coerceIn(0, 100)
                            if (pct != draft.clampedCloudCover) {
                                draft = draft.copy(cloudCoverPercent = pct)
                            }
                        },
                        onValueChangeFinished = {
                            onConditionsChange(draft)
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CLOUD_PRESETS.forEach { (label, presetPct) ->
                            val isSelected = draft.clampedCloudCover == presetPct
                            val chipContainer by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                label = "cloudPresetContainer",
                            )
                            val chipContent by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                label = "cloudPresetContent",
                            )
                            Surface(
                                color = chipContainer,
                                contentColor = chipContent,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) {
                                        commit(draft.copy(cloudCoverPercent = presetPct))
                                    },
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Cloud Layer / Thickness
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_filter_drama),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cloud Density",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            text = draft.cloudType.layerLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (draft.hasClouds) 1f else 0.55f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CloudType.entries.forEach { type ->
                            val isSelected = draft.cloudType == type
                            val itemContainer by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                label = "cloudTypeContainer",
                            )
                            val itemContent by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                label = "cloudTypeContent",
                            )
                            val shortLayer = when (type) {
                                CloudType.HIGH_THIN -> "High"
                                CloudType.MID_MODERATE -> "Mid"
                                CloudType.LOW_THICK -> "Low"
                                CloudType.STORM_HEAVY -> "Storm"
                            }
                            Surface(
                                color = itemContainer,
                                contentColor = itemContent,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(role = Role.Button) {
                                        commit(draft.copy(cloudType = type))
                                    },
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                ) {
                                    Text(
                                        text = type.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = shortLayer,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = itemContent.copy(alpha = 0.78f),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (draft.hasClouds) {
                            draft.cloudType.description
                        } else {
                            "Applies when cloud cover is above 0%"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Card 3: Ground Surface / Albedo
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ground Surface",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        val albedoTag = when (draft.groundSurface) {
                            GroundSurface.DEFAULT -> "No modifier"
                            GroundSurface.WATER -> "Fresnel 6–36%"
                            else -> "Albedo ${(draft.groundSurface.shortwaveAlbedo * 100).roundToInt()}%"
                        }
                        Text(
                            text = albedoTag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = GoogleSansFlexRounded,
                                fontFeatureSettings = "tnum",
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val surfaceRows = remember { GroundSurface.entries.chunked(3) }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        surfaceRows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                rowItems.forEach { surface ->
                                    val isSelected = draft.groundSurface == surface
                                    val tileContainer by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        },
                                        label = "groundTileContainer",
                                    )
                                    val tileContent by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        label = "groundTileContent",
                                    )
                                    Surface(
                                        color = tileContainer,
                                        contentColor = tileContent,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(role = Role.Button) {
                                                commit(draft.copy(groundSurface = surface))
                                            },
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        ) {
                                            GroundSurfaceIcon(
                                                surface = surface,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = surface.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = draft.groundSurface.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live optical impact summary at nominal high-sun (60° elevation, sin = 0.866)
        OpticalImpactSummaryRow(conditions = draft)
    }
}

@Composable
private fun GroundSurfaceIcon(
    surface: GroundSurface,
    modifier: Modifier = Modifier,
) {
    when (surface) {
        GroundSurface.DEFAULT -> Icon(
            painter = painterResource(id = R.drawable.ic_landscape_filled),
            contentDescription = null,
            modifier = modifier,
        )
        GroundSurface.WATER -> Icon(
            painter = painterResource(id = R.drawable.ic_airwave),
            contentDescription = null,
            modifier = modifier,
        )
        GroundSurface.GRASS -> Icon(
            painter = painterResource(id = R.drawable.ic_eco),
            contentDescription = null,
            modifier = modifier,
        )
        GroundSurface.CONCRETE -> Icon(
            imageVector = Icons.Rounded.Domain,
            contentDescription = null,
            modifier = modifier,
        )
        GroundSurface.BEACH -> Icon(
            painter = painterResource(id = R.drawable.ic_beach_access),
            contentDescription = null,
            modifier = modifier,
        )
        GroundSurface.SNOW -> Icon(
            painter = painterResource(id = R.drawable.ic_snowflake),
            contentDescription = null,
            modifier = modifier,
        )
    }
}

@Composable
private fun OpticalImpactSummaryRow(
    conditions: AtmosphericConditions,
) {
    val refSinElev = 0.8660254 // 60° reference elevation
    val beamPct = (conditions.directBeamTransmittance() * 100.0).roundToInt()
    val swCloudT = conditions.globalSwTransmittance()
    val swTotalFactor = swCloudT + conditions.groundSwExtraDiffuse(swCloudT, refSinElev)
    val globalPct = (swTotalFactor * 100.0).roundToInt()
    val uvFactor = conditions.uvCloudModificationFactor() * conditions.uvSurfaceMultiplier(refSinElev)
    val uvPct = (uvFactor * 100.0).roundToInt()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImpactStatItem(label = "Direct Beam", value = "$beamPct%")
            ImpactStatItem(label = "Global Light", value = "$globalPct%")
            ImpactStatItem(label = "UV Index", value = "$uvPct%")
        }
    }
}

@Composable
private fun ImpactStatItem(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = GoogleSansFlexRounded,
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WeatherBottomSheetDefaultPreview() {
    HeliosTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            WeatherBottomSheetContent(
                conditions = AtmosphericConditions.DEFAULT,
                onConditionsChange = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeatherBottomSheetCustomPreview() {
    HeliosTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            WeatherBottomSheetContent(
                conditions = AtmosphericConditions(
                    cloudCoverPercent = 55,
                    cloudType = CloudType.MID_MODERATE,
                    groundSurface = GroundSurface.BEACH,
                ),
                onConditionsChange = {},
            )
        }
    }
}
