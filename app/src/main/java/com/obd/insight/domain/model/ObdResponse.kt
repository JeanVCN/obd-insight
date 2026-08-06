package com.obd.insight.domain.model

data class ObdResponse(
    val mode: Int,
    val pid: Int,
    val data: List<Int>,
    val rawData: String = data.joinToString(" ") { it.toString(16).padStart(2, '0').uppercase() }
)
