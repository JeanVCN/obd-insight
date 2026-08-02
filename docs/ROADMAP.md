# OBD Insight — Roadmap

> Single source of truth for all past, present, and future steps.
> Session-specific context is in [SESSION_STATE.md](./SESSION_STATE.md).

---

## Phase 1 — Foundation ✅

**Goal**: Bootable app that connects to ELM327 and initializes the chip.

| # | Feature | Status | Depends On | Deliverable |
|---|---|---|---|---|
| 1 | Bluetooth connection | ✅ | — | Scan paired devices, connect, show state |
| 2 | ELM327 initialization | ✅ | #1 | ATZ, ATE0, ATL0, ATS0, protocol detection |

### Implementation Details

#### Project Scaffolding
- [x] `gradle/wrapper/gradle-wrapper.properties` — Gradle 8.7
- [x] `gradle/libs.versions.toml` — Version catalog
- [x] `gradle.properties` — Standard config
- [x] `settings.gradle.kts` — rootProject.name = "obd-insight"
- [x] `build.gradle.kts` (root) — Plugin declarations only
- [x] `app/build.gradle.kts` — All dependencies declared
- [x] `app/proguard-rules.pro` — Basic ProGuard rules for coroutines and data classes
- [x] `app/src/main/AndroidManifest.xml` — BT permissions, activity, application

#### Domain Models
- [x] `domain/model/BluetoothError.kt` — Enum: DEVICE_NOT_FOUND, PERMISSION_DENIED, BLUETOOTH_OFF, CONNECTION_TIMEOUT, SOCKET_ERROR, IO_ERROR, PROTOCOL_ERROR, UNKNOWN
- [x] `domain/model/BluetoothResult.kt` — Sealed class: Success\<T\> / Error
- [x] `domain/model/ConnectionState.kt` — Sealed interface: Disconnected, Scanning, FoundDevices, Connecting, Connected, Error

#### Bluetooth Layer
- [x] `data/bluetooth/PermissionManager.kt` — hasBluetoothPermissions(), requiredPermissions() (API 31+ / legacy)
- [x] `data/bluetooth/BluetoothConnectionManager.kt` — connect, disconnect, getPairedDevices, sendCommand, state flow

#### ELM327 Layer
- [x] `data/elm327/Elm327Command.kt` — Sealed class: Reset, EchoOff, LinefeedsOff, SpacesOff, HeadersOn, AdaptiveTimingAuto, AutoProtocol, SetProtocol, ReadPid, ReadDtc
- [x] `data/elm327/Elm327Response.kt` — Sealed class: Raw, Error, NoData, Unknown
- [x] `data/elm327/Elm327Protocol.kt` — initialize() (7-step), execute(), parse()

#### UI
- [x] `ui/connection/ConnectionScreen.kt` — State-aware Compose UI: StatusCard, DeviceList, scan/connect/disconnect
- [x] `ui/connection/ConnectionViewModel.kt` — scanDevices, connect, disconnect; collects state from BluetoothConnectionManager
- [x] `ui/theme/Color.kt` — Color palette
- [x] `ui/theme/Type.kt` — Typography
- [x] `ui/theme/Theme.kt` — Material 3 theme, dark/light

#### DI and App
- [x] `ObdInsightApplication.kt` — Empty Application class
- [x] `MainActivity.kt` — Uses ConnectionScreen composable with viewModel lookup
- [x] `di/AppModule.kt` — Singleton providers for BluetoothManager, Elm327Protocol, PermissionManager, ViewModel

#### Tests
- [x] `data/bluetooth/BluetoothConnectionManagerTest.kt` — State, sendCommand, getPairedDevices
- [x] `data/bluetooth/PermissionManagerTest.kt` — Permission checks, requiredPermissions
- [x] `data/elm327/Elm327ProtocolTest.kt` — Initialize, parse, execute
- [x] `ui/connection/ConnectionViewModelTest.kt` — scanDevices, disconnect

#### Documentation
- [x] `docs/VISION.md` — Project identity, learning goals, philosophy
- [x] `docs/SPECIFICATION.md` — Current system behavior, APIs, stack
- [x] `docs/ROADMAP.md` — 12-step feature progression
- [x] `docs/DECISIONS.md` — 9 ADRs logged
- [x] `docs/DIARY.md` — Development journal entry
- [x] `docs/SESSION_STATE.md` — Session continuity
- [x] `docs/features/bluetooth-connection/` — README, protocol, implementation, testing, observations
- [x] `docs/features/elm327-initialization/README.md`
- [x] `docs/features/obd2-protocols/README.md`
- [x] `docs/features/pid-reading/README.md`

#### Resources
- [x] `res/values/themes.xml` — Material Light theme
- [x] `res/drawable/ic_launcher_foreground.xml` — Vector drawable
- [x] `res/drawable/ic_launcher_background.xml` — Vector drawable
- [x] `res/values/ic_launcher_background.xml` — Background color resource
- [x] `res/mipmap-anydpi-v26/ic_launcher.xml` — Adaptive icon referencing foreground/background

---

## Phase 2 — OBD Communication ✅

**Goal**: Send OBD requests, parse responses, display sensor values.

| # | Feature | Status | Depends On | Deliverable |
|---|---|---|---|---|
| 3 | AT commands | ✅ | #2 | Send arbitrary AT, parse response |
| 4 | Protocol identification | ✅ | #3 | Detect vehicle protocol (CAN, ISO, etc.) |
| 5 | First OBD request | ✅ | #4 | Send Mode 01 PID 00 (supported PIDs) |
| 6 | Response parsing | ✅ | #5 | Decode hex to physical values |
| 7 | Basic sensor reading | ✅ | #6 | RPM, speed, coolant temp, etc. |

