package com.obd.insight.domain.model

data class TripSummary(
    val trip: Trip,
    val readingCount: Int,
    val maxRpm: Float?,
    val averageSpeed: Float?,
    val maxCoolantTemperature: Float?
)
