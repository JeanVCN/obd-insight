package com.obd.insight.domain.model

sealed class BluetoothResult<out T> {
    data class Success<T>(val data: T) : BluetoothResult<T>()
    data class Error(val reason: BluetoothError) : BluetoothResult<Nothing>()
}