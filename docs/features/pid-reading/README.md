# PID Reading — Feature Overview

**Status**: Implemented (V2 in progress)

## Purpose

Request and decode OBD-II Parameter IDs (PIDs) to obtain vehicle sensor data.

## OBD-II Modes

| Mode | Name | Purpose |
|---|---|---|
| 01 | Show Current Data | Read live sensor values |
| 02 | Show Freeze Frame Data | Snapshot of fault conditions |
| 03 | Show Stored Diagnostic Trouble Codes | Read DTCs |
| 04 | Clear Diagnostic Trouble Codes | Clear DTCs |
| 05 | Test Results (Oxygen Sensors) | O2 sensor monitor |
| 06 | Test Results (On-Board Monitors) | Component/system monitoring |
| 07 | Show Pending Diagnostic Trouble Codes | Pending DTCs |
| 08 | Control Operation of On-Board Component | Actuator tests |
| 09 | Request Vehicle Information | VIN, calibration info |

## Common PIDs (Mode 01)

| PID | Bytes | Formula | Description |
|---|---|---|---|
| 00 | 4 | Bitmask | Supported PIDs 01-20 |
| 05 | 1 | A-40 | Engine Coolant Temperature (°C) |
| 0C | 2 | ((A*256)+B)/4 | Engine RPM |
| 0D | 1 | A | Vehicle Speed (km/h) |
| 10 | 2 | (A*256)+B | Mass Air Flow (g/s) |
| 42 | 2 | ((A*256)+B)/1000 | Control module voltage (V) |
| 5C | 1 | A-40 | Engine oil temperature (°C) |

## Implementation

- `ObdPidReader` sends Mode 01 PID requests and removes optional ELM327 headers.
- `PidValueConverter` converts supported responses to physical values.
- `ObdPidReader` discovers consecutive supported PID blocks (`0100`, `0120`, `0140`, ...), stopping when the ECU reports no continuation.
- `ObdSensorReader` polls supported convertible PIDs sequentially every second, falling back to RPM (`010C`), speed (`010D`) and coolant temperature (`0105`) when the support query is unavailable.
- The converter also covers fuel trim, fuel pressure, timing advance, intake temperature, throttle positions, fuel level, distance, module voltage and fuel rate.
