package com.ashwin.smartledcontroller.communication.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.ashwin.smartledcontroller.communication.LedTransport
import com.ashwin.smartledcontroller.communication.protocol.LedCommand
import com.ashwin.smartledcontroller.communication.protocol.LedProtocol
import com.ashwin.smartledcontroller.communication.protocol.LedResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicReference

class BleLedTransport(
    context: Context,
    private val device: BluetoothDevice
) : LedTransport {

    private val appContext = context.applicationContext

    @Volatile
    private var connected = false

    @Volatile
    private var negotiatedMtu = 23

    private var gatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    private var connectionSignal: CompletableDeferred<Boolean>? = null
    private var writeSignal: CompletableDeferred<Boolean>? = null

    private val pendingResponse =
        AtomicReference<CompletableDeferred<LedResponse>?>(null)

    @Volatile
    private var pendingCommandId: String? = null

    private val callback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS &&
                newState == BluetoothProfile.STATE_CONNECTED
            ) {
                if (!gatt.requestMtu(247)) {
                    gatt.discoverServices()
                }
            } else {
                connected = false
                connectionSignal?.complete(false)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(
            gatt: BluetoothGatt,
            mtu: Int,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                negotiatedMtu = mtu
            }
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connectionSignal?.complete(false)
                return
            }

            val service = gatt.getService(BleUuid.SERVICE)

            val rx = service?.getCharacteristic(BleUuid.RX)
            val tx = service?.getCharacteristic(BleUuid.TX)

            if (rx == null || tx == null) {
                connectionSignal?.complete(false)
                return
            }

            rxCharacteristic = rx
            txCharacteristic = tx

            val notificationEnabled = gatt.setCharacteristicNotification(
                tx,
                true
            )

            val descriptor = tx.getDescriptor(BleUuid.CCCD)

            if (!notificationEnabled || descriptor == null) {
                connectionSignal?.complete(false)
                return
            }

            descriptor.value =
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

            if (!gatt.writeDescriptor(descriptor)) {
                connectionSignal?.complete(false)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid == BleUuid.CCCD) {
                connected = status == BluetoothGatt.GATT_SUCCESS
                connectionSignal?.complete(connected)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            writeSignal?.complete(
                status == BluetoothGatt.GATT_SUCCESS
            )
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid != BleUuid.TX) return

            val responseText = characteristic.value
                ?.toString(Charsets.UTF_8)
                ?: return

            val expectedId = pendingCommandId ?: return

            val response = LedProtocol.decode(
                responseJson = responseText,
                expectedCommandId = expectedId
            )

            pendingResponse.get()?.complete(response)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(): Boolean {
        if (connected) return true

        return withContext(Dispatchers.Main) {
            val signal = CompletableDeferred<Boolean>()
            connectionSignal = signal

            try {
                gatt = device.connectGatt(
                    appContext,
                    false,
                    callback,
                    BluetoothDevice.TRANSPORT_LE
                )

                if (gatt == null) {
                    return@withContext false
                }

                withTimeout(20000) {
                    signal.await()
                }
            } catch (exception: Exception) {
                connected = false
                closeGatt()
                false
            } finally {
                connectionSignal = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        connected = false

        pendingResponse.getAndSet(null)?.complete(
            LedResponse(
                ok = false,
                message = "Bluetooth disconnected",
                error = "Connection closed"
            )
        )

        withContext(Dispatchers.Main) {
            closeGatt()
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun send(command: LedCommand): LedResponse {
        if (!connected) {
            return LedResponse(
                id = command.id,
                ok = false,
                message = "Bluetooth disconnected",
                error = "Connect to the ESP32 first"
            )
        }

        val characteristic = rxCharacteristic
            ?: return LedResponse(
                id = command.id,
                ok = false,
                message = "BLE characteristic unavailable"
            )

        val bytes = LedProtocol.encode(command).toByteArray(Charsets.UTF_8)

        if (bytes.size > negotiatedMtu - 3) {
            return LedResponse(
                id = command.id,
                ok = false,
                message = "BLE command is too large",
                error = "Command size ${bytes.size} exceeds the negotiated BLE limit ${negotiatedMtu - 3}"
            )
        }

        val responseDeferred = CompletableDeferred<LedResponse>()
        val writeDeferred = CompletableDeferred<Boolean>()

        pendingCommandId = command.id
        pendingResponse.set(responseDeferred)
        writeSignal = writeDeferred

        return try {
            val started = withContext(Dispatchers.Main) {
                characteristic.writeType =
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

                characteristic.value = bytes

                gatt?.writeCharacteristic(characteristic) == true
            }

            if (!started) {
                return LedResponse(
                    id = command.id,
                    ok = false,
                    message = "BLE write could not start"
                )
            }

            val writeSucceeded = withTimeout(7000) {
                writeDeferred.await()
            }

            if (!writeSucceeded) {
                return LedResponse(
                    id = command.id,
                    ok = false,
                    message = "BLE write failed"
                )
            }

            withTimeout(10000) {
                responseDeferred.await()
            }
        } catch (exception: Exception) {
            LedResponse(
                id = command.id,
                ok = false,
                message = "Bluetooth command failed",
                error = exception.message ?: "Unknown BLE error"
            )
        } finally {
            pendingResponse.compareAndSet(responseDeferred, null)
            pendingCommandId = null
            writeSignal = null
        }
    }

    override fun isConnected(): Boolean = connected

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {
        }

        gatt = null
        rxCharacteristic = null
        txCharacteristic = null
        connected = false
    }
}

