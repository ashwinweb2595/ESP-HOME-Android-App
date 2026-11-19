package com.ashwin.smartledcontroller.communication.wifi

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Esp32Connection {

    private const val DEFAULT_PORT = 80
    private const val STATUS_ENDPOINT = "/api/v1/status"
    private const val EXPECTED_DEVICE = "smart-led-controller"
    private const val PROTOCOL_VERSION = 1

    fun checkApiConnection(
        ipAddress: String,
        port: Int = DEFAULT_PORT,
        timeout: Int = 3000
    ): Boolean {

        if (ipAddress.isBlank() || port !in 1..65535) {
            return false
        }

        var connection: HttpURLConnection? = null

        return try {
            val url = URL(
                "http",
                ipAddress.trim(),
                port,
                STATUS_ENDPOINT
            )

            connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                connectTimeout = timeout
                readTimeout = timeout
                useCaches = false
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return false
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val json = JSONObject(response)

            json.optBoolean("ok", false) &&
                json.optString("device") == EXPECTED_DEVICE &&
                json.optInt("protocol") == PROTOCOL_VERSION

        } catch (exception: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }
}
