package com.ephemeris.helios.ui.composables

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicker(
    time: ZonedDateTime,
    showTimePicker: Boolean,
    onShowTimePickerChange: (Boolean) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
    onTimeChange: (ZonedDateTime) -> Unit,
    context: Context
) {
    if (showTimePicker) {
        val is24Hour = DateFormat.is24HourFormat(context)
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = is24Hour
        )
        var showTimeInput by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { onShowTimePickerChange(false) },
            title = { Text(text = "Select time", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (showTimeInput) {
                        TimeInput(state = timePickerState)
                    } else {
                        TimePicker(state = timePickerState)
                    }

                    // Toggle between dial and keyboard
                    IconButton(
                        onClick = { showTimeInput = !showTimeInput },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            painter = painterResource(id = if (showTimeInput) R.drawable.ic_schedule else R.drawable.ic_keyboard),
                            contentDescription = "Toggle time input mode"
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = time.withHour(timePickerState.hour)
                        .withMinute(timePickerState.minute)
                        .withSecond(0) // Optionally reset seconds

                    onShowTimePickerChange(false)
                    onAutoUpdateChange(false)
                    onTimeChange(newTime)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowTimePickerChange(false) }) { Text("Cancel") }
            }
        )
    }
}