package com.obd.insight.data.obd

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PidValueConverterTest {

    @Test
    fun `convert RPM from hex data`() {
        // 0x0C = RPM, data = [0x1A, 0xF8] → (0x1A * 256 + 0xF8) / 4 = (6656 + 248) / 4 = 1726
        val result = PidValueConverter.convert(0x0C, listOf(0x1A, 0xF8))
        assertNotNull(result)
        assertEquals(1726f, result.value)
        assertEquals("rpm", result.unit)
        assertEquals("Rotação do motor", result.label)
    }

    @Test
    fun `convert coolant temp`() {
        // 0x05 = Coolant Temp, data = [0x64] → 100 - 40 = 60°C
        val result = PidValueConverter.convert(0x05, listOf(0x64))
        assertNotNull(result)
        assertEquals(60f, result.value)
        assertEquals("°C", result.unit)
    }

    @Test
    fun `convert speed`() {
        // 0x0D = Speed, data = [0x50] → 80 km/h
        val result = PidValueConverter.convert(0x0D, listOf(0x50))
        assertNotNull(result)
        assertEquals(80f, result.value)
        assertEquals("km/h", result.unit)
    }

    @Test
    fun `convert engine load`() {
        // 0x04 = Engine Load, data = [0x80] → 128 * 100 / 255 = 50.2%
        val result = PidValueConverter.convert(0x04, listOf(0x80))
        assertNotNull(result)
        assertEquals(50.2f, result.value, 0.1f)
        assertEquals("%", result.unit)
    }

    @Test
    fun `convert MAF`() {
        // 0x10 = MAF, data = [0x03, 0xE8] → 0x03E8 / 100 = 10.0 g/s
        val result = PidValueConverter.convert(0x10, listOf(0x03, 0xE8))
        assertNotNull(result)
        assertEquals(10.0f, result.value, 0.1f)
        assertEquals("g/s", result.unit)
    }

    @Test
    fun `convert throttle position`() {
        // 0x11 = Throttle Position, data = [0x4C] → 76 * 100 / 255 = 29.8%
        val result = PidValueConverter.convert(0x11, listOf(0x4C))
        assertNotNull(result)
        assertEquals(29.8f, result.value, 0.1f)
        assertEquals("%", result.unit)
    }

    @Test
    fun `convert short fuel trim using signed formula`() {
        val result = PidValueConverter.convert(0x06, listOf(0x80))

        assertNotNull(result)
        assertEquals(0f, result.value, 0.1f)
        assertEquals("Correção de combustível curta B1", result.label)
    }

    @Test
    fun `convert fuel level from one byte`() {
        val result = PidValueConverter.convert(0x2F, listOf(0x80))

        assertNotNull(result)
        assertEquals(50.2f, result.value, 0.1f)
        assertEquals("%", result.unit)
    }

    @Test
    fun `convert oil temperature`() {
        val result = PidValueConverter.convert(0x5C, listOf(0x64))

        assertNotNull(result)
        assertEquals(60f, result.value)
        assertEquals("°C", result.unit)
    }

    @Test
    fun `convert control module voltage`() {
        val result = PidValueConverter.convert(0x42, listOf(0x34, 0xB0))

        assertNotNull(result)
        assertEquals(13.488f, result.value, 0.01f)
        assertEquals("V", result.unit)
    }

    @Test
    fun `convert returns null for unsupported pid`() {
        val result = PidValueConverter.convert(0xFF, listOf(0x00))
        assertNull(result)
    }

    @Test
    fun `convert returns null for pid 0x00`() {
        val result = PidValueConverter.convert(0x00, listOf(0xBE, 0x1F, 0xB8, 0x11))
        assertNull(result)
    }

    @Test
    fun `convert returns null for RPM with insufficient data`() {
        val result = PidValueConverter.convert(0x0C, listOf(0x1A))
        assertNull(result)
    }
}
