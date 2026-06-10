# 🗺️ Vision OS — ROADMAP v16.0
## "Sovereign Intelligence Edition" · Built with Claude AI (Anthropic)

**آخرین به‌روزرسانی:** ژوئن ۲۰۲۶ · **وضعیت:** 🚀 Build-Ready · **مخزن:** https://github.com/KIAN-IRANI/Jarvis-android

> این پروژه با کمک Claude AI (Anthropic) طراحی و ساخته می‌شود.
> This project is designed and built with the assistance of Claude AI by Anthropic.

---

## 🎯 North Star — Vision چیست؟

Vision **یک Launcher نیست.**
Vision **یک Chatbot نیست.**
Vision **یک Agent منفرد نیست.**

**Vision یک Personal Intelligence Operating Layer است** — لایه‌ای حاکمیتی که روی سخت‌افزار خود کاربر اجرا می‌شود، تمام دستگاه‌ها، داده‌ها، و قابلیت‌های AI را در یک اکوسیستم واحد متحد می‌کند، و هر روز هوشمندتر می‌شود.

> کاربر نباید با ابزارها کار کند. کاربر فقط با Vision صحبت می‌کند، و Vision بقیه کار را انجام می‌دهد.

### اصل محوری: Distributed Brain — هر دستگاه یک مغز بالقوه

هر دستگاه کاربر می‌تواند به‌تنهایی Vision را اجرا کند. وقتی دستگاه جدیدی به Mesh اضافه می‌شود، منابع آن (CPU، RAM، GPU) با رضایت کاربر به Brain اضافه می‌شود و Vision سریع‌تر، باهوش‌تر، و قدرتمندتر می‌شود. این سیستم کاملاً ماژولار است — هر node می‌تواند آفلاین شود بدون اینکه سیستم از کار بیفتد.

---

## 📊 تحلیل رقابتی — Vision در برابر جهان

| ویژگی | Microsoft Solara | Google Gemini OS | **Vision v16.0** |
|--------|-----------------|------------------|------------------|
| سخت‌افزار مورد نیاز | Badge/Desk جدید | Android XR اختصاصی | ✅ **هر دستگاه موجود** |
| آفلاین کامل | ❌ Cloud-dependent | ❌ API-dependent | ✅ **Local-First کامل** |
| پشتیبانی از AI Providers | ❌ Azure only | ❌ Google only | ✅ **نامحدود** |
| هزینه ماهانه | Enterprise | ۱۰۰$ (AI Ultra) | ✅ **۳ دلار** |
| پشتیبانی زبانی | محدود | محدود | ✅ **همه زبان‌ها — Universal** |
| Persona سفارشی | ❌ | ❌ | ✅ **کنترل کامل** |
| MCP | محدود | جزئی | ✅ **Native (Client + Host)** |
| حریم خصوصی | Cloud-based | Gmail/Calendar access | ✅ **Zero-Knowledge Local** |
| Plugin ecosystem | ❌ | ❌ | ✅ **با revenue share** |
| Distributed compute | ❌ | ❌ | ✅ **Mesh CPU/GPU sharing** |
| انتقال فایل سریع | ❌ | ❌ | ✅ **VISN Protocol (LZ4/zstd)** |
| Brain روی هر دستگاه | ❌ | ❌ | ✅ **Phone/VPS/PC/هر چیزی** |
| Vision Lab (sandbox) | ❌ | ❌ | ✅ **محیط آزمایش agent** |
| Trust Level System | ❌ | ❌ | ✅ **Read/Suggest/Auto/Critical** |
| Offline Mode کامل | ❌ | ❌ | ✅ **بدون degradation** |
| Session Handoff | ❌ | ❌ | ✅ **ادامه context بین دستگاه‌ها** |
| Privacy Threat Monitor | ❌ | ❌ | ✅ **مانیتور فعال تهدیدات** |
| دسترس‌پذیری (Accessibility) | جزئی | جزئی | ✅ **Universal Design + AI** |
| یادداشت هوشمند | ❌ | ❌ | ✅ **Vision Notes + Memory** |

---

## 🔴 Blockerهای فوری — قبل از هر چیز رفع شوند

| # | مشکل | راه‌حل | زمان |
|---|------|---------|------|
| 1 | WebSocket `/ws/mobile` → 404 پشت Caddy | اصلاح Caddyfile با `handle /ws/*` + Upgrade headers | ۲-۳ ساعت |
| 2 | مخزن فقط مستندات دارد، کد وجود ندارد | Push skeleton: `android/ brain/ node-agent/ docs/` | ۱ روز |
| 3 | وابستگی به n8n — سنگین و ناپایدار | جایگزینی با Temporal Workflow Engine | ۱ هفته |

**Fix Caddy:**
```caddy
handle /ws/* {
    reverse_proxy localhost:8000 {
        header_up Upgrade {http.request.header.Upgrade}
        header_up Connection {http.request.header.Connection}
    }
}
```

---

## 🏗️ ساختار هدف مخزن

```
Jarvis-android/
├── android/
│   └── app/src/main/java/com/jarvis/vision/
│       ├── launcher/        # Home screen launcher
│       ├── brain/           # BrainLiteService + KtorServer
│       ├── agent/           # Agent engine + ReAct loop
│       ├── router/          # Multi-token AI router
│       ├── search/          # AnySearch engine
│       ├── notes/           # Vision Notes              ← NEW v16
│       ├── mcp/             # MCP client + host
│       ├── mesh/            # Device mesh + node registry
│       ├── transfer/        # Fast file transfer (VISN Protocol)
│       ├── memory/          # Memory system + RAG
│       ├── voice/           # STT + TTS + wake word
│       ├── language/        # Universal Language Engine  ← NEW v16
│       ├── persona/         # Personality + emotion engine
│       ├── security/        # Auth + encryption + vault
│       ├── privacy/         # Privacy Threat Monitor     ← NEW v16
│       ├── ui/              # Compose screens + HUD
│       ├── accessibility/   # Vision Accessibility Mode  ← NEW v16
│       ├── health/          # Health Connect + biometrics
│       ├── compute/         # Distributed compute mesh
│       ├── capture/         # Vision Capture (بصری + OCR)
│       ├── lab/             # Vision Lab sandbox
│       ├── timeline/        # Vision Timeline
│       ├── focus/           # Vision Focus Mode
│       ├── broadcast/       # Vision Broadcast
│       └── utils/
├── brain/                   # Brain-Full (Python/FastAPI)
│   ├── api/
│   ├── router/
│   ├── memory/
│   ├── agents/
│   ├── transfer/
│   ├── compute/
│   ├── language/            # Universal Language Engine  ← NEW v16
│   ├── security/            # Behavioral Baseline        ← NEW v16
│   ├── migrations/          # Alembic DB migrations
│   ├── tests/               # pytest test suite
│   └── docker-compose.yml
├── node-agent/              # Cross-platform node script (Python)
├── sdk/                     # Vision Plugin SDK
├── docs/
│   ├── architecture/
│   ├── api/                 # OpenAPI/Swagger auto-generated
│   ├── adr/                 # Architecture Decision Records
│   └── user-guide/
├── .github/
│   ├── workflows/           # CI/CD pipelines
│   └── PULL_REQUEST_TEMPLATE.md
├── ROADMAP.md
└── README.md
```

---

## 📅 جدول زمانی فازها — v16.0

| فاز | موضوع | زمان | اولویت |
|-----|-------|------|--------|
| **PX** | Dev Tooling & Code Standards | هفته ۱ (موازی با P0) | 🔴 بحرانی |
| **P0** | Foundation Fix | هفته ۱ | 🔴 بحرانی |
| **P1** | Flexible Brain Core | هفته ۲–۴ | 🔴 بحرانی |
| **P1.5** | Fast File Transfer (VISN Protocol) | هفته ۳–۴ (موازی) | 🔴 بحرانی |
| **P2** | Multi-Token AI Router | هفته ۵–۶ | 🔴 بحرانی |
| **P3** | Android Launcher MVP | هفته ۷–۹ | 🔴 بحرانی |
| **P4** | Activation & Licensing | هفته ۱۰–۱۱ | 🔴 بحرانی |
| **P4.5** | Vision Trust Level System | هفته ۱۱ (موازی با P4) | 🔴 بحرانی |
| **P5** | Agentic Reasoning Core | هفته ۱۲–۱۵ | 🟠 زیاد |
| **P5.5** | Vision Lab — محیط آزمایش | هفته ۱۴–۱۵ (موازی) | 🟠 زیاد |
| **P6** | AnySearch Engine + Vision Timeline + Vision Notes | هفته ۱۶–۱۷ | 🟠 زیاد |
| **P7** | Adaptive Voice & Persona + Universal Language | هفته ۱۸–۲۰ ← **نقطه عطف Beta** | 🟠 زیاد |
| **P7.5** | Vision Capture + Context Cards + Accessibility | هفته ۱۹–۲۰ (موازی) | 🟠 زیاد |
| **P8** | Device Mesh & Node Network | هفته ۲۱–۲۳ | 🟠 زیاد |
| **P8.5** | Offline Mode کامل | هفته ۲۲–۲۳ (موازی) | 🟠 زیاد |
| **P9** | Memory System & RAG | هفته ۲۴–۲۵ | 🟡 متوسط |
| **P9.5** | Vision Tutor + Automation Builder | هفته ۲۵ (موازی) | 🟡 متوسط |
| **P10** | Digital Twin & Self-Learning + Smart Power | هفته ۲۶–۲۸ | 🟡 متوسط |
| **P10.5** | Shared Brain (Team Mode واقعی) | هفته ۲۷–۲۸ (موازی) | 🟡 متوسط |
| **P11** | OS-Level Integration + Conversational OS | هفته ۲۹–۳۲ | 🟠 زیاد |
| **P11.5** | Vision Focus Mode + Broadcast | هفته ۳۱–۳۲ (موازی) | 🟡 متوسط |
| **P12** | MCP & Plugin Ecosystem + Widget API | هفته ۳۳–۳۵ | 🟡 متوسط |
| **P13** | Zero-Trust Security + Behavioral Baseline + Privacy Monitor | هفته ۳۶–۳۸ | 🔴 بحرانی |
| **P14** | Communication Layer | هفته ۳۹–۴۲ | 🟡 متوسط |
| **P15** | Health & Biometrics + Activity-Aware | هفته ۴۳–۴۵ | 🟢 پایین |
| **P16** | IoT & Smart Environment | هفته ۴۶–۴۹ | 🟢 پایین |
| **P17** | Developer Suite & Marketplace | هفته ۵۰–۵۳ | 🟡 متوسط |
| **P17.5** | Distributed Compute Mesh | هفته ۵۱–۵۴ | 🟡 متوسط |
| **P18** | Marketing & Launch | مداوم از P3 | 🟠 زیاد |
| **P19** | Cross-Platform Expansion | بلندمدت | 🟢 پایین |
| **P20** | Full Vision OS | بلندمدت | 🟢 آینده |

**نقطه عطف Beta:** پایان فاز ۷ (هفته ۲۰)

---

## 🗺️ جدول Milestones

| Milestone | تاریخ هدف | Deliverables |
|-----------|-----------|--------------|
| **M0** — Foundation Ready | روز ۷ | WebSocket fixed، Code pushed، n8n حذف، CI/CD فعال |
| **M1** — Brain-Lite MVP | هفته ۳ | Vision روی گوشی بدون VPS کار می‌کند |
| **M2** — Multi-Provider AI | هفته ۶ | کاربر API keys اضافه می‌کند، Router هوشمند کار می‌کند |
| **M3** — Agentic Alpha | هفته ۱۰ | Agentها وظایف ساده را انجام می‌دهند |
| **M4** — Beta Launch | هفته ۲۰ | نسخه عمومی برای ۱۰۰۰ کاربر اول |

