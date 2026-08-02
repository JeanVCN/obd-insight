package com.obd.insight.ui.terminal

import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.domain.model.BluetoothError
import com.obd.insight.domain.model.BluetoothResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AtTerminalViewModelTest {

    private val bluetoothManager: BluetoothConnectionManager = mockk(relaxUnitFun = true)
    private lateinit var viewModel: AtTerminalViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AtTerminalViewModel(bluetoothManager)
    }

    @Test
    fun `sendCommand adds entry on success`() = runTest {
        coEvery { bluetoothManager.sendCommand("ATI") } returns BluetoothResult.Success("ELM327 v1.5")

        viewModel.sendCommand("ATI")

        val entries = viewModel.entries.value
        assertEquals(1, entries.size)
        assertEquals("ATI", entries[0].command)
        assertEquals("ELM327 v1.5", entries[0].response)
        assertFalse(entries[0].isError)
    }

    @Test
    fun `sendCommand adds error entry on failure`() = runTest {
        coEvery { bluetoothManager.sendCommand("ATZ") } returns BluetoothResult.Error(BluetoothError.IO_ERROR)

        viewModel.sendCommand("ATZ")

        val entries = viewModel.entries.value
        assertEquals(1, entries.size)
        assertEquals("ATZ", entries[0].command)
        assertTrue(entries[0].isError)
        assertTrue(entries[0].response.contains("IO_ERROR"))
    }

    @Test
    fun `sendCommand ignores empty input`() {
        viewModel.sendCommand("   ")

        assertTrue(viewModel.entries.value.isEmpty())
    }

    @Test
    fun `clear removes all entries`() = runTest {
        coEvery { bluetoothManager.sendCommand(any()) } returns BluetoothResult.Success("OK")

        viewModel.sendCommand("ATZ")
        viewModel.sendCommand("ATE0")
        assertEquals(2, viewModel.entries.value.size)

        viewModel.clear()

        assertTrue(viewModel.entries.value.isEmpty())
    }

    @Test
    fun `sendCommand sets isSending during request`() = runTest {
        coEvery { bluetoothManager.sendCommand("ATI") } returns BluetoothResult.Success("OK")

        assertFalse(viewModel.isSending.value)
        viewModel.sendCommand("ATI")
        assertFalse(viewModel.isSending.value)
    }

    @Test
    fun `sendCommand sends trimmed command`() = runTest {
        coEvery { bluetoothManager.sendCommand(any()) } returns BluetoothResult.Success("OK")

        viewModel.sendCommand("  ATI  ")

        coVerify { bluetoothManager.sendCommand("ATI") }
    }
}
