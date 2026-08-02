package com.obd.insight.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obd.insight.data.obd.ObdSensorReader
import com.obd.insight.data.persistence.TripRepository
import com.obd.insight.domain.model.PidValue
import com.obd.insight.domain.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val sensorReader: ObdSensorReader,
    private val tripRepository: TripRepository? = null
) : ViewModel() {

    private val _sensorValues = MutableStateFlow<List<PidValue>>(emptyList())
    val sensorValues: StateFlow<List<PidValue>> = _sensorValues.asStateFlow()

    private val _activeTrip = MutableStateFlow<Trip?>(null)
    val activeTrip: StateFlow<Trip?> = _activeTrip.asStateFlow()

    private var collecting = false

    fun startCollecting() {
        if (collecting) return
        collecting = true
        viewModelScope.launch {
            _activeTrip.value = tripRepository?.getUnfinishedTrip()
            sensorReader.readSensorValues().collect { values ->
                _sensorValues.value = values
                _activeTrip.value?.takeIf { it.isRecording }?.let { trip ->
                    tripRepository?.recordValues(trip.id, values)
                }
            }
        }
    }

    fun startTrip() {
        val repository = tripRepository ?: return
        viewModelScope.launch {
            _activeTrip.value = repository.startTrip()
        }
    }

    fun pauseTrip() {
        val repository = tripRepository ?: return
        val trip = _activeTrip.value ?: return
        viewModelScope.launch {
            repository.pauseTrip(trip.id)
            _activeTrip.value = trip.copy(isRecording = false)
        }
    }

    fun resumeTrip() {
        val repository = tripRepository ?: return
        val trip = _activeTrip.value ?: return
        viewModelScope.launch {
            repository.resumeTrip(trip.id)
            _activeTrip.value = trip.copy(isRecording = true)
        }
    }

    fun finishTrip() {
        val repository = tripRepository ?: return
        val trip = _activeTrip.value ?: return
        viewModelScope.launch {
            repository.finishTrip(trip.id)
            _activeTrip.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        collecting = false
    }
}
