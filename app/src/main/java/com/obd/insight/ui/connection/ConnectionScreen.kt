package com.obd.insight.ui.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.obd.insight.di.AppModule
import com.obd.insight.data.bluetooth.PermissionManager
import com.obd.insight.domain.model.BluetoothError
import com.obd.insight.domain.model.ConnectionState
import com.obd.insight.domain.model.ProtocolType

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ConnectionScreen(
    onNavigateToAtTerminal: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToTripHistory: () -> Unit = {},
    viewModel: ConnectionViewModel = viewModel(factory = AppModule.viewModelFactory)
) {
    val context = LocalContext.current
    val permissionManager = remember(context) { PermissionManager(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.scanDevices()
        } else {
            viewModel.onPermissionsDenied()
        }
    }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.scanDevices()
        }
    }
    val state by viewModel.state.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val pairingDeviceName by viewModel.pairingDeviceName.collectAsState()
    val discoveryMessage by viewModel.discoveryMessage.collectAsState()
    val warningMessage by viewModel.warningMessage.collectAsState()
    val protocol by viewModel.protocol.collectAsState()
    val supportedPids by viewModel.supportedPids.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Column {
                        Text("OBD Insight", style = MaterialTheme.typography.titleLarge)
                        Text("Conecte. Monitore. Entenda.", style = MaterialTheme.typography.labelSmall)
                    }
                },
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusCard(state = state)

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is ConnectionState.Disconnected -> {
                    Button(onClick = {
                        if (permissionManager.hasBluetoothPermissions()) {
                            viewModel.scanDevices()
                        } else {
                            permissionLauncher.launch(permissionManager.requiredPermissions())
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Text("Procurar dispositivos")
                    }
                }
                is ConnectionState.Scanning -> {
                    CircularProgressIndicator()
                    Text("Procurando dispositivos próximos...")
                }
                is ConnectionState.FoundDevices -> {
                    Button(onClick = {
                        if (permissionManager.hasBluetoothPermissions()) {
                            viewModel.scanDevices()
                        } else {
                            permissionLauncher.launch(permissionManager.requiredPermissions())
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Text("Procurar novamente")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    discoveryMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    DeviceList(
                        modifier = Modifier.weight(1f),
                        pairedDevices = devices,
                        discoveredDevices = discoveredDevices,
                        onConnect = { device -> viewModel.connect(device) },
                        onPair = { device -> viewModel.pair(device) }
                    )
                    pairingDeviceName?.let { name ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Solicitação de pareamento para $name. Confirme no diálogo do Android.")
                    }
                }
                is ConnectionState.Connecting -> {
                    CircularProgressIndicator()
                    Text("Conectando a ${(state as ConnectionState.Connecting).deviceName}...")
                }
                is ConnectionState.Connected -> {
                    warningMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(onClick = { viewModel.disconnect() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Text("Desconectar")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onNavigateToAtTerminal, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Text("Terminal AT")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onNavigateToDashboard, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Text("Painel")
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
                        text = "Erro: ${(state as ConnectionState.Error).message}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if ((state as ConnectionState.Error).error == BluetoothError.BLUETOOTH_OFF) {
                        Button(onClick = {
                            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("Ligar Bluetooth")
                        }
                    } else {
                        Button(onClick = {
                            if (permissionManager.hasBluetoothPermissions()) {
                                viewModel.scanDevices()
                            } else {
                                permissionLauncher.launch(permissionManager.requiredPermissions())
                            }
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToTripHistory, modifier = Modifier.fillMaxWidth()) {
                Text("Histórico de viagens")
            }
        }
    }
}

@Composable
private fun StatusCard(state: ConnectionState) {
    val statusText = when (state) {
        ConnectionState.Disconnected -> "Desconectado"
        is ConnectionState.Scanning -> "Procurando..."
        is ConnectionState.FoundDevices -> "Dispositivos encontrados"
        is ConnectionState.Connecting -> "Conectando..."
        is ConnectionState.Connected -> "Conectado a ${state.deviceName}"
        is ConnectionState.Error -> "Erro"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
            .padding(22.dp)
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
        Text("STATUS DA CONEXÃO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f))
        Spacer(modifier = Modifier.height(6.dp))
        Text(statusText, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(
            if (state is ConnectionState.Connected) "Leituras prontas para o painel" else "O histórico continua disponível offline",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .78f)
        )
        }
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
                text = "Protocolo",
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
                text = "PIDs suportados",
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

@SuppressLint("MissingPermission")
@Composable
private fun DeviceList(
    modifier: Modifier = Modifier,
    pairedDevices: List<android.bluetooth.BluetoothDevice>,
    discoveredDevices: List<android.bluetooth.BluetoothDevice>,
    onConnect: (android.bluetooth.BluetoothDevice) -> Unit,
    onPair: (android.bluetooth.BluetoothDevice) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pairedDevices.isNotEmpty()) {
            item {
                Text("Dispositivos pareados", style = MaterialTheme.typography.titleSmall)
            }
        }
        items(pairedDevices) { device ->
            Card(
                onClick = { onConnect(device) },
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
                        text = device.name ?: "Dispositivo desconhecido",
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
        if (discoveredDevices.isNotEmpty()) {
            item {
                Text("Dispositivos próximos", style = MaterialTheme.typography.titleSmall)
            }
        }
        items(discoveredDevices) { device ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = device.name ?: "Dispositivo desconhecido",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = device.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { onPair(device) }) {
                        Text("Parear")
                    }
                }
            }
        }
        if (pairedDevices.isEmpty() && discoveredDevices.isEmpty()) {
            item {
                Text("Nenhum dispositivo Bluetooth pareado ou próximo foi encontrado.")
            }
        }
    }
}
