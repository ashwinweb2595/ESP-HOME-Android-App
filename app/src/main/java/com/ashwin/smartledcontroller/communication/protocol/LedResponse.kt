package com.ashwin.smartledcontroller.communication.protocol

import org.json.JSONObject

data class LedResponse(
    val id: String = "",
    val ok: Boolean,
    val message: String = "",
    val data: JSONObject = JSONObject(),
    val error: String? = null
) {
    companion object {

        fun fromJson(json: JSONObject): LedResponse {
            return LedResponse(
                id = json.optString("id", ""),
                ok = json.optBoolean("ok", false),
                message = json.optString("message", ""),
                data = json.optJSONObject("data") ?: JSONObject(),
                error = json.optString("error")
                    .takeIf { it.isNotBlank() && it != "null" }
            )
        }

        fun fromJsonString(jsonString: String): LedResponse {
            return try {
                fromJson(JSONObject(jsonString))
            } catch (exception: Exception) {
                LedResponse(
                    ok = false,
                    message = "Invalid response from ESP32",
                    error = exception.message ?: "Unable to parse response"
                )
            }
        }
    }
}