---

## 🛠️ Phase X — Dev Tooling & Code Standards (هفته ۱، موازی با P0)

> **پایه‌ای‌ترین چیزی است که یک پروژه پیشرفته نیاز دارد. بدون این، با هر contributor جدید کد به chaos تبدیل می‌شود.**

### X.1 — معماری کد

```
Android — Clean Architecture + MVI:
  UI Layer (Compose) ↕ StateFlow/SharedFlow
  ViewModel (MVI: Intent → State → Effect)
  ↕ UseCases
  Domain Layer (pure Kotlin, no Android deps)
  ↕ Repository Interfaces
  Data Layer (Room, Ktor, DataStore)

Python Brain — Hexagonal (Ports & Adapters):
  API Layer (FastAPI routers)
  ↕ Application Layer (use cases/services)
  ↕ Port interfaces
  Domain Layer (pure Python, no framework dep)
  ↕ Adapter implementations
  Infrastructure (PostgreSQL, Redis, ChromaDB)
```

- [ ] **ADR-001** — Clean Architecture for Android
- [ ] **ADR-002** — Hexagonal for Brain-Full
- [ ] **ADR-003** — CRDT for sync
- [ ] **ADR-004** — LangGraph for ReAct
- [ ] **ADR-005** — LZ4+zstd for file transfer (VISN Protocol)
- [ ] **ADR-006** — Brain Score Algorithm for auto-election
- [ ] **ADR-007** — Trust Level System for agent permissions
- [ ] **ADR-008** — Offline Mode degradation policy
- [ ] **ADR-009** — Universal Language Support architecture  ← NEW v16
- [ ] **ADR-010** — Behavioral Baseline for security anomaly detection  ← NEW v16
- [ ] **ADR-011** — Privacy Threat Monitor policy  ← NEW v16

### X.2 — Android Code Standards

**ابزارها:**
- [ ] **Detekt** — static analysis با `detekt.yml`
- [ ] **ktlint** — code formatting یکپارچه
- [ ] **Hilt** — Dependency Injection (نه Koin)
- [ ] **Kotlin Coroutines + Flow** — همه async ops
- [ ] **Kotlin Serialization** — (نه Gson)
- [ ] **JUnit 5 + Turbine** — unit testing با Flow testing
- [ ] **Mockk** — mocking

**نسخه‌های target:**
```
compileSdk = 35
minSdk = 26  (Android 8.0 — ۹۴٪ بازار)
targetSdk = 35
Kotlin = 2.0.x
Compose BOM = 2025.06.x
```

### X.3 — Python Brain Code Standards

**ابزارها:**
- [ ] **Ruff** — linting + formatting
- [ ] **mypy** — strict type checking
- [ ] **pytest + pytest-asyncio** — testing
- [ ] **pytest-cov** — coverage: حداقل ۷۰٪ برای core modules
- [ ] **Pydantic v2** — validation و serialization
- [ ] **SQLAlchemy 2.x async** — ORM
- [ ] **Alembic** — database migrations
- [ ] **structlog** — structured JSON logging

**نسخه‌های target:**
```
Python = 3.12+
FastAPI = 0.115+
Pydantic = 2.x
SQLAlchemy = 2.x
LangGraph = 0.2.x
```

### X.4 — CI/CD Pipeline

- [ ] **GitHub Actions** — Android + Brain workflows روی هر PR
- [ ] **Renovate Bot** — آپدیت خودکار dependency‌ها هر هفته
- [ ] **Dependabot** — security alerts
- [ ] **CodeRabbit / Claude API** — AI code review روی PR
- [ ] **Branch Protection** — `main` فقط از طریق PR، حداقل ۱ approval
- [ ] **pre-commit hook** — detect-secrets: جلوگیری از commit کردن secret

### X.5 — Observability Stack

```
Brain:    OpenTelemetry SDK → Grafana Cloud
Android:  Firebase Crashlytics + custom events
Dev:      Prometheus + Grafana در docker-compose-dev.yml
```

- [ ] **OpenTelemetry** — traces + metrics + logs در Brain
- [ ] **Structured Logging** — JSON output با `device_id`, `session_id`, `trace_id`
- [ ] **Health Endpoints** — `/health/live` و `/health/ready`
- [ ] **Alerting** — Grafana alerts → Telegram notification

---

## 🔨 Phase 0 — Foundation Fix (هفته ۱)

**هدف:** رفع blockerها، تنظیم زیرساخت، ایجاد ساختار.

- [ ] **Fix WebSocket Caddy config** (۲-۳ ساعت)
- [ ] **Push skeleton به GitHub** — ساختار کامل دایرکتوری‌ها
- [ ] **Remove n8n** → جایگزینی با Temporal Workflow Engine
- [ ] **GitHub Actions CI** — android + brain workflows
- [ ] **Error Reporting Foundation** ← از v12 — سیستم متمرکز crash + anomaly logging
- [ ] **Structured logging** — JSON با device_id + session_id
- [ ] **Update README.md** — معرفی v16.0
- [ ] **Update LICENSE** — تصمیم Source-Available vs MIT

**معیار موفقیت:** WebSocket متصل می‌شود. Skeleton روی GitHub است. CI پاس می‌شود. n8n حذف شده. Crash reporting فعال است.

---

## 🧠 Phase 1 — Flexible Brain Core (هفته ۲–۴)

**هدف:** Vision باید مستقل روی گوشی بدون VPS کار کند. هر دستگاه باید بتواند به‌تنهایی Brain باشد.

### حالت‌های کار Brain

```
Offline  → Brain-Lite روی دستگاه (قابلیت کامل، مدل‌های محلی)
Hybrid   → Brain-Lite + sync با Brain-Full remote
Online   → Brain-Full روی VPS/PC (حداکثر قابلیت)
```

### Brain Tiers

| Tier | دستگاه | DB | Cache | LLM | حداقل RAM |
|------|---------|-----|-------|-----|-----------|
| **Brain-Nano** | Android ضعیف | SQLite | SharedPrefs | API-only | ۲GB |
| **Brain-Lite** | Android معمولی | Room/SQLite | In-Memory Map | API + Ollama اختیاری | ۳GB |
| **Brain-Full** | Linux/VPS/PC/Mac | PostgreSQL + Redis | Redis | Ollama + API | ۴GB |

### Brain Score Algorithm (انتخاب خودکار بهترین Brain)

```
Score = (RAM_free_GB × 20)
      + (CPU_cores × 10)
      + (is_VPS ? 50 : 0)
      + (battery_percent × 0.3)
      + (network_mbps × 0.1)
      - (is_on_battery ? 20 : 0)
      - (thermal_throttling ? 30 : 0)

بالاترین score = Brain اصلی
کاربر می‌تواند دستی override کند
Auto-failover: Brain اصلی قطع شد → بعدی با بالاترین score
```

### B.1 — BrainLite for Android

- [ ] **BrainLiteService.kt** — ForegroundService، پورت ۷۷۹۹، START_STICKY
- [ ] **Ktor HTTP Server** — ۱۰ endpoint اصلی:
  - `GET /health` — گزارش سلامت و قابلیت‌ها
  - `POST /chat` — endpoint اصلی تعامل AI
  - `POST /embed` — embedding محلی برای جستجو
  - `GET/POST /memory` — حافظه episodic و semantic
  - `GET/POST /nodes` — registry node‌های Mesh
  - `POST /task` — ثبت task‌های async
  - `GET/POST /files` — لایه دسترسی به فایل
  - `POST /search` — query AnySearch
  - `GET /status` — وضعیت token pool
  - `POST /events` — event stream برای UI
- [ ] **Room Database** — schema سازگار با Brain-Full PostgreSQL
- [ ] **In-Memory Cache** — ConcurrentHashMap با TTL قابل تنظیم
- [ ] **WakeLock Manager** — فقط هنگام request فعال، بعد release
- [ ] **Doze Handler** — JobScheduler برای sync در حالت Doze
- [ ] **Storage Budget Manager** — حداکثر ۲GB برای footprint Brain-Lite
- [ ] **Brain Score Calculator** — محاسبه real-time برای Election

### B.2 — نصب Brain-Full

```bash
# نصب یک‌دستوری روی Linux/VPS
curl -fsSL https://get.vision-os.app/brain.sh | bash

# Windows PowerShell
iwr https://get.vision-os.app/brain.ps1 | iex
```

- [ ] **brain-install.sh** — تشخیص OS + نصب dependencies + راه‌اندازی سرویس
- [ ] **brain-compose.yml** — Docker Compose جداگانه برای Brain-Full
- [ ] **brain-config.yml** — فایل تنظیمات یکپارچه

### B.3 — Brain Discovery

- [ ] **Manual IP** — کاربر آدرس را وارد می‌کند
- [ ] **QR Code pairing** — Brain QR تولید می‌کند، گوشی اسکن می‌کند
- [ ] **mDNS discovery** — NsdManager برای LAN (`_jarvis._tcp.local`)
- [ ] **Brain Token** — کد ۶ رقمی + Rendezvous Server
- [ ] **Brain Election UI** — نمایش همه Brain‌های ممکن با score
- [ ] **Auto-Failover** — تشخیص offline Brain و سوئیچ خودکار

### B.4 — CRDT Sync

- [ ] **Conflict-free replication** — automerge-py برای sync تنظیمات
- [ ] **Sync queue** — SQLite buffer + ارسال هنگام reconnect
- [ ] **Conflict resolution UI** — نمایش تعارض با انتخاب کاربر
- [ ] **Zero-Knowledge Sync** — رمزنگاری قبل از ترک دستگاه

### B.5 — Setup Wizard

- [ ] **۴-مرحله onboarding:**
  1. انتخاب نوع Brain (این گوشی / VPS / PC / اسکن QR)
  2. API keys
  3. تست اتصال
  4. تنظیم Persona
- [ ] **Brain Scanner** — اسکن mDNS + نمایش Brain‌های موجود در LAN
- [ ] **Migration Tool** — import از نسخه قبلی
- [ ] **Offline Model Download** — دانلود پیش‌فرض مدل‌های offline در Setup

### B.6 — Session Handoff

**هدف:** کاربر کار را روی گوشی شروع می‌کند، روی PC ادامه می‌دهد — بدون از دست دادن یک کلمه context.

```
گوشی: «Vision، این فایل رو تحلیل کن...» [نیمه‌کاره]
PC:   «Vision، ادامه بده» → Vision از همان نقطه ادامه می‌دهد
```

- [ ] **Session State Serializer** — ذخیره کامل context + چرخه ReAct + فایل‌های باز
- [ ] **Cross-Device Session Registry** — همه session‌های فعال در همه Nodes
- [ ] **Handoff Trigger** — «ادامه بده»، «از کجا بودیم»، یا باز کردن اپ روی دستگاه دیگر
- [ ] **Seamless Context Transfer** — انتقال E2E encrypted با VISN protocol
- [ ] **Session History** — تاریخچه همه session‌ها با قابلیت بازگشت

**معیار موفقیت:** Vision بدون VPS روی گوشی کار می‌کند. کاربر می‌تواند مکالمه را از هر دستگاهی ادامه دهد.

---

## ⚡ Phase 1.5 — Fast File Transfer / VISN Protocol (هفته ۳–۴، موازی با P1)

**هدف:** انتقال فایل بین nodeها سریع، خودکار، و شفاف باشد.

### الگوریتم انتخاب هوشمند

```
هر فایل قبل از ارسال:
  ↓
[Magic Bytes → Content-Type Detection]
  ↓
از قبل فشرده است؟ (jpg, png, mp4, zip, apk...)
  ├─ بله → Raw Transfer
  └─ خیر ↓
     حجم < 64KB؟
     ├─ بله → LZ4 (~500 MB/s | ratio ~2x)
     └─ خیر ↓
        متن / JSON / لاگ؟
        ├─ بله → zstd level 3 (~4x ratio)
        └─ خیر → zstd level 1 (سریع‌تر)

Chunked Pipeline:
  - تقسیم به chunk‌های 256KB
  - ارسال موازی تا ۴ chunk
  - time-to-first-byte < 200ms همیشه
  - Resume از chunk ناقص
  - XXH3 checksum برای integrity
```

