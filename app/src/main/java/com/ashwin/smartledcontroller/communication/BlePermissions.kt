package com.ashwin.smartledcontroller.communication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object BlePermissions {

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun hasRequiredPermissions(context: Context): Boolean {
        return requiredPermissions().all { permission ->
            context.checkSelfPermission(permission) ==
                PackageManager.PERMISSION_GRANTED
        }
    }
}
