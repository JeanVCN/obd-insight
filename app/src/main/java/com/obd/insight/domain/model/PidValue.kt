package com.obd.insight.domain.model

data class PidValue(
    val pid: Int,
    val value: Float,
    val unit: String,
    val label: String
)
