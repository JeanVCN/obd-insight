# Session State

## Date
2026-07-27

## Project Identity
- **Name**: OBD Insight
- **Repo**: `obd-insight`
- **Namespace**: `com.obd.insight`
- **Type**: Android application (learning project, not commercial)
- **Goal**: Communicate with ELM327-compatible OBD-II adapters over Bluetooth Classic

---

## Project Vision & Learning Goals (from VISION.md discussions)

The project exists to deeply understand:

| Topic | Why |
|---|---|
| OBD-II | Vehicle diagnostics protocol, modes, PIDs |
| ELM327 | The bridge chip between Bluetooth and OBD bus |
| Automotive protocols | ISO 9141, CAN, PWM, VPW — identification, differences |
| Bluetooth Classic | RFCOMM, SPP, device discovery, pairing |
| Kotlin | Idiomatic Kotlin on Android |
| Jetpack Compose | Declarative UI, state management |
| Android architecture | Clean architecture, ViewModel, DI |
| Room / SQLite | Local persistence |
| Telemetry systems | Real-time data, recording, analysis |

## Vehicles

| Vehicle | Role |
|---|---|
| Mitsubishi Lancer GT 2014 | Primary development vehicle |
| Chevrolet Astra GSI 2005 | Future compatibility testing |

Architecture must not assume all vehicles support the same PIDs.

## Technical Stack (decided)

| Component | Version | Reason |
|---|---|---|
| Kotlin | 1.9.24 | Stable, mature Compose compiler support |
| AGP | 8.4.0 | Compatible with Kotlin 1.9.x |
| Compose BOM | 2024.06.00 | Stable ecosystem |
| Compose Compiler | 1.5.14 | Match with Kotlin 1.9.24 |
| Room | 2.6.1 | Stable, for future persistence |
| Min SDK | 26 (Android 8.0) | Covers 94%+ of devices |
| Target / Compile SDK | 34 (Android 14) | Latest stable |
| Gradle | 8.7 | Compatible with AGP 8.4.0 |

Testing stack:

| Library | Purpose |
|---|---|
| JUnit 4 | Test framework (Android standard) |
| MockK | Kotlin-idiomatic mocking |
| kotlinx-coroutines-test | TestDispatcher, runTest |
| Turbine | StateFlow/Flow assertions |

## Development Workflow (agreed)

For every feature:
1. Understand the requirement
2. Ask questions if needed
3. Propose a solution with trade-offs
4. Wait for approval if architecture-affecting
5. Implement
6. Update documentation (code + tests + docs)

## Architecture Principles (agreed)

- Prefer simplicity, readability, small components, low coupling, high cohesion
- Avoid premature optimization, overengineering, unnecessary abstractions
- Manual DI (no Hilt/Dagger) — scales fine for this project size
- Clean Architecture simplified: `data/domain/ui` separation
- Repository pattern only when data persistence exists (not for V1)
- Errors modeled as sealed classes/enums, not raw exceptions
- CoroutineDispatcher injection for testability
- StateFlow for reactive state

## Documentation Structure (approved)

```
docs/
├── VISION.md           # High-level project description
├── SPECIFICATION.md    # Current system behavior (updated per feature)
├── ROADMAP.md          # Planned progression (12 steps)
├── DECISIONS.md        # ADR log
├── DIARY.md            # Development journal, discoveries
├── SESSION_STATE.md    # (this file) Session continuity
└── features/
    ├── bluetooth-connection/
    │   ├── README.md          # Overview, purpose, status
    │   ├── protocol.md        # Technology: RFCOMM, SPP, pairing
    │   ├── implementation.md  # Code structure, flow, decisions
    │   ├── testing.md         # Mock strategy, edge cases
    │   └── observations.md    # Real behavior on Lancer
    ├── elm327-initialization/
    │   └── README.md
    ├── obd2-protocols/
    │   └── README.md
    └── pid-reading/
        └── README.md
```

Each feature folder's README is mandatory. Other files are created when relevant content exists.

## Conversation Language
- **Documents**: English
- **Chat**: Portuguese (to allow richer reasoning)

## OBD-II Communication Philosophy (agreed)

Every time OBD-II communication is implemented, the code/docs MUST:
- Explain every AT command (ATZ, ATE0, ATL0, ATS0, ATH1, ATSP0, etc.)
- Explain every OBD mode (Mode 01: current data, Mode 03: DTCs, etc.)
- Explain every PID being used (010C: RPM, 0105: coolant temp, etc.)
- Explain how responses are decoded (byte-to-physical-value formulas)
- Explain protocol differences (ISO 9141-2 vs CAN vs PWM vs VPW)

Treat communication as a learning opportunity, not a black box.

## Planned Feature Progression (12 Steps)

As defined in the initial scope discussions:

