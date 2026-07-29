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
    private val defaultPids = listOf(0x0C, 0x0D, 0x05)

    fun readSensorValues(pids: List<Int> = defaultPids): Flow<List<PidValue>> = flow {
        while (true) {
            val values = mutableListOf<PidValue>()
            for (pid in pids) {
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
