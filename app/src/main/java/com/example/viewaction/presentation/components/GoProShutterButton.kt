package com.example.viewaction.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.viewaction.domain.GoProMode
import com.example.viewaction.domain.GoProState
import com.example.viewaction.presentation.theme.GoProColors

@Composable
fun GoProShutterButton(
    state: GoProState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Recording Live Timer Display (if recording)
        if (state.isRecording) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(GoProColors.Red.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(GoProColors.Red)
                )
                Text(
                    text = state.formattedRecordingTime,
                    color = GoProColors.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            // Short Idle Header
            Text(
                text = when (state.mode) {
                    GoProMode.VIDEO -> "MODO VÍDEO"
                    GoProMode.PHOTO -> "MODO FOTO"
                    GoProMode.TIMELAPSE -> "MODO TIMELAPSE"
                },
                color = GoProColors.TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Main Shutter Button
        Box(
            modifier = Modifier
                .size(62.dp)
                .then(
                    if (state.isRecording) Modifier.scale(pulseScale) else Modifier
                )
                .clip(CircleShape)
                .border(
                    width = 3.dp,
                    color = if (state.isRecording) GoProColors.Red else GoProColors.TextWhite,
                    shape = CircleShape
                )
                .background(if (state.isRecording) GoProColors.Red.copy(alpha = 0.15f) else Color.Transparent)
                .clickable { onClick() }
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (state.isRecording) 24.dp else 46.dp)
                    .clip(if (state.isRecording) RoundedCornerShape(6.dp) else CircleShape)
                    .background(GoProColors.Red),
                contentAlignment = Alignment.Center
            ) {
                if (!state.isRecording && state.mode == GoProMode.PHOTO) {
                    Text(
                        text = "📷",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
