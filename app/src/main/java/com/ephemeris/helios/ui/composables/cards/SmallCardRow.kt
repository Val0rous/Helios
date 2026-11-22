package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
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
    forceSquare: Boolean = false,
    leftShape: Shape? = null,
    rightShape: Shape? = null,
    onLeftClick: (() -> Unit)? = null,
    onRightClick: (() -> Unit)? = null,
) {
    val useIntrinsicHeight = !forceSquare && !leftCircular && !rightCircular && !(leftFullBleed && rightFullBleed)
    val defaultShape = CardDefaults.shape
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
            shape = leftShape ?: defaultShape,
            isCircular = leftCircular,
            fullBleed = leftFullBleed,
            fillHeight = useIntrinsicHeight,
            forceSquare = forceSquare,
            onClick = onLeftClick,
        )
        // Right Card
        SmallCard(
            card = { rightCard() },
            modifier = Modifier.weight(1f),
            disableInnerHorizontalPadding = disableInnerHorizontalPadding,
            shape = rightShape ?: defaultShape,
            isCircular = rightCircular,
            fullBleed = rightFullBleed,
            fillHeight = useIntrinsicHeight,
            forceSquare = forceSquare,
            onClick = onRightClick,
        )
    }
}
