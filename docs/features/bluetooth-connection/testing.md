# Bluetooth Connection — Testing Strategy

## Unit Testing Approach

### Mock Strategy

- `BluetoothAdapter`, `BluetoothDevice`, `BluetoothSocket` are Android framework classes — use MockK mocks
- `PermissionManager` uses real `Context` mock for permission checks
- `BluetoothConnectionManager` tests inject `Dispatchers.Unconfined` for `ioDispatcher`

### BluetoothConnectionManager Tests

| Test Case | Description |
|---|---|
| `connect succeeds` | Mock socket connect, verify state → `Connected` |
| `connect fails with IOException` | Mock socket throws, verify state → `Error(SOCKET_ERROR)` |
| `disconnect` | Mock socket close, verify state → `Disconnected` |
| `sendCommand success` | Mock output/input streams, verify written bytes and returned line |
| `sendCommand with null socket` | Call before connect, verify `Error(IO_ERROR)` |
| `getPairedDevices` | Mock adapter bondedDevices |

### PermissionManager Tests

| Test Case | Description |
|---|---|
| `hasBluetoothPermissions API 31+` | Mock context with BLUETOOTH_SCAN + BLUETOOTH_CONNECT granted |
| `hasBluetoothPermissions API 30` | Mock context with ACCESS_FINE_LOCATION granted |
| `hasBluetoothPermissions denied` | Mock permission denied |
| `requiredPermissions API 31+` | Verify array contains BLUETOOTH_SCAN and BLUETOOTH_CONNECT |
| `requiredPermissions API 30` | Verify array contains ACCESS_FINE_LOCATION |

### Edge Cases

- Bluetooth adapter is null (device without BT)
- Socket is null when sendCommand is called
- Empty bonded devices list
- Permission denied mid-flow
- Multiple rapid connect/disconnect calls
