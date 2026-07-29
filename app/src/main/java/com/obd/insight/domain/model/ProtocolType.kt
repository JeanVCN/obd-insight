package com.obd.insight.domain.model

sealed class ProtocolType(val number: Int, val description: String) {
    data object J1850Pwm : ProtocolType(1, "SAE J1850 PWM")
    data object J1850Vpw : ProtocolType(2, "SAE J1850 VPW")
    data object Iso9141_2 : ProtocolType(3, "ISO 9141-2")
    data object Kwp2000Slow : ProtocolType(4, "ISO 14230-4 (KWP2000 slow)")
    data object Kwp2000Fast : ProtocolType(5, "ISO 14230-4 (KWP2000 fast)")
    data object Can11bit500k : ProtocolType(6, "ISO 15765-4 CAN 11-bit 500k")
    data object Can29bit500k : ProtocolType(7, "ISO 15765-4 CAN 29-bit 500k")
    data object Can11bit250k : ProtocolType(8, "ISO 15765-4 CAN 11-bit 250k")
    data object Can29bit250k : ProtocolType(9, "ISO 15765-4 CAN 29-bit 250k")
    data object J1939 : ProtocolType(10, "SAE J1939 CAN 29-bit 250k")
    data object Unknown : ProtocolType(0, "Unknown")

    companion object {
        fun fromNumber(n: Int): ProtocolType = when (n) {
            1 -> J1850Pwm
            2 -> J1850Vpw
            3 -> Iso9141_2
            4 -> Kwp2000Slow
            5 -> Kwp2000Fast
            6 -> Can11bit500k
            7 -> Can29bit500k
            8 -> Can11bit250k
            9 -> Can29bit250k
            10 -> J1939
            else -> Unknown
        }

        fun fromDescription(desc: String): ProtocolType {
            val lower = desc.lowercase()
            return when {
                lower.contains("j1850") && lower.contains("pwm") -> J1850Pwm
                lower.contains("j1850") && lower.contains("vpw") -> J1850Vpw
                lower.contains("9141") -> Iso9141_2
                lower.contains("kwp") || lower.contains("14230") -> {
                    if (lower.contains("fast")) Kwp2000Fast else Kwp2000Slow
                }
                lower.contains("15765") || lower.contains("can") -> {
                    when {
                        lower.contains("29") || lower.contains("29bit") -> {
                            if (lower.contains("250")) Can29bit250k else Can29bit500k
                        }
                        lower.contains("250") -> Can11bit250k
                        else -> Can11bit500k
                    }
                }
                lower.contains("j1939") -> J1939
                else -> Unknown
            }
        }
    }
}
