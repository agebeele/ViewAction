package com.example.viewaction.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.viewaction.domain.BleConnectionState
import com.example.viewaction.domain.GoProState
import com.example.viewaction.presentation.theme.GoProColors

@Composable
fun GoProHeader(
    state: GoProState,
    onConnectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // GoPro Hero Tag & Connection State
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GoProColors.Card)
                .clickable { onConnectionClick() }
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        when (state.connectionState) {
                            BleConnectionState.CONNECTED -> GoProColors.Green
                            BleConnectionState.SCANNING,
                            BleConnectionState.CONNECTING -> GoProColors.Yellow
                            else -> GoProColors.Red
                        }
                    )
            )

            Text(
                text = if (state.connectionState == BleConnectionState.CONNECTED) state.deviceName else "GoPro Disconnected",
                color = GoProColors.TextWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 5.dp)
            )
        }

        // Battery & SD Storage Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = state.sdRemainingTime,
                color = GoProColors.TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            // Battery Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (state.batteryLevel <= 20) GoProColors.Red.copy(alpha = 0.2f)
                        else GoProColors.Surface
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 11.dp, height = 6.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(GoProColors.Card)
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.batteryLevel / 100f)
                            .height(4.dp)
                            .background(
                                if (state.batteryLevel <= 20) GoProColors.Red else GoProColors.Green
                            )
                    )
                }

                Text(
                    text = "${state.batteryLevel}%",
                    color = GoProColors.TextWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 3.dp)
                )
            }
        }
    }
}
