package com.ashwin.smartledcontroller.communication.wifi

import com.ashwin.smartledcontroller.communication.LedTransport
import com.ashwin.smartledcontroller.communication.protocol.LedCommand
import com.ashwin.smartledcontroller.communication.protocol.LedProtocol
import com.ashwin.smartledcontroller.communication.protocol.LedResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HttpLedTransport(
    private val ipAddress: String,
    private val port: Int = 80
) : LedTransport {

    @Volatile
    private var connected = false

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = executeRequest(
                path = "/api/v1/status",
                method = "GET"
            )

            val json = JSONObject(response)

            connected =
                json.optBoolean("ok", false) &&
                json.optString("device") == "smart-led-controller" &&
                json.optInt("protocol", -1) == LedProtocol.VERSION

            connected
        } catch (exception: Exception) {
            connected = false
            false
        }
    }

    override suspend fun disconnect() {
        connected = false
    }

    override suspend fun send(command: LedCommand): LedResponse {
        if (!connected) {
            return LedResponse(
                ok = false,
                message = "Wi-Fi transport is disconnected",
                error = "Connect to the ESP32 before sending commands"
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = executeRequest(
                    path = "/api/v1/command",
                    method = "POST",
                    body = LedProtocol.encode(command)
                )

                LedProtocol.decode(
                    responseJson = response,
                    expectedCommandId = command.id
                )
            } catch (exception: Exception) {
                connected = false

                LedResponse(
                    id = command.id,
                    ok = false,
                    message = "Wi-Fi command failed",
                    error = exception.message ?: "Unknown network error"
                )
            }
        }
    }

    override fun isConnected(): Boolean = connected

    private fun executeRequest(
        path: String,
        method: String,
        body: String? = null
    ): String {
        val url = URL("http://$ipAddress:$port$path")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.bufferedReader()?.use {
                it.readText()
            }.orEmpty()

            if (statusCode !in 200..299) {
                throw IllegalStateException(
                    "ESP32 HTTP error $statusCode: $response"
                )
            }

            return response
        } finally {
            connection.disconnect()
        }
    }
}
