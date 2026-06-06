# 🗺️ ROADMAP — Vision Agent OS
### نسخه v5.1 · CyberDeck Edition

<div align="center">

**آخرین به‌روزرسانی:** ژوئن ۲۰۲۶ · **وضعیت:** 🔨 توسعه فعال · **لایسنس:** Source-Available انحصاری (نه اوپن‌سورس)

</div>

---

## 📊 نمای کلی فازها / Phase Overview

| # | فاز | موضوع | وضعیت | زمان تخمینی | اولویت |
|---|-----|--------|--------|-------------|--------|
| 0 | [Phase 0](#-phase-0--پایه-تکمیل‌شده-) | پایه تکمیل‌شده | ✅ Done | — | — |
| 1 | [Phase 1](#-phase-1--اتصال--زیرساخت-7-10-روز) | اتصال و زیرساخت | 🔨 Active | ۷–۱۰ روز | 🔴 بحرانی |
| 2 | [Phase 2](#-phase-2--multi-api--token-pool-10-14-روز) | Multi-API & Token Pool | 🔨 Active | ۱۰–۱۴ روز | 🔴 بحرانی |
| 3 | [Phase 3](#-phase-3--cyberpunk-hud-3-4-هفته) | Cyberpunk HUD | 📋 Planned | ۳–۴ هفته | 🔴 بحرانی |
| 4 | [Phase 4](#-phase-4--voice-engine-2-3-هفته) | Voice Engine | 📋 Planned | ۲–۳ هفته | 🟠 زیاد |
| 5 | [Phase 5](#-phase-5--agent-engine-3-4-هفته) | Agent Engine | 📋 Planned | ۳–۴ هفته | 🟠 زیاد |
| 6 | [Phase 6](#-phase-6--management-skills-4-هفته) | Management Skills | 📋 Planned | ۴ هفته | 🟠 زیاد |
| 7 | [Phase 7](#-phase-7--cyberdeck-mode--living-persona-4-6-هفته) | CyberDeck + Living Persona | 📋 Planned | ۴–۶ هفته | 🟡 متوسط |
| 8 | [Phase 8](#-phase-8--memory-system--rag) | Memory System & RAG | 📋 Planned | ۳ هفته | 🟡 متوسط |
| 9 | [Phase 9](#-phase-9--security--privacy) | Security & Privacy | 📋 Planned | ۲–۳ هفته | 🔴 بحرانی |
| 10 | [Phase 10](#-phase-10--on-device-ai) | On-Device AI | 🔮 Future | — | 🟡 متوسط |
| 11 | [Phase 11](#-phase-11--automation-engine) | Automation Engine | 🔮 Future | — | 🟡 متوسط |
| 12 | [Phase 12](#-phase-12--marketplace--sdk) | Marketplace & SDK | 🔮 Future | — | 🟢 پایین |
| 13 | [Phase 13](#-phase-13--multi-device--sync) | Multi-Device & Sync | 🔮 Future | — | 🟢 پایین |
| 14 | [Phase 14](#-phase-14--vision-os-mode-هدف-نهایی) | Vision OS Mode | 🔮 Future | — | 🟢 پایین |

---

## ✅ Phase 0 — پایه تکمیل‌شده ✅

> بر اساس پروژه Jarvis-android

- [x] Brain Server (FastAPI + LiteLLM + WebSocket + Dashboard + PostgreSQL/Redis)
- [x] Node Agent + Mesh Network (چند Node، auto-discovery)
- [x] Launcher Base اولیه (HomeActivity، App Drawer، Chat پایه)
- [x] CI/CD اولیه + ساخت APK خودکار
- [x] مستندات پایه (CHANGELOG، CONTRIBUTING، SECURITY)
- [x] تم Dark اولیه با Jetpack Compose

---

## 🔑 Phase A — Activation & Licensing (kiancdn) — موازی با Phase 1

> **هدف:** کنترل دسترسی کاربر نهایی و جلوگیری از سوءاستفاده از طریق فعال‌سازی توکن‌محور.

```
کاربر → ربات تلگرام «kiancdn» (@kian_irani_cdn_f)
      → /start → احراز هویت تلگرام → صدور توکن امضاشده (HMAC)
      → کاربر توکن را در اپ وارد می‌کند
      → اپ توکن را به Activation Service می‌فرستد → تأیید/رد
      → فعال‌سازی + ذخیره امن در Android Keystore
```

- [ ] **Activation Service** (FastAPI): endpoint صدور و اعتبارسنجی توکن
- [ ] **kiancdn Bot Integration**: دستور صدور توکن، نگاشت کاربر تلگرام ↔ توکن
- [ ] توکن امضاشده (HMAC/JWT)، انقضا، rate-limit، ابطال
- [ ] اپ: صفحه‌ی ورود توکن + ذخیره در Keystore + بررسی دوره‌ای اعتبار
- [ ] حالت آفلاین مجاز با grace period
- [ ] داشبورد مدیریت توکن‌ها (صادر/فعال/ابطال‌شده)
- [ ] لاگ و ضدتقلب (تشخیص اشتراک‌گذاری توکن)

> طراحی کامل: [docs/ACTIVATION.md](docs/ACTIVATION.md)

---

## 🔨 Phase 1 — اتصال & زیرساخت (۷–۱۰ روز)

> **هدف:** پایه پایدار برای ارتباط Launcher ↔ Brain Server ↔ Mesh

### 🔴 Blocker اصلی
- [ ] **رفع `/ws/mobile` WebSocket 404 پشت Caddy**
  - بررسی headers، matcher و timeout در Caddy config
  - تست اتصال کامل Launcher → Brain Server

### اتصال و شبکه
- [ ] Mesh Status Panel کامل در HUD (لیست Nodes + CPU/RAM/Disk + Ping)
- [ ] Remote Terminal (SSH-like از Launcher به Nodes با sandbox)
- [ ] مدیریت Permissions + بهینه‌سازی باتری (Foreground Service، Doze Mode)
- [ ] Backup/Restore تنظیمات

### چندزبانه
- [ ] پشتیبانی کامل فارسی/انگلیسی + RTL layout
- [ ] رشته‌های قابل ترجمه برای community

### CI/CD
- [ ] GitHub Actions بهبودیافته (دستی + خودکار)
- [ ] Release Notes خودکار
- [ ] F-Droid metadata آماده‌سازی

---

## 🔨 Phase 2 — Multi-API & Token Pool (۱۰–۱۴ روز)

> **هدف:** سیستم AI هوشمند، انعطاف‌پذیر و مقرون‌به‌صرفه

### Provider System
- [ ] **Anthropic** (Claude Sonnet، Haiku، Opus)
- [ ] **OpenAI** (GPT-4o، o3-mini)
- [ ] **Google Gemini** (2.5 Pro، Flash)
- [ ] **DeepSeek** (V3، R1)
- [ ] **Groq** (سرعت بالا، کم‌تأخیر)
- [ ] **OpenRouter** (Gateway برای ۱۰۰+ مدل)
- [ ] **Ollama** (Local LLM)
- [ ] **MLC-LLM** (On-Device inference)
- [ ] **Local GGUF** (مدل‌های سفارشی)
- [ ] رابط افزودن Provider سفارشی

### Token Pool Manager
```
Token Pool Architecture:
┌─────────────────────────────┐
│  Priority Router            │
│  Provider A (Priority 1) ──►├──► AI Request
│  Provider B (Priority 2) ──►│
│  Provider C (Fallback)   ──►│
│  Local Model (Emergency) ──►│
└─────────────────────────────┘
     │           │
  Failover    Budget
  (Rate Limit) Control
```
- [ ] Priority Routing (ترتیب اولویت قابل تنظیم)
- [ ] Auto Failover (سوئیچ خودکار هنگام Rate Limit یا خطا)
- [ ] Token Rotation (چرخش بین چند کلید یک Provider)
- [ ] Cost Tracking (ردیابی هزینه per-request)
- [ ] Budget Control (سقف روزانه/ماهانه با هشدار)
- [ ] Smart Rate-limit Handling
- [ ] Fallback خودکار به مدل محلی

### امنیت ذخیره‌سازی
- [ ] **Android Keystore** برای رمزنگاری API Keys
- [ ] **EncryptedDataStore** برای تنظیمات حساس
- [ ] احراز هویت بیومتریک برای دسترسی به Keys

### داشبورد مدیریت
- [ ] رابط افزودن/حذف/ویرایش Token
- [ ] نمودار مصرف و هزینه
- [ ] وضعیت لحظه‌ای هر Provider
- [ ] تاریخچه خطاها و Failoverها

---

## 🎨 Phase 3 — Cyberpunk HUD (۳–۴ هفته)

> **هدف:** جذاب‌ترین و منحصربه‌فردترین رابط کاربری موبایل — قلب بصری پروژه

### Core UI Components
```kotlin
// کامپوننت‌های قابل استفاده مجدد
HoloPanel()          // پنل شیشه‌ای شناور
GlassCard()          // کارت Glassmorphism
NeonText()           // متن با glow نئونی
NeonButton()         // دکمه با hover effect
PulsingArcReactor()  // قلب مرکزی HUD
DataStreamBackground() // پس‌زمینه data rain
```

- [ ] **Haze 2.0** — Glassmorphism بومی (blur + tint + noise)
- [ ] **`Modifier.glitchEffect()`** — RGB split، scanlines، color overlay، شدت متغیر
- [ ] **AGSL Shaders** — افکت CRT/VHS/Noise مستقیم روی GPU
- [ ] **Particle Field** — ذرات واکنش‌دهنده به صدا و لمس
- [ ] **Data Stream** — بارش data در پس‌زمینه

### Arc Reactor Core (المان مرکزی)
- [ ] Ring چندلایه با انیمیشن pulsing
- [ ] نمایش وضعیت باتری (رنگ + سرعت pulse)
- [ ] نمایش وضعیت Mesh (تعداد nodes فعال)
- [ ] نمایش اطمینان AI (confidence indicator)
- [ ] واکنش به صدا (بزرگ‌تر/کوچک‌تر + رنگ)
- [ ] واکنش به وضعیت AI (در حال تفکر / پاسخ / خطا)

### پنل‌های Holographic
- [ ] پنل آب‌وهوا (floating، قابل جابجایی)
- [ ] پنل تقویم/وظایف
- [ ] پنل پیشنهادات AI (contextual)
- [ ] پنل ابزارهای سریع
- [ ] پنل وضعیت Mesh و Nodes
- [ ] انیمیشن ورود/خروج پنل‌ها (morph + glitch)

### سیستم Audio-Reactive (کامل)
- [ ] **Waveform لحظه‌ای** (MediaRecorder input)
- [ ] **FFT Frequency Bars** با Visualizer API
  - Bass (20-300Hz) → پالس Arc Reactor
  - Mid (300-3kHz) → درخشش HoloPanel
  - Treble (3-20kHz) → Particle glow
- [ ] **Spatial Audio Reactivity**
  - جهت‌یابی صدا (azimuth/elevation)
  - Glow جهت‌دار روی لبه‌های صفحه
  - جریان Particle از منبع صدا
- [ ] Glitch trigger هنگام تأکید صوتی

### Spatial Reactivity (Gyroscope)
- [ ] Parallax effect روی پنل‌ها با کج کردن گوشی
- [ ] Perspective shift لایه‌های HUD
- [ ] Tilt-reactive glow

### تم‌ها
- [ ] **Night City** (پیش‌فرض) — Cyan/Magenta، Dark Deep
- [ ] **Iron Vision** — Gold/Red، metallic
- [ ] **Neon Hacker** — Green/White، matrix-style
- [ ] **Dream Ambient** — Pastel، soft glow
- [ ] **Minimal Glass** — Monochrome، ultra-clean
- [ ] **AI Theme Generator** — تولید تم با توصیف متنی

### App Drawer و Navigation
- [ ] App Drawer هولوگرافیک با انیمیشن ورود
- [ ] **Universal Search** با LLM-power (جستجوی معنایی)
- [ ] Gesture Navigation کامل (swipe، pinch، tap)
- [ ] Shared Element Transitions
- [ ] Lottie Animations برای اقدامات مهم

---

## 🎙️ Phase 4 — Voice Engine (۲–۳ هفته)

> **هدف:** تعامل صوتی طبیعی، پایدار و کم‌مصرف

### Wake Word
- [ ] «هی ویژن» / "Hey Vision" — آفلاین با Vosk
- [ ] آموزش Wake Word سفارشی
- [ ] حساسیت قابل تنظیم
- [ ] حالت Always-Listening کم‌مصرف (VAD + Doze-aware)

### Speech-to-Text
- [ ] **Vosk** فارسی قوی (مدل بزرگ آفلاین)
- [ ] **Vosk** انگلیسی
- [ ] پشتیبانی از مکالمه دوزبانه فارسی/انگلیسی
- [ ] Push-to-Talk (آلترناتیو)
- [ ] Noise Cancellation

### Text-to-Speech
- [ ] **Piper** TTS با چند صدا فارسی
- [ ] Emotional Prosody (لحن متناسب با محتوا)
- [ ] سرعت و pitch قابل تنظیم
- [ ] انیمیشن لب‌خوانی Arc Reactor هنگام صحبت

### Voice HUD
- [ ] Waveform زنده هنگام گوش دادن
- [ ] Spatial glow جهت‌دار (از کجا صحبت می‌شود)
- [ ] زیرنویس هولوگرافیک (متن آنچه گفته می‌شود)
- [ ] Glitch effect هنگام تأکید

### Intent Routing
- [ ] Rule-based intent تشخیص سریع
- [ ] LLM-based routing برای موارد پیچیده
- [ ] Fallback graceful
- [ ] تأیید صوتی اقدامات مهم

---

## ⚙️ Phase 5 — Agent Engine (۳–۴ هفته)

> **هدف:** تبدیل AI از چت‌بات به عامل عملیاتی واقعی

### ReAct Architecture
```
کاربر: «برای سرورم بکاپ بگیر»
    ↓
[Thought] چه ابزاری نیاز است؟ → SSH + Backup Script
    ↓
[Action] ssh_connect("server01") → اجرا
    ↓
[Observation] connected, disk 85% → نتیجه
    ↓
[Action] run_backup() → اجرا
    ↓
[Result] ✅ بکاپ کامل شد، ۲.۳GB ذخیره
```

- [ ] Thought/Action/Observation loop
- [ ] نمایش مراحل تفکر برای کاربر (شفافیت)
- [ ] توقف/رد عملیات در هر مرحله
- [ ] لاگ کامل با timestamp

### Multi-Agent System
| Agent | مسئولیت |
|-------|----------|
| **Orchestrator** | هماهنگی و تقسیم وظایف بین Agentها |
| **Planner** | تجزیه وظایف پیچیده به مراحل کوچک |
| **Coder** | نوشتن، debug، توضیح و review کد |
| **Researcher** | جستجو، خواندن و خلاصه‌سازی اطلاعات |
| **Writer** | نوشتن محتوا، ایمیل، پست، گزارش |
| **DeviceManager** | مدیریت تنظیمات و سیستم گوشی |

- [ ] Parallel Agent execution (وظایف موازی)
- [ ] Agent-to-Agent communication
- [ ] نمایش گراف عملیات (بصری)

### Tool System (Sandboxed)
```
ابزارهای Agent:
├── 📁 FileSystem  (read/write/search/move)
├── 🖥️ Shell       (bash، محدود به sandbox)
├── 🌐 WebSearch   (جستجوی اینترنت)
├── 🔒 SSH         (اتصال به سرورها)
├── 🐳 Docker      (مدیریت container)
├── 🐙 Git/GitHub  (عملیات repository)
├── 💬 SMS         (ارسال/دریافت)
├── 📷 Camera      (عکس/ویدیو)
├── 📲 AppControl  (باز/بستن اپ)
├── ⚙️ Settings    (تنظیمات گوشی)
└── 🏗️ APKBuilder  (ساخت اپ اندروید)
```

- [ ] Permission prompt برای هر Tool
- [ ] Sandbox isolation
- [ ] Rate limiting برای Tools حساس
- [ ] ثبت لاگ هر Tool call

---

## 📦 Phase 6 — Management Skills (۴ هفته)

> **هدف:** ماژول‌های مدیریتی کامل، تقویت‌شده با AI

### File Manager (AI-Powered)
- [ ] مرور حافظه داخلی + SD Card
- [ ] **AI Search**: جستجوی معنایی ("عکس‌های سفر پارسال")
- [ ] **AI Tagging**: برچسب‌گذاری خودکار فایل‌ها
- [ ] پیش‌نمایش تصویر، ویدیو، صدا، PDF، کد
- [ ] انتقال Cross-Node (بین دستگاه‌های Mesh)
- [ ] Compress/Extract
- [ ] اشتراک‌گذاری سریع

### App Manager
- [ ] نصب/حذف/آپدیت
- [ ] کنترل Permissions
- [ ] آمار مصرف (زمان، باتری، شبکه)
- [ ] Hidden Apps
- [ ] App Backup/Restore
- [ ] **AI Automation**: «اگر این اپ رو ۷ روز باز نکردم، پیشنهاد حذف بده»

### Server/Node Manager
- [ ] اتصال SSH با Key و رمز
- [ ] ذخیره پروفایل سرورها
- [ ] Terminal تعاملی
- [ ] **SFTP** با File Manager یکپارچه
- [ ] **Docker**: مشاهده/شروع/توقف Containers
- [ ] **VPS Monitoring**: CPU، RAM، Disk، Network
- [ ] **هشدارهای هوشمند** (سرور داره پر می‌شه)
- [ ] V2Ray/Xray Status Monitoring
- [ ] Kubernetes پایه (Pods، Services)

### Message Manager
- [ ] SMS: خواندن، نوشتن، فیلتر
- [ ] Notifications: مدیریت یکپارچه
- [ ] **Telegram** (via Bot API): خواندن، خلاصه‌سازی، پاسخ AI
- [ ] **AI Reply Suggestion**: پیشنهاد پاسخ هوشمند
- [ ] فیلتر هوشمند (مهم/غیرمهم)

### Coding Workspace
- [ ] Code Editor با Syntax Highlighting (Kotlin، Python، JS، Go، Bash)
- [ ] Code Completion پایه
- [ ] Terminal (مبتنی بر Termux) با Tab‌های چندگانه
- [ ] Git: Clone، Commit، Push، Pull، Diff، Branch
- [ ] GitHub Integration: Issues، PR، Code Review با AI
- [ ] APK Builder (Gradle wrapper)
- [ ] **AI Pair Programmer**: کمک به نوشتن و debug

---

## 🌟 Phase 7 — CyberDeck Mode & Living Persona (۴–۶ هفته)

> **هدف:** المان‌های منحصربه‌فرد که Vision را از همه متمایز می‌کند

### CyberDeck Mode
- [ ] ورود با gesture یا دستور صوتی
- [ ] تمام‌صفحه Holographic Terminal
- [ ] Glitch transition هنگام ورود/خروج
- [ ] AI Pair Programmer در terminal
- [ ] همه ابزارها (SSH، Git، Docker) در یک محیط
- [ ] Split-screen (Terminal + AI Chat)

### Living Persona (Echo)
```
Echo = شخصیت AI پویا که:
├── سبک نوشتاری شما را یاد می‌گیرد
├── موضوعات علاقه‌مند شما را می‌شناسد
├── زمان‌بندی فعالیت شما را درک می‌کند
├── لحن خود را با حالت شما تنظیم می‌کند
└── به مرور زمان "بزرگ‌تر" و باهوش‌تر می‌شود
```
- [ ] یادگیری سبک نوشتاری کاربر
- [ ] یادگیری موضوعات علاقه‌مند
- [ ] تطبیق لحن با حالت کاربر
- [ ] پیشنهادات پیشگیرانه (Proactive)
- [ ] خلاصه روزانه/هفتگی شخصی‌سازی‌شده
- [ ] «مکالمه صبح‌گاهی» — گزارش روز پیش رو

### Ambient Dream Mode
- [ ] نمایش آرام data rain هنگام idle
- [ ] «افکار» Echo روی Ambient Display
- [ ] آمار روز (قدم‌ها، مصرف، وظایف)
- [ ] اطلاعیه‌های مهم به شکل هولوگرافیک

### Lite AR Overlays (ARCore)
- [ ] شناسایی اشیاء با دوربین
- [ ] اطلاعات هولوگرافیک روی دنیای واقعی
- [ ] «پیدا کردن اشیاء گم‌شده» (last seen location)

### Mesh Implants & Drones
- [ ] کنترل دستگاه‌های Mesh با دستور صوتی
- [ ] دریافت تصویر از دوربین Nodeهای دیگر
- [ ] اجرای وظایف موازی روی چند Node

---

## 💾 Phase 8 — Memory System & RAG (۳ هفته)

> **هدف:** حافظه بلندمدت برای تجربه واقعاً شخصی‌سازی‌شده

### حافظه ساختاریافته
- [ ] ذخیره ترجیحات کاربر (SQLite)
- [ ] ذخیره پروژه‌های فعال
- [ ] ذخیره وظایف و یادداشت‌ها
- [ ] تاریخچه مکالمات قابل جستجو
- [ ] خلاصه‌سازی خودکار مکالمات قدیمی

### Vector Database
- [ ] **Chroma** (محلی) — برای جستجوی معنایی
- [ ] **Qdrant Remote** (اختیاری) — برای مقیاس‌پذیری
- [ ] Embedding خودکار محتوای مهم
- [ ] جستجوی معنایی در تمام حافظه

### RAG (Retrieval-Augmented Generation)
- [ ] ایندکس‌گذاری خودکار مکالمات مهم
- [ ] ایندکس‌گذاری فایل‌های مهم (با اجازه کاربر)
- [ ] Context injection هوشمند در مکالمات

---

## 🔒 Phase 9 — Security & Privacy (۲–۳ هفته) — موازی با فازهای دیگر

> **هدف:** امنیت بدون فدا کردن راحتی — Agent با دسترسی زیاد = سطح حمله زیاد

### Permission Architecture
```
هر Agent داری Permission Profile:
Planner  → فقط خواندن + Tool delegation
Coder    → File R/W + Terminal + Git
Device   → Settings + SMS + Camera
Mesh     → Network + SSH
```
- [ ] Permission Profile برای هر Agent
- [ ] نمایش شفاف دسترسی‌های درخواستی
- [ ] رد/تأیید دستی دسترسی‌های جدید

### Action Approval System
- [ ] تعریف سطوح حساسیت (🔴 همیشه بپرس / 🟡 هوشمند / 🟢 خودکار)
- [ ] Notification تأیید برای اقدامات بحرانی
- [ ] Timeout (اگر تأیید نشد، لغو)
- [ ] Batch approval برای چند اقدام مشابه

### Secret Vault
- [ ] ذخیره رمزنگاری‌شده API Keys
- [ ] ذخیره رمز عبور SSH و سرورها
- [ ] **Android Keystore** + احراز هویت بیومتریک
- [ ] Import/Export امن (رمزنگاری‌شده)

### Sandbox & Isolation
- [ ] اجرای کد ناشناس در محیط ایزوله
- [ ] محدودیت شبکه برای Agentهای خاص
- [ ] File system namespace isolation

### Audit System
- [ ] لاگ کامل تمام اقدامات Agent (با timestamp)
- [ ] قابلیت جستجو و فیلتر در لاگ‌ها
- [ ] هشدار برای اقدامات غیرعادی
- [ ] گزارش هفتگی امنیتی

---

## 🧠 Phase 10 — On-Device AI (آینده)

- [ ] بهینه‌سازی Ollama برای موبایل (quantized models)
- [ ] MLC-LLM با acceleration (GPU + NPU)
- [ ] Thermal/Battery-aware inference
- [ ] Offline-first architecture کامل
- [ ] مدل‌های تخصصی سبک (coding، فارسی، task)

---

## 🔄 Phase 11 — Automation Engine (آینده)

> **هدف:** گوشی کارها را خودش انجام دهد

### Rule Builder بصری
```
تریگر → شرط → اقدام

مثال‌ها:
🔋 باتری < ۲۰٪ → پاورسیو فعال
📨 پیام Telegram از X → Agent تحلیل کند
🕐 هر شب ۲ بامداد → بکاپ سرور
📶 WiFi وصل شد → sync فایل‌ها
📍 رسیدن به خانه → حالت خانه فعال
```
- [ ] رابط بصری IF/THEN/ELSE (drag & drop)
- [ ] تریگرهای زمانی (Cron-like)
- [ ] تریگرهای سیستمی (باتری، WiFi، مکان)
- [ ] تریگرهای پیام (SMS، Telegram)
- [ ] تریگرهای فایل (ایجاد/تغییر)
- [ ] اکشن‌های آماده (۲۰+ اکشن)
- [ ] LLM-triggered automation («وقتی یه ایمیل مهم رسید...»)

---

## 🛒 Phase 12 — Marketplace & SDK (آینده)

### Skill Store
- [ ] رابط مرور، جستجو و نصب Skill
- [ ] رتبه‌بندی، نظرات، دسته‌بندی
- [ ] بروزرسانی خودکار

### Skill‌های پیش‌بینی‌شده
- 🖥️ VPS Manager Pro
- 📨 Telegram Channel Manager
- 📥 YouTube Downloader
- 🐙 GitHub Assistant
- 🔒 V2Ray/Xray Full Manager
- ☁️ Cloudflare Assistant
- 📅 Smart Calendar
- 💰 Crypto Portfolio
- 🏠 Home Assistant Bridge
- 🌦️ Weather Intelligence

### Developer SDK
- [ ] مستندات ساخت Skill
- [ ] CLI ابزار scaffold
- [ ] Emulator برای تست Skill
- [ ] فرآیند Review برای Publish

### Persona & Theme Marketplace
- [ ] خرید/دانلود تم‌های جامعه
- [ ] پرسونالیتی‌های سفارشی Echo

---

## 🔗 Phase 13 — Multi-Device & Sync (آینده)

- [ ] Layout تطبیقی برای Tablet و Desktop
- [ ] Sync وضعیت از طریق Mesh
- [ ] Web Client برای Brain Server
- [ ] یکپارچه‌سازی با Home Assistant، IFTTT

---

## 🌌 Phase 14 — Vision OS Mode (هدف نهایی)

> **همان جهتی که صنعت AI در ۲۰۲۶ به سمت آن حرکت می‌کند**

```
کاربر می‌گوید:       Vision انجام می‌دهد:
─────────────────    ──────────────────────────────────
«برای سرورم بکاپ بگیر»  → SSH → Backup → تأیید → اجرا
«این ریپو رو clone کن»  → Git → Clone → تحلیل → گزارش
«برای کانالم پست بنویس» → Researcher → Writer → تأیید → ارسال
«تنظیماتم رو بهینه کن» → DeviceAgent → آنالیز → اعمال تغییر
```

- [ ] تبدیل به Default Launcher دستگاه
- [ ] Agent-First Navigation (نه App-First)
- [ ] یکپارچه‌سازی سطح سیستم (Accessibility Service)
- [ ] جایگزینی تدریجی Navigation Bar
- [ ] Proactive AI (بدون درخواست پیشنهاد می‌دهد)

---

## 📅 جدول زمانی پیشنهادی

```
ماه ۱      : Phase 1 + Phase 2 (زیرساخت + AI Core)
ماه ۲      : Phase 3 (HUD Cyberpunk — پروتوتایپ)
ماه ۲-۳   : Phase 4 (Voice Engine)
ماه ۳      : Phase 3 (HUD کامل) + Phase 9 (Security — موازی)
ماه ۴      : Phase 5 (Agent Engine)
ماه ۴-۵   : Phase 6 (Management Skills)
ماه ۵-۶   : Phase 7 (CyberDeck + Living Persona)
ماه ۶      : Phase 8 (Memory + RAG)
ماه ۷+     : Phase 10-14 (بر اساس بازخورد جامعه)
```

---

## 🔴 Blockerهای فعلی

| # | مشکل | اثر | راه‌حل |
|---|------|-----|--------|
| 1 | **WebSocket `/ws/mobile` — 404 پشت Caddy** | Launcher نمی‌تواند به Brain وصل شود | بررسی Caddy config، headers، timeout |
| 2 | HUD هنوز پایه است | بازدارنده جذب contributor | شروع Phase 3 موازی |

---

## 🚀 اولویت‌های فوری (هفته جاری)

```
1. 🔧 رفع WebSocket blocker  
2. 🤖 شروع Token Pool Manager
3. 🎨 Prototype HUD: Haze + Glitch + Arc Reactor + Waveform
4. 📝 بروزرسانی مستندات در repo
```

---

## 👥 راهنمای مشارکت

**اولویت‌های جذب contributor:**

| حوزه | مهارت مورد نیاز | وضعیت |
|------|-----------------|--------|
| 🎨 HUD/UI | Kotlin + Compose + AGSL | 🔴 فوری |
| 🤖 AI Integration | Python + LiteLLM | 🔴 فوری |
| 🔒 Security | Android Security + Crypto | 🟠 زیاد |
| 🎙️ Voice | ML + Vosk + Piper | 🟠 زیاد |
| 📁 File Manager | Android Storage API | 🟡 متوسط |

هر فاز → GitHub Issues + Project Board مخصوص  
هر هفته → PROGRESS.md + screenshot/video  
تست روی: دستگاه mid-range، flagship، tablet + emulator

---

## 📌 یادداشت‌های معماری

```
ماژول‌بندی:
:app              → Activity اصلی + Navigation
:ui-hud           → همه کامپوننت‌های HUD و Cyberpunk
:ai-core          → Multi-Provider + Token Pool
:agent            → Agent Engine + Tools
:voice            → Vosk + Piper + Wake Word
:skills:file      → File Manager
:skills:server    → SSH + Docker + VPS
:skills:message   → SMS + Telegram
:skills:coding    → Terminal + Git + Editor
:brain-server     → FastAPI backend (Python)
:node-agent       → Mesh node (Python)
```

---

<div align="center">

*این Roadmap یک سند زنده است — بر اساس بازخورد جامعه بروز می‌شود.*

**هدف نهایی: قدرتمندترین CyberDeck Personal Agent OS — source-available و فعال‌سازی‌محور.**  
**Let's build the most immersive agent OS ever seen.** 🔥🌀

</div>
