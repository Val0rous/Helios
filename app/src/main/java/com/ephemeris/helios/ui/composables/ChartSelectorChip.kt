package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.utils.Charts

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChartSelectorChip(
    chartType: Charts,
    isSelected: Boolean,
    onSelectedChartTypeChange: (Charts) -> Unit,
    shapes: ToggleButtonShapes,
) {
    val iconModifier = Modifier.size(16.dp)
    OutlinedToggleButton(
        checked = isSelected,
        onCheckedChange = { onSelectedChartTypeChange(chartType) },
        shapes = shapes,
        border = ButtonDefaults.outlinedButtonBorder(),
        colors = ToggleButtonDefaults.colors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp),
    ) {
        Icon(
            painter = painterResource(id = if (isSelected) chartType.filledIcon else chartType.icon),
            contentDescription = null,
            modifier = iconModifier,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(chartType.label),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}