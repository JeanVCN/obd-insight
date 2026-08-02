package com.obd.insight.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obd.insight.data.persistence.TripRepository
import com.obd.insight.domain.model.TripSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TripHistoryViewModel(repository: TripRepository) : ViewModel() {
    val trips: StateFlow<List<TripSummary>> = repository.observeFinishedTrips().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}
