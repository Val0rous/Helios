package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.ui.composables.charts.IrradianceDial
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.utils.roundToSignificant
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.entries.TextEntry
import com.ephemeris.helios.utils.printSignificant
import kotlin.math.max

@Composable
fun Irradiance(
    currentIrradiance: Double,
    peakIrradiance: Double
) {
    Column(
    ) {
        HeaderEntry(
            text = stringResource(R.string.irradiance),
            icon = R.drawable.ic_bolt_filled
        )
        // Inside your screen:
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            IrradianceDial(
                currentIrradiance = currentIrradiance.toFloat(),
                maxIrradiance = 950f,
                modifier = Modifier.size(160.dp), // Make it as big as you need!
                strokeWidth = 20.dp
            ) {
                // Because the component exposes a BoxScope,
                // this Column sits perfectly centered inside the ring.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.ic_bolt_filled),
//                        contentDescription = "Current Irradiance",
//                        modifier = Modifier.size(18.dp),
//                        tint = MaterialTheme.colorScheme.primary
//                    )
                    Text(
                        text = currentIrradiance.printSignificant(),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "W/m²",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

//            // Optional: Add the Low/High text at the bottom corners
//            Row(
//                modifier = Modifier
//                    .width(100.dp)
//                    .align(Alignment.BottomCenter)
//                    .offset(y = 1.dp), // Nudge it just below the arc ends
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text("Low", style = MaterialTheme.typography.labelMedium)
//                Text("High", style = MaterialTheme.typography.labelMedium)
//            }
        }
        TextEntry(text = peakIrradiance.printSignificant(), textVariant = "W/m²", icon = R.drawable.ic_bolt, desc = "Peak Irradiance")
    }
}