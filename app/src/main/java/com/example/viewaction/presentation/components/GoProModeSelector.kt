package com.example.viewaction.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.viewaction.domain.GoProMode
import com.example.viewaction.presentation.theme.GoProColors

@Composable
fun GoProModeSelector(
    selectedMode: GoProMode,
    onModeSelected: (GoProMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GoProColors.Surface)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GoProMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) GoProColors.Blue else GoProColors.Surface,
                label = "modeBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) GoProColors.DarkBackground else GoProColors.TextMuted,
                label = "modeText"
            )

            val (modeIcon, modeLabel) = when (mode) {
                GoProMode.VIDEO -> Pair("🎥", "VÍDEO")
                GoProMode.PHOTO -> Pair("📷", "FOTO")
                GoProMode.TIMELAPSE -> Pair("⏱️", "LAPSE")
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(backgroundColor)
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = modeIcon,
                        fontSize = 10.sp
                    )
                    Text(
                        text = modeLabel,
                        color = textColor,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
