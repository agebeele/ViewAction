package com.example.viewaction.presentation.components

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import com.example.viewaction.domain.GoProState
import com.example.viewaction.presentation.theme.GoProColors

@OptIn(UnstableApi::class)
@Composable
fun GoProCameraViewfinder(
    state: GoProState,
    onEnableWifiStream: () -> Unit,
    onCloseViewfinder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isStreamPlaying by remember { mutableStateOf(false) }
    var streamError by remember { mutableStateOf<String?>(null) }

    // GoPro RTSP Live Stream URL
    val goproRtspUrl = "rtsp://10.5.5.9:8554/live/amba.m3u8"

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isStreamPlaying = isPlaying
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    streamError = "Para vídeo en vivo: Conecta el Wi-Fi del reloj a la red GoPro"
                    isStreamPlaying = false
                }
            })
        }
    }

    DisposableEffect(Unit) {
        val mediaSource = RtspMediaSource.Factory()
            .createMediaSource(MediaItem.fromUri(Uri.parse(goproRtspUrl)))
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GoProColors.DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // Video Stream Viewport
        if (isStreamPlaying) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            // Viewfinder HUD Placeholder / Wi-Fi Activation Guide
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .clip(CircleShape)
                    .border(2.dp, GoProColors.Blue.copy(alpha = 0.5f), CircleShape)
                    .background(GoProColors.Surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "📷 VISTA PREVIA LIVE",
                        color = GoProColors.Blue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = streamError ?: "1. Toca 'Activar Wi-Fi GoPro'\n2. Conecta tu reloj al Wi-Fi 'GoPro HERO12...'",
                        color = GoProColors.TextWhite,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = onEnableWifiStream,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoProColors.Blue,
                            contentColor = GoProColors.DarkBackground
                        ),
                        modifier = Modifier
                            .height(30.dp)
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Activar Wi-Fi GoPro 📶",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Camera Crosshairs HUD Overlays
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(1.dp, GoProColors.TextWhite.copy(alpha = 0.4f), CircleShape)
        )

        // Top Status Header Overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${state.resolution.displayName} • ${state.fps}FPS • ${state.lens.displayName}",
                color = GoProColors.TextWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Close Viewfinder Button Overlay at Bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(GoProColors.Card)
                .clickable { onCloseViewfinder() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✖",
                color = GoProColors.TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
