# OBD-II Protocols — Feature Overview

**Status**: Pending (not implemented)

## Purpose

Identify and interact with the vehicle's OBD-II protocol. Modern vehicles typically use CAN (ISO 15765-4), but older vehicles may use ISO 9141-2, KWP2000, PWM, or VPW.

## Protocols

| Protocol | Bus Type | Typical Vehicles |
|---|---|---|
| ISO 15765-4 (CAN) | CAN (11-bit / 29-bit) | Most cars after ~2008 |
| ISO 9141-2 | K-line | Chrysler, European, Asian (pre-2004) |
| ISO 14230 (KWP2000) | K-line | Similar to ISO 9141, slower |
| SAE J1850 PWM | PWM | Ford vehicles |
| SAE J1850 VPW | VPW | GM vehicles |

## ELM327 Protocol Detection

`ATSP0` tells the ELM327 to auto-detect the protocol. The response header (enabled via `ATH1`) identifies which protocol is in use.

## Next Steps

- Parse `ATH1` response header to identify protocol
- Configure protocol-specific behavior if needed
- Document the protocol interaction differences
