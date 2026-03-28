package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.MaterialColors
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeMachine(
    time: ZonedDateTime,
    isAutoUpdate: Boolean,
    onTimeChange: (ZonedDateTime) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
) {
    val dayOfWeek = time.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val datePart = time.format(dateFormatter)
    val timePart = time.format(timeFormatter)
    val date = "$dayOfWeek $datePart".replace(",", "").uppercase()
    val time = "$timePart".replace(",", "").uppercase()
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
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Todo: add row with text for date (left) and time (center), with autoUpdate controls to the right
            //Todo: set the logic for those to work
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp).height(IntrinsicSize.Min)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    TextButton(
                        onClick = {},
                    ) {
                        Text(
                            text = date,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    TextButton(
                        onClick = {},
                    ) {
                        Text(
                            text = time,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Normal),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!isAutoUpdate) {
                        IconButton(
                            onClick = {}
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_history),
                                contentDescription = "Reset",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_circle_filled),
                            contentDescription = "Auto Time Update",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 16.dp).size(8.dp)
                        )
                    }
                }
            }
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