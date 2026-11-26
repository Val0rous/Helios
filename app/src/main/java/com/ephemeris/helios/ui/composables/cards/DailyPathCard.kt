package com.ephemeris.helios.ui.composables.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import com.ephemeris.helios.ui.composables.ChartSelectorChip
import com.ephemeris.helios.ui.composables.charts.CompactDailyRibbon
import com.ephemeris.helios.ui.composables.charts.DailyChart
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.calc.LocalDayTimeline
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.MoonMetrics
import com.ephemeris.helios.utils.calc.SunMetrics
import com.ephemeris.helios.utils.charts.toDecimalHour
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

const val X_SIZE = 1441 // 1440 items (one sampling per minute)

private val CollapsedRibbonHeight = 20.dp
private val RibbonRowGap = 4.dp
private val RibbonSectionHeight = CollapsedRibbonHeight + RibbonRowGap
private val RibbonCornerRadius = 8.dp
private val ColumnHorizontalPadding = 16.dp
private val ExpandedChartTelemetryGap = 4.dp
private val ExpandHysteresis = 4.dp
private val FastFlingMorphSpan = 32.dp

data class ChartArrays(
    val timeXValues: FloatArray,
    val xDataSets: Map<Charts, FloatArray>,
    val yDataSets: Map<Charts, FloatArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChartArrays

        if (!timeXValues.contentEquals(other.timeXValues)) return false

        // Deep compare the X maps
        if (xDataSets != other.xDataSets) return false
        for ((key, value) in xDataSets) {
            if (!value.contentEquals(other.xDataSets[key])) return false
        }

        // Deep compare the Y maps
        if (yDataSets != other.yDataSets) return false
        for ((key, value) in yDataSets) {
            if (!value.contentEquals(other.yDataSets[key])) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = timeXValues.contentHashCode()
        // Simple hash for the X map arrays
        xDataSets.forEach { (key, value) ->
            result = 31 * result + key.hashCode()
            result = 31 * result + value.contentHashCode()
        }
        // Simple hash for the Y map arrays
        yDataSets.forEach { (key, value) ->
            result = 31 * result + key.hashCode()
            result = 31 * result + value.contentHashCode()

        }
        return result
    }
}

/**
 * Full-screen layout for the Sun and Moon tabs with two independent scroll transitions:
 * 1. The chart transitions into a pinned, rounded-border 1D ribbon once the remaining visible chart
 *    height equals [CollapsedRibbonHeight].
 * 2. Independently, the telemetry row continues scrolling beneath the ribbon and transitions into
 *    the compact 1-line row on `surfaceContainer` once its remaining visible height equals
 *    [CollapsedTelemetryRowHeight].
 */
