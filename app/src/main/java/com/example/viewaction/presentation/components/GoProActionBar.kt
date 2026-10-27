package com.example.viewaction.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
fun GoProActionBar(
    state: GoProState,
    onHiLightTag: () -> Unit,
    onOpenScanner: () -> Unit,
    onEnableWifiPreview: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // HiLight Tag Button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(GoProColors.Surface)
                .clickable { onHiLightTag() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "💛",
                    fontSize = 13.sp
                )
                if (state.highlightTagsCount > 0) {
                    Text(
                        text = "${state.highlightTagsCount}",
                        color = GoProColors.Yellow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 1.dp)
                    )
                }
            }
        }

        // BLE Connection Button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (state.connectionState == BleConnectionState.CONNECTED) GoProColors.Blue.copy(alpha = 0.2f)
                    else GoProColors.Surface
                )
                .clickable { onOpenScanner() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌐",
                fontSize = 13.sp
            )
        }

        // Wi-Fi AP / Preview Streaming Button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(GoProColors.Surface)
                .clickable { onEnableWifiPreview() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📶",
                fontSize = 13.sp
            )
        }

        // Settings Button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(GoProColors.Surface)
                .clickable { onOpenSettings() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚙️",
                fontSize = 13.sp
            )
        }
    }
}
