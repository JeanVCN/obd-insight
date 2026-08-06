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
        val supported = mutableListOf<Int>()
        var blockPid = 0

        while (true) {
            when (val result = requestPid(1, blockPid)) {
                is BluetoothResult.Error -> {
                    if (blockPid == 0) return result
                    return BluetoothResult.Success(supported.distinct().sorted())
                }
                is BluetoothResult.Success -> {
                    val data = result.data.data.take(4)
                    if (data.size < 4) {
                        if (blockPid == 0) return BluetoothResult.Error(BluetoothError.PROTOCOL_ERROR)
                        return BluetoothResult.Success(supported.distinct().sorted())
                    }

                    supported += decodePidBitmask(data, blockPid + 1)
                    if ((data[3] and 0x01) == 0) {
                        return BluetoothResult.Success(supported.distinct().sorted())
                    }
                    blockPid += 0x20
                }
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
        return BluetoothResult.Success(
            ObdResponse(
                mode = requestedMode,
                pid = requestedPid,
                data = data,
                rawData = hexTokens.joinToString(" ")
            )
        )
    }

    private fun decodePidBitmask(data: List<Int>, firstPid: Int): List<Int> {
        val pids = mutableListOf<Int>()
        for (byteIndex in data.indices) {
            val byte = data[byteIndex]
            for (bit in 0..7) {
                if ((byte shr (7 - bit)) and 1 == 1) {
                    pids.add(firstPid + byteIndex * 8 + bit)
                }
            }
        }
        return pids
    }
}
