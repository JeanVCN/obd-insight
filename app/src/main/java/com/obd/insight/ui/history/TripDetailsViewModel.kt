package com.obd.insight.ui.history

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obd.insight.data.persistence.SensorReadingEntity
import com.obd.insight.data.persistence.TripExportManager
import com.obd.insight.data.persistence.TripRepository
import com.obd.insight.domain.model.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TripDetailsViewModel(
    private val tripId: Long,
    private val repository: TripRepository
) : ViewModel() {
    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _readings = MutableStateFlow<List<SensorReadingEntity>>(emptyList())
    val readings: StateFlow<List<SensorReadingEntity>> = _readings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _trip.value = repository.getTrip(tripId)
            _readings.value = repository.getReadings(tripId)
            _isLoading.value = false
        }
    }

    fun createPdfExportIntent(context: Context, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val currentTrip = _trip.value ?: return@launch
            val intent = withContext(Dispatchers.IO) {
                TripExportManager.createPdfShareIntent(context, currentTrip, _readings.value)
            }
            onReady(intent)
        }
    }

    fun createCsvExportIntent(context: Context, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val currentTrip = _trip.value ?: return@launch
            val intent = withContext(Dispatchers.IO) {
                TripExportManager.createShareIntent(context, currentTrip, _readings.value)
            }
            onReady(intent)
        }
    }
}
