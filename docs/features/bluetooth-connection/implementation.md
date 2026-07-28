# Bluetooth Connection — Implementation

## Code Structure

```
data/bluetooth/
├── BluetoothConnectionManager.kt   # Socket + I/O
└── PermissionManager.kt            # Runtime permission checks

domain/model/
├── BluetoothError.kt               # Error enum
├── BluetoothResult.kt              # Success/Error sealed class
└── ConnectionState.kt              # State machine sealed interface

ui/connection/
├── ConnectionViewModel.kt          # Scan, connect, disconnect
└── ConnectionScreen.kt             # Compose UI
```

## Key Design Decisions

### BluetoothConnectionManager

- Single class owns the socket reference and both I/O streams
- `sendCommand()` writes `command + "\r\n"`, reads one response line
- State is exposed as `StateFlow<ConnectionState>` for reactive collection
- `ioDispatcher` is injected as a constructor parameter (default `Dispatchers.IO`) for testability

### Error Handling

All Bluetooth operations return `BluetoothResult<T>`:
- `Success(data)` on success
- `Error(reason)` with a `BluetoothError` enum value

This forces the caller to handle errors exhaustively.

### PermissionManager

Encapsulates the API-level-dependent permission check:
- `hasBluetoothPermissions()` — boolean check
- `requiredPermissions()` — array for permission request

## State Flow

```
User taps "Scan"
  → ViewModel.scanDevices()
    → state = Scanning
    → bluetoothManager.getPairedDevices()
    → devices = result
    → state = FoundDevices(devices)

User taps a device
  → ViewModel.connect(device)
    → bluetoothManager.connect(device)
      → state = Connecting(name)
      → socket.connect()
      → state = Connected(name)
      → elm327Protocol.initialize()

User taps "Disconnect"
  → ViewModel.disconnect()
    → bluetoothManager.disconnect()
      → socket.close()
      → state = Disconnected
```
