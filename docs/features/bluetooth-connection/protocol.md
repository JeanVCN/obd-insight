# Bluetooth Connection — Protocol: RFCOMM / SPP

## Bluetooth Classic

Android's Bluetooth Classic API provides RFCOMM (Radio Frequency Communication) — a serial port emulation over Bluetooth.

## SPP (Serial Port Profile)

ELM327 adapters expose an SPP service with the well-known UUID:

```
00001101-0000-1000-8000-00805F9B34FB
```

This is the standard Serial Port Profile UUID.

## Connection Flow

1. Obtain `BluetoothAdapter` via `BluetoothAdapter.getDefaultAdapter()`
2. Get paired devices via `adapter.bondedDevices`
3. Create RFCOMM socket: `device.createRfcommSocketToServiceRecord(uuid)`
4. Cancel discovery (improves connection speed)
5. Call `socket.connect()` (blocking — runs on IO dispatcher)
6. Open `outputStream` and `inputStream` for command/response

## Command Format

Commands are sent as ASCII strings terminated with `\r\n`:

```
ATZ\r\n
ATE0\r\n
010C\r\n
```

Responses are read line-by-line from the input stream.

## Permissions

| API Level | Required Permissions |
|---|---|
| API 31+ (Android 12) | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` |
| Below API 31 | `ACCESS_FINE_LOCATION` |

These are runtime permissions that must be requested before any Bluetooth operation.
