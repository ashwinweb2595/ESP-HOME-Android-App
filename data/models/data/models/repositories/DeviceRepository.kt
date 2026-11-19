
package com.ashwin.smartledcontroller.data.repositories

import android.content.Context
import com.ashwin.smartledcontroller.data.models.DeviceProfile
import com.ashwin.smartledcontroller.data.models.HardwareProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class DeviceRepository(context: Context) {

    private val preferences = context.getSharedPreferences(
        "smart_led_devices",
        Context.MODE_PRIVATE
    )

    fun getDevices(): List<DeviceProfile> {
        val raw = preferences.getString("devices", null)
            ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            val devices = mutableListOf<DeviceProfile>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val hardware = item.getJSONObject("hardware")

                devices.add(
                    DeviceProfile(
                        deviceId = item.getString("deviceId"),
                        deviceName = item.getString("deviceName"),
                        hardware = HardwareProfile(
                            chip = hardware.optString("chip", "ESP32"),
                            gpio = hardware.optInt("gpio", 23),
                            ledType = hardware.optString("ledType", "WS2811"),
                            ledCount = hardware.optInt("ledCount", 66),
                            rgbOrder = hardware.optString("rgbOrder", "GRB"),
                            channels = hardware.optInt("channels", 3),
                            defaultBrightness = hardware.optInt("defaultBrightness", 35)
                        )
                    )
                )
            }

            devices
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDevices(devices: List<DeviceProfile>) {
        val array = JSONArray()

        devices.forEach { device ->
            val hardware = JSONObject().apply {
                put("chip", device.hardware.chip)
                put("gpio", device.hardware.gpio)
                put("ledType", device.hardware.ledType)
                put("ledCount", device.hardware.ledCount)
                put("rgbOrder", device.hardware.rgbOrder)
                put("channels", device.hardware.channels)
                put("defaultBrightness", device.hardware.defaultBrightness)
            }

            val item = JSONObject().apply {
                put("deviceId", device.deviceId)
                put("deviceName", device.deviceName)
                put("hardware", hardware)
            }

            array.put(item)
        }

        preferences.edit()
            .putString("devices", array.toString())
            .apply()
    }

    fun addDevice(name: String, hardware: HardwareProfile): DeviceProfile {
        val device = DeviceProfile(
            deviceId = UUID.randomUUID().toString(),
            deviceName = name,
            hardware = hardware
        )

        val updated = getDevices().toMutableList()
        updated.add(device)
        saveDevices(updated)

        return device
    }

    fun updateDevice(device: DeviceProfile) {
        val updated = getDevices().map {
            if (it.deviceId == device.deviceId) device else it
        }
        saveDevices(updated)
    }

    fun deleteDevice(deviceId: String) {
        val updated = getDevices().filterNot {
            it.deviceId == deviceId
        }
        saveDevices(updated)
    }
}
