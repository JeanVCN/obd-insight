package com.obd.insight.ui.dashboard

import com.obd.insight.data.obd.ObdSensorReader
import com.obd.insight.domain.model.PidValue
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DashboardViewModelTest {

    private val sensorReader: ObdSensorReader = mockk()
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `startCollecting emits sensor values from reader`() = runTest {
        val rpm = PidValue(0x0C, 1726f, "rpm", "Engine RPM")
        val speed = PidValue(0x0D, 80f, "km/h", "Speed")
        val coolant = PidValue(0x05, 60f, "°C", "Coolant Temp")
        val mockValues = listOf(rpm, speed, coolant)

        every { sensorReader.readSensorValues() } returns flowOf(mockValues)

        viewModel = DashboardViewModel(sensorReader)
        viewModel.startCollecting()

        val values = viewModel.sensorValues.value
        assertEquals(3, values.size)
        assertEquals(1726f, values[0].value)
        assertEquals(80f, values[1].value)
        assertEquals(60f, values[2].value)
    }
}
