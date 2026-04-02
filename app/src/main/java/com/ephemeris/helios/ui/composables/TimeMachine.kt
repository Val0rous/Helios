package com.ephemeris.helios.ui.composables

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.ephemeris.helios.R
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeMachine(
    time: ZonedDateTime,
    isAutoUpdate: Boolean,
    onTimeChange: (ZonedDateTime) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    drawRect(
                        color = MaterialColors.Amber300,
                        size = size // refers to Canvas size
    //                    topLeft = Offset(10f, 10f), // Optional: starting point
    //                    size = Size(width = 50f, height = 50f) // Optional: specific dimensions
                    )
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}