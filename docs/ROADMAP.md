# OBD Insight — Roadmap

## Milestones

### Phase 1 — Foundation (current) ✅

**Goal**: Bootable app that connects to ELM327 and initializes the chip.

| # | Feature | Depends On | Deliverable |
|---|---|---|---|
| 1 | Bluetooth connection | — | Scan paired devices, connect, show state |
| 2 | ELM327 initialization | #1 | ATZ, ATE0, ATL0, ATS0, protocol detection |

**Definition of Done**:
- [x] Project scaffolding (Gradle, manifest, dependencies)
- [x] Domain models (BluetoothError, BluetoothResult, ConnectionState)
- [x] Bluetooth layer (BluetoothConnectionManager, PermissionManager)
- [x] ELM327 layer (Elm327Command, Elm327Response, Elm327Protocol)
- [x] UI (ConnectionScreen, ConnectionViewModel)
- [x] DI (AppModule)
- [x] Theme (Material 3, dark/light)
- [x] Documentation (VISION, SPECIFICATION, DECISIONS, DIARY, feature docs)
- [x] Build config (proguard-rules.pro)
- [x] Resources (themes.xml, launcher icons)
- [ ] Unit tests (BluetoothConnectionManager, PermissionManager, Elm327Protocol, ConnectionViewModel)

---

### Phase 2 — OBD Communication (next)

**Goal**: Send OBD requests, parse responses, display sensor values.

| # | Feature | Depends On | Deliverable |
|---|---|---|---|
| 3 | AT commands | #2 | Send arbitrary AT, parse response |
| 4 | Protocol identification | #3 | Detect vehicle protocol (CAN, ISO, etc.) |
| 5 | First OBD request | #4 | Send Mode 01 PID 00 (supported PIDs) |
| 6 | Response parsing | #5 | Decode hex to physical values |
| 7 | Basic sensor reading | #6 | RPM, speed, coolant temp, etc. |

**Definition of Done**:
- [ ] Protocol detection screen showing detected protocol
- [ ] Real-time RPM, speed, coolant temp displayed
- [ ] Error handling for unsupported PIDs
- [ ] DashboardScreen with live gauges

---

### Phase 3 — Persistence

**Goal**: Record and review trips.

| # | Feature | Depends On | Deliverable |
|---|---|---|---|
| 8 | Local persistence | #6 | Room database, store readings |
| 9 | Trip recording | #8 | Start/stop trip, aggregate data |
| 11 | Statistics | #9 | Per-trip analysis |

**Definition of Done**:
- [ ] Room database with readings table
- [ ] Trip recording (start/stop/resume)
- [ ] Trip history list
- [ ] Per-trip statistics (max RPM, avg speed, etc.)

---

### Phase 4 — Dashboard & Analysis

**Goal**: Visualize data in real-time and historically.

| # | Feature | Depends On | Deliverable |
|---|---|---|---|
| 10 | Dashboard | #7, #9 | Real-time gauges, charts |
| 12 | Historical analysis | #8, #11 | Chart history, export |

**Definition of Done**:
- [ ] Real-time gauge UI (RPM, speed, coolant temp)
- [ ] Chart history for any recorded parameter
- [ ] Data export (CSV or similar)
