package com.obd.insight.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, SensorReadingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ObdDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun sensorReadingDao(): SensorReadingDao
}
