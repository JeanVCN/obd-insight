package com.obd.insight.data.obd

import com.obd.insight.domain.model.PidValue

object PidValueConverter {

    fun convert(pid: Int, data: List<Int>): PidValue? {
        if (data.isEmpty()) return null

        return when (pid) {
            0x00 -> null // supported PIDs bitmask — handled elsewhere
            0x01 -> PidValue(pid, data[0].toFloat(), "status", "Status dos monitores")
            0x04 -> PidValue(pid, (data[0] * 100.0 / 255.0).toFloat(), "%", "Carga do motor")
            0x05 -> PidValue(pid, (data[0] - 40).toFloat(), "°C", "Temperatura do líquido")
            0x06 -> PidValue(pid, (data[0] * 100.0 / 128.0 - 100.0).toFloat(), "%", "Correção de combustível curta B1")
            0x0A -> PidValue(pid, (data[0] * 3.0).toFloat(), "kPa", "Pressão de combustível")
            0x0B -> PidValue(pid, data[0].toFloat(), "kPa", "Pressão do coletor")
            0x0C -> {
                if (data.size < 2) return null
                val rpm = ((data[0] * 256 + data[1]) / 4.0).toFloat()
                PidValue(pid, rpm, "rpm", "Rotação do motor")
            }
            0x0D -> PidValue(pid, data[0].toFloat(), "km/h", "Velocidade")
            0x0E -> PidValue(pid, (data[0] / 2.0 - 64.0).toFloat(), "°", "Avanço de ignição")
            0x0F -> PidValue(pid, (data[0] - 40).toFloat(), "°C", "Temperatura do ar de admissão")
            0x10 -> {
                if (data.size < 2) return null
                PidValue(pid, ((data[0] * 256 + data[1]) / 100.0).toFloat(), "g/s", "Fluxo de ar (MAF)")
            }
            0x11 -> PidValue(pid, (data[0] * 100.0 / 255.0).toFloat(), "%", "Posição da borboleta")
            0x21 -> {
                if (data.size < 2) return null
                PidValue(pid, (data[0] * 256 + data[1]).toFloat(), "km", "Distância com MIL acesa")
            }
            0x2F -> PidValue(pid, (data[0] * 100.0 / 255.0).toFloat(), "%", "Nível de combustível")
            0x31 -> {
                if (data.size < 2) return null
                PidValue(pid, (data[0] * 256 + data[1]).toFloat(), "km", "Distância desde limpeza de DTC")
            }
            0x46 -> PidValue(pid, (data[0] - 40).toFloat(), "°C", "Temperatura ambiente")
            0x42 -> {
                if (data.size < 2) return null
                PidValue(pid, ((data[0] * 256 + data[1]) / 1000.0).toFloat(), "V", "Tensão do módulo")
            }
            0x45, 0x49, 0x4A, 0x4B, 0x4C, 0x5A ->
                PidValue(pid, (data[0] * 100.0 / 255.0).toFloat(), "%", pidLabel(pid))
            0x5C -> PidValue(pid, (data[0] - 40).toFloat(), "°C", "Temperatura do óleo")
            0x5E -> {
                if (data.size < 2) return null
                PidValue(pid, ((data[0] * 256 + data[1]) / 20.0).toFloat(), "L/h", "Taxa de combustível")
            }
            else -> null
        }
    }

    private fun pidLabel(pid: Int): String = when (pid) {
        0x45 -> "Posição relativa da borboleta"
        0x49 -> "Posição do pedal do acelerador D"
        0x4A -> "Posição do pedal do acelerador E"
        0x4B -> "Posição do pedal do acelerador F"
        0x4C -> "Atuador comandado da borboleta"
        0x5A -> "Posição relativa do pedal do acelerador"
        else -> "PID 0x${pid.toString(16).uppercase()}"
    }
}
