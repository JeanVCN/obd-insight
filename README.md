<picture>
  <source
    srcset="https://img.shields.io/badge/Android-34-brightgreen?logo=android&style=for-the-badge"
    media="(prefers-color-scheme: dark)"
  />
  <img src="https://img.shields.io/badge/Android-34-brightgreen?logo=android&style=for-the-badge" />
</picture>
<br/>
<picture>
  <source
    srcset="https://img.shields.io/badge/Kotlin-1.9.24-purple?logo=kotlin&style=for-the-badge"
    media="(prefers-color-scheme: dark)"
  />
  <img src="https://img.shields.io/badge/Kotlin-1.9.24-purple?logo=kotlin&style=for-the-badge" />
</picture>
<br/>
<picture>
  <source
    srcset="https://img.shields.io/badge/Jetpack_Compose-2024.06.00-4285F4?logo=jetpackcompose&style=for-the-badge"
    media="(prefers-color-scheme: dark)"
  />
  <img src="https://img.shields.io/badge/Jetpack_Compose-2024.06.00-4285F4?logo=jetpackcompose&style=for-the-badge" />
</picture>
<br/>
<picture>
  <source
    srcset="https://img.shields.io/badge/Material_3-FF6F00?logo=materialdesign&style=for-the-badge"
    media="(prefers-color-scheme: dark)"
  />
  <img src="https://img.shields.io/badge/Material_3-FF6F00?logo=materialdesign&style=for-the-badge" />
</picture>

---

<div align="center">
  <samp>
    <h1>🔧 OBD Insight</h1>
    <h3>Android OBD-II diagnostics via Bluetooth ELM327</h3>
    <p><i>Um laboratório portátil para entender a fundo a comunicação com veículos</i></p>
  </samp>
</div>

---

## 📋 Sobre

**OBD Insight** é um aplicativo Android que se conecta a adaptadores OBD-II compatíveis com **ELM327** via **Bluetooth Classic (RFCOMM/SPP)** para ler dados de diagnóstico do veículo.

Este é um **projeto de aprendizado** — construído do zero para entender profundamente cada camada da comunicação veicular:

- Protocolos OBD-II (ISO 9141, CAN, PWM, VPW)
- O chip ELM327 (ponte entre Bluetooth e barramento OBD)
- Bluetooth Classic (RFCOMM, SPP, descoberta e emparelhamento)
- Desenvolvimento Android moderno (Kotlin, Compose, Arquitetura Limpa)
- Futuramente: APIs em **Go** para processamento e análise de telemetria

### 🚗 Veículos de Teste

| Veículo | Função |
|---------|--------|
| **Mitsubishi Lancer GT 2014** | Desenvolvimento principal |
| **Chevrolet Astra GSI 2005** | Testes de compatibilidade futuros |

---

## ✨ Funcionalidades

### ✅ Fase 1 — Fundação (Concluída)

| Funcionalidade | Descrição |
|----------------|-----------|
| 📶 **Conexão Bluetooth** | Escaneamento, conexão e desconexão com adaptadores ELM327 |
| ⚙️ **Inicialização ELM327** | Sequência completa de 7 comandos AT (ATZ, ATE0, ATL0, ATS0, ATH1, ATSP0, AT@1) |
| 🎨 **Interface Compose** | Tela de conexão com status, dispositivo e ações |
| 🌓 **Tema Material 3** | Suporte a tema claro e escuro |
| 🧪 **Testes unitários** | 16 testes cobrindo Bluetooth, ELM327 e ViewModel |

### 🔄 Fase 2 — Comunicação OBD (Em andamento)

- Leitura de sensores (RPM, velocidade, temperatura do líquido de arrefecimento)
- Identificação do protocolo do veículo
- Parsing de respostas hex para valores físicos

### ✅ Fase 3 — Persistência (Concluída)

- Banco de dados Room para viagens e leituras de sensores
- Gravação de viagens (iniciar, pausar, retomar e finalizar)
- Histórico e estatísticas por viagem

### 🚀 Fase 4 — Dashboard & Análise

- Medidores em tempo real (RPM, velocidade, temperatura)
- Gráficos históricos
- Exportação de dados (CSV)
- API em **Go** para processamento avançado de telemetria

---

## 🧱 Arquitetura

```
com.obd.insight/
├── domain/model/      → Modelos sem dependência Android
├── data/bluetooth/    → Gerenciamento Bluetooth RFCOMM
├── data/elm327/       → Protocolo ELM327 (comandos, parsing)
├── ui/theme/          → Tema Material 3 (light/dark)
├── ui/connection/     → Tela de conexão + ViewModel
└── di/                → Injeção manual de dependências
```

