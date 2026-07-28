# OBD Insight — Vision

## Project Identity

**OBD Insight** is an Android application that communicates with ELM327-compatible OBD-II adapters over Bluetooth Classic. It is a **learning project** — built to deeply understand vehicle diagnostics protocols, Bluetooth communication, and modern Android development.

## Learning Goals

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

## Philosophy

Every OBD-II communication in code and docs MUST:
- Explain every AT command (ATZ, ATE0, ATL0, ATS0, ATH1, ATSP0, etc.)
- Explain every OBD mode (Mode 01: current data, Mode 03: DTCs, etc.)
- Explain every PID being used (010C: RPM, 0105: coolant temp, etc.)
- Explain how responses are decoded (byte-to-physical-value formulas)
- Explain protocol differences (ISO 9141-2 vs CAN vs PWM vs VPW)

Treat communication as a learning opportunity, not a black box.

## Documentation Language

- **Documents**: English
- **Chat**: Portuguese (to allow richer reasoning)
