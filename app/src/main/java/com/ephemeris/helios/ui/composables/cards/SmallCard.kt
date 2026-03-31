package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SmallCard(
    card: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paddingValue = if (modifier == Modifier) 16.dp else 8.dp
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = paddingValue),
//        border = CardDefaults.outlinedCardBorder(enabled = false)
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            card()
        }
    }
}