package com.ashwin.smartledcontroller.communication.bluetooth

import java.util.UUID

object BleUuid {
    val SERVICE: UUID =
        UUID.fromString("7f510001-1b15-4f0a-9a3c-2d9e7b1a0001")

    // Android -> ESP32 commands
    val RX: UUID =
        UUID.fromString("7f510002-1b15-4f0a-9a3c-2d9e7b1a0001")

    // ESP32 -> Android responses
    val TX: UUID =
        UUID.fromString("7f510003-1b15-4f0a-9a3c-2d9e7b1a0001")

    val CCCD: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
