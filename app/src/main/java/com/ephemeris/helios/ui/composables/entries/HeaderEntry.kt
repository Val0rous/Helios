package com.ephemeris.helios.ui.composables.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HeaderEntry(
    text: String,
    modifier: Modifier = Modifier,
    textVariant: String = "",
    textColor: Color = MaterialTheme.colorScheme.primary,
    icon: Int? = null,
    desc: String = "",
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconModifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    iconSize: Dp = 18.dp,
    textSize: TextUnit = 14.sp,
    spacing: Dp = 6.dp,
) {
    val headerStyle = MaterialTheme.typography.titleSmall.copy(
        fontSize = textSize,
        lineHeight = textSize * 1.25f,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing, horizontalAlignment),
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = desc,
                modifier = iconModifier.size(iconSize),
                tint = iconTint,
            )
        }
        Text(text = text, style = headerStyle)
        if (textVariant != "") TextEntryVariant(textVariant)
    }
}