### پروتکل VISN v1

```
Header (16 bytes):
  [4B] Magic: 0x56 0x49 0x53 0x4E  ("VISN")
  [1B] Algorithm: 0x01=LZ4, 0x02=zstd, 0x00=raw
  [1B] Flags: bit0=chunked, bit1=resume, bit2=encrypted
  [4B] Original size
  [4B] Compressed size
  [2B] Chunk index
```

**Brain/Node (Python):**
- [ ] **CompressionMiddleware** — FastAPI middleware، فشرده‌سازی خودکار
- [ ] **ContentTypeDetector** — از magic bytes، بدون نیاز به extension
- [ ] **AlgorithmRouter** — انتخاب LZ4 یا zstd
- [ ] **ChunkedSender** — 256KB chunks + pipeline
- [ ] **ResumeHandler** — ادامه transfer ناقص

**Android (Kotlin):**
- [ ] **FileTransferManager** — upload/download با progress callback
- [ ] **AutoDecompressor** — extraction از VISN header
- [ ] **ParallelChunkDownloader** — تا ۴ coroutine موازی
- [ ] **TransferProgressUI** — نوار پیشرفت با MB/s real-time
- [ ] **BackgroundTransfer** — WorkManager برای ادامه هنگام minimize

```
هدف benchmark:
  فایل 10MB متن روی WiFi: < 2 ثانیه end-to-end
  فایل 100MB باینری روی WiFi: < 15 ثانیه
  time-to-first-byte: < 200ms همیشه
```

**معیار موفقیت:** انتقال فایل ۱۰۰MB حداقل ۳ برابر سریع‌تر از قبل.

---

## 🔀 Phase 2 — Multi-Token AI Router (هفته ۵–۶)

**هدف:** پشتیبانی از providers و token‌های نامحدود با routing هوشمند.

### قوانین Multi-Token (غیرقابل مذاکره)

1. کاربر می‌تواند هر تعداد API key از هر provider اضافه کند
2. هیچ token تنهایی drain نمی‌شود وقتی جایگزین وجود دارد
3. سوئیچ خودکار در کمتر از ۱۰۰ms
4. تخمین هزینه قبل از task‌های سنگین به کاربر نمایش داده می‌شود
5. token‌های سنگین به token با بالاترین capacity routing می‌شوند
6. سلامت token به‌صورت real-time نمایش داده می‌شود

### R.1 — Token Pool Manager

- [ ] **Centralized key store** — همه API keys در یک جا با Keystore encryption
- [ ] **Rate limit tracker** — sliding window با TTL per token
- [ ] **Usage monitor** — quota باقی‌مانده real-time
- [ ] **Health score** — composite: latency + quota + error rate
- [ ] **Cost estimator** — پیش‌بینی هزینه قبل از اجرا
- [ ] **A/B Testing Framework** — مقایسه دو مدل روی task مشابه برای بهبود routing
- [ ] **Canary Routing** — ۵٪ requests به مدل جدید برای A/B test
- [ ] **Model Fallback Chain** — زنجیره fallback قابل تنظیم کاربر («اگر Claude ناموجود شد، به Gemini برو، اگر Gemini هم نبود به Groq برو»)

### R.2 — Smart Model Selector

```
if task.needs_large_context  → prefer Claude or Gemini
if task.is_coding            → prefer specialized code model
if task.is_realtime          → prefer lowest latency (Groq)
if task.is_image_gen         → route to image provider
if token.usage > 80%         → switch to healthiest alternative
if all_online_tokens.fail    → fallback to local Ollama
if local_model.latency > 5s  → fallback to online provider
if token.cost_estimate > budget → warn user before executing
```

- [ ] **Model Capability Matrix** — context window / vision / tools / speed / cost
- [ ] **Capability-task matching** — match task → model
- [ ] **Batch queue** — صف non-urgent requests
- [ ] **Prompt caching** — SHA256 hash + Redis TTL=1h

### R.3 — Providers پشتیبانی‌شده (روز اول)

- Claude (Anthropic) — استدلال پیچیده
- Gemini (Google) — context بلند
- Groq — سرعت
- OpenAI — GPT-4o
- Grok (xAI)
- OpenRouter — meta-router
- Ollama — مدل‌های محلی (phi-3-mini, gemma-2b, llama-3)
- Custom OpenAI-compatible endpoints

### R.4 — Cost & Usage Dashboard

- [ ] **Per-session cost** — هر مکالمه چقدر خرج داشت
- [ ] **Per-task breakdown** — هر agent task چقدر
- [ ] **Monthly budget ceiling** — با alert هنگام ۸۰٪
- [ ] **Provider comparison** — مقایسه هزینه providers

**معیار موفقیت:** کاربر ۵ key از ۳ provider اضافه می‌کند. Vision بین آنها routing می‌کند. هیچ token به صفر نمی‌رسد.

---

## 📱 Phase 3 — Android Launcher MVP (هفته ۷–۹)

**هدف:** اولین تجربه قابل استفاده برای کاربر غیرفنی. نصب Vision = داشتن یک CyberDeck.

### L.1 — Launcher Core

- [ ] **Home screen** — تنظیم به عنوان launcher پیش‌فرض با `category.HOME`
- [ ] **App drawer** — جستجو، دسته‌بندی، pin favorites
- [ ] **Quick actions panel** — ۶ fast action قابل تنظیم
- [ ] **Gesture navigation** — رفتارهای swipe/tap/long-press سفارشی
- [ ] **Widget system** — آب‌وهوا، تقویم، وضعیت Brain، سلامت Mesh
- [ ] **Universal Clipboard** — clipboard روی همه دستگاه‌های Mesh sync می‌شود (E2E encrypted)
- [ ] **Brain Selector Panel** — تغییر Brain با یک swipe
- [ ] **Contextual Shortcuts** — میانبرهای هوشمند بر اساس پیش‌بینی اقدام بعدی

### L.2 — CyberDeck HUD

```
HUD Components (هدف عملکرد: < 5% CPU overhead، 60fps):
├── 🔵 Arc Reactor Widget    (CPU/RAM/Network/Brain — Canvas.drawArc)
├── 🌫️ Particle Haze         (SurfaceView + thread جداگانه، max 200 particles)
├── ⚡ Glitch Shader          (RenderEffect API 31+ / fallback)
├── 🎵 Voice Waveform        (AudioRecord → FFT → Canvas.drawLines)
├── 📊 Brain Status Widget   (نام Brain + Tier + health score)
├── 🔲 Corner Brackets       (Canvas.drawPath — المان‌های cyberpunk)
└── 🎨 Scanline Effect       (bitmap shader)
```

- [ ] **Arc Reactor Widget** — reactor display با ValueAnimator
- [ ] **Particle Haze** — max 200 particle برای جلوگیری از battery drain
- [ ] **Glitch Transitions** — RenderEffect روی API 31+
- [ ] **Voice Waveform** — real-time FFT visualization
- [ ] **HUD Notifications** — Iron Man-style overlay cards
- [ ] **Dark Glass Theme** — شیشه تاریک با blur effect
- [ ] **Color Scheme Switcher** — Arc Blue / Danger Red / Stealth Green

### L.3 — OS Presence

- [ ] **Always-On Display Widget** — اطلاعات روی صفحه خاموش
- [ ] **Floating HUD Overlay** — Vision روی همه اپ‌ها (Accessibility Service، opt-in)
- [ ] **Quick Command Bar** — رابط slash-command برای اقدامات سریع
- [ ] **Lock Screen Integration** — حضور Vision روی lock screen
- [ ] **Boot Screen Branding** — نمایش Vision هنگام بوت

**معیار موفقیت:** کاربر Vision را به عنوان launcher پیش‌فرض تنظیم می‌کند. HUD زیبا و روان است.

---

## 🔐 Phase 4 — Activation & Licensing (هفته ۱۰–۱۱)

**هدف:** سیستم activation ساده، امن، و قابل گسترش.

### مدل فعلی (Telegram-based)

- [ ] **Telegram bot** (@kiancdn_bot) — ارسال کد activation
- [ ] **Device fingerprint** — HMAC از hardware identifiers
- [ ] **۱۴ روز trial** — دسترسی کامل، بدون credit card
- [ ] **License validation** — آنلاین + fallback JWT آفلاین
- [ ] **Grace period** — نمایش واضح روزهای باقی‌مانده
- [ ] **Offline activation** — token-based برای محیط‌های بدون اینترنت

### مدل آینده (Website-based)

- [ ] **kianirani.tr integration** — کدهای activation از وب‌سایت
- [ ] **Payment: ۳$/month یا ۳۰$/year**
- [ ] **License tiers:**
  - Personal — یک کاربر، تا ۵ دستگاه
  - Developer — دسترسی کامل API + انتشار plugin
  - Team — Brain مشترک، چند کاربر
  - Enterprise — Brain اختصاصی، SLA، پشتیبانی اولویت‌دار
- [ ] **Referral system** — معرفی دوست → trial بیشتر

**معیار موفقیت:** کاربر کد را دریافت، وارد، و Vision را فعال می‌کند. فرآیند زیر ۲ دقیقه.

---

## 🔒 Phase 4.5 — Vision Trust Level System (هفته ۱۱، موازی با P4)

> **اولویت: 🔴 بحرانی** — با گسترش قابلیت‌های Agent، بدون این سیستم کاربر یا همیشه تأیید می‌کند (خسته‌کننده) یا همیشه آزاد می‌گذارد (خطرناک).

### سطوح اعتماد

| سطح | عملکرد | مثال |
|-----|---------|------|
| **Read** | Vision فقط می‌خواند، هیچ تغییری نمی‌دهد | جستجوی فایل‌ها، خواندن تقویم |
| **Suggest** | Vision پیشنهاد می‌دهد، کاربر تأیید می‌کند | ارسال پیام، حذف فایل |
| **Auto** | Vision خودکار انجام می‌دهد + لاگ کامل | organize کردن Downloads، ارسال خبرنامه |
| **Critical** | همیشه تأیید دستی، بدون استثنا | تماس تلفنی، خرید، حذف دائمی |

### پیاده‌سازی

- [ ] **Trust Level Manager** — تنظیم سطح per-agent و per-skill
- [ ] **Action Classifier** — LLM: این action در کدام سطح قرار می‌گیرد؟
- [ ] **Conflict Detector** — تشخیص تضاد بین دو action قبل از اجرا
- [ ] **Trust UI** — داشبورد بصری: هر agent چه سطح اعتمادی دارد
- [ ] **Audit Trail** — لاگ tamper-evident همه اقدامات Auto
- [ ] **Trust Escalation** — برای task جدید با سطح بالاتر: از کاربر بپرس یک‌بار
- [ ] **Default Trust Profiles:**
  - File Agent: Auto
  - Communication Agent: Suggest
  - Browser Agent: Suggest
  - Code Agent: Auto
  - Payment Agent: Critical (همیشه)
- [ ] **Per-App Trust Override** — کاربر می‌تواند برای هر اپ تنظیم جداگانه داشته باشد

**معیار موفقیت:** کاربر می‌تواند به File Agent اعتماد Auto بدهد اما Communication Agent را روی Suggest نگه دارد.

---

## 🤖 Phase 5 — Agentic Reasoning Core (هفته ۱۲–۱۵)

**هدف:** Vision باید فکر کند، برنامه‌ریزی کند، و مسائل را حل کند — نه فقط دستورات را اجرا کند.

### معماری ReAct Loop

