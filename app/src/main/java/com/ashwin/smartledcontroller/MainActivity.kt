
package com.ashwin.smartledcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashwin.smartledcontroller.communication.protocol.LedCommand
import com.ashwin.smartledcontroller.communication.wifi.HttpLedTransport
import kotlinx.coroutines.launch

private val AppBackground = Color(0xFF101722)
private val CardBackground = Color(0xFF1D2938)
private val Accent = Color(0xFFFFB45F)
private val MutedText = Color(0xFF9EADBF)

private data class SegmentUi(
    val id: Int,
    val name: String,
    val start: Int,
    val count: Int,
    val brightness: Int,
    val enabled: Boolean,
    val color: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartLedApp()
        }
    }
}

@Composable
fun SmartLedApp() {
    var ipAddress by remember { mutableStateOf("192.168.1.34") }

    var transport by remember {
        mutableStateOf<HttpLedTransport?>(null)
    }

    var connected by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Not connected") }
    var errorMessage by remember { mutableStateOf("") }

    var totalLeds by remember { mutableIntStateOf(0) }
    var masterBrightness by remember { mutableIntStateOf(0) }
    var segments by remember { mutableStateOf(emptyList<SegmentUi>()) }

    val scope = rememberCoroutineScope()

    fun refreshConfiguration(activeTransport: HttpLedTransport) {
        scope.launch {
            loading = true
            errorMessage = ""

            try {
                val response = activeTransport.send(
                    LedCommand.getConfig()
                )

                if (!response.ok) {
                    errorMessage = response.error
                        ?: response.message
                    statusMessage = "Configuration retrieval failed"
                    return@launch
                }

                val data = response.data
                val segmentArray = data.optJSONArray("segments")

                val updatedSegments = mutableListOf<SegmentUi>()

                if (segmentArray != null) {
                    for (index in 0 until segmentArray.length()) {
                        val item = segmentArray.optJSONObject(index)
                            ?: continue

                        updatedSegments.add(
                            SegmentUi(
                                id = item.optInt("id"),
                                name = item.optString("name"),
                                start = item.optInt("start"),
                                count = item.optInt("count"),
                                brightness = item.optInt("brightness"),
                                enabled = item.optBoolean("enabled"),
                                color = item.optString("color")
                            )
                        )
                    }
                }

                totalLeds = data.optInt("totalLeds")
                masterBrightness = data.optInt("brightness")
                segments = updatedSegments

                statusMessage = "Configuration synchronized"
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "Unknown error"
            } finally {
                loading = false
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = AppBackground,
            surface = CardBackground,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Header
                Column {
                    Text(
                        "SMART LED",
                        color = Accent,
                        fontSize = 12.sp,
                        letterSpacing = 3.sp
                    )

                    Text(
                        "Controller",
                        fontSize = 28.sp,
                        color = Color.White
                    )

                    Text(
                        "Your lighting, your control.",
                        color = MutedText,
                        fontSize = 14.sp
                    )
                }

                // Connection card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ESP32 CONNECTION", color = MutedText)
                            Text(
                                if (connected) "ONLINE" else "OFFLINE",
                                color = if (connected) {
                                    Color(0xFF70D99A)
                                } else {
                                    Accent
                                }
                            )
                        }

                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text("ESP32 IP address") },
                            singleLine = true,
                            enabled = !connected,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!connected) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        loading = true
                                        errorMessage = ""
                                        statusMessage = "Connecting..."

                                        try {
                                            val candidate =
                                                HttpLedTransport(
                                                    ipAddress.trim()
                                                )

                                            if (candidate.connect()) {
                                                transport = candidate
                                                connected = true
                                                statusMessage =
                                                    "Connected. Reading configuration..."

                                                refreshConfiguration(candidate)
                                            } else {
                                                candidate.disconnect()
                                                statusMessage =
                                                    "Connection failed"
                                                errorMessage =
                                                    "Check the ESP32 IP and Wi-Fi network."
                                            }
                                        } catch (exception: Exception) {
                                            statusMessage =
                                                "Connection failed"
                                            errorMessage =
                                                exception.message
                                                    ?: "Unknown network error"
                                        } finally {
                                            loading = false
                                        }
                                    }
                                },
                                enabled = !loading && ipAddress.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Connect to ESP32")
                            }
                        } else {
                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        transport?.let {
                                            refreshConfiguration(it)
                                        }
                                    },
                                    enabled = !loading,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Refresh")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            transport?.disconnect()
                                            transport = null
                                            connected = false
                                            segments = emptyList()
                                            totalLeds = 0
                                            statusMessage = "Disconnected"
                                        }
                                    },
                                    enabled = !loading,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Disconnect")
                                }
                            }
                        }

                        if (loading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            statusMessage,
                            color = MutedText,
                            fontSize = 13.sp
                        )

                        if (errorMessage.isNotBlank()) {
                            Text(
                                errorMessage,
                                color = Color(0xFFFF8585),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Live device configuration
                if (connected && segments.isNotEmpty()) {

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CardBackground
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "DEVICE CONFIGURATION",
                                color = Accent,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Text("Addressable sections")
                                Text(
                                    "$totalLeds",
                                    color = Accent
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Text("Master brightness")
                                Text(
                                    "$masterBrightness%",
                                    color = Accent
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Text("Total segments")
                                Text(
                                    "${segments.size}",
                                    color = Accent
                                )
                            }
                        }
                    }

                    Text(
                        "LIVE LED SEGMENTS",
                        color = Accent,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )

                    segments.forEach { segment ->
                        SegmentCard(segment)
                    }
                }

                if (connected && segments.isEmpty() && !loading) {
                    Text(
                        "Connected, but no segments were returned. " +
                            "Try refreshing the configuration.",
                        color = MutedText
                    )
                }

                HorizontalDivider(color = CardBackground)

                Text(
                    "SMART LED CONTROLLER • VERSION 1",
                    color = MutedText,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                Text(
                    "Live configuration mode. LED output controls " +
                        "will be enabled after communication testing.",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SegmentCard(segment: SegmentUi) {
    val swatch = runCatching {
        Color(android.graphics.Color.parseColor(segment.color))
    }.getOrDefault(Color.White)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        swatch,
                        RoundedCornerShape(10.dp)
                    )
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    segment.name,
                    fontSize = 16.sp
                )

                Text(
                    "ID ${segment.id} • Start ${segment.start} • " +
                        "${segment.count} sections",
                    color = MutedText,
                    fontSize = 11.sp
                )

                Text(
                    "${segment.brightness}% • ${segment.color}",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    if (segment.enabled) "ON" else "OFF",
                    color = if (segment.enabled) {
                        Color(0xFF70D99A)
                    } else {
                        MutedText
                    },
                    fontSize = 12.sp
                )
            }
        }
    }
}
