package com.obd.insight.data.elm327

sealed class Elm327Response {
    data class Raw(val hexData: List<String>) : Elm327Response()
    data class Error(val code: String, val message: String) : Elm327Response()
    data object NoData : Elm327Response()
    data object Unknown : Elm327Response()
}