package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
internal fun SmallCard(
    card: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    disableInnerHorizontalPadding: Boolean = false,
    shape: Shape = CardDefaults.shape,
    isCircular: Boolean = false,
    fullBleed: Boolean = false,
    fillHeight: Boolean = true,
) {
    val paddingValue = if (modifier == Modifier) 16.dp else 8.dp
    val horizontalPadding = if (disableInnerHorizontalPadding) 0.dp else 12.dp
    val useBoxLayout = isCircular || fullBleed
    val useSquareAspect = isCircular || (fullBleed && !fillHeight)
    val resolvedShape = when {
        isCircular -> CircleShape
        fullBleed -> RoundedCornerShape(28.dp)
        else -> shape
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = paddingValue)
            .then(if (useSquareAspect) Modifier.aspectRatio(1f) else Modifier),
        shape = resolvedShape,
//        border = CardDefaults.outlinedCardBorder(enabled = false)
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        if (useBoxLayout) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (useSquareAspect || fillHeight) Modifier.fillMaxHeight() else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                card()
            }
        } else {
            Column(
                modifier = Modifier
                    .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
                    .padding(horizontal = horizontalPadding, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                card()
            }
        }
    }
}
