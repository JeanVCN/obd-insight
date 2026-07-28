package com.obd.insight.di

import android.content.Context
import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.data.bluetooth.PermissionManager
import com.obd.insight.data.elm327.Elm327Protocol
import com.obd.insight.ui.connection.ConnectionViewModel

object AppModule {

    private var bluetoothManager: BluetoothConnectionManager? = null
    private var elm327Protocol: Elm327Protocol? = null
    private var permissionManager: PermissionManager? = null

    fun provideBluetoothManager(): BluetoothConnectionManager {
        return bluetoothManager ?: BluetoothConnectionManager().also {
            bluetoothManager = it
        }
    }

    fun provideElm327Protocol(): Elm327Protocol {
        return elm327Protocol ?: Elm327Protocol(provideBluetoothManager()).also {
            elm327Protocol = it
        }
    }

    fun providePermissionManager(context: Context): PermissionManager {
        return permissionManager ?: PermissionManager(context).also {
            permissionManager = it
        }
    }

    fun provideConnectionViewModel(): ConnectionViewModel {
        return ConnectionViewModel(
            bluetoothManager = provideBluetoothManager(),
            elm327Protocol = provideElm327Protocol()
        )
    }

    fun cleanup() {
        bluetoothManager?.disconnect()
        bluetoothManager = null
        elm327Protocol = null
        permissionManager = null
    }
}
