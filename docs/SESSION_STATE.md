# Session State

> Current development task only.
> All past / future steps are tracked in [ROADMAP.md](./ROADMAP.md).

---

## Current Session

| Field | Value |
|---|---|
| Date | 2026-08-02 |
| Phase | Phase 3 — Persistence ✅ |
| Current Feature | First-use Bluetooth flow implemented; physical validation pending |

## Last Checkpoint

Phase 3 implemented:
- Room database stores trips and individual PID readings
- Dashboard supports start, pause, resume and finish recording
- Trip history shows reading counts and core sensor statistics

Phase 3.5 implementation started:
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

## Immediate Next Actions

1. Validate expanded PID polling and formulas on the physical vehicle
2. Improve the dashboard with gauges, sensor groups, detailed trips and historical charts
3. Add DTC reading, vehicle information and CSV export

## Blockers

No software blocker remains for the first physical smoke test. Real hardware validation is still required for adapter/vehicle-specific behavior.

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
