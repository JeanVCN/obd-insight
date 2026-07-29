package com.obd.insight.data.elm327

import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.domain.model.BluetoothResult
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Elm327ProtocolTest {

    private val bluetoothManager: BluetoothConnectionManager = mockk()
    private val protocol = Elm327Protocol(bluetoothManager)

    @Test
    fun `initialize sends all AT commands in order`() = runTest {
        coEvery { bluetoothManager.sendCommand(any()) } returns BluetoothResult.Success("OK")

        val result = protocol.initialize()

        assertTrue(result is BluetoothResult.Success)
        coVerifySequence {
            bluetoothManager.sendCommand("ATZ")
            bluetoothManager.sendCommand("ATE0")
            bluetoothManager.sendCommand("ATL0")
            bluetoothManager.sendCommand("ATS0")
            bluetoothManager.sendCommand("ATH1")
            bluetoothManager.sendCommand("ATAT1")
            bluetoothManager.sendCommand("ATSP0")
        }
    }

    @Test
    fun `initialize returns error when any step fails`() = runTest {
        coEvery { bluetoothManager.sendCommand("ATZ") } returns BluetoothResult.Success("OK")
        coEvery { bluetoothManager.sendCommand("ATE0") } returns BluetoothResult.Error(
            com.obd.insight.domain.model.BluetoothError.IO_ERROR
        )

        val result = protocol.initialize()

        assertTrue(result is BluetoothResult.Error)
    }

    @Test
    fun `parse returns Raw for valid hex response`() {
        val response = protocol.parse("41 0C 1A F8")

        assertTrue(response is Elm327Response.Raw)
        assertEquals(listOf("41", "0C", "1A", "F8"), (response as Elm327Response.Raw).hexData)
    }

    @Test
    fun `parse returns NoData for NO DATA response`() {
        val response = protocol.parse("NO DATA")

        assertEquals(Elm327Response.NoData, response)
    }

    @Test
    fun `parse returns Error for ERROR response`() {
        val response = protocol.parse("ERROR")

        assertTrue(response is Elm327Response.Error)
    }

    @Test
    fun `parse returns Unknown for unrecognized response`() {
        val response = protocol.parse("some gibberish")

        assertEquals(Elm327Response.Unknown, response)
    }

    @Test
    fun `execute returns Error when bluetooth fails`() = runTest {
        coEvery { bluetoothManager.sendCommand(any()) } returns BluetoothResult.Error(
            com.obd.insight.domain.model.BluetoothError.IO_ERROR
        )

        val result = protocol.execute(Elm327Command.Reset)

        assertTrue(result is BluetoothResult.Error)
    }
}
