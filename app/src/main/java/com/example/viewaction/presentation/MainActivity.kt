package com.example.viewaction.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.viewaction.domain.GoProMode
import com.example.viewaction.presentation.components.GoProActionBar
import com.example.viewaction.presentation.components.GoProCameraViewfinder
import com.example.viewaction.presentation.components.GoProHeader
import com.example.viewaction.presentation.components.GoProModeSelector
import com.example.viewaction.presentation.components.GoProPresetPill
import com.example.viewaction.presentation.components.GoProScannerDialog
import com.example.viewaction.presentation.components.GoProSettingsDialog
import com.example.viewaction.presentation.components.GoProShutterButton
import com.example.viewaction.presentation.theme.GoProActionTheme
import com.example.viewaction.presentation.theme.GoProColors

class MainActivity : ComponentActivity() {

    private val viewModel: GoProViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoProApp(viewModel = viewModel)
        }
    }
}

@Composable
fun GoProApp(viewModel: GoProViewModel) {
    val state by viewModel.uiState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val context = LocalContext.current

    var showScannerDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Bluetooth Permissions Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startBleScan()
            showScannerDialog = true
        } else {
            Toast.makeText(context, "Permisos de Bluetooth requeridos para conectar la GoPro", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestPermissionsAndScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    GoProActionTheme {
        AppScaffold {
            when {
                state.showViewfinder -> {
                    GoProCameraViewfinder(
                        state = state,
                        onEnableWifiStream = {
                            viewModel.enableWifiPreview()
                            Toast.makeText(context, "📶 Iniciando Stream Wi-Fi GoPro...", Toast.LENGTH_LONG).show()
                        },
                        onCloseViewfinder = { viewModel.closeViewfinder() }
                    )
                }

                showScannerDialog -> {
                    GoProScannerDialog(
                        state = state,
                        discoveredDevices = discoveredDevices,
                        onStartScan = { requestPermissionsAndScan() },
                        onConnectDevice = { address ->
                            viewModel.connectDevice(address)
                            showScannerDialog = false
                        },
                        onDisconnect = { viewModel.disconnectBle() },
                        onClose = { showScannerDialog = false }
                    )
                }

                showSettingsDialog -> {
                    GoProSettingsDialog(
                        state = state,
                        onPresetSelected = { preset ->
                            viewModel.selectPreset(preset)
                            Toast.makeText(context, "Modo GoPro: ${preset.name}", Toast.LENGTH_SHORT).show()
                        },
                        onResolutionSelected = { res, fps ->
                            viewModel.setResolutionAndFps(res, fps)
                        },
                        onLensSelected = { lens ->
                            viewModel.setLens(lens)
                        },
                        onClose = { showSettingsDialog = false }
                    )
                }

                else -> {
                    val columnState = rememberTransformingLazyColumnState()
                    val transformationSpec = rememberTransformationSpec()

                    ScreenScaffold(
                        scrollState = columnState
                    ) { contentPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GoProColors.DarkBackground)
                        ) {
                            TransformingLazyColumn(
                                state = columnState,
                                contentPadding = contentPadding
                            ) {
                                // 1. Header (Nombre, Batería %, Espacio SD)
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .transformedHeight(this, transformationSpec),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GoProHeader(
                                            state = state,
                                            onConnectionClick = { showScannerDialog = true }
                                        )
                                    }
                                }

                                // 2. Selector de Modo (VÍDEO | FOTO | LAPSE)
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .transformedHeight(this, transformationSpec),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GoProModeSelector(
                                            selectedMode = state.mode,
                                            onModeSelected = { viewModel.setMode(it) }
                                        )
                                    }
                                }

                                // 3. Botón Principal Obturador y Temporizador
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .transformedHeight(this, transformationSpec),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GoProShutterButton(
                                            state = state,
                                            onClick = {
                                                viewModel.toggleRecording()
                                                if (state.mode == GoProMode.PHOTO) {
                                                    Toast.makeText(context, "📸 Capturando Foto GoPro!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }

                                // 4. Preset Bar (Nombre del Preset y Resolución)
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .transformedHeight(this, transformationSpec),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GoProPresetPill(
                                            state = state,
                                            onClick = { showSettingsDialog = true }
                                        )
                                    }
                                }

                                // 5. Barra de Acciones (HiLight Tag, Escáner BLE, Vista Previa Live, Ajustes)
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .transformedHeight(this, transformationSpec),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GoProActionBar(
                                            state = state,
                                            onHiLightTag = {
                                                viewModel.addHiLightTag()
                                                Toast.makeText(context, "💛 Tag GoPro Guardado", Toast.LENGTH_SHORT).show()
                                            },
                                            onOpenScanner = { showScannerDialog = true },
                                            onEnableWifiPreview = { viewModel.openViewfinder() },
                                            onOpenSettings = { showSettingsDialog = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun GoProAppPreview() {
    // Preview with dummy state
}
