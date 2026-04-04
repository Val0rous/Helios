package com.ephemeris.helios.ui.composables

import android.text.format.DateFormat
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.utils.TimeMachineFilter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.ephemeris.helios.R
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.calc.SolarEphemeris
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeMachine(
    time: ZonedDateTime,
    coordinates: Coordinates?,  // Nullable in case location hasn't loaded yet
    isAutoUpdate: Boolean,
    onTimeChange: (ZonedDateTime) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val dayOfWeek = time.format(DateTimeFormatter.ofPattern("EEE", LocalLocale.current.platformLocale))
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val datePart = time.format(dateFormatter)
    val timePart = time.format(timeFormatter)
    val dateString = "$dayOfWeek $datePart".replace(",", "").uppercase()
    val timeString = "$timePart".replace(",", "").uppercase()
//    val dateTime = "$dayOfWeek $datePart \t $timePart".replace(",", "").uppercase()
    var selectedFilterType by remember { mutableStateOf<TimeMachineFilter>(TimeMachineFilter.Day) }

    // Picker states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val textMeasurer = rememberTextMeasurer()
    val latestTime by rememberUpdatedState(time)

    val isDark = isSystemInDarkTheme()

    // Date Picker Dialog
    DatePicker(
        time = time,
        showDatePicker = showDatePicker,
        onShowDatePickerChange = { showDatePicker = it },
        onAutoUpdateChange = onAutoUpdateChange,
        onTimeChange = onTimeChange
    )

    // Time Picker Dialog
    TimePicker(
        time = time,
        showTimePicker = showTimePicker,
        onShowTimePickerChange = {showTimePicker = it },
        onAutoUpdateChange = onAutoUpdateChange,
        onTimeChange = onTimeChange,
        context = context
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow, // was surfaceContainer
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Todo: add row with text for date (left) and time (center), with autoUpdate controls to the right
            //Todo: set the logic for those to work
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp)
                    .height(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.weight(1f)
                ) {
                    TextButton(
                        onClick = { showDatePicker = true },
                    ) {
                        Text(
                            text = dateString,
                            color = MaterialTheme.colorScheme.primary,
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 13.sp,
                            ),
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    TextButton(
                        onClick = { showTimePicker = true },
                    ) {
                        Text(
                            text = timeString,
                            color = MaterialTheme.colorScheme.primary,
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeMachineFilter.entries.forEachIndexed { index, filter ->
                            OutlinedToggleButton(
                                checked = filter == selectedFilterType,
                                onCheckedChange = { selectedFilterType = filter },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    TimeMachineFilter.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                                border = ButtonDefaults.outlinedButtonBorder(),
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(32.dp) // Matching your previous FilterChip size
                            ) {
                                Text(
                                    text = stringResource(id = filter.label),
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
            // Infinite Slider Canvas Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                val sliderBackground = MaterialTheme.colorScheme.surfaceContainerHighest
                val onSurface = MaterialTheme.colorScheme.onSurface
                val primary = MaterialTheme.colorScheme.primary
                val triangleIndicatorTop = MaterialTheme.colorScheme.surfaceContainerLow
                val triangleIndicatorBottom = MaterialTheme.colorScheme.surfaceContainerLow

                // Use bright white for the colored gradients, and standard text color for the flat Year view
                val tickAndTextColor = if (selectedFilterType == TimeMachineFilter.Year) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.White
                }

                val textStyle = TextStyle(
                    color = tickAndTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val scope = rememberCoroutineScope()
                val haptic = LocalHapticFeedback.current
                val decay = rememberSplineBasedDecay<Float>()

                var canvasWidth by remember { mutableFloatStateOf(1f) }
                var timeAtDragStart by remember { mutableStateOf(latestTime) }
                var accumulatedDrag by remember { mutableFloatStateOf(0f) }
                val currentFilter by rememberUpdatedState(selectedFilterType)

                val draggableState = rememberDraggableState { dragAmount ->
                    accumulatedDrag += dragAmount

                    if (currentFilter == TimeMachineFilter.Day) {
                        // Continuous Drag (Day)
                        val pixelsPerMinute = canvasWidth / (24f * 60f)
                        val minutesToMove = (-accumulatedDrag / pixelsPerMinute).toInt()

                        if (minutesToMove != 0) {
                            val newTime = timeAtDragStart.plusMinutes(minutesToMove.toLong())
                            onTimeChange(newTime)

                            // Light haptic only when crossing into a new hour so we don't buzz constantly
                            if (newTime.hour != timeAtDragStart.hour) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }

                            timeAtDragStart = newTime
                            accumulatedDrag += (minutesToMove * pixelsPerMinute)
                        }
                    } else {
                        // Discrete Drag (Hour / Year)
                        val pixelsPerStep = when (currentFilter) {
                            TimeMachineFilter.Hour -> canvasWidth / 60f
                            TimeMachineFilter.Year -> canvasWidth / 365f
                            else -> 1f
                        }

                        val stepsToMove = (-accumulatedDrag / pixelsPerStep).toInt()

                        if (stepsToMove != 0) {
                            val newTime = when (currentFilter) {
                                TimeMachineFilter.Hour -> timeAtDragStart.plusMinutes(stepsToMove.toLong())
                                TimeMachineFilter.Year -> timeAtDragStart.plusDays(stepsToMove.toLong())
                                else -> timeAtDragStart
                            }
                            onTimeChange(newTime)

                            // Light haptic on every distinct step
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                            timeAtDragStart = newTime
                            accumulatedDrag += (stepsToMove * pixelsPerStep)
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .onSizeChanged { canvasWidth = it.width.toFloat() } // Capture width for the drag math
                        // 1. Tap Gestures (Left/Right discrete stepping with Safety Buffer)
                        .pointerInput(selectedFilterType) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val center = size.width / 2f
                                    // Calculate the physical width of the triangle dead zone
                                    val triHeight = 8.dp.toPx()
                                    val triBaseHalf = triHeight * 2f // Base is 4x height, so half is 2x

                                    // Safety Buffer: If the tap is within the center indicator rectangle, do nothing
                                    if (offset.x >= center - triBaseHalf && offset.x <= center + triBaseHalf) {
                                        return@detectTapGestures
                                    }

                                    onAutoUpdateChange(false)   // Pause live updating on interaction

                                    // Check if the tap was on the left half or right half
                                    val isLeftHalf = offset.x < center
                                    val stepMultiplier = if (isLeftHalf) -1L else 1L

                                    // Apply discrete 1-unit step
                                    val newTime = when (selectedFilterType) {
                                        TimeMachineFilter.Hour -> latestTime.plusMinutes(stepMultiplier)
                                        TimeMachineFilter.Day -> latestTime.plusHours(stepMultiplier)
                                        TimeMachineFilter.Year -> latestTime.plusDays(stepMultiplier)
                                    }
                                    onTimeChange(newTime)

                                    // Haptic on successful tap
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                        }
                        // 2. Draggable & Fling Gestures
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            onDragStarted = {
                                onAutoUpdateChange(false)
                                timeAtDragStart = latestTime
                                accumulatedDrag = 0f
                            },
                            onDragStopped = { velocity ->
                                // Physics-based fling deceleration
                                scope.launch {
                                    draggableState.drag { // suspends until the decay animation finishes
                                        var lastValue = 0f
                                        AnimationState(initialValue = 0f, initialVelocity = velocity)
                                            .animateDecay(decay) {
                                                val delta = value - lastValue
                                                lastValue = value
                                                dragBy(delta) // Feeds the animation delta back into rememberDraggableState
                                            }
                                    }
                                }
                            }
                        )
                ) {
                    val w = size.width
                    val h = size.height
                    val center = w / 2f

//                    // Draw Background
//                    drawRect(
////                        color = MaterialColors.Amber300,
//                        color = sliderBackground,
//                        size = size // refers to Canvas size
//    //                    topLeft = Offset(10f, 10f), // Optional: starting point
//    //                    size = Size(width = 50f, height = 50f) // Optional: specific dimensions
//                    )

                    // 3. Determine the visible time range on the canvas
                    val millisPerPixel = when (selectedFilterType) {
                        TimeMachineFilter.Hour -> Duration.ofHours(1).toMillis().toFloat() / w
                        TimeMachineFilter.Day -> Duration.ofHours(24).toMillis().toFloat() / w
                        TimeMachineFilter.Year -> Duration.ofDays(365).toMillis().toFloat() / w
                    }

                    // We can use the raw 'time' parameter here (not latestTime) so the UI redraws correctly
                    // on recomposition when the ViewModel pushes the new state down
                    val halfWidthMillis = (center * millisPerPixel).toLong()
                    val startTime = time.minus(halfWidthMillis, ChronoUnit.MILLIS)
                    val endTime = time.plus(halfWidthMillis, ChronoUnit.MILLIS)

                    // Draw Background Gradient
                    if (coordinates != null && selectedFilterType != TimeMachineFilter.Year) {
                        // Determine the step size based on zoom level to ensure smooth gradients
                        // Hour mode: Anchor every 2 minutes. Day mode: Anchor every 15 minutes.
                        val stepMillis = if (selectedFilterType == TimeMachineFilter.Hour) {
                            Duration.ofMinutes(2).toMillis()
                        } else {
                            Duration.ofMinutes(15).toMillis()
                        }
                        val stops = mutableListOf<Pair<Float, Color>>()

                        // 1. Calculate a "rounded" absolute start time.
                        // This is the magic that stops the jitter. By snapping to an absolute 15-min clock interval,
                        // the color anchors are physically glued to the timeline and move with your finger.
                        val anchorStartMillis = (startTime.toInstant().toEpochMilli() / stepMillis) * stepMillis
                        var currentAnchor = Instant.ofEpochMilli(anchorStartMillis).atZone(time.zone)

                        // Expand bounds slightly to ensure the gradient paints past the visible edges
                        val stopTime = endTime.plus(stepMillis, ChronoUnit.MILLIS)

                        while (!currentAnchor.isAfter(stopTime)) {
                            // Calculate where this specific absolute time falls on the physical screen (0.0 to 1.0)
                            val offsetMillis = Duration.between(startTime, currentAnchor).toMillis()
                            val fraction = offsetMillis.toFloat() / (halfWidthMillis * 2)

                            // Calculate the exact solar altitude at this specific time anchor
                            val pos = SolarEphemeris.calculatePosition(currentAnchor, coordinates)
                            val color = SolarColorMap.getColorForAltitude(pos.altitude)

                            // Compose brushes require strictly ascending fractions
                            if (stops.isEmpty() || fraction > stops.last().first) {
                                stops.add(fraction to color)
                            }

                            currentAnchor = currentAnchor.plus(stepMillis, ChronoUnit.MILLIS)
                        }

                        // Safety check: A gradient needs at least 2 stops to render without crashing
                        if (stops.size >= 2) {
                            drawRect(
                                brush = Brush.horizontalGradient(colorStops = stops.toTypedArray()),
                                size = size
                            )
                        }
                    } else {
                        // Fallback if coordinates aren't loaded yet
                        drawRect(color = sliderBackground, size = size)
                    }

                    // 4. Draw tick marks and labels based on the selected scale
                    val minorTickLen = 4.dp.toPx()
                    val majorTickLen = 8.dp.toPx()

                    when (selectedFilterType) {
                        TimeMachineFilter.Hour -> {
                            var currentTick = startTime.truncatedTo(ChronoUnit.MINUTES)
                            while (!currentTick.isAfter(endTime)) {
                                val offsetMillis = Duration.between(time, currentTick).toMillis()
                                val x = center + (offsetMillis / millisPerPixel)
                                val isMajor = currentTick.minute % 10 == 0

                                // Draw Top and Bottom ticks
                                val tickLen = if (isMajor) majorTickLen else minorTickLen
                                drawLine(
                                    tickAndTextColor,
                                    Offset(x, 0f),
                                    Offset(x, tickLen),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    tickAndTextColor,
                                    Offset(x, h),
                                    Offset(x, h - tickLen),
                                    strokeWidth = 1.dp.toPx()
                                )

                                if (isMajor) {
                                    val timeStr = currentTick
                                        .format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a"))
                                    val textResult = textMeasurer.measure(timeStr, textStyle)
                                    // Centered horizontally on the tick line
                                    drawText(
                                        textLayoutResult = textResult,
                                        topLeft = Offset(x - (textResult.size.width / 2f), tickLen + 2.dp.toPx())
                                    )

                                    // Date on midnight
                                    if (currentTick.hour == 0 && currentTick.minute == 0) {
                                        val dateStr = currentTick.format(
                                            DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                                        )
                                        val dateResult = textMeasurer.measure(dateStr, textStyle)
                                        drawText(
                                            textLayoutResult = dateResult,
                                            topLeft = Offset(
                                                x - (dateResult.size.width / 2f),   // Centered horizontally
                                                tickLen + 2.dp.toPx() + textResult.size.height
                                            )
                                        )
                                    }
                                }
                                currentTick = currentTick.plusMinutes(1)
                            }
                        }

                        TimeMachineFilter.Day -> {
                            var currentTick = startTime.truncatedTo(ChronoUnit.HOURS)
                            while (!currentTick.isAfter(endTime)) {
                                val offsetMillis = Duration.between(time, currentTick).toMillis()
                                val x = center + (offsetMillis / millisPerPixel)

                                drawLine(tickAndTextColor, Offset(x, 0f), Offset(x, majorTickLen), strokeWidth = 1.dp.toPx())
                                drawLine(tickAndTextColor, Offset(x, h), Offset(x, h - majorTickLen), strokeWidth = 1.dp.toPx())

                                val labelInterval = if (is24Hour) 2 else 3
                                if (currentTick.hour % labelInterval == 0) {
                                    val timeStr = currentTick.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h a"))
                                    val textResult = textMeasurer.measure(timeStr, textStyle)
                                    drawText(
                                        textLayoutResult = textResult,
                                        topLeft = Offset(
                                            x - (textResult.size.width / 2f),   // Centered horizontally
                                            (h - textResult.size.height) / 2f   // Centered vertically
                                        )
                                    )
                                }
                                currentTick = currentTick.plusHours(1)
                            }
                        }

                        TimeMachineFilter.Year -> {
                            var currentTick =
                                startTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
                            while (!currentTick.isAfter(endTime)) {
                                val offsetMillis = Duration.between(time, currentTick).toMillis()
                                val x = center + (offsetMillis / millisPerPixel)

                                drawLine(
                                    onSurface,
                                    Offset(x, 0f),
                                    Offset(x, majorTickLen),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    onSurface,
                                    Offset(x, h),
                                    Offset(x, h - majorTickLen),
                                    strokeWidth = 1.dp.toPx()
                                )

                                val monthStr =
                                    currentTick.format(DateTimeFormatter.ofPattern("MMM"))
                                        .uppercase()
                                val textResult = textMeasurer.measure(monthStr, textStyle)
                                drawText(
                                    textLayoutResult = textResult,
                                    topLeft = Offset(
                                        x - (textResult.size.width / 2f),   // Centered horizontally
                                        majorTickLen + 2.dp.toPx()
                                    )
                                )

                                if (currentTick.monthValue == 1) {
                                    val yearStr =
                                        currentTick.format(DateTimeFormatter.ofPattern("yyyy"))
                                    val yearResult = textMeasurer.measure(
                                        yearStr,
                                        textStyle.copy(fontWeight = FontWeight.Bold)
                                    )
                                    drawText(
                                        textLayoutResult = yearResult,
                                        topLeft = Offset(
                                            x - (yearResult.size.width / 2f),   // Centered horizontally
                                            majorTickLen + 2.dp.toPx() + textResult.size.height
                                        )
                                    )
                                }
                                currentTick = currentTick.plusMonths(1)
                            }
                        }
                    }

                    // 5. Draw the Center Indicator Triangles (drawn last so they render on top)
                    val triHeight = 8.dp.toPx()
                    val triBaseHalf = triHeight * 2f

                    val topTriangle = Path().apply {
                        moveTo(center - triBaseHalf, 0f)
                        lineTo(center + triBaseHalf, 0f)
                        lineTo(center, triHeight)
                        close()
                    }
                    drawPath(topTriangle, triangleIndicatorTop)

                    val bottomTriangle = Path().apply {
                        moveTo(center - triBaseHalf, h)
                        lineTo(center + triBaseHalf, h)
                        lineTo(center, h - triHeight)
                        close()
                    }
                    drawPath(bottomTriangle, triangleIndicatorBottom)
                }

                if (!isAutoUpdate) {
                    OutlinedIconButton(
                        onClick = { onAutoUpdateChange(true) },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = IconButtonDefaults.outlinedIconButtonVibrantBorder(true)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_history),
                            contentDescription = "Restore Auto Time Update",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

object SolarColorMap {
    // Flatted Thresholds based on your specifications
    private const val NIGHT = -18.0
    private const val ASTRO = -12.0
    private const val PEAK_ASTRO = (ASTRO + NIGHT) / 2
    private const val NAUTICAL = -6.0
    private const val PEAK_NAUTICAL = (NAUTICAL + ASTRO) / 2
    private const val BLUE_HOUR_LOWER = -6.50
    private const val BLUE_HOUR_UPPER = -3.50
    private const val PEAK_BLUE = (BLUE_HOUR_LOWER + BLUE_HOUR_UPPER) / 2
    private const val PINK_HOUR_LOWER = -3.50
    private const val PINK_HOUR_UPPER = -0.833
    private const val PEAK_PINK = (PINK_HOUR_LOWER + PINK_HOUR_UPPER) / 2
    private const val ALPENGLOW_LOWER = -2.50
    private const val ALPENGLOW_UPPER = 2.50
    private const val PEAK_ALPENGLOW = -1.00    // Indirect red light hitting peaks just before sunrise
//    private const val PEAK_ALPENGLOW = (ALPENGLOW_LOWER + ALPENGLOW_UPPER) / 2

    private const val SUNRISE_SET = -0.833
    private const val GOLDEN_HOUR_LOWER = -0.833
    private const val GOLDEN_HOUR_UPPER = 10.50
    private const val PEAK_GOLDEN = (GOLDEN_HOUR_LOWER + GOLDEN_HOUR_UPPER) / 2
    private const val DAYLIGHT = 10.50
    private const val MIDDAY = 90.0

    fun getColorForAltitude(altitude: Double): Color {
        val alt = altitude.coerceIn(NIGHT, MIDDAY)

        return when {
            alt <= ASTRO -> calculateLerp(alt, NIGHT, ASTRO, MaterialColors.Gray900, MaterialColors.BlueGray900)
            alt <= PEAK_NAUTICAL -> calculateLerp(alt, ASTRO, PEAK_NAUTICAL, MaterialColors.BlueGray900, MaterialColors.BlueGray800)
            alt <= PEAK_BLUE -> calculateLerp(alt, PEAK_NAUTICAL, PEAK_BLUE, MaterialColors.BlueGray800, MaterialColors.Blue700)
            alt <= PEAK_PINK -> calculateLerp(alt, PEAK_BLUE, PEAK_PINK, MaterialColors.Blue700, MaterialColors.Pink500)
            alt <= SUNRISE_SET -> calculateLerp(alt, PEAK_ALPENGLOW, SUNRISE_SET, MaterialColors.Pink500, MaterialColors.Red700)
            alt <= PEAK_GOLDEN -> calculateLerp(alt, SUNRISE_SET, PEAK_GOLDEN, MaterialColors.Red700, MaterialColors.Amber700)
            alt <= DAYLIGHT -> calculateLerp(alt, PEAK_GOLDEN, DAYLIGHT, MaterialColors.Amber700, MaterialColors.Yellow700)
            // Taper the daylight slightly lighter toward true noon to give a visual peak
            else -> calculateLerp(alt, DAYLIGHT, MIDDAY, MaterialColors.Yellow700, MaterialColors.Yellow400)
        }
    }

    private fun calculateLerp(
        currentAlt: Double,
        lowerAlt: Double,
        upperAlt: Double,
        lowerColor: Color,
        upperColor: Color
    ): Color {
        val fraction = ((currentAlt - lowerAlt) / (upperAlt - lowerAlt)).toFloat()
        return lerp(lowerColor, upperColor, fraction)
    }
}