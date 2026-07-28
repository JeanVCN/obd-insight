package com.obd.insight.domain.model

import android.bluetooth.BluetoothDevice

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Scanning : ConnectionState
    data class FoundDevices(val devices: List<BluetoothDevice>) : ConnectionState
    data class Connecting(val deviceName: String) : ConnectionState
    data class Connected(val deviceName: String) : ConnectionState
    data class Error(val error: BluetoothError, val message: String) : ConnectionState
}