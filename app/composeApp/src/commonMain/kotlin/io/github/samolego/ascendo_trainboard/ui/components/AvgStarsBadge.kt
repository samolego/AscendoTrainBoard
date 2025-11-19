package io.github.samolego.ascendo_trainboard.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AvgStarsBadge(
    modifier: Modifier = Modifier,
    stars: Float,
) {
    Badge(
        modifier = modifier
            .clip(RoundedCornerShape(64.dp)),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text("$stars")
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = Color.Yellow,
            modifier = Modifier.size(16.dp)
        )
    }
}
