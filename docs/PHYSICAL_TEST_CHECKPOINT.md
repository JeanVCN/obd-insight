# Physical Test Checkpoint

Date: 2026-08-02
Phone: Poco 2207117BPG
Android: 13
App package: com.obd.insight
Adapter: OBDII, Bluetooth Classic, address 00:10:CC:4F:36:03

## Result

The first real vehicle test succeeded:

- Bluetooth permissions were granted.
- The OBDII adapter was paired and connected through RFCOMM/SPP.
- ELM327 initialization completed.
- The vehicle responded to Mode 01 PID requests.
- The connection stayed active while the Dashboard polled data.
- RPM was visible in real time with the engine running.
- The AT Terminal returned responses correctly.

## Observed CAN Responses

The vehicle returns responses with CAN headers and no spaces. Two ECUs commonly respond:

```text
7E8 410C ....
7E9 410C ....
```

The parser must continue removing the 3-digit CAN header and frame length before decoding the OBD payload.

## Recent Observed Values

Values below are approximate and were captured while the vehicle was stationary with the engine running, around 14:17.

| PID | Raw sample | Approximate value |
|---|---|---|
| 04 | 51 | Engine load: 31.8% |
| 05 | 69 | Coolant temperature: 65 C |
| 06 | 80 | Short fuel trim bank 1: 0% |
| 0B | 25 | Intake manifold pressure: 37 kPa |
| 0C | 0C9C / 0CA2 | Engine speed: about 807-809 rpm |
| 0D | 00 | Vehicle speed: 0 km/h |
| 0E | 98 | Timing advance: 12 degrees |
| 0F | 4D | Intake air temperature: 37 C |
| 10 | 0164 | MAF: about 3.56 g/s |
| 11 | 20 | Throttle position: about 12.9% |
| 21 | 0000 | Distance with MIL on: 0 km |
| 31 | 2BD2 | Distance since DTC clear: about 11218 km |
| 42 | 3780 / 36D5 | Module voltage: about 14.0-14.2 V |
| 45 | 0B | Relative throttle position: about 4.3% |
| 46 | 4D | Ambient temperature: 37 C |
| 49 | 31 / 32 | Accelerator pedal position D: about 19-20% |
| 4A | 18 | Accelerator pedal position E: about 9.4% |
| 4C | 0A | Commanded throttle actuator: about 3.9% |
| 5A | 00 | Relative accelerator pedal position: 0% |

PIDs `0A`, `2F`, `5C` and `5E` were not polled in the captured cycle, which indicates they were not included in the supported/conversible set for this connection.

## Important Findings

- Multiple ECUs can answer the same PID. Current parsing uses the first matching response. A future improvement should identify the preferred ECU instead of mixing frames.
- PID `31` returned different values from different ECUs, including `FFFF` from one response. This must be handled before using the value as authoritative.
- The car was stationary, so speed `0 km/h` is expected.
- The app is currently an offline mobile monitor. No Go API or desktop telemetry was added.

## Resume From Home

1. Review `data/obd/ObdPidReader.kt` and CAN frame selection.
2. Add a session/vehicle model for ECU identity and connection metadata.
3. Improve the Dashboard layout for the expanded sensor list.
4. Add DTC reading through Modes 03 and 07.
5. Add vehicle information through Mode 09, especially VIN.
6. Add charts and richer trip statistics using the Room readings already stored.

## Diagnostic Capture

The phone was reachable through `adb` during capture. Useful command:

```bash
adb logcat -d -s BluetoothConnectionManager:D '*:S'
```