### Stack Tecnológica

| Tecnologia | Versão |
|------------|--------|
| **Kotlin** | 1.9.24 |
| **Min / Target SDK** | 26 → 34 |
| **Jetpack Compose** | 2024.06.00 (BOM) |
| **Material Design 3** | via BOM |
| **Navigation Compose** | 2.7.7 |
| **Coroutines + StateFlow** | 1.8.1 |
| **Room** (futuro) | 2.6.1 |
| **Testes** | JUnit 4, MockK, Turbine |

### Decisões de Arquitetura

- **Clean Architecture simplificada** — 3 camadas (data/domain/ui) sem overengineering
- **Injeção manual de dependências** — sem Hilt/Dagger para manter a complexidade controlada
- **StateFlow** — gerenciamento de estado reativo e seguro para coroutines
- **BluetoothResult sealed class** — tratamento explícito de sucesso/erro
- **ConnectionState sealed interface** — máquina de estados finita para conexão

---

## 🔌 APIs em Go (Futuro)

Está nos planos construir um **backend em Go** para:

- **Processamento de telemetria** — análise de dados coletados em tempo real
- **Armazenamento centralizado** — consolidar leituras de múltiplas viagens
- **APIs REST** — consultar histórico, gerar relatórios e exportar dados
- **WebSocket** — streaming de dados ao vivo do veículo para dashboards web

A comunicação entre o app Android e o backend Go será feita via API REST, permitindo que o dispositivo móvel atue como coletor e o servidor Go como cérebro analítico.

---

## 📖 Documentação

A documentação completa do projeto está em `docs/`:

| Documento | Descrição |
|-----------|-----------|
| [VISION](docs/VISION.md) | Identidade, objetivos de aprendizado e filosofia |
| [SPECIFICATION](docs/SPECIFICATION.md) | Especificação técnica completa |
| [ROADMAP](docs/ROADMAP.md) | Roadmap em 4 fases |
| [DECISIONS](docs/DECISIONS.md) | 9 Architecture Decision Records (ADRs) |
| [DIARY](docs/DIARY.md) | Diário de desenvolvimento |
| [SESION STATE](docs/SESSION_STATE.md) | Estado detalhado das sessões |

### Documentação por Funcionalidade

- [Bluetooth Connection](docs/features/bluetooth-connection/README.md) — RFCOMM/SPP, implementação e testes
- [ELM327 Inicialização](docs/features/elm327-initialization/README.md) — Sequência de inicialização
- [Protocolos OBD2](docs/features/obd2-protocols/README.md) — CAN, ISO 9141, PWM, VPW
- [Leitura de PIDs](docs/features/pid-reading/README.md) — Modos OBD, PIDs comuns, fórmulas de conversão

---

## 🧪 Testes

```bash
# Rodar todos os testes
./gradlew test

# Testes específicos
./gradlew testDebugUnitTest
```

### Cobertura Atual (16 testes)

| Classe | Testes | O que cobre |
|--------|--------|-------------|
| `BluetoothConnectionManagerTest` | 4 | Estados de conexão, envio de comando, dispositivos pareados |
| `PermissionManagerTest` | 3 | Verificações de permissão em diferentes APIs |
| `Elm327ProtocolTest` | 6 | Inicialização, parsing, execução de comandos |
| `ConnectionViewModelTest` | 3 | Scan, connect, disconnect |

---

## 🛠️ Como Buildar

```bash
git clone https://github.com/seu-usuario/obd-insight.git
cd obd-insight
./gradlew assembleDebug
```

O APK será gerado em `app/build/outputs/apk/debug/`.

---

## 📚 Filosofia de Aprendizado

Cada comunicação OBD-II neste projeto é documentada em detalhe:

- Cada **comando AT** tem sua função explicada (ATZ, ATE0, ATL0, ATS0, ATH1, ATSP0...)
- Cada **modo OBD** é descrito (Modo 01: dados atuais, Modo 03: DTCs...)
- Cada **PID** tem sua fórmula de conversão documentada (byte → valor físico)
- As diferenças entre **protocolos automotivos** são explicadas

Nada é tratado como caixa-preta — o objetivo é aprender cada camada.

---

## 👤 Autor

**Jean** — Desenvolvido como projeto de aprendizado sobre diagnóstico veicular, Bluetooth e desenvolvimento Android moderno.

---

<div align="center">
  <samp>
    <sub>Built with ❤️ for learning | Kotlin • Jetpack Compose • ELM327 • OBD-II</sub>
    <br/>
    <sub>Em breve: Go • APIs REST • Telemetria • Dashboards</sub>
  </samp>
</div>