```
Observe → Reason → Plan → Act → Check → Observe → ...
    ↑___________________________________|
هدف: هر چرخه decision < 2 ثانیه
```

### A.1 — Reasoning Engine

- [ ] **LangGraph state machine** — مدیریت حالت‌های پیچیده agent
- [ ] **Plan Generator** — تولید برنامه چند مرحله‌ای با JSON DAG (موازی + ترتیبی)
- [ ] **Tool Selection Scorer** — LLM + rule-based hybrid برای انتخاب ابزار
- [ ] **Confidence Scoring** — < 70% → از کاربر تأیید بگیر
- [ ] **Self-Correction** — شکست → تحلیل دلیل → رویکرد جدید
- [ ] **Goal Decomposition** — هدف بزرگ → subgoals
- [ ] **Human-in-the-Loop** — تأیید اجباری برای اقدامات پرریسک
- [ ] **Reasoning Trace Log** — ذخیره chain-of-thought برای debugging و یادگیری

### A.2 — Agent Pool

- [ ] **Browser Agent** — کنترل WebView، form fill، scraping
- [ ] **File Agent** — جستجو، سازماندهی، تبدیل، انتقال فایل در Mesh
- [ ] **Data Analyst Agent** — تحلیل CSV/Excel/JSON با polars
- [ ] **API Agent** — اتصال REST/GraphQL از طریق MCP
- [ ] **Notification Manager Agent** — batching هوشمند و digest اعلان‌ها
- [ ] **Code Agent** — نوشتن، اجرا، و debug کد
- [ ] **Research Agent** — جستجوی وب + خلاصه‌سازی
- [ ] **Image Generation Agent** — تولید تصویر از زبان طبیعی
- [ ] **Video Processing Agent** — ویرایش و پردازش ویدیو
- [ ] **Smart Notification Digest** — گروه‌بندی و خلاصه‌سازی اعلان‌ها با AI
- [ ] **Battery Optimizer Agent** — پروفایل‌های باتری مدیریت‌شده با AI

### A.3 — Agent Collaboration

- [ ] **Agent Communication Protocol** — JSON schema استاندارد
- [ ] **Task Handoff** — انتقال task بین Agentها با context کامل
- [ ] **Checkpointing** — ذخیره و resume task‌های طولانی
- [ ] **Activity Audit Log** — لاگ tamper-evident از همه actions
- [ ] **Agent Leaderboard** — ردیابی success/failure per agent per task type
- [ ] **Distributed Agent Execution** — Agent روی قوی‌ترین Node در Mesh اجرا می‌شود

### A.4 — Vision Scheduler

**هدف:** Vision می‌تواند task‌ها را زمانبندی کند — نه فقط «الان انجام بده» بلکه «فردا صبح» یا «هر دوشنبه».

- [ ] **Natural Language Scheduling** — «هر روز صبح ۸، خلاصه اخبار برایم بفرست»
- [ ] **Cron-based Scheduler** — Temporal Workflow Engine با schedule
- [ ] **Condition-based Trigger** — «وقتی به WiFi خانه متصل شدم، backup بگیر»
- [ ] **Event-based Trigger** — «وقتی باتری زیر ۲۰٪ رفت، حالت صرفه‌جویی را فعال کن»
- [ ] **Recurring Task Dashboard** — نمایش همه task‌های زمانبندی‌شده
- [ ] **Smart Timing** — Vision بهترین زمان اجرا را بر اساس عادات کاربر پیشنهاد می‌دهد

**معیار موفقیت:** Vision می‌تواند «یادداشت‌های جلسه امروز را خلاصه کن و به Telegram بفرست» را بدون دخالت کاربر انجام دهد.

---

## 🧪 Phase 5.5 — Vision Lab — محیط آزمایش (هفته ۱۴–۱۵، موازی)

> **اولویت: 🟠 زیاد** — وقتی کاربر یک chain طولانی می‌سازد، باید بداند نتیجه چه خواهد بود قبل از اینکه داده‌های واقعی را تغییر دهد.

```
نمونه:
  کاربر یک chain می‌سازد: «فایل‌ها را بخوان → خلاصه کن → به Telegram بفرست»
  Vision Lab: یک run شبیه‌سازی می‌کند با داده‌های fake
  خروجی نمایش داده می‌شود: «این chain این نتیجه را تولید می‌کرد»
  کاربر تأیید می‌کند یا ویرایش می‌کند → سپس اجرای واقعی
```

- [ ] **Sandbox Environment** — اجرای agent در حالت dry-run با داده‌های mock
- [ ] **Chain Visualizer** — نمایش بصری DAG workflow قبل از اجرا
- [ ] **Expected Output Preview** — نمایش خروجی تخمینی قبل از اجرای واقعی
- [ ] **Step-by-Step Debug Mode** — اجرا یک مرحله در یک زمان با بررسی هر گام
- [ ] **Prompt Tester** — تست prompt با چند مدل به‌صورت موازی و مقایسه نتایج
- [ ] **Agent Profiler** — نمایش زمان و هزینه هر مرحله در آزمایش
- [ ] **Lab History** — ذخیره آزمایش‌های قبلی برای بازگشت و ویرایش
- [ ] **Quick Deploy** — تبدیل آزمایش موفق به workflow واقعی با یک کلیک

**معیار موفقیت:** کاربر می‌تواند یک workflow را بدون ریسک، در sandbox، قبل از اجرای واقعی آزمایش کند.

---

## 🔍 Phase 6 — AnySearch Engine + Vision Timeline + Vision Notes (هفته ۱۶–۱۷)

**هدف:** Vision نقطه ورود تکین برای جستجوی همه چیز در همه دستگاه‌ها باشد.

### کنترل محدوده جستجو

```
☐ فقط این دستگاه
☐ یک دستگاه خاص
☐ همه دستگاه‌های Mesh
```

### S.1 — معماری جستجو

- [ ] **Local Vector Index** — ChromaDB-lite برای embedding همه محتوای محلی
- [ ] **Content-Aware Search** — OCR برای تصاویر، transcription برای صوت/ویدیو
- [ ] **Temporal Search** — «فایل Excel که دیروز ساعت ۳ باز کردم»
- [ ] **Cross-Device Query** — broadcast query به همه Mesh nodes و merge نتایج
- [ ] **Smart Scope Suggestion** — پیشنهاد scope بر اساس query
- [ ] **Search History** — ذخیره و بازیابی جستجوهای قبلی
- [ ] **Mesh File Deduplication** — شناسایی و حذف اختیاری فایل‌های تکراری

### S.2 — انواع جستجو

| نوع | محتوا |
|-----|-------|
| فایل‌ها | متن، تصویر، صوت، ویدیو، PDF، archive |
| اپ‌ها | اپلیکیشن‌های نصب‌شده روی همه دستگاه‌ها |
| تنظیمات | سیستم + تنظیمات Vision |
| پیام‌ها | SMS، تاریخچه chat |
| مکالمات | تاریخچه مکالمات Vision |
| حافظه | حافظه episodic و semantic |
| تقویم | رویدادها و task‌ها |
| فعالیت | تاریخچه استفاده از اپ |
| کد | جستجو در فایل‌های کد در همه دستگاه‌ها |
| Captures | تصاویر و اسکرین‌شات‌های ذخیره‌شده با Vision Capture |
| یادداشت‌ها | محتوای Vision Notes |

### S.3 — Vision Timeline

**هدف:** یک نمای زمانی از همه اتفاقات دیجیتال کاربر — کاملاً local، کاملاً private.

```
تفاوت با Windows Recall:
  - کاملاً local (داده هرگز از دستگاه خارج نمی‌شود)
  - کاملاً private (قابل جستجو با زبان طبیعی فارسی)
  - قابل کنترل (کاربر تعیین می‌کند چه چیزی ثبت شود)
```

- [ ] **Activity Logger** — ثبت opt-in: کدام اپ، کِی، چقدر
- [ ] **File Access Log** — کدام فایل‌ها در چه زمانی باز شدند
- [ ] **Vision Interaction Log** — تاریخچه مکالمات Vision
- [ ] **Timeline UI** — نمای بصری زمانی با scroll و zoom
- [ ] **Natural Language Query** — «روز ۱۵ اردیبهشت چه فایل‌هایی کار کردم؟»
- [ ] **Cross-Device Timeline** — ادغام فعالیت همه دستگاه‌های Mesh در یک نما
- [ ] **Privacy Control Panel** — کاربر تعیین می‌کند کدام اپ‌ها در Timeline ثبت شوند
- [ ] **Export Timeline** — خروجی گرفتن از بازه زمانی انتخاب‌شده

### S.4 — Vision Notes ← NEW v16

**هدف:** یک سیستم یادداشت‌برداری هوشمند که با Memory، Timeline، و Agents یکپارچه است — نه یک اپ یادداشت معمولی.

```
تفاوت با یادداشت معمولی:
  یادداشت معمولی: فقط ذخیره متن
  Vision Notes: یادداشت زنده‌ای است که به حافظه Vision متصل است،
                با صدا قابل دیکته است، و Vision می‌تواند روی آن
                عمل کند («یادداشت جلسه دیروز رو خلاصه کن»)
```

- [ ] **Quick Capture** — ذخیره ایده سریع با یک دکمه (متن، صوت، یا تصویر)
- [ ] **Voice-to-Note** — دیکته صوتی با transcription خودکار (فارسی + چندزبانه)
- [ ] **AI Summarizer** — خلاصه‌سازی یادداشت‌های طولانی با یک دستور
- [ ] **Note-to-Memory** — تبدیل یادداشت مهم به حافظه دائمی Vision
- [ ] **Smart Tags** — تگ‌گذاری خودکار توسط Vision
- [ ] **Note Linking** — لینک دادن یادداشت‌ها به هم بر اساس ارتباط موضوعی
- [ ] **Cross-Device Sync** — یادداشت‌ها روی همه دستگاه‌های Mesh
- [ ] **Note Actions** — «این یادداشت رو به تیمم بفرست»، «از این یادداشت task بساز»
- [ ] **Search in Notes** — جستجوی semantic در همه یادداشت‌ها

**معیار موفقیت:** کاربر می‌تواند با صدا یادداشت بگیرد و بعد به Vision بگوید «از یادداشت‌های این هفته یک گزارش بساز».

---

## 🎤 Phase 7 — Adaptive Voice & Persona + Universal Language (هفته ۱۸–۲۰) ← **نقطه عطف Beta**

**هدف:** Vision نباید مثل کامپیوتر صحبت کند. باید گرم، شخصی، طبیعی، و برای همه مردم دنیا قابل استفاده باشد.

### V.1 — Voice Engine

- [ ] **Custom Wake Word** — openWakeWord با قابلیت تعریف عبارت دلخواه
- [ ] **Persian STT** — Vosk با database لهجه‌ها (شیرازی، اصفهانی، مشهدی، تهرانی، کردی)
- [ ] **faster-whisper** — دقت بالا برای transcription — مدل tiny آفلاین
- [ ] **Persian TTS** — Coqui XTTS v2 fine-tuned برای فارسی + Edge TTS fallback
- [ ] **Streaming TTS** — صدا از chunk اول پخش می‌شود (بدون انتظار)
- [ ] **Emotion Detection** — تشخیص خستگی، استرس، شادی از صدا و متن
- [ ] **Silence Detection** — WebRTC VAD بدون نیاز به دکمه
- [ ] **Interruption Handling** — توقف فوری هنگام قطع صحبت
- [ ] **Real-Time Translation** — ترجمه زنده مکالمات بین زبان‌ها
- [ ] **Phrase Variation Pool** — ۲۰+ جواب تأیید متنوع (نه همیشه «چشم»)

### V.2 — Persona Customization

