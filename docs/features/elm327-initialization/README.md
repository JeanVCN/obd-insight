# ELM327 Initialization — Feature Overview

**Status**: Implemented (V1)

## Purpose

Initialize the ELM327 chip after Bluetooth connection is established, bringing it to a known state ready for OBD-II communication.

## Initialization Sequence

```
Step 1: ATZ          → Reset ELM327
Step 2: ATE0         → Echo off
Step 3: ATL0         → Linefeeds off
Step 4: ATS0         → Spaces off
Step 5: ATH1         → Headers on (for debugging)
Step 6: ATAT1        → Adaptive timing auto
Step 7: ATSP0        → Auto protocol detection
```

## Why Each Command

| Command | Purpose |
|---|---|
| `ATZ` | Reset — returns ELM327 to factory state |
| `ATE0` | Echo Off — response won't echo the command back |
| `ATL0` | Linefeeds Off — cleaner response parsing |
| `ATS0` | Spaces Off — responses are contiguous hex |
| `ATH1` | Headers On — includes protocol header in response (useful for debugging) |
| `ATAT1` | Adaptive Timing Auto — ELM327 adjusts inter-character timing |
| `ATSP0` | Auto Protocol — ELM327 auto-detects the vehicle protocol |

## Implementation

Handled by `Elm327Protocol.initialize()`:
- Sends each command sequentially via `BluetoothConnectionManager.sendCommand()`
- Validates each response before proceeding
- Returns `BluetoothResult.Error` if any step fails
