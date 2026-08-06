package com.obd.insight.data.obd

import com.obd.insight.domain.model.BluetoothResult
import com.obd.insight.domain.model.PidValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ObdSensorReader(
    private val pidReader: ObdPidReader,
    private val isConnected: () -> Boolean = { true }
) {
    private val fallbackPids = listOf(0x0C, 0x0D, 0x05)
    private var supportedPids: Set<Int>? = null

    fun setSupportedPids(pids: List<Int>) {
        supportedPids = pids.toSet()
    }

    fun clearSupportedPids() {
        supportedPids = null
    }

    fun readSensorValues(pids: List<Int>? = null): Flow<List<PidValue>> = flow {
        while (true) {
            if (!isConnected()) {
                delay(500)
                continue
            }
            val requestedPids = pids ?: supportedPids
                ?.let { supported -> supported.sorted() }
                ?.ifEmpty { fallbackPids }
                ?: fallbackPids
            val values = mutableListOf<PidValue>()
            for (pid in requestedPids) {
                try {
                    val requestPid = pidReader.requestPid(1, pid)
                    if (requestPid is BluetoothResult.Success) {
                        val response = requestPid.data
                        val converted = PidValueConverter.convert(pid, response.data)
                        values.add(
                            converted?.copy(rawData = response.rawData)
                                ?: PidValue(
                                    pid = pid,
                                    value = response.data.firstOrNull()?.toFloat() ?: 0f,
                                    unit = "raw",
                                    label = "PID 0x${pid.toString(16).padStart(2, '0').uppercase()}",
                                    rawData = response.rawData
                                )
                        )
                    }
                } catch (_: Exception) {
                    // A lost adapter must stop this poll cycle, not crash the UI.
                    break
                }
            }
            if (values.isNotEmpty()) {
                emit(values)
            }
            delay(1000)
        }
    }
}
