package com.example.viewaction.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.viewaction.data.GoProBleManager
import com.example.viewaction.domain.BleConnectionState
import com.example.viewaction.domain.GoProDiscoveredDevice
import com.example.viewaction.domain.GoProMode
import com.example.viewaction.domain.GoProPreset
import com.example.viewaction.domain.GoProState
import com.example.viewaction.domain.LensType
import com.example.viewaction.domain.VideoResolution
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GoProViewModel(application: Application) : AndroidViewModel(application) {

    private val bleManager = GoProBleManager(application)

    private val _uiState = MutableStateFlow(
        GoProState(
            connectionState = BleConnectionState.DISCONNECTED
        )
    )
    val uiState: StateFlow<GoProState> = _uiState.asStateFlow()

    val discoveredDevices: StateFlow<List<GoProDiscoveredDevice>> = bleManager.discoveredDevices

    private var recordingTimerJob: Job? = null

    init {
        // Collect BLE state changes
        viewModelScope.launch {
            bleManager.connectionState.collect { connState ->
                _uiState.update { it.copy(connectionState = connState) }
            }
        }

        viewModelScope.launch {
            bleManager.batteryLevel.collect { bat ->
                _uiState.update { it.copy(batteryLevel = bat) }
            }
        }

        viewModelScope.launch {
            bleManager.sdRemainingTime.collect { sdTime ->
                _uiState.update { it.copy(sdRemainingTime = sdTime) }
            }
        }

        viewModelScope.launch {
            bleManager.isRecording.collect { rec ->
                _uiState.update { current ->
                    if (rec != current.isRecording) {
                        if (rec && current.mode != GoProMode.PHOTO) startRecordingTimer() else stopRecordingTimer()
                    }
                    current.copy(
                        isRecording = if (current.mode == GoProMode.PHOTO) false else rec,
                        recordingSeconds = if (rec && current.mode != GoProMode.PHOTO) current.recordingSeconds else 0
                    )
                }
            }
        }

        viewModelScope.launch {
            bleManager.currentMode.collect { mode ->
                _uiState.update { it.copy(mode = mode) }
            }
        }

        viewModelScope.launch {
            bleManager.activePreset.collect { preset ->
                _uiState.update { it.copy(activePreset = preset, mode = preset.mode) }
            }
        }

        viewModelScope.launch {
            bleManager.wifiApStatus.collect { status ->
                _uiState.update { it.copy(wifiApStatus = status) }
            }
        }

        viewModelScope.launch {
            bleManager.errorMessage.collect { msg ->
                if (msg != null) {
                    _uiState.update { it.copy(errorMessage = msg) }
                }
            }
        }
    }

    fun toggleRecording() {
        val currentState = _uiState.value

        if (currentState.mode == GoProMode.PHOTO) {
            // Photo Mode: Snapshot trigger without starting persistent recording timer
            if (currentState.connectionState == BleConnectionState.CONNECTED) {
                bleManager.setShutter(true)
            }
            _uiState.update { it.copy(isRecording = false) }
            return
        }

        val newRecordingState = !currentState.isRecording

        if (currentState.connectionState == BleConnectionState.CONNECTED) {
            bleManager.setShutter(newRecordingState)
        }

        _uiState.update {
            it.copy(
                isRecording = newRecordingState,
                recordingSeconds = if (newRecordingState) 0 else it.recordingSeconds
            )
        }

        if (newRecordingState) {
            startRecordingTimer()
        } else {
            stopRecordingTimer()
        }
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                delay(1000)
                _uiState.update { it.copy(recordingSeconds = it.recordingSeconds + 1) }
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    fun setMode(mode: GoProMode) {
        val currentState = _uiState.value
        if (currentState.isRecording) {
            toggleRecording()
        }

        if (currentState.connectionState == BleConnectionState.CONNECTED) {
            bleManager.setMode(mode)
        }

        _uiState.update { it.copy(mode = mode) }
    }

    fun selectPreset(preset: GoProPreset) {
        if (_uiState.value.connectionState == BleConnectionState.CONNECTED) {
            bleManager.loadPreset(preset)
        }
        _uiState.update { it.copy(activePreset = preset, mode = preset.mode) }
    }

    fun setResolutionAndFps(res: VideoResolution, fps: String) {
        _uiState.update { it.copy(resolution = res, fps = fps) }
    }

    fun setLens(lens: LensType) {
        _uiState.update { it.copy(lens = lens) }
    }

    fun addHiLightTag() {
        if (_uiState.value.connectionState == BleConnectionState.CONNECTED) {
            bleManager.sendHiLightTag()
        }
        _uiState.update { it.copy(highlightTagsCount = it.highlightTagsCount + 1) }
    }

    fun enableWifiPreview() {
        if (_uiState.value.connectionState == BleConnectionState.CONNECTED) {
            bleManager.enableWifiApAndStartStream()
        }
    }

    fun openViewfinder() {
        _uiState.update { it.copy(showViewfinder = true) }
        enableWifiPreview()
    }

    fun closeViewfinder() {
        _uiState.update { it.copy(showViewfinder = false) }
    }

    fun startBleScan() {
        bleManager.startScan()
    }

    fun connectDevice(address: String) {
        bleManager.connectDevice(address)
    }

    fun disconnectBle() {
        bleManager.disconnect()
        _uiState.update { it.copy(connectionState = BleConnectionState.DISCONNECTED) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.disconnect()
    }
}
