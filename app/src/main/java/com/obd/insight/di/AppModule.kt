package com.obd.insight.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.data.bluetooth.PermissionManager
import com.obd.insight.data.elm327.Elm327Protocol
import com.obd.insight.data.obd.ObdPidReader
import com.obd.insight.data.obd.ObdSensorReader
import com.obd.insight.ui.connection.ConnectionViewModel
import com.obd.insight.ui.dashboard.DashboardViewModel
import com.obd.insight.ui.terminal.AtTerminalViewModel

object AppModule {

    private var bluetoothManager: BluetoothConnectionManager? = null
    private var elm327Protocol: Elm327Protocol? = null
    private var obdPidReader: ObdPidReader? = null
    private var obdSensorReader: ObdSensorReader? = null
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

    fun provideObdPidReader(): ObdPidReader {
        return obdPidReader ?: ObdPidReader(provideElm327Protocol()).also {
            obdPidReader = it
        }
    }

    fun provideObdSensorReader(): ObdSensorReader {
        return obdSensorReader ?: ObdSensorReader(provideObdPidReader()).also {
            obdSensorReader = it
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
                elm327Protocol = provideElm327Protocol(),
                obdPidReader = provideObdPidReader()
            )
        }
        initializer {
            AtTerminalViewModel(
                bluetoothManager = provideBluetoothManager()
            )
        }
    }

    val dashboardViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                sensorReader = provideObdSensorReader()
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
