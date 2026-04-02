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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextEntry(
    text: String,
    textVariant: String = "",
    icon: Int? = null,
    iconVariant: Int? = null,
    desc: String = "",
    descVariant: String = "",
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconModifier: Modifier = Modifier
) {
    val textStyle = TextStyle(fontSize = (14).sp, fontFamily = FontFamily.Default, color = textColor)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        if (icon != null) Icon(painter = painterResource(id = icon), contentDescription = desc, modifier = iconModifier.size(18.dp), tint = iconTint)
        Text(text = text, style = textStyle)
        if(textVariant != "") TextEntryVariant(textVariant)
    }
}