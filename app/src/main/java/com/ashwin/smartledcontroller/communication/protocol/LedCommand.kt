package com.ashwin.smartledcontroller.communication.protocol

import org.json.JSONObject
import java.util.UUID

enum class LedCommandType(val wireName: String) {
    SET_BRIGHTNESS("set_brightness"),
    SET_COLOR("set_color"),
    SET_LED("set_led"),
    SET_EFFECT("set_effect"),
    SET_TRANSITION("set_transition"),
    GET_STATUS("get_status"),
    GET_CONFIG("get_config"),
    SET_CONFIG("set_config")
}

/**
 * Shared command format for Wi-Fi HTTP and Bluetooth BLE.
 *
 * Segment indexes are zero-based:
 * 0 = Segment 1, 1 = Segment 2 ... 5 = Segment 6.
 * If segment is omitted, the command applies to the entire strip.
 */
data class LedCommand(
    val id: String = UUID.randomUUID().toString(),
    val type: LedCommandType,
    val payload: JSONObject = JSONObject()
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("protocol", PROTOCOL_VERSION)
            .put("id", id)
            .put("type", type.wireName)
            .put("payload", payload)
    }

    fun toJsonString(): String = toJson().toString()

    companion object {
        const val PROTOCOL_VERSION = 1
        const val MAX_SEGMENTS = 6
        const val MAX_LEDS = 66

        private fun validateSegment(segment: Int?) {
            require(segment == null || segment in 0 until MAX_SEGMENTS) {
                "Segment must be between 0 and 5"
            }
        }

        private fun validateHexColor(hex: String) {
            require(HEX_COLOR_REGEX.matches(hex)) {
                "Color must be in #RRGGBB format"
            }
        }

        private val HEX_COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")

        fun setBrightness(
            value: Int,
            segment: Int? = null
        ): LedCommand {
            require(value in 0..100) {
                "Brightness must be between 0 and 100"
            }
            validateSegment(segment)

            val data = JSONObject().put("value", value)
            segment?.let { data.put("segment", it) }

            return LedCommand(
                type = LedCommandType.SET_BRIGHTNESS,
                payload = data
            )
        }

        fun setColor(
            hex: String,
            segment: Int? = null
        ): LedCommand {
            validateHexColor(hex)
            validateSegment(segment)

            val data = JSONObject().put("color", hex.uppercase())
            segment?.let { data.put("segment", it) }

            return LedCommand(
                type = LedCommandType.SET_COLOR,
                payload = data
            )
        }

        fun setLed(
            index: Int,
            hex: String
        ): LedCommand {
            require(index in 0 until MAX_LEDS) {
                "LED index must be between 0 and 65"
            }
            validateHexColor(hex)

            return LedCommand(
                type = LedCommandType.SET_LED,
                payload = JSONObject()
                    .put("index", index)
                    .put("color", hex.uppercase())
            )
        }

        fun setEffect(
            effect: String,
            speed: Int = 50,
            intensity: Int = 50
        ): LedCommand {
            require(effect.isNotBlank()) {
                "Effect name cannot be empty"
            }
            require(speed in 0..100) {
                "Effect speed must be between 0 and 100"
            }
            require(intensity in 0..100) {
                "Effect intensity must be between 0 and 100"
            }

            return LedCommand(
                type = LedCommandType.SET_EFFECT,
                payload = JSONObject()
                    .put("effect", effect)
                    .put("speed", speed)
                    .put("intensity", intensity)
            )
        }

        fun setTransition(
            durationMs: Int,
            targetBrightness: Int? = null,
            targetColor: String? = null,
            segment: Int? = null
        ): LedCommand {
            require(durationMs in 0..300_000) {
                "Transition duration must be between 0 and 300000 ms"
            }
            require(targetBrightness == null || targetBrightness in 0..100) {
                "Target brightness must be between 0 and 100"
            }
            targetColor?.let { validateHexColor(it) }
            validateSegment(segment)

            val data = JSONObject()
                .put("duration_ms", durationMs)

            targetBrightness?.let {
                data.put("brightness", it)
            }

            targetColor?.let {
                data.put("color", it.uppercase())
            }

            segment?.let {
                data.put("segment", it)
            }

            return LedCommand(
                type = LedCommandType.SET_TRANSITION,
                payload = data
            )
        }

        fun getStatus(): LedCommand {
            return LedCommand(type = LedCommandType.GET_STATUS)
        }

        fun getConfig(): LedCommand {
            return LedCommand(type = LedCommandType.GET_CONFIG)
        }

        fun setConfig(config: JSONObject): LedCommand {
            return LedCommand(
                type = LedCommandType.SET_CONFIG,
                payload = JSONObject().put("config", config)
            )
        }
    }
}
