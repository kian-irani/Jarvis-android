# 🏗️ ARCHITECTURE — Vision Agent OS

> مستند معماری فنی · Technical Architecture Document  
> نسخه: v1.0 · ژوئن ۲۰۲۶

---

## دیاگرام کلی / High-Level Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                    VISION AGENT OS                           │
│                  (Android Application)                       │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                   UI Layer                          │    │
│  │  HUD Cyberpunk · App Drawer · Widgets · Overlays    │    │
│  └──────────────────┬──────────────────────────────────┘    │
│                     │                                        │
│  ┌──────────────────▼──────────────────────────────────┐    │
│  │               Core Services                         │    │
│  │  VoiceEngine · AgentEngine · TokenPool · Memory     │    │
│  └──────┬───────────────┬──────────────┬───────────────┘    │
│         │               │              │                     │
│  ┌──────▼──────┐ ┌──────▼──────┐ ┌────▼────────────────┐   │
│  │  Brain      │ │  Mesh       │ │  Skills             │   │
│  │  Server     │ │  Network    │ │  File·App·Server    │   │
│  │  (Remote)   │ │  (Nodes)    │ │  Message·Coding     │   │
│  └─────────────┘ └─────────────┘ └─────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

---

## لایه‌های معماری / Architecture Layers

### 1. UI Layer — رابط کاربری

```
UI Layer
├── HUD Engine
│   ├── ArcReactorComponent   # قلب مرکزی
│   ├── GlitchEffectModifier  # افکت glitch
│   ├── HazePanelComponent    # glassmorphism
│   ├── AudioReactiveSystem   # واکنش صوتی
│   └── SpatialReactiveSystem # واکنش gyroscope
│
├── Navigation
│   ├── HomeScreen           # صفحه اصلی HUD
│   ├── AppDrawer            # کشوی اپ‌ها
│   ├── ChatScreen           # مکالمه با AI
│   ├── SkillsScreen         # مهارت‌های مدیریتی
│   └── SettingsScreen       # تنظیمات
│
└── Theme System
    ├── ThemeManager         # مدیریت تم‌ها
    ├── ColorTokens          # رنگ‌های سیستم
    └── AnimationSpec        # تعریف انیمیشن‌ها
```

**Tech:** Kotlin + Jetpack Compose + Haze 2.0 + AGSL + Lottie

---

### 2. AI Core Layer — هسته هوش مصنوعی

```
AI Core
├── ProviderManager
│   ├── AnthropicProvider
│   ├── OpenAIProvider
│   ├── GeminiProvider
│   ├── GroqProvider
│   ├── OllamaProvider (local)
│   └── CustomProvider
│
├── TokenPoolManager
│   ├── PriorityRouter       # اولویت‌بندی
│   ├── FailoverHandler      # مدیریت خطا
│   ├── CostTracker          # ردیابی هزینه
│   ├── BudgetController     # کنترل بودجه
│   └── RateLimitHandler     # مدیریت محدودیت
│
└── SecureKeyStore
    ├── KeystoreManager      # Android Keystore
    └── EncryptedDataStore   # ذخیره امن
```

**جریان درخواست:**
```
User Input
    → TokenPool.route()
    → ProviderManager.selectBest()
    → Provider.complete()
    → [on failure] FailoverHandler.next()
    → Response
```

---

### 3. Agent Engine Layer — موتور عامل

```
Agent Engine
├── Orchestrator             # هماهنگ‌کننده
│   ├── TaskDecomposer       # تجزیه وظیفه
│   ├── AgentSelector        # انتخاب Agent
│   └── ResultAggregator     # جمع‌بندی نتایج
│
├── Agents
│   ├── PlannerAgent
│   ├── CoderAgent
│   ├── ResearcherAgent
│   ├── WriterAgent
│   └── DeviceManagerAgent
│
├── ReActLoop
│   ├── ThoughtGenerator
│   ├── ActionExecutor
│   └── ObservationParser
│
└── ToolRegistry
    ├── FileSystemTool (sandboxed)
    ├── ShellTool (sandboxed)
    ├── SSHTool
    ├── DockerTool
    ├── GitTool
    ├── WebSearchTool
    ├── SMSTool
    └── CameraTool
```

---

### 4. Voice Layer — لایه صوتی

```
Voice Engine
├── WakeWordDetector (Vosk, offline)
├── SpeechToText
│   ├── VoskSTT (Persian + English)
│   └── CloudSTT (fallback)
├── TextToSpeech
│   ├── PiperTTS (offline)
│   └── EmotionalProsody
└── IntentRouter
    ├── RuleBasedMatcher
    └── LLMIntentClassifier
```

---

### 5. Memory Layer — لایه حافظه

```
Memory System
├── ShortTermMemory
│   └── ConversationBuffer (in-memory, last N turns)
│
├── LongTermMemory
│   ├── SQLiteDB
│   │   ├── user_preferences
│   │   ├── projects
│   │   ├── tasks
│   │   └── conversation_summaries
│   └── VectorDB
│       ├── ChromaDB (local)
│       └── QdrantRemote (optional)
│
└── RAGPipeline
    ├── EmbeddingGenerator
    ├── ChunkIndexer
    └── ContextRetriever
```

---

### 6. Brain Server — سرور مرکزی

```
brain-server/ (Python)
├── main.py                  # FastAPI app
├── api/
│   ├── chat.py             # /api/chat
│   ├── agents.py           # /api/agents
│   └── mesh.py             # /api/mesh
├── ws/
│   └── mobile.py           # /ws/mobile (WebSocket)
├── core/
│   ├── litellm_router.py   # LiteLLM multi-provider
│   ├── token_pool.py       # Token management
│   └── agent_runner.py     # Agent execution
└── db/
    ├── postgres.py         # PostgreSQL
    └── redis.py            # Redis cache
```

