package com.obd.insight.data.elm327

import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.domain.model.BluetoothResult
import com.obd.insight.domain.model.ProtocolType

class Elm327Protocol(
    private val bluetoothManager: BluetoothConnectionManager
) {
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
            val result = execute(step)
            when (result) {
                is BluetoothResult.Error -> return result
                is BluetoothResult.Success -> {
                    if (result.data is Elm327Response.Error) {
                        val error = result.data
                        return BluetoothResult.Error(error.mappedError)
                    }
                }
            }
        }

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
        if (trimmed.isEmpty()) return Elm327Response.Unknown

        val hexValues = trimmed.split("\\s+".toRegex())
            .filter { it.matches(Regex("^[0-9A-Fa-f]+$")) }
            .flatMap(::normalizeHexFrame)

        if (hexValues.isNotEmpty()) {
            return Elm327Response.Raw(hexValues.map { it.uppercase() })
        }

        return when {
            trimmed.startsWith("?") -> Elm327Response.Error("UNKNOWN", "Command not recognized")
            trimmed.startsWith("NO DATA") -> Elm327Response.NoData
            trimmed.startsWith("UNABLE TO CONNECT") -> Elm327Response.Error("UNABLE_TO_CONNECT", trimmed)
            trimmed.startsWith("SEARCHING") -> Elm327Response.Error("SEARCHING", trimmed)
            trimmed.startsWith("STOPPED") -> Elm327Response.Error("STOPPED", trimmed)
            trimmed.startsWith("ERROR") -> Elm327Response.Error("ERROR", trimmed)
            else -> Elm327Response.Unknown
        }
    }
}

private fun normalizeHexFrame(token: String): List<String> {
    if (token.length >= 5 && token.length % 2 == 1) {
        val payload = token.drop(3).chunked(2)
        if (payload.isNotEmpty()) {
            // With headers enabled, ELM327 emits a 3-digit CAN ID followed by frame length.
            return payload.drop(1)
        }
    }
    return token.chunked(2).filter { it.length == 2 }
}

private val Elm327Response.Error.mappedError: com.obd.insight.domain.model.BluetoothError
    get() = when (code) {
        "UNABLE_TO_CONNECT", "SEARCHING", "STOPPED" -> com.obd.insight.domain.model.BluetoothError.SOCKET_ERROR
        else -> com.obd.insight.domain.model.BluetoothError.PROTOCOL_ERROR
    }
