package com.example.viewaction.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.example.viewaction.domain.BleConnectionState
import com.example.viewaction.domain.GOPRO_PRESETS_LIST
import com.example.viewaction.domain.GoProDiscoveredDevice
import com.example.viewaction.domain.GoProMode
import com.example.viewaction.domain.GoProPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class GoProBleManager(private val context: Context) {

    companion object {
        private const val TAG = "GoProBleManager"

        // Open GoPro BLE Service & Characteristic UUIDs
        val GOPRO_16BIT_SERVICE_UUID: UUID = UUID.fromString("0000fea6-0000-1000-8000-00805f9b34fb")
        val GOPRO_PRIMARY_SERVICE_UUID: UUID = UUID.fromString("b5f90001-aa8d-11e3-9046-0002a5d5c51b")

        val COMMAND_CHAR_UUID: UUID = UUID.fromString("b5f90072-aa8d-11e3-9046-0002a5d5c51b")
        val COMMAND_RESP_CHAR_UUID: UUID = UUID.fromString("b5f90073-aa8d-11e3-9046-0002a5d5c51b")
        val SETTINGS_CHAR_UUID: UUID = UUID.fromString("b5f90074-aa8d-11e3-9046-0002a5d5c51b")
        val SETTINGS_RESP_CHAR_UUID: UUID = UUID.fromString("b5f90075-aa8d-11e3-9046-0002a5d5c51b")
        val QUERY_CHAR_UUID: UUID = UUID.fromString("b5f90076-aa8d-11e3-9046-0002a5d5c51b")
        val QUERY_RESP_CHAR_UUID: UUID = UUID.fromString("b5f90077-aa8d-11e3-9046-0002a5d5c51b")

        val CLIENT_CHAR_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Open GoPro BLE Commands
        private val CMD_SHUTTER_START = byteArrayOf(0x03, 0x01, 0x01, 0x01)
        private val CMD_SHUTTER_STOP = byteArrayOf(0x03, 0x01, 0x01, 0x00)
        private val CMD_MODE_VIDEO = byteArrayOf(0x03, 0x02, 0x01, 0x00)
        private val CMD_MODE_PHOTO = byteArrayOf(0x03, 0x02, 0x01, 0x01)
        private val CMD_MODE_TIMELAPSE = byteArrayOf(0x03, 0x02, 0x01, 0x02)
        private val CMD_HILIGHT_TAG = byteArrayOf(0x01, 0x18)
        private val CMD_KEEP_ALIVE = byteArrayOf(0x03, 0x00, 0x01, 0x00)
        private val CMD_ENABLE_WIFI_AP = byteArrayOf(0x03, 0x17, 0x01, 0x01)

        // Query Commands: Get All Statuses & Register for live status updates
        private val QUERY_GET_ALL_STATUS = byteArrayOf(0x01, 0x13)
        private val QUERY_REGISTER_STATUS = byteArrayOf(0x07, 0x52, 0x01, 0x0A, 0x01, 0x46, 0x01, 0x0D)
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var commandChar: BluetoothGattCharacteristic? = null
    private var settingsChar: BluetoothGattCharacteristic? = null
    private var queryChar: BluetoothGattCharacteristic? = null

    private val descriptorWriteQueue = ConcurrentLinkedQueue<Pair<BluetoothGatt, BluetoothGattDescriptor>>()
    private var isWritingDescriptor = false

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var keepAliveJob: Job? = null

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<GoProDiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<GoProDiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _batteryLevel = MutableStateFlow(85)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _sdRemainingTime = MutableStateFlow("2h 45m")
    val sdRemainingTime: StateFlow<String> = _sdRemainingTime.asStateFlow()

    private val _currentMode = MutableStateFlow(GoProMode.VIDEO)
    val currentMode: StateFlow<GoProMode> = _currentMode.asStateFlow()

    private val _activePreset = MutableStateFlow(GOPRO_PRESETS_LIST[0])
    val activePreset: StateFlow<GoProPreset> = _activePreset.asStateFlow()

    private val _wifiApStatus = MutableStateFlow("Wi-Fi Desactivado")
    val wifiApStatus: StateFlow<String> = _wifiApStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = try { device.name ?: result.scanRecord?.deviceName } catch (_: SecurityException) { null }

            if (name != null && name.contains("GoPro", ignoreCase = true)) {
                val newDevice = GoProDiscoveredDevice(name, device.address)
                if (_discoveredDevices.value.none { it.address == device.address }) {
                    _discoveredDevices.value += newDevice
                    Log.d(TAG, "Found GoPro Device: $name (${device.address})")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed code: $errorCode")
            _connectionState.value = BleConnectionState.ERROR
            _errorMessage.value = "Error al escanear BLE ($errorCode)"
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _errorMessage.value = "Bluetooth desactivado en el reloj"
            _connectionState.value = BleConnectionState.ERROR
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _errorMessage.value = "Escáner BLE no disponible"
            return
        }

        _discoveredDevices.value = emptyList()
        _connectionState.value = BleConnectionState.SCANNING
        _errorMessage.value = null

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(GOPRO_16BIT_SERVICE_UUID)).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(GOPRO_PRIMARY_SERVICE_UUID)).build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(filters, settings, scanCallback)
            scope.launch {
                delay(12000)
                stopScan()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scan with filters failed, scanning without filter", e)
            try {
                scanner.startScan(null, settings, scanCallback)
            } catch (_: SecurityException) {
                _errorMessage.value = "Sin permisos de Bluetooth"
                _connectionState.value = BleConnectionState.ERROR
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            if (_connectionState.value == BleConnectionState.SCANNING) {
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop BLE scan", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(address: String) {
        val adapter = bluetoothAdapter ?: return
        try {
            stopScan()
            val device = adapter.getRemoteDevice(address)
            _connectionState.value = BleConnectionState.CONNECTING
            _errorMessage.value = null

            if (device.bondState == BluetoothDevice.BOND_NONE) {
                Log.i(TAG, "Initiating Bluetooth bonding with GoPro...")
                device.createBond()
            }

            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (_: SecurityException) {
            _errorMessage.value = "Permisos faltantes para conectar Bluetooth"
            _connectionState.value = BleConnectionState.ERROR
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting device", e)
            _connectionState.value = BleConnectionState.ERROR
            _errorMessage.value = "Falló la conexión: ${e.localizedMessage}"
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            keepAliveJob?.cancel()
            descriptorWriteQueue.clear()
            isWritingDescriptor = false
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            commandChar = null
            settingsChar = null
            queryChar = null
            _connectionState.value = BleConnectionState.DISCONNECTED
            _isRecording.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT Connected. Requesting MTU 512 & discovering services...")
                _connectionState.value = BleConnectionState.CONNECTING
                gatt.requestMtu(512)
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT Disconnected (status $status)")
                disconnect()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "GATT Services Discovered! Queuing notification descriptor writes...")
                var foundCmdChar = false
                descriptorWriteQueue.clear()

                for (service in gatt.services) {
                    val cCmd = service.getCharacteristic(COMMAND_CHAR_UUID)
                    if (cCmd != null) {
                        commandChar = cCmd
                        foundCmdChar = true
                        Log.i(TAG, "Found Command Char: ${cCmd.uuid}")
                    }

                    val cSet = service.getCharacteristic(SETTINGS_CHAR_UUID)
                    if (cSet != null) {
                        settingsChar = cSet
                    }

                    val cQuery = service.getCharacteristic(QUERY_CHAR_UUID)
                    if (cQuery != null) {
                        queryChar = cQuery
                        Log.i(TAG, "Found Query Char: ${cQuery.uuid}")
                    }

                    val cCmdResp = service.getCharacteristic(COMMAND_RESP_CHAR_UUID)
                    if (cCmdResp != null) queueEnableNotification(gatt, cCmdResp)

                    val cSetResp = service.getCharacteristic(SETTINGS_RESP_CHAR_UUID)
                    if (cSetResp != null) queueEnableNotification(gatt, cSetResp)

                    val cQueryResp = service.getCharacteristic(QUERY_RESP_CHAR_UUID)
                    if (cQueryResp != null) queueEnableNotification(gatt, cQueryResp)
                }

                if (foundCmdChar) {
                    processNextDescriptor()
                } else {
                    Log.e(TAG, "GoPro Command Characteristic not found in GATT services!")
                    _errorMessage.value = "Asegúrate que la GoPro está en 'Conectar Dispositivo > Quik App'"
                    disconnect()
                }
            } else {
                Log.e(TAG, "Services discovery failed with status $status")
                _errorMessage.value = "Fallo de servicios ($status)"
                disconnect()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Log.d(TAG, "onDescriptorWrite finished: status $status for char ${descriptor.characteristic.uuid}")
            isWritingDescriptor = false
            if (!descriptorWriteQueue.isEmpty()) {
                processNextDescriptor()
            } else {
                Log.i(TAG, "All CCCD notifications enabled! GoPro connection ACTIVE.")
                _connectionState.value = BleConnectionState.CONNECTED
                _errorMessage.value = null
                onGattReady()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            parseGoProNotification(characteristic.value)
        }
    }

    @SuppressLint("MissingPermission")
    private fun queueEnableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CHAR_CONFIG_UUID) ?: return
        descriptorWriteQueue.add(Pair(gatt, descriptor))
    }

    @SuppressLint("MissingPermission")
    private fun processNextDescriptor() {
        if (isWritingDescriptor) return
        val pair = descriptorWriteQueue.poll() ?: return
        isWritingDescriptor = true
        val (gatt, descriptor) = pair
        @Suppress("DEPRECATION")
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        @Suppress("DEPRECATION")
        gatt.writeDescriptor(descriptor)
    }

    private fun onGattReady() {
        startKeepAlive()
        scope.launch {
            delay(300)
            writeQueryCommand(QUERY_GET_ALL_STATUS)
            delay(400)
            writeQueryCommand(QUERY_REGISTER_STATUS)
        }
    }

    private fun parseGoProNotification(bytes: ByteArray?) {
        if (bytes == null || bytes.size < 3) return
        Log.d(TAG, "GoPro Notification Received: ${bytes.joinToString { "%02X".format(it) }}")

        try {
            var idx = 1 // Skip initial length byte

            val cmdId = bytes[idx++].toInt() and 0xFF

            // Process status query & notification payloads
            if (cmdId == 0x93 || cmdId == 0x92 || cmdId == 0x52 || cmdId == 0x13 || cmdId == 0x33 || cmdId == 0x3A) {
                val statusResult = bytes[idx++].toInt() and 0xFF
                if (statusResult != 0x00 && statusResult != 0x01) return

                while (idx < bytes.size - 1) {
                    val statusId = bytes[idx++].toInt() and 0xFF
                    if (idx >= bytes.size) break
                    val valLen = bytes[idx++].toInt() and 0xFF
                    if (idx + valLen > bytes.size) break

                    when (statusId) {
                        0x0A -> { // Recording State (Status 10 / 0x0A)
                            if (valLen >= 1) {
                                val isRec = (bytes[idx].toInt() and 0xFF) == 1
                                _isRecording.value = isRec
                                Log.i(TAG, "Parsed GoPro Recording: $isRec")
                            }
                        }
                        0x46 -> { // Battery Level % (Status 70 / 0x46)
                            if (valLen >= 1) {
                                val bat = bytes[idx].toInt() and 0xFF
                                if (bat in 0..100) {
                                    _batteryLevel.value = bat
                                    Log.i(TAG, "Parsed GoPro Battery %: $bat%")
                                }
                            }
                        }
                        0x0D -> { // SD Card Video Remaining Time in Seconds (Status 13 / 0x0D)
                            var seconds = 0L
                            for (b in 0 until valLen) {
                                seconds = (seconds shl 8) or (bytes[idx + b].toLong() and 0xFF)
                            }
                            val hours = seconds / 3600
                            val mins = (seconds % 3600) / 60
                            _sdRemainingTime.value = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            Log.i(TAG, "Parsed GoPro SD Remaining Time: ${_sdRemainingTime.value}")
                        }
                        0x02 -> { // Current Flat Mode (Status 2 / 0x02)
                            if (valLen >= 1) {
                                val modeVal = bytes[idx].toInt() and 0xFF
                                _currentMode.value = when (modeVal) {
                                    0 -> GoProMode.VIDEO
                                    1 -> GoProMode.PHOTO
                                    2 -> GoProMode.TIMELAPSE
                                    else -> _currentMode.value
                                }
                            }
                        }
                        0x59 -> { // Active Preset ID (Status 89 / 0x59)
                            if (valLen >= 2) {
                                val presetId = ((bytes[idx].toInt() and 0xFF) shl 8) or (bytes[idx + 1].toInt() and 0xFF)
                                GOPRO_PRESETS_LIST.find { it.id == presetId }?.let { matched ->
                                    _activePreset.value = matched
                                    _currentMode.value = matched.mode
                                }
                            }
                        }
                    }
                    idx += valLen
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GoPro notification", e)
        }
    }

    fun setShutter(enable: Boolean) {
        val command = if (enable) CMD_SHUTTER_START else CMD_SHUTTER_STOP
        writeCommand(command)
        _isRecording.value = enable
    }

    fun setMode(mode: GoProMode) {
        val command = when (mode) {
            GoProMode.VIDEO -> CMD_MODE_VIDEO
            GoProMode.PHOTO -> CMD_MODE_PHOTO
            GoProMode.TIMELAPSE -> CMD_MODE_TIMELAPSE
        }
        writeCommand(command)
        _currentMode.value = mode
    }

    fun loadPreset(preset: GoProPreset) {
        // Open GoPro Load Preset Command: 0x04, 0x3E, 0x02, PresetID_High, PresetID_Low
        val pId = preset.id
        val command = byteArrayOf(
            0x04, 0x3E, 0x02,
            ((pId shr 8) and 0xFF).toByte(),
            (pId and 0xFF).toByte()
        )
        writeCommand(command)
        _activePreset.value = preset
        _currentMode.value = preset.mode
    }

    fun sendHiLightTag() {
        writeCommand(CMD_HILIGHT_TAG)
    }

    fun enableWifiApAndStartStream() {
        writeCommand(CMD_ENABLE_WIFI_AP)
        _wifiApStatus.value = "Wi-Fi GoPro Activado"

        // Trigger GoPro Stream Start HTTP command over background thread
        scope.launch {
            delay(1500)
            try {
                val url = URL("http://10.5.5.9:8080/gopro/camera/stream/start")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                val code = connection.responseCode
                Log.i(TAG, "GoPro Stream Start HTTP Response Code: $code")
                connection.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "HTTP Stream Start attempt failed (connect watch Wi-Fi to GoPro AP)", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeCommand(bytes: ByteArray) {
        val gatt = bluetoothGatt
        val char = commandChar
        if (gatt == null || char == null) {
            Log.w(TAG, "Cannot write command - GATT or Command Characteristic is null")
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    char,
                    bytes,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                @Suppress("DEPRECATION")
                char.value = bytes
                @Suppress("DEPRECATION")
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
            Log.d(TAG, "Sent GoPro BLE command: ${bytes.joinToString { "%02X".format(it) }}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write command to GoPro", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeQueryCommand(bytes: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val char = queryChar ?: commandChar ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                @Suppress("DEPRECATION")
                char.value = bytes
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
            Log.d(TAG, "Sent GoPro Query command: ${bytes.joinToString { "%02X".format(it) }}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write query command", e)
        }
    }

    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (connectionState.value == BleConnectionState.CONNECTED) {
                delay(3500)
                writeCommand(CMD_KEEP_ALIVE)
                delay(500)
                // Periodically query all statuses to refresh battery level & SD card time
                writeQueryCommand(QUERY_GET_ALL_STATUS)
            }
        }
    }
}
