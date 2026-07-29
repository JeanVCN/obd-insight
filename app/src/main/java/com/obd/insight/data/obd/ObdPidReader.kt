package com.obd.insight.data.obd

import com.obd.insight.data.elm327.Elm327Command
import com.obd.insight.data.elm327.Elm327Protocol
import com.obd.insight.data.elm327.Elm327Response
import com.obd.insight.domain.model.BluetoothError
import com.obd.insight.domain.model.BluetoothResult
import com.obd.insight.domain.model.ObdResponse

class ObdPidReader(
    private val protocol: Elm327Protocol
) {
    suspend fun requestPid(mode: Int, pid: Int): BluetoothResult<ObdResponse> {
        val result = protocol.execute(Elm327Command.ReadPid(mode, pid))
        return when (result) {
            is BluetoothResult.Error -> result
            is BluetoothResult.Success -> {
                val response = result.data
                when (response) {
                    is Elm327Response.Raw -> parsePidResponse(mode, pid, response.hexData)
                    is Elm327Response.Error -> BluetoothResult.Error(BluetoothError.PROTOCOL_ERROR)
                    is Elm327Response.NoData -> BluetoothResult.Error(BluetoothError.PROTOCOL_ERROR)
                    is Elm327Response.Unknown -> BluetoothResult.Error(BluetoothError.PROTOCOL_ERROR)
                }
            }
        }
    }

    suspend fun requestSupportedPids(): BluetoothResult<List<Int>> {
        val result = requestPid(1, 0)
        return when (result) {
            is BluetoothResult.Error -> result
            is BluetoothResult.Success -> {
                val data = result.data.data
                if (data.size < 4) return BluetoothResult.Error(BluetoothError.PROTOCOL_ERROR)
                BluetoothResult.Success(decodePidBitmask(data))
            }
        }
    }

    private fun parsePidResponse(
        requestedMode: Int,
        requestedPid: Int,
        hexTokens: List<String>
    ): BluetoothResult<ObdResponse> {
        val bytes = hexTokens.flatMap { token ->
            token.chunked(2).filter { it.length == 2 }.map { it.toInt(16) }
        }

        val expectedResponseByte = 0x40 + requestedMode
        val modeIndex = bytes.indexOf(expectedResponseByte)
        if (modeIndex == -1 || modeIndex + 1 >= bytes.size) {
            return BluetoothResult.Error(BluetoothError.PROTOCOL_ERROR)
        }

        val pidByte = bytes[modeIndex + 1]
        if (pidByte != requestedPid) {
            return BluetoothResult.Error(BluetoothError.PROTOCOL_ERROR)
        }

        val data = bytes.drop(modeIndex + 2)
        return BluetoothResult.Success(ObdResponse(requestedMode, requestedPid, data))
    }

    private fun decodePidBitmask(data: List<Int>): List<Int> {
        val pids = mutableListOf<Int>()
        for (byteIndex in data.indices) {
            val byte = data[byteIndex]
            for (bit in 0..7) {
                if ((byte shr (7 - bit)) and 1 == 1) {
                    pids.add(byteIndex * 8 + bit + 1)
                }
            }
        }
        return pids
    }
}
