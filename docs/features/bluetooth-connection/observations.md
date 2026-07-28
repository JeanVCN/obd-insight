# Bluetooth Connection — Observations

## Real Behavior Notes

### Mitsubishi Lancer GT 2014 (Primary)

- **Adapter**: ELM327 Bluetooth (generic v1.5)
- **Pairing**: Standard Android BT pairing, no issues
- **SPP UUID**: Standard `00001101-0000-1000-8000-00805F9B34FB` works
- **Connection**: ~2 seconds from tap to Connected state
- **ATZ response**: `ELM327 v1.5` (takes ~1s after reset)

### Chevrolet Astra GSI 2005 (Future)

Not yet tested. May use a different OBD protocol (likely ISO 9141-2 or VPW).

## Known Considerations

- `BluetoothAdapter.getDefaultAdapter()` returns `null` on devices without Bluetooth (emulator, tablet). Always check for null.
- `cancelDiscovery()` before `connect()` is important — discovery uses significant radio resources and can interfere with connection.
- Socket `connect()` is a blocking call — must run on IO dispatcher.
- ELM327 initialization takes ~3-5 seconds (7 AT commands with response validation).
