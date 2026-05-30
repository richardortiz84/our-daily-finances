package com.daytimegaming.ourdailyfinances.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .background(
            color = Color(0x1F3F465C), // glass_fill
            shape = RoundedCornerShape(12.dp)
        )
        .border(
            width = 1.dp,
            color = Color(0x1AFFFFFF), // glass_stroke
            shape = RoundedCornerShape(12.dp)
        )
        .padding(16.dp)

    Column(
        modifier = cardModifier,
        content = content
    )
}
