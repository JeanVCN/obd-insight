# Bluetooth Connection — Feature Overview

**Status**: Implemented (V1)

## Purpose

Establish and manage a Bluetooth Classic RFCOMM/SPP connection between the Android device and an ELM327-compatible OBD-II adapter.

## Key Components

| Component | Responsibility |
|---|---|
| `BluetoothConnectionManager` | Socket lifecycle, I/O streams, state emissions |
| `PermissionManager` | Runtime permission checks (API 31+ / pre-31) |
| `ConnectionViewModel` | Scan, connect, disconnect orchestration |
| `ConnectionScreen` | UI for device list, connection status, errors |

## State Machine

```
Disconnected → Scanning → FoundDevices → Connecting → Connected
                                                  ↓
                                               Error → Disconnected
```

## Dependencies

- BluetoothAdapter (system service)
- RFCOMM socket with SPP UUID (`00001101-0000-1000-8000-00805F9B34FB`)
