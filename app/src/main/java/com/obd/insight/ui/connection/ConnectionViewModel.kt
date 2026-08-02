package com.obd.insight.ui.connection

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.data.elm327.Elm327Protocol
import com.obd.insight.data.obd.ObdPidReader
import com.obd.insight.data.obd.ObdSensorReader
import com.obd.insight.domain.model.ConnectionState
import com.obd.insight.domain.model.ProtocolType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel(
    private val bluetoothManager: BluetoothConnectionManager,
    private val elm327Protocol: Elm327Protocol,
    private val obdPidReader: ObdPidReader,
    private val sensorReader: ObdSensorReader? = null
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

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _pairingDeviceName = MutableStateFlow<String?>(null)
    val pairingDeviceName: StateFlow<String?> = _pairingDeviceName.asStateFlow()

    private val _discoveryMessage = MutableStateFlow<String?>(null)
    val discoveryMessage: StateFlow<String?> = _discoveryMessage.asStateFlow()

    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow()

    init {
        viewModelScope.launch {
            bluetoothManager.state.collect { connectionState ->
                _state.value = connectionState
            }
        }
        viewModelScope.launch {
            bluetoothManager.discoveredDevices.collect { devices ->
                _discoveredDevices.value = devices
            }
        }
    }

    fun scanDevices() {
        if (!bluetoothManager.isBluetoothAvailable()) {
            _state.value = ConnectionState.Error(
                com.obd.insight.domain.model.BluetoothError.UNKNOWN,
                "O Bluetooth não está disponível neste dispositivo"
            )
            return
        }
        if (!bluetoothManager.isBluetoothEnabled()) {
            _state.value = ConnectionState.Error(
                com.obd.insight.domain.model.BluetoothError.BLUETOOTH_OFF,
                "Ligue o Bluetooth para procurar adaptadores OBD"
            )
            return
        }
        _state.value = ConnectionState.Scanning
        _devices.value = bluetoothManager.getPairedDevices()
        _discoveredDevices.value = emptyList()
        _discoveryMessage.value = null
        if (!bluetoothManager.startDiscovery()) {
            if (_devices.value.isNotEmpty()) {
                _discoveryMessage.value =
                    "A busca por dispositivos próximos não iniciou. Exibindo os dispositivos pareados."
                _state.value = ConnectionState.FoundDevices(_devices.value)
            } else {
                _state.value = ConnectionState.Error(
                    com.obd.insight.domain.model.BluetoothError.DISCOVERY_FAILED,
                    "Não foi possível iniciar a busca Bluetooth. Verifique as configurações e tente novamente."
                )
            }
        } else {
            _state.value = ConnectionState.FoundDevices(_devices.value)
        }
    }

    fun onPermissionsDenied() {
        _state.value = ConnectionState.Error(
            com.obd.insight.domain.model.BluetoothError.PERMISSION_DENIED,
                "A permissão Bluetooth é necessária para encontrar e conectar um adaptador OBD."
        )
    }

    fun pair(device: BluetoothDevice) {
        if (bluetoothManager.pair(device)) {
            _pairingDeviceName.value = deviceName(device)
        } else {
            _state.value = ConnectionState.Error(
                com.obd.insight.domain.model.BluetoothError.PERMISSION_DENIED,
                "Não foi possível iniciar o pareamento com ${deviceName(device)}"
            )
        }
    }

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            _warningMessage.value = null
            val result = bluetoothManager.connect(device)
            if (result is com.obd.insight.domain.model.BluetoothResult.Success) {
                val initResult = elm327Protocol.initialize()
                if (initResult is com.obd.insight.domain.model.BluetoothResult.Error) {
                    failHandshake(initResult.reason, "A inicialização do ELM327 falhou")
                    return@launch
                }
                val protocolResult = elm327Protocol.detectProtocol()
                if (protocolResult is com.obd.insight.domain.model.BluetoothResult.Error) {
                    failHandshake(protocolResult.reason, "A identificação do protocolo falhou")
                    return@launch
                }
                _protocol.value = (protocolResult as com.obd.insight.domain.model.BluetoothResult.Success).data
                val pidResult = obdPidReader.requestSupportedPids()
                if (pidResult is com.obd.insight.domain.model.BluetoothResult.Error) {
                    _warningMessage.value =
                        "Não foi possível ler os PIDs suportados (${pidResult.reason}). " +
                            "O adaptador está conectado; use o terminal ou o painel para continuar."
                    return@launch
                }
                _supportedPids.value = (pidResult as com.obd.insight.domain.model.BluetoothResult.Success).data
                sensorReader?.setSupportedPids(_supportedPids.value.orEmpty())
            }
        }
    }

    private suspend fun failHandshake(
        error: com.obd.insight.domain.model.BluetoothError,
        message: String
    ) {
        bluetoothManager.disconnect()
        _state.value = ConnectionState.Error(error, message)
    }

    private fun deviceName(device: BluetoothDevice): String =
        try {
            device.name ?: device.address
        } catch (_: SecurityException) {
            device.address
        }

    fun disconnect() {
        bluetoothManager.disconnect()
        _protocol.value = null
        _supportedPids.value = null
        sensorReader?.clearSupportedPids()
        _warningMessage.value = null
    }
}
