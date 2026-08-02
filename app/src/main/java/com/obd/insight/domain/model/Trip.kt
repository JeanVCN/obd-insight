package com.obd.insight.domain.model

data class Trip(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val isRecording: Boolean
)