**Definition of Done**:
- [x] `Elm327Command.RawAt` variant for arbitrary AT commands
- [x] `sendCommand` updated to read multi-line responses
- [x] AT terminal screen with command input and response display
- [x] Protocol type model (enum with 10 protocols + Unknown)
- [x] Protocol detection via ATDPN/ATDP commands
- [x] Protocol card displayed on ConnectionScreen when connected
- [x] ObdPidReader for requesting and parsing OBD PID responses
- [x] ObdResponse domain model
- [x] Supported PIDs displayed on ConnectionScreen when connected
- [x] PidValueConverter decodes hex to physical values (RPM, speed, coolant temp, etc.)
- [x] ObdSensorReader polls PIDs periodically (1s interval)
- [x] DashboardScreen with live sensor cards (RPM, speed, coolant temp)
- [x] Error handling for unsupported PIDs (returns null from converter)

---

## Phase 3 — Persistence ✅

**Goal**: Record and review trips.

| # | Feature | Status | Depends On | Deliverable |
|---|---|---|---|---|
| 8 | Local persistence | ✅ | #6 | Room database, store readings |
| 9 | Trip recording | ✅ | #8 | Start/stop/resume trip |
| 11 | Statistics | ✅ | #9 | Per-trip analysis |

**Definition of Done**:
- [x] Room database with trips and readings tables
- [x] Trip recording (start/pause/resume/finish)
- [x] Trip history list
- [x] Per-trip statistics (max RPM, average speed and max coolant temperature)

---

## Phase 3.5 — First-Use Flow & Physical Validation ⬜

**Goal**: Make the app usable on a phone that has never paired with an OBD adapter, then validate the full Bluetooth and OBD flow on real hardware.

| # | Feature | Status | Depends On | Deliverable |
|---|---|---|---|---|
| 10 | Bluetooth permissions | ✅ | #1 | Runtime permission request and clear denied-state UI |
| 11 | Device discovery and pairing | ✅ | #10 | Discover nearby Bluetooth Classic devices and guide pairing |
| 12 | Physical validation | ⬜ | #11 | Validate ELM327 initialization, protocol detection and PIDs on a vehicle |

**Definition of Done**:
- [x] Request `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` at runtime on API 31+, or location on older versions
- [x] Show actionable states when Bluetooth is unavailable, disabled or permission is denied
- [x] Discover nearby Bluetooth Classic devices in addition to already paired devices
- [x] Let the user start Android's pairing flow for a discovered adapter
- [x] Keep paired and discovered devices clearly distinguishable in the UI
- [x] Read ELM327 responses until the `>` prompt instead of relying on a fixed delay and `ready()`
- [x] Reuse one input stream per socket and serialize commands
- [x] Apply command timeout and close failed sockets
- [x] Propagate ELM327 handshake failures to the connection UI
- [x] Keep paired devices available when nearby discovery fails
- [x] Distinguish discovery failure from missing Bluetooth permission
- [ ] Test connection, AT initialization, protocol detection and sensor polling on a physical Android device with an ELM327 and an energized OBD-II port
- [ ] Record a test trip and verify its readings and statistics in local Room storage

**Development validation tools**:
- Use Android Studio or `adb` to deploy the debug build directly to the phone; downloading APKs manually is not required
- Use Android Studio Logcat or `adb logcat` to inspect Bluetooth and OBD diagnostics during a test
- Use the emulator only for build, navigation, UI and local persistence checks; it cannot validate Bluetooth Classic RFCOMM or an ELM327

---

## Phase 4 — Dashboard & Analysis 🔄

**Goal**: Visualize data in real-time and historically.

| # | Feature | Status | Depends On | Deliverable |
|---|---|---|---|---|
| 10 | Dashboard | ⬜ | #7, #9 | Real-time gauges, charts |
| 12 | Historical analysis | ⬜ | #8, #11 | Chart history, export |
| 16 | PID expansion | ✅ | #7 | Dynamic supported PID blocks and expanded conversion formulas |

**Definition of Done**:
- [ ] Real-time gauge UI (RPM, speed, coolant temp)
- [ ] Chart history for any recorded parameter
- [ ] Data export (CSV or similar)
- [x] Dynamic supported PID blocks and expanded sensor conversion catalog
- [ ] Selectable sensor groups and configurable polling interval
- [ ] Diagnostic trouble codes and vehicle information

---

## Phase 5 — Local Development Telemetry (Deferred) ⏸️

**Goal**: Optionally mirror diagnostic data from a phone to a notebook on the local network. This is deferred until local sensor analysis and dashboard capabilities are mature.

| # | Feature | Status | Depends On | Deliverable |
|---|---|---|---|---|
| 13 | Local telemetry API | ⬜ | Phase 3.5 | Minimal Go service running on the notebook |
| 14 | Diagnostic event delivery | ⬜ | #13 | Connection, AT command, response and error events sent to the notebook |
| 15 | Reading delivery | ⬜ | #13 | Sensor readings and trip/session metadata sent to the notebook |

**Architecture constraints**:
- Room remains the app's local source of truth; Bluetooth and trip recording must work with no network connection
- The API is an optional development and inspection tool, not a production dependency
- API delivery failures must not interrupt Bluetooth communication or discard locally recorded data
- Start with a local HTTP REST API: `GET /health`, `POST /sessions`, `POST /events` and `POST /readings`
- Defer WebSocket, cloud hosting, authentication and synchronization until there is a concrete product need
