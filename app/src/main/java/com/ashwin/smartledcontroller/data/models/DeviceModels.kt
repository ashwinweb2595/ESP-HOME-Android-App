package com.ashwin.smartledcontroller.data.models

data class DeviceProfile(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String = "ESP32",
    val connectionType: String = "WIFI_BLE",
    val hardware: HardwareProfile,
    val firmwareVersion: String = "Not detected",
    val isConfigured: Boolean = true,
    val ipAddress: String = ""
)

data class HardwareProfile(
    val chip: String = "ESP32",
    val gpio: Int = 23,
    val ledType: String = "WS2811",
    val ledCount: Int = 66,
    val rgbOrder: String = "GRB",
    val channels: Int = 3,
    val defaultBrightness: Int = 35
)

data class DeviceCapabilities(
    val supportsWifi: Boolean = true,
    val supportsBluetooth: Boolean = true,
    val supportsSegments: Boolean = true,
    val supportsAnimations: Boolean = true,
    val supportsOfflineSchedules: Boolean = true
)

