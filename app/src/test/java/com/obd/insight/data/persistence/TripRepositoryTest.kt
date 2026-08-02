package com.obd.insight.data.persistence

import com.obd.insight.domain.model.PidValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class TripRepositoryTest {
    private val tripDao: TripDao = mockk(relaxed = true)
    private val readingDao: SensorReadingDao = mockk(relaxed = true)
    private val repository = TripRepository(tripDao, readingDao) { 1_000L }

    @Test
    fun `startTrip persists an active trip with current time`() = runTest {
        coEvery { tripDao.insert(any()) } returns 42L

        val trip = repository.startTrip()

        assertEquals(42L, trip.id)
        assertEquals(1_000L, trip.startedAt)
        assertEquals(true, trip.isRecording)
        coVerify { tripDao.insert(TripEntity(startedAt = 1_000L)) }
    }

    @Test
    fun `recordValues persists every sensor value for the trip`() = runTest {
        val values = listOf(
            PidValue(0x0C, 1726f, "rpm", "Engine RPM"),
            PidValue(0x0D, 80f, "km/h", "Speed")
        )

        repository.recordValues(7L, values)

        coVerify {
            readingDao.insertAll(match { readings ->
                readings.size == 2 &&
                    readings.all { it.tripId == 7L && it.recordedAt == 1_000L }
            })
        }
    }

    @Test
    fun `finishTrip records the current time`() = runTest {
        repository.finishTrip(7L)

        coVerify { tripDao.finish(7L, 1_000L) }
    }
}
