package com.example.viewaction.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.viewaction.domain.GoProState
import com.example.viewaction.presentation.theme.GoProColors

@Composable
fun GoProPresetPill(
    state: GoProState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GoProColors.Card)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = state.activePreset.icon,
            fontSize = 10.sp
        )

        Text(
            text = "${state.activePreset.name} • ${state.resolution.displayName} ${state.fps}FPS",
            color = GoProColors.Blue,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
