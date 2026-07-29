package com.obd.insight.domain.model

data class ObdResponse(
    val mode: Int,
    val pid: Int,
    val data: List<Int>
)
