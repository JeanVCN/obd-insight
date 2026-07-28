package com.obd.insight.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.obd.insight.domain.model.BluetoothError
import com.obd.insight.domain.model.BluetoothResult
import com.obd.insight.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

class BluetoothConnectionManager(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var device: BluetoothDevice? = null
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    suspend fun connect(device: BluetoothDevice): BluetoothResult<Unit> = withContext(ioDispatcher) {
        _state.value = ConnectionState.Connecting(device.name ?: "Unknown")
        try {
            this@BluetoothConnectionManager.device = device
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            adapter?.cancelDiscovery()
            socket?.let { it.connect() }
            _state.value = ConnectionState.Connected(device.name ?: "Unknown")
            BluetoothResult.Success(Unit)
        } catch (e: IOException) {
            _state.value = ConnectionState.Error(BluetoothError.SOCKET_ERROR, e.message ?: "Connection failed")
            BluetoothResult.Error(BluetoothError.SOCKET_ERROR)
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: IOException) { }
        socket = null
        device = null
        _state.value = ConnectionState.Disconnected
    }

    suspend fun sendCommand(command: String): BluetoothResult<String> = withContext(ioDispatcher) {
        try {
            val outputStream: OutputStream = socket?.outputStream
                ?: return@withContext BluetoothResult.Error(BluetoothError.IO_ERROR)

            val inputStream = socket?.inputStream
                ?: return@withContext BluetoothResult.Error(BluetoothError.IO_ERROR)

            outputStream.write((command + "\r\n").toByteArray(Charsets.US_ASCII))
            outputStream.flush()

            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.US_ASCII))
            val response = reader.readLine()

            if (response == null) {
                BluetoothResult.Error(BluetoothError.IO_ERROR)
            } else {
                BluetoothResult.Success(response.trim())
            }
        } catch (e: IOException) {
            _state.value = ConnectionState.Error(BluetoothError.IO_ERROR, e.message ?: "I/O error during sendCommand")
            BluetoothResult.Error(BluetoothError.IO_ERROR)
        }
    }
