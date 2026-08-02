package com.obd.insight.data.obd

import com.obd.insight.data.elm327.Elm327Response
import com.obd.insight.domain.model.BluetoothResult
import com.obd.insight.domain.model.PidValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ObdSensorReader(
    private val pidReader: ObdPidReader
) {
    private val defaultPids = listOf(
        0x04, 0x05, 0x06, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x21, 0x2F, 0x31, 0x42, 0x45, 0x46, 0x49,
        0x4A, 0x4B, 0x4C, 0x5A, 0x5C, 0x5E
    )
    private val fallbackPids = listOf(0x0C, 0x0D, 0x05)
    private var supportedPids: Set<Int>? = null

    fun setSupportedPids(pids: List<Int>) {
        supportedPids = pids.toSet()
    }

    fun clearSupportedPids() {
        supportedPids = null
    }

    fun readSensorValues(pids: List<Int>? = null): Flow<List<PidValue>> = flow {
        val requestedPids = pids ?: supportedPids
            ?.let { supported -> defaultPids.filter { it in supported } }
            ?.ifEmpty { fallbackPids }
            ?: fallbackPids

        while (true) {
            val values = mutableListOf<PidValue>()
            for (pid in requestedPids) {
                val requestPid = pidReader.requestPid(1, pid)
                if (requestPid is BluetoothResult.Success) {
                    val data = requestPid.data.data
                    val converted = PidValueConverter.convert(pid, data)
                    if (converted != null) {
                        values.add(converted)
                    }
                }
            }
            if (values.isNotEmpty()) {
                emit(values)
            }
            delay(1000)
        }
    }
}
