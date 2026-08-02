package com.obd.insight.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.obd.insight.di.AppModule
import com.obd.insight.domain.model.TripSummary
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: TripHistoryViewModel = viewModel(
        factory = AppModule.tripHistoryViewModelFactory(LocalContext.current)
    )
) {
    val trips by viewModel.trips.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de viagens") },
                navigationIcon = { Button(onClick = onNavigateBack) { Text("Voltar") } }
            )
        }
    ) { padding ->
        if (trips.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Nenhuma viagem finalizada ainda.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trips, key = { it.trip.id }) { summary -> TripCard(summary) }
            }
        }
    }
}

@Composable
private fun TripCard(summary: TripSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = DateFormat.getDateTimeInstance().format(Date(summary.trip.startedAt)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("${summary.readingCount} leituras de sensores registradas")
            MetricRow("RPM máximo", summary.maxRpm?.let { "${it.toInt()} rpm" } ?: "Sem dados")
            MetricRow("Velocidade média", summary.averageSpeed?.let { "%.1f km/h".format(it) } ?: "Sem dados")
            MetricRow("Temperatura máxima", summary.maxCoolantTemperature?.let { "%.1f °C".format(it) } ?: "Sem dados")
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
