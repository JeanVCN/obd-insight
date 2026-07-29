# OBD Insight — Referência Go

> Documento local de comparação com Go para aprendizado.
> Evolui junto com o projeto. Não versionado (`.gitignore`).

---

## Índice

1. [Visão Geral do Projeto](#1-visão-geral-do-projeto)
2. [Arquitetura vs Go](#2-arquitetura-vs-go)
3. [Sintaxe Kotlin vs Go](#3-sintaxe-kotlin-vs-go)
4. [Build System](#4-build-system)
5. [DI (Injeção de Dependência)](#5-di-injeção-de-dependência)
6. [Corrotinas vs Goroutines](#6-corrotinas-vs-goroutines)
7. [StateFlow vs Canais](#7-stateflow-vs-canais)
8. [ViewModel Pattern](#8-viewmodel-pattern)
9. [Sealed Classes vs Interfaces](#9-sealed-classes-vs-interfaces)
10. [UI com Compose vs Alternativas Go](#10-ui-com-compose-vs-alternativas-go)
11. [Testes](#11-testes)
12. [Bluetooth](#12-bluetooth)
13. [Data Classes vs Structs](#13-data-classes-vs-structs)
14. [OBD-II PID Handling](#14-obd-ii-pid-handling)
15. [Fluxo de Conexão Completo](#15-fluxo-de-conexão-completo)
16. [Observações e Decisões](#16-observações-e-decisões)

---

## 1. Visão Geral do Projeto

**OBD Insight** é um app Android que se comunica com um scanner ELM327 via Bluetooth Classic para ler dados do veículo (RPM, velocidade, temperatura, etc.).

**Stack atual:** Kotlin 2.0.21, Jetpack Compose, Material 3, Coroutines, Gradle 9.4, AGP 9.0.1

### O que seria em Go

Em Go, seria um **CLI + servidor HTTP/WebSocket** rodando em um Raspberry Pi (ou notebook com adaptador Bluetooth USB). Não existe equivalente mobile nativo em Go — você usaria:

| Alternativa | Descrição |
|---|---|
| `tinygo` + `tinygo/bluetooth` | Bluetooth LE apenas (não Classic). Não serve para ELM327. |
| `golang.org/x/mobile` | Bindings para Android/iOS, mas muito limitado e sem suporte a Bluetooth Classic. |
| **Gio** (`gioui.org`) | UI declarativa multiplataforma (desktop + mobile) similar ao Compose, mas sem suporte nativo a Bluetooth Classic. |
| `github.com/tarm/serial` | Comunicação serial via USB-ELM327 (se o adaptador for USB ao invés de Bluetooth). |

**Conclusão:** Um equivalente Go faria mais sentido como **daemon serial/bluetooth com API REST ou WebSocket**, consumido por um frontend web ou CLI.

```go
// Exemplo: daemon OBD em Go (conceitual)
package main

import (
    "fmt"
    "github.com/tarm/serial"
)

func main() {
    cfg := &serial.Config{Name: "/dev/ttyUSB0", Baud: 38400}
    port, err := serial.OpenPort(cfg)
    if err != nil {
        panic(err)
    }
    defer port.Close()

    port.Write([]byte("ATZ\r\n"))
    buf := make([]byte, 128)
    n, _ := port.Read(buf)
    fmt.Printf("ELM327: %s\n", buf[:n])
}
```

---

## 2. Arquitetura vs Go

### Kotlin (Clean Architecture simplificada)

```
app/src/main/java/com/obd/insight/
├── domain/model/          # Entidades puras (sem frameworks)
│   ├── ConnectionState.kt
│   ├── BluetoothResult.kt
│   ├── ProtocolType.kt
│   ├── PidValue.kt
│   └── ObdResponse.kt
├── data/
│   ├── bluetooth/         # Comunicação com hardware
│   ├── elm327/            # Protocolo ELM327
│   └── obd/               # Leitura OBD-II
├── ui/                    # Camada de apresentação
│   ├── connection/
│   ├── terminal/
│   └── dashboard/
└── di/                    # DI manual
```

### Equivalente em Go

```go
cmd/obd-daemon/            # Entrada do programa
    main.go
internal/
    domain/
        model/
            connection.go
            bluetooth.go
            protocol.go
            pid.go
    data/
        bluetooth/         # Gerenciamento Bluetooth/serial
        elm327/            # Comandos AT
        obd/               # PID reader
    api/                   # HTTP handlers (substitui a camada UI)
        handler.go
    service/               # Lógica de negócio
        reader.go
```

**Diferenças chave:**
- Go não tem `sealed class`, usa `interface` + `type switch`
- Go não tem `data class`, usa `struct` com getters/setters manuais ou métodos
- Go não tem package-private (public/private via maiúscula/minúscula)
- Clean Architecture em Go usa `internal/` para esconder implementação
- Go não tem ViewModel, usa handlers HTTP ou WebSocket

---

## 3. Sintaxe Kotlin vs Go

### Sealed Class / Union Types

**Kotlin:**
```kotlin
sealed class BluetoothResult<out T> {
    data class Success<T>(val data: T) : BluetoothResult<T>()
    data class Error(val reason: BluetoothError) : BluetoothResult<Nothing>()
}

// Uso com when (exaustivo)
when (result) {
    is BluetoothResult.Success -> handle(result.data)
    is BluetoothResult.Error -> handleError(result.reason)
}
```

**Go:**
```go
type BluetoothResult[T any] struct {
    Data   T
    Err    error
}

// Ou com interface + type switch (sem genéricos pré-1.18)
type Result interface {
    isResult()
}

type Success[T any] struct {
    Data T
}
func (s Success[T]) isResult() {}

type Error struct {
    Reason error
}
func (e Error) isResult() {}

// Uso
switch r := result.(type) {
case Success[PidValue]:
    fmt.Println(r.Data)
case Error:
    fmt.Println(r.Reason)
}
```

### Data Class / Struct

**Kotlin:**
```kotlin
data class PidValue(
    val pid: Int,
    val value: Float,
    val unit: String,
    val label: String
)
```

**Go:**
```go
type PidValue struct {
    Pid   int
    Value float64
    Unit  string
    Label string
}
```

### Named Arguments / Default Values

**Kotlin:**
```kotlin
fun connect(device: BluetoothDevice, timeout: Long = 5000)
connect(device)                           // usa default
connect(device, timeout = 10000)          // named argument
```

**Go:**
```go
// Go não tem default nem named arguments
// Padrão: Functional Options Pattern
type ConnectOpts struct {
    Timeout time.Duration
}

func Connect(device BluetoothDevice, opts ...ConnectOpts) {
    timeout := 5 * time.Second
    if len(opts) > 0 {
        timeout = opts[0].Timeout
    }
}
```

### Extension Functions

**Kotlin:**
```kotlin
fun String.toHexBytes(): List<Int> =
    chunked(2).filter { it.length == 2 }.map { it.toInt(16) }
```

**Go:**
```go
// Go não tem extension functions. Usa função avulsa ou método em tipo próprio.
type HexString string

func (h HexString) ToBytes() ([]byte, error) {
    return hex.DecodeString(string(h))
}
```

### Flow / Channels

**Kotlin (StateFlow):**
```kotlin
private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
val state: StateFlow<ConnectionState> = _state.asStateFlow()
```

**Go (channel):**
```go
type ConnectionManager struct {
    state chan ConnectionState
}

func NewConnectionManager() *ConnectionManager {
    cm := &ConnectionManager{
        state: make(chan ConnectionState, 1),
    }
    cm.state <- ConnectionStateDisconnected
    return cm
}
```

### Object (Singleton)

**Kotlin:**
```kotlin
object PidValueConverter {
    fun convert(pid: Int, data: List<Int>): PidValue? { ... }
}
```

**Go:**
```go
// Package-level functions (sem estado)
package converter

func Convert(pid int, data []byte) (*PidValue, error) { ... }

// Ou singleton com sync.Once
var (
    instance *MySingleton
    once     sync.Once
)

func GetInstance() *MySingleton {
    once.Do(func() {
        instance = &MySingleton{}
    })
    return instance
}
```

---

## 4. Build System

| Aspecto | Kotlin/Android | Go |
|---|---|---|
| Build tool | Gradle (Groovy/Kotlin DSL) | `go build`, `go test` |
| Dependências | `libs.versions.toml` (version catalog) | `go mod` + `go.mod` |
| Configuração | `build.gradle.kts` declarativo | `go.mod` + flags no código |
| Multi-módulo | Sim (Gradle subprojetos) | Sim (módulos Go) |
| Plugins | AGP, Kotlin, Compose | Não precisa (ferramentas separadas) |
| Version catalog | `[versions]`, `[libraries]`, `[plugins]` | Apenas `require` no `go.mod` |

**Exemplo Gradle (Kotlin DSL) vs Go:**

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.obd.insight"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        targetSdk = 35
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
}
```

```go
// go.mod
module github.com/jeanvcn/obd-insight

go 1.22

require (
    github.com/tarm/serial v0.0.0-20240109133652-6890a3c4f3a0
    go.bug.st/serial v1.6.2
)
```

---

## 5. DI (Injeção de Dependência)

### Kotlin (DI Manual)

```kotlin
object AppModule {
    private var bluetoothManager: BluetoothConnectionManager? = null

    fun provideBluetoothManager(): BluetoothConnectionManager {
        return bluetoothManager ?: BluetoothConnectionManager().also {
            bluetoothManager = it
        }
    }

    fun provideElm327Protocol(): Elm327Protocol {
        return Elm327Protocol(provideBluetoothManager())
    }

    val viewModelFactory = viewModelFactory {
        initializer {
            ConnectionViewModel(
                bluetoothManager = provideBluetoothManager(),
                elm327Protocol = provideElm327Protocol(),
                obdPidReader = provideObdPidReader()
            )
        }
    }
}
```

### Go (também manual, mas mais explícito)

```go
package di

type AppModule struct {
    btManager    *BluetoothManager
    elmProtocol  *Elm327Protocol
    pidReader    *ObdPidReader
    once         sync.Once
}

func (m *AppModule) BluetoothManager() *BluetoothManager {
    m.once.Do(func() {
        m.btManager = NewBluetoothManager()
    })
    return m.btManager
}

func (m *AppModule) Elm327Protocol() *Elm327Protocol {
    m.once.Do(func() {
        m.elmProtocol = NewElm327Protocol(m.BluetoothManager())
    })
    return m.elmProtocol
}
```

**Diferenças:**
- Kotlin usa `object` para singleton nativo
- Kotlin usa `lazy` via `?:` com `also`
- Go precisa gerenciar concorrência com `sync.Once`
- Wire (Google) é o equivalente Go a Dagger/Hilt — gera código de DI em compile-time

---

## 6. Corrotinas vs Goroutines

### Kotlin Coroutines

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val result = bluetoothManager.connect(device)
    if (result is BluetoothResult.Success) {
        elm327Protocol.initialize()
    }
}

// Sequencial vs paralelo
suspend fun foo() {
    val a = async { task1() }  // paralelo
    val b = async { task2() }  // paralelo
    println(a.await() + b.await())
}

// Com timeout
withTimeout(5000) {
    delay(10000) // cancela após 5s
}
```

### Go Goroutines

```go
go func() {
    result, err := btManager.Connect(device)
    if err == nil {
        elmProtocol.Initialize()
    }
}()

// Sequencial vs paralelo
func foo() {
    ch := make(chan int)
    go func() { ch <- task1() }()
    go func() { ch <- task2() }()
    a, b := <-ch, <-ch
    fmt.Println(a + b)
}

// Com timeout
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
defer cancel()

select {
case <-done:
    // sucesso
case <-ctx.Done():
    // timeout
}
```

### Comparação Direta

| Kotlin | Go |
|---|---|
| `viewModelScope.launch { }` | `go func() { }()` |
| `async/await` | `ch := make(chan T); go func() { ch <- result }(); <-ch` |
| `delay(ms)` | `time.Sleep(duration)` |
| `withTimeout` | `context.WithTimeout` |
| `Dispatchers.IO` | Runtime gerencia (GOMAXPROCS) |
| Estrutura hierárquica (escopo) | Flat (goroutines são independentes) |
| Cancelamento cooperativo via `isActive` | Cancelamento via `context.Context` |
| `Flow` (cold stream) | `chan` (hot) ou RxGo |

**Exemplo de polling de sensores:**

**Kotlin:**
```kotlin
fun readSensorValues(): Flow<List<PidValue>> = flow {
    while (true) {
        val values = mutableListOf<PidValue>()
        for (pid in pids) {
            val result = pidReader.requestPid(1, pid)
            if (result is BluetoothResult.Success) {
                PidValueConverter.convert(pid, result.data.data)?.let { values.add(it) }
            }
        }
        emit(values)
        delay(1000)
    }
}
```

**Go:**
```go
func (r *ObdSensorReader) ReadSensorValues(ctx context.Context, pids []int) <-chan []PidValue {
    ch := make(chan []PidValue)
    go func() {
        ticker := time.NewTicker(1 * time.Second)
        defer ticker.Stop()
        for {
            select {
            case <-ctx.Done():
                close(ch)
                return
            case <-ticker.C:
                var values []PidValue
                for _, pid := range pids {
                    data, err := r.pidReader.RequestPid(ctx, 1, pid)
                    if err == nil {
                        if v := converter.Convert(pid, data); v != nil {
                            values = append(values, *v)
                        }
                    }
                }
                ch <- values
            }
        }
    }()
    return ch
}
```

---

## 7. StateFlow vs Canais

### Kotlin (StateFlow)

```kotlin
private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
val state: StateFlow<ConnectionState> = _state.asStateFlow()

// Emitir
_state.value = ConnectionState.Connected("OBD-II")

// Coletar (no ViewModel)
viewModelScope.launch {
    bluetoothManager.state.collect { connectionState ->
        _state.value = connectionState
    }
}

// Coletar (no Compose)
val state by viewModel.state.collectAsState()
```

### Go (Canais)

```go
type ConnectionManager struct {
    state chan ConnectionState
}

func NewConnectionManager() *ConnectionManager {
    cm := &ConnectionManager{
        state: make(chan ConnectionState, 1),
    }
    cm.state <- Disconnected
    return cm
}

// Emitir (sem bloquear)
func (cm *ConnectionManager) SetState(s ConnectionState) {
    select {
    case <-cm.state: // esvazia buffer
    default:
    }
    cm.state <- s
}

// Consumir
go func() {
    for state := range cm.state {
        fmt.Println(state)
    }
}()
```

**Diferenças chave:**
- `StateFlow` sempre tem valor (buffer obrigatório)
- `StateFlow` desduplica valores iguais (não emite se `value` for o mesmo)
- `collect` é síncrono (bloqueia a corrotina)
- `StateFlow` é **cold** — só executa se tiver coletor
- Em Go, canais são **hot** — quem envia bloqueia até alguém receber (ou buffer)
- Go não tem desduplicação automática (precisa implementar)

---

## 8. ViewModel Pattern

### Kotlin (Android Architecture Components)

```kotlin
class ConnectionViewModel(
    private val bluetoothManager: BluetoothConnectionManager,
    private val elm327Protocol: Elm327Protocol,
    private val obdPidReader: ObdPidReader
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            bluetoothManager.state.collect { _state.value = it }
        }
    }

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            val result = bluetoothManager.connect(device)
            if (result is BluetoothResult.Success) {
                elm327Protocol.initialize()
            }
        }
    }

    override fun onCleared() {
        bluetoothManager.disconnect()
        super.onCleared()
    }
}
```

### Equivalente Go

```go
// Go não tem ViewModel. Equivalente mais próximo: Handler HTTP + service.
type ConnectionHandler struct {
    btManager  *BluetoothManager
    elmProto   *Elm327Protocol
    pidReader  *ObdPidReader
    connected  bool
    mu         sync.RWMutex
}

func (h *ConnectionHandler) Connect(w http.ResponseWriter, r *http.Request) {
    addr := r.URL.Query().Get("address")
    
    // Busca device
    device := h.btManager.GetDevice(addr)
    
    // Conecta (bloqueante, mas em goroutine separada)
    go func() {
        result := h.btManager.Connect(device)
        if result.Success() {
            h.elmProto.Initialize()
        }
    }()
    
    w.Write([]byte("connecting..."))
}
```

**Diferenças:**
- `ViewModel` sobrevive a rotações de tela (config changes) — lifecycle ciente
- Em Go desktop/web, não existe esse conceito — você gerencia estado manualmente
- `viewModelScope` amarra o ciclo de vida ao ViewModel
- Em Go, usa `context.Context` para cancelamento
- `onCleared()` em Go é `defer` ou `Close()` explícito

---

## 9. Sealed Classes vs Interfaces

Sealed classes são um dos recursos mais úteis do Kotlin que Go não tem equivalente direto.

### Kotlin (Sealed Class)

```kotlin
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val deviceName: String) : ConnectionState()
    data class Connected(val deviceName: String) : ConnectionState()
    data class Error(val error: BluetoothError, val message: String) : ConnectionState()
}

// Uso com when exaustivo
when (state) {
    ConnectionState.Disconnected -> scanButton()
    is ConnectionState.Connecting -> progressIndicator()
    is ConnectionState.Connected -> disconnectedButton()
    is ConnectionState.Error -> errorMessage()
}
```

### Go (Interface + Type Switch)

```go
type ConnectionState interface {
    isConnectionState()
}

type Disconnected struct{}
func (Disconnected) isConnectionState() {}

type Connecting struct {
    DeviceName string
}
func (Connecting) isConnectionState() {}

type Connected struct {
    DeviceName string
}
func (Connected) isConnectionState() {}

type ConnectionError struct {
    Reason  error
    Message string
}
func (ConnectionError) isConnectionState() {}

// Uso com type switch
switch s := state.(type) {
case Disconnected:
    scanButton()
case Connecting:
    progressIndicator()
case Connected:
    disconnectButton()
case ConnectionError:
    errorMessage()
}
```

**Observações:**
- Kotlin obriga exaustividade no `when` (o compilador reclama se faltar um branch)
- Go precisa do `default` se quiser segurança, mas não há verificação em compile-time
- A interface selada (com método privado) impede que outros pacotes implementem — simula sealed class
- Kotlin `data object` é singleton; Go `struct{}` também

---

## 10. UI com Compose vs Alternativas Go

### Kotlin (Jetpack Compose)

```kotlin
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val values by viewModel.sensorValues.collectAsState()

    Column {
        values.forEach { value ->
            SensorCard(value)
        }
    }
}

@Composable
fun SensorCard(value: PidValue) {
    Card {
        Text(text = value.label)
        Text(text = "${formatValue(value.value)} ${value.unit}")
    }
}
```

### Alternativas Go

Go não tem um framework UI nativo. Opções:

| Opção | Tipo | Prós | Contras |
|---|---|---|---|
| **Gio** (`gioui.org`) | Declarativa, multiplataforma | Mais próxima do Compose; GPU | API instável, comunidade pequena |
| **Fyne** (`fyne.io`) | Widget-based | Madura, documentação boa | Widgets limitados |
| **Web + HTMX** | Web | Go já serve bem HTTP; qualquer frontend | Precisa de navegador |
| **Wails** | Desktop nativo com frontend web | Go no backend, HTML/JS no frontend | Só desktop |

**Exemplo Gio (mais próximo do Compose):**

```go
package main

import (
    "gioui.org/app"
    "gioui.org/layout"
    "gioui.org/widget/material"
)

func main() {
    go func() {
        w := app.NewWindow()
        th := material.NewTheme()
        
        for {
            e := w.Event()
            switch e := e.(type) {
            case app.FrameEvent:
                gtx := app.NewContext(&ops, e)
                layout.Flex{Axis: layout.Vertical}.Layout(gtx,
                    material.H2(th, "Dashboard").Layout,
                    material.Body1(th, "RPM: 1726 rpm").Layout,
                    material.Body1(th, "Speed: 80 km/h").Layout,
                )
                e.Frame(gtx.Ops)
            }
        }
    }()
    app.Main()
}
```

---

## 11. Testes

### Kotlin (JUnit4 + MockK + Turbine)

```kotlin
class Elm327ProtocolTest {
    private val bluetoothManager: BluetoothConnectionManager = mockk()
    private val protocol = Elm327Protocol(bluetoothManager)

    @Test
    fun `initialize sends all AT commands in order`() = runTest {
        coEvery { bluetoothManager.sendCommand(any()) } returns BluetoothResult.Success("OK")

        val result = protocol.initialize()

        assertTrue(result is BluetoothResult.Success)
        coVerifySequence {
            bluetoothManager.sendCommand("ATZ")
            bluetoothManager.sendCommand("ATE0")
            bluetoothManager.sendCommand("ATL0")
            bluetoothManager.sendCommand("ATS0")
            bluetoothManager.sendCommand("ATH1")
            bluetoothManager.sendCommand("ATAT1")
            bluetoothManager.sendCommand("ATSP0")
        }
    }
}
```

### Go (testing + testify + mock)

```go
type MockBluetoothManager struct {
    mock.Mock
}

func (m *MockBluetoothManager) SendCommand(cmd string) (string, error) {
    args := m.Called(cmd)
    return args.String(0), args.Error(1)
}

func TestInitialize_SendsAllCommands(t *testing.T) {
    btMock := new(MockBluetoothManager)
    protocol := NewElm327Protocol(btMock)

    btMock.On("SendCommand", "ATZ").Return("OK", nil)
    btMock.On("SendCommand", "ATE0").Return("OK", nil)
    btMock.On("SendCommand", "ATL0").Return("OK", nil)
    btMock.On("SendCommand", "ATS0").Return("OK", nil)
    btMock.On("SendCommand", "ATH1").Return("OK", nil)
    btMock.On("SendCommand", "ATAT1").Return("OK", nil)
    btMock.On("SendCommand", "ATSP0").Return("OK", nil)

    err := protocol.Initialize()
    assert.NoError(t, err)
    btMock.AssertExpectations(t)
}
```

**Diferenças:**
- Kotlin: `coEvery` para suspend functions
- Go: `mock.On` para métodos normais (não precisa de tratamento especial para async)
- Kotlin: `runTest` para escopo de teste com corrotinas
- Go: `t *testing.T` padrão
- Turbine para testar Flow; em Go, testa channels com select
- MockK usa `relaxUnitFun = true` para métodos void; Go precisa implementar explicitamente

---

## 12. Bluetooth

### Android (Kotlin)

```kotlin
// Conexão Bluetooth Classic (SPP)
val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
val socket = device.createRfcommSocketToServiceRecord(uuid)
socket.connect()

// I/O
val output = socket.outputStream
val input = socket.inputStream
output.write("ATZ\r\n".toByteArray())
val reader = BufferedReader(InputStreamReader(input))
val response = reader.readLine()
```

### Go (Desktop/Linux)

```go
// Go não tem Bluetooth Classic via stdlib.
// Opções:

// 1. Serial sobre RFCOMM (Linux)
import "github.com/tarm/serial"

cfg := &serial.Config{
    Name: "/dev/rfcomm0",  // rfcomm bind do Linux
    Baud: 38400,
}
port, _ := serial.OpenPort(cfg)
port.Write([]byte("ATZ\r\n"))

// 2. tinygo/bluetooth (BLE apenas, não serve)
import "tinygo.org/x/bluetooth"

adapter := bluetooth.DefaultAdapter
adapter.Scan(func(adapter *bluetooth.Adapter, device bluetooth.ScanResult) {
    // Só BLE, não encontra ELM327 (Bluetooth Classic)
})

// 3. go-bluetooth (via BlueZ D-Bus)
import "github.com/godbus/dbus/v5"

conn, _ := dbus.SystemBus()
// BlueZ D-Bus API para RFCOMM — complexo e pouco documentado
```

**Conclusão sobre Bluetooth:** Não existe uma lib Go simples e madura para Bluetooth Classic. O caminho mais prático em Go seria usar um adaptador USB-ELM327 (serial) ao invés de Bluetooth, ou usar `rfcomm` via linha de comando.

---

## 13. Data Classes vs Structs

### Kotlin (Data Class)

```kotlin
data class ObdResponse(
    val mode: Int,
    val pid: Int,
    val data: List<Int>
)

// Ganha de graça:
// - equals() / hashCode()
// - toString() = "ObdResponse(mode=1, pid=12, data=[26, 248])"
// - copy() = obdResponse.copy(pid = 13)
// - component1(), component2(), component3() para destructuring
val (mode, pid, data) = obdResponse
```

### Go (Struct)

```go
type ObdResponse struct {
    Mode int
    PID  int
    Data []byte
}

// Nada é gerado automaticamente. Precisa implementar:
func (r ObdResponse) String() string {
    return fmt.Sprintf("ObdResponse{Mode: %d, PID: %d, Data: %x}", r.Mode, r.PID, r.Data)
}

// Não tem copy() — copiar struct é trivial (atribuição)
r2 := r1 // cópia por valor

// Não tem destructuring automático
mode, pid, data := r.Mode, r.PID, r.Data
```

### Comparable / Equals

**Kotlin:**
```kotlin
data class PidValue(val pid: Int, val value: Float, ...)
// equals() compara todas as propriedades automaticamente
```

**Go:**
```go
// Structs comparáveis se todos campos forem comparáveis
type PidValue struct {
    Pid   int
    Value float32  // float32 não é comparável com == (ponto flutuante)
    ...
}

// Precisa de reflect.DeepEqual ou implementação manual
func (a PidValue) Equals(b PidValue) bool {
    return a.Pid == b.Pid && math.Abs(float64(a.Value - b.Value)) < 0.01 && ...
}
```

---

## 14. OBD-II PID Handling

### Kotlin (PidValueConverter)

```kotlin
object PidValueConverter {
    fun convert(pid: Int, data: List<Int>): PidValue? {
        return when (pid) {
            0x0C -> {
                val rpm = ((data[0] * 256 + data[1]) / 4.0).toFloat()
                PidValue(pid, rpm, "rpm", "Engine RPM")
            }
            0x0D -> PidValue(pid, data[0].toFloat(), "km/h", "Speed")
            0x05 -> PidValue(pid, (data[0] - 40).toFloat(), "°C", "Coolant Temp")
            else -> null
        }
    }
}
```

### Go

```go
package converter

type PidValue struct {
    PID   int
    Value float64
    Unit  string
    Label string
}

func Convert(pid int, data []byte) *PidValue {
    if len(data) == 0 {
        return nil
    }
    switch pid {
    case 0x0C:
        if len(data) < 2 {
            return nil
        }
        rpm := float64(data[0]*256+data[1]) / 4.0
        return &PidValue{PID: pid, Value: rpm, Unit: "rpm", Label: "Engine RPM"}
    case 0x0D:
        return &PidValue{PID: pid, Value: float64(data[0]), Unit: "km/h", Label: "Speed"}
    case 0x05:
        return &PidValue{PID: pid, Value: float64(data[0] - 40), Unit: "°C", Label: "Coolant Temp"}
    default:
        return nil
    }
}
```

**Diferenças:**
- Kotlin `when` é expressão (retorna valor); Go `switch` precisa de `return` explícito
- Kotlin `List<Int>` vs Go `[]byte`
- Kotlin nullable `PidValue?` vs Go `*PidValue` (nil)
- Kotlin `else -> null` vs Go `default: return nil`

---

## 15. Fluxo de Conexão Completo

### Kotlin

```
User clica "Connect"
  → ConnectionViewModel.connect(device)
    → BluetoothConnectionManager.connect(device)
      → Cria RFCOMM socket
      → socket.connect()
      → state = Connected
    → Elm327Protocol.initialize()
      → ATZ (reset)
      → ATE0 (echo off)
      → ATL0 (linefeeds off)
      → ATS0 (spaces off)
      → ATH1 (headers on)
      → ATAT1 (adaptive timing auto)
      → ATSP0 (auto protocol)
    → Elm327Protocol.detectProtocol()
      → ATDPN → parse protocol number
      → (fallback) ATDP → parse description
    → ObdPidReader.requestSupportedPids()
      → 01 00 → parse 4-byte bitmask
      → Supported: [1, 3, 4, 5, 6, 7, 12, ...]
```

### Go (conceitual)

```
func main()
  → Connect("/dev/rfcomm0")
    → serial.OpenPort()
    → Initialize()
      → Write("ATZ\r\n")
      → Write("ATE0\r\n")
      → ...
    → DetectProtocol()
      → Write("ATDPN\r\n")
      → Parse response
    → RequestSupportedPIDs()
      → Write("01 00\r\n")
      → Parse 4-byte bitmask
  → StartDashboard(ctx)
    → go readSensorRoutine(ctx)
      → ticker every 1s
        → Write("01 0C\r\n") → RPM
        → Write("01 0D\r\n") → Speed
        → Write("01 05\r\n") → Coolant
```

---

## 16. Observações e Decisões

### Por que Kotlin e não Go para este projeto?

| Motivo | Detalhe |
|---|---|
| **Android nativo** | Kotlin é a linguagem oficial do Android |
| **Bluetooth Classic** | Android tem API madura; Go não tem lib confiável |
| **UI declarativa** | Compose é muito produtivo; Go não tem equivalente maduro |
| **Ecossistema** | Android tem tudo pronto (permissoes, lifecycle, viewmodel) |
| **Aprendizado** | Projeto é para aprender Kotlin/Android |

### O que Go faria melhor

| Aspecto | Go |
|---|---|
| **CLI/daemon** | Binário único, cross-compile fácil, deploy simples |
| **Concorrência** | Goroutines são mais leves que corrotinas (2KB vs 1KB+ stack) |
| **Serialização** | `encoding/json` nativo, rápido, sem reflection pesada |
| **Performance** | Compilado nativamente (sem JVM, sem garbage collector pesado) |
| **Cross-platform** | Um binário para Linux ARM (RPi), macOS, Windows |

### Se fosse fazer um OBD Reader em Go

```
Arquitetura sugerida:
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  ELM327      │────▶│  obd-daemon  │────▶│  WebSocket   │
│  (Serial)    │     │  (Go)        │     │  Server      │
└──────────────┘     └──────┬───────┘     └──────┬───────┘
                            │                    │
                            ▼                    ▼
                     ┌──────────────┐     ┌──────────────┐
                     │  SQLite      │     │  Frontend    │
                     │  (histórico) │     │  (HTML/JS)   │
                     └──────────────┘     └──────────────┘
```

### Links úteis para referência

- [tinygo.org/x/bluetooth](https://github.com/tinygo-org/bluetooth) — BLE para Go
- [gioui.org](https://gioui.org) — UI declarativa Go
- [github.com/tarm/serial](https://github.com/tarm/serial) — Serial para Go
- [go.bug.st/serial](https://go.bug.st/serial) — Serial multiplataforma
- [Google Wire](https://github.com/google/wire) — DI em compile-time para Go
- [testify](https://github.com/stretchr/testify) — Assertions + mocking
- [Go Channels vs Kotlin Flow](https://kotlinlang.org/docs/flow.html)
- [SAE J1979 PID definitions](https://en.wikipedia.org/wiki/OBD-II_PIDs)

---

> Este arquivo é um documento vivo. Conforme o projeto evolui, novas comparações serão adicionadas.