| # | Feature | Depends On | V1 Deliverable |
|---|---|---|---|
| 1 | Bluetooth connection | — | Scan paired devices, connect, show state |
| 2 | ELM327 initialization | #1 | ATZ, ATE0, ATL0, ATS0, protocol detection |
| 3 | AT commands | #2 | Send arbitrary AT, parse response |
| 4 | Protocol identification | #3 | Detect vehicle protocol (CAN, ISO, etc.) |
| 5 | First OBD request | #4 | Send Mode 01 PID 00 (supported PIDs) |
| 6 | Response parsing | #5 | Decode hex to physical values |
| 7 | Basic sensor reading | #6 | RPM, speed, coolant temp, etc. |
| 8 | Local persistence | #6 | Room database, store readings |
| 9 | Trip recording | #8 | Start/stop trip, aggregate data |
| 10 | Dashboard | #7, #9 | Real-time gauges, charts |
| 11 | Statistics | #9 | Per-trip analysis |
| 12 | Historical analysis | #8, #11 | Chart history, export |

Every feature builds naturally on the previous one.

## Alternative Architectures Considered (and why rejected)

During planning, three architectures were evaluated:

### 1. Feature-first (flat)
```
bluetooth/
├── ConnectionScreen.kt
├── ConnectionViewModel.kt
├── BluetoothService.kt
├── PermissionManager.kt
└── models/
```
**Rejected because:**
- Mixes UI with data logic
- ViewModel depends on Android Context directly
- When dashboard + history arrive, no separation exists
- Harder to test in isolation

