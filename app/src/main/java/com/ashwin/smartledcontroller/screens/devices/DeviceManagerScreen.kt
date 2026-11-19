package com.ashwin.smartledcontroller.screens.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ashwin.smartledcontroller.communication.wifi.Esp32Connection
import com.ashwin.smartledcontroller.data.models.DeviceProfile
import com.ashwin.smartledcontroller.data.models.HardwareProfile
import com.ashwin.smartledcontroller.data.repositories.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeviceManagerScreen() {
    val context = LocalContext.current
    val repository = remember(context) { DeviceRepository(context) }
    val scope = rememberCoroutineScope()

    val devices = remember(repository) {
        mutableStateListOf<DeviceProfile>().apply {
            val saved = repository.getDevices()

            if (saved.isEmpty()) {
                add(
                    repository.addDevice(
                        "Living Room",
                        HardwareProfile()
                    )
                )
            } else {
                addAll(saved)
            }
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<DeviceProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Smart LED Controller",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Manage your ESP32 controllers",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = {
                editingDevice = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add New Controller")
        }

        Text(
            "MY CONTROLLERS (${devices.size})",
            style = MaterialTheme.typography.titleSmall
        )

        devices.forEach { device ->
            DeviceCard(
                device = device,

                onEdit = {
                    editingDevice = device
                    showDialog = true
                },

                onDuplicate = {
                    val copy = repository.addDevice(
                        "${device.deviceName} Copy",
                        device.hardware
                    ).copy(
                        ipAddress = device.ipAddress
                    )

                    repository.updateDevice(copy)
                    devices.add(copy)
                },

                onIpAddressChange = { updated ->
                    repository.updateDevice(updated)

                    val index = devices.indexOfFirst {
                        it.deviceId == updated.deviceId
                    }

                    if (index >= 0) {
                        devices[index] = updated
                    }
                },

                onDelete = {
                    repository.deleteDevice(device.deviceId)
                    devices.remove(device)
                },

                onTestConnection = { ipAddress, onResult ->
                    scope.launch {
                        val connected = withContext(Dispatchers.IO) {
                            Esp32Connection.checkApiConnection(ipAddress)
                        }

                        onResult(
                            if (connected) {
                                "Custom firmware detected"
                            } else {
                                "Firmware not detected"
                            }
                        )
                    }
                }
            )
        }

        HorizontalDivider()

        Text(
            "Device profiles are stored locally on this phone. " +
                "Custom firmware communication is being prepared.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (showDialog) {
        DeviceEditorDialog(
            device = editingDevice,
            onDismiss = {
                showDialog = false
            },
            onSave = { name, hardware, ipAddress ->
                val existing = editingDevice

                if (existing == null) {
                    val newDevice = repository.addDevice(
                        name,
                        hardware
                    ).copy(
                        ipAddress = ipAddress
                    )

                    repository.updateDevice(newDevice)
                    devices.add(newDevice)
                } else {
                    val updated = existing.copy(
                        deviceName = name,
                        hardware = hardware,
                        ipAddress = ipAddress
                    )

                    repository.updateDevice(updated)

                    val index = devices.indexOfFirst {
                        it.deviceId == updated.deviceId
                    }

                    if (index >= 0) {
                        devices[index] = updated
                    }
                }

                showDialog = false
            }
        )
    }
}

@Composable
private fun DeviceCard(
    device: DeviceProfile,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onIpAddressChange: (DeviceProfile) -> Unit,
    onTestConnection: (
        String,
        (String) -> Unit
    ) -> Unit
) {
    var isChecking by remember(device.deviceId) {
        mutableStateOf(false)
    }

    var connectionStatus by remember(device.deviceId) {
        mutableStateOf("Not tested")
    }

    var ipAddress by remember(device.deviceId, device.ipAddress) {
        mutableStateOf(device.ipAddress)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                device.deviceName,
                style = MaterialTheme.typography.titleLarge
            )

            Text("Firmware: ${device.firmwareVersion}")
            Text("Status: $connectionStatus")
            Text("Chip: ${device.hardware.chip}")
            Text("LED: ${device.hardware.ledType}")
            Text("GPIO: ${device.hardware.gpio}")
            Text("Addressable sections: ${device.hardware.ledCount}")
            Text("RGB order: ${device.hardware.rgbOrder}")
            Text("Default brightness: ${device.hardware.defaultBrightness}%")

            OutlinedTextField(
                value = ipAddress,
                onValueChange = {
                    ipAddress = it
                    connectionStatus = "IP address not saved"
                },
                label = { Text("ESP32 IP address") },
                placeholder = { Text("192.168.1.100") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onIpAddressChange(
                        device.copy(ipAddress = ipAddress.trim())
                    )
                    connectionStatus = "IP address saved"
                },
                enabled = ipAddress.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save IP Address")
            }

            Button(
                onClick = {
                    val ip = ipAddress.trim()

                    if (ip.isBlank()) {
                        connectionStatus = "Enter an IP address"
                        return@Button
                    }

                    isChecking = true
                    connectionStatus = "Checking firmware..."

                    onTestConnection(ip) { result ->
                        isChecking = false
                        connectionStatus = result
                    }
                },
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isChecking) {
                        "Checking..."
                    } else {
                        "Test Wi-Fi Connection"
                    }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }

                TextButton(onClick = onDuplicate) {
                    Text("Duplicate")
                }

                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun DeviceEditorDialog(
    device: DeviceProfile?,
    onDismiss: () -> Unit,
    onSave: (String, HardwareProfile, String) -> Unit
) {
    var name by remember(device?.deviceId) {
        mutableStateOf(device?.deviceName ?: "")
    }

    var chip by remember(device?.deviceId) {
        mutableStateOf(device?.hardware?.chip ?: "ESP32")
    }

    var gpio by remember(device?.deviceId) {
        mutableStateOf(device?.hardware?.gpio?.toString() ?: "23")
    }

    var ledType by remember(device?.deviceId) {
        mutableStateOf(device?.hardware?.ledType ?: "WS2811")
    }

    var ledCount by remember(device?.deviceId) {
        mutableStateOf(device?.hardware?.ledCount?.toString() ?: "66")
    }

    var rgbOrder by remember(device?.deviceId) {
        mutableStateOf(device?.hardware?.rgbOrder ?: "GRB")
    }

    var ipAddress by remember(device?.deviceId) {
        mutableStateOf(device?.ipAddress ?: "")
    }

    var channels by remember(device?.deviceId) {
        mutableStateOf(device?.hardware?.channels?.toString() ?: "3")
    }

    var brightness by remember(device?.deviceId) {
        mutableStateOf(
            device?.hardware?.defaultBrightness?.toString() ?: "35"
        )
    }

    var error by remember(device?.deviceId) {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                if (device == null) {
                    "Add Controller"
                } else {
                    "Edit Controller"
                }
            )
        },

        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Controller name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("ESP32 IP address") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = chip,
                    onValueChange = { chip = it },
                    label = { Text("ESP chip") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = gpio,
                    onValueChange = { gpio = it },
                    label = { Text("GPIO pin") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = ledType,
                    onValueChange = { ledType = it },
                    label = { Text("LED type") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = ledCount,
                    onValueChange = { ledCount = it },
                    label = { Text("Addressable sections") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = rgbOrder,
                    onValueChange = { rgbOrder = it },
                    label = { Text("RGB order (e.g. GRB)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = channels,
                    onValueChange = { channels = it },
                    label = { Text("Colour channels") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = brightness,
                    onValueChange = { brightness = it },
                    label = { Text("Default brightness (%)") },
                    singleLine = true
                )

                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },

        confirmButton = {
            TextButton(
                onClick = {
                    val gpioValue = gpio.toIntOrNull()
                    val countValue = ledCount.toIntOrNull()
                    val channelValue = channels.toIntOrNull()
                    val brightnessValue = brightness.toIntOrNull()

                    error = when {
                        name.isBlank() ->
                            "Enter a controller name."

                        chip.isBlank() ->
                            "Enter the ESP chip."

                        gpioValue == null || gpioValue !in 0..48 ->
                            "Enter a valid GPIO number (0–48)."

                        ledType.isBlank() ->
                            "Enter the LED type."

                        countValue == null || countValue !in 1..1000 ->
                            "Sections must be between 1 and 1000."

                        rgbOrder.isBlank() ->
                            "Enter the RGB order."

                        channelValue == null || channelValue !in 1..4 ->
                            "Channels must be between 1 and 4."

                        brightnessValue == null ||
                            brightnessValue !in 0..100 ->
                            "Brightness must be between 0 and 100."

                        else -> ""
                    }

                    if (error.isEmpty()) {
                        onSave(
                            name.trim(),
                            HardwareProfile(
                                chip = chip.trim(),
                                gpio = gpioValue!!,
                                ledType = ledType.trim().uppercase(),
                                ledCount = countValue!!,
                                rgbOrder = rgbOrder.trim().uppercase(),
                                channels = channelValue!!,
                                defaultBrightness = brightnessValue!!
                            ),
                            ipAddress.trim()
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
