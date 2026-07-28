# OBD Insight — Architecture Decision Records

This file logs every significant architecture decision with context, options considered, and rationale.

---

## ADR-001: Clean Architecture Simplified

**Context**: Need a project structure that supports learning Android architecture without overengineering.

**Options considered**:
1. **Feature-first (flat)** — `bluetooth/`, `elm327/`, etc. each with their own UI, data, and models.
2. **Hexagonal / Ports & Adapters** — Domain ports with adapter implementations.
3. **Clean Architecture simplified** — `data/domain/ui` separation.

**Decision**: Option 3.

**Rationale**:
- Clear separation without excess layers
- Each layer maps to a learning concept (domain models, data sources, UI)
- Easy to add Room repository later (just another `data/` class)
- Manual DI scales fine for <10 dependencies

**Rejected alternatives**:
- Feature-first mixes UI with data logic; ViewModel depends on Android Context directly; when dashboard + history arrive, no separation exists; harder to test in isolation.
- Hexagonal is overengineering for V1; interfaces for everything before understanding the domain; violates YAGNI.

---

## ADR-002: Manual Dependency Injection (no Hilt/Dagger)

**Context**: Need DI approach for a small learning project.

**Options considered**:
1. **Hilt** — Annotation-based DI
2. **Dagger** — Manual DI with annotation processing
3. **Manual DI** — Object with factory methods

**Decision**: Option 3 (manual DI via `AppModule` singleton).

**Rationale**: Scales fine for <10 dependencies; no annotation processing overhead; simpler to understand and debug.

---

## ADR-003: StateFlow for Reactive State

**Context**: Need to expose connection state changes from BluetoothConnectionManager to the UI.

**Options considered**:
1. **LiveData** — Android-standard lifecycle-aware
2. **StateFlow** — Kotlin-idiomatic, coroutine-native
3. **SharedFlow** — For one-shot events

**Decision**: `StateFlow` for state, with `StateFlow` also for device list.

**Rationale**: Coroutine-native, works well with Compose `collectAsState()`, explicit initial value, no Android dependency in domain.

---

## ADR-004: CoroutineDispatcher Injection

**Context**: Need to test BluetoothConnectionManager without real I/O.

**Decision**: Inject `ioDispatcher: CoroutineDispatcher = Dispatchers.IO` as constructor parameter.

**Rationale**: Tests can pass `Dispatchers.Unconfined` or `TestCoroutineDispatcher`; default to `Dispatchers.IO` in production.

---

## ADR-005: Sealed BluetoothResult Instead of Exceptions

**Context**: Error handling for Bluetooth operations.

**Options considered**:
1. **Raw exceptions** — Try/catch at call site
2. **Sealed result** — `BluetoothResult.Success<T>` / `BluetoothResult.Error`

**Decision**: Option 2 (sealed result).

**Rationale**: Forces exhaustive handling at compile time; no unexpected crashes from uncaught exceptions; idiomatic Kotlin.

---

## ADR-006: Connection State as Sealed Interface

**Context**: Need to represent the state machine transitions.

**Decision**: Sealed interface `ConnectionState` with explicit states (`Disconnected`, `Scanning`, `FoundDevices`, `Connecting`, `Connected`, `Error`).

**Rationale**: Exhaustive `when` guarantees all states are handled in the UI; no invalid/illegal states representable.

---

## ADR-007: BluetoothConnectionManager Handles Socket + I/O

**Context**: Where to place the Bluetooth socket and stream I/O.

**Decision**: Unified in `BluetoothConnectionManager` (connect, disconnect, sendCommand).

**Rationale**: Keeps the socket reference and I/O streams in one place; Elm327Protocol only handles command serialization and response parsing, not raw I/O.

---

## ADR-008: Sealed Elm327Command Instead of String Constants

**Context**: Need to represent ELM327 commands in a type-safe way.

**Decision**: Sealed class `Elm327Command` with typed variants (`Reset`, `EchoOff`, `ReadPid(mode, pid)`, etc.).

**Rationale**: Compile-time safety for command types; `ReadPid` formats hex values automatically; easy to enumerate all supported commands.

---

## ADR-009: Testing Stack (JUnit 4 + MockK + Turbine)

**Context**: Testing framework for unit tests.

**Decision**: JUnit 4 (Android standard), MockK (Kotlin mocking), Turbine (Flow assertions), kotlinx-coroutines-test (runTest).

**Rationale**: Standard Android testing stack; MockK is Kotlin-idiomatic; Turbine provides readable Flow assertions.
