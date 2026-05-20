package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.AnalemmaIndicator
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.TextEntry
import com.ephemeris.helios.utils.round
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EquationOfTime(
    currentEotMinutes: Double,
    currentDeclinationDeg: Double,
    currentTime: ZonedDateTime,
    modifier: Modifier = Modifier
) {
    // 1. Determine the status description
    // Positive EoT = Solar Time is ahead of Mean Time (Sundial is Fast)
    // Negative EoT = Solar Time is behind Mean Time (Sundial is Slow)
    val eotFloat = currentEotMinutes.toFloat()
    val absEot = abs(eotFloat)
    // The Analemma curve ranges from ~14.3m slow (Feb) to ~16.4m fast (Nov).
    // This scale properly weights the distribution of the shifts.
    val status = when {
        absEot <= 0.5f -> "Synchronized"       // Within 30 seconds of clock time
        eotFloat > 0 -> when {
            eotFloat >= 14.5f -> "Peak Fast"   // The November extreme
            eotFloat > 9.0f -> "Significantly Fast"
            eotFloat > 3.0f -> "Moderately Fast"
            else -> "Slightly Fast"            // 0.5m to 3.0m
        }
        else -> when {
            eotFloat <= -14.0f -> "Peak Slow"  // The February extreme
            eotFloat < -9.0f -> "Significantly Slow"
            eotFloat < -3.0f -> "Moderately Slow"
            else -> "Slightly Slow"            // -0.5m to -3.0m
        }
    }

    // 2. Format the time string (e.g., "+ 4m 22s")
    val sign = if (currentEotMinutes >= 0) "+" else "-"
    var absMinutes = abs(currentEotMinutes)
    var mins = absMinutes.toInt()
    var secs = ((absMinutes - mins) * 60).roundToInt()

    // Handle edge case where rounding pushes seconds to 60
    if (secs == 60) {
        mins += 1
        secs = 0
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        HeaderEntry(
            text = "Equation of Time",
            icon = R.drawable.ic_nest_clock_farsight_analog_filled // Replace with a clock/time icon
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: The Data
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$sign${mins}m ${secs}s",
                        style = MaterialTheme.typography.headlineSmall,
//                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = "vs. Mean Time",
                    style = MaterialTheme.typography.labelSmall,
//                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // RIGHT SIDE: The Analemma Curve
            AnalemmaIndicator(
                currentEot = currentEotMinutes.toFloat(),
                currentDeclination = currentDeclinationDeg.toFloat(),
                currentTime = currentTime,
                modifier = Modifier.width(48.dp).height(72.dp) // Slightly wider to fit the figure-8
            )
        }
    }
}