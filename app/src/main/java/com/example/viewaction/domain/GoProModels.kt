package com.example.viewaction.domain

import java.util.Locale

enum class GoProMode(val displayName: String) {
    VIDEO("VIDEO"),
    PHOTO("FOTO"),
    TIMELAPSE("TIMELAPSE")
}

data class GoProPreset(
    val id: Int,
    val name: String,
    val mode: GoProMode,
    val icon: String
)

val GOPRO_PRESETS_LIST = listOf(
    // Video Presets
    GoProPreset(1000, "Estándar", GoProMode.VIDEO, "📹"),
    GoProPreset(1001, "Actividad", GoProMode.VIDEO, "🏃"),
    GoProPreset(1002, "Cinematográfico", GoProMode.VIDEO, "🎬"),
    GoProPreset(1003, "Slo-Mo", GoProMode.VIDEO, "🐢"),

    // Photo Presets
    GoProPreset(2000, "Foto", GoProMode.PHOTO, "📷"),
    GoProPreset(2001, "Ráfaga", GoProMode.PHOTO, "📸"),
    GoProPreset(2002, "Foto Nocturna", GoProMode.PHOTO, "🌙"),

    // Timelapse Presets
    GoProPreset(3000, "TimeWarp", GoProMode.TIMELAPSE, "🌀"),
    GoProPreset(3001, "Estrellas", GoProMode.TIMELAPSE, "✨"),
    GoProPreset(3002, "Pintura de Luz", GoProMode.TIMELAPSE, "🖌️"),
    GoProPreset(3003, "Estelas Luces", GoProMode.TIMELAPSE, "🚗"),
    GoProPreset(3004, "Secuencia Nocturna", GoProMode.TIMELAPSE, "🌌")
)

enum class VideoResolution(val displayName: String, val availableFps: List<String>) {
    RES_5_3K("5.3K", listOf("60", "30", "24")),
    RES_4K("4K", listOf("120", "60", "30", "24")),
    RES_2_7K("2.7K", listOf("240", "120", "60")),
    RES_1080P("1080p", listOf("240", "120", "60", "30"))
}

enum class LensType(val displayName: String) {
    HYPERVIEW("HyperView"),
    SUPERVIEW("SuperView"),
    WIDE("Wide"),
    LINEAR("Linear")
}

enum class BleConnectionState(val statusText: String) {
    DISCONNECTED("Desconectado"),
    SCANNING("Buscando..."),
    CONNECTING("Conectando..."),
    CONNECTED("Conectado"),
    ERROR("Error de conexión")
}

data class GoProDiscoveredDevice(
    val name: String,
    val address: String
)

data class GoProState(
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val deviceName: String = "HERO12 Black",
    val mode: GoProMode = GoProMode.VIDEO,
    val activePreset: GoProPreset = GOPRO_PRESETS_LIST[0],
    val resolution: VideoResolution = VideoResolution.RES_4K,
    val fps: String = "60",
    val lens: LensType = LensType.WIDE,
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val batteryLevel: Int = 85,
    val sdRemainingTime: String = "2h 45m",
    val highlightTagsCount: Int = 0,
    val wifiApStatus: String = "Wi-Fi Desactivado",
    val showViewfinder: Boolean = false,
    val errorMessage: String? = null
) {
    val formattedRecordingTime: String
        get() {
            val hours = recordingSeconds / 3600
            val minutes = (recordingSeconds % 3600) / 60
            val seconds = recordingSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
}
