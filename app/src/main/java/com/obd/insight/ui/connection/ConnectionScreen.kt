package com.obd.insight.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.obd.insight.di.AppModule
import com.obd.insight.domain.model.ConnectionState
import com.obd.insight.domain.model.ProtocolType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    onNavigateToAtTerminal: () -> Unit = {},
    viewModel: ConnectionViewModel = viewModel(factory = AppModule.viewModelFactory)
) {
    val state by viewModel.state.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val protocol by viewModel.protocol.collectAsState()
    val supportedPids by viewModel.supportedPids.collectAsState()

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("OBD Insight") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusCard(state = state)

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is ConnectionState.Disconnected -> {
                    Button(onClick = { viewModel.scanDevices() }) {
                        Text("Scan for Devices")
                    }
                }
                is ConnectionState.Scanning -> {
                    CircularProgressIndicator()
                    Text("Scanning...")
                }
                is ConnectionState.FoundDevices -> {
                    Button(onClick = { viewModel.scanDevices() }) {
                        Text("Rescan")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DeviceList(
                        devices = devices,
                        onDeviceClick = { device -> viewModel.connect(device) }
                    )
                }
                is ConnectionState.Connecting -> {
                    CircularProgressIndicator()
                    Text("Connecting to ${(state as ConnectionState.Connecting).deviceName}...")
                }
                is ConnectionState.Connected -> {
                    Button(onClick = { viewModel.disconnect() }) {
                        Text("Disconnect")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onNavigateToAtTerminal) {
                        Text("AT Terminal")
                    }
                    protocol?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        ProtocolCard(protocol = it)
                    }
                    supportedPids?.let { pids ->
                        Spacer(modifier = Modifier.height(12.dp))
                        SupportedPidsCard(pids = pids)
                    }
                }
                is ConnectionState.Error -> {
                    Text(
                        text = "Error: ${(state as ConnectionState.Error).message}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.scanDevices() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: ConnectionState) {
    val statusText = when (state) {
        ConnectionState.Disconnected -> "Disconnected"
        is ConnectionState.Scanning -> "Scanning..."
        is ConnectionState.FoundDevices -> "Devices Found"
        is ConnectionState.Connecting -> "Connecting..."
        is ConnectionState.Connected -> "Connected to ${state.deviceName}"
        is ConnectionState.Error -> "Error"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ProtocolCard(protocol: ProtocolType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Protocol",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = protocol.description,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "ATDPN: ${protocol.number}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SupportedPidsCard(pids: List<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Supported PIDs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pids.joinToString(", "),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<android.bluetooth.BluetoothDevice>,
    onDeviceClick: (android.bluetooth.BluetoothDevice) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices) { device ->
            Card(
                onClick = { onDeviceClick(device) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = device.name ?: "Unknown Device",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
