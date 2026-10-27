package com.example.viewaction.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.viewaction.domain.BleConnectionState
import com.example.viewaction.domain.GoProDiscoveredDevice
import com.example.viewaction.domain.GoProState
import com.example.viewaction.presentation.theme.GoProColors

@Composable
fun GoProScannerDialog(
    state: GoProState,
    discoveredDevices: List<GoProDiscoveredDevice>,
    onStartScan: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
    onClose: () -> Unit
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoProColors.DarkBackground)
    ) {
        ScreenScaffold(
            scrollState = columnState
        ) { contentPadding ->
            TransformingLazyColumn(
                state = columnState,
                contentPadding = contentPadding
            ) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(
                            text = "Conectar GoPro BLE",
                            color = GoProColors.Blue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Pairing Instructions
                item {
                    Text(
                        text = "En tu GoPro activa:\nConexiones > Conectar Dispositivo > GoPro Quik App",
                        color = GoProColors.TextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Scan / Connect / Disconnect Action Button
                item {
                    Button(
                        onClick = {
                            if (state.connectionState == BleConnectionState.CONNECTED) {
                                onDisconnect()
                            } else {
                                onStartScan()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (state.connectionState) {
                                BleConnectionState.CONNECTED -> GoProColors.Red
                                BleConnectionState.SCANNING -> GoProColors.Surface
                                else -> GoProColors.Blue
                            },
                            contentColor = if (state.connectionState == BleConnectionState.CONNECTED) GoProColors.TextWhite else GoProColors.DarkBackground
                        )
                    ) {
                        Text(
                            text = when (state.connectionState) {
                                BleConnectionState.CONNECTED -> "Desconectar " + state.deviceName
                                BleConnectionState.SCANNING -> "Buscando cámaras..."
                                BleConnectionState.CONNECTING -> "Conectando..."
                                else -> "🔍 Escanear GoPro"
                            },
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Error Notification
                if (state.errorMessage != null) {
                    item {
                        Text(
                            text = state.errorMessage,
                            color = GoProColors.Red,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Discovered GoPro Cameras
                if (discoveredDevices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Cámaras Encontradas:",
                            color = GoProColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }

                    discoveredDevices.forEach { device ->
                        item {
                            Button(
                                onClick = { onConnectDevice(device.address) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec),
                                transformation = SurfaceTransformation(transformationSpec),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoProColors.Surface,
                                    contentColor = GoProColors.Blue
                                )
                            ) {
                                Text(
                                    text = "📷 ${device.name}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Close Button
                item {
                    Button(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoProColors.Card,
                            contentColor = GoProColors.TextWhite
                        )
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}
