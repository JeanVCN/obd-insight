package com.obd.insight.data.elm327

import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.domain.model.BluetoothResult
import com.obd.insight.domain.model.ProtocolType

class Elm327Protocol(
    private val bluetoothManager: BluetoothConnectionManager
) {
    private var initialized = false
    private var _protocol: ProtocolType = ProtocolType.Unknown
    val protocol: ProtocolType get() = _protocol

    suspend fun initialize(): BluetoothResult<Unit> {
        val steps = listOf(
            Elm327Command.Reset,
            Elm327Command.EchoOff,
            Elm327Command.LinefeedsOff,
            Elm327Command.SpacesOff,
            Elm327Command.HeadersOn,
            Elm327Command.AdaptiveTimingAuto,
            Elm327Command.AutoProtocol
        )

        for (step in steps) {
            when (val result = execute(step)) {
                is BluetoothResult.Error -> return result
                is BluetoothResult.Success -> { }
            }
        }

        initialized = true
        return BluetoothResult.Success(Unit)
    }

    suspend fun detectProtocol(): BluetoothResult<ProtocolType> {
        when (val dpnResult = bluetoothManager.sendCommand("ATDPN")) {
            is BluetoothResult.Error -> return dpnResult
            is BluetoothResult.Success -> {
                val dpn = dpnResult.data.trim()
                val number = dpn.filter { it.isDigit() }.takeWhile { it.isDigit() }.toIntOrNull()
                if (number != null) {
                    _protocol = ProtocolType.fromNumber(number)
                    return BluetoothResult.Success(_protocol)
                }
            }
        }

        when (val dpResult = bluetoothManager.sendCommand("ATDP")) {
            is BluetoothResult.Error -> return dpResult
            is BluetoothResult.Success -> {
                _protocol = ProtocolType.fromDescription(dpResult.data.trim())
                return BluetoothResult.Success(_protocol)
            }
        }
    }

    suspend fun execute(command: Elm327Command): BluetoothResult<Elm327Response> {
        val rawResult = bluetoothManager.sendCommand(command.raw)
        return when (rawResult) {
            is BluetoothResult.Error -> BluetoothResult.Error(rawResult.reason)
            is BluetoothResult.Success -> BluetoothResult.Success(parse(rawResult.data))
        }
    }

    fun parse(response: String): Elm327Response {
        val trimmed = response.trim()
        if (trimmed.startsWith("?")) return Elm327Response.Error("UNKNOWN", "Command not recognized")
        if (trimmed.startsWith("NO DATA")) return Elm327Response.NoData
        if (trimmed.startsWith("UNABLE TO CONNECT")) return Elm327Response.Error("UNABLE_TO_CONNECT", trimmed)
        if (trimmed.startsWith("SEARCHING")) return Elm327Response.Error("SEARCHING", trimmed)
        if (trimmed.startsWith("STOPPED")) return Elm327Response.Error("STOPPED", trimmed)
        if (trimmed.startsWith("ERROR")) return Elm327Response.Error("ERROR", trimmed)

        val hexValues = trimmed.split("\\s+".toRegex()).filter { it.matches(Regex("^[0-9A-Fa-f]+$")) }
        return if (hexValues.isNotEmpty()) {
            Elm327Response.Raw(hexValues.map { it.uppercase() })
        } else {
            Elm327Response.Unknown
        }
    }
}