@Composable
fun CollapsibleDailyPathScaffold(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    currentAltitude: Double,
    currentAzimuth: Double,
    type: Charts,
    chartArrays: ChartArrays?,
    modifier: Modifier = Modifier,
    phase: String = "",
    liveTime: ZonedDateTime = currentTime,
    moonPhase: MoonPhaseAppearance = MoonPhaseAppearance.Full,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedChartType by rememberSaveable { mutableStateOf(type) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val expandedChartHeight = ((maxWidth - ColumnHorizontalPadding * 2) / 2f).coerceAtLeast(120.dp)
        val ribbonHeightPx = remember(density) { with(density) { CollapsedRibbonHeight.toPx() } }
        val ribbonSectionHeightPx = remember(density) { with(density) { RibbonSectionHeight.toPx() } }
        val collapsedRowHeightPx = remember(density) { with(density) { CollapsedTelemetryRowHeight.toPx() } }
        val expandHysteresisPx = remember(density) { with(density) { ExpandHysteresis.toPx() } }
        val fastFlingSpanPx = remember(density) { with(density) { FastFlingMorphSpan.toPx() } }

        var measuredChartCardHeightPx by remember(density, expandedChartHeight) {
            mutableIntStateOf(with(density) { (56.dp + expandedChartHeight).roundToPx() })
        }
        var measuredInlineTotalHeightPx by remember(density, expandedChartHeight) {
            mutableIntStateOf(
                with(density) {
                    (56.dp + expandedChartHeight + ExpandedChartTelemetryGap + ExpandedTelemetryRowHeight)
                        .roundToPx()
                }
            )
        }

        // Trigger 1: remaining chart height == CollapsedRibbonHeight
        val ribbonTriggerCollapsePx = (measuredChartCardHeightPx - ribbonHeightPx).coerceAtLeast(1f)
        val ribbonTriggerExpandPx = (ribbonTriggerCollapsePx - expandHysteresisPx).coerceAtLeast(0f)

        // Trigger 2: remaining telemetry row height below pinned ribbon == CollapsedTelemetryRowHeight
        val rowTriggerCollapsePx =
            (measuredInlineTotalHeightPx - (ribbonSectionHeightPx + collapsedRowHeightPx)).coerceAtLeast(1f)
        val rowTriggerExpandPx = (rowTriggerCollapsePx - expandHysteresisPx).coerceAtLeast(0f)

        var isRibbonCollapsed by rememberSaveable { mutableStateOf(false) }
        var isRowCollapsed by rememberSaveable { mutableStateOf(false) }

        val ribbonAnimatable = remember { Animatable(if (isRibbonCollapsed) 1f else 0f) }
        val rowAnimatable = remember { Animatable(if (isRowCollapsed) 1f else 0f) }

        LaunchedEffect(isRibbonCollapsed) {
            ribbonAnimatable.animateTo(
                targetValue = if (isRibbonCollapsed) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isRibbonCollapsed) 110 else 90,
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        LaunchedEffect(isRowCollapsed) {
            rowAnimatable.animateTo(
                targetValue = if (isRowCollapsed) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isRowCollapsed) 110 else 90,
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        LaunchedEffect(
            listState,
            ribbonTriggerCollapsePx,
            ribbonTriggerExpandPx,
            rowTriggerCollapsePx,
            rowTriggerExpandPx,
            fastFlingSpanPx,
        ) {
            snapshotFlow {
                if (listState.firstVisibleItemIndex > 0) Int.MAX_VALUE
                else listState.firstVisibleItemScrollOffset
            }.collect { scrollPx ->
                val nextRibbon = when {
                    scrollPx >= ribbonTriggerCollapsePx -> true
                    scrollPx < ribbonTriggerExpandPx -> false
                    else -> isRibbonCollapsed
                }
                if (nextRibbon != isRibbonCollapsed) {
                    isRibbonCollapsed = nextRibbon
                }
                if (nextRibbon) {
                    val boost = ((scrollPx - ribbonTriggerCollapsePx) / fastFlingSpanPx).coerceIn(0f, 1f)
                    if (boost >= 1f && ribbonAnimatable.value < 1f) ribbonAnimatable.snapTo(1f)
                } else {
                    val rem = ((scrollPx - (ribbonTriggerCollapsePx - fastFlingSpanPx)) / fastFlingSpanPx)
                        .coerceIn(0f, 1f)
                    if (rem <= 0f && ribbonAnimatable.value > 0f) ribbonAnimatable.snapTo(0f)
                }

                val nextRow = when {
                    scrollPx >= rowTriggerCollapsePx -> true
                    scrollPx < rowTriggerExpandPx -> false
                    else -> isRowCollapsed
                }
                if (nextRow != isRowCollapsed) {
                    isRowCollapsed = nextRow
                }
                if (nextRow) {
                    val boost = ((scrollPx - rowTriggerCollapsePx) / fastFlingSpanPx).coerceIn(0f, 1f)
                    if (boost >= 1f && rowAnimatable.value < 1f) rowAnimatable.snapTo(1f)
                } else {
                    val rem = ((scrollPx - (rowTriggerCollapsePx - fastFlingSpanPx)) / fastFlingSpanPx)
                        .coerceIn(0f, 1f)
                    if (rem <= 0f && rowAnimatable.value > 0f) rowAnimatable.snapTo(0f)
                }
            }
        }

        val ribbonDownwardBoost by remember(ribbonTriggerCollapsePx, fastFlingSpanPx) {
            derivedStateOf {
                if (listState.firstVisibleItemIndex > 0) 1f
                else ((listState.firstVisibleItemScrollOffset - ribbonTriggerCollapsePx) / fastFlingSpanPx)
                    .coerceIn(0f, 1f)
            }
        }
        val ribbonUpwardRemaining by remember(ribbonTriggerCollapsePx, fastFlingSpanPx) {
            derivedStateOf {
                if (listState.firstVisibleItemIndex > 0) 1f
                else ((listState.firstVisibleItemScrollOffset - (ribbonTriggerCollapsePx - fastFlingSpanPx)) / fastFlingSpanPx)
                    .coerceIn(0f, 1f)
            }
        }
        val effectiveRibbonProgress = if (isRibbonCollapsed) {
            maxOf(ribbonAnimatable.value, ribbonDownwardBoost)
        } else {
            minOf(ribbonAnimatable.value, ribbonUpwardRemaining)
        }

        val rowDownwardBoost by remember(rowTriggerCollapsePx, fastFlingSpanPx) {
            derivedStateOf {
                if (listState.firstVisibleItemIndex > 0) 1f
                else ((listState.firstVisibleItemScrollOffset - rowTriggerCollapsePx) / fastFlingSpanPx)
                    .coerceIn(0f, 1f)
            }
        }
        val rowUpwardRemaining by remember(rowTriggerCollapsePx, fastFlingSpanPx) {
            derivedStateOf {
                if (listState.firstVisibleItemIndex > 0) 1f
                else ((listState.firstVisibleItemScrollOffset - (rowTriggerCollapsePx - fastFlingSpanPx)) / fastFlingSpanPx)
                    .coerceIn(0f, 1f)
            }
        }
        val effectiveRowProgress = if (isRowCollapsed) {
            maxOf(rowAnimatable.value, rowDownwardBoost)
        } else {
            minOf(rowAnimatable.value, rowUpwardRemaining)
        }

        val showRibbonOverlay = isRibbonCollapsed || effectiveRibbonProgress > 0.001f
        val showRowOverlay = isRowCollapsed || effectiveRowProgress > 0.001f

        val rowViewportHeightPx by remember(
            measuredInlineTotalHeightPx,
            ribbonSectionHeightPx,
            collapsedRowHeightPx,
        ) {
            derivedStateOf {
                val inlineRowBottomYPx = if (listState.firstVisibleItemIndex > 0) {
                    0f
                } else {
                    (measuredInlineTotalHeightPx - listState.firstVisibleItemScrollOffset).toFloat()
                }
                (inlineRowBottomYPx - ribbonSectionHeightPx).coerceAtLeast(collapsedRowHeightPx)
            }
        }

        val scrollToTop: () -> Unit = {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "daily_path_header") {
                DailyPathCard(
                    currentTime = currentTime,
                    liveTime = liveTime,
                    coordinates = coordinates,
                    currentAltitude = currentAltitude,
                    currentAzimuth = currentAzimuth,
                    phase = phase,
                    type = type,
                    chartArrays = chartArrays,
                    moonPhase = moonPhase,
                    selectedChartType = selectedChartType,
                    onSelectedChartTypeChange = { selectedChartType = it },
                    expandedChartHeight = expandedChartHeight,
                    hideTelemetryRow = showRowOverlay,
                    onChartCardHeightMeasured = { height ->
                        if (height > 0 && height != measuredChartCardHeightPx) {
                            measuredChartCardHeightPx = height
                        }
                    },
                    modifier = Modifier.onSizeChanged { size ->
                        if (size.height > 0 && size.height != measuredInlineTotalHeightPx) {
                            measuredInlineTotalHeightPx = size.height
                        }
                    },
                )
            }
            content()
        }

        // Layer 1: Independent pinned telemetry row overlay directly beneath the ribbon section
        if (showRowOverlay) {
            val body = when (type) {
                is Charts.Sun -> CelestialBodyTelemetry.SUN
                else -> CelestialBodyTelemetry.MOON
            }
            val dividerColor = MaterialTheme.colorScheme.outlineVariant
            val dividerAlpha = effectiveRowProgress * 0.45f
            val telemetryTopCorner = lerpDp(5.dp, 0.dp, effectiveRowProgress)
            val telemetryBottomCorner = lerpDp(12.dp, 0.dp, effectiveRowProgress)
            val tapModifier = if (effectiveRowProgress > 0.9f) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = scrollToTop,
                )
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = RibbonSectionHeight)
                    .fillMaxWidth()
                    .clipToBounds()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .drawWithContent {
                        drawContent()
                        if (dividerAlpha > 0.005f) {
                            val strokePx = 1.dp.toPx()
                            drawLine(
                                color = dividerColor.copy(alpha = dividerAlpha),
                                start = Offset(0f, size.height - strokePx / 2f),
                                end = Offset(size.width, size.height - strokePx / 2f),
                                strokeWidth = strokePx,
                            )
                        }
                    }
                    .then(tapModifier)
                    .layout { measurable, constraints ->
                        val viewportHeight = rowViewportHeightPx.roundToInt().coerceAtLeast(0)
                        val placeable = measurable.measure(
                            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                        )
                        layout(placeable.width, viewportHeight) {
                            placeable.placeRelative(0, viewportHeight - placeable.height)
                        }
                    },
            ) {
                ChartTelemetryRow(
                    body = body,
                    altitude = currentAltitude,
                    azimuth = currentAzimuth,
                    phase = phase,
                    moonPhase = moonPhase,
                    collapseFraction = effectiveRowProgress,
                    drawBottomDivider = false,
                    shape = RoundedCornerShape(
                        topStart = telemetryTopCorner,
                        topEnd = telemetryTopCorner,
                        bottomStart = telemetryBottomCorner,
                        bottomEnd = telemetryBottomCorner,
                    ),
                    modifier = Modifier.padding(horizontal = ColumnHorizontalPadding),
                )
            }
        }

        // Layer 2: Independent pinned 1D ribbon overlay at the very top
        if (showRibbonOverlay) {
            PinnedRibbonOverlay(
                currentTime = currentTime,
                coordinates = coordinates,
                type = type,
                activeChartType = selectedChartType,
                chartArrays = chartArrays,
                progress = effectiveRibbonProgress,
                onClick = scrollToTop,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun PinnedRibbonOverlay(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    type: Charts,
    activeChartType: Charts,
    chartArrays: ChartArrays?,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = progress.coerceIn(0f, 1f)
    val backdropAlpha = (p * 1.4f).coerceIn(0f, 1f)
    val ribbonShape = RoundedCornerShape(RibbonCornerRadius)
    val tapModifier = if (p > 0.9f) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RibbonSectionHeight)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = backdropAlpha))
            .padding(
                start = ColumnHorizontalPadding,
                end = ColumnHorizontalPadding,
                bottom = RibbonRowGap,
            )
            .then(tapModifier),
    ) {
        val ribbonModifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = p }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = ribbonShape,
            )
            .clip(ribbonShape)

        val arrays = chartArrays
        if (arrays != null) {
            val elevationKey = when (type) {
                is Charts.Sun -> Charts.Sun.Daily.Elevation
                else -> Charts.Moon.Daily.Elevation
            }
            val elevationY = arrays.yDataSets[elevationKey] ?: FloatArray(X_SIZE)
            val activeY = arrays.yDataSets[activeChartType] ?: elevationY
            CompactDailyRibbon(
                timeXValues = arrays.timeXValues,
                elevationYValues = elevationY,
                activeYValues = activeY,
                chartType = activeChartType,
                currentHour = currentTime.toDecimalHour(),
                coordinates = coordinates,
                modifier = ribbonModifier,
            )
        } else {
            Spacer(
                modifier = ribbonModifier.background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DailyPathCard(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    currentAltitude: Double,
    currentAzimuth: Double,
    type: Charts,
    chartArrays: ChartArrays?,
    modifier: Modifier = Modifier,
    phase: String = "",
    liveTime: ZonedDateTime = currentTime,
    moonPhase: MoonPhaseAppearance = MoonPhaseAppearance.Full,
    selectedChartType: Charts? = null,
    onSelectedChartTypeChange: ((Charts) -> Unit)? = null,
    expandedChartHeight: Dp? = null,
    hideTelemetryRow: Boolean = false,
    onChartCardHeightMeasured: ((Int) -> Unit)? = null,
) {
    var internalSelectedChartType by rememberSaveable { mutableStateOf(type) }
    val activeChartType = selectedChartType ?: internalSelectedChartType
    val updateChartType: (Charts) -> Unit = onSelectedChartTypeChange ?: { internalSelectedChartType = it }

    val currentHour: Float = currentTime.toDecimalHour()
    val liveHour: Float = liveTime.toDecimalHour()

    val chartModifier = if (expandedChartHeight != null) {
        Modifier
            .fillMaxWidth()
            .height(expandedChartHeight)
    } else {
        Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(ExpandedChartTelemetryGap),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ColumnHorizontalPadding),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    if (size.height > 0) onChartCardHeightMeasured?.invoke(size.height)
                },
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = 5.dp,
                bottomEnd = 5.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column {
                val charts = when (type) {
                    is Charts.Sun -> Charts.Sun.Daily.entries
                    is Charts.Moon -> Charts.Moon.Daily.entries
                    else -> Charts.Moon.Daily.entries // Todo: Charts.Planets.Daily.entries, keeping Moon for now to be able to compile
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    itemsIndexed(charts) { index, chart ->
                        val shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            charts.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                        ChartSelectorChip(
                            chartType = chart,
                            isSelected = chart == activeChartType,
                            onSelectedChartTypeChange = updateChartType,
                            shapes = shapes,
                        )
                    }
                }
                // Show a placeholder or empty box while arrays are generating in the background
                val arrays = chartArrays
                if (arrays == null) {
                    Box(modifier = chartModifier, contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val isTrajectory = activeChartType.javaClass.simpleName.contains("Trajectory")

                    val yValues = when (activeChartType) {
                        is Charts.SunMoonCombo.Daily.Elevation -> arrays.yDataSets[Charts.Sun.Daily.Elevation]
                        is Charts.SunMoonCombo.Daily.Trajectory -> arrays.yDataSets[Charts.Sun.Daily.Trajectory]
                        else -> arrays.yDataSets[activeChartType]
                    } ?: FloatArray(X_SIZE)

                    val xValues = if (isTrajectory) {
                        arrays.xDataSets[activeChartType] ?: FloatArray(X_SIZE)
                    } else {
                        arrays.timeXValues
                    }

                    DailyChart(
                        xValues = xValues,
                        yValues = yValues,
                        currentHour = currentHour,
                        liveHour = liveHour,
                        chartType = activeChartType,
                        currentAzimuth = currentAzimuth.toFloat(),
                        currentAltitude = currentAltitude.toFloat(),
                        coordinates = coordinates,
                        moonPhase = moonPhase,
                        modifier = chartModifier
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 5.dp,
                                    bottomEnd = 5.dp
                                )
                            )
                            .padding(vertical = 0.dp)
                    )
                }
            }
        }
        val body = when (type) {
            is Charts.Sun -> CelestialBodyTelemetry.SUN
            else -> CelestialBodyTelemetry.MOON
        }
        ChartTelemetryRow(
            body = body,
            altitude = currentAltitude,
            azimuth = currentAzimuth,
            phase = phase,
            moonPhase = moonPhase,
            shape = RoundedCornerShape(
                topStart = 5.dp,
                topEnd = 5.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp,
            ),
            modifier = Modifier.graphicsLayer { alpha = if (hideTelemetryRow) 0f else 1f },
        )
    }
}

@Composable
internal fun CustomHorizontalDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth(),
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    )
}

internal fun generateSunData(
    localDate: LocalDate,
    hoursCalc: DoubleArray,
    coordinates: Coordinates,
    zoneId: ZoneId,
    dailyOzone: Double
): Pair<Map<Charts, FloatArray>, Map<Charts, FloatArray>> {
    val timeline = LocalDayTimeline(localDate, zoneId)
    val anchorOffset = timeline.startUtcAnchorOffsetHours
    val elevationCalc = DoubleArray(X_SIZE)
    val azimuthsCalc = FloatArray(X_SIZE)
    val distancesCalc = DoubleArray(X_SIZE)
    val xMap = mutableMapOf<Charts, FloatArray>()
    val yMap = mutableMapOf<Charts, FloatArray>()
    for (i in 0 until X_SIZE) {
        val elapsedHour = timeline.wallClockHourToElapsedHour(hoursCalc[i])
        val position = SolarEphemeris.getPositionAtHour(
            date = localDate,
            decimalHour = elapsedHour,
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
            tzOffsetHours = anchorOffset
        )
        elevationCalc[i] = position.altitude
        azimuthsCalc[i] = position.azimuth.toFloat()
        distancesCalc[i] = position.distanceAu
    }
    val elevation = FloatArray(X_SIZE) { elevationCalc[it].toFloat() }
    val irradiance = FloatArray(X_SIZE)
    val uvIntensity = FloatArray(X_SIZE)
    val illuminance = FloatArray(X_SIZE)
    val shadowRatio = FloatArray(X_SIZE)
    val colorTemp = FloatArray(X_SIZE)
    val airMass = FloatArray(X_SIZE)

    SunMetrics.calculateMetrics(
        sunElevationsDeg = elevationCalc,
        observerAltitudeMeters = coordinates.altitude,
        distancesAu = distancesCalc,
        ozoneDU = dailyOzone,
        horizonAltDeg = coordinates.sunApparentHorizonAlt,
        outIrradiance = irradiance,
        outUvi = uvIntensity,
        outIlluminance = illuminance,
        outShadowRatio = shadowRatio,
        outColorTemp = colorTemp,
        outAirMass = airMass
    )

    // Map the arrays to their specific Sun enum keys
    yMap[Charts.Sun.Daily.Elevation] = elevation
    yMap[Charts.Sun.Daily.Irradiance] = irradiance
    yMap[Charts.Sun.Daily.UvIntensity] = uvIntensity
    yMap[Charts.Sun.Daily.Trajectory] = elevation
    yMap[Charts.Sun.Daily.Illuminance] = illuminance
    yMap[Charts.Sun.Daily.Shadows] = shadowRatio
    yMap[Charts.Sun.Daily.ColorTemperature] = colorTemp
    yMap[Charts.Sun.Daily.AirMass] = airMass

    xMap[Charts.Sun.Daily.Trajectory] = azimuthsCalc

    return xMap to yMap
}

