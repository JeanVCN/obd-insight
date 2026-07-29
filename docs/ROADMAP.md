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

## Phase 3 — Persistence ⬜

**Goal**: Record and review trips.

| # | Feature | Status | Depends On | Deliverable |
|---|---|---|---|---|
| 8 | Local persistence | ⬜ | #6 | Room database, store readings |
| 9 | Trip recording | ⬜ | #8 | Start/stop trip, aggregate data |
| 11 | Statistics | ⬜ | #9 | Per-trip analysis |

**Definition of Done**:
- [ ] Room database with readings table
- [ ] Trip recording (start/stop/resume)
- [ ] Trip history list
- [ ] Per-trip statistics (max RPM, avg speed, etc.)

---

## Phase 4 — Dashboard & Analysis ⬜

**Goal**: Visualize data in real-time and historically.

| # | Feature | Status | Depends On | Deliverable |
|---|---|---|---|---|
| 10 | Dashboard | ⬜ | #7, #9 | Real-time gauges, charts |
| 12 | Historical analysis | ⬜ | #8, #11 | Chart history, export |

**Definition of Done**:
- [ ] Real-time gauge UI (RPM, speed, coolant temp)
- [ ] Chart history for any recorded parameter
- [ ] Data export (CSV or similar)
