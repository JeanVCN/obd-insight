package com.obd.insight.data.obd

import com.obd.insight.domain.model.PidValue

object PidValueConverter {

    fun convert(pid: Int, data: List<Int>): PidValue? {
        if (data.isEmpty()) return null

        return when (pid) {
            0x00 -> null // supported PIDs bitmask — handled elsewhere
            0x01 -> PidValue(pid, data[0].toFloat(), "status", "Monitor Status")
            0x04 -> PidValue(pid, (data[0] * 100.0 / 255.0).toFloat(), "%", "Engine Load")
            0x05 -> PidValue(pid, (data[0] - 40).toFloat(), "°C", "Coolant Temp")
            0x06 -> PidValue(pid, (data[0] * 100.0 / 255.0).toFloat(), "%", "Fuel Trim 1")
            0x0B -> PidValue(pid, data[0].toFloat(), "kPa", "Intake Manifold Pressure")
            0x0C -> {
                if (data.size < 2) return null
                val rpm = ((data[0] * 256 + data[1]) / 4.0).toFloat()
                PidValue(pid, rpm, "rpm", "Engine RPM")
            }
            0x0D -> PidValue(pid, data[0].toFloat(), "km/h", "Speed")
            0x0F -> PidValue(pid, (data[0] - 40).toFloat(), "°C", "Intake Air Temp")
            0x10 -> {
                if (data.size < 2) return null
                PidValue(pid, ((data[0] * 256 + data[1]) / 100.0).toFloat(), "g/s", "MAF")
            }
            0x11 -> PidValue(pid, (data[0] * 100.0 / 255.0).toFloat(), "%", "Throttle Position")
            0x21 -> {
                if (data.size < 2) return null
                PidValue(pid, ((data[0] * 256 + data[1]) / 10.0).toFloat(), "km", "Distance MIL On")
            }
            0x2F -> {
                if (data.size < 2) return null
                PidValue(pid, ((data[0] * 256 + data[1]) / 10.0).toFloat(), "%", "Fuel Level")
            }
            0x46 -> PidValue(pid, (data[0] - 40).toFloat(), "°C", "Ambient Air Temp")
            0x5C -> PidValue(pid, (data[0] * 100.0 / 255.0).toFloat(), "%", "Engine Oil Temp")
            else -> null
        }
    }
}
