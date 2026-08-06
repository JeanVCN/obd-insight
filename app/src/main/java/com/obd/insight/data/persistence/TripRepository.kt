package com.obd.insight.data.persistence

import com.obd.insight.domain.model.PidValue
import com.obd.insight.domain.model.Trip
import com.obd.insight.domain.model.TripSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TripRepository(
    private val tripDao: TripDao,
    private val sensorReadingDao: SensorReadingDao,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun getUnfinishedTrip(): Trip? = tripDao.getUnfinishedTrip()?.toDomain()

    suspend fun getTrip(tripId: Long): Trip? = tripDao.getById(tripId)?.toDomain()

    suspend fun startTrip(): Trip {
        val startedAt = clock()
        val id = tripDao.insert(TripEntity(startedAt = startedAt))
        return Trip(id = id, startedAt = startedAt, endedAt = null, isRecording = true)
    }

    suspend fun resumeTrip(tripId: Long) = tripDao.setRecording(tripId, true)

    suspend fun pauseTrip(tripId: Long) = tripDao.setRecording(tripId, false)

    suspend fun finishTrip(tripId: Long) = tripDao.finish(tripId, clock())

    suspend fun getReadings(tripId: Long): List<SensorReadingEntity> =
        sensorReadingDao.getForTrip(tripId)

    suspend fun recordValues(tripId: Long, values: List<PidValue>) {
        if (values.isEmpty()) return
        val recordedAt = clock()
        sensorReadingDao.insertAll(values.map { value ->
            SensorReadingEntity(
                tripId = tripId,
                pid = value.pid,
                value = value.value,
                unit = value.unit,
                label = value.label,
                rawData = value.rawData,
                recordedAt = recordedAt
            )
        })
    }

    fun observeFinishedTrips(): Flow<List<TripSummary>> = sensorReadingDao.observeTripSummaries()
        .map { rows -> rows.map { it.toDomain() } }
}

private fun TripEntity.toDomain() = Trip(id, startedAt, endedAt, isRecording)

private fun TripSummaryRow.toDomain() = TripSummary(
    trip = Trip(id, startedAt, endedAt, isRecording),
    readingCount = readingCount,
    maxRpm = maxRpm,
    averageSpeed = averageSpeed,
    maxCoolantTemperature = maxCoolantTemperature
)
