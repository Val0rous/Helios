package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SmallCardRow(
    leftCard: @Composable () -> Unit,
    rightCard: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 8.dp)
    ) {
        // Left Card
        SmallCard(
            card = { leftCard() },
            modifier = Modifier.weight(1f)
        )
        // Right Card
        SmallCard(
            card = { rightCard() },
            modifier = Modifier.weight(1f)
        )
    }
}