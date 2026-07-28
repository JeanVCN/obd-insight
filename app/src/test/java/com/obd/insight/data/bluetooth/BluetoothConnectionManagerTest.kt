package com.obd.insight.data.bluetooth

import com.obd.insight.domain.model.BluetoothError
import com.obd.insight.domain.model.ConnectionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BluetoothConnectionManagerTest {

    private val manager = BluetoothConnectionManager(ioDispatcher = Dispatchers.Unconfined)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        manager.disconnect()
    }

    @Test
    fun `state starts as Disconnected`() = runTest {
        assertEquals(ConnectionState.Disconnected, manager.state.first())
    }

    @Test
    fun `getPairedDevices returns empty list when adapter has no bonded devices`() {
        assertTrue(manager.getPairedDevices().isEmpty())
    }

    @Test
    fun `sendCommand returns IO_ERROR when socket is null`() = runTest {
        val result = manager.sendCommand("ATZ")
        assertTrue(result is com.obd.insight.domain.model.BluetoothResult.Error)
        assertEquals(BluetoothError.IO_ERROR, (result as com.obd.insight.domain.model.BluetoothResult.Error).reason)
    }

    @Test
    fun `sendCommand writes command and reads response`() = runTest {
        val outputStream = ByteArrayOutputStream()
        val inputStream = ByteArrayInputStream("OK\r\n".toByteArray(Charsets.US_ASCII))

        val mockSocket = mockk<android.bluetooth.BluetoothSocket> {
            every { outputStream } returns outputStream
            every { inputStream } returns inputStream
            every { isConnected } returns true
        }

        val managerWithMock = BluetoothConnectionManager(ioDispatcher = Dispatchers.Unconfined)
        val field = BluetoothConnectionManager::class.java.getDeclaredField("socket")
        field.isAccessible = true
        field.set(managerWithMock, mockSocket)

        val result = managerWithMock.sendCommand("ATZ")

        assertTrue(result is com.obd.insight.domain.model.BluetoothResult.Success)
        assertEquals("OK", (result as com.obd.insight.domain.model.BluetoothResult.Success).data)
        assertEquals("ATZ\r\n", outputStream.toString(Charsets.US_ASCII.name()))
    }
}
