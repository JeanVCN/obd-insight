package com.obd.insight.ui.history

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.obd.insight.data.persistence.SensorReadingEntity
import com.obd.insight.di.AppModule
import com.obd.insight.domain.model.Trip
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    tripId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TripDetailsViewModel = viewModel(
        factory = AppModule.tripDetailsViewModelFactory(LocalContext.current, tripId)
    )
) {
    val trip by viewModel.trip.collectAsState()
    val readings by viewModel.readings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da viagem") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.createPdfExportIntent(context) { intent ->
                            context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
                        }
                    }, enabled = trip != null) { Icon(Icons.Default.PictureAsPdf, "Exportar PDF") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        when {
            isLoading -> LoadingDetails(padding)
            trip == null -> MissingDetails(padding)
            else -> TripDetailsContent(
                padding = padding,
                trip = trip!!,
                readings = readings,
                onPdf = {
                    viewModel.createPdfExportIntent(context) { intent ->
                        context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
                    }
                },
                onCsv = {
                    viewModel.createCsvExportIntent(context) { intent ->
                        context.startActivity(Intent.createChooser(intent, "Compartilhar CSV"))
                    }
                }
            )
        }
    }
}

@Composable
private fun TripDetailsContent(padding: PaddingValues, trip: Trip, readings: List<SensorReadingEntity>, onPdf: () -> Unit, onCsv: () -> Unit) {
    val summaries = summarizeSensors(readings)
    val rpm = readings.filter { it.pid == 0x0C }
    val speed = readings.filter { it.pid == 0x0D }
    val coolant = readings.filter { it.pid == 0x05 }
    val load = readings.filter { it.pid == 0x04 }
    val maf = readings.filter { it.pid == 0x10 }
    val duration = (trip.endedAt ?: readings.maxOfOrNull { it.recordedAt } ?: trip.startedAt) - trip.startedAt

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                ).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Viagem #${trip.id}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                Text(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(Date(trip.startedAt)), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Dados processados localmente • ${readings.size} amostras", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TripMetricTile("Duração", formatDuration(duration), "tempo registrado", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                TripMetricTile("PIDs", summaries.size.toString(), "sensores encontrados", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TripMetricTile("RPM máximo", rpm.maxOfOrNull { it.value }?.let { "${it.toInt()}" } ?: "--", "rotação", Color(0xFFFF8A65), Modifier.weight(1f))
                TripMetricTile("Vel. média", speed.takeIf { it.isNotEmpty() }?.map { it.value }?.average()?.let { "%.1f".format(Locale.US, it) } ?: "--", "km/h", Color(0xFF64B5F6), Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TripMetricTile("Temp. máxima", coolant.maxOfOrNull { it.value }?.let { "%.1f°".format(Locale.US, it) } ?: "--", "líquido", Color(0xFFFFB74D), Modifier.weight(1f))
                TripMetricTile("Carga máxima", load.maxOfOrNull { it.value }?.let { "%.1f%%".format(Locale.US, it) } ?: "--", "motor", Color(0xFF81C784), Modifier.weight(1f))
            }
        }
        item { SectionTitle("Evolução durante a viagem", "Sensores selecionados com mais de uma amostra") }
        if (rpm.isNotEmpty()) item { SensorChartCard(rpm, Color(0xFFFF8A65)) }
        if (speed.isNotEmpty()) item { SensorChartCard(speed, Color(0xFF64B5F6)) }
        if (coolant.isNotEmpty()) item { SensorChartCard(coolant, Color(0xFFFFB74D)) }
        if (load.isNotEmpty()) item { SensorChartCard(load, Color(0xFF81C784)) }
        if (maf.isNotEmpty()) item { SensorChartCard(maf, Color(0xFFBA68C8)) }
        item { SectionTitle("Todos os sensores", "Mínimo / média / máximo") }
        items(summaries, key = { it.pid }) { summary -> SensorSummaryRow(summary) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCsv, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Description, null)
                    Spacer(Modifier.padding(3.dp))
                    Text("CSV")
                }
                Button(onClick = onPdf, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.padding(3.dp))
                    Text("PDF")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SensorSummaryRow(summary: SensorSummary) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(summary.label, fontWeight = FontWeight.SemiBold)
            Text("PID 0x${summary.pid.toString(16).padStart(2, '0').uppercase()} • ${summary.count} amostras", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${number(summary.average)} ${summary.unit}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("${number(summary.min)} / ${number(summary.max)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingDetails(padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Carregando dados da viagem...")
    }
}

@Composable
private fun MissingDetails(padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.Center) {
        Text("Viagem não encontrada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("O registro pode ter sido removido do banco local.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(durationMillis: Long): String {
    val seconds = max(0L, durationMillis) / 1000
    return "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}

private fun number(value: Float): String = "%.1f".format(Locale.US, value)
