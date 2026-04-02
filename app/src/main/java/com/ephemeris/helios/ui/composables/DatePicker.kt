package com.ephemeris.helios.ui.composables

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Composable
fun DatePicker(
    time: ZonedDateTime,
    showDatePicker: Boolean,
    onShowDatePickerChange: (Boolean) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
    onTimeChange: (ZonedDateTime) -> Unit
) {
    if (showDatePicker) {
        // M3 DatePicker uses UTC milliseconds. Convert current local date to UTC midnight millis.
        val initialDateMillis = time.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

        DatePickerDialog(
            onDismissRequest = { onShowDatePickerChange(false) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        val newTime = time.withYear(selectedDate.year)
                            .withMonth(selectedDate.monthValue)
                            .withDayOfMonth(selectedDate.dayOfMonth)

                        onShowDatePickerChange(false)
                        onAutoUpdateChange(false)
                        onTimeChange(newTime)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDatePickerChange(false) }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}