- [ ] **Custom Name** — کاربر هر اسمی می‌دهد
- [ ] **Voice Selection** — چندین voice profile
- [ ] **Personality Sliders:**
  - میزان شوخ‌طبعی (۰–۱۰)
  - میزان صداقت (۰–۱۰)
  - رسمی بودن (۰–۱۰)
  - طول پاسخ ترجیحی (۰–۱۰)
- [ ] **Context-Aware Response Mode:**
  - صدای آرام → پاسخ مختصر و آرام
  - input متنی → فقط text (بدون TTS)
  - ریتم سریع → پاسخ‌های مختصر
  - دیر وقت شب → پاسخ‌های آرام و کم‌انرژی
- [ ] **Mood Detection** — اگر ساعت ۳ شب است، Vision می‌داند کاربر خسته است

### V.3 — Universal Language Engine ← NEW v16 (جایگزین لیست زبان‌های محدود)

**هدف:** Vision برای همه مردم دنیا، به هر زبانی، با کیفیت یکسان کار کند. نه یک لیست محدود زبان — بلکه پشتیبانی واقعی از همه زبان‌ها.

```
معماری سه‌لایه:
  Tier 1 (بومی‌سازی کامل):  فارسی + لهجه‌ها — STT/TTS اختصاصی، دیکشنری فرهنگی
  Tier 2 (پشتیبانی کامل):   عربی، ترکی، روسی، چینی، هندی، اسپانیایی، فرانسوی، آلمانی
  Tier 3 (پشتیبانی عمومی):  همه زبان‌های دیگر از طریق Universal LID + Whisper
```

- [ ] **Language Identification (LID)** — تشخیص خودکار زبان از روی متن یا صوت
- [ ] **Multilingual Auto-Switch** — سوئیچ خودکار بین هر دو زبان در یک مکالمه
- [ ] **Tier 1 — Persian Full Support:**
  - Dialect DB: شیرازی، اصفهانی، مشهدی، تهرانی، کردی، آذری
  - TTS اختصاصی: Coqui XTTS v2 fine-tuned فارسی
  - دیکشنری فرهنگی: اصطلاحات، ضرب‌المثل، ادب محاوره
- [ ] **Tier 2 — Full Support Pack:**
  - Arabic (با dialect detection: مصری، خلیجی، شامی)
  - Turkish, Russian, Chinese (Mandarin), Hindi, Spanish, French, German
  - هر زبان: STT + TTS اختصاصی + grammar rules
- [ ] **Tier 3 — Universal Fallback:**
  - faster-whisper large-v3 برای STT هر زبان
  - Edge TTS با ۴۰۰+ voice برای TTS هر زبان
  - LLM context در زبان کاربر
- [ ] **RTL/LTR Smart Layout** — تشخیص خودکار جهت نوشتار در UI
- [ ] **Language Pack Downloader** — دانلود on-demand پک زبانی برای offline
- [ ] **Cultural Tone Adapter** — لحن محاوره‌ای متناسب با فرهنگ هر زبان
- [ ] **Transliteration Support** — پشتیبانی از فینگلیش، عربیزی، و رومن‌نویسی فارسی

### V.4 — Vision Insights

- [ ] **Weekly AI Report** — «این هفته ۴۲ ساعت کار کردی، بیشتر روی [موضوع]...»
- [ ] **Digital Life Dashboard** — نمای بصری نحوه گذران وقت دیجیتال

**معیار موفقیت:** کاربر به هر زبانی صحبت کند، Vision به همان زبان پاسخ می‌دهد. Persian کاربر با لهجه شیرازی خوشامد می‌گوید.

---

## 📸 Phase 7.5 — Vision Capture + Context Cards + Accessibility (هفته ۱۹–۲۰، موازی)

### Vision Capture — حافظه بصری

- [ ] **Screenshot Capture** — گرفتن اسکرین‌شات و ارسال مستقیم به Vision
- [ ] **Camera Capture** — عکس از دوربین → OCR → ذخیره در حافظه
- [ ] **Screen Content Capture** — گرفتن محتوای فعلی صفحه نمایش (opt-in)
- [ ] **OCR Engine** — تشخیص متن از تصویر: Persian + Arabic + English + همه زبان‌ها
- [ ] **Smart Categorizer** — Vision خودکار تشخیص می‌دهد: رسید، کارت ویزیت، مقاله، قیمت
- [ ] **Capture Library** — مشاهده و مدیریت همه Capture‌ها
- [ ] **Capture-to-Action** — «این رسید را به صفحه‌گسترش هزینه‌هایم اضافه کن»
- [ ] **Capture Search** — جستجوی متن در همه Capture‌ها

### Vision Context Cards — کارت‌های اطلاعاتی هوشمند

```
مثال‌ها:
  اسم شخص در SMS → کارت: آخرین مکالمه، یادداشت‌ها، تاریخ تولد
  نام شرکت → اطلاعات جمع‌آوری‌شده قبلی از Captures و Memory
  عدد/تاریخ → Context تقویم مرتبط
  لینک → پیش‌نمایش ذخیره‌شده
```

- [ ] **Screen Analyzer** — تشخیص موجودیت‌های مهم در صفحه (Accessibility Service)
- [ ] **Context Card Engine** — ساخت کارت اطلاعاتی از Memory + Captures + Timeline
- [ ] **Smart Popup** — نمایش card به صورت overlay کوچک (قابل رد کردن)
- [ ] **Card Customization** — کاربر تعیین می‌کند چه نوع کارت‌هایی نمایش داده شوند
- [ ] **Quick Actions on Card** — از روی کارت شخص مستقیماً پیام بفرستد

### ♿ Vision Accessibility Mode ← NEW v16

**هدف:** Vision برای همه کاربران، صرف‌نظر از توانایی جسمی یا بینایی، با کیفیت یکسان کار کند.

```
تفاوت با accessibility معمولی Android:
  Android Accessibility: ابزارهای کمکی برای navigation
  Vision Accessibility: AI که محتوا را درک می‌کند و
                       به شکل مناسب برای هر کاربر بازنمایی می‌کند
```

- [ ] **Screen Reader Integration** — همکاری کامل با TalkBack و Switch Access
- [ ] **Vision Voice Assistant** — کنترل کامل Vision با صدا برای کاربران با محدودیت بینایی
- [ ] **AI Image Description** — توصیف خودکار تصاویر و آیکون‌ها با صدا
- [ ] **Smart Font Scaling** — تنظیم هوشمند اندازه متن بر اساس نیاز کاربر
- [ ] **High Contrast Mode** — تم‌های پرکنتراست برای کاربران با مشکل بینایی
- [ ] **Motion Reduction** — کاهش یا حذف انیمیشن‌ها برای کاربران حساس به حرکت
- [ ] **One-Switch Control** — کنترل کامل با یک دکمه/سوئیچ فیزیکی
- [ ] **Simplified HUD Mode** — نسخه ساده‌شده HUD برای کاربران مسن یا مبتدی
- [ ] **Dyslexia-Friendly Font** — فونت‌های مناسب برای کاربران با dyslexia
- [ ] **Accessibility Profile** — پروفایل کامل تنظیمات دسترس‌پذیری قابل اشتراک‌گذاری

**معیار موفقیت:** یک کاربر نابینا می‌تواند با صدا تمام قابلیت‌های Vision را استفاده کند.

---

## 🌐 Phase 8 — Device Mesh & Node Network (هفته ۲۱–۲۳)

**هدف:** همه دستگاه‌های کاربر یک Brain واحد می‌شوند. هر دستگاه جدید = منابع بیشتر برای Vision.

### M.1 — Device Pairing

- [ ] **Node Registry** — ثبت و مدیریت همه دستگاه‌های Mesh
- [ ] **Auto-Discovery** — mDNS + QR + manual IP + Brain Token
- [ ] **Resource Sharing (با کنترل کاربر):**
  - از کاربر پرسیده می‌شود: «آیا Vision از CPU/RAM این دستگاه استفاده کند؟»
  - نمایش contribution منابع هر دستگاه
- [ ] **Speed Boost Mode** — اضافه کردن دستگاه به‌طور مشهود سرعت Vision را بیشتر می‌کند
- [ ] **Selective Folder Sharing** — PC: فقط Drive D / Phone: فقط Downloads
- [ ] **Cross-Device Clipboard** — کپی روی گوشی، paste روی PC (E2E encrypted)

### M.2 — File Access

- [ ] **Remote File Browser** — مرور فایل‌های همه دستگاه‌های Mesh
- [ ] **Fast File Transfer** — VISN Protocol (Phase 1.5)
- [ ] **Remote Terminal** — shell با تأیید صریح
- [ ] **Settings Sync** — تنظیمات Vision روی همه دستگاه‌ها یکسان
- [ ] **Brain Election & Failover** — انتخاب خودکار بهترین Brain با auto-failover

### M.3 — Node Agent (Cross-Platform)

- [ ] **Auto-detect OS** — Linux / Windows / macOS / Android (Termux)
- [ ] **Persistent WebSocket** — exponential backoff (1s→2s→4s→max 60s)
- [ ] **Service installation** — systemd / Task Scheduler / launchd / Termux:Boot
- [ ] **Health Heartbeat** — ping Brain هر ۳۰ ثانیه
- [ ] **Graceful Shutdown** — ثبت خروج قبل از stop

**معیار موفقیت:** کاربر PC را به Mesh اضافه می‌کند. از گوشی فایل‌های PC را مرور می‌کند.

---

## ✈️ Phase 8.5 — Vision Offline Mode کامل (هفته ۲۲–۲۳، موازی)

> **اولویت: 🟠 زیاد** — برای کاربران ایرانی با دسترسی اینترنت محدود، این حیاتی است.

**هدف:** Vision در حالت آفلاین **بدون degradation** کار کند. کاربر نباید احساس کند Vision «ضعیف‌تر» شده.

```
آفلاین = همان تجربه، فقط از منابع محلی:
  Chat → Ollama (phi-3-mini یا gemma-2b)
  Search → ChromaDB local index
  Memory → SQLite local
  Voice → Vosk offline + Coqui offline TTS
  Agents → فقط agent‌هایی که API خارجی نیاز ندارند
```

- [ ] **Offline Mode Detector** — تشخیص خودکار قطع اینترنت
- [ ] **Graceful Degradation Manager** — تعیین کدام قابلیت‌ها offline در دسترسند
- [ ] **Offline Model Manager** — دانلود و مدیریت مدل‌های local (Ollama)
- [ ] **Local-Only Agent Profiles** — مشخص کردن کدام agent‌ها offline کار می‌کنند
- [ ] **Sync Queue** — ذخیره actions آفلاین برای اجرا هنگام reconnect
- [ ] **Offline Indicator** — نمایش ظریف: «حالت آفلاین — از مدل محلی استفاده می‌شود»
- [ ] **Offline Model Quality Selector** — کاربر بین سرعت و دقت مدل انتخاب می‌کند
- [ ] **Auto-Reconnect** — هنگام بازگشت اینترنت، بدون سر و صدا به حالت عادی برمی‌گردد
- [ ] **Offline-First Guarantee** — مدل‌های offline در Setup Wizard پیش‌فرض دانلود می‌شوند

**معیار موفقیت:** کاربر با قطع اینترنت، Vision را باز می‌کند. تمام قابلیت‌های اصلی کار می‌کنند.

---

## 🧬 Phase 9 — Memory System & RAG (هفته ۲۴–۲۵)

```
Embedding model:
  Brain-Lite: paraphrase-multilingual-MiniLM-L12-v2 (420MB، فارسی ✅)
  Brain-Full: bge-m3 برای دقت بیشتر
```

