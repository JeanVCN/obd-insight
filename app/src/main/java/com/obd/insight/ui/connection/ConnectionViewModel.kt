package com.obd.insight.ui.connection

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.data.elm327.Elm327Protocol
import com.obd.insight.data.obd.ObdPidReader
import com.obd.insight.domain.model.ConnectionState
import com.obd.insight.domain.model.ProtocolType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val bluetoothManager: BluetoothConnectionManager,
    private val elm327Protocol: Elm327Protocol,
    private val obdPidReader: ObdPidReader
) : ViewModel() {

    private val _state: MutableStateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _protocol = MutableStateFlow<ProtocolType?>(null)
    val protocol: StateFlow<ProtocolType?> = _protocol.asStateFlow()

    private val _supportedPids = MutableStateFlow<List<Int>?>(null)
    val supportedPids: StateFlow<List<Int>?> = _supportedPids.asStateFlow()

    init {
        viewModelScope.launch {
            bluetoothManager.state.collect { connectionState ->
                _state.value = connectionState
            }
        }
    }

    fun scanDevices() {
        _state.value = ConnectionState.Scanning
        _devices.value = bluetoothManager.getPairedDevices()
        _state.value = ConnectionState.FoundDevices(_devices.value)
    }

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            val result = bluetoothManager.connect(device)
            if (result is com.obd.insight.domain.model.BluetoothResult.Success) {
                val initResult = elm327Protocol.initialize()
                if (initResult is com.obd.insight.domain.model.BluetoothResult.Success) {
                    val protocolResult = elm327Protocol.detectProtocol()
                    if (protocolResult is com.obd.insight.domain.model.BluetoothResult.Success) {
                        _protocol.value = protocolResult.data
                    }
                    val pidResult = obdPidReader.requestSupportedPids()
                    if (pidResult is com.obd.insight.domain.model.BluetoothResult.Success) {
                        _supportedPids.value = pidResult.data
                    }
                }
            }
        }
    }

    fun disconnect() {
        bluetoothManager.disconnect()
    }
}
