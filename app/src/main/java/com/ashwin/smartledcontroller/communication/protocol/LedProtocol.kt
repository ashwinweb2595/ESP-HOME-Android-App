package com.ashwin.smartledcontroller.communication.protocol

import org.json.JSONObject

/**
 * Shared protocol handler for Wi-Fi and Bluetooth communication.
 */
object LedProtocol {

    const val VERSION = 1

    /**
     * Converts an LED command into the JSON string
     * that will be sent to the ESP32.
     */
    fun encode(command: LedCommand): String {
        return command.toJsonString()
    }

    /**
     * Parses an ESP32 response and validates:
     * 1. JSON format
     * 2. Protocol version
     * 3. Optional command ID matching
     */
    fun decode(
        responseJson: String,
        expectedCommandId: String? = null
    ): LedResponse {

        return try {
            val json = JSONObject(responseJson)

            val protocolVersion = json.optInt("protocol", -1)

            if (protocolVersion != VERSION) {
                return LedResponse(
                    id = json.optString("id", ""),
                    ok = false,
                    message = "Unsupported protocol version",
                    error = "Expected protocol $VERSION but received $protocolVersion"
                )
            }

            val response = LedResponse.fromJson(json)

            if (
                expectedCommandId != null &&
                response.id != expectedCommandId
            ) {
                return LedResponse(
                    id = response.id,
                    ok = false,
                    message = "Response ID mismatch",
                    error = "The response does not match the requested command"
                )
            }

            response

        } catch (exception: Exception) {
            LedResponse(
                ok = false,
                message = "Unable to decode ESP32 response",
                error = exception.message ?: "Invalid JSON response"
            )
        }
    }
}
