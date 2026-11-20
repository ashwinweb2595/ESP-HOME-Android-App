package com.ashwin.smartledcontroller.communication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class BleScanner(context: Context) {

    private val appContext = context.applicationContext

    @SuppressLint("MissingPermission")
    suspend fun scan(timeoutMs: Long = 10000): List<BluetoothDevice> {
        return withContext(Dispatchers.Main) {
            val manager = appContext.getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as BluetoothManager

            val adapter = manager.adapter
                ?: throw IllegalStateException("Bluetooth is not supported")

            if (!adapter.isEnabled) {
                throw IllegalStateException("Please enable Bluetooth")
            }

            val scanner = adapter.bluetoothLeScanner
                ?: throw IllegalStateException("BLE scanner unavailable")

            val devices = ConcurrentHashMap<String, BluetoothDevice>()

            val callback = object : ScanCallback() {
                override fun onScanResult(
                    callbackType: Int,
                    result: android.bluetooth.le.ScanResult
                ) {
                    val device = result.device
                    devices[device.address] = device
                }

                override fun onBatchScanResults(
                    results: MutableList<android.bluetooth.le.ScanResult>
                ) {
                    results.forEach { result ->
                        devices[result.device.address] = result.device
                    }
                }
            }

            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleUuid.SERVICE))
                .build()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            try {
                scanner.startScan(listOf(filter), settings, callback)
                delay(timeoutMs)
                devices.values.toList()
            } finally {
                scanner.stopScan(callback)
            }
        }
    }
}
