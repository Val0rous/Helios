package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.theme.MaterialColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeMachine(
    time: LocalDateTime,
    isAutoUpdate: Boolean,
    onTimeChange: (LocalDateTime) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
) {
    val dayOfWeek = time.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
    val datePart = time.format(dateFormatter)
    val timePart = time.format(timeFormatter)
    val dateTime = "$dayOfWeek $datePart \t $timePart".replace(",", "").uppercase()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant, // was surfaceContainer
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Todo: add row with text for date (left) and time (center), with autoUpdate controls to the right
            //Todo: set the logic for those to work
            Text(
                text = dateTime,
                style = TextStyle(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)/*.size(100.dp)*/
            ) {
                drawRect(
                    color = MaterialColors.Amber300,
                    size = size // refers to Canvas size
//                    topLeft = Offset(10f, 10f), // Optional: starting point
//                    size = Size(width = 50f, height = 50f) // Optional: specific dimensions
                )
            }
        }
    }
}