internal fun generateMoonData(
    lunarModel: LunarEphemeris.LunarDayModel,
    localDate: LocalDate,
    hoursCalc: DoubleArray,
    coordinates: Coordinates,
    zoneId: ZoneId,
): Pair<Map<Charts, FloatArray>, Map<Charts, FloatArray>> {
    val elevation = FloatArray(X_SIZE)
    val azimuthsCalc = FloatArray(X_SIZE)
    val distancesKm = DoubleArray(X_SIZE)
    lunarModel.fillDailyCurve(
        hoursCalc = hoursCalc,
        outElevation = elevation,
        outAzimuth = azimuthsCalc,
        outDistancesKm = distancesKm,
    )

    val illuminance = FloatArray(X_SIZE)
    val shadowRatio = FloatArray(X_SIZE)
    val colorTemp = FloatArray(X_SIZE)
    val airMass = FloatArray(X_SIZE)

    MoonMetrics.calculateMetrics(
        date = localDate,
        hoursCalc = hoursCalc,
        zoneId = zoneId,
        moonElevationsDeg = elevation,
        distancesKm = distancesKm,
        observerAltitudeMeters = coordinates.altitude,
        horizonAltDeg = coordinates.moonApparentHorizonAlt,
        outIlluminance = illuminance,
        outShadowRatio = shadowRatio,
        outColorTemp = colorTemp,
        outAirMass = airMass,
    )

    val xMap = mutableMapOf<Charts, FloatArray>()
    val yMap = mutableMapOf<Charts, FloatArray>()

    yMap[Charts.Moon.Daily.Elevation] = elevation
    yMap[Charts.Moon.Daily.Trajectory] = elevation
    yMap[Charts.Moon.Daily.Illuminance] = illuminance
    yMap[Charts.Moon.Daily.Shadows] = shadowRatio
    yMap[Charts.Moon.Daily.ColorTemperature] = colorTemp
    yMap[Charts.Moon.Daily.AirMass] = airMass

    xMap[Charts.Moon.Daily.Trajectory] = azimuthsCalc
    return xMap to yMap
}