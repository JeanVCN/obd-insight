package com.obd.insight.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.obd.insight.domain.model.BluetoothError
import com.obd.insight.domain.model.BluetoothResult
import com.obd.insight.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Manages the Bluetooth Classic RFCOMM/SPP connection to an ELM327 adapter.
 *
 * Callers must ensure Bluetooth permissions are granted before invoking any
 * Bluetooth API (BLUETOOTH_SCAN / BLUETOOTH_CONNECT on API 31+, location on
 * older versions). The UI gates permissions before reaching these methods, so
 * lint is suppressed here; a SecurityException is still handled defensively.
 */
@SuppressLint("MissingPermission")
class BluetoothConnectionManager(
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val commandTimeoutMillis: Long = 3000L
) {
    private companion object {
        const val TAG = "BluetoothConnectionManager"
    }
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var receiverRegistered = false
    private val commandMutex = Mutex()
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return
                    if (device.bondState == BluetoothDevice.BOND_NONE) {
                        _discoveredDevices.value = (_discoveredDevices.value + device)
                            .distinctBy { it.address }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return
                    if (device.bondState == BluetoothDevice.BOND_BONDED) {
                        _discoveredDevices.value = _discoveredDevices.value.filterNot { it.address == device.address }
                    }
                }
            }
        }
    }

    fun isBluetoothAvailable(): Boolean = adapter != null

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun startDiscovery(): Boolean {
        val bluetoothAdapter = adapter ?: return false
        if (!bluetoothAdapter.isEnabled || context == null) return false

        return try {
            registerDiscoveryReceiver()
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            _discoveredDevices.value = emptyList()
            val started = bluetoothAdapter.startDiscovery()
            if (!started) {
                Log.w(
                    TAG,
                    "Bluetooth discovery was rejected: enabled=${bluetoothAdapter.isEnabled}, " +
                        "discovering=${bluetoothAdapter.isDiscovering}"
                )
            }
            started
        } catch (_: SecurityException) {
            Log.e(TAG, "Bluetooth discovery permission was rejected", null)
            false
        }
    }

    fun stopDiscovery() {
        try {
            adapter?.cancelDiscovery()
        } catch (_: SecurityException) { }
    }

    fun pair(device: BluetoothDevice): Boolean {
        return try {
            stopDiscovery()
            device.createBond()
        } catch (_: SecurityException) {
            false
        }
    }

    suspend fun connect(device: BluetoothDevice): BluetoothResult<Unit> = withContext(ioDispatcher) {
        val name = deviceName(device)
        _state.value = ConnectionState.Connecting(name)
        try {
            stopDiscovery()
            val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket = newSocket
            newSocket.connect()
            inputStream = newSocket.inputStream
            _state.value = ConnectionState.Connected(name)
            BluetoothResult.Success(Unit)
        } catch (e: SecurityException) {
            closeSocket()
            _state.value = ConnectionState.Error(BluetoothError.PERMISSION_DENIED, e.message ?: "A permissão Bluetooth foi revogada")
            BluetoothResult.Error(BluetoothError.PERMISSION_DENIED)
        } catch (e: IOException) {
            closeSocket()
            _state.value = ConnectionState.Error(BluetoothError.SOCKET_ERROR, e.message ?: "A conexão falhou")
            BluetoothResult.Error(BluetoothError.SOCKET_ERROR)
        }
    }

    fun disconnect() {
        closeSocket()
        closeDiscoveryReceiver()
        _state.value = ConnectionState.Disconnected
    }

    fun close() {
        disconnect()
    }

    suspend fun sendCommand(command: String): BluetoothResult<String> = commandMutex.withLock {
        withContext(ioDispatcher) {
            val activeSocket = socket ?: return@withContext BluetoothResult.Error(BluetoothError.IO_ERROR)
            val input = inputStream ?: return@withContext BluetoothResult.Error(BluetoothError.IO_ERROR)

            try {
                val output: OutputStream = activeSocket.outputStream
                output.write((command + "\r").toByteArray(Charsets.US_ASCII))
                output.flush()

                val response = readUntilPrompt(input)
                if (response == null) {
                    closeSocket()
                    _state.value = ConnectionState.Error(
                        BluetoothError.CONNECTION_TIMEOUT,
                        "Nenhuma resposta completa do adaptador para '$command'"
                    )
                    BluetoothResult.Error(BluetoothError.CONNECTION_TIMEOUT)
                } else if (response.isEmpty()) {
                    BluetoothResult.Error(BluetoothError.IO_ERROR)
                } else {
                    Log.d(TAG, "Command '$command' response: $response")
                    BluetoothResult.Success(response)
                }
            } catch (e: SecurityException) {
                _state.value = ConnectionState.Error(BluetoothError.PERMISSION_DENIED, "A permissão Bluetooth foi revogada")
                BluetoothResult.Error(BluetoothError.PERMISSION_DENIED)
            } catch (e: IOException) {
                _state.value = ConnectionState.Error(BluetoothError.IO_ERROR, e.message ?: "Erro de comunicação ao enviar o comando")
                BluetoothResult.Error(BluetoothError.IO_ERROR)
            }
        }
    }

    private suspend fun readUntilPrompt(input: InputStream): String? {
        val builder = StringBuilder()
        val deadline = System.nanoTime() + commandTimeoutMillis * 1_000_000L
        while (System.nanoTime() < deadline) {
            if (input.available() > 0) {
                val value = input.read()
                if (value < 0) return null
                if (value.toChar() == '>') {
                    return builder.toString().replace(Regex("\\s+"), " ").trim()
                }
                builder.append(value.toChar())
            } else {
                delay(10)
            }
        }
        return null
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (_: IOException) { }
        socket = null
        inputStream = null
    }

    private fun registerDiscoveryReceiver() {
        if (receiverRegistered) return
        val receiverContext = context ?: return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            receiverContext,
            discoveryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    private fun closeDiscoveryReceiver() {
        context?.takeIf { receiverRegistered }?.unregisterReceiver(discoveryReceiver)
        receiverRegistered = false
    }

    private fun deviceName(device: BluetoothDevice): String =
        try {
            device.name ?: device.address
        } catch (_: SecurityException) {
            device.address
        }
}