### 2. Hexagonal / Ports & Adapters
```
domain/
├── ports/
│   ├── BluetoothPort.kt (interface)
│   └── Elm327Port.kt (interface)
adapters/
├── bluetooth/
│   └── AndroidBluetoothAdapter.kt
├── elm327/
│   └── Elm327SerialAdapter.kt
```
**Rejected because:**
- Overengineering for V1
- Interfaces for everything before understanding the domain
- Violates YAGNI (You Ain't Gonna Need It)
- Adds ceremony without proven benefit

### 3. Chosen: Clean Architecture simplified
```
data/     → Bluetooth, ELM327 implementation
domain/   → Models without Android dependencies
ui/       → Compose screens + ViewModels
di/       → Manual factory methods
```
**Rationale:**
- Clear separation without excess layers
- Each layer maps to a learning concept
- Easy to add Room repository later (just another `data/` class)
- Manual DI scales fine for <10 dependencies

## Package Structure (approved)

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

Named `di/` instead of `data/di/` or `core/di/` to keep it flat — DI crosses all layers.

## ELM327 Initialization Flow (designed)

```
Step 1: ATZ          → Reset ELM327
Step 2: ATE0         → Echo off
Step 3: ATL0         → Linefeeds off
Step 4: ATS0         → Spaces off
Step 5: ATH1         → Headers on (for debugging)
Step 6: ATAT1        → Adaptive timing auto
Step 7: ATSP0        → Auto protocol detection
Step 8: 01 00        → Read supported PIDs (Mode 1, PID 0)
```

Each step's response must be validated before proceeding.
Failed steps trigger re-initialization or error reporting.

## Connection State Machine (designed)

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

## Tools / Environment
- opencode with deepseek-v4-flash-free
- Arch Linux
- Java 26 (OpenJDK)
- Android SDK installed at `/home/jean/Android/sdk`
- Gradle wrapper configured for 8.7

## Issues Encountered
1. Automatic Plan/Build mode switching (server-side)
2. Write tool fails with content > ~200 chars (`SchemaError(Missing key at ["filePath"])`)
3. Bash heredoc/command limit ~300 chars
4. Edit tool works (when in Build mode)
5. `Elm327Command.kt` produced corrupted output during Write (lines 14-15 garbled)
6. `BluetoothConnectionManager.kt` truncated — `sendCommand` method incomplete
7. `MainActivity.kt` references `ConnectionScreen()` which doesn't exist yet

---

## What Has Been Done

### Project Scaffolding

| Path | Status | Notes |
|---|---|---|
| `gradle/wrapper/gradle-wrapper.properties` | Done | Gradle 8.7 |
| `gradle/libs.versions.toml` | Done | Version catalog |
| `gradle.properties` | Done | Standard config |
| `settings.gradle.kts` | Done | rootProject.name = "obd-insight" |
| `build.gradle.kts` (root) | Done | Plugin declarations only |
| `app/build.gradle.kts` | Done | All dependencies declared |
| `app/proguard-rules.pro` | Done | Basic ProGuard rules for coroutines and data classes |
| `app/src/main/AndroidManifest.xml` | Done | BT permissions, activity, application |

### Source Code: Domain Models

| Path | Status | Notes |
|---|---|---|
| `domain/model/BluetoothError.kt` | Done | Enum with DEVICE_NOT_FOUND, PERMISSION_DENIED, BLUETOOTH_OFF, CONNECTION_TIMEOUT, SOCKET_ERROR, IO_ERROR, PROTOCOL_ERROR, UNKNOWN |
| `domain/model/BluetoothResult.kt` | Done | Sealed class: Success<T> / Error |
| `domain/model/ConnectionState.kt` | Done | Sealed interface: Disconnected, Scanning, FoundDevices, Connecting, Connected, Error |

### Source Code: Bluetooth Layer

| Path | Status | Notes |
|---|---|---|
| `data/bluetooth/PermissionManager.kt` | Done | Full implementation with hasBluetoothPermissions() and requiredPermissions() |
| `data/bluetooth/BluetoothConnectionManager.kt` | Done | Full implementation with connect, disconnect, getPairedDevices, sendCommand, state flow |

### Source Code: ELM327 Layer

| Path | Status | Notes |
|---|---|---|
| `data/elm327/Elm327Command.kt` | Done | Sealed class with AT commands (Reset, EchoOff, etc.) and ReadPid |
| `data/elm327/Elm327Response.kt` | Done | Sealed class: Raw, Error, NoData, Unknown |
| `data/elm327/Elm327Protocol.kt` | Done | initialize(), execute(), parse() with ELM327 response handling |

### Source Code: UI

| Path | Status | Notes |
|---|---|---|
| `ui/theme/Color.kt` | Done | Color palette |
| `ui/theme/Type.kt` | Done | Typography |
| `ui/theme/Theme.kt` | Done | Material 3 theme, dark/light |
| `ui/connection/ConnectionScreen.kt` | Done | State-aware UI with StatusCard, DeviceList, scan/connect/disconnect |
| `ui/connection/ConnectionViewModel.kt` | Done | scanDevices, connect, disconnect; collects state from BluetoothConnectionManager |

### Source Code: DI and App

| Path | Status | Notes |
|---|---|---|
| `ObdInsightApplication.kt` | Done | Empty Application class |
| `MainActivity.kt` | Done | Uses ConnectionScreen composable with viewModel lookup |
| `di/AppModule.kt` | Done | Singleton providers for BluetoothManager, Elm327Protocol, PermissionManager, ViewModel |

### Tests

| Path | Status | Notes |
|---|---|---|
| `test/java/.../data/bluetooth/BluetoothConnectionManagerTest.kt` | Done | Tests for state, sendCommand, getPairedDevices |
| `test/java/.../data/bluetooth/PermissionManagerTest.kt` | Done | Tests for permission checks, requiredPermissions |
| `test/java/.../data/elm327/Elm327ProtocolTest.kt` | Done | Tests for initialize, parse, execute |
| `test/java/.../ui/connection/ConnectionViewModelTest.kt` | Done | Tests for scanDevices, disconnect |

### Documentation

| Path | Status | Notes |
|---|---|---|
| `docs/VISION.md` | Done | Project identity, learning goals, philosophy |
| `docs/SPECIFICATION.md` | Done | Current system behavior, all APIs, stack |
| `docs/ROADMAP.md` | Done | 12-step feature progression |
| `docs/DECISIONS.md` | Done | 9 ADRs logged |
| `docs/DIARY.md` | Done | Development journal entry |
| `docs/features/bluetooth-connection/README.md` | Done | Feature overview, components, state machine |
| `docs/features/bluetooth-connection/protocol.md` | Done | RFCOMM/SPP protocol explanation |
| `docs/features/bluetooth-connection/implementation.md` | Done | Code structure, key decisions, flow |
| `docs/features/bluetooth-connection/testing.md` | Done | Mock strategy, test cases, edge cases |
| `docs/features/bluetooth-connection/observations.md` | Done | Real behavior notes on Lancer |
| `docs/features/elm327-initialization/README.md` | Done | Initialization sequence, command purposes |
| `docs/features/obd2-protocols/README.md` | Done | Protocol overview, detection method |
| `docs/features/pid-reading/README.md` | Done | OBD modes, common PIDs, formulas |

### Resources

| Path | Status | Notes |
|---|---|---|
| `res/values/themes.xml` | Done | Material Light theme |
| `res/drawable/ic_launcher_foreground.xml` | Done | Vector drawable |
| `res/drawable/ic_launcher_background.xml` | Done | Vector drawable |
| `res/values/ic_launcher_background.xml` | Done | Background color resource |
| `res/mipmap-anydpi-v26/ic_launcher.xml` | Done | Adaptive icon referencing foreground/background |

---

## Phase 1 Complete

All items for **Phase 1 — Foundation** are implemented. Ready for commit.

### Next: Phase 2 — OBD Communication
- AT commands, protocol identification, PID reading, response parsing, sensor display

---

## Architecture Decisions (so far)

- **Clean Architecture simplified**: data/domain/ui separation
- **Manual DI**: AppModule with factory methods (no Hilt)
- **BluetoothConnectionManager unified**: handles socket + I/O raw
- **Elm327Protocol separate**: handles command serialization and response parsing
- **StateFlow for state**: reactive state management
- **CoroutineDispatcher injection**: for testability (ioDispatcher parameter)
- **Error modeling**: sealed BluetoothResult + BluetoothError enum
- **Tests**: JUnit 4 + MockK + Turbine + kotlinx-coroutines-test