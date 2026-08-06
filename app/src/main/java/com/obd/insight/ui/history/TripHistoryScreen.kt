package com.obd.insight.ui.history

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    onOpenTrip: (Long) -> Unit = {},
    viewModel: TripHistoryViewModel = viewModel(
        factory = AppModule.tripHistoryViewModelFactory(LocalContext.current)
    )
) {
    val trips by viewModel.trips.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Histórico", style = MaterialTheme.typography.titleLarge)
                        Text("Suas viagens, em um só lugar", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (trips.isEmpty()) {
            EmptyHistory(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HistoryHero(trips.size)
                    Spacer(Modifier.height(8.dp))
                }
                items(trips, key = { it.trip.id }) { summary ->
                    TripHistoryCard(
                        summary = summary,
                        onOpen = { onOpenTrip(summary.trip.id) },
                        onCsv = {
                            viewModel.createExportIntent(context, summary) { intent ->
                                context.startActivity(Intent.createChooser(intent, "Compartilhar dados da viagem"))
                            }
                        },
                        onPdf = {
                            viewModel.createPdfExportIntent(context, summary) { intent ->
                                context.startActivity(Intent.createChooser(intent, "Compartilhar relatório da viagem"))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryHero(count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                )
            )
            .padding(22.dp)
    ) {
        Text("Visão geral", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Text("$count ${if (count == 1) "viagem registrada" else "viagens registradas"}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Toque em uma viagem para explorar métricas e gráficos.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TripHistoryCard(summary: TripSummary, onOpen: () -> Unit, onCsv: () -> Unit, onPdf: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(summary.trip.startedAt)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Viagem #${summary.trip.id}  •  ${summary.readingCount} amostras", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Abrir detalhes", tint = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill("RPM máx", summary.maxRpm?.let { "${it.toInt()}" } ?: "--", Modifier.weight(1f))
                MetricPill("Vel. média", summary.averageSpeed?.let { "%.1f".format(it) } ?: "--", Modifier.weight(1f))
                MetricPill("Temp. máx", summary.maxCoolantTemperature?.let { "%.1f°".format(it) } ?: "--", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Exportar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                IconButton(onClick = onCsv) { Icon(Icons.Default.Description, "Exportar CSV") }
                IconButton(onClick = onPdf) { Icon(Icons.Default.PictureAsPdf, "Exportar PDF") }
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ainda não há viagens", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Quando você finalizar uma gravação, ela aparecerá aqui com métricas, gráficos e exportação.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
