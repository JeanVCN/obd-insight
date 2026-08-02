package com.obd.insight.domain.model

import org.junit.Test
import kotlin.test.assertEquals

class ProtocolTypeTest {

    @Test
    fun `fromNumber maps 1 to J1850Pwm`() {
        assertEquals(ProtocolType.J1850Pwm, ProtocolType.fromNumber(1))
    }

    @Test
    fun `fromNumber maps 6 to Can11bit500k`() {
        assertEquals(ProtocolType.Can11bit500k, ProtocolType.fromNumber(6))
    }

    @Test
    fun `fromNumber maps 10 to J1939`() {
        assertEquals(ProtocolType.J1939, ProtocolType.fromNumber(10))
    }

    @Test
    fun `fromNumber maps unknown to Unknown`() {
        assertEquals(ProtocolType.Unknown, ProtocolType.fromNumber(99))
    }

    @Test
    fun `fromDescription matches J1850 PWM`() {
        assertEquals(ProtocolType.J1850Pwm, ProtocolType.fromDescription("SAE J1850 PWM"))
    }

    @Test
    fun `fromDescription matches J1850 VPW`() {
        assertEquals(ProtocolType.J1850Vpw, ProtocolType.fromDescription("SAE J1850 VPW"))
    }

    @Test
    fun `fromDescription matches ISO 9141-2`() {
        assertEquals(ProtocolType.Iso9141_2, ProtocolType.fromDescription("ISO 9141-2"))
    }

    @Test
    fun `fromDescription matches KWP2000 fast`() {
        assertEquals(ProtocolType.Kwp2000Fast, ProtocolType.fromDescription("ISO 14230-4 (KWP2000 fast)"))
    }

    @Test
    fun `fromDescription matches KWP2000 slow`() {
        assertEquals(ProtocolType.Kwp2000Slow, ProtocolType.fromDescription("ISO 14230-4 (KWP2000 slow)"))
    }

    @Test
    fun `fromDescription matches CAN 11-bit 500k`() {
        assertEquals(ProtocolType.Can11bit500k, ProtocolType.fromDescription("ISO 15765-4 CAN 11-bit 500k"))
    }

    @Test
    fun `fromDescription matches CAN 29-bit 500k`() {
        assertEquals(ProtocolType.Can29bit500k, ProtocolType.fromDescription("ISO 15765-4 CAN 29-bit 500k"))
    }

    @Test
    fun `fromDescription matches CAN 11-bit 250k`() {
        assertEquals(ProtocolType.Can11bit250k, ProtocolType.fromDescription("ISO 15765-4 CAN 11-bit 250k"))
    }

    @Test
    fun `fromDescription matches CAN 29-bit 250k`() {
        assertEquals(ProtocolType.Can29bit250k, ProtocolType.fromDescription("ISO 15765-4 CAN 29-bit 250k"))
    }

    @Test
    fun `fromDescription matches J1939`() {
        assertEquals(ProtocolType.J1939, ProtocolType.fromDescription("SAE J1939 CAN 29-bit 250k"))
    }

    @Test
    fun `fromDescription falls back to Unknown`() {
        assertEquals(ProtocolType.Unknown, ProtocolType.fromDescription("GARBAGE PROTOCOL"))
    }

    @Test
    fun `fromDescription matches by description prefix`() {
        assertEquals(ProtocolType.Can11bit500k, ProtocolType.fromDescription("AUTO, ISO 15765-4 CAN 11-bit 500k"))
    }
}
