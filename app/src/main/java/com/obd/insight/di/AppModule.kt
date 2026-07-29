package com.obd.insight.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.data.bluetooth.PermissionManager
import com.obd.insight.data.elm327.Elm327Protocol
import com.obd.insight.ui.connection.ConnectionViewModel
import com.obd.insight.ui.terminal.AtTerminalViewModel

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

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            ConnectionViewModel(
                bluetoothManager = provideBluetoothManager(),
                elm327Protocol = provideElm327Protocol()
            )
        }
        initializer {
            AtTerminalViewModel(
                bluetoothManager = provideBluetoothManager()
            )
        }
    }

    fun cleanup() {
        bluetoothManager?.disconnect()
        bluetoothManager = null
        elm327Protocol = null
        permissionManager = null
    }
}
