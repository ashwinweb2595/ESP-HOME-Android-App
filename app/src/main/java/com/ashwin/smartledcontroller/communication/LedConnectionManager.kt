package com.ashwin.smartledcontroller.communication

import com.ashwin.smartledcontroller.communication.protocol.LedCommand
import com.ashwin.smartledcontroller.communication.protocol.LedResponse

/**
 * Transport selection for LED communication.
 */
enum class LedTransportMode {
    WIFI,
    BLUETOOTH
}

/**
 * Coordinates Wi-Fi and Bluetooth transports.
 *
 * Only the explicitly selected transport sends commands.
 * No automatic fallback or duplicate command sending.
 */
class LedConnectionManager(
    private val wifiTransport: LedTransport,
    private val bluetoothTransport: LedTransport
) {

    @Volatile
    var selectedMode: LedTransportMode = LedTransportMode.WIFI
        private set

    fun selectTransport(mode: LedTransportMode) {
        selectedMode = mode
    }

    suspend fun connect(mode: LedTransportMode): Boolean {
        return transportFor(mode).connect()
    }

    suspend fun disconnect(mode: LedTransportMode) {
        transportFor(mode).disconnect()
    }

    fun isConnected(mode: LedTransportMode): Boolean {
        return transportFor(mode).isConnected()
    }

    suspend fun send(command: LedCommand): LedResponse {
        val selectedTransport = transportFor(selectedMode)

        if (!selectedTransport.isConnected()) {
            return LedResponse(
                id = command.id,
                ok = false,
                message = "Selected transport is disconnected",
                error = "Connect using $selectedMode before sending commands"
            )
        }

        return try {
            selectedTransport.send(command)
        } catch (exception: Exception) {
            LedResponse(
                id = command.id,
                ok = false,
                message = "Command transmission failed",
                error = exception.message ?: "Unknown communication error"
            )
        }
    }

    private fun transportFor(mode: LedTransportMode): LedTransport {
        return when (mode) {
            LedTransportMode.WIFI -> wifiTransport
            LedTransportMode.BLUETOOTH -> bluetoothTransport
        }
    }
}
