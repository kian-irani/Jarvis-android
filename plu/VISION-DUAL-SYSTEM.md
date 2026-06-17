# VISION OS — Dual System (Widget + Launcher + Brain)

> Design + target architecture for the dual-product evolution of Vision OS.
> **Grounded in the real codebase as of v54** — this is an *evolution map*, not a
> greenfield rewrite. Where a part already exists it is named with its real package;
> where it is new it is marked **NEW**. Implementation-focused; real Android
> constraints (overlay limits, foreground services, battery, Doze).

Package today: `com.kianirani.jarvis` (Android, Compose + Hilt + Room + Ktor).

---

## 0. What already exists (so we don't rebuild it)

| Dual-System concept | Already in repo | Status |
|---|---|---|
| Vision Brain (cognitive router) | `router/orchestrator/VisionOrchestrator`, `router/backend/BackendRouter` (+ Cloud/Local/Mesh), `router/substitution`, `router/cost`, `router/health`, `data/ai/TokenPool` | ✅ VB1–VB9 shipped |
| Task/agent engine | `core/agent/AgentEngine`, `ToolCaller`, `core/planner/TaskPlanner` | ✅ CF1–CF3 |
| Memory (short + long term) | `data/ai/ChatHistoryStore` (6-turn) + **`core/memory/MemoryEngine`** (semantic, CF4) over `brain/data/MemoryRepository` (MiniLM + cosine in Room) | ✅ CF4.1–4.2 (v53/54) |
| Voice I/O (STT/TTS, code-switch, neural) | `voice/VoiceController`, `VoiceSegmenter`, `EdgeTtsClient` | ✅ FV1–FV6 |
| Launcher (Android) | `data/launcher/*`, `ui/screen/workspace/WorkspaceScreen` (grid/pages/folders/drag) | ✅ LR1–LR11 |
| Device actions / context | `core/agent/CommandInterpreter`, `service/VisionAccessibilityService`, `service/VisionNotificationService` | ✅ CF2 + services |
| Brain server + mesh | `brain/server/KtorServer` (port 7799), election, heartbeat, mDNS discovery, Brain-Lite endpoints | ✅ Mesh M0 |
| Floating overlay ("Widget") | planned **PAO** (PRD Part 8.1) | ⛔ **NEW** |
| Windows shell | — | ⛔ **NEW** |

**Conclusion:** the "Vision Brain (Core System)" of this spec is ~80% built. The two
genuinely new products are the **Floating Widget** and the **Windows shell**. The
work is *productizing + cross-platform*, not greenfield.

---

## 1. System architecture (text diagram)

```
                         ┌──────────────────────────────────────────┐
                         │            VISION BRAIN (core)            │
                         │  Kotlin, shared (KMP target → :brain-core)│
                         │                                          │
   ┌────────────┐  in-proc│  Orchestrator → CapabilityRouter         │
   │  Widget    │────────▶│  BackendRouter ─┬─ Local (ONNX/llama)    │
   │ (overlay)  │◀────────│                 ├─ Cloud (providers)     │
   └────────────┘  Flow   │                 └─ Mesh (peer Brain)     │
   ┌────────────┐  in-proc│  AgentEngine → ToolCaller → Interpreter  │
   │  Launcher  │────────▶│  MemoryEngine (short + long term)        │
   │  (home)    │◀────────│  ContextEngine (current app/notif/time)  │
   └────────────┘  Flow   │  EventBus · TaskScheduler (WorkManager)  │
                         │  VoiceController (STT/TTS)                │
                         └──────────────┬───────────────────────────┘
                                        │  Brain-Lite API (Ktor :7799)
                          LAN / mDNS    │  {ok,data,error} envelope
                         ┌──────────────▼───────────────┐
                         │   Windows Shell (Compose MP)  │  ← cross-device peer
                         │   window mgmt · snap · panel  │
                         └───────────────────────────────┘
```

