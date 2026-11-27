package com.ephemeris.helios.ui.composables.cards

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val X_SIZE = 1441 // 1440 items (one sampling per minute)

private val CollapsedRibbonHeight = 20.dp
private val RibbonCornerRadius = 8.dp
private val ColumnHorizontalPadding = 16.dp
private val ExpandedChartTelemetryGap = 4.dp
private val RibbonHideHysteresis = 4.dp
private val MaxExpandOvershoot = 22.dp
private val MaxCollapseOvershoot = 12.dp
private val CollapseFlingVelocity = 700.dp
private val ExpandFlingVelocity = 500.dp
private val MaxBounceInitialVelocity = 2200.dp
private const val ChartBounceSpringDampingRatio = 0.62f
private const val ChartBounceSpringStiffness = 380f

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
 * Full-screen layout for the Sun and Moon tabs with separate chart & telemetry row collapse:
 * - While dragging, the chart and telemetry row track the user's finger 1:1 with no premature snap.
 * - On release (finger lifted), if the chart is mid-transition:
 *   - Crossing half the chart (excluding the top chips — the horizon line) bounces the chart into
 *     the pinned 1D elevation ribbon while leaving the telemetry row separate.
 *   - Pulling up from the ribbon by at least [CollapsedRibbonHeight] bounces the chart back to
 *     full height.
 * - The telemetry row follows the collapsed chart in sync using its own horizontal transition
 *   without ever eating into the `16.dp` padding above the card list.
 * - Scrolling or flinging up through the card list stops cleanly at the collapsed state and
 *   requires another upward drag from the top to expand.
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
        val layoutDirection = LocalLayoutDirection.current
        val expandedChartHeight = ((maxWidth - ColumnHorizontalPadding * 2) / 2f).coerceAtLeast(120.dp)
        val expandedChartHeightPx = remember(density, expandedChartHeight) {
            with(density) { expandedChartHeight.toPx() }
        }
        val ribbonHeightPx = remember(density) { with(density) { CollapsedRibbonHeight.toPx() } }
        val ribbonHideHysteresisPx = remember(density) {
            with(density) { RibbonHideHysteresis.toPx() }
        }
        val maxExpandOvershootPx = remember(density) { with(density) { MaxExpandOvershoot.toPx() } }
        val maxCollapseOvershootPx = remember(density) { with(density) { MaxCollapseOvershoot.toPx() } }
        val collapseFlingVelocityPx = remember(density) { with(density) { CollapseFlingVelocity.toPx() } }
        val expandFlingVelocityPx = remember(density) { with(density) { ExpandFlingVelocity.toPx() } }
        val maxBounceInitialVelocityPx = remember(density) {
            with(density) { MaxBounceInitialVelocity.toPx() }
        }

        var measuredChipsHeightPx by remember(density) {
            mutableIntStateOf(with(density) { 52.dp.roundToPx() })
        }
        var measuredChartCardHeightPx by remember(density, expandedChartHeight) {
            mutableIntStateOf(with(density) { (52.dp + expandedChartHeight).roundToPx() })
        }

        // Chart travel: full chart card height -> CollapsedRibbonHeight (20.dp)
        val maxChartOffsetPx = (measuredChartCardHeightPx - ribbonHeightPx).coerceAtLeast(1f)
        // Chart collapse threshold: horizon line (top chips excluded, half the chart height)
        val chartCollapseThresholdPx =
            (measuredChipsHeightPx + expandedChartHeightPx * 0.5f).coerceIn(1f, maxChartOffsetPx)
        // Chart expand threshold: scrolling up by one ribbon height from collapsed ribbon
        val chartExpandThresholdPx = (maxChartOffsetPx - ribbonHeightPx).coerceAtLeast(0f)

        var isChartCollapsed by rememberSaveable { mutableStateOf(false) }
        var isRibbonShown by rememberSaveable { mutableStateOf(isChartCollapsed) }

        var chartOffsetPx by remember {
            mutableFloatStateOf(if (isChartCollapsed) maxChartOffsetPx else 0f)
        }
        var chartBounceJob by remember { mutableStateOf<Job?>(null) }
        var chartBounceGen by remember { mutableIntStateOf(0) }
        var isChartBouncing by remember { mutableStateOf(false) }
        // True whenever the current gesture started while the card list was scrolled down, so
        // scrolling/flinging up fast stops at collapsed and requires a fresh drag up to expand.
        var blockExpandUntilNextGesture by remember { mutableStateOf(false) }

        val currentMaxChartOffsetPx by rememberUpdatedState(maxChartOffsetPx)
        val currentRibbonHideHysteresisPx by rememberUpdatedState(ribbonHideHysteresisPx)
        val currentChartCollapseThresholdPx by rememberUpdatedState(chartCollapseThresholdPx)
        val currentChartExpandThresholdPx by rememberUpdatedState(chartExpandThresholdPx)
        val currentCollapseFlingVelocityPx by rememberUpdatedState(collapseFlingVelocityPx)
        val currentExpandFlingVelocityPx by rememberUpdatedState(expandFlingVelocityPx)
        val currentMaxBounceInitialVelocityPx by rememberUpdatedState(maxBounceInitialVelocityPx)

        val updateRibbonVisibility: (Float, Boolean, Boolean) -> Unit = remember {
            { offset, collapsedTarget, bouncing ->
                val maxChart = currentMaxChartOffsetPx
                if (!isRibbonShown) {
                    // Show ribbon as soon as remaining chart height reaches CollapsedRibbonHeight
                    if (offset >= maxChart - 1.5f) {
                        isRibbonShown = true
                    }
                } else {
                    // Keep the ribbon latched through the collapse spring's slight rebound dip
                    // below maxChart so it never disappears and flashes the 2D chart legend.
                    val lockedByCollapseBounce = collapsedTarget && bouncing
                    if (!lockedByCollapseBounce && offset < maxChart - currentRibbonHideHysteresisPx) {
                        isRibbonShown = false
                    }
                }
            }
        }

        val launchChartBounce: (Float, Float) -> Unit = remember(coroutineScope, updateRibbonVisibility) {
            { target, initialVelocity ->
                chartBounceJob?.cancel()
                val gen = ++chartBounceGen
                isChartBouncing = true
                chartBounceJob = coroutineScope.launch {
                    try {
                        val start = chartOffsetPx
                        if (abs(start - target) < 0.5f && abs(initialVelocity) < 1f) {
                            chartOffsetPx = target
                            updateRibbonVisibility(target, isChartCollapsed, false)
                            return@launch
                        }
                        animate(
                            initialValue = start,
                            targetValue = target,
                            initialVelocity = initialVelocity,
                            animationSpec = spring(
                                dampingRatio = ChartBounceSpringDampingRatio,
                                stiffness = ChartBounceSpringStiffness,
                            ),
                        ) { value, _ ->
                            chartOffsetPx = value
                            updateRibbonVisibility(value, isChartCollapsed, true)
                        }
                        chartOffsetPx = target
                        updateRibbonVisibility(target, isChartCollapsed, false)
                    } finally {
                        if (chartBounceGen == gen) {
                            isChartBouncing = false
                        }
                    }
                }
            }
        }

        val settleChartOnRelease: (Float) -> Unit = remember(launchChartBounce, updateRibbonVisibility) {
            { vy ->
                val maxChart = currentMaxChartOffsetPx
                val maxVel = currentMaxBounceInitialVelocityPx
                if (chartOffsetPx > 0.5f && chartOffsetPx < maxChart - 0.5f) {
                    val shouldCollapse = if (!isChartCollapsed) {
                        chartOffsetPx >= currentChartCollapseThresholdPx ||
                            vy < -currentCollapseFlingVelocityPx
                    } else {
                        chartOffsetPx > currentChartExpandThresholdPx &&
                            vy <= currentExpandFlingVelocityPx
                    }
                    isChartCollapsed = shouldCollapse
                    val target = if (shouldCollapse) maxChart else 0f
                    launchChartBounce(target, (-vy).coerceIn(-maxVel, maxVel))
                } else {
                    val collapsed = chartOffsetPx >= maxChart - 0.5f
                    isChartCollapsed = collapsed
                    updateRibbonVisibility(chartOffsetPx, collapsed, false)
                }
            }
        }

        // Keep collapsed chart aligned if layout measurements refine after initial composition
        LaunchedEffect(maxChartOffsetPx) {
            if (isChartCollapsed && !isChartBouncing && !listState.isScrollInProgress) {
                chartOffsetPx = maxChartOffsetPx
                isRibbonShown = true
            }
        }

        // Safety net when gesture ends or is cancelled without firing fling callbacks
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
                if (!scrolling) {
                    blockExpandUntilNextGesture = false
                    if (!isChartBouncing) {
                        settleChartOnRelease(0f)
                    }
                }
            }
        }

        val nestedScrollConnection = remember(
            listState,
            launchChartBounce,
            settleChartOnRelease,
            updateRibbonVisibility,
        ) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val dy = available.y // dy < 0 = scrolling down; dy > 0 = scrolling up
                    if (dy == 0f) return Offset.Zero

                    // Scrolling/flinging while the card list is scrolled down must stop at collapsed
                    if (listState.canScrollBackward) {
                        blockExpandUntilNextGesture = true
                    }

                    if (dy < 0f) {
                        if (source == NestedScrollSource.UserInput && isChartBouncing) {
                            chartBounceJob?.cancel()
                        }

                        val maxChart = currentMaxChartOffsetPx
                        val currentChart = chartOffsetPx.coerceIn(0f, maxChart)
                        if (currentChart < maxChart) {
                            val nextChart = (currentChart - dy).coerceAtMost(maxChart)
                            val consumed = nextChart - currentChart
                            chartOffsetPx = nextChart
                            if (nextChart >= maxChart - 0.5f) {
                                isChartCollapsed = true
                            }
                            updateRibbonVisibility(nextChart, isChartCollapsed, false)
                            return Offset(0f, -consumed)
                        }
                    }

                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (consumed.y > 0f) {
                        blockExpandUntilNextGesture = true
                    }

                    val dy = available.y
                    if (dy <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
                    if (blockExpandUntilNextGesture) return Offset.Zero

                    if (isChartBouncing) {
                        chartBounceJob?.cancel()
                    }

                    val maxChart = currentMaxChartOffsetPx
                    val currentChart = chartOffsetPx.coerceIn(0f, maxChart)
                    if (currentChart > 0f) {
                        val nextChart = (currentChart - dy).coerceAtLeast(0f)
                        val delta = currentChart - nextChart
                        chartOffsetPx = nextChart
                        if (nextChart <= 0.5f) {
                            isChartCollapsed = false
                        }
                        updateRibbonVisibility(nextChart, isChartCollapsed, false)
                        return Offset(0f, delta)
                    }

                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val vy = available.y // vy < 0 = downward scroll flick; vy > 0 = upward scroll flick
                    val maxChart = currentMaxChartOffsetPx
                    val maxVel = currentMaxBounceInitialVelocityPx

                    // Chart mid-transition on finger lift: trigger bounce now that user is not dragging
                    if (chartOffsetPx > 0.5f && chartOffsetPx < maxChart - 0.5f) {
                        if (vy < -currentCollapseFlingVelocityPx) {
                            // Fast downward flick: allow momentum to carry through into the list
                            return Velocity.Zero
                        }
                        settleChartOnRelease(vy)
                        return available
                    }

                    // Fresh upward flick starting from collapsed at the top of the list expands all
                    if (!blockExpandUntilNextGesture &&
                        !listState.canScrollBackward &&
                        vy > currentExpandFlingVelocityPx &&
                        chartOffsetPx > 0.5f
                    ) {
                        isChartCollapsed = false
                        isRibbonShown = false
                        launchChartBounce(0f, (-vy).coerceAtLeast(-maxVel))
                        return available
                    }

                    return Velocity.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity {
                    // Always reset the content-scroll block once a gesture/fling completes so
                    // the next upward drag from the top can expand the header.
                    blockExpandUntilNextGesture = false

                    // If a downward fling ran out of steam mid-chart, bounce it cleanly to rest
                    if (!isChartBouncing) {
                        settleChartOnRelease(0f)
                    }
                    return Velocity.Zero
                }
            }
        }

        val ribbonProgress by animateFloatAsState(
            targetValue = if (isRibbonShown) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (isRibbonShown) 95 else 75,
                easing = FastOutSlowInEasing,
            ),
            label = "ribbonProgress",
        )
        val showRibbonOverlay = isRibbonShown || ribbonProgress > 0.001f

        // Info row follows the collapsed chart in sync while keeping its own horizontal transition
        val isRowTargetCollapsed = isRibbonShown || (isChartCollapsed && isChartBouncing)
        val rowProgress by animateFloatAsState(
            targetValue = if (isRowTargetCollapsed) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (isRowTargetCollapsed) 150 else 130,
                easing = FastOutSlowInEasing,
            ),
            label = "rowProgress",
        )

        val expandOvershootPx = (-chartOffsetPx).coerceIn(0f, maxExpandOvershootPx)
        val collapseOvershootPx = (chartOffsetPx - maxChartOffsetPx).coerceIn(0f, maxCollapseOvershootPx)

        // Keep exact 4.dp gap between chart and row during expand bounce overshoot, and keep
        // height locked at CollapsedRibbonHeight while ribbon is latched through collapse rebound.
        val expandBounceShiftPx = expandOvershootPx * 0.55f
        val effectiveChartOffsetPx = if (isRibbonShown) {
            maxChartOffsetPx
        } else {
            chartOffsetPx.coerceIn(0f, maxChartOffsetPx)
        }
        val visibleChartCardHeightPx = when {
            !isRibbonShown && chartOffsetPx < 0f -> measuredChartCardHeightPx + expandBounceShiftPx
            else -> (measuredChartCardHeightPx - effectiveChartOffsetPx).coerceAtLeast(ribbonHeightPx)
        }

        val expandAndScrollToTop: () -> Unit = {
            blockExpandUntilNextGesture = false
            isChartCollapsed = false
            isRibbonShown = false
            launchChartBounce(0f, 0f)
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
        ) {
            // Persistent header wired to LazyListState so dragging on the chart, ribbon,
            // or telemetry row scrolls seamlessly with the card list.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scrollable(
                        state = listState,
                        orientation = Orientation.Vertical,
                        reverseDirection = ScrollableDefaults.reverseDirection(
                            layoutDirection = layoutDirection,
                            orientation = Orientation.Vertical,
                            reverseScrolling = false,
                        ),
                    ),
            ) {
                // 1. Collapsible Chart Section (measuredChartCardHeight -> CollapsedRibbonHeight)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                            )
                            val viewportHeight = visibleChartCardHeightPx
                                .roundToInt()
                                .coerceIn(0, constraints.maxHeight)
                            layout(placeable.width, viewportHeight) {
                                placeable.placeRelative(0, 0)
                            }
                        },
                ) {
                    // Layer 0: Sliding chart card (stays composed so chart draw caches stay warm)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = if (!isRibbonShown && chartOffsetPx < 0f) {
                                    expandBounceShiftPx
                                } else {
                                    -effectiveChartOffsetPx
                                }
                                alpha = if (ribbonProgress >= 0.999f) 0f else 1f
                            },
                    ) {
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
                            showTelemetryRow = false,
                            onChipsHeightMeasured = { height ->
                                if (height > 0 && height != measuredChipsHeightPx) {
                                    measuredChipsHeightPx = height
                                }
                            },
                            onChartCardHeightMeasured = { height ->
                                if (height > 0 && height != measuredChartCardHeightPx) {
                                    measuredChartCardHeightPx = height
                                }
                            },
                        )
                    }

                    // Layer 1: Pinned 1D elevation ribbon
                    if (showRibbonOverlay) {
                        val ribbonSquashScaleY =
                            1f - (collapseOvershootPx / ribbonHeightPx * 0.10f).coerceAtMost(0.10f)
                        PinnedRibbonOverlay(
                            currentTime = currentTime,
                            coordinates = coordinates,
                            type = type,
                            chartArrays = chartArrays,
                            progress = ribbonProgress,
                            onClick = expandAndScrollToTop,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .graphicsLayer {
                                    scaleY = ribbonSquashScaleY
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                },
                        )
                    }
                }

                // Fixed 4.dp gap between chart/ribbon and telemetry row
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ExpandedChartTelemetryGap)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                )

                // 2. Independent Telemetry Info Row (54.dp expanded -> 38.dp compact)
                val body = when (type) {
                    is Charts.Sun -> CelestialBodyTelemetry.SUN
                    else -> CelestialBodyTelemetry.MOON
                }
                val dividerColor = MaterialTheme.colorScheme.outlineVariant
                val dividerAlpha = rowProgress * 0.45f
                val telemetryTopCorner = lerpDp(5.dp, 0.dp, rowProgress)
                val telemetryBottomCorner = lerpDp(12.dp, 0.dp, rowProgress)
                val tapModifier = if (rowProgress > 0.85f || ribbonProgress > 0.85f) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = expandAndScrollToTop,
                    )
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        .then(tapModifier),
                ) {
                    ChartTelemetryRow(
                        body = body,
                        altitude = currentAltitude,
                        azimuth = currentAzimuth,
                        phase = phase,
                        moonPhase = moonPhase,
                        collapseFraction = rowProgress,
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

            // 3. Card list directly below the telemetry row; its 16.dp top padding is strictly
            //    preserved during chart bounce and row collapse.
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PinnedRibbonOverlay(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    type: Charts,
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
            .height(CollapsedRibbonHeight)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = backdropAlpha))
            .padding(horizontal = ColumnHorizontalPadding)
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
            CompactDailyRibbon(
                timeXValues = arrays.timeXValues,
                elevationYValues = elevationY,
                chartType = elevationKey,
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
    showTelemetryRow: Boolean = true,
    onChipsHeightMeasured: ((Int) -> Unit)? = null,
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
                    modifier = Modifier.onSizeChanged { size ->
                        if (size.height > 0) onChipsHeightMeasured?.invoke(size.height)
                    },
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
        if (showTelemetryRow) {
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
            )
        }
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