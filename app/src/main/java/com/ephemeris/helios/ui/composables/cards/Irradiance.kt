package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val irradianceFloat = currentIrradiance.toFloat()

    val description = when {
        irradianceFloat <= 0.01f -> "None"
        irradianceFloat <= 10f -> "Negligible"
        irradianceFloat <= 100f -> "Marginal"
        irradianceFloat <= 250f -> "Low"
        irradianceFloat <= 500f -> "Moderate"
        irradianceFloat <= 800f -> "High"
        irradianceFloat <= 1000f -> "Excellent"
        else -> "Extreme"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        HeaderEntry(
            text = stringResource(R.string.irradiance),
            textVariant = "(Direct)",
            icon = R.drawable.ic_bolt_filled
        )
        // Inside your screen:
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Because the component exposes a BoxScope,
            // this Column sits perfectly centered inside the ring.
            Column(horizontalAlignment = Alignment.Start) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.ic_bolt_filled),
//                        contentDescription = "Current Irradiance",
//                        modifier = Modifier.size(18.dp),
//                        tint = MaterialTheme.colorScheme.primary
//                    )
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currentIrradiance.printSignificant(),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "W/m²",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IrradianceDial(
                currentIrradiance = irradianceFloat,
                maxIrradiance = 950f,
                modifier = Modifier.size(72.dp), // Make it as big as you need!
                strokeWidth = 16.dp
            ) {}

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
    }
    CustomHorizontalDivider()
    Row(Modifier.padding(horizontal = 12.dp)) {
        TextEntry(text = peakIrradiance.printSignificant(), textVariant = "W/m²", icon = R.drawable.ic_bolt, desc = "Peak Irradiance")
    }
}