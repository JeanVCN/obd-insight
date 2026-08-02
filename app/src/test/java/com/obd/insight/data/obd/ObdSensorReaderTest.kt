package com.obd.insight.data.obd

import com.obd.insight.data.elm327.Elm327Protocol
import com.obd.insight.data.elm327.Elm327Response
import com.obd.insight.domain.model.BluetoothResult
import com.obd.insight.domain.model.ObdResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObdSensorReaderTest {

    private val pidReader: ObdPidReader = mockk()
    private val reader = ObdSensorReader(pidReader)

    @Test
    fun `readSensorValues emits converted values for each pid`() = runTest {
        val rpmResponse = ObdResponse(1, 0x0C, listOf(0x1A, 0xF8))
        val speedResponse = ObdResponse(1, 0x0D, listOf(0x50))
        val coolantResponse = ObdResponse(1, 0x05, listOf(0x64))

        coEvery { pidReader.requestPid(1, 0x0C) } returns BluetoothResult.Success(rpmResponse)
        coEvery { pidReader.requestPid(1, 0x0D) } returns BluetoothResult.Success(speedResponse)
        coEvery { pidReader.requestPid(1, 0x05) } returns BluetoothResult.Success(coolantResponse)

        val values = reader.readSensorValues(listOf(0x0C, 0x0D, 0x05)).first()

        assertEquals(3, values.size)
        assertEquals(1726f, values[0].value) // RPM
        assertEquals(80f, values[1].value)   // Speed
        assertEquals(60f, values[2].value)   // Coolant
    }

    @Test
    fun `readSensorValues skips failed pid requests`() = runTest {
        coEvery { pidReader.requestPid(1, 0x0C) } returns BluetoothResult.Error(
            com.obd.insight.domain.model.BluetoothError.IO_ERROR
        )
        val speedResponse = ObdResponse(1, 0x0D, listOf(0x50))
        coEvery { pidReader.requestPid(1, 0x0D) } returns BluetoothResult.Success(speedResponse)

        val values = reader.readSensorValues(listOf(0x0C, 0x0D)).first()

        assertEquals(1, values.size)
        assertEquals(80f, values[0].value)
    }
}
