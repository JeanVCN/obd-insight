package com.obd.insight.ui.history

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obd.insight.data.persistence.TripRepository
import com.obd.insight.domain.model.TripSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TripHistoryViewModel(private val repository: TripRepository) : ViewModel() {
    val trips: StateFlow<List<TripSummary>> = repository.observeFinishedTrips().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun createExportIntent(context: Context, summary: TripSummary, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val intent = withContext(Dispatchers.IO) {
                val readings = repository.getReadings(summary.trip.id)
                com.obd.insight.data.persistence.TripExportManager.createShareIntent(context, summary.trip, readings)
            }
            onReady(intent)
        }
    }

    fun createPdfExportIntent(context: Context, summary: TripSummary, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val intent = withContext(Dispatchers.IO) {
                val readings = repository.getReadings(summary.trip.id)
                com.obd.insight.data.persistence.TripExportManager.createPdfShareIntent(context, summary.trip, readings)
            }
            onReady(intent)
        }
    }
}
