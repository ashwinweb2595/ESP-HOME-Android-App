
package com.ashwin.smartledcontroller.communication

import com.ashwin.smartledcontroller.communication.protocol.LedCommand
import com.ashwin.smartledcontroller.communication.protocol.LedResponse

/**
 * Common interface for Wi-Fi and Bluetooth communication.
 * Both transports use the same command and response format.
 */
interface LedTransport {

    suspend fun connect(): Boolean

    suspend fun disconnect()

    suspend fun send(command: LedCommand): LedResponse

    fun isConnected(): Boolean
}
