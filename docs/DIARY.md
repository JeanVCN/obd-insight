# OBD Insight — Development Diary

## 2026-07-27 — Session 1

### What was done
- Project scaffolding: Gradle wrapper, version catalog, build files, AndroidManifest
- Domain models: `BluetoothError`, `BluetoothResult`, `ConnectionState`
- Bluetooth layer: `BluetoothConnectionManager` (connect, disconnect, sendCommand, state flow), `PermissionManager`
- ELM327 layer: `Elm327Command`, `Elm327Response`, `Elm327Protocol` (initialization, execute, parse)
- UI: `ConnectionScreen`, `ConnectionViewModel` with full state handling
- DI: `AppModule` with singleton providers
- Theme: `Color.kt`, `Type.kt`, `Theme.kt` (Material 3, dark/light)
- Documentation: `VISION.md`, `SPECIFICATION.md`, `ROADMAP.md`, `DECISIONS.md`, `DIARY.md`, feature docs

### Issues encountered
- Write tool fails with content > ~200 chars
- Bash heredoc/command limit ~300 chars
- `Elm327Command.kt` corrupted on write (lines 14-15 garbled) — fixed via re-write
- `BluetoothConnectionManager.kt` truncated during write — `sendCommand` method completed
- The `res/` directory was tagged as missing but actually exists (empty subdirectories)

### Next steps
- Create `app/proguard-rules.pro`
- Create resource files (themes.xml, launcher icons)
- Write unit tests

---

## 2026-07-29 — Session 2

### What was done
- Build upgrade: Gradle 8.7 → 9.4.0, AGP 8.4.0 → 9.0.1, Kotlin 1.9.24 → 2.0.21 (fix JDK 26 compatibility)
- Feature 3 — AT commands: `Elm327Command.RawAt`, multi-line `sendCommand`, `AtTerminalScreen` + `AtTerminalViewModel`
- Feature 4 — Protocol identification: `ProtocolType` sealed class (10 protocols), `detectProtocol()` via ATDPN/ATDP
- Feature 5 — First OBD request (Mode 01 PID 00): `ObdPidReader` with hex parsing, `ObdResponse` model
- Feature 6 — Response parsing: `PidValueConverter` with 9 conversion formulas (RPM, speed, coolant temp, MAF, etc.)
- Feature 7 — Dashboard: `ObdSensorReader` polling RPM/Speed/Coolant at 1s interval, `DashboardViewModel`, `DashboardScreen` with live sensor cards
- Navigation: `ConnectionScreen` → `DashboardScreen` via NavHost
- Docs: `ROADMAP.md` reorganized as single source of truth, `SESSION_STATE.md` slimmed to 38 lines, `GO_REFERENCE.md` with Go comparison
- Tests: 33 → 57 tests across 11 test classes, covering all ViewModels, PidValueConverter, ProtocolType matching, ObdSensorReader, etc.

### Issues encountered
- PermissionManagerTest used `context.checkPermission()` which correctly matches `ContextCompat.checkSelfPermission` via concrete method delegation
- `ProtocolType.fromDescription` had J1939 matching after CAN branch, causing J1939 descs containing "can" to match as CAN instead — fixed by reordering
- `StandardTestDispatcher` for ViewModel coroutine tests needs `UnconfinedTestDispatcher` or explicit `advanceUntilIdle()`
- Unused `initialized` field in `Elm327Protocol` — removed
- `BluetoothConnectionManager` test uses reflection to inject mock socket — fragile but necessary without DI refactoring

### Key decisions
- `ObdPidReader` manually strips OBD response headers by searching for the mode byte pattern, supporting both ATH1 and ATH0 responses
- `PidValueConverter` returns `null` for unsupported PIDs (no crash, just no display)
- `ObdSensorReader` uses sequential polling (not parallel) for simplicity and to avoid overwhelming the ELM327
- `DashboardViewModel` uses `viewModelScope` for lifecycle-bound collection with a `collecting` flag to prevent double-start
