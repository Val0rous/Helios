package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SmallCardRow(
    leftCard: @Composable () -> Unit,
    rightCard: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    disableInnerHorizontalPadding: Boolean = false,
    leftCircular: Boolean = false,
    rightCircular: Boolean = false,
    leftFullBleed: Boolean = false,
    rightFullBleed: Boolean = false,
) {
    val useIntrinsicHeight = !leftCircular && !rightCircular
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (useIntrinsicHeight) Modifier.height(IntrinsicSize.Min) else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left Card
        SmallCard(
            card = { leftCard() },
            modifier = Modifier.weight(1f),
            disableInnerHorizontalPadding = disableInnerHorizontalPadding,
            isCircular = leftCircular,
            fullBleed = leftFullBleed,
            fillHeight = useIntrinsicHeight,
        )
        // Right Card
        SmallCard(
            card = { rightCard() },
            modifier = Modifier.weight(1f),
            disableInnerHorizontalPadding = disableInnerHorizontalPadding,
            isCircular = rightCircular,
            fullBleed = rightFullBleed,
            fillHeight = useIntrinsicHeight,
        )
    }
}
