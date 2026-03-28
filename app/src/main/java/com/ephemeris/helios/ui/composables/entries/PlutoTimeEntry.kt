package com.ephemeris.helios.ui.composables.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.MaterialColors

@Composable
fun PlutoTimeEntry(morningTime: String = "", eveningTime: String = "") {
    val plutoColor = MaterialColors.Red800
    HeaderEntry(text = stringResource(R.string.pluto_times), color = plutoColor)
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextEntryHours(label = "Morning", time = morningTime, color = plutoColor, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        TextEntryHours(label = "Evening", time = eveningTime, color = plutoColor, modifier = Modifier.weight(1f))
    }
    Text(
        text = stringResource(R.string.pluto_time_desc),
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            textAlign = TextAlign.Start
        )
    )
}

// Values are only an estimate. I've never been to Pluto, and you probably haven't either.