**Two planes:**
- **In-process plane (same Android app):** Widget + Launcher call the Brain core via
  direct Kotlin (`StateFlow`/suspend). No IPC, lowest latency, battery-friendly.
- **Network plane (cross-device / cross-app):** the existing **Brain-Lite Ktor
  server** is the uniform API. Windows shell + paired phones are peers discovered by
  mDNS and ranked by Brain election (already built).

---

## 2. Product 1 — Vision Widget (always-on overlay)

A draggable floating orb over all apps. **NEW**, but reuses Brain + Voice + Memory.

### Real Android design
- **Hosting:** a single **foreground service** (`FloatingWidgetService`, extends the
  existing Brain-Lite service pattern) owns a `WindowManager` overlay view.
- **View:** a `ComposeView` (LifecycleOwner + SavedStateRegistry + ViewModelStore
  attached manually — overlay has no Activity) rendering `VisionOrb` with `OrbState`
  (ORB state machine, PRD Part 7).
- **Window type:** `TYPE_APPLICATION_OVERLAY`, flags
  `FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_NO_LIMITS`, `PixelFormat.TRANSLUCENT`. Tap →
  `FLAG_NOT_FOCUSABLE` cleared so the input field can take focus, then re-set.
- **States:** idle (breathing) · listening (waveform) · thinking (spin) · responding
  (sound-reactive) — driven by `VisionOrchestrator` decision + `VoiceController.isSpeaking`.
- **Mini panels** (expand on tap): Ask bar, Reminders, Notes, Search (CF SRCH),
  Quick actions (reuse `QuickActionsStore`).
- **Context awareness:** `ContextEngine` (**NEW**, PRD 8.2) reads foreground app via
  the existing `VisionAccessibilityService` (opt-in) + active notifications via
  `VisionNotificationService`, injected into the Brain prompt.
- **Battery:** low-priority ongoing notification; overlay animations gated by
  `ThemeStore.animations` (reduced-motion); wake-word (FV4) on a low-power audio path;
  collapse to a tiny dot when idle; honor Doze (no wakelocks; deferrable work →
  WorkManager).
- **Permissions:** `SYSTEM_ALERT_WINDOW` (Settings.canDrawOverlays + intent),
  `RECORD_AUDIO`, `POST_NOTIFICATIONS` (API 33+), `FOREGROUND_SERVICE` +
  `FOREGROUND_SERVICE_MICROPHONE`. A first-run permission funnel screen.

### Pseudo-code — overlay service (grounded in real classes)
```kotlin
@AndroidEntryPoint
class FloatingWidgetService : LifecycleService(), SavedStateRegistryOwner {
    @Inject lateinit var brain: VisionBrain          // facade over orchestrator+router (§4)
    @Inject lateinit var voice: VoiceController       // existing
    @Inject lateinit var context: ContextEngine       // NEW

    private lateinit var wm: WindowManager
    private lateinit var root: ComposeView
    private val orbState = MutableStateFlow(OrbState.IDLE)

    override fun onCreate() {
        super.onCreate()
        startForeground(ID, lowPriorityNotification())     // persistence
        wm = getSystemService<WindowManager>()!!
        root = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingWidgetService)
            setViewTreeSavedStateRegistryOwner(this@FloatingWidgetService)
            setContent { VisionOverlay(orbState, onAsk = ::ask, onDrag = ::move) }
        }
        wm.addView(root, overlayParams())                  // TYPE_APPLICATION_OVERLAY
        if (settings.wakeWordEnabled) wakeWord.start { ask(it) }   // FV4, low-power
    }

    private fun ask(text: String) = lifecycleScope.launch {
        orbState.value = OrbState.THINKING
        val ctx = context.snapshot()                       // current app/notif/time
        val reply = brain.handle(VisionRequest(text, ctx)) // orchestrate→route→tools→memory
        orbState.value = OrbState.RESPONDING
        voice.speak(reply.spoken)
    }

    private fun move(dx: Float, dy: Float) { params.x += dx.toInt(); params.y += dy.toInt(); wm.updateViewLayout(root, params) }
    override fun onDestroy() { runCatching { wm.removeView(root) }; super.onDestroy() }
}
```

