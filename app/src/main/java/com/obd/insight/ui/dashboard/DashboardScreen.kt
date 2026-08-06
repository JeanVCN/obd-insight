package com.obd.insight.ui.dashboard

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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.obd.insight.di.AppModule
import com.obd.insight.domain.model.PidValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(
        factory = AppModule.dashboardViewModelFactory(LocalContext.current)
    )
) {
    val sensorValues by viewModel.sensorValues.collectAsState()
    val activeTrip by viewModel.activeTrip.collectAsState()

    LaunchedEffect(Unit) { viewModel.startCollecting() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Painel ao vivo", style = MaterialTheme.typography.titleLarge)
                        Text("Monitoramento do veículo", style = MaterialTheme.typography.labelSmall)
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { LiveHero(isRecording = activeTrip?.isRecording == true, hasData = sensorValues.isNotEmpty()) }
            item {
                TripControls(
                    isRecording = activeTrip?.isRecording,
                    onStart = viewModel::startTrip,
                    onPause = viewModel::pauseTrip,
                    onResume = viewModel::resumeTrip,
                    onFinish = viewModel::finishTrip
                )
            }
            if (sensorValues.isEmpty()) {
                item { WaitingCard() }
            } else {
                item { Text("Leituras atuais", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(sensorValues, key = { it.pid }) { value -> LiveSensorCard(value) }
            }
        }
    }
}

@Composable
private fun LiveHero(isRecording: Boolean, hasData: Boolean) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(
            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
        ).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(if (isRecording) "GRAVANDO VIAGEM" else "MONITORAMENTO PRONTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
        Text(if (hasData) "Dados chegando em tempo real" else "Aguardando resposta do veículo", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        Text("As leituras ficam salvas localmente quando a gravação está ativa.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TripControls(isRecording: Boolean?, onStart: () -> Unit, onPause: () -> Unit, onResume: () -> Unit, onFinish: () -> Unit) {
    when (isRecording) {
        null -> Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.padding(3.dp))
            Text("Iniciar gravação")
        }
        true -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Pause, null)
                Spacer(Modifier.padding(2.dp))
                Text("Pausar")
            }
            Button(onClick = onFinish, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Stop, null)
                Spacer(Modifier.padding(2.dp))
                Text("Finalizar")
            }
        }
        false -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onResume, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.padding(2.dp))
                Text("Retomar")
            }
            OutlinedButton(onClick = onFinish, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Finalizar") }
        }
    }
}

@Composable
private fun LiveSensorCard(value: PidValue) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(value.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("PID 0x${value.pid.toString(16).padStart(2, '0').uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatValue(value.value), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(3.dp))
            Text(value.unit, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WaitingCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text("Aguardando dados OBD...", style = MaterialTheme.typography.titleMedium)
            Text("Verifique a conexão com o adaptador para iniciar as leituras.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatValue(value: Float): String = if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)
