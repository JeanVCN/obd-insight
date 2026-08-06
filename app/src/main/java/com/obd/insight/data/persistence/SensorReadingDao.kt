package com.obd.insight.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorReadingDao {
    @Insert
    suspend fun insertAll(readings: List<SensorReadingEntity>)

    @Query("SELECT * FROM sensor_readings WHERE tripId = :tripId ORDER BY recordedAt ASC, id ASC")
    suspend fun getForTrip(tripId: Long): List<SensorReadingEntity>

    @Query("""
        SELECT trips.id, trips.startedAt, trips.endedAt, trips.isRecording,
               COUNT(sensor_readings.id) AS readingCount,
               MAX(CASE WHEN sensor_readings.pid = 12 THEN sensor_readings.value END) AS maxRpm,
               AVG(CASE WHEN sensor_readings.pid = 13 THEN sensor_readings.value END) AS averageSpeed,
               MAX(CASE WHEN sensor_readings.pid = 5 THEN sensor_readings.value END) AS maxCoolantTemperature
        FROM trips LEFT JOIN sensor_readings ON trips.id = sensor_readings.tripId
        WHERE trips.endedAt IS NOT NULL
        GROUP BY trips.id
        ORDER BY trips.startedAt DESC
    """)
    fun observeTripSummaries(): Flow<List<TripSummaryRow>>
}

data class TripSummaryRow(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val isRecording: Boolean,
    val readingCount: Int,
    val maxRpm: Float?,
    val averageSpeed: Float?,
    val maxCoolantTemperature: Float?
)
