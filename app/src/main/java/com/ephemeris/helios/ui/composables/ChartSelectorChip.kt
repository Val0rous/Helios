package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.utils.Charts

@Composable
fun ChartSelectorChip(chartType: Charts, isSelected: Boolean, onSelectedChartTypeChange: (Charts) -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = { onSelectedChartTypeChange(chartType) },
        label = { Text(text = stringResource(chartType.label)) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = (if (isSelected) chartType.filledIcon else chartType.icon)),
                contentDescription = "",
                modifier = Modifier.size(18.dp)
            )
        }
    )
}