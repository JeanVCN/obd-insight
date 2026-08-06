# Session State

> Current development task only.
> All past / future steps are tracked in [ROADMAP.md](./ROADMAP.md).

---

## Current Session

| Field | Value |
|---|---|
| Date | 2026-08-05 |
| Phase | Phase 4 — Historical analysis and export 🔄 |
| Current Feature | Offline trip details, raw-data persistence, CSV/PDF export and visual redesign |

## Last Checkpoint

Completed today:
- Room database stores trips and individual PID readings
- Dashboard supports start, pause, resume and finish recording
- Trip history opens detailed trip dashboards without requiring Bluetooth
- Trip detail screen shows metrics, sensor summaries and charts
- CSV export preserves processed values and raw hexadecimal payloads for new trips
- PDF export provides a colored offline report with metrics and charts
- Connection and live dashboard screens use the refreshed visual system
- Launcher icon is a minimal black graph line on white
- Physical database recovery found two trips and 4,695 historical readings
- Room migration from schema version 1 to 2 was verified on the physical phone

Bluetooth and collection behavior:
- Connection screen requests the required Bluetooth permissions at runtime
- The app handles unavailable and disabled Bluetooth with actionable UI
- Nearby Bluetooth Classic devices are discovered separately from already paired devices
- Discovered adapters can start Android's native pairing flow
- ELM327 responses are read through a single socket stream until the `>` prompt, with a real polling timeout
- Commands are serialized to prevent terminal and dashboard requests from interleaving
- PID requests use canonical two-byte service/PID formatting and handshake failures reach the UI
- `testDebugUnitTest`, `assembleDebug` and `lintDebug` pass after a clean sequential build
- PID support discovery now follows blocks such as `0100`, `0120` and `0140`
- Sensor polling uses supported convertible PIDs and falls back to RPM, speed and coolant temperature
- Local Go telemetry API is deferred; analysis remains on the phone
- Android 13 validation confirmed both nearby-device permissions granted; discovery failures are now reported separately from permission failures
- If discovery fails but paired devices exist, the connection screen keeps showing paired devices so an existing ELM327 can still be tested

## Blockers

No software blocker remains. Another physical trip is recommended to validate the updated charts and PDF with fresh raw-data recordings.

## Hot Context

- ELM327 init flow: `ATZ → ATE0 → ATL0 → ATS0 → ATH1 → ATAT1 → ATSP0`
- `ConnectionState` sealed interface: 6 states
- Testing: JUnit4 + MockK + Turbine + kotlinx-coroutines-test
- Commands flow through `BluetoothConnectionManager.sendCommand()` → `Elm327Protocol.execute()`
- Persistence flow: `DashboardViewModel` → `TripRepository` → Room
- First use must support a phone with no paired adapter: permissions, Bluetooth state, nearby discovery and Android pairing
- The emulator is useful for UI and Room, but physical Bluetooth Classic RFCOMM and ELM327 validation require a real Android phone
- A future local telemetry API is optional and offline-safe: Room remains the source of truth if the notebook is unreachable
- Physical test checkpoint: `docs/PHYSICAL_TEST_CHECKPOINT.md`
- Confirmed live vehicle data through Bluetooth Classic ELM327, including RPM, load, coolant, MAF, throttle, module voltage and multiple ECU CAN responses

## Immediate Next Actions

1. Use the updated APK during another real trip and review the new detail charts and PDF on-device
2. Add selectable chart ranges and optional sensor filters for very long trips
3. Add DTC reading, VIN and further vehicle information

## Data Notes

- Historical database snapshot: `/tmp/opencode/obd-export-2026-08-05/`
- Historical trips contain processed readings only; `rawData` is empty for rows created before schema version 2
- New trips store all successful supported PID responses, including raw hexadecimal payloads
