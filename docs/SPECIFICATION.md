# OBD Insight — Specification

## Architecture

Clean Architecture simplified into three layers:

```
data/     → Bluetooth, ELM327 implementation
domain/   → Models without Android dependencies
ui/       → Compose screens + ViewModels
di/       → Manual factory methods
```

### Package Structure

```
com.obd.insight/
├── ObdInsightApplication.kt
├── MainActivity.kt
├── data/
│   ├── bluetooth/
│   │   ├── BluetoothConnectionManager.kt
│   │   └── PermissionManager.kt
│   └── elm327/
│       ├── Elm327Command.kt
│       ├── Elm327Protocol.kt
│       └── Elm327Response.kt
├── domain/
│   └── model/
│       ├── BluetoothError.kt
│       ├── BluetoothResult.kt
│       └── ConnectionState.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── connection/
│       ├── ConnectionScreen.kt
│       └── ConnectionViewModel.kt
└── di/
    └── AppModule.kt
```

---

## Domain Models

### `BluetoothError` (enum)

```
DEVICE_NOT_FOUND, PERMISSION_DENIED, BLUETOOTH_OFF,
CONNECTION_TIMEOUT, SOCKET_ERROR, IO_ERROR,
PROTOCOL_ERROR, UNKNOWN
```

### `BluetoothResult<T>` (sealed class)

```
Success(data: T)
Error(reason: BluetoothError)
```

### `ConnectionState` (sealed interface)

```
Disconnected
Scanning
FoundDevices(devices: List<BluetoothDevice>)
Connecting(deviceName: String)
Connected(deviceName: String)
Error(error: BluetoothError, message: String)
```

---

## Bluetooth Layer

### `BluetoothConnectionManager`

- **connect(device)**: Opens RFCOMM socket, emits `Connecting` → `Connected` or `Error`
- **disconnect()**: Closes socket, emits `Disconnected`
- **getPairedDevices()**: Returns bonded devices from adapter
- **sendCommand(command)**: Writes command + `\r\n` to output stream, reads one line from input stream
- **state**: `StateFlow<ConnectionState>` exposed for reactive UI

CoroutineDispatcher is injected (`ioDispatcher`) for testability.

### `PermissionManager`

- **hasBluetoothPermissions()**: Checks runtime permissions based on API level
  - API 31+: `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT`
  - Below API 31: `ACCESS_FINE_LOCATION`
- **requiredPermissions()**: Returns the array of permissions to request

---

## ELM327 Layer

### `Elm327Command` (sealed class)

| Command | Raw | Purpose |
|---|---|---|
| `Reset` | ATZ | Reset ELM327 |
| `EchoOff` | ATE0 | Disable echo |
| `LinefeedsOff` | ATL0 | Disable linefeeds |
| `SpacesOff` | ATS0 | Disable spaces |
| `HeadersOn` | ATH1 | Enable response headers |
| `AdaptiveTimingAuto` | ATAT1 | Adaptive timing |
| `AutoProtocol` | ATSP0 | Auto protocol detection |
| `SetProtocol(n)` | ATSPn | Manual protocol selection |
| `ReadPid(mode, pid)` | XX YY | OBD request (mode + PID) |
| `ReadDtc` | 03 | Read stored DTCs |

### `Elm327Response` (sealed class)

```
Raw(hexData: List<String>)
Error(code: String, message: String)
NoData
Unknown
```

### `Elm327Protocol`

- **initialize()**: Runs 7-step initialization sequence (ATZ → ATE0 → ATL0 → ATS0 → ATH1 → ATAT1 → ATSP0)
- **execute(command)**: Sends command via BluetoothConnectionManager, parses response
- **parse(response)**: Classifies raw string into Elm327Response

### Initialization Flow

```
Step 1: ATZ          → Reset ELM327
Step 2: ATE0         → Echo off
Step 3: ATL0         → Linefeeds off
Step 4: ATS0         → Spaces off
Step 5: ATH1         → Headers on (for debugging)
Step 6: ATAT1        → Adaptive timing auto
Step 7: ATSP0        → Auto protocol detection
```

Each step's response is validated before proceeding.

---

## UI

### `ConnectionViewModel`

- Exposes `state: StateFlow<ConnectionState>` and `devices: StateFlow<List<BluetoothDevice>>`
- **scanDevices()**: Loads paired devices, emits `Scanning` → `FoundDevices`
- **connect(device)**: Delegates to BluetoothConnectionManager, triggers ELM327 initialization on success
- **disconnect()**: Delegates to BluetoothConnectionManager

### `ConnectionScreen`

State-aware Compose UI with:
- `StatusCard` showing current connection state
- `Scan for Devices` button (when disconnected)
- `DeviceList` of paired devices (when found)
- `CircularProgressIndicator` during scanning/connecting
- `Disconnect` button (when connected)
- `Error` message with retry (when error state)

---

## Connection State Machine

```
Disconnected
    ↓ (scanDevices)
Scanning ──→ FoundDevices(list)
                    ↓ (select device)
               Connecting(deviceName)
                    ↓ (success / fail)
              Connected / Error
                    ↓ (disconnect / reconnect)
              Disconnected
```

States are modeled as a sealed interface for exhaustive `when` handling.

---

## DI

`AppModule` is a singleton object with manual factory methods:
- `provideBluetoothManager()`
- `provideElm327Protocol()`
- `providePermissionManager(context)`
- `provideConnectionViewModel()`
- `cleanup()`

---

## Technical Stack

| Component | Version |
|---|---|
| Kotlin | 1.9.24 |
| AGP | 8.4.0 |
| Compose BOM | 2024.06.00 |
| Compose Compiler | 1.5.14 |
| Room | 2.6.1 |
| Min SDK | 26 |
| Target / Compile SDK | 34 |
| Gradle | 8.7 |

### Testing

| Library | Purpose |
|---|---|
| JUnit 4 | Test framework |
| MockK | Kotlin mocking |
| kotlinx-coroutines-test | TestDispatcher, runTest |
| Turbine | StateFlow/Flow assertions |
