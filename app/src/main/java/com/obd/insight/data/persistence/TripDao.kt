package com.obd.insight.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Query("SELECT * FROM trips WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getUnfinishedTrip(): TripEntity?

    @Query("UPDATE trips SET isRecording = :isRecording WHERE id = :tripId")
    suspend fun setRecording(tripId: Long, isRecording: Boolean)

    @Query("UPDATE trips SET isRecording = 0, endedAt = :endedAt WHERE id = :tripId")
    suspend fun finish(tripId: Long, endedAt: Long)

    @Query("SELECT * FROM trips WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinishedTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getById(tripId: Long): TripEntity?
}
