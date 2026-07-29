package com.obd.insight.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obd.insight.data.obd.ObdSensorReader
import com.obd.insight.domain.model.PidValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val sensorReader: ObdSensorReader
) : ViewModel() {

    private val _sensorValues = MutableStateFlow<List<PidValue>>(emptyList())
    val sensorValues: StateFlow<List<PidValue>> = _sensorValues.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var collecting = false

    fun startCollecting() {
        if (collecting) return
        collecting = true
        viewModelScope.launch {
            sensorReader.readSensorValues().collect { values ->
                _sensorValues.value = values
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        collecting = false
    }
}
