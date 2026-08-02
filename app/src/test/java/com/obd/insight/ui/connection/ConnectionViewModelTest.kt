package com.obd.insight.ui.connection

import com.obd.insight.data.bluetooth.BluetoothConnectionManager
import com.obd.insight.data.elm327.Elm327Protocol
import com.obd.insight.data.obd.ObdPidReader
import com.obd.insight.domain.model.BluetoothResult
import com.obd.insight.domain.model.ConnectionState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionViewModelTest {

    private val bluetoothManager: BluetoothConnectionManager = mockk(relaxUnitFun = true)
    private val elm327Protocol: Elm327Protocol = mockk(relaxUnitFun = true)
    private val obdPidReader: ObdPidReader = mockk(relaxUnitFun = true)
    private lateinit var viewModel: ConnectionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { bluetoothManager.state } returns MutableStateFlow(ConnectionState.Disconnected)
        every { bluetoothManager.discoveredDevices } returns MutableStateFlow(emptyList())
        every { bluetoothManager.getPairedDevices() } returns emptyList()
        every { bluetoothManager.isBluetoothAvailable() } returns true
        every { bluetoothManager.isBluetoothEnabled() } returns true
        every { bluetoothManager.startDiscovery() } returns true
        viewModel = ConnectionViewModel(bluetoothManager, elm327Protocol, obdPidReader)
    }

    @Test
    fun `scanDevices updates state to FoundDevices`() {
        viewModel.scanDevices()

        assertEquals(ConnectionState.FoundDevices(emptyList()), viewModel.state.value)
    }

    @Test
    fun `scanDevices calls getPairedDevices`() {
        viewModel.scanDevices()

        verify { bluetoothManager.getPairedDevices() }
    }

    @Test
    fun `disconnect calls bluetoothManager disconnect`() {
        viewModel.disconnect()

        verify { bluetoothManager.disconnect() }
    }

    @Test
    fun `scanDevices reports Bluetooth off`() {
        every { bluetoothManager.isBluetoothEnabled() } returns false

        viewModel.scanDevices()

        assertTrue(viewModel.state.value is ConnectionState.Error)
    }

    @Test
    fun `onPermissionsDenied reports permission error`() {
        viewModel.onPermissionsDenied()

        assertTrue(viewModel.state.value is ConnectionState.Error)
    }
}