---

## 3. Product 2 — Vision Launcher (Android + Windows)

### Android (mostly exists: LR1–LR11)
- AI-first home: page 0 = `VisionOrb` hero + command bar; swipe → `WorkspaceScreen`
  grid from `LauncherStore`. **Smart dynamic grouping** = **NEW** (cluster apps by
  usage + category via `MemoryEngine`/usage stats → suggested folders). **AI layout
  suggestions** = **NEW** (Brain proposes reflow; user accepts). Universal search =
  CF **SRCH**. Gesture nav = **LR9**. Adaptive UI = usage-driven (ContextEngine).

### Windows shell (NEW) — recommended stack: **Kotlin Multiplatform + Compose Multiplatform Desktop**
Rationale: the Brain is Kotlin. Compose-MP Desktop lets the **same `:brain-core`
module** run on Windows with a native-feeling Compose UI — one Brain, three faces.
(Alternatives: Tauri (Rust+web) if a web UI is preferred; .NET/WinUI for deepest
shell integration. We choose Compose-MP for code reuse; drop to JNA/`user32` for the
few Win32 shell hooks.)
- **Shell replacement:** a borderless always-on top bar/dock + an `Ask Vision` panel.
  True `explorer.exe` replacement is invasive — phase 1 ships a **dock + global
  hotkey panel** (not a full shell swap); phase 2 evaluates shell registration.
- **Window management / snap layouts:** Win32 via JNA (`SetWindowPos`,
  `MonitorFromWindow`, `EnumWindows`) wrapped in a `WindowManager` expect/actual.
- **Cross-device sync:** Windows runs a **Brain-Lite peer**; pairs with the phone via
  the existing **mDNS + QR + Brain election**; shares memory/clipboard/handoff over
  the Ktor API (MX track).

---

## 4. Vision Brain — API design

### A. Internal facade (NEW thin wrapper over existing router) — `:brain-core`
```kotlin
interface VisionBrain {
    suspend fun handle(req: VisionRequest): VisionResponse   // intent→route→tools→memory
    fun state(): StateFlow<BrainState>                       // idle/thinking/...
    suspend fun remember(content: String, type: MemoryType, importance: Float = .5f)
    suspend fun recall(query: String, topK: Int = 5): List<MemoryEngine.Recalled>
}
data class VisionRequest(val text: String, val context: DeviceContext, val privacy: Boolean = false)
data class VisionResponse(val spoken: String, val display: String, val action: ToolResult?, val model: String)
```
Implementation = today's `HudViewModel.sendChat` flow extracted into a reusable class:
`orchestrator.decide()` → `backendRouter.execute()` → `ToolCaller.parse()` →
`CommandInterpreter` → `MemoryEngine` (recall before, capture after).

### B. Network API (exists — Brain-Lite Ktor `:7799`, envelope `{ok,data,error}`)
```
POST /v1/chat        {message, context?}        → {reply, model, ms}
POST /v1/embed       {texts[]}                  → {vectors[][]}
GET  /v1/memory?q=&k= · POST /v1/memory {type,content,importance}
GET  /v1/nodes       · POST /v1/tasks {kind,payload} · GET /v1/election
WS   /v1/stream      token-by-token (planned)   · heartbeat (exists)
```
Used by: Windows shell, paired phones, and any separate-app Widget. Same-app Widget +
Launcher skip this and call the in-process facade.

### C. Local vs cloud separation (exists)
`VisionOrchestrator` classifies intent + privacy; `BackendRouter` picks Local
(on-device ONNX/llama.cpp — LM track) / Cloud (providers, TokenPool) / Mesh (peer).
Trust gate: SOVEREIGN (local only) · BALANCED · OPEN. Already shipped (VB1–VB9).

