<div align="center">

# ⚡ Vision OS
### Sovereign Intelligence Edition · live build v48 · formerly Jarvis-android

![Vision OS](assets/promo/banner.png)

**A sovereign Personal Intelligence Operating Layer — it lives with you and gets smarter every day**  
*یک لایه‌ی هوش شخصی حاکمیتی — که با تو زندگی می‌کند و هر روز باهوش‌تر می‌شود*

[![License: Source-Available (Proprietary)](https://img.shields.io/badge/License-Source--Available%20(Proprietary)-red.svg?style=for-the-badge)](LICENSE)
[![Status](https://img.shields.io/badge/status-live%20v48-magenta?style=for-the-badge)](ROADMAP.md)
[![Platform](https://img.shields.io/badge/platform-Android%20·%20Linux%20·%20PC-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![Local-First](https://img.shields.io/badge/AI-Local--First%20·%20Offline-9cf?style=for-the-badge)](ROADMAP.md)
[![Activation](https://img.shields.io/badge/Activation-via%20kiancdn%20bot-0088cc?style=for-the-badge&logo=telegram)](docs/ACTIVATION.md)
[![Contribute](https://img.shields.io/badge/Contributors-Welcome%20(CLA)-cyan?style=for-the-badge)](CONTRIBUTING.md)
[![Download APK](https://img.shields.io/badge/Download-VISION%20v48%20APK-00E5FF?style=for-the-badge&logo=android&logoColor=black)](https://github.com/kian-irani/Jarvis-android/releases/latest)

</div>

---

> ⚠️ **This is NOT open source.** The code is *source-available* for transparency and collaboration only. Commercial use, redistribution, and derivative works are prohibited — see [LICENSE](LICENSE).
>
> ⚠️ *این پروژه اوپن‌سورس نیست. کد فقط برای شفافیت و مشارکت قابل‌مشاهده است؛ استفاده‌ی تجاری/بازانتشار/اثر مشتق ممنوع.*

> 🤖 Designed and built with the assistance of **Claude AI (Anthropic)**.

---


## 📊 Status — what's actually built (updated 2026-06-17, build v48)

### 🧠 VISION BRAIN — cognitive multi-model router (✅ shipped, v13–v21)
| Area | Status | Where |
|---|---|---|
| Orchestrator + Capability Router + Model Registry | ✅ DONE | `app/.../router/orchestrator/`, `router/capability/`, `router/registry/` |
| Availability Graph (latency/error EWMA + circuit-breaker + Retry-After) | ✅ DONE | `app/.../router/health/` |
| Smart Substitution chain + Adaptive Cost Controller | ✅ DONE | `app/.../router/substitution/`, `router/cost/` |
| Token Pool (per-key health, secret-safe rotation) + Backend adapters | ✅ DONE | `app/.../data/ai/TokenPool.kt`, `router/backend/` |
| Decision telemetry in the HUD (which model / why) | ✅ DONE | `app/.../ui/screen/hud/` |
| Agent engine · Task planner · ToolCaller (function-calling) | ✅ DONE | `app/.../core/agent/`, `core/planner/` |
| On-device local model catalog + on-demand download (resume, SHA-pin) | ✅ DONE | `app/.../router/local/` |

### 📱 REAL LAUNCHER + Vision OS redesign (✅ shipped, v22–v48)
| Area | Status | Where |
|---|---|---|
| Launcher layout model + pure ops + JSON persistence | ✅ DONE | `app/.../data/launcher/` |
| Workspace grid + pages (`HorizontalPager`, page dots), orb as hero | ✅ DONE | `app/.../ui/screen/workspace/` |
| Drag & drop (move / make folder / add to folder / remove) | ✅ DONE | `WorkspaceScreen.kt` + `LauncherGeometry.kt` |
| Folders (create / open / rename / 2×2 preview / pull-out) | ✅ DONE | `WorkspaceScreen.kt` |
| Long-press icon menu (App info / Remove) + Edit-Home sheet | ✅ DONE | `WorkspaceScreen.kt` |
| Grid-density presets (safe reflow) + layout backup/restore/reset | ✅ DONE | `LauncherOps.kt`, `SettingsHubScreen.kt` |
| App drawer (QUERY_ALL_PACKAGES) + search + categories + A–Z index | ✅ DONE | `app/.../ui/screen/drawer/` |
| Vision OS visual redesign (orb, glass, azure→violet, font picker) | ✅ DONE | `app/.../ui/theme/`, `ui/screen/home/` |

### 🎙️ VOICE — code-switch + free neural Persian (✅ shipped, v34/v41)
| Area | Status | Where |
|---|---|---|
| Code-switch segmenter — Persian/English spoken each in its own voice | ✅ DONE | `app/.../voice/VoiceSegmenter.kt` |
| Best-installed-voice selection + per-language voice picker + TEST | ✅ DONE | `voice/VoiceController.kt`, `SettingsHubScreen.kt` |
| Free **Edge neural** Persian TTS (opt-in, fallback) | ⚙️ needs on-device confirm | `voice/EdgeTtsProtocol.kt`, `voice/EdgeTtsClient.kt` |

### 🧩 Distributed Brain + foundation (✅ shipped, M0–M2)
| Area | Status | Where |
|---|---|---|
| Brain-Lite server (10 REST endpoints, Ktor on :7799) + Room + election/failover | ✅ DONE | `app/.../brain/` |
| Discovery (`vision://join` + mDNS) · Setup Wizard · QR pairing (+ CameraX scanner) | ✅ DONE | `app/.../brain/discovery/`, `ui/screen/setup/` |
| Multi-token AI providers (encrypted slots) · Brain-Full (FastAPI, CI) · Temporal | ✅ DONE | `app/.../data/ai/`, `brain/`, `docker-compose.yml` |

> **Where is the Android code?** In [`/app`](app) (`com.kianirani.jarvis`). The `android/` folder is only a pointer/module map. Full task plan lives in [`plu/PLAN.md`](plu/PLAN.md); roadmap in [ROADMAP.md](ROADMAP.md).


## What is Vision?

**Vision is an AI-native Android launcher built on a sovereign Personal Intelligence layer** —
it's how you *live* on your device (home, app grid, folders, dock, drawer, widgets) **and** the
on-device brain that makes everything smart. It's not a chatbot and not a single agent: you talk
to Vision and it does the rest, but it's also the real launcher you use all day.

The **Vision Orb** is the hero of the home screen — the AI core is page one, your apps are a swipe away.
A sovereign layer that runs on the user's *own* hardware, unifies all devices, data, and AI capabilities
into one ecosystem, and gets smarter every day.

*ویژن یک **لانچرِ AI-nativeِ اندروید** است که روی یک لایه‌ی هوشِ شخصیِ حاکمیتی ساخته شده — هم لانچرِ واقعیِ
هرروزه‌ی شماست (هوم، گرید، فولدر، داک، drawer، ویجت) و هم مغزِ روی‌دستگاه که همه‌چیز را هوشمند می‌کند. گویِ ویژن
هیروِ صفحه‌ی اول است؛ هسته‌ی AI صفحه‌ی یک، و اپ‌ها یک swipe آن‌طرف‌تر.*

```
        ┌──────────────────────────────────────────────┐
        │                  V I S I O N                  │
        │      Personal Intelligence Operating Layer    │
        └──────────────────────────────────────────────┘
                 │            │            │
            ┌────┴───┐   ┌────┴────┐  ┌────┴────┐
            │ Phone  │   │  VPS/PC │  │  Mesh   │   ← every device is a potential Brain
            │ Brain  │   │  Brain  │  │  Nodes  │
            └────────┘   └─────────┘  └─────────┘
```

> **Core principle — Distributed Brain:** every device can run Vision on its own. When a new device joins
> the Mesh, its resources (CPU/RAM/GPU) are added to the Brain with user consent, making Vision faster and
> smarter. Any node can go offline without taking the system down.

> **In one line:** Vision = Iron Man HUD + Claude Code + Server Manager + Distributed Brain — on your own hardware, offline, with any model.

---

## 🌍 Competitive Edge

| Feature | Microsoft Solara | Google Gemini OS | **Vision v2** |
|---------|:---:|:---:|:---:|
| Hardware required | new Badge/Desk | dedicated Android XR | ✅ **any existing device** |
| Full offline | ❌ | ❌ | ✅ **Local-First** |
| AI providers | Azure only | Google only | ✅ **unlimited** |
| Monthly cost | Enterprise | $100 | ✅ **$3** |
| Privacy | Cloud | Gmail/Calendar access | ✅ **Zero-Knowledge Local** |
| Brain on every device | ❌ | ❌ | ✅ **Phone/VPS/PC** |
| Distributed compute | ❌ | ❌ | ✅ **Mesh CPU/GPU** |
| Every world language | limited | limited | ✅ **Universal** |

---

## 🔑 Activation

Vision is an **activation-based** product. The user receives an **activation token** from the official bot:

```
User → «kiancdn» Telegram bot → receive token → enter in app → activated
```

- 🤖 Activation bot: **[@kian_irani_cdn_f](https://t.me/kian_irani_cdn_f)** (kiancdn)
- 💳 Plan: **$3/month** or **$30/year** · 14-day full trial, no card
- 📄 Full flow, token security & architecture: **[docs/ACTIVATION.md](docs/ACTIVATION.md)**

> Tokens are personal, revocable and rate-limited. Sharing or bypassing activation violates the license.
> *توکن‌ها شخصی، قابل‌ابطال و دارای محدودیت نرخ‌اند.*

---

## ✨ Core Features (v2)

| Area | Capabilities |
|------|--------------|
| 📱 **AI-native Launcher** | Real Android launcher — `HorizontalPager` home (orb on page 1), workspace grid + pages, drag & drop, folders, long-press icon menu, edit-home sheet, grid-density presets, app drawer with search + A–Z index, layout backup/restore. **No mockups — everything persists in `LauncherStore`.** |
| 🧠 **VISION BRAIN (cognitive router)** | Orchestrator that *thinks*: intent → capability → ranked candidates, **Availability Graph** (circuit-breaker), **Smart Substitution** chain, **Adaptive Cost Controller**, secret-safe Token Pool, on-device decision telemetry |
| 🧠 **Distributed Brain** | Three tiers (Brain-Nano / Lite / Full), auto-election via **Brain Score**, Auto-Failover, full phone operation without a VPS |
| 🔀 **Multi-Token AI Router** | Unlimited providers (Claude · Gemini · Groq · OpenAI · Grok · OpenRouter · Ollama), < 100 ms switch, Fallback Chain, Cost Dashboard |
| 🤖 **Agent Engine** | Goal → Plan → Tool → Execute: `TaskPlanner` + `AgentEngine` + `ToolCaller` (real function-calling: call/SMS/open-app/settings/nav), "never claim done without executing" |
| 🗣️ **Local AI on the phone** | On-device model catalog + on-demand download (resumable, SHA-pinned), hybrid local-first routing in Economy/Privacy modes |
| ⚡ **VISN Protocol** | Ultra-fast file transfer between nodes (smart LZ4/zstd, chunked, resumable, ≥3× faster) |
| 🤖 **Agentic Core** | ReAct + LangGraph, Plan DAG, Self-Correction, Agent Pool (Browser/File/Code/Research…), natural-language **Vision Scheduler** |
| 🔒 **Trust Level System** | Read / Suggest / Auto / Critical — per-agent trust, tamper-evident Audit Trail |
| 🧪 **Vision Lab** | Dry-run any workflow with mock data before real execution + Chain Visualizer |
| 🔍 **AnySearch + Timeline + Notes** | Semantic search across all devices, local digital timeline, smart notes wired to Memory |
| 🎙️ **Voice & Persona** | **Code-switch TTS** (Persian + English each spoken in its own voice), per-language voice picker, free **Edge neural** Persian voice (opt-in), best-installed-voice selection, Persona sliders |
| 🌐 **Universal Language** | True support for **every language** (3-tier: full Persian → 9 full languages → universal fallback) |
| 🎨 **Cyberpunk HUD** | Arc Reactor, Glassmorphism (Haze 2.0), AGSL shaders, audio-reactive, < 5% CPU @ 60fps |
| 🛡️ **Zero-Trust Security** | Secret Vault (Keystore + biometrics), Behavioral Baseline, active **Privacy Threat Monitor** |
| 🤝 **Mesh & Handoff** | Cross-device Session Handoff, Universal Clipboard, Distributed Compute Mesh |

Full 20+ phases: **[ROADMAP.md](ROADMAP.md)** · architecture: **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**

---

## 🏗️ Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                          V I S I O N   O S                       │
├───────────────┬───────────────┬───────────────┬────────────────┤
│   HUD Layer   │   AI Core      │  Agent Engine │  Mesh Network  │
│  Cyberpunk    │  Multi-Token   │  ReAct+Trust  │  Distributed   │
│  Compose/AGSL │  Router        │  Vision Lab   │  Brain + VISN  │
├───────────────┴───────────────┴───────────────┴────────────────┤
│   Distributed Brain  ·  Brain-Nano / Lite / Full  ·  Score      │
├──────────────────────────────────────────────────────────────── ┤
│   Universal Language · AnySearch · Timeline · Notes · Memory    │
├──────────────────────────────────────────────────────────────── ┤
│   Activation & Licensing (kiancdn)  ·  Zero-Trust Security      │
└────────────────────────────────────────────────────────────────┘
```

**Tech Stack v16**
```
Android   : Kotlin 2.0 + Jetpack Compose + Haze 2.0 + AGSL · Clean Arch + MVI · Hilt
Brain     : Python 3.12 + FastAPI + LangGraph · Hexagonal (Ports & Adapters)
Workflow  : Temporal Workflow Engine  (replaces n8n)
Transfer  : VISN Protocol — LZ4 + zstd + XXH3, chunked/resumable
Data      : Room/SQLite (Lite) · PostgreSQL + Redis + ChromaDB (Full) · CRDT sync
Voice     : Vosk / faster-whisper (STT) + Coqui XTTS v2 / Piper (TTS)
Security  : Android Keystore + EncryptedDataStore · Behavioral Baseline
Quality   : Detekt/ktlint · Ruff/mypy · pytest · GitHub Actions CI/CD · OpenTelemetry
```

See the full target tree in **[ROADMAP.md](ROADMAP.md)** and module map in [`android/MODULES.md`](android/MODULES.md) · [`brain/MODULES.md`](brain/MODULES.md).

---

## 🗺️ Phase Map (v16)

| Band | Phases | Focus | Status |
|------|--------|-------|--------|
| 🔴 Foundation | PX · P0 · P1 · P1.5 · P2 | Code Standards, Foundation Fix, Flexible Brain, VISN, Router | ✅ done |
| 🟠 Product | P3 · P4 · P4.5 · P6 · P7 | Launcher, Licensing, Trust, Search/Notes, Voice/Language | ✅ mostly shipped |
| 🟣 VISION BRAIN | VB1–VB9 | Cognitive router (orchestrator/capability/health/substitution/cost/telemetry) | ✅ done (v13–v21) |
| 🟣 Real Launcher | LR1–LR12 · NEO1–14 | Grid+pages, drag&drop, folders, edit-mode, drawer, dock, widgets | 🚧 in progress (v33–v48) |
| 🟡 Network | P8 → P12 | Device Mesh, Offline, Memory/RAG, OS-Integration, MCP & Plugins | 🟠 partial |
| 🔴 Security | P13 | Zero-Trust + Behavioral Baseline + Privacy Monitor | 🟠 partial |
| 🟢 Horizon | P14 → P20 | Communication, Health, IoT, Marketplace, Full Vision OS | ⏳ planned |

**Beta milestone:** end of Phase 7 · **Active mission:** the **Real Launcher** rebuild (LR/NEO) — live task list in [`plu/PLAN.md`](plu/PLAN.md).

---

## 🚀 Getting Started

This repository is **source code**, not a public APK download. For real use:

1. Go to **[@kian_irani_cdn_f](https://t.me/kian_irani_cdn_f)** and get an activation token.
2. Get the official Vision build via the same bot/channel.
3. Open the app → enter the token → activate.

> **Developers:** to build from source and run the Brain Server, see [CONTRIBUTING.md](CONTRIBUTING.md) and [docs/SETUP.md](docs/SETUP.md). Building from source is permitted only for contribution under the [CLA](CLA.md).

---

## 🤝 Call for Contributors

We're looking for serious developers. Contributions are under the **[CLA](CLA.md)** (the code stays source-available, not open source).

> **Most-wanted right now:** the launcher rebuild ships fast but the build server has **no emulator**, so a lot of UI is marked *"needs on-device confirmation."* **On-device QA and Compose UI polish are the #1 need.**

| Area | Skills | Priority |
|------|--------|----------|
| 📱 Launcher UI + on-device QA | Kotlin + Jetpack Compose (test real builds on a device) | 🔴 urgent |
| 🧠 Local AI engine | Kotlin/NDK + llama.cpp / MediaPipe LLM (on-device inference) | 🔴 urgent |
| 🤖 Agentic core | Kotlin (Accessibility automation, tool-calling, scheduler) | 🔴 urgent |
| 🧠 Brain Core | Python + FastAPI + LangGraph | 🟠 high |
| 🎙️ Voice/Language | Android TTS/STT + Edge/Azure neural verification, Vosk/Whisper | 🟠 high |
| 🔒 Security | Android Security + Crypto | 🟠 high |
| ⚡ VISN Transfer / Mesh | Kotlin + Python (LZ4/zstd, mesh model exchange) | 🟢 medium |

> New contributors: start from [`plu/PLAN.md`](plu/PLAN.md) (the live, prioritized task list) — pick the nearest open task, build it green, and open a PR.

👉 Full guide: **[CONTRIBUTING.md](CONTRIBUTING.md)** · contact: [@Kian_irani_t](https://t.me/Kian_irani_t)

---

## 📚 Documentation

| File | Content |
|------|---------|
| [`plu/PLAN.md`](plu/PLAN.md) | **Live, prioritized task list** — the work driver (start here to contribute) |
| [`plu/reports/`](plu/reports/) | Dated shift reports of what shipped each session |
| [ROADMAP.md](ROADMAP.md) | Full roadmap — 20+ phases, milestones, ADRs |
| [CHANGELOG.md](CHANGELOG.md) | Release-by-release changes |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Technical architecture |
| [docs/ACTIVATION.md](docs/ACTIVATION.md) | Activation flow & kiancdn token service |
| [docs/SETUP.md](docs/SETUP.md) | Development setup |
| [docs/adr/](docs/adr/) | Architecture Decision Records (ADR-001…011) |
| [CONTRIBUTING.md](CONTRIBUTING.md) · [CLA.md](CLA.md) | Contribution |
| [SECURITY.md](SECURITY.md) | Vulnerability reporting |
| [LICENSE](LICENSE) | Source-available proprietary license |

---

## 📄 License

**Vision OS Source-Available License (VAOS-SAL) v1.0** — © 2026 Kian Irani.  
Source is viewable; commercial use / redistribution / derivatives prohibited. Full text: [LICENSE](LICENSE).

---

<div align="center">

**Vision** — not just an app. **The future of human–AI interaction, on your own hardware.**

*Made with ❤️ and Neon — by Kian Irani & contributors · Built with Claude AI*

</div>