**ارتباط Android ↔ Brain Server:**
```
Android App
    ↕ WebSocket (ws/mobile)
    ↕ REST API (/api/*)
Brain Server (FastAPI)
    ↕ LiteLLM
AI Providers (Cloud/Local)
```

---


---

### 6.5 Activation & Token Service — سرویس فعال‌سازی (kiancdn)

```
activation-service/ (Python, کنار brain-server)
├── bot/
│   └── kiancdn_bot.py       # ربات تلگرام @kian_irani_cdn_f
│       ├── /start            # احراز هویت کاربر تلگرام
│       └── /token            # صدور توکن امضاشده برای کاربر
├── api/
│   ├── issue.py             # POST /activation/issue  (داخلی، فقط ربات)
│   └── verify.py            # POST /activation/verify (از سمت اپ)
├── core/
│   ├── token_signer.py      # HMAC/JWT امضا + انقضا
│   ├── rate_limit.py        # محدودیت نرخ per-user
│   └── revocation.py        # لیست ابطال
└── db/
    └── tokens (Postgres)    # user_id ↔ token ↔ status ↔ issued_at
```

**جریان فعال‌سازی:**
```
[Telegram User] → kiancdn bot /token
    → Activation Service.issue()  (HMAC sign, store)
    → bot پاسخ: توکن به کاربر
[Android App] → ورودی توکن
    → POST /activation/verify
    → [valid] فعال‌سازی + ذخیره در Android Keystore
    → [invalid/revoked] رد
[App periodic] → re-verify (با grace period آفلاین)
```

**اصول امنیتی:** توکن امضاشده و غیرقابل‌جعل، انقضا و ابطال، rate-limit، تشخیص اشتراک‌گذاری، ذخیره‌ی امن در Keystore. طراحی کامل: `docs/ACTIVATION.md`.

---

### 7. Mesh Network — شبکه توزیع‌شده

```
Mesh Network
├── NodeDiscovery (mDNS/Bonjour)
├── NodeRegistry
├── CommandRouter
│   ├── SSHChannel
│   └── HTTPChannel
└── NodeMonitor
    ├── HealthChecker
    └── MetricsCollector (CPU/RAM/Disk)
```

---

### 8. Security Layer — لایه امنیت

```
Security
├── PermissionManager
│   ├── AgentPermissionProfile
│   └── ToolAccessControl
├── ActionApproval
│   ├── SensitivityClassifier
│   ├── ApprovalNotification
│   └── AuditLogger
├── SecretVault
│   ├── KeystoreWrapper
│   ├── BiometricGate
│   └── EncryptedStorage
└── Sandbox
    ├── FileSystemNamespace
    └── NetworkIsolation
```

---

## جریان داده / Data Flow

### مکالمه ساده
```
کاربر تایپ می‌کند
    → VoiceEngine (اگر صوتی)
    → AgentEngine.process()
    → ReActLoop.think()
    → TokenPool.route() → Provider → Response
    → UI.display()
```

### اجرای وظیفه پیچیده
```
کاربر: «برای سرورم بکاپ بگیر»
    → IntentRouter → Task Detection
    → Orchestrator.decompose()
        ├── DeviceManagerAgent.checkSSH()
        ├── PlannerAgent.buildPlan()
        └── CoderAgent.writeScript()
    → ActionApproval.request() ← کاربر تأیید می‌کند
    → SSHTool.execute()
    → AuditLogger.log()
    → UI.showResult()
```

---

## ماژول‌بندی Android / Android Module Structure

```
:app                    → Activity، Navigation، DI
:ui-hud                 → HUD، Glitch، Haze، ArcReactor
:ai-core                → TokenPool، Providers
:agent                  → AgentEngine، Tools، ReAct
:voice                  → Vosk، Piper، WakeWord
:memory                 → SQLite، VectorDB، RAG
:security               → Vault، Permissions، Audit
:skills:file            → File Manager
:skills:app             → App Manager
:skills:server          → SSH، Docker، VPS
:skills:message         → SMS، Telegram
:skills:coding          → Terminal، Git، Editor
:mesh                   → Node discovery، control
:common                 → Shared models، utils
```

---

## قرارداد نام‌گذاری / Naming Conventions

```kotlin
// Composables: PascalCase
@Composable fun ArcReactorCore()
@Composable fun HolographicPanel()

// ViewModels: [Screen]ViewModel
class HomeViewModel : ViewModel()
class ChatViewModel : ViewModel()

// Repositories: [Domain]Repository
class AgentRepository
class TokenRepository

// Use Cases: [Action][Domain]UseCase
class ExecuteAgentTaskUseCase
class RotateTokenUseCase
```

---

## تصمیمات معماری / Architecture Decisions

| تصمیم | گزینه انتخابی | دلیل |
|--------|--------------|------|
| UI Framework | Jetpack Compose | مدرن، انیمیشن قوی، RTL خوب |
| Blur Effect | Haze 2.0 | GPU-native، بهترین performance |
| GPU Shaders | AGSL | بومی Android 13+، نه OpenGL |
| STT | Vosk | آفلاین، فارسی قوی |
| TTS | Piper | آفلاین، طبیعی |
| Local LLM | Ollama + MLC | انعطاف + سرعت |
| VectorDB | Chroma (local) | ساده، بدون وابستگی خارجی |
| DI | Hilt | استاندارد Android |
| Storage | SQLite + DataStore | ساده + امن |
| Security | Android Keystore | بومی و قوی |
| Activation | kiancdn bot + HMAC token | کنترل دسترسی، ضد سوءاستفاده |

---

*آخرین به‌روزرسانی: ژوئن ۲۰۲۶*