- [ ] **Episodic Memory** — رویدادهای خاص («دیروز درباره X صحبت کردیم»)
- [ ] **Semantic Memory** — دانش کلی درباره کاربر
- [ ] **Preference Memory** — سبک و علایق کاربر
- [ ] **Cross-Session Continuity** — ادامه مکالمه از session قبلی
- [ ] **Semantic Deduplication** — cosine > 0.95 → ادغام خودکار
- [ ] **ChromaDB Local** — offline vector DB کامل روی دستگاه
- [ ] **Qdrant Remote** — scale-out اختیاری روی VPS
- [ ] **Memory Export/Import** — backup و migrate بین دستگاه‌ها
- [ ] **Privacy-First** — کاربر کنترل کامل دارد: چه چیزی ذخیره شود، چه چیزی حذف شود
- [ ] **Forgetting Mechanism** — decay score بر اساس زمان + اهمیت
- [ ] **Versioned Memories** — تاریخچه تغییر preferences کاربر در طول زمان

---

## 🎓 Phase 9.5 — Vision Tutor + Automation Builder (هفته ۲۵، موازی)

### Vision Tutor — یادگیری شخصی‌سازی‌شده

```
مثال‌ها:
  «می‌بینم داری با Excel کار می‌کنی — یک shortcut برایت دارم»
  «این کد را می‌توانی با این روش بهتر بنویسی»
  «این هفته سه بار همین جستجو را کردی — بگذار یک workflow بسازم»
```

- [ ] **Usage Pattern Analyzer** — از Timeline و Digital Twin: کاربر با چه ابزارهایی کار می‌کند
- [ ] **Tip Engine** — تولید tips مرتبط با context فعلی
- [ ] **Spaced Repetition** — یادآوری چیزهایی که کاربر یاد گرفته اما ممکن است فراموش کرده
- [ ] **Learning Profile** — ردیابی مهارت‌های آموخته‌شده در طول زمان
- [ ] **Non-Intrusive Delivery** — tips فقط در زمان‌های مناسب نمایش داده می‌شوند

### Vision Automation Builder — ضبط و تکرار

```
تفاوت با Conversational OS:
  Conversational OS: کاربر توصیف می‌کند
  Automation Builder: کاربر نشان می‌دهد — Vision یاد می‌گیرد
```

- [ ] **Action Recorder** — ثبت دنباله اقدامات کاربر (opt-in)
- [ ] **Pattern Recognizer** — «این ۵ کار را هر روز صبح انجام می‌دهی»
- [ ] **Workflow Suggester** — «می‌خواهی این را به routine صبحگاهی تبدیل کنم؟»
- [ ] **One-Click Convert** — تبدیل pattern به workflow اجرایی
- [ ] **Workflow Library** — مدیریت همه workflow‌های ساخته‌شده

---

## 🤳 Phase 10 — Digital Twin & Self-Learning + Smart Power (هفته ۲۶–۲۸)

### D.1 — Personal Digital Twin

- [ ] **Behavioral Profiler** — الگوهای روزانه: بیداری، کار، استراحت، سرگرمی
- [ ] **Usage Pattern Analysis** — کدام اپ، کِی، چقدر
- [ ] **Preference Learning** — از thumbs up/down feedback
- [ ] **Routine Detection** — شناسایی عادات تکراری
- [ ] **Knowledge Graph** — نقشه دانش، علایق، و روابط کاربر
- [ ] **Personal KB** — پایگاه دانش اختصاصی از همه interactions

### D.2 — Self-Learning Loop

- [ ] **Agent Success/Failure Tracking** — کدام agent در کدام task موفق بود
- [ ] **Routing Optimization** — بهبود خودکار انتخاب مدل بر اساس نتایج
- [ ] **Self-Healing** — تشخیص anomaly و تلاش fix قبل از هشدار به کاربر
- [ ] **Crash Detection** — شناسایی کرش + گزارش با context
- [ ] **Version-over-Version Improvement** — هر به‌روزرسانی Vision را هوشمندتر می‌کند
- [ ] **Predictive Network Switching** — سوئیچ خودکار بین WiFi/LTE بر اساس الگوهای استفاده

### D.3 — Predictive Intelligence

- [ ] **Next Action Prediction** — پیش‌بینی اقدام بعدی با Markov chain
- [ ] **Resource Pre-allocation** — pre-warm محتمل‌ترین اپ
- [ ] **Focus Mode Auto-Detect** — کاهش interruption هنگام تمرکز
- [ ] **Smart Daily Brief** — هر صبح: تقویم + task‌ها + خبر مرتبط

### D.4 — Smart AI Battery Profiles ← NEW v16

**هدف:** Vision به شکل هوشمند مصرف باتری را مدیریت می‌کند — نه یک حالت صرفه‌جویی ساده، بلکه یک سیستم که یاد می‌گیرد.

```
مثال:
  Vision می‌داند کاربر هر روز ۸ تا ۱۰ صبح در جلسه است
  → در این بازه، AI processing کاهش می‌یابد
  → sync‌های غیرضروری به بعد از جلسه موکول می‌شوند
  → باتری ۲۰٪ بیشتر دوام می‌آورد
```

- [ ] **Adaptive Power Scheduler** — تنظیم خودکار پردازش AI بر اساس برنامه روزانه کاربر
- [ ] **Background Task Optimizer** — زمانبندی هوشمند sync و indexing (شارژ + WiFi)
- [ ] **Vision Power Profiles:**
  - **Performance** — بیشترین قدرت، مصرف بالا
  - **Balanced** — پیش‌فرض: هوشمند
  - **Efficiency** — طول عمر باتری بیشتر با کیفیت کمتر
  - **Stealth** — حداقل نور، صدا، و مصرف (جلسه، سینما)
- [ ] **Battery-Aware Model Selection** — باتری زیر ۲۰٪ → مدل سبک‌تر
- [ ] **Usage Prediction** — Vision پیش‌بینی می‌کند تا کِی باتری کافی است

---

## 🤝 Phase 10.5 — Shared Brain (Team Mode واقعی) (هفته ۲۷–۲۸، موازی)

```
معماری:
  Brain مشترک (حافظه تیم، تصمیمات، مستندات، اهداف)
       ↕
  Brain شخصی هر کاربر (context خصوصی، preferences)

قانون: هر کاربر می‌تواند از حافظه مشترک بخواند
       اما نوشتن در حافظه مشترک نیاز به تأیید دارد
```

- [ ] **Shared Memory Namespace** — حافظه مشترک جدا از حافظه شخصی
- [ ] **Personal Context Isolation** — اطلاعات شخصی هرگز به Shared Brain نمی‌رود
- [ ] **Team Knowledge Base** — تصمیمات، مستندات، اهداف تیم
- [ ] **Agent Delegation** — کاربر می‌تواند task را به Vision عضو دیگر تیم بدهد
- [ ] **Shared Audit Trail** — لاگ: چه کسی چه چیزی از Brain مشترک خواند/نوشت
- [ ] **Team Dashboard** — نمای کلی فعالیت همه اعضا (با اجازه هر عضو)
- [ ] **Permission Matrix** — هر عضو چه سطح دسترسی به Shared Brain دارد

---

## 🖥️ Phase 11 — OS-Level Integration + Conversational OS (هفته ۲۹–۳۲)

### OS Integration

- [ ] **Floating HUD Overlay** — روی همه اپ‌ها (Accessibility Service، opt-in)
- [ ] **Always-On Display** — widgets هوشمند روی صفحه خاموش
- [ ] **Notification Bar Integration** — Vision در notification panel
- [ ] **Boot Screen Branding** — نمایش Vision هنگام بوت
- [ ] **Screen Understanding** — تحلیل صفحه با MediaProjection (opt-in)
- [ ] **Gesture Control** — با دوربین یا سنسور
- [ ] **Popup Assistant** — کمک context-aware روی هر اپ (opt-in)
- [ ] **Custom Phone/SMS App** — اپ تماس و پیام اختصاصی Vision
- [ ] **App Usage Optimizer** — «۳ اپ یادداشت داری. ادغام کنیم؟»

### Conversational OS (کنترل OS با مکالمه)

```
نمونه:
"یادداشت‌های جلسه امروز رو خلاصه کن و بفرست به Slack تیم"
↓ Intent Parser
  intent: summarize + share
  source: Memory
  target: Slack API
↓ DAG Executor
  1. Memory Query → 2. LLM Summarize → 3. Slack POST → 4. Notify
زمان: < 10 ثانیه
```

- [ ] **Intent Parser** — LLM Function Calling با JSON schema
- [ ] **Cross-App Chains** — networkx DAG executor
- [ ] **Ambiguity Resolver** — سوال هوشمند هنگام ابهام
- [ ] **Context Reference** — درک «همون»، «اون»، «آخری»
- [ ] **Action Preview** — نمایش plan قبل از اجرا
- [ ] **Undo Chain** — برگشت زنجیره اقدامات (تا ۵ دقیقه)
- [ ] **Template Chains** — ذخیره chain‌های پرکاربرد
- [ ] **Cross-Device Chains** — actions روی Node‌های مختلف Mesh

---

## 🎯 Phase 11.5 — Vision Focus Mode + Broadcast (هفته ۳۱–۳۲، موازی)

### Vision Focus Mode — تمرکز عمیق هوشمند

```
تفاوت با Focus Mode معمولی:
  Focus Mode معمولی: فقط notifications را قطع می‌کند
  Vision Focus Mode: Vision درک می‌کند کاربر چه کاری می‌کند و کمک می‌کند
```

- [ ] **App Blocker** — block کردن اپ‌های حواس‌پرتی در زمان تمرکز
- [ ] **Smart Pomodoro** — Vision می‌فهمد کِی کاربر خسته شده و استراحت پیشنهاد می‌دهد
- [ ] **AI Ambient Sound** — صدای محیطی تولیدشده با AI بر اساس task جاری
- [ ] **Focus Session Summary** — پایان session: «امروز این کارها را انجام دادی»
- [ ] **Deep Work Detector** — تشخیص خودکار: کاربر در حال کار عمیق است
- [ ] **Energy Level Aware** — اگر استرس بالاست، Pomodoro کوتاه‌تر پیشنهاد می‌دهد
- [ ] **Focus History** — ردیابی session‌های تمرکز در طول هفته

### Vision Broadcast — پخش هوشمند

```
مثال:
  «این خلاصه را به Telegram، Email، و Notion من بفرست»
  Vision:
    Telegram → پیام کوتاه ۲۰۰ کلمه‌ای
    Email → HTML کامل با heading و bullet
    Notion → markdown با ساختار database
```

- [ ] **Multi-Destination Sender** — ارسال همزمان به چندین پلتفرم
- [ ] **Platform-Aware Formatter** — فرمت متفاوت برای هر پلتفرم
- [ ] **Scheduled Broadcast** — «این را فردا صبح ۸ بفرست»
- [ ] **Broadcast Templates** — قالب‌های پرکاربرد ذخیره‌شده
- [ ] **Delivery Confirmation** — تأیید رسیدن پیام به هر مقصد

---

## 🔌 Phase 12 — MCP & Plugin Ecosystem + Widget API (هفته ۳۳–۳۵)

> MCP در ۲۰۲۶ به استاندارد جهانی تبدیل شده با ۱۰,۰۰۰+ server عمومی.

### MC.1 — MCP Client

- [ ] **Full MCP Protocol** — tools / resources / prompts
- [ ] **Server Discovery** — auto-discover MCP servers در شبکه
- [ ] **Multi-Server Support** — اتصال همزمان به چندین server
- [ ] **Community Registry** — لیست ۱۰,۰۰۰+ server عمومی

### MC.2 — MCP Host (Vision به عنوان MCP Server)

- [ ] **Expose Vision Skills** — همه Vision Agentها از طریق MCP قابل دسترس
- [ ] **MCP Authentication** — OAuth + API key
- [ ] **Rate Limiting** — کنترل مصرف per-client
- [ ] **Agent Sandboxing** — plugins در container ایزوله اجرا می‌شوند

### MC.3 — Plugin Marketplace

