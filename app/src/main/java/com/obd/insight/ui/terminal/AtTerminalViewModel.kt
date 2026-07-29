package com.obd.insight.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.domain.model.BluetoothResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AtCommandEntry(
    val command: String,
    val response: String,
    val isError: Boolean
)

class AtTerminalViewModel(
    private val bluetoothManager: BluetoothConnectionManager
) : ViewModel() {

    private val _entries = MutableStateFlow<List<AtCommandEntry>>(emptyList())
    val entries: StateFlow<List<AtCommandEntry>> = _entries.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun sendCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        _isSending.value = true

        viewModelScope.launch {
            val result = bluetoothManager.sendCommand(trimmed)
            val entry = when (result) {
                is BluetoothResult.Success -> AtCommandEntry(
                    command = trimmed,
                    response = result.data,
                    isError = false
                )
                is BluetoothResult.Error -> AtCommandEntry(
                    command = trimmed,
                    response = "Error: ${result.reason.name}",
                    isError = true
                )
            }
            _entries.value = _entries.value + entry
            _isSending.value = false
        }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
