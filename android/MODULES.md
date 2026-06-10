# Vision Android — Module Map (v16)

Clean Architecture + MVI. UI (Compose) → ViewModel (Intent→State→Effect) → UseCases → Domain → Data.
Base package: `com.jarvis.vision`.

| Module | Responsibility | Phase |
|--------|----------------|-------|
| `launcher/` | Home-screen launcher, app drawer, gestures | P3 |
| `brain/` | BrainLiteService + Ktor server (10 endpoints) | P1 |
| `agent/` | Agent engine + ReAct loop | P5 |
| `router/` | Multi-token AI router (client) | P2 |
| `search/` | AnySearch engine | P6 |
| `notes/` | Vision Notes — NEW v16 | P6 |
| `mcp/` | MCP client + host | P12 |
| `mesh/` | Device mesh + node registry | P8 |
| `transfer/` | VISN fast file transfer | P1.5 |
| `memory/` | Memory system + RAG | P9 |
| `voice/` | STT + TTS + wake word | P7 |
| `language/` | Universal Language Engine — NEW v16 | P7 |
| `persona/` | Personality + emotion engine | P7 |
| `security/` | Auth + encryption + vault | P13 |
| `privacy/` | Privacy Threat Monitor — NEW v16 | P13 |
| `ui/` | Compose screens + Cyberpunk HUD | P3 |
| `accessibility/` | Vision Accessibility Mode — NEW v16 | P7.5 |
| `health/` | Health Connect + biometrics | P15 |
| `compute/` | Distributed compute mesh | P17.5 |
| `capture/` | Vision Capture (visual + OCR) | P7.5 |
| `lab/` | Vision Lab sandbox | P5.5 |
| `timeline/` | Vision Timeline | P6 |
| `focus/` | Vision Focus Mode | P11.5 |
| `broadcast/` | Vision Broadcast | P11.5 |
| `utils/` | Shared utilities | — |
