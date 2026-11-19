
package com.ashwin.smartledcontroller

import android.os.Bundle
import com.ashwin.smartledcontroller.screens.devices.DeviceManagerScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private val AppBackground = Color(0xFF101722)
private val CardBackground = Color(0xFF1D2938)
private val Accent = Color(0xFFFFB45F)
private val MutedText = Color(0xFF9EADBF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                DeviceManagerScreen()
            }
        }
    }
}

@Composable
fun SmartLedApp() {
    var brightness by remember { mutableFloatStateOf(35f) }
    var selectedColor by remember { mutableStateOf("#FFB66D") }
    var hexInput by remember { mutableStateOf("#FFB66D") }
    var selectedSegment by remember { mutableIntStateOf(1) }

    val presets = listOf(
        "Warm White" to "#FFB66D",
        "Natural White" to "#F5E9C9",
        "Deep Amber" to "#FF8A24"
    )

    val previewColor = remember(selectedColor) {
        runCatching {
            Color(android.graphics.Color.parseColor(selectedColor))
        }.getOrDefault(Color.White)
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
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

                // Connection status
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "ESP32 CONNECTION",
                                color = MutedText,
                                fontSize = 11.sp
                            )
                            Text(
                                "Not connected",
                                fontSize = 18.sp
                            )
                            Text(
                                "Wi-Fi / Bluetooth",
                                color = MutedText,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            "OFFLINE",
                            color = Accent,
                            fontSize = 11.sp
                        )
                    }
                }

                // Brightness
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Brightness",
                                fontSize = 19.sp
                            )

                            Text(
                                "${brightness.toInt()}%",
                                color = Accent,
                                fontSize = 19.sp
                            )
                        }

                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = 0f..100f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0%", color = MutedText, fontSize = 12.sp)
                            Text("100%", color = MutedText, fontSize = 12.sp)
                        }
                    }
                }

                // Colour presets
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Lighting Presets",
                        fontSize = 20.sp
                    )

                    presets.forEach { (name, hex) ->
                        PresetCard(
                            name = name,
                            hex = hex,
                            selected = selectedColor == hex,
                            onClick = {
                                selectedColor = hex
                                hexInput = hex
                            }
                        )
                    }
                }

                // Colour selection
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
                            "Custom Colour",
                            fontSize = 19.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp)
                                .background(
                                    previewColor,
                                    RoundedCornerShape(12.dp)
                                )
                        )

                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { hexInput = it },
                            label = { Text("HEX colour") },
                            placeholder = { Text("#FFB66D") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val value = hexInput.trim()
                                val valid = value.matches(
                                    Regex("#[0-9a-fA-F]{6}")
                                )
                                if (valid) {
                                    selectedColor = value.uppercase()
                                    hexInput = value.uppercase()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Colour")
                        }
                    }
                }

                // Segment selection
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "LED Segments",
                        fontSize = 20.sp
                    )

                    Text(
                        "Choose one of your six sections.",
                        color = MutedText,
                        fontSize = 13.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..6).chunked(3).forEach { group ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                group.forEach { segment ->
                                    FilterChip(
                                        selected = selectedSegment == segment,
                                        onClick = {
                                            selectedSegment = segment
                                        },
                                        label = {
                                            Text("Segment $segment")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        "Selected: Segment $selectedSegment",
                        color = Accent,
                        fontSize = 13.sp
                    )
                }

                // Footer
                HorizontalDivider(color = CardBackground)

                Text(
                    "SMART LED CONTROLLER • VERSION 1",
                    color = MutedText,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                Text(
                    "Controls currently run in preview mode. " +
                        "ESP32 communication will be added next.",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PresetCard(
    name: String,
    hex: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val swatchColor = Color(
        android.graphics.Color.parseColor(hex)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                Color(0xFF354253)
            } else {
                CardBackground
            }
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        swatchColor,
                        RoundedCornerShape(10.dp)
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 15.sp)
                Text(
                    hex,
                    color = MutedText,
                    fontSize = 12.sp
                )
            }

            if (selected) {
                Text("SELECTED", color = Accent, fontSize = 10.sp)
            }
        }
    }
}