- [ ] **Skill Store** — نصب با یک کلیک
- [ ] **Plugin SDK** — ساخت plugin با Python یا Kotlin
- [ ] **KianIrani Projects as Plugins** — MHRV, KV2M, KICDN به عنوان Vision plugins
- [ ] **Plugin Revenue Share** — developer از فروش درآمد دارد
- [ ] **Vision Public API** — REST API برای third-party developers

### MC.4 — Vision Widget API

**هدف:** توسعه‌دهندگان بتوانند ویجت‌های تعاملی برای HUD بسازند که مستقیماً با Brain صحبت می‌کنند.

- [ ] **Widget SDK** — API برای ساخت ویجت HUD (Kotlin)
- [ ] **Widget Registry** — مدیریت و نصب ویجت‌های third-party
- [ ] **Widget Sandbox** — اجرای ویجت در context ایزوله
- [ ] **Brain-Widget Bridge** — WebSocket channel برای ارتباط widget با Brain
- [ ] **Widget Marketplace** — فروشگاه ویجت‌های سفارشی

---

## 🔒 Phase 13 — Zero-Trust Security + Behavioral Baseline + Privacy Monitor (هفته ۳۶–۳۸)

**هدف:** کاربر می‌تواند Vision را با اقدامات حساس و داده‌ها اعتماد کند.

```
Zero-Trust — هر درخواست:
├── Who?   → Token + fingerprint
├── What?  → action در whitelist
├── Why?   → reason قابل verify
├── When?  → timestamp در پنجره مجاز
└── Where? → از Node شناخته‌شده
```

### SEC.1 — Core Security

- [ ] **End-to-End Encryption** — AES-256-GCM برای همه ارتباطات Node-Brain
- [ ] **mTLS for Mesh** — mutual TLS بین همه دستگاه‌ها
- [ ] **Speaker Verification** — احراز هویت صوتی کاملاً آفلاین
- [ ] **Anti-Spoofing** — مقاومت در برابر حملات صدای ضبط‌شده
- [ ] **Intent Verification** — LLM بررسی می‌کند: آیا action با request سازگار است؟
- [ ] **Action Rate Limiting** — محدودیت اقدامات حساس per time window
- [ ] **Rollback Engine** — snapshot + یک‌کلیک برگشت
- [ ] **Secret Vault** — Android Keystore برای API keys
- [ ] **Privacy Dashboard** — نمایش بصری: همه data کجاست؟
- [ ] **Data Residency Control** — بدون اجازه explicit، data از device خارج نمی‌شود
- [ ] **Privacy Score** — AI-generated: external services چقدر به data دسترسی دارند
- [ ] **Vision Backup/Restore** — backup رمزنگاری‌شده کامل

### SEC.2 — Behavioral Baseline ← برگشت از v12/v13/v14

**هدف:** شناسایی رفتار غیرعادی و تهدیدات امنیتی قبل از اینکه آسیبی وارد شود.

```
تفاوت با Behavioral Profiler در Digital Twin:
  Digital Twin Behavioral Profiler: یاد گرفتن عادات کاربر برای کمک بهتر
  Security Behavioral Baseline: تشخیص انحراف از رفتار عادی به عنوان تهدید امنیتی

مثال تهدید:
  کاربر معمولاً ۱۰-۲۰ فایل در روز کار می‌کند
  ناگهان ۵۰۰ فایل در ۵ دقیقه دستکاری می‌شوند
  → هشدار فوری: رفتار غیرعادی شناسایی شد
```

- [ ] **Normal Behavior Profiler** — ساخت پروفایل رفتار عادی کاربر (۷ روز اول)
  - الگوی دسترسی به فایل (چه فایل‌هایی، چه زمانی، چند تا)
  - الگوی استفاده از agent (کدام agentها، با چه تناوبی)
  - الگوی دستورات (نوع و پیچیدگی دستورات معمول)
  - ساعات فعال معمول
- [ ] **Anomaly Detector** — real-time مقایسه رفتار جاری با baseline
- [ ] **Threat Scoring** — نمره‌دهی به هر انحراف بر اساس شدت
- [ ] **Tiered Alert System:**
  - Low: لاگ silent
  - Medium: نمایش به کاربر
  - High: توقف عملیات + تأیید اجباری
  - Critical: قفل آنی + هشدار فوری
- [ ] **False Positive Learning** — اگر کاربر هشدار را رد کرد، baseline به‌روزرسانی شود
- [ ] **Baseline Reset** — کاربر می‌تواند baseline را reset کند (مثلاً بعد از تغییر عادات)
- [ ] **Multi-Device Baseline** — baseline جداگانه برای هر دستگاه در Mesh

### SEC.3 — Privacy Threat Monitor ← NEW v16

**هدف:** Vision فعالانه اپ‌های نصب‌شده را از نظر دسترسی به داده‌های کاربر بررسی می‌کند و هشدار می‌دهد.

```
مثال:
  یک اپ بازی نصب شده که به میکروفن، مخاطبین، و موقعیت مکانی دسترسی دارد
  Vision: «این اپ ۳ دسترسی غیرضروری دارد — می‌خواهی محدود کنم؟»
```

- [ ] **App Permission Auditor** — بررسی دوره‌ای همه دسترسی‌های اپ‌های نصب‌شده
- [ ] **Unnecessary Permission Detector** — تشخیص دسترسی‌های غیرمنطقی نسبت به عملکرد اپ
- [ ] **Data Flow Monitor** — (opt-in) نظارت بر ارسال داده به سرورهای خارجی
- [ ] **Privacy Risk Score** — نمره ریسک حریم خصوصی برای هر اپ
- [ ] **Auto-Revoke Suggestions** — پیشنهاد لغو دسترسی‌های بلااستفاده با یک کلیک
- [ ] **New App Alert** — هنگام نصب اپ جدید: بررسی فوری دسترسی‌ها
- [ ] **Privacy Weekly Report** — گزارش هفتگی وضعیت حریم خصوصی دستگاه
- [ ] **Tracker Blocker** — (اختیاری) بلوک SDK‌های tracking شناخته‌شده

**معیار موفقیت:** کاربر می‌تواند با یک نگاه ببیند کدام اپ‌ها بیشترین ریسک حریم خصوصی را دارند.

---

## 💬 Phase 14 — Communication Layer (هفته ۳۹–۴۲)

**هدف:** Vision می‌تواند از طرف کاربر ارتباط برقرار کند. کاربر تعیین می‌کند Vision خودش را معرفی کند یا نه.

- [ ] **Text Message Assistant** — draft، پیشنهاد، ارسال با تأیید کاربر
- [ ] **Voice Call Assistant** — صحبت از طرف کاربر (با تأیید)
- [ ] **Video Call Integration** — کمک حین video call
- [ ] **Identity Mode Selector:**
  - Vision خودش را AI معرفی کند
  - Vision به عنوان کاربر صحبت کند (انتخاب کاربر)
- [ ] **Safety Confirmation Layer** — همه اقدامات ارتباطی نیاز به تأیید صریح دارند
- [ ] **Contact Intelligence** — پروفایل غنی از هر مخاطب در طول زمان
- [ ] **Encrypted P2P Calls** — تماس رمزنگاری‌شده بین Vision users بدون واسطه

---

## 🏥 Phase 15 — Health & Biometrics + Activity-Aware (هفته ۴۳–۴۵)

**هدف:** Vision وضعیت جسمی کاربر را درک می‌کند و بر اساس آن سازگار می‌شود.

- [ ] **Health Connect Integration** — ضربان قلب، گام، کیفیت خواب (کاملاً local)
- [ ] **Wear OS Support** — ساعت هوشمند به Mesh اضافه می‌شود
- [ ] **Sleep Stage Detection** — از accelerometer
- [ ] **Stress Detection** — از HRV + الگوهای صوتی
- [ ] **Sleep-Aware Mode** — Vision در ساعات خواب آرام است
- [ ] **Calm Mode** — استرس بالا → کاهش interruption + tone آرام‌تر
- [ ] **Weekly Health Brief** — خلاصه هفتگی با TTS هر یکشنبه صبح
- [ ] **Smart Reminder Timing** — یادآوری در اوج هوشیاری
- [ ] **Activity-Aware Suggestions** ← برگشت از v12 — پیشنهادهای هوشمند بر اساس فعالیت فیزیکی جاری:
  - در حال پیاده‌روی → «یادداشت صوتی بگیر؟»
  - بعد از ورزش → «آب بنوش — یادآوری هیدراتاسیون»
  - ساعات بیداری اوج (HRV بالا) → «بهترین زمان کار عمیق است»
  - خستگی جسمی (قدم‌های کم + HRV پایین) → کاهش complexity دستورات
- [ ] **Privacy Firewall** — داده‌های سلامت هرگز از دستگاه خارج نمی‌شوند

---

## 🏠 Phase 16 — IoT & Smart Environment (هفته ۴۶–۴۹)

**هدف:** Vision محیط فیزیکی کاربر را کنترل می‌کند.

- [ ] **Matter Protocol Support** — کنترل دستگاه‌های Matter-compatible
- [ ] **Internal MQTT Broker** — Brain به عنوان broker؛ جایگزین سرویس‌های خارجی
- [ ] **Home Assistant Bridge** — یکپارچه‌سازی دوطرفه
- [ ] **Scene Engine** — «Work Mode»، «Cinema Mode»، «Sleep»، «Away»
- [ ] **Voice Control Hub** — جایگزین بالقوه Alexa/Google Home
- [ ] **Energy Monitor** — مصرف برق از smart meters و یکپارچه با Smart Daily Brief

---

## 🛠️ Phase 17 — Developer Suite & Marketplace (هفته ۵۰–۵۳)

**هدف:** Vision پلتفرمی شود که توسعه‌دهندگان می‌خواهند رویش بسازند.

- [ ] **MCP Server Builder** — ساخت MCP server بدون کد
- [ ] **API Playground** — تست تعاملی همه Vision endpoints
- [ ] **Log Aggregator** — لاگ‌های همه Mesh nodes در یک جا
- [ ] **Performance Dashboard** — مانیتور همه دستگاه‌ها در یک نما
- [ ] **Docker Manager** — مدیریت container از رابط Vision
- [ ] **CI/CD Monitor** — وضعیت GitHub Actions در Vision
- [ ] **Skill SDK** — Python/Kotlin SDK برای ساخت Vision skills
- [ ] **Voice-to-Code** — دیکته کد با صدا برای توسعه‌دهندگان
- [ ] **AI Code Review** — review خودکار PR با Claude API
- [ ] **Infrastructure Diagram AI** — رسم خودکار diagram از docker-compose.yml

---

## ⚙️ Phase 17.5 — Distributed Compute Mesh (هفته ۵۱–۵۴)

**هدف:** همه دستگاه‌های Mesh CPU/RAM خود را به اشتراک می‌گذارند. هر task سنگین روی قوی‌ترین دستگاه اجرا می‌شود — با رضایت کاربر.

```
مثال:
  کاربر: "این فایل 4K ویدیو رو compress کن"
  Vision: گوشی فقط 2GB RAM آزاد دارد
  Decision: ارسال task به PC (16GB RAM، GPU)
  Result: ویدیو روی PC compress می‌شود
  User experience: فقط progress bar → نتیجه نهایی
```

- [ ] **Task Capability Matrix** — هر Node چه task‌هایی را می‌تواند انجام دهد
- [ ] **Resource Bidding** — Nodes اعلام می‌کنند چه منابعی آزاد دارند
- [ ] **Task Scheduler** — بهترین Node برای هر task انتخاب می‌شود
- [ ] **Result Relay** — نتیجه از Node به Brain به Launcher
- [ ] **Progress Streaming** — پیشرفت real-time از Node به کاربر
- [ ] **Privacy Guarantee** — کاربر کنترل می‌کند کدام task‌ها روی کدام Node اجرا شوند
- [ ] **Fault Tolerance** — Node قطع شد → task به Node دیگری منتقل شود
- [ ] **Resource Contribution UI** — «PC شما ۱۲٪ از tasks را اجرا کرد»

