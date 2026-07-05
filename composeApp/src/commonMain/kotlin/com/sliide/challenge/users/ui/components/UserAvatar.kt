package com.sliide.challenge.users.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

/**
 * Initials avatar with a per-user gradient derived from the name hash, so the
 * feed gets stable, varied color without avatars from the network.
 */
@Composable
fun UserAvatar(
    name: String,
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val palette = avatarPalettes[name.hashCode().absoluteValue % avatarPalettes.size]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = if (size >= 64.dp) MaterialTheme.typography.headlineSmall
            else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

private val avatarPalettes = listOf(
    listOf(Color(0xFF5C6BC0), Color(0xFF3949AB)),
    listOf(Color(0xFF26A69A), Color(0xFF00796B)),
    listOf(Color(0xFFEF5350), Color(0xFFC62828)),
    listOf(Color(0xFFAB47BC), Color(0xFF7B1FA2)),
    listOf(Color(0xFFFF7043), Color(0xFFD84315)),
    listOf(Color(0xFF42A5F5), Color(0xFF1565C0)),
    listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)),
    listOf(Color(0xFFEC407A), Color(0xFFAD1457)),
)
