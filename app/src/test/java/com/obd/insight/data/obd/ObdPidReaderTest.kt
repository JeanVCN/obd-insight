package com.obd.insight.data.obd

import com.obd.insight.data.elm327.Elm327Command
import com.obd.insight.data.elm327.Elm327Protocol
import com.obd.insight.data.elm327.Elm327Response
import com.obd.insight.domain.model.BluetoothError
import com.obd.insight.domain.model.BluetoothResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObdPidReaderTest {

    private val protocol: Elm327Protocol = mockk()
    private val reader = ObdPidReader(protocol)

    @Test
    fun `requestSupportedPids returns list of supported PIDs from bitmask`() = runTest {
        val response = Elm327Response.Raw(listOf("41", "00", "BE", "1F", "B8", "11"))
        coEvery { protocol.execute(Elm327Command.ReadPid(1, 0)) } returns BluetoothResult.Success(response)
        coEvery { protocol.execute(Elm327Command.ReadPid(1, 0x20)) } returns BluetoothResult.Error(
            BluetoothError.PROTOCOL_ERROR
        )

        val result = reader.requestSupportedPids()

        assertTrue(result is BluetoothResult.Success)
        val pids = (result as BluetoothResult.Success).data
        assertEquals(listOf(1, 3, 4, 5, 6, 7, 12, 13, 14, 15, 16, 17, 19, 20, 21, 28, 32), pids)
    }

    @Test
    fun `requestSupportedPids returns error when protocol fails`() = runTest {
        coEvery { protocol.execute(any()) } returns BluetoothResult.Error(BluetoothError.IO_ERROR)

        val result = reader.requestSupportedPids()

        assertTrue(result is BluetoothResult.Error)
    }

    @Test
    fun `requestSupportedPids returns error when response has no data`() = runTest {
        coEvery { protocol.execute(any()) } returns BluetoothResult.Success(Elm327Response.NoData)

        val result = reader.requestSupportedPids()

        assertTrue(result is BluetoothResult.Error)
    }

    @Test
    fun `requestPid parses response with 3 header bytes`() = runTest {
        val response = Elm327Response.Raw(listOf("48", "6B", "10", "41", "00", "BE", "1F", "B8", "11"))
        coEvery { protocol.execute(Elm327Command.ReadPid(1, 0)) } returns BluetoothResult.Success(response)

        val result = reader.requestPid(1, 0)

        assertTrue(result is BluetoothResult.Success)
        val obdResponse = (result as BluetoothResult.Success).data
        assertEquals(1, obdResponse.mode)
        assertEquals(0, obdResponse.pid)
        assertEquals(listOf(0xBE, 0x1F, 0xB8, 0x11), obdResponse.data)
    }

    @Test
    fun `requestPid parses response without header bytes`() = runTest {
        val response = Elm327Response.Raw(listOf("41", "00", "BE", "1F", "B8", "11"))
        coEvery { protocol.execute(Elm327Command.ReadPid(1, 0)) } returns BluetoothResult.Success(response)

        val result = reader.requestPid(1, 0)

        assertTrue(result is BluetoothResult.Success)
        val obdResponse = (result as BluetoothResult.Success).data
        assertEquals(1, obdResponse.mode)
        assertEquals(0, obdResponse.pid)
        assertEquals(listOf(0xBE, 0x1F, 0xB8, 0x11), obdResponse.data)
    }

    @Test
    fun `requestPid returns error when response has wrong pid`() = runTest {
        val response = Elm327Response.Raw(listOf("41", "01", "00"))
        coEvery { protocol.execute(Elm327Command.ReadPid(1, 0)) } returns BluetoothResult.Success(response)

        val result = reader.requestPid(1, 0)

        assertTrue(result is BluetoothResult.Error)
    }
}