---

## 📢 Phase 18 — Marketing & Launch (مداوم از P3+)

### کوتاه‌مدت (قبل از Beta)

- [ ] **Landing Page — kianirani.tr** — email capture، داستان پروژه
- [ ] **Teaser Video** — ۶۰ ثانیه نمایش HUD و قابلیت‌های کلیدی
- [ ] **Telegram/Discord channel** — ساخت جامعه اولیه
- [ ] **Persian tech YouTubers** — پارتنرشیپ preview زودهنگام

### هنگام Beta Launch

- [ ] **Product Hunt Launch** — استراتژی هماهنگ
- [ ] **Hacker News** — معرفی فنی
- [ ] **Referral campaign** — معرفی دوست → trial طولانی‌تر
- [ ] **Open Source Core** — Apache 2.0 برای core غیرتجاری
- [ ] **Full documentation** — فارسی + English
- [ ] **Beta Program** — ۱۰۰۰ کاربر اول با onboarding اختصاصی

### موضع‌گیری برای سرمایه‌گذاران

- [ ] **Pitch Deck** — roadmap، team، بازار، تمایز رقابتی
- [ ] **Live Demo Video** — نمایش قابلیت‌های واقعی
- [ ] **Anthropic Partnership Approach** — محور: «ساخته شده با Claude AI»
- [ ] **Accelerator Programs** — Y Combinator، Seedcamp، acceleratorهای خاورمیانه
- [ ] **App Store Release** — Google Play، F-Droid، Cafebazaar

### روایت برای سرمایه‌گذاران

> Vision یک اپ نیست. Vision یک پلتفرم است. در دنیایی که مایکروسافت و گوگل در حال ساخت سیستم‌عامل‌های AI-first هستند که نیازمند سخت‌افزار جدید و cloud اختصاصی هستند، Vision همان قابلیت را به هر دستگاه موجود برای ۳ دلار در ماه می‌دهد. با کمک کامل Claude AI ساخته شده، از همه زبان‌های دنیا پشتیبانی می‌کند، و نشان می‌دهد وقتی AI قدرتمند، قابل دسترس، مقرون‌به‌صرفه، و حاکمیتی باشد چه اتفاقی می‌افتد.

---

## 🖥️ Phase 19 — Cross-Platform Expansion (بلندمدت)

- [ ] **Windows Client** — قابلیت‌های کامل
- [ ] **Linux Client** — نسخه server + desktop
- [ ] **macOS Client**
- [ ] **Browser Extension** — Vision در Chrome/Firefox
- [ ] **Unified Brain Sync** — یک Brain در همه platforms
- [ ] **PWA Fallback** — Progressive Web App برای دستگاه‌های بدون native client

---

## 🌌 Phase 20 — Full Vision OS (بلندمدت)

**هدف نهایی:** Vision یک سیستم‌عامل کامل می‌شود. مثل Windows، مثل Android، اما هوشمندتر.

اپلیکیشن‌های first-party آینده:
- Vision Messenger — اپ پیام‌رسانی اختصاصی
- Vision Calls — تماس و مخاطبین
- Vision Camera — دوربین هوشمند AI
- Vision Files — مدیر فایل پیشرفته
- Vision Browser — مرورگر AI-enhanced
- Vision Music/Video Player — پخش‌کننده رسانه با AI
- Vision Dashboard — dashboard cross-device شخصی
- Vision Widgets — کتابخانه widget پیشرفته
- Vision Themes — فروشگاه تم‌های گرافیکی
- Vision App Store — marketplace اختصاصی

---

## ⚙️ Stack فنی کامل — v16.0

| لایه | فناوری | نسخه |
|------|---------|------|
| **Android** | Kotlin + Jetpack Compose | 2.0.x / BOM 2025.06 |
| **Brain-Lite** | Ktor + Room + SQLite | 2.x |
| **Brain-Full** | FastAPI + PostgreSQL + Redis | 0.115+ / 16 / 7 |
| **Reasoning** | LangGraph + Custom State Machine | 0.2.x |
| **AI Gateway** | LiteLLM (multi-provider) | 1.x |
| **Vector DB** | ChromaDB-lite (local) + Qdrant (remote) | 0.5 / 1.x |
| **STT Offline** | Vosk (Persian) + faster-whisper large-v3 | 0.3.47 / 1.0 |
| **TTS** | Coqui XTTS v2 + Edge TTS (400+ voices) | — |
| **Wake Word** | openWakeWord + custom training | 0.6+ |
| **Language ID** | langdetect + fastText LID model | — |
| **MCP** | MCP SDK (Python + Kotlin) | — |
| **Compression** | LZ4 + zstd (VISN Protocol) | 1.3.0 / 1.5.5 |
| **Discovery** | mDNS (NsdManager) + QR + Brain Token | — |
| **Security** | mTLS + AES-256-GCM + Android Keystore | — |
| **IoT** | Matter SDK + Eclipse Mosquitto | 1.2 / 2.x |
| **Health** | Health Connect API + Wear OS | 1.1.0 |
| **Sync** | automerge-py (CRDT) | — |
| **Offline AI** | Ollama (phi-3-mini / gemma-2b) | — |
| **Workflow** | Temporal (جایگزین n8n) | 1.x |
| **Knowledge Graph** | NetworkX + Pyvis | 3.x |
| **Compute Mesh** | Temporal + WebSocket + Redis Queue | — |
| **OCR** | Tesseract + PaddleOCR (Multi-language) | — |
| **DI (Android)** | Hilt | 2.x |
| **Testing (Android)** | JUnit 5 + Turbine + Mockk | — |
| **Testing (Python)** | pytest + pytest-asyncio + Locust | — |
| **Linting (Android)** | Detekt + ktlint | — |
| **Linting (Python)** | Ruff + mypy --strict | — |
| **DB Migrations** | Alembic (Python) + Room migrations | — |
| **Observability** | OpenTelemetry + structlog + Grafana | — |
| **CI/CD** | GitHub Actions + Fastlane | — |
| **Secret Mgmt** | dotenv-vault + Android Keystore | — |
| **API Docs** | OpenAPI 3.1 (auto-generated) | — |

---

## 💰 قیمت‌گذاری و Activation

| پلن | قیمت | قابلیت‌ها |
|-----|------|-----------|
| Trial | رایگان، ۱۴ روز | همه قابلیت‌ها، بدون credit card |
| Personal | ۳$/month یا ۳۰$/year | قابلیت کامل، تا ۵ دستگاه |
| Developer | ۸$/month | دسترسی API + انتشار plugin + Widget API |
| Team | ۱۵$/month | Shared Brain، تا ۱۰ کاربر |
| Enterprise | سفارشی | Brain اختصاصی، SLA، پشتیبانی اولویت‌دار |

**جریان Activation (فعلی):** Telegram bot → کد ۱۴ روز trial
**جریان Activation (آینده):** kianirani.tr → پرداخت → activation فوری

---

## 🔑 قوانین طراحی محوری

### قوانین Multi-Token
- هر تعداد key از هر provider قابل اضافه کردن
- هیچ token تنهایی drain نمی‌شود وقتی جایگزین وجود دارد
- سوئیچ خودکار < 100ms
- هزینه قبل از task سنگین به کاربر نمایش داده می‌شود
- Model Fallback Chain قابل تنظیم توسط کاربر

### قوانین Distributed Brain
- هر دستگاه باید بتواند به‌تنهایی Vision را اجرا کند
- وقتی دستگاه جدید اضافه می‌شود، با رضایت کاربر منابع آن به Brain اضافه می‌شود
- Brain خودکار بهترین دستگاه موجود را انتخاب می‌کند (Brain Score)
- قطع شدن یک Node نباید سیستم را از کار بیاندازد
- Session Handoff: context هرگز از دست نمی‌رود

### قوانین Search
- Scope همیشه explicit است
- پوشش: files, text, audio, video, images, apps, settings, messages, conversations, memory, activity, code, captures, notes

### قوانین Trust
- هر agent و skill یک Trust Level دارد (Read / Suggest / Auto / Critical)
- اقدامات مالی و ارتباطی همیشه Critical هستند
- Conflict Detector قبل از هر action تضادها را بررسی می‌کند
- Audit log کامل برای همه اقدامات Auto

### قوانین Offline
- مدل‌های offline در Setup Wizard پیش‌فرض نصب می‌شوند
- کاربر نباید هیچ‌وقت با پیغام خطا روبرو شود — فقط fallback به مدل محلی
- Sync Queue آفلاین: هر action ذخیره می‌شود برای اجرا هنگام reconnect

### قوانین زبان
- Vision برای همه زبان‌ها به صورت یکسان کار می‌کند
- فارسی زبان Tier 1 است — بومی‌سازی کامل با dialect support
- هر زبان جدید با کیفیت native تجربه می‌شود، نه ترجمه
- RTL/LTR به صورت خودکار تشخیص داده و اعمال می‌شود

### قوانین Security
- اقدامات حساس نیاز به تأیید explicit دارند
- Secrets فقط در Android Keystore
- داده‌های سلامت هرگز به Cloud نمی‌روند
- Behavioral Baseline فعال است — انحراف از رفتار عادی alert ایجاد می‌کند
- Privacy Threat Monitor اپ‌های با دسترسی غیرضروری را شناسایی می‌کند
- Audit log کامل برای همه agent actions
- Rollback برای هر تغییر agent در دسترس است

### قوانین Persona
- هرگز مثل کامپیوتر صحبت نکن
- گرم، طبیعی، شخصی به عنوان default
- کاربر کنترل: نام، صدا، شوخ‌طبعی، رسمی بودن، صداقت
- انرژی و context کاربر را match کن

### قوانین کد
- Clean Architecture + MVI در Android
- Hexagonal در Brain-Full
- Type hints همیشه در Python
- Detekt + ktlint در Android، Ruff + mypy در Python
- Coverage حداقل ۷۰٪ برای core modules
- هرگز secret در کد

---

## 📌 اولویت‌های اجرایی (همین الان)

```
1.  🔧 رفع WebSocket Caddy blocker (1-2 ساعت)
2.  📁 Push skeleton code به GitHub (1 روز)
3.  🛠️ Phase X: Setup Detekt + Ruff + CI/CD (موازی)
4.  🚨 Error Reporting Foundation (موازی با P0)
5.  🧠 شروع BrainLiteService skeleton (Ktor + ForegroundService)
6.  ⚡ Phase 1.5: اضافه کردن lz4-java + zstd-jni به build.gradle
7.  🔒 Phase 4.5: Trust Level Manager skeleton (موازی با P4)
8.  🤖 Multi-Token Router (Phase 2)
9.  📱 Android Launcher MVP (Phase 3)
10. 🔐 Activation system (Phase 4)
11. 🤖 Agentic Reasoning Core + Scheduler (Phase 5)
12. 🎤 Voice and Persona + Universal Language (Phase 7)
13. 📸 Vision Capture + Context Cards + Accessibility (Phase 7.5)
14. 🌐 Device Mesh expansion (Phase 8)
15. ✈️ Offline Mode کامل (Phase 8.5)
16. 🔍 Behavioral Baseline + Privacy Monitor (Phase 13)
17. 🚀 Launch Beta + Marketing — ۱۰۰۰ کاربر اول (Phase 18)
```
---

*Vision OS — نه فقط یک اپ. یک ابرقدرت در دستان کاربر.*
*آینده تعامل انسان و هوش مصنوعی. هر دستگاه یک مغز. هر زبانی خوش‌آمد است.*

**Built with Claude AI (Anthropic) · v16.0 · June 2026**