---

## 5. Background system

- **Services:** one `FloatingWidgetService` (overlay + wake word) + Brain-Lite server
  service (mesh). Both foreground, low-priority notifications, START_STICKY.
- **Event bus:** existing `brain/data/EventBus` — extend with `WakeWordEvent`,
  `AppForegroundEvent`, `WidgetCommandEvent`.
- **Task scheduler (NEW, CF5):** `AutomationEngine` over **WorkManager**
  (`PeriodicWorkRequest` / constraints / `ExistingWorkPolicy`). Triggers: TIME,
  CONDITION (battery/wifi), APP_OPEN, NOTIFICATION, LOCATION. Deferrable + Doze-safe.

---

## 6. Target repo structure (KMP evolution)

```
vision-os/
├── brain-core/                 ← KMP shared (NEW module; lift today's router+agent+memory)
│   └── src/commonMain/…/brain/{orchestrator,backend,agent,planner,memory,context}
│       (expect/actual for platform bits: embedder, db, audio, windowing)
├── android-app/                ← today's app/ (Compose) — Launcher + Widget entry points
│   └── …/{ui/screen/workspace (launcher), service/FloatingWidgetService (widget)}
├── desktop-app/                ← NEW Compose-Multiplatform Desktop (Windows shell)
│   └── …/{shell, windowing (JNA), panel}
├── brain-server/               ← today's brain/ Ktor (mesh + Brain-Lite API)
└── build-logic/ gradle/libs.versions.toml
```
Migration is incremental: keep shipping the Android app; extract `brain-core` as a
plain Kotlin module first (no behavior change), then KMP-enable it, then add `desktop-app`.

---

## 7. Suggested tech stack

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin 2.1 (KMP) | one Brain, all platforms |
| UI | Jetpack Compose (Android) + Compose Multiplatform (Desktop) | shared UI idiom |
| DI | Hilt (Android) · Koin (KMP/desktop) | Hilt is Android-only |
| Networking | Ktor client + server | already in use; multiplatform |
| Persistence | Room (Android) → SQLDelight (KMP) | KMP-friendly DB |
| Serialization | kotlinx.serialization | already in use |
| On-device AI | ONNX Runtime (embeddings, today) + llama.cpp NDK (gen, LM track) | offline |
| Background | WorkManager + Foreground services | Doze-safe |
| Windows shell | Compose-MP Desktop + JNA (Win32) | reuse Kotlin; shell hooks |
| Voice | Android STT/TTS + Edge neural (online) | FV stack (exists) |

---

## 8. Phased delivery (added to PLAN as the **DS — Dual System** track)

- **DS1 brain-core extraction** — lift router+agent+memory into a plain Kotlin module + `VisionBrain` facade (no behavior change). Foundation for everything.
- **DS2 Vision Widget MVP** — `FloatingWidgetService` overlay orb (drag, states, Ask bar) calling `VisionBrain` in-process. Permission funnel. (= PAO, productized.)
- **DS3 ContextEngine** — foreground-app + notification context into the Brain (PRD 8.2).
- **DS4 Widget panels** — reminders/notes/search/quick-actions.
- **DS5 Launcher AI grouping + layout suggestions** — smart folders + reflow proposals.
- **DS6 CF5 scheduler** — WorkManager automation/triggers.
- **DS7 KMP-enable brain-core** — expect/actual (embedder/db/audio); SQLDelight.
- **DS8 Windows shell MVP** — Compose-MP desktop dock + Ask panel + Brain-Lite peer pairing.
- **DS9 Window mgmt + snap (Win32/JNA)**; **DS10 cross-device sync** (memory/clipboard/handoff = MX).

> Constraints honored throughout: overlay focus juggling, single foreground service,
> battery (collapse-when-idle, WorkManager, Doze), permission funnels, offline-first
> with cloud fallback via the existing privacy/trust gate.
