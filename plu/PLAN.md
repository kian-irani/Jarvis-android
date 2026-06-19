---
title: "🗂️ PLAN — 05-vision"
tags: [project, vision, plan, active]
date: 2026-06-10
status: active
project: 05-vision
---

# 🗂️ PLAN — Vision OS
> منبع کامل: `repo/ROADMAP.md` + `repo/docs/VISION-CAPABILITIES.md` + PRDِ VCF `docs/2026-06-18-vision-cognitive-framework-PRD.md`.
> ریپو: `kian-irani/Jarvis-android` · **آخرین نسخه: v77** (2026-06-19).
> وضعیت: `[x]` انجام‌شده · `[~]` نیمه · `[ ]` باز.

---

## 🎯 اولویت‌های فعلی — به ترتیب (به‌روزرسانیِ 2026-06-18، پس از تکمیلِ VCF)

> **سکوی فعلی (کامل و منتشرشده):** **VISION BRAIN** (VB1–VB9) · **CF4 Memory** (4.1–4.3) · **سیستمِ مکالمه** (BUG-1…5) · **کلِ فریم‌ورکِ VCF** — runtime (G1–G3) · tools (T1/T3) · ReAct (A1) · ModelNode/Planner/Reflect/Steering (A2–A5) · image (M1) · team (X1–X3) · events (R2) · trace (E1) · facadeِ **VisionAgent**. ۳۶۵+ تستِ سبز. **VCF یک frameworkِ کامل و تست‌شده اما هنوز به چتِ زنده وصل‌نشده (dormant) است.**
>
> **محدودیتِ سرورِ بیلد:** نه دستگاه/امولاتور، نه شبکه‌ی مدل → کارهای «نیازِ دستگاه/شبکه» فقط build-green می‌شوند و تأییدِ رفتاری روی دستگاهِ کاربر لازم دارند.

1. **[دستگاه/شبکه] زنده‌کردنِ VCF** — ~~**VCF-T2** (function-calling بومی روی سیم)~~ ✅ v76 (`CloudChatRouter.complete` + `RouterModelClient` حالا native-FC با fallbackِ متنی؛ shaping/parse تست‌شده). باقی‌مانده: وصل‌کردنِ `VisionAgent`/`VisionGateway` به مسیرِ چت (جایگزینِ تدریجیِ `HudViewModel.sendChat`) + تأییدِ round-tripِ FC روی دستگاه. بیشترین ارزش.
2. **[دستگاه] تکمیلِ لانچر** — `LR6` dockِ پیکربندی‌پذیر + dock-drag · `LR9` ژست‌ها · `LR12` دکمه‌ی مرکزیِ Vision · `LR8` widget host → سپس **NEO7** passِ کیفیتِ بصری.
3. **[دستگاه/نیمه] صدا** — `FV4` wake-word روی foreground service (مصرف‌کننده‌ی `VisionEventBus`) · `FV6` انتزاعِ `TTSEngine`/`VoiceProvider`.
4. **[نیمه-headless] تعمیقِ VCF** — `MEM1` خلاصه‌سازی + memory-as-tools · `AgentDelegate` (هم‌سطحِ X3) · `EvalHarness` golden روی `FeedbackLog` · `R1` gateway / `R3` stream · `B1` parityِ Python.
5. **[دستگاه] Proactive/Ambient** — `PAO` overlayِ شناور · `CTX` ContextEngine · `CF5` Automation/Scheduler (روی triggerهای R2) · `ORB` state-machine · `MON` timeline.
6. **[آینده] DS Dual Experience** (Widget+Launcher+Windows، KMP) · `MX` mesh · `SRCH` AnySearch · `TWIN` · `MM` multimodal-live · `MCP/A2A`.

> **روالِ کار (auto-rp):** یک فاز را **کامل** کن → build+test سبز → tick → bump+commit+push (CI ریلیز). فازهای «نیازِ دستگاه» را build-green کن و **needs on-device** علامت بزن.
> **پاکسازیِ ساختار (2026-06-18):** بخش‌های پایین = مرجع/جزئیات. بخش ۲ (VISION BRAIN VB) و بیشترِ «FULL PROGRAM phases» در بخش ۳ **آرشیوِ انجام‌شده‌اند**؛ کارِ بازِ واقعی در همین لیستِ بالا و تیک‌نخورده‌های زیر است.

---

# بخش ۱ — جزئیاتِ تسک‌ها (backlog)

## 🗣️ PRD v2.0 — سیستم مکالمه (BUG-1…BUG-5) — انجام‌شده (v49، 2026-06-17)
> طبقِ PRD v2.0 (Part 2 — Conversation System). build سبز: `compileDebugKotlin` + `testDebugUnitTest` (**۲۲۳ تست، ۷ تستِ جدید**). منطقِ خالص با TDD، Android/UI دورِ آن. design طبقِ `ui-ux-pro-max` (truncation→expand، state-clarity، interruptible، رنگ‌تنها نه).
- [x] **BUG-1 خروجیِ بازشونده (بدونِ truncate)** — `VisionOutput` در HomeScreen: متن دیگر `maxLines=4` بریده نمی‌شود؛ collapsed تا ۱۳۲dp اسکرول، «Show more/less» برای پاسخ‌های بلند (>۲۸۰ کاراکتر). `HudUiState.isOutputExpanded` + `HudViewModel.toggleOutputExpanded` (با هر پاسخِ جدید جمع می‌شود).
- [x] **BUG-2 دکمه‌ی Stop + interrupt گفتار** — `VoiceController.isSpeaking: StateFlow` + `stopSpeaking()`؛ دکمه‌ی command-bar سه‌حالته (Mic/Send/**Stop قرمز**) با منطقِ خالصِ `commandBarMode` (۳ تست)؛ تپ روی گوی هنگامِ صحبت = قطع؛ barge-in (شروعِ شنیدن گفتار را قطع می‌کند).
- [x] **BUG-3 حافظه‌ی مکالمه + پاکسازی** — ⚠️ حافظه **از قبل** در مسیرِ واقعی وصل بود (`CloudChatRouter.chatWith`→`history.recent(6)` + appendِ دوطرف، از طریقِ `CloudBackend`)؛ ادعای PRD مبنی بر «وصل‌نبودن» منسوخ بود. بخشِ مفقود **Clear** اضافه شد: `HudViewModel.clearConversation()` (پاکِ `ChatHistoryStore` + خروجی) + کنترلِ discreetِ Clear در `VisionOutput`.
- [x] **BUG-4 صدای نورونیِ فارسی پیش‌فرض** — `VoiceRouting.useNeural(language,neuralEnabled,online)` خالص (۴ تست): فارسیِ online → نورونی حتی بدونِ توگل؛ offline → on-device؛ غیرفارسی بدونِ توگل → on-device.
- [x] **BUG-5 مرزِ پیامِ TOOL_PROTOCOL** — قانونِ مرزِ پیام در پرامپت: فقط متنِ گفتنی در args (نه کلِ جمله)، مثال‌های FA/EN، پرسشِ شفاف‌سازی هنگامِ ابهام، نگاشتِ روابطِ فارسی (مامان/بابا/خاله/…)؛ **بدونِ تغییرِ schemaِ `{"tool","args"}`** (سازگار با `ToolCaller`/`CommandInterpreter`).
> **نیازِ تأییدِ روی دستگاه (به NEO7 وصل):** ظاهرِ خروجیِ بازشونده، رفتارِ دکمه‌ی Stop/barge-in، و کیفیتِ صدای نورونیِ فارسی روی دستگاهِ واقعی.

### 🔧 v51 — رفعِ ممیزی (audit fixes، 2026-06-17)
- [x] **Fix-1 truncationِ خروجی در `HudScreen.TypewriterPanel`** — `maxLines=3`/ellipsis حذف شد (هم‌کلاسِ BUG-1، روی صفحه‌ی legacyِ HUD که در nav صدا زده نمی‌شود؛ برای جلوگیری از regression اگر دوباره wire شود). متن با `weight(1f, fill=false)` wrap می‌شود.
- [x] **Fix-2 BUG-4 به ۳-حالته** — `NeuralVoiceMode {AUTO, ON, OFF}` جایگزینِ توگلِ boolean شد: AUTO=نورونی فقط برای فارسیِ online، ON=همیشه نورونی، **OFF=همیشه on-device حتی فارسی** (رفعِ override کردنِ خواستِ کاربرِ data-conscious). migration از کلیدِ قدیمی. UI: `NeuralVoiceRow` (Auto/On/Off). `VoiceRouting.useNeural(language, mode, online)` + ۴ تستِ به‌روز.
- [x] **Fix-3 `isSpeaking` گیرکردن (v52)** — اگر TTS init شکست بخورد، `pendingSpeech` پاک و `_isSpeaking=false` می‌شود (else در init callback) تا دکمه‌ی Stop گیر نکند.
- [x] **Fix-4 تأییدِ Clear (v52)** — پاک‌کردنِ مکالمه حالا تأییدِ inlineِ دومرحله‌ای دارد («Clear chat? · Clear/Cancel»، هم‌سبکِ اپ، بدونِ AlertDialog) چون حافظه‌ی کوتاه‌مدت را پاک می‌کند.

## 🆕 PRD v2.0 — تسک‌های جدید (backlog بعد از v49)
> از `VISION OS — PRD v2.0` (Parts 3–14, 16) استخراج شد؛ تیرها به‌ترتیبِ اولویتِ PRD. موارد *(موجود)* از قبل در پلن هستند (این‌جا فقط برای ترتیب)؛ بقیه **جدید** و باز. اصول: منطقِ خالص TDD، UI با `ui-ux-pro-max`، هیچ mockup، شکستنِ کارِ سالم ممنوع.

### 🟠 HIGH — کارِ بازِ بعدی
- [x] **CF4.1 MemoryEngine — foundation** (v53، 2026-06-17): ماژولِ `core/memory/` ساخته شد روی زیرساختِ موجود (`brain/data/MemoryRepository` = MiniLM embedding + cosine در Room؛ کشفِ audit: زیرساختِ vector-memory از قبل بود، پس دوباره ساخته نشد). `MemoryType` (۷ نوع + `fromName`)، `MemoryScoring` خالص (cosine×importance×recency-decay، ۷ تست)، `PreferenceLearner` خالص (۳-strike blacklist، ۵ تست)، `MemoryEngine` (@Singleton: `remember`/`recall`/`buildContextWindow`/`learnDislike`، graceful وقتی مدلِ embedding دانلود نشده). importance در `metadata` JSON نگه داشته شد (بدونِ Room migration). `MemoryRepository.searchDetailed` افزوده شد (۱ تست). **۱۵ تستِ جدید، build سبز (۲۳۸).** [foundation — هنوز به چتِ زنده وصل نشده.]
- [x] **CF4.2 — اتصال + populate** (v54، 2026-06-17): `CloudChatRouter` حالا (۱) قبل از هر درخواست `memory.buildContextWindow(message)` را به system prompt اضافه می‌کند (guarded، "" وقتی خاطره/مدل نیست)، و (۲) بعد از پاسخِ موفق، پیامِ کاربر را به‌صورتِ fire-and-forget روی scope ذخیره می‌کند **اگر** `MemoryPolicy.worthRemembering` باشد (≥۲۰ کاراکتر و ≥۴ کلمه — نه «سلام»/کامند/URL). `MemoryPolicy` خالص + ۵ تست. `test()` دست‌نخورده (بدونِ recall/populate). **build سبز (۲۴۳).** [فعال‌سازیِ واقعی نیازِ مدلِ embedding (LM2) روی دستگاه — در sandbox no-op.]
- [x] **CF4.3 — UI + یادگیری ترجیح** ✅ (v58، 2026-06-18): صفحه‌ی Memory کامل شد — **مرور** (`MemoryEngine.browse`/`count` روی `list`ِ Room، بدونِ نیازِ مدلِ embedding)، **جستجو** (`MemoryFilter` خالصِ مستقل‌از‌مدل: زیررشته‌ی case-insensitive + فیلترِ نوع، زنده حین تایپ)، **حذف** (`forget(id)` + DAO/repo `delete`/`clear`) با **تأییدِ inlineِ دو‌مرحله‌ای** per-row (بدونِ AlertDialog) + **Clear-all** مجزا با تأیید، **چیپ‌های فیلترِ نوع** (All + انواعِ موجود)، و timestampِ نسبی (`RelativeTime` خالص). **اتصالِ PreferenceLearner:** حالا `@Singleton` تزریق‌شده در `MemoryEngine`؛ `learnDislike` از طریقِ `preferences.reject(preferenceKey)` می‌رود (تکرارِ رد → blacklist) + `isDisliked` query. UI با `ui-ux-pro-max` (destructive-emphasis/جداسازی، inline-confirm، نقطه+لیبل نه رنگ‌تنها، tap≥40–48dp، reduced-motion via `visionEnter`). صفحه از `VisionColors` (نه cyanِ legacy) استفاده می‌کند. **۱۳ تستِ جدید** (`MemoryFilterTest` ۷ + `RelativeTimeTest` ۶) · build+test سبز **۲۷۲ تست**. [ظاهرِ صفحه نیازِ تأییدِ بصری روی دستگاه (NEO7).]
- [ ] **LR6 Configurable Dock (Hotseat)** *(موجود)*: count 4/5/6 + reorder + drop به/از dock + persist؛ دکمه‌ی مرکزیِ Vision همیشه (PRD Part 6.2).
- [ ] **LR12 Vision Assistant dock button** *(موجود)*: دکمه‌ی مرکزی → دستیار (voice/text/agent/search/device).
- [ ] **FV4 Wake word** (PRD Part 3.3): «Hey Vision»/«ویژن» با Porcupine یا openWakeWord روی ForegroundServiceِ موجود (Brain-Lite)؛ بودجه‌ی <۱٪ باتری/ساعت → `EventBus` به UI.
- [ ] **FV6 Voice provider abstraction** (PRD Part 3.1): اینترفیسِ `TTSEngine` + `VoiceProvider` (Edge/Android فعلی، Piper/ElevenLabs/OpenAI آینده) — تعمیمِ معماریِ صدای v34/v41.
- [ ] **NEO7 UI quality pass** *(موجود)*: نیازِ بازخوردِ دستگاه روی v49.
- [x] **BUG-1b Markdown render** ✅ (v59، 2026-06-18): خروجیِ مدل دیگر مارک‌داون خام نشان نمی‌دهد. `core/text/MarkdownLite` خالص (parse → `MdBlock`/`MdSpan`): **bold** (`**`/`__`)، inline `` `code` ``، و لیستِ bullet (`-`/`*`) و numbered (`1.`)؛ مارکِ بی‌جفت → متنِ ادبی (هرگز throw نمی‌کند)، code بر bold اولویت دارد. رندررِ نازکِ Compose `ui/components/MarkdownText` (AnnotatedString: bold=FontWeight.Bold، code=Monospace+بک‌گراندِ ملایم؛ لیست=prefix در Row). `VisionOutput` در HomeScreen به‌جای `Text(output)` از `MarkdownText` استفاده می‌کند. **۱۱ تستِ جدید** (`MarkdownLiteTest`: bold/code/mixed/precedence/unmatched/single-star/bullet/numbered/blank-lines/empty) · build+test سبز **۲۸۳ تست**. [ظاهرِ رندر نیازِ تأییدِ بصری روی دستگاه.]

### 🟡 MEDIUM — این هفته
- [ ] **ORB State Machine** (PRD Part 7): `OrbState` (IDLE/LISTENING/THINKING/SPEAKING/EXECUTING/NOTIFICATION/ERROR/SLEEPING) + انیمیشن/رنگِ هر state در `VisionOrb` (جایگزینِ boolean `listening`؛ از `isSpeaking`/decision تغذیه شود). reduced-motion محترم.
- [x] **SAFE Safety & Trust** ✅ (v60، 2026-06-18): گیتِ trust-levelِ per-action پیاده شد — `core/agent/SafetyLayer` خالص (= مرجعِ §7.4 و trust-gateِ VCF-T3): `enum ActionRisk{AUTO,CONFIRM,CRITICAL}` + `ALWAYS_CRITICAL`(sms/message/call/email/delete/purchase/transfer/pay) و `ALWAYS_AUTO`(time/date/battery/weather/open_app/flashlight/screenshot) + `riskOf(action, declared?)` و `requiresConfirmation(...)` (always-critical بر همه مقدم؛ declared=CRITICAL حتی auto را escalate می‌کند؛ ناشناخته→CONFIRM؛ case/space-insensitive). نام `ActionRisk` (نه `TrustLevel`ِ موجودِ data.agent که سقفِ خودمختاریِ ایجنت است). **۸ تستِ جدید** (`SafetyLayerTest`) · build+test سبز **۲۹۱ تست**. `ANTI_HALLUCINATION_RULES` به‌صورتِ const اکسپوز شد. [تزریقِ زنده‌ی قوانینِ ضدِ توهم به system prompt + مصرفِ گیت توسطِ ToolNode = follow-up در VCF-T3.]
- [ ] **CTX ContextEngine** (PRD Part 8.2): app/نوتیف/ساعت/باتری/شبکه‌ی فعلی → تزریق به prompt (opt-in Accessibility).
- [ ] **PAO Floating overlay** (PRD Part 8.1): سرویسِ `SYSTEM_ALERT_WINDOW` — گویِ شناور روی همه‌ی اپ‌ها؛ tap=پنل، drag=جابه‌جایی، double-tap=voice.
- [ ] **CF5 Automation/Scheduler** (PRD Part 10): `AutomationTrigger`/`Workflow` روی WorkManager (TIME/CONDITION/APP_OPEN/NOTIFICATION/LOCATION) → `AgentEngine`.
- [ ] **SRCH AnySearch** (PRD Part 9): جستجوی معناییِ یکپارچه (MEMORY/APPS/CONTACTS/MESSAGES/FILES/WEB) با embedding + `SearchResult`.
- [~] **AGT Agent Society** (PRD Part 5): گسترشِ rosterِ `AgentId` + `AgentDelegate` (واگذاریِ agent→agent) + pipelineِ Planner→Memory→Tools→Feedback. — **`AgentDelegate` ✅ (v77، 2026-06-19):** ابزارِ delegation به سبکِ CrewAI «Delegate work to coworker» (`core/agent/AgentDelegate.kt`): rosterِ `AgentRole` را به‌صورتِ یک `VisionTool` با argهای `{coworker, task}` عرضه می‌کند؛ managerِ عامل در زمانِ اجرا coworker را **با نام** (تطبیقِ case-insensitive روی `role` یا `id`) انتخاب و sub-task را واگذار می‌کند؛ پاسخِ coworker = نتیجه‌ی ابزار. متمایز از `AgentAsTool` (یک sub-agentِ ثابت) و `Crew` (اجرای همه). runnerِ هر agent تزریقی (تولید: ReActAgentFactory+VB، تست: fake) → بدونِ مدل/شبکه؛ coworkerِ ناشناخته/argِ کم → نتیجه‌ی خطا با لیستِ نام‌های معتبر (failure-as-data، بدونِ throw)؛ `ToolNode` callId را stamp می‌کند. **۶ تستِ `AgentDelegateTest`** · build+test سبز **۴۰۲ تست**. [باقی‌ماندهٔ AGT: گسترشِ rosterِ `AgentId` + اتصالِ delegate به registryِ زنده/UI = نیازِ دستگاه.]
- [ ] **LR8 Widget Host** *(موجود)* · **LR9 Gestures** *(موجود)* · **VB9.1 health dots per-provider در UI**.

### 🟢 FUTURE
- [ ] **MM Multimodal** (PRD Part 11): فهمِ تصویر (مدلِ vision-capable: Claude/GPT-4V/Gemini) — `chatWithImage`.
- [ ] **MX1–MX4 Mesh** (PRD Part 14): تبادلِ مدل + session handoff + universal clipboard + cross-device search.
- [ ] **LM On-device inference** *(موجود)*: llama.cpp NDK.
- [ ] **TWIN Digital Twin** (PRD Part 14.2): مدلِ ماندگارِ کاربر (preferences/routines/contacts/projects/usage).
- [ ] **MCP / A2A**: اکوسیستمِ پلاگین + پروتکلِ agent-to-agent.

## 🧠 VCF — Vision Cognitive Framework — ✅ کامل (v57–v71، 2026-06-18) · باقی‌مانده‌ها needs-device/network
> **منبعِ کامل (با راهنما + نمونه‌کد): [`docs/2026-06-18-vision-cognitive-framework-PRD.md`](../docs/2026-06-18-vision-cognitive-framework-PRD.md)** · مطالعه‌ی ریپوها: [`docs/research/2026-06-18-agent-frameworks-study.md`](../docs/research/2026-06-18-agent-frameworks-study.md).
> **چرا:** برینِ *routing* ما قوی است (VB1–VB9)، اما برینِ *agentic* (CF1–CF4) ابتدایی است — `AgentEngine` فقط یک پلنِ خطیِ ثابت را راه می‌رود، `TaskPlanner` رشته را روی «then/سپس» می‌شکند، `ToolCaller` یک JSONِ regexی می‌خواند، و هیچ pipelineِ تصویر/صوت نیست. از **LangGraph** (گرافِ stateful + checkpoint + interrupt)، **AutoGen** (actor runtime + group-chat + multimodal Image)، **CrewAI** (roles + process + native-FC-with-fallback + guardrails)، **openclaw** (gateway + agent-loop hooks = trust gate + steering + voice split) یاد گرفتیم.
> **اصلِ کار:** VCF کنترلِ جریان را روی دستگاه نگه می‌دارد؛ مدلِ ابری فقط **یک node** است که از طریقِ routerِ VB صدا زده می‌شود → «خودش تصمیم می‌گیرد، ولی از توکنِ پراوایدرها استفاده می‌کند».
> **جذب می‌کند (تکراری نسازید):** `CF5`→§10 · `AGT`→§9 · `MM`→§8 · `SAFE`→§7.4 · `DS-B1/B3/B4/B5`→§6/§11/§5/§10 · `FV4/FV6`→§8.2. **VB و CF4 حفظ و مصرف می‌شوند، نه جایگزین.** ترتیب: foundation-first؛ توالی نسبت به LR/DS با کاربر است (VCF موازی با کارِ UI پیش می‌رود).

### VCF-0 — Foundation (پایه‌ی همه؛ بدونِ UI) — §4،§5
- [x] **VCF-G1 Core contracts** ✅ (v57، 2026-06-18): سه فایلِ پایه در `core/graph/` ساخته شد — `VisionMessage.kt` (`Role` + `sealed ContentPart{Text,Image,Audio,ToolCall,ToolResult}` با برابریِ محتوایی برای ByteArray + helperهای `text()/toolCalls()/hasImage()`)، `GraphState.kt` (`GraphState`/`StateUpdate`/`reduce` با کانال‌های messages=append · plan=replace · obs=append · scratch=merge · steps=bound + `ActionPlan`/`PlanStep`/`Observation`ِ سریالایزبلِ پایه)، و `GraphEvent.kt` (lifecycleِ stream). همه `@Serializable` (پیش‌نیازِ checkpointِ VCF-G3). **۱۶ تستِ جدید** (`GraphStateTest`: reduce per-channel + bound + خلوص + round-tripِ JSONِ چندوجهیِ polymorphic) · build+test سبز **۲۵۹ تست**. [foundation خالص — بدونِ runtime؛ VCF-G2 بعدی.] — اصلِ §4 برای مرجع: نمونه:
  ```kotlin
  sealed interface ContentPart { data class Text(val text:String):ContentPart
      data class Image(val bytes:ByteArray, val mime:String="image/png"):ContentPart /* + Audio,ToolCall,ToolResult */ }
  fun GraphState.reduce(u:StateUpdate) = copy(messages = messages + u.appendMessages,
      plan = u.plan ?: plan, remainingSteps = remainingSteps - if(u.spendStep)1 else 0)
  ```
  **پذیرش:** تست‌های append/replace/merge + bound. (LangGraph `graph/state.py`).
- [x] **VCF-G2 Graph runtime** ✅ (v61، 2026-06-18): runtimeِ Pregel-style پیاده شد روی VCF-G1. `Node` (fun interface) + `NodeContext(preApproved)` + `NodeResult{Continue(update,goto?),Interrupt(reason,payload)}` + `END`؛ `VisionGraph.Builder{addNode/addEdge/addConditionalEdge/setEntry/compile}` (با validationِ entry) + `next()` (conditional > static > END)؛ `CompiledGraph.stream(input,threadId,ctx): Flow<GraphEvent>` (super-step loop، reduce per node، `ensureActive()` cancellable، **bound با `remainingSteps`**، `checkpointer?.save` بعد هر step، `catch→Failed(aborted=CancellationException)`) + `resume(threadId,answer)` (HIL: answer→state، threadId→preApproved، re-enter همان node). `Checkpointer` interface تعریف شد (پیاده‌سازیِ Room = VCF-G3). **۸ تستِ جدید** (`CompiledGraphTest` با fake nodeها + in-memory checkpointer): linear/conditional/goto/**سیکلِ a→b→a که با budget تمام می‌شود**/interrupt-persist/resume-completes/throw→Failed/compile-validation. **پذیرشِ §5 کامل.** build+test سبز **۲۹۹ تست**. خالصِ JVM — بدونِ دستگاه.
- [x] **VCF-G3 Checkpointer (durable + HIL)** ✅ (v62، 2026-06-18): پیاده‌سازیِ Room برای `Checkpointer` (تعریف‌شده در G2). `CheckpointEntity{threadId,seq,cursor,stateJson,ts}` (PKِ مرکب threadId+seq) + `CheckpointDao` (insert/latest/maxSeq/all) در `VisionDatabase` (نسخه ۱→۲ با **`MIGRATION_1_2`ِ افزایشی**—جداولِ موجود دست‌نخورده). `data/graph/RoomCheckpointer`: `save` = seqِ یکنواخت + `Json.encodeToString(state)`؛ `load`=آخرین revision، `history`=همه به‌ترتیب (time-travel). در `BrainModule` provide شد؛ `CompiledGraph.resume` (از G2) حالا durableِ واقعی دارد. **۵ تستِ جدید** (`RoomCheckpointerTest` با fake DAO: seq/latest/history/round-trip JSON/threadِ ایزوله). **migration در برابرِ DDLِ تولیدیِ Room بایت‌به‌بایت تأیید شد** (نه فقط headless-guess) → schema-validation پاس می‌شود. build+test سبز **۳۰۴ تست**. [رفتارِ زنده‌ی SQLite = همان patternِ Roomِ موجود؛ schema تأییدشده.]

### VCF-1 — ReAct loop + Tools + Trust (جایگزینِ `AgentEngine`) — §6،§7
- [x] **VCF-A1 ReAct graph** ✅ (v64، 2026-06-18): `ReActAgentFactory.build(model, tools, reflect?, checkpointer?)` گرافِ ReAct را روی runtimeِ G2 می‌سازد — entry=`model`، `addConditionalEdge(model){ اگر آخرین پیام toolCall دارد→tools؛ وگرنه reflect?→reflect : END }`، `tools→model` (سیکلِ observe→reason)، reflect (اختیاری)→END. nodeهای model/tools/reflect تزریق می‌شوند (ModelNode=A2، ToolNode=T3 بعداً) → توپولوژی با fake کاملاً testable. **۴ تستِ جدید** (`ReActAgentFactoryTest`): مدلِ بی‌ابزار مستقیم جواب می‌دهد · **حلقه‌ی model→tools→model→answer** · مدلی که همیشه tool صدا می‌زند با budget **bounded** می‌شود · reflect یک‌بار بعد جواب اجرا می‌شود. build+test سبز **۳۱۵ تست**. [جایگزینیِ واقعیِ `AgentEngine` با اتصالِ A2/T3.]
- [x] **VCF-A2 ModelNode** ✅ (v69، 2026-06-18): `core/agent/ModelClient` (port: `complete(messages, toolSchema?): ModelResponse`) + `ModelNode : Node` که client را صدا می‌زند، پاسخِ assistant (متن/toolCall) را append و step خرج می‌کند، خطا/throw → پیامِ خطای گفتنی (نه crash)، modelِ پاسخ‌دهنده در scratch. **adapterِ تولیدی** `data/ai/RouterModelClient` (پلِ ModelClient→`CloudChatRouter.chat` موجود — **افزایشی، چتِ کارکنده دست‌نخورده**؛ native-FC هنوز روی سیم نمی‌رود = T2). با fake testable. ۴ تستِ `ModelNodeTest`.
- [x] **VCF-T1 Tool contract + registry** ✅ (v63، 2026-06-18): قراردادِ خالصِ ابزار در `core/tools/`. `ToolSpec(name,description,parameters:JsonObject(JSON-Schema),trust:ActionRisk)` (`@Serializable`؛ **trust = همان `ActionRisk`ِ SAFE/v60** نه enum چهارمِ جدید) + `interface VisionTool{spec; suspend execute(args,ctx):ContentPart.ToolResult}` + `ToolContext` + `ToolSpec.toOpenAiFunction()` (شکلِ FCِ OpenAI) + `ToolAllowlist`(null=همه) + `ToolRegistry{byName, permitted(), functionSchema(supportsFc):JsonArray?}` (null وقتی مدل FC ندارد → fallbackِ متنیِ T2). **۷ تستِ جدید** (`ToolRegistryTest`: byName/allowlist/schema-null/schema-shape/round-tripِ JSON). build+test سبز **۳۱۱ تست**. [مهاجرتِ ابزارهای دستگاهیِ موجود (`data/tools` — Call/Sms/… واقعی) به این interface = follow-upِ روی‌دستگاه؛ کدِ کارکنده دست‌نخورده ماند.] (نکته: KDoc حاویِ `/*` در Kotlin کامنتِ تو‌در‌تو باز می‌کند — رفع شد.)
- [x] **VCF-T2 Native function-calling + text fallback** ✅ (v76، 2026-06-19): `data/ai/FunctionCalling.kt` — کدکِ خالصِ سه‌خانواده‌ی wire (OpenAI/xAI/Groq/OpenRouter · Anthropic · Gemini): `toolsFragment` (schemaِ canonical OpenAI → `tools`/`tool_choice` · Anthropic `input_schema`+`tool_choice{auto}` · Gemini `functionDeclarations`)، `requestBody` (نگاشتِ `VisionMessage`ها شامل tool-call/tool-result به بدنه‌ی هر provider با hoistِ system)، و `parseAssistant` (پاسخ → `ContentPart.Text` + structured `ContentPart.ToolCall`؛ defensive، هرگز throw). `CloudChatRouter.complete(messages, toolSchema)` افزوده شد (provider-walk + token-pool مثلِ `chat`، `postRaw` مجزا) — **مسیرِ متنیِ `chat`/`ask` (که HUDِ زنده استفاده می‌کند) دست‌نخورده** پس chat رگرس نمی‌شود. `RouterModelClient` حالا با schema → native FC و بدونِ schema/خالی → fallbackِ متنیِ `TOOL_PROTOCOL`. **۱۶ تستِ `FunctionCallingTest`** (fragmentها/parse/body/fallback) · build+test سبز **۳۹۶ تست**. [round-tripِ روی‌سیم نیازِ تأییدِ شبکه/دستگاه؛ shaping+parse خالص و تست‌شده.] — توزیعِ per-model FC (via `ModelRegistry.supportsTools` یا modelِ انتخابیِ کاربر) = follow-up؛ همه‌ی providerهای ابریِ فعلی FC دارند.
- [x] **VCF-T3 ToolNode + Trust gate** (§7.3–7.4): اجرای موازیِ ابزارهای read-only؛ ابزارِ mutating → sequential؛ `SafetyLayer.requiresConfirmation` → `NodeResult.Interrupt("confirm_tool:…")` تا کاربر تأیید کند؛ خطا→error observation (failure-as-data). **`SafetyLayer` خالص + TDD** (= `SAFE`). نمونه:
  ```kotlin
  val needsOk = calls.firstOrNull{ safety.requiresConfirmation(registry.byName(it.name)?.spec, it) }
  if (needsOk!=null && !ctx.preApproved(needsOk.id)) return NodeResult.Interrupt("confirm_tool:${needsOk.name}", needsOk.toJson())
  ```
  **پذیرش:** ابزارِ `CRITICAL` فقط بعدِ `resume`+approval اجرا می‌شود؛ ابزارِ ناشناخته graceful. (openclaw `beforeToolCall.block`).
  - [x] **VCF-T3 ToolNode** ✅ (v65، 2026-06-18): `core/tools/ToolNode : Node` ساخته شد روی registry/T1 + `SafetyLayer`/SAFE-v60. trust-gate: اولین callِ **ثبت‌شده‌ی** ریسکی و تأییدنشده → `Interrupt("confirm_tool:<name>")` (ناشناخته‌ها از گیت رد می‌شوند و به errorObservation می‌رسند تا مدل خودش اصلاح کند، نه پرامپتِ بی‌معنی). اجرا: هر call از registry، خطا/ناشناخته → `ToolResult(isError=true)` (failure-as-data، بدونِ crash)؛ node خودش `callId` را روی نتیجه مهر می‌زند؛ خروجی = پیام‌های `Role.TOOL` + `Observation`ها. **۷ تستِ جدید** (`ToolNodeTest`: read-only اجرا · unknown→error · throw→error · CRITICAL→interrupt · نامِ always-critical حتی با declared=AUTO → interrupt · pre-approved→اجرا · بدونِ call=no-op). build+test سبز **۳۲۲ تست**. [اجرای موازیِ read-only = بهینه‌سازیِ بعدی؛ فعلاً sequentialِ درست. اتصال به ReActِ A1 با ModelNodeِ A2 واقعی.]
  - [x] **VCF-T3 اجرای موازیِ read-only** ✅ (v81، 2026-06-19): تکمیلِ بخشِ بازمانده‌ی VCF-T3. `ToolSpec.readOnly: Boolean = false` افزوده شد (متمایز از `trust`: `open_app` با trust=AUTO است ولی mutating، پس read-only **نیست**) و `RecallTool` با `readOnly=true` علامت خورد. `ToolNode.executeCalls` حالا callهای read-only را در یک step **هم‌زمان** (`coroutineScope`+`async`) اجرا می‌کند و mutatingها را **ترتیبی به‌ترتیبِ call**؛ نتایج با اندیسِ اصلی بازچینش می‌شوند پس خروجیِ پیام‌ها/observationها deterministic می‌ماند فارغ از اینکه کدام read زودتر تمام شد. ابزارهای بی‌علامت پیش‌فرض sequential (رفتارِ قبلی دست‌نخورده — ۷ تستِ قبلی سبز ماند). **۳ تستِ جدید** (`ToolNodeTest` ۷→۱۰: fan-outِ هم‌زمان با اثباتِ زمانِ مجازی ۱۰۰ms نه ۲۰۰ms · mutating ترتیبی · حفظِ ترتیبِ خروجی در میکسِ read-only+mutating) — build+test سبز **۴۲۵**. [trust-gate از v65 بود؛ این بخش بهینه‌سازیِ latencyِ یک stepِ چندابزاره است.]

### VCF-2 — Perception: تصویر + صوت (خواستِ صریحِ کاربر) — §8
- [x] **VCF-M1 Image input + per-provider encoding** ✅ (v69، 2026-06-18): `core/perception/ImageEncoding` خالص — `encode(bytes, provider, mime?)` به شکلِ هر provider (`image_url`/data-URL برای OpenAI، `source/base64` برای Anthropic، `inline_data` برای Gemini) + `sniffMime` از magic-bytes (png/jpeg/gif/webp). از `java.util.Base64` (نه android) → unit-test. ۴ تستِ `ImageEncodingTest` در برابرِ fixture. [اتصال به `CloudChatRouter.ask` برای ارسالِ واقعی = نیازِ شبکه/T2.] (AutoGen `_image.py`).
- [ ] **VCF-M2 Visual perception pipeline** (§8.1): `VisualPerception.seeScreen(prompt,screenshot)` — اگر مدلِ vision در دسترس → Image part؛ وگرنه **OCR (ML Kit) degrade** → متن. منابع: `MediaProjection`/`AccessibilityService.takeScreenshot`، camera، gallery. (نیازِ تأییدِ دستگاه.)
- [ ] **VCF-M3 Audio pipeline + STT abstraction** (§8.2 = `FV6`): `interface SpeechToText` (Android SpeechRecognizer→Vosk/Whisper→cloud)؛ `AudioPerception.listen()` = STTِ محلی‌اول → reason. جداسازیِ acquisition(محلی)/processing(agent). (openclaw voice split).
- [ ] **VCF-M4 Wake word** (§8.2 = `FV4`): Porcupine/openWakeWord روی foreground-serviceِ Brain-Lite → `VisionEvent.WakeWord` به gateway؛ بودجه‌ی <۱٪/ساعت، Doze-safe. (نیازِ دستگاه.)

### VCF-3 — Planner + Reflection + Memory deepening — §6.3،§6.4،§11
- [x] **VCF-A3 PlannerNode** ✅ (v69، 2026-06-18): `core/agent/PlannerNode : Node` — از مدل JSONِ پلن می‌خواهد، `parsePlan` (خالص، JSON را از میانِ prose استخراج و به `ActionPlan`/`PlanStep`ها تبدیل می‌کند، idِ گمشده تولید می‌شود) و در کانالِ plan (REPLACE) می‌نویسد؛ پلنِ خراب/خالی → fallbackِ تک‌مرحله‌ایِ goal. ۵ تستِ `PlannerNodeTest`. (CrewAI structured tasks).
- [x] **VCF-A4 ReflectNode** ✅ (v69، 2026-06-18): `core/agent/ReflectNode : Node` — وقتی `scratch["needsReflection"]=="true"`، از مدل نقد+اصلاحِ پاسخِ آخر را می‌خواهد، پاسخِ بهبودیافته را append و فلگ را پاک می‌کند (هرگز loop نمی‌شود)؛ بدونِ فلگ یا با خطای مدل = no-op (فلگ پاک می‌شود تا run تمام شود). ۳ تستِ `ReflectNodeTest`. (CrewAI/AutoGen reflection).
- [x] **VCF-A5 Steering** ✅ (v69، 2026-06-18): `core/agent/SteeringSource.drain()` (fun interface) + `QueueSteeringSource` (thread-safe، `QueueMode.ALL|ONE`، `push`) — پیام‌های کاربر که وسطِ run تزریق می‌شوند. ۳ تستِ `SteeringSourceTest`. [اتصال به runner/Orb UI بعداً.] (openclaw steering).
- [x] **VCF-MEM1 Summarization** ✅ (v73، 2026-06-18): `core/memory/ConversationSummarizer` — `shouldSummarize(messages)` خالص (history>threshold) + `summarize(messages)` مدل‌محور (از ModelClient، خلاصه‌ی فکت/ترجیح/تصمیمِ ماندگار) برای ذخیره به‌عنوانِ SEMANTIC memory؛ graceful (`""` وقتی لازم نیست یا مدل خطا داد). **۳ تستِ جدید** (`ConversationSummarizerTest`: زیرِ آستانه بدونِ صدا · بالای آستانه خلاصه می‌دهد · شکستِ مدل→خالی). build+test سبز **۳۷۱ تست**. + **memory-as-tools** (v74): `core/tools/MemoryTools` — `RememberTool`/`RecallTool` (تزریقِ تابعِ suspend، AUTO trust، JSON-schema params) که عامل می‌تواند وسطِ run «به خاطر بسپار/یادآوری کن» صدا بزند؛ با fake-lambda testable (۵ تست، **۳۷۶ تست**). **MEM1 کامل.** (§11)

### VCF-4 — Multi-agent Team (= `AGT`) — §9
- [x] **VCF-X1 Roles** ✅ (v66، 2026-06-18): `core/agent/AgentRole{id,role,goal,backstory,tools}` `@Serializable` + `systemPrompt()` (CrewAI role/goal/backstory). `data/agent/AgentRoles` چهار اسمِ موجودِ `AgentId` (RESEARCH/AUTOMATION/DEVELOPER/DEVICE) را به نقشِ واقعی نگاشت می‌کند — goal + persona + tool-allowlist (مثلاً DEVICE: call/send_sms/…، DEVELOPER: بدونِ ابزار) → «دادنِ رفتار به اسم‌ها». **۵ تستِ جدید** (`AgentRoleTest` systemPrompt/round-trip · `AgentRolesTest` هر AgentId نقش دارد/۴ نگاشت/ابزارهای حساس). build+test سبز **۳۲۷ تست**. [اتصال به Crew/X2 + UIِ Agents بعداً.]
- [x] **VCF-X2 Crew + Process** ✅ (v68، 2026-06-18): `core/agent/Crew.run(agents, task, Process.SEQUENTIAL|HIERARCHICAL, manager?)` + `CrewResult`. **SEQUENTIAL** = pipeline (خروجیِ هر agent به context‌ِ بعدی fold می‌شود)؛ **HIERARCHICAL** = manager به همه‌ی workerها delegate و سپس synthesize می‌کند (بدونِ manager → fail-fast). اجرای per-agent تزریقی (`runAgent: suspend (AgentRole,String)->String`؛ impl واقعی = ReActِ A1 + ModelNodeِ VB) → orchestration با fake کاملاً testable. **۴ تستِ جدید** (`CrewTest`: feed-forwardِ sequential · delegate→synthesizeِ hierarchical · بدونِ manager throw · crewِ خالی). build+test سبز **۳۳۴ تست**. **تریوِ تیمِ VCF-X کامل شد (X1+X2+X3).** [اتصالِ `runAgent` واقعی نیازِ A2 (شبکه).]
- [x] **VCF-X3 Agent-as-tool** ✅ (v67، 2026-06-18): `core/agent/AgentAsTool(sub:CompiledGraph, spec) : VisionTool` — یک sub-agentِ کامپایل‌شده را به‌عنوان ابزارِ صداکردنی نمایش می‌دهد (AutoGen agent-as-tool). `execute` آرگِ `task` را به‌عنوان پیامِ USER می‌گذارد، sub-graph را روی threadِ مجزا `stream` می‌کند، و آخرین پاسخِ assistant را برمی‌گرداند؛ sub-runِ شکست‌خورده → `ToolResult(isError)` (failure-as-data). **۳ تستِ جدید** (`AgentAsToolTest`: echo/شکست/taskِ خالی). build+test سبز **۳۳۰ تست**. [`AgentDelegate`ِ agent→agent (handoff) = follow-upِ کوتاه.]

### VCF-5 — Runtime / Gateway / Events + Eval — §10،§12
- [x] **VCF-R1 VisionGateway (broker)** ✅ (v72، 2026-06-18): `core/gateway/VisionGateway` — درِ ورودیِ واحد. `VisionRequest{text,sessionId,channel}` + `Channel{MAIN,GROUP,WIDGET,REMOTE}`؛ `submit(request): Flow<GraphEvent>` → agentِ مناسبِ channel را (`agentFor`) با threadId=sessionId اجرا می‌کند؛ `resume(sessionId,answer)` به همان channelِ ثبت‌شده مسیر می‌دهد؛ `activeSessions()`. ایزولاسیونِ session با checkpointer-thread؛ surfaceها نازک. **۳ تستِ جدید** (`VisionGatewayTest`: routingِ MAIN/GROUP · ردیابیِ session · resume به channelِ اصلی). build+test سبز **۳۶۸ تست**. [imageِ ورودی در request + اتصالِ زنده‌ی surfaceها = on-device.] (openclaw gateway).
- [x] **VCF-R2 EventBus + triggers** ✅ (v70، 2026-06-18): `core/event/` — `sealed VisionEvent`(WakeWord/AppOpened/UserIdle/Scheduled/Custom، هرکدام `kind`) + `VisionEventBus` (@Singleton، hot `SharedFlow` با buffer، `emit/tryEmit` + `subscriptionCount` برای gating) — **مجزا از `BrainEvent`ِ سرورِ Brain-Lite** + `Trigger(id,on,action,condition)` و `TriggerMatcher.matching` خالص (کدام triggerها برای یک event آتش می‌گیرند، با شرط). **۶ تستِ جدید** (`TriggerMatcherTest` ۴ + `VisionEventBusTest` ۲ با subscriptionCount-gating دترمینیستیک). build+test سبز **۳۶۲ تست**. [اتصالِ triggerها به WorkManager + اجرای گرافِ VisionAgent = on-device (CF5/DS-BG).] (CrewAI Flow `@listen`).
- [x] **VCF-R3 Streaming network plane** (§10 = `DS-C2`) ✅ (v80، 2026-06-19): WebSocketِ `/v1/stream` به Brain-Lite Ktor افزوده شد — یک surfaceِ راه‌دور/desktop می‌تواند یک turnِ چت را drive کند و آن را **افزایشی** (توکن‌به‌توکن) بگیرد، به‌علاوه‌ی relayِ زنده‌ی `BrainEvent`های ambient. چون `ChatPort` درخواست/پاسخ است (نه token-native)، پاسخ در `StreamProtocol` (خالص) به deltaهای مرتب re-tokenize می‌شود که دقیقاً به متنِ اصلی بازمی‌گردند (`tokenize(t).join == t`) — حسِ استریم همین حالا، و سوییچ به استریمِ توکنِ واقعی بعداً بدونِ تغییرِ پروتکل. فریم‌ها: `token`/`done`/`event`/`pong`/`error` (هرگز crash؛ JSONِ خراب→error frame). پلاگینِ `WebSockets` در `installBrainPlugins` + mount در `KtorServer`؛ دپندنسیِ `ktor-server-websockets` به catalog/build افزوده شد. **۷ تستِ جدیدِ `StreamRoutesTest`** (tokenize lossless/leading-ws/Persian · resolveMessages · stream token→done via `testApplication`+client-WS · ping→pong · malformed→error · empty→VALIDATION · event-relay)، build+test سبز **۴۲۲ تست**. [اتصالِ زنده‌ی surfaceهای راه‌دور/desktop + استریمِ توکنِ واقعی از مدل = follow-up.] (DS-C2 network plane).
- [x] **VCF-E1 Trace (RunTrace)** ✅ (v71، 2026-06-18): `core/eval/RunTrace` (events + nodeVisits/stepCount/completed/interrupted/failed/finalState/toolRuns) + `TraceRecorder.record(flow)/of(events)` — رکوردِ مشاهده‌پذیرِ یک run برای HUD/VB9 + time-travel. **۳ تستِ جدید** (`RunTraceTest`: runِ کاملِ tool-using با VisionAgent + interrupted + failed). build+test سبز **۳۶۵ تست**. + **golden-eval عامل** (v75): `core/eval/AgentEvalHarness` — هر `AgentEvalCase` را با VisionAgent اجرا، trace می‌کند و تکمیل + صداخوردنِ ابزارِ موردانتظار را چک می‌کند؛ `AgentEvalReport.passRate`/`describeFailures`. با مدلِ fake در CI بدونِ شبکه (regression-net برای ReActِ ابزاری). ۴ تستِ `AgentEvalHarnessTest` (**۳۸۰ تست**). **E1 کامل.**

### VCF-6 — Brain-Full (Python) parity — §13
- [ ] **VCF-B1 Contract mirror**: همان `VisionMessage/ToolSpec/GraphEvent` در `brain/` (pydantic ↔ kotlinx، JSONِ یکسان) + nodeهای سبکِ LangGraph در Python برای tierِ سنگین (VPS/PC). delegation از گوشی به brain و stream برگشت. **پذیرش:** conformance-testِ JSONِ یکسانِ دو طرف.

---

## 🧩 DS — VISION OS Dual Experience (Widget + Launcher + Brain) — طرحِ کاربر 2026-06-17
> طرحِ کامل (PRD/معماری/monorepo/pseudo-code/API/stack): **`plu/VISION-DUAL-SYSTEM.md`** (سندِ full-spec در حالِ تکمیل).
> **یافته:** «Vision Brain (Core)»ِ این طرح ~۸۰٪ ساخته شده (router/agent/memory/voice/mesh)؛ دو محصولِ واقعاً جدید = **Widget شناور** و **Windows shell**. کارِ اصلی = productize + cross-platform، نه greenfield. stack: **KMP + Compose Multiplatform** (یک Brainِ Kotlin، سه frontend). **هم‌پوشانی:** بعضی آیتم‌ها بلوک‌های موجود را در بر می‌گیرند (PAO→DS-W، CF5→DS-BG2، SRCH→DS-L4، MX→DS-C3، CF4→DS-B3، CTX→DS-B2/DS-W5، ORB→DS-W2). ترتیبِ پیشنهادی: **DS-F → DS-W → DS-B → DS-L → DS-BG → DS-WIN → DS-C → DS-X**.

### DS-F — Foundation & shared (پایه)
- [ ] **DS-F1 brain-core extraction**: انتقالِ router+agent+memory به ماژولِ Kotlinِ ساده + facadeِ `VisionBrain` (`handle/state/remember/recall`)، بدونِ تغییرِ رفتار. **پایه‌ی همه.**
- [ ] **DS-F2 vision-protocol**: مدل‌های مشترک (`VisionRequest/Response`, `Intent`, `DeviceContext`, `MemoryDTO`, `VisionEvent`) با kotlinx-serialization، KMP-ready.
- [ ] **DS-F3 vision-sdk**: کلاینتِ مشترک برای Widget/Launcher/Desktop (آداپترِ in-process + network به Brain).
- [ ] **DS-F4 monorepo split**: ماژول‌بندیِ Gradle (`vision-widget`، `vision-launcher`، `vision-brain`، `vision-protocol`، `vision-sdk`، `vision-api`، `vision-desktop`) via composite/included builds؛ مهاجرتِ تدریجی (بدونِ شکستنِ اپ).

### DS-W — Vision Widget (overlayِ همیشه‌روشن)  [= PAO، productized]
- [ ] **DS-W1 FloatingWidgetService**: foreground service + `WindowManager` overlay (`TYPE_APPLICATION_OVERLAY`) + هاستِ `ComposeView` (الحاقِ Lifecycle/SavedState)؛ funnelِ مجوز (overlay/mic/notif).
- [ ] **DS-W2 Orb state machine UI** [= ORB]: idle breathing / listening wave / thinking pulse / responding glow؛ reduced-motion، ۱۵۰–۳۰۰ms، interruptible.
- [ ] **DS-W3 Gestures**: tap→quick prompt · long-press→voice · swipe-up→پنلِ گسترده · swipe-side→quick actions · double-tap→تکرارِ آخرین اکشن؛ drag-threshold؛ collapse-to-dot در idle.
- [ ] **DS-W4 Mini-panels**: Notes (capture→Memory) · Reminders (→scheduler) · Smart Search (DS-L4) · Automation (macro) · Recent-memory (نمای recall).
- [ ] **DS-W5 Context awareness**: اپِ foreground (Accessibility) + نوتیف‌ها → promptِ Brain (مشترک با DS-B2).
- [ ] **DS-W6 Battery/low-mem**: notifِ کم‌اولویت، idle-collapse، Doze-safe، wake-word کم‌مصرف، degrade در حافظه‌ی کم.

### DS-L — Vision Launcher (Android، روی LR/NEO)
- [ ] **DS-L1 AI smart grouping**: خوشه‌بندیِ اپ‌ها بر اساسِ usage+category → فولدرهای زمینه‌ای (Work/Social/Media/Tools).
- [ ] **DS-L2 Adaptive home**: پیشنهادِ پویای اپ بر اساسِ الگوی استفاده؛ هابِ orb مرکزی.
- [ ] **DS-L3 Edit-mode AI optimize**: اکشن‌های «clean home»/«optimize productivity» + تاریخچه‌ی undo/redo چیدمان.
- [ ] **DS-L4 Universal search** [= SRCH]: apps/files/settings/contacts/AI-actions/web/automation در یک لیستِ رتبه‌بندی‌شده‌ی معنایی.
- [ ] **DS-L5 Usage-based ranking**: رتبه‌بندیِ اپ‌ها/اکشن‌ها.

### DS-B — Vision Brain (تعمیقِ هسته)
- [ ] **DS-B1 Intent classifier + planner**: کلاسیفایرِ صریحِ intent + plannerِ چندمرحله‌ای (توسعه‌ی `TaskPlanner`).
- [ ] **DS-B2 ContextEngine** [= CTX]: اپ/نوتیف/ساعت/باتری/شبکه → تزریق به prompt.
- [ ] **DS-B3 Memory deepen** [CF4 ✅ پایه]: session کوتاه‌مدت + بلندمدتِ معنایی + preference + **summarization engine** (فشرده‌سازیِ نوبت‌های قدیمی).
- [ ] **DS-B4 Task engine**: workflowِ زنجیره‌ای + retry/fallback (توسعه‌ی `AgentEngine`/`ToolCaller`).
- [ ] **DS-B5 Event system**: توسعه‌ی `EventBus` با `app_opened`/`user_idle`/`command_received`/`context_changed` + subscriberها.

### DS-BG — Background system
- [ ] **DS-BG1 Foreground service manager**: یکپارچه‌سازیِ سرویس‌های widget + brain-lite (یک ساختارِ پایدار).
- [ ] **DS-BG2 Task scheduler** [= CF5]: WorkManager (TIME/CONDITION/APP_OPEN/NOTIFICATION/LOCATION).
- [ ] **DS-BG3 Notification handler** + event bus (DS-B5).
- [ ] **DS-BG4 Resource/power optimizer**: اجرای power-aware، Doze، low-memory.

### DS-C — Communication
- [ ] **DS-C1 In-process plane**: Widget+Launcher ↔ Brain مستقیم (facadeِ `VisionBrain`، هم‌پروسه).
- [ ] **DS-C2 Network plane**: توسعه‌ی Brain-Lite API — استریمِ توکن WebSocket (`/v1/stream`)؛ ارزیابیِ gRPC برای `vision-api`.
- [ ] **DS-C3 Cross-device sync** [= MX]: memory/clipboard/handoff روی mesh؛ CRDT برای stateِ چیدمان.
- [ ] **DS-C4 IPC (گزینه‌ی جدا-اپ)**: AIDL bound-service / Messenger برای Widgetِ cross-process.

### DS-WIN — Windows shell (Compose-MP)
- [ ] **DS-WIN1 Desktop skeleton**: Compose-MP + pairingِ peerِ Brain-Lite (mDNS/QR/election).
- [ ] **DS-WIN2 Command palette**: hotkeyِ سراسری سبکِ Spotlight/Copilot.
- [ ] **DS-WIN3 AI taskbar/dock** (جایگزینِ taskbar).
- [ ] **DS-WIN4 Window manager + AI snap**: Win32 via JNA (`SetWindowPos`/`EnumWindows`/`MonitorFromWindow`).
- [ ] **DS-WIN5 Multi-desktop + Focus mode** (پنهان‌کردنِ حواس‌پرتی توسط AI).
- [ ] **DS-WIN6 Cross-device sync** با stateِ لانچرِ اندروید (DS-C3).

### DS-X — Plugin ecosystem + deployment
- [ ] **DS-X1 Plugin SDK**: registryِ اکشن + ابزارهای قابل‌توسعه + sandbox (هم‌راستا با MCP).
- [ ] **DS-X2 Deployment**: CI/CD per-module، signing، packagingِ desktop (MSI/exe)، deployِ brain-server، کانال‌های dev/beta/prod، feature flags.

## 🚀 اولویت ۰ — LR: لانچرِ واقعیِ اندروید (REAL LAUNCHER)
> **دستور کاربر (2026-06-16): «بس کن mockup ساختن — Vision باید یک لانچرِ واقعی باشد مثل Pixel/Nova/Nothing.»**
> **ممیزی (2026-06-16):** اپ به‌عنوان HOME رجیستر شده (`CATEGORY_HOME`+`QUERY_ALL_PACKAGES`) و drawer اپ‌های واقعی را اجرا می‌کند — اما **هوم یک داشبوردِ اسکرولیِ ثابت است، نه لانچر**. تنها persistence = `QuickActionsStore` (۶ شورتکات). **هیچ‌کدام نیست:** گریدِ قابل‌ویرایش، long-press، **drag&drop**، folder، صفحاتِ متعدد، edit-mode، dockِ پیکربندی‌شدنی، widget host (`AppWidgetHost`)، app-drawerِ pro (A-Z/recent/pinned/hidden)، ژست‌های پیشرفته، و persistenceِ layout.
> معماری هدف (Lawnchair-style): `LauncherActivity`(=MainActivity) · Workspace · Hotseat · AppDrawer · FolderManager · WidgetHost · DragController · LauncherSettings · SearchProvider · VisionAssistant. **همه با ui-ux-pro-max + vision-patterns. هیچ‌چیز hardcode نشود.**

### LR — به ترتیبِ اولویت (هرکدام = کارِ واقعی، نه mockup)
- [x] **LR1 Launcher data model + persistence (پایه‌ی همه‌چیز)** (2026-06-16): `data/launcher/` ساخته شد — `LauncherModel` (`@Serializable` `LauncherLayout`{gridCols/Rows,dockCount,pageCount,items} + `LauncherItem`{APP/FOLDER/WIDGET، container WORKSPACE/HOTSEAT، page/cellX/cellY/spanX/spanY، parentId برای فرزندِ folder، widgetId} + helperهای `cells()`/`folderChildren()`) + **`LauncherOps`** (توابعِ کاملاً خالصِ JVM-testable: add/remove/move/setDockCount(3..6)/setGrid/addPage/removePage(+shift)/createFolder/addToFolder/removeFromFolder/renameFolder/isOccupied/firstFreeCell) + **`LauncherStore`** (@Singleton، StateFlow، JSON در `vision_launcher` prefs، id minting، `seedDefault(installedApps)` که dock را پر و بقیه را row-major روی page0 می‌چیند). **۱۱ تستِ سبز** (`LauncherOpsTest`: add/remove/move/dock-clamp/removePage-shift/createFolder/folder ops/firstFreeCell/json round-trip). compile+test سبز.
- [x] **LR2 Workspace grid + pages** (2026-06-16): هوم به یک لانچرِ واقعی با `HorizontalPager` تبدیل شد — **صفحه‌ی ۰ = هوم AI-core (همان orb، طبق RD11 هیرو می‌ماند)** و صفحه‌های بعدی **گریدِ واقعیِ workspace** که اپ‌های pin‌شده را از `LauncherStore` رندر می‌کنند (نه mockup). `ui/screen/workspace/`: **`LauncherViewModel`** (@HiltViewModel — `LauncherStore.layout` + بارگذاریِ آیکن/لیبلِ واقعیِ اپ‌ها با `QUERY_ALL_PACKAGES`، seedDefault در اولین اجرا، `launch()` با شمارشِ usage مشترکِ drawer) + **`WorkspaceScreen`** (`WorkspaceHomePager`: pager با pageCount=۱+pageCount، گریدِ ثابتِ `gridCols×gridRows` که سلول‌ها را از `layout.cells(WORKSPACE,page)` می‌کشد، AppTile با آیکنِ واقعی، **page dots** شناور بالای dock، و **FolderTile→FolderDialog** برای peekِ فولدر/launch). در `MainActivity` مسیرِ HOMEِ compact حالا `WorkspaceHomePager` را با `homePage={homeContent}` می‌سازد (مسیرِ Expanded/tablet دست‌نخورده). `compileDebugKotlin` + `testDebugUnitTest` سبز. **نیازِ تأییدِ بصری روی دستگاه.** [drag&drop = LR3.] **اصلاح (v48):** `seedDefault` حالا فقط **یک صفحه‌ی curated** را seed می‌کند (سبکِ Neo/Pixel: هومِ تمیز، نه ده‌ها صفحه از همه‌ی اپ‌ها)؛ بقیه‌ی اپ‌ها فقط در drawer هستند. dock/hotseat برای LR6 خالی می‌ماند.
- [~] **LR3 DragController (drag & drop واقعی)** (v35، 2026-06-16): long-press روی گرید → برداشتنِ آیکنِ زیرِ انگشت، آیکنِ شناورِ دنبال‌کننده، و روی رها کردن: سلولِ مقصد با **`LauncherGeometry.cellAt`** (خالص، ۶ تست) محاسبه و مدل تغییر می‌کند — drop روی خالی → `move`، روی اپ → `createFolder`، روی فولدر → `addToFolder`. آیتمِ برداشته‌شده در جای خود dim می‌شود. tap همچنان launch می‌کند (long-press جداست). `pointerInput`(`detectDragGesturesAfterLongPress`) روی کلِ گرید تا مختصاتِ grid-local دقیق باشد. compile+test سبز. **نیازِ تأییدِ روی دستگاه.** [باقی‌مانده = drag بینِ صفحات و به/از dock → LR6.]
- [~] **LR4 Edit Mode (bottom sheet)** (v36، 2026-06-16 — افزودن/حذفِ پایه): دو اکشنِ ضروریِ مدیریتِ هوم اضافه شد — **افزودنِ اپ به هوم** با long-press در App Drawer (`AppDrawerViewModel.addToHome` → `LauncherStore.addAppToHome` که اولین سلولِ خالی را در صفحات پیدا می‌کند، در صورتِ پر بودن صفحه می‌سازد، و از تکرار جلوگیری می‌کند؛ + Toast) و **حذف از هوم** با کشیدنِ آیکن به نوارِ قرمزِ «Remove from home» که هنگامِ drag بالای صفحه ظاهر می‌شود (`onRemove`→`store.remove`). build+test سبز. **نیازِ تأییدِ روی دستگاه.** **(v43): منوی long-pressِ آیکن (Neo/Pixel-style)** — long-press بدونِ حرکت روی آیکن → منوی شیشه‌ای با **App info** + **Remove from home**. **(v44): شیتِ Edit Homeِ سبکِ Neo** — long-press روی فضای خالی → `ModalBottomSheet` با **Wallpaper** (chooserِ سیستم)، **Add a page**، و **Remove this page**. LR4 پایه کامل. **(v46): شیتِ Edit Home کامل شد** — حالا **Grid size** اینلاین (۴×۵…۶×۶ با reflow) + **Launcher settings** هم دارد (علاوه بر Wallpaper/Add page/Remove page). [باقی‌مانده: widget picker (=LR8).]
- [~] **LR5 Folders** (v38، 2026-06-16): هسته کامل شد — **ساخت** (drag اپ روی اپ، LR3)، **باز کردن** (تپ روی فولدر → دیالوگِ شیشه‌ای با گریدِ ۴‌ستونه‌ی فرزندان، tap→launch)، **rename** (عنوانِ فولدر در دیالوگ `BasicTextField` است و با هر ویرایش از `LauncherStore.renameFolder` persist می‌شود)، و **persist** کاملِ مدل. build+test سبز. **نیازِ تأییدِ روی دستگاه.** **(v39): previewِ ۲×۲ آیکنِ واقعیِ فرزندان.** **(v40): کشیدنِ اپ به بیرونِ فولدر** — long-press روی اپ داخلِ دیالوگ → `LauncherStore.pullFromFolder` (اولین سلولِ خالیِ هوم). هسته‌ی LR5 کامل (create/open/rename/preview/pull-out/persist). [باقی‌مانده: فقط انیمیشنِ open.]
- [ ] **LR6 Configurable Dock (Hotseat)**: count 4/5/6 + reorder + add/remove + persist؛ دکمه‌ی مرکزیِ Vision همیشه. (روی `LauncherStore` HOTSEAT.)
- [~] **LR7 App Drawer Pro** (v47، 2026-06-17): **ایندکسِ A–Z fast-scroll** (سبکِ Neo) روی لبه‌ی راستِ drawer اضافه شد — تپ روی هر حرف به اولین اپِ شروع‌شده با آن حرف اسکرول می‌کند (`rememberLazyGridState.animateScrollToItem` با محاسبه‌ی offsetِ هدرها/specialها؛ حروفِ بدونِ اپ غیرفعال/کم‌رنگ). search + category chips + Recent از قبل بود (RD3). [باقی‌مانده: Hidden/Pinned + swipe-up transition.]
- [ ] **LR8 Widget Host**: `AppWidgetHost`/`AppWidgetManager` + widget picker + bind/permission + resize + قراردادن روی گرید (span). (نیازِ تست روی دستگاه.)
- [ ] **LR9 Gestures**: double-tap/swipe-up/swipe-down/two-finger/pinch + نگاشتِ custom action (open drawer/notifications/Vision/app). persist.
- [~] **LR10 Launcher Settings screen** (v45، 2026-06-17 — پایه): بخشِ LAUNCHERِ Settings حالا **Grid size** زنده دارد — presetهای ۴×۵/۵×۵/۵×۶/۶×۶ که `LauncherOps.reflowWorkspace` (خالص، ۲ تستِ جدید) را صدا می‌زنند تا گرید تغییر کند و همه‌ی آیکن‌ها **بدونِ گم‌شدن** دوباره row-major چیده شوند (سرریز → صفحه‌ی بعد). + Backup/Restore/Reset (v37). [باقی‌مانده: Dock/Gestures/Search/Widgets/Animations زیربخش‌ها.]
- [x] **LR11 Backup / Restore** (v37، 2026-06-16): در بخشِ LAUNCHERِ Settings سه اکشن اضافه شد — **Back up** (کلِ layout را با `LauncherStore.exportJson` به clipboard کپی می‌کند)، **Restore** (متنِ clipboard را با `importJson` پارس و اعمال می‌کند، fail-safe با Toast)، و **Reset** (`reset()` — همه‌ی pinها پاک، در restartِ بعدی دوباره seed می‌شود). compile+test سبز. **نیازِ تأییدِ روی دستگاه.** [export/import فایلیِ کامل = follow-up.]
- [ ] **LR12 Vision Assistant integration**: دکمه‌ی مرکزیِ dock → دستیار (voice/text/agent/automation/search/device-actions) — اتصال به ارکستراتورِ موجود.

## 🟣 اولویت ۰.۵ — NEO: برابریِ تجربه با Neo-Launcher (clean-room) + گزارش‌ها
> **دستور کاربر (2026-06-17):** «از Neo-Launcher (`NeoApplications/Neo-Launcher`) استفاده کن، کدها/منوها/همه‌چیزش را به کار ببر، امکاناتِ خودمان را هم بگذار، گویِ ویژن را نگه دار، دقیقاً ویژن را به آن اضافه کن و پیشرفته‌اش کن.»
> **تصمیمِ تأییدشده‌ی کاربر (2026-06-17): «بله، clean-room ادامه بده».** ⚠️ **Neo-Launcher = GPL-3.0** (فورکِ Lawnchair/Omega)؛ این ریپو «source-available / Not open source» است. کپیِ مستقیمِ کدِ Neo کلِ اپ را مجبور به GPL-3.0 می‌کند (ناسازگار با انتشارِ بسته/تجاری) و عملاً روی Launcher3 سوار است که در اپِ Compose جا نمی‌شود. **پس به‌جای کپی، تجربه/منوها/امکاناتِ Neo را با کدِ خودمان بازسازی می‌کنیم** و گویِ ویژن + قابلیت‌های AI حفظ می‌شوند. گزارش‌ها در `plu/reports/`.
> **بازخوردِ کاربر (2026-06-17):** «UI نسخه‌ی اندروید اصلاً خوب نبود و همه‌چی مشکل داشت.» + باگِ اسم (رفع شد v42). **نیازِ بازخوردِ مشخص روی دستگاه برای ادامه‌ی دقیق.**

### 🎨 الهام و شکلِ هدفِ لانچر (Design North-Star)
> **از کجا الهام گرفتیم:**
> - **Neo-Launcher / Lawnchair / Omega** — الگوهای رفتاریِ یک لانچرِ بالغِ اندرویدی: workspace گرید + صفحات، hotseat، app drawer با ایندکسِ A–Z، فولدرها، edit-mode، انتخابِ ویجت، ژست‌ها، تنظیماتِ عمیق. (فقط **الهامِ رفتاری/منو**؛ کد GPL-3.0 است و کپی نمی‌شود — clean-room.)
> - **Nova / Pixel Launcher** — هومِ **curated** (نه همه‌ی اپ‌ها)، دات‌های صفحه، long-press popup، At-a-Glance، swipe-up→drawer.
> - **زبانِ بصریِ ویژن** (از `repo/CLAUDE.md` و `Example/`): **Apple Vision Pro + Nothing OS + Arc** — شیشه‌ی نیمه‌شفاف (`glassPanel`)، گرادیانِ azure→violet، آیکنِ وکتورِ یکپارچه (بدونِ emoji)، موشنِ ظریف. **گویِ ویژن (`VisionOrb`) همیشه هیروِ صفحه‌ی اول است.**
>
> **لانچر باید به این شکل باشد (target shape):**
> 1. **هوم = AI-first + لانچرِ واقعی، هم‌زمان.** صفحه‌ی ۰ هومِ AI-coreِ ویژن (orb + command bar)؛ با swipe به صفحاتِ workspaceِ گرید (اپ‌های pin‌شده) می‌رسیم. دات‌های صفحه شناور بالای داک.
> 2. **هومِ تمیز و curated** — فقط یک صفحه‌ی اولیه پر می‌شود؛ همه‌ی اپ‌ها در drawer هستند. بدونِ شلوغی، فضای خالیِ نفس‌کشیدن، طبقِ ui-ux-pro-max.
> 3. **تعاملاتِ کاملِ لانچر:** long-press آیکن → منوی App info/Remove؛ long-press فضای خالی → شیتِ Wallpaper/Grid/Pages/Settings؛ drag&drop برای جابه‌جایی/فولدر؛ کشیدن به نوارِ Remove برای حذف.
> 4. **App Drawer pro:** جستجو + ایندکسِ A–Z + دسته‌بندی + Recent (+ آینده: Hidden/Pinned).
> 5. **داک:** دکمه‌ی مرکزیِ ویژن همیشه (هیرو)؛ بقیه‌ی اسلات‌ها قابلِ‌پیکربندی (LR6).
> 6. **AI در همه‌جا:** گوی + command bar مسیرِ شناختیِ ویژن (orchestrator/router)؛ صدای روانِ فارسی/چندزبانه؛ دستیار از داک و گوی در دسترس.
> 7. **شیشه + موشن + state-backed colors** در همه‌ی اجزا؛ پشتیبانی از ۳ تم و reduced-motion؛ ریسپانسیو (فون/تبلت/دسکتاپ).
> 8. **هیچ‌چیز hardcode نیست** — همه‌چیز از `LauncherStore` (persist) رندر و mutate می‌شود.

- [x] **NEO0 تصمیمِ رویکرد (clean-room)** (2026-06-17): تأییدِ کاربر گرفته شد؛ کدِ GPL کپی نمی‌شود، تجربه‌ی Neo با کدِ خودمان بازسازی می‌شود.
- [x] **NEO1 فیکسِ باگِ اسم** (v42): onboarding «نامِ شما» و «نامِ دستیار» را جدا می‌پرسد؛ صفحه‌ی اصلی با اسمِ خودِ کاربر خوش‌آمد می‌گوید (نه اسمِ ویژن). در Settings هر دو editable.
- [x] **NEO2 منوی long-pressِ آیکن** (v43): App info + Remove from home (تشخیص drag با آستانه‌ی ۲۴dp).
- [x] **NEO3 Edit Home sheet** (v44/v46): long-press فضای خالی → Wallpaper · Grid size (۴×۵…۶×۶ با reflow) · Add/Remove page · Launcher settings.
- [x] **NEO4 Grid density** (v45): `LauncherOps.reflowWorkspace` (۲ تست) + کنترل در Settings.
- [x] **NEO5 App Drawer A–Z fast-scroll** (v47): ایندکسِ Neo-style روی لبه‌ی راست.
- [x] **NEO6 هومِ curated** (v48): seedDefault فقط یک صفحه می‌چیند (نه ده‌ها صفحه)؛ بقیه در drawer.
- [ ] **NEO7 UI quality pass (نیازِ بازخوردِ دستگاه)**: رفعِ مشکلاتِ مشخصِ بصری که کاربر روی نسخه‌ی نصب‌شده گزارش می‌دهد (اسپیسینگ/کنتراست/اسکرول/اندازه‌ی آیکن). تا بازخورد نیاید، حدسی است.
- [ ] **NEO8 LR6 dock decision + پیاده‌سازی**: تصمیمِ کاربر — ادغامِ داکِ نَوِ شناور (Phone/Messages/Vision/Camera/Apps) با hotseatِ قابل‌ویرایش، یا جدا ماندن. سپس hotseatِ مدل‌محور (تعداد ۴/۵/۶ + reorder + drop به/از dock).
- [ ] **NEO9 LR8 widgets واقعی**: `AppWidgetHost`/`AppWidgetManager` + picker + bind/permission + resize + قراردادن روی گرید. (نیازِ تستِ دستگاه.)
- [ ] **NEO10 LR9 gestures**: swipe-up از workspace → drawer (transition واقعی)، double-tap → lock، نگاشتِ custom. (مراقبِ تداخل با long-press drag.)
- [ ] **NEO11 At-a-Glance + search bar**: نوارِ جستجو/ویجتِ نگاهِ‌سریعِ سبکِ Neo/Pixel بالای هوم (ساعت/تاریخ/هوا) + جستجوی یکپارچه‌ی اپ/مخاطب/وب.
- [ ] **NEO12 Icon pack + notification dots روی هوم**: پشتیبانی از آیکن‌پک (resolverِ واقعی) + نقطه‌ی نوتیفِ per-app روی آیکن‌های هوم (badge در drawer از قبل هست).
- [ ] **NEO13 folder open animation + cross-page drag**: انیمیشنِ بازشدنِ فولدر + کشیدنِ آیکن بینِ صفحات و به/از dock (تکمیلِ LR3).
- [ ] **NEO14 Neural voice follow-up**: تأییدِ Edge neural روی دستگاهِ واقعی + pickerِ صدای neural per-voice + streamingِ chunk-by-chunk + STT چندلوکیلِ خودکار.

> کاربر v12 را نصب کرد: **هر دو طراحی قبلی (HUD) و جدید (v12 orb) رد شد** — «هیچ اثری از طراحی قدیمی نباشد، کاملاً جدید، از عکس‌ها استفاده کن.» + امکان تغییر فونت در اپ + فونت بهترِ پیش‌فرض.
> **منبعِ طراحی = پوشه‌ی `05-vision/Example/`** (عکس‌های مرجع) + `repo/docs/design/references/vision-*.png|jpg`. طرح کامل: `repo/docs/design/2026-06-14-vision-launcher-redesign-v2.md`. **همه‌ی طراحی با ui-ux-pro-max.**

### RD — Launcher Redesign v2.2 (سه لایه‌ی ریسپانسیو؛ طراحی قدیمی کاملاً حذف)
- [x] **RD0 پاکسازی طراحی قدیمی** (v28، 2026-06-15 auto-rp): هویتِ بصریِ HUDِ cyan/teal کاملاً حذف شد — پالتِ جدیدِ **azure→violet** روی فضای تیره (طبق عکس‌ها)، تایپوگرافیِ chip از monospace به Inter سَنس، و **حذفِ همه‌ی emoji**؛ یک خانواده‌ی آیکنِ وکتورِ یکپارچه (`VisionIcons`، Material Symbols Rounded) در bottom-nav/greeting/command/stats/quick-actions/agents. چون پالت state-backed است، تغییر در `JarvisTheme.deepSpace`+default accent کلِ اپ را recolor می‌کند. [بازچینشِ کاملِ صفحه‌ها = RD2–RD8 هنوز باز].
- [x] **RD1 AICoreOrb جدید** (v27، 2026-06-15 auto-rp): `VisionOrb` از کره‌ی توپُر به **ringِ نورانی/portal** طبق عکس‌های `Example/` بازنویسی شد — sweepِ چرخانِ cyan→blue→violet→magenta با bloom (۳ stroke)، coreِ تیره‌ی نیمه‌شفاف، **ذرات نورونی** چشمک‌زن (Canvas)، **ripple**های بازتابِ پایین، و wordmarkِ «VISION / AI CORE ONLINE» با فونتِ Space Grotesk (display) + monoِ caption. pulse/spin پشتِ animation-toggle. امضای composable دست‌نخورد. **نیازِ تأییدِ بصری روی دستگاه.** [RD0/RD2–RD10 هنوز باز].
- [x] **RD2 HomeScreen (فون)** (2026-06-16): `HomeScreen.kt` کاملاً طبق عکسِ `Example/example for ui.png` بازچینش شد — هدرِ **«VISION OS / AI-NATIVE LAUNCHER»** وسط‌چین (با heartbeat dot + گیرِ ghost)، greeting **«Good Evening, {name}»** + چیپِ شیشه‌ایِ هوا (۲۴° Mostly Cloudy)، **OrbCluster**: همان `VisionOrb` با ۴ ماهواره‌ی شیشه‌ای دورِ آن (Memory/Projects/Agents/Automation، هرکدام shortcut)، command bar با placeholder **«What would you like to do today?»** + گلیفِ Vision (PlasmaSweep) چپ و دکمه‌ی send راست، **Quick Access** تک‌ردیفه‌ی ۶‌تایی (reorder/edit حفظ شد)، کارتِ **Today's Overview** (tasks/agent/memory زنده)، کارتِ **AI Status** («All systems operational» + RAM/Nodes/Battery + **DonutGauge** که باتری را نشان می‌دهد)، و ردیفِ دوتاییِ **Active Agents + Connected Devices**. آیکن‌های جدید به `VisionIcons` افزوده شد (Spark/Projects/Automation/Files/Browser/More/Cpu/Ram/Battery). همه با ui-ux-pro-max (touch ≥44dp، vector icons، state-backed colors، visionEnter سازگار با reduced-motion، systemBarsPadding). `compileDebugKotlin` سبز. **نیازِ تأییدِ بصری روی دستگاه.**
- [x] **RD2.d Floating Vision Dock** (2026-06-16): `MainActivity.VisionBottomBar` از `NavigationBar`ِ متریال به **dockِ شناورِ شیشه‌ای** طبق عکس بازنویسی شد — pillِ گرد (radius 30) که با `navigationBarsPadding` + حاشیه‌ی افقی بالای gesture-bar شناور است، با haloِ glow؛ آیتم‌ها: **Phone · Messages · Vision(مرکز) · Camera · Apps**. Phone/Messages/Camera اپِ پیش‌فرضِ سیستم را با intent باز می‌کنند (`ACTION_DIAL` / `CATEGORY_APP_MESSAGING` / `STILL_IMAGE_CAMERA`)، Vision → Home (دکمه‌ی بزرگ‌ترِ plasma که بالای ریل می‌زند + glow، selected-aware)، Apps → drawer. آیکن‌های Phone/Messages/Camera به `VisionIcons`. هابِ‌های داخلی (Agents/Memory/Settings) از Home در دسترس‌اند (satelliteها/quick-actions/گیرِ هدر)؛ تبلت همان `VisionNavRail` را نگه می‌دارد. `compileDebugKotlin` سبز. **نیازِ تأییدِ بصری روی دستگاه.**
- [x] **RD2.b اپ‌های مهم روی Home به‌صورت پیش‌فرض** (2026-06-16): ردیفِ **Favorites** روی Home اضافه شد — `HomeViewModel.favorites` پراستفاده‌ترین اپ‌های واقعیِ کاربر را (از prefs مشترکِ `vision_app_usage`) با آیکن/لیبلِ واقعی بارگذاری می‌کند؛ **پیش از وجودِ usage، پیش‌فرض‌های سیستم** برای phone (`ACTION_DIAL`)/SMS (`CATEGORY_APP_MESSAGING`)/browser (`CATEGORY_APP_BROWSER`)/camera (`STILL_IMAGE_CAMERA`) را resolve و نمایش می‌دهد تا از همان بوتِ اول روی Home باشند. تپ → launch با intentِ سیستم + شمارشِ usage (یادگیری). build+test سبز. **نیازِ تأییدِ بصری روی دستگاه.**
- [x] **RD3 AppsScreen** (2026-06-16): `AppDrawerScreen.kt` طبق مرجع بازنویسی شد — هدرِ تمیز (chipِ back شیشه‌ای وکتور + «Applications» + شمارش)، **search pillِ شیشه‌ای** با آیکنِ search، ردیفِ **category chips** افقی (All/Recent/Communication/Productivity/Media/Tools/System که از `ApplicationInfo.category`+system-flag استخراج می‌شود)، و **گرید ۵‌ستونه‌ی آیکن‌گرد** با سکشن‌های Recent/All apps. کارت‌های ویژه‌ی **Settings/Vision Hub** حالا آیکنِ وکتور روی PlasmaSweep دارند (اموجی ⚙/✦ حذف شد). دیتالایر (`AppDrawerViewModel`: QUERY_ALL_PACKAGES + usage counters + memory search P6) دست‌نخورد؛ فقط `category` به `AppEntry` و `usageCount()` افزوده شد. `compileDebugKotlin` سبز. **نیازِ تأییدِ بصری روی دستگاه.**
- [x] **RD4 AgentsScreen (management)** (2026-06-16): مدیریتِ کامل از قبل بود (status زنده + toggle فعال‌سازی + selectorِ trust چهارسطحی Read/Suggest/Auto/Critical + recent activity، persist via `AgentRegistry`). در این فاز با زبانِ مرجع هم‌تراز شد — هدرِ HUDِ «‹ BACK / AGENTS» به **chipِ back شیشه‌ای وکتور + «Agents» (TextPrimary) + «X of N active»** تبدیل شد، و هر کارت حالا خطِ **Current Action** (از history، با dotِ رنگِ وضعیت) را نشان می‌دهد (طبق اسپک Status/Progress/Current Action). build+test سبز. **نیازِ تأییدِ بصری روی دستگاه.**
- [x] **RD5 MemoryScreen** (2026-06-16): دیتالایر (count + episodic list + semantic search روی `MemoryRepository`) دست‌نخورد؛ UI با زبانِ مرجع هم‌تراز شد — هدرِ HUD «‹ BACK / MEMORY» → **chipِ back شیشه‌ای وکتور + «Memory» (TextPrimary) + «On-device knowledge graph»**، کارتِ آمار با badgeِ آیکنِ Memory، **search pillِ شیشه‌ای** با آیکنِ search چپ و دکمه‌ی send راست (به‌جای متنِ «GO»)، سرتیترِ RECENT/RESULTS، و ردیف‌های حافظه با badgeِ آیکن + chipِ نوع/score. `compileDebugKotlin` سبز. **نیازِ تأییدِ بصری روی دستگاه.**
- [x] **RD6 SettingsScreen کامل** (2026-06-16): از قبل feature-complete بود (APPEARANCE: theme۳/accent/font-picker/animation/wallpaper/brain-badge + INTELLIGENCE: AI Providers/Brain&Mesh + VOICE + PERSONA: name/Language EN/FA-AUTO/humor/formality/length + INTERFACE FX + TRUST LEVEL + ACTIVATION + PRIVACY MONITOR + LAUNCHER + ABOUT؛ همه live + persist). در این فاز با مرجع هم‌تراز شد — هدرِ HUD «‹ BACK / SYSTEM CONFIG» → **chipِ back شیشه‌ای + «Settings» (TextPrimary) + زیرنویس**، و کارت‌های Section گردتر/تمیزتر (radius 10→18، پدینگ و تایپوگرافیِ بهترِ گروه‌بندی) طبق اسپک «Large Titles / Grouped Sections / Modern Settings UI». در دسترس از gear هوم + کارت Apps. `compileDebugKotlin` سبز. **نیازِ تأییدِ بصری روی دستگاه.**
- [x] **RD7 Responsive** (2026-06-16): پایه از قبل بود (WindowSizeClass: phone=dockِ شناور / tablet=NavRail + عرضِ محدودِ ۷۶۰). در این فاز لایهٔ **Expanded/desktop** کامل شد — `widthClass==Expanded` به‌صورت `expanded` از onCreate→VisionShell→RouteContent عبور می‌کند؛ روی HOMEِ Expanded، صفحه به‌صورت Row رندر می‌شود: paneِ اصلیِ Home (با `showSidePanels=true` تا two-upِ Agents/Devices حذف شود) + **پنلِ راستِ ۳۶۰dp** که `AgentsScreen(showBack=false)` را میزبانی می‌کند؛ capِ خوانایی ۷۶۰ روی این حالت غیرفعال می‌شود تا با پنل نجنگد. `compileDebugKotlin` سبز. **نیازِ تأییدِ بصری روی تبلت/دسکتاپ.**
- [x] **RD8 Command bar → cognitive router** (2026-06-16، تأیید): از قبل درست سیم‌کشی شده بود — `HudViewModel.sendChat` از `VisionOrchestrator.decide()` + `BackendRouter.execute()` استفاده می‌کند (مسیرِ شناختیِ VB3+VB8) که صریحاً جایگزینِ gateِ brain-first شد (BUG-AI: قبلاً فقط Groqِ Brain-Lite را می‌زد و توکن‌های کاربر را نادیده می‌گرفت). brain فقط fallbackِ بعد از شکستِ router است (و در SOVEREIGN خاموش). command barِ Home (دکمه + IME) هر دو `sendChat` را صدا می‌زنند. بدون نیاز به تغییر کد.
- [x] **RD9 کیفیت (ui-ux-pro-max)** (2026-06-16): بیشترِ معیارها در خودِ بازطراحی رعایت شده بود — **آیکنِ وکتور** همه‌جا (بدونِ اموجی)، **safe-area** با `systemBarsPadding`/`navigationBarsPadding`، **reduced-motion** چون `visionEnter` و orb پشتِ `ThemeStore.animations` هستند، و رنگ‌های **state-backed برای هر دو تم**. در این فاز **`collectAsState` → `collectAsStateWithLifecycle`** در هر ۵ صفحه‌ی بازطراحی‌شده (Home/Drawer/Agents/Memory/Settings — جمعاً ۲۴ collector) مهاجرت کرد (lifecycle-aware: جمع‌آوری در STOP متوقف می‌شود). build+test سبز. [بازماندهٔ نیازمندِ دستگاه: ممیزیِ کنتراست ≥4.5:1 و touch ≥48dp روی هر دو تم، و home <300ms با profiling].
- [x] **RD10 قابلیت‌های لانچرِ سنتی (بخش ۲ پرامپت)** (2026-06-16، پایه): **بَج نوتیف روی آیکنِ اپ‌ها** ✅ — `VisionNotificationService` حالا شمارشِ per-package نوتیف‌های clearable را به‌صورت `StateFlow<Map<pkg,Int>>` منتشر می‌کند (on connect/posted/removed)، و App Drawer روی هر tile بَجِ مجنتا (۹+) نشان می‌دهد. **ژستِ swipe-down → shade نوتیف** ✅ — کششِ پایین روی هدرِ Home با `StatusBarManager#expandNotificationsPanel` (reflection، best-effort). **ویجت‌های هوم** (weather/clock/device-stats) ✅ از قبل در RD2 (چیپِ هوا + ساعتِ status-bar + کارتِ AI Status). build+test سبز. **تکمیل (2026-06-16):** **swipe-up→drawer** روی dock اضافه شد (درگِ بالا روی نوار dock → APPS؛ چون drag از tap جداست، دکمه‌ها سالم می‌مانند، و dock بیرونِ scrollِ هوم است پس تداخل ندارد). **فولدر** عملاً با **category chips**ِ App Drawer (Communication/Productivity/Media/Tools/System) پوشش داده می‌شود. **Icon Pack** عمداً موکول شد (placeholderِ بی‌اثر = dead-code خلافِ ui-ux-pro-max؛ نیازمندِ resolverِ واقعیِ آیکن‌پک به‌عنوان فیچرِ مستقل). RD10 پایه = کامل.
- [x] **RD11 Vision OS Home Overhaul (دستور کاربر 2026-06-16: «یک AI-OS، نه لانچرِ اندروید»)**: هوم کاملاً بازطراحی شد تا **AI-core غالب** باشد (هوش اول، اپ‌ها دوم). **(۱)** هدرِ «VISION OS» حذف → فقط greeting («Good Evening / {name} / Vision is ready to assist you») + چیپِ هوا + دکمه‌ی settingsِ کوچک. **(۲) Orb بزرگ‌تر و زنده‌تر** = هیرو: cluster تمام‌عرض `aspectRatio(1)` (~۴۵٪ ارتفاع)، paddingِ داخلی ۳۴→۱۰dp (≈+۴۰٪ بزرگ‌تر)؛ orb بازنویسی شد با **glowِ لایه‌ای**، **energy ringsِ ضدچرخش**، **inner reflection** (هایلایتِ بالا-چپ، نه حفره‌ی سیاه)، **ذرات + خطوطِ اتصالِ عصبی** (شبکه‌ی زنده). **(۳) Neural nodes** (نه دکمه): Memory/Projects/Agents/Automation به‌صورت نودهای دایره‌ایِ کوچک با **رشته‌های نورانیِ متصل به هسته** (Canvas). **(۴) Command bar** = ۷۲dp، radius ۳۶، glass+glow، sparkle چپ + دکمه‌ی voice/send راست. **(۵) Quick Actions** = گریدِ ۳×۲ کارتِ premium (radius ۲۴، فاصله ۱۶dp، آیکنِ PlasmaSweep). **(۶) محتوا = حداکثر ۴ کارت**: Continue Working · Recent Activity · Active Agents · Connected Devices (حذفِ Favorites/AI-Status/Today's-Overview). **(۷) Dock** = بلندتر (آیتم ۶۴dp)، دکمه‌ی مرکزیِ Vision بزرگ‌تر (۷۰dp) با glowِ دولایه. **(۸) رنگ‌ها**: پس‌زمینه از near-black به **گرادیانِ آبیِ #081120→#102040→#182B5A** (فیلدِ `BackgroundGlow` به Palette/۳ تم اضافه شد)، glassِ نیمه‌شفاف‌تر. **(۹) whitespace** بیشتر (فاصله‌های ۲۸/۱۶dp). همه با ui-ux-pro-max. `FavoriteApp`/loaderها از HomeViewModel پاک شد (Favorites حذف). build+test سبز. **نیازِ تأییدِ بصری روی دستگاه.**

### FNT — Typography system + font picker ✅ (v22، 2026-06-15 auto-rp)
- [x] **FNT1 فونت‌های bundled**: `res/font/` — **Space Grotesk** + **Inter** + **Space Mono** (regular+bold) + **Vazirmatn** + **DM Sans** + **Exo 2** (همه OFL، variable به‌جز Space Mono). داخل APK (offline-first). لایسنس در `docs/licenses/FONTS.md`.
- [x] **FNT2 VisionTypography (state-backed)**: `VisionFonts` + `FontStore` (هم‌شکلِ `VisionColors`/`ThemeStore`)؛ `JarvisTheme` تایپوگرافی را از state می‌سازد → تعویض فونت = recompose سراسری. نقش‌های display*/label* مونو می‌مانند (هویتِ HUD)، heading+body فونتِ انتخابی می‌گیرند.
- [x] **FNT3 Font picker در Settings**: بخشِ Typeface در APPEARANCE — چیپِ پیش‌نمایش «Aa» با فونتِ خودش (Auto/Space Grotesk/Inter/DM Sans/Exo 2/Vazirmatn) + persist (SharedPreferences `vision_fonts`)؛ پیش‌فرض = Auto (Space Grotesk+Inter)، با language=FA → خودکار Vazirmatn (به language toggle wired). تستِ `FontCatalogTest` (۶ تست). reset appearance فونت را هم ریست می‌کند.

## 🚨 اولویت ۲ — رفعِ باگ‌های v20 (صدا/زبان) + ToolCaller
> **FIX-ACTIONS (تماس/پیام)** در **v21** رفع شد (`CallTool`+`SmsTool` واقعی، `0c017db`/`c2e7454`) — جزئیات در بخش ۵. باقی‌مانده:
- [x] **FA3 ToolCaller/AgentEngine** (هسته‌ای، = CF2) (v29، 2026-06-15 auto-rp): دیده شود CF2 بالا — `ToolCaller` + `TOOL_PROTOCOL` + `HudViewModel.deliver()`. مدل برای اکشن‌ها JSON می‌دهد → ابزارِ واقعی اجرا → نتیجه‌ی واقعی.
- [ ] **🚨 FIX-VOICE/LANG (باگ v20، تست روی دستگاه واقعی لازم)**:
  - [x] **FV1 TTS نمی‌گوید** (v28، 2026-06-15 auto-rp): ریشه پیدا شد — وقتی پاسخ به فارسیِ واقعی تبدیل شد، `VoiceController` صدا را `fa` می‌گذاشت و روی دستگاهِ بدونِ صدای فارسی، `setLanguage` مقدارِ `LANG_MISSING_DATA`/`NOT_SUPPORTED` برمی‌گرداند و `speak()` بی‌صدا no-op می‌شد (قبلاً Finglishِ لاتین با صدای انگلیسی خوانده می‌شد، برای همین «یکم حرف می‌زد»). فیکس: تشخیصِ صدای ناموجود → اجرای یک‌باره‌ی `ACTION_INSTALL_TTS_DATA` + Toastِ راهنما + fallback به صدای موجود تا هرگز بی‌صدا نباشد. (چت متنی از قبل speak می‌شد — `HudViewModel` پشتِ `ttsEnabled`.)
  - [x] **FV2 voice picker واقعی** (v34، 2026-06-16): انتخابِ صدای واقعی از `TextToSpeech.voices` به ازای **هر زبان** (فارسی + انگلیسی) در بخشِ VOICEِ Settings — چیپِ «Auto (best)» + یک چیپ برای هر صدای نصب‌شده (با برچسبِ کیفیت/کشور/online) + دکمه‌ی **TEST** که نمونه را با همان انتخاب می‌گوید + **persist** (`VisionSettings.voiceNameFa/En`). `VoiceController` حالا `voicesFor(lang)`/`speakSample(lang)` دارد و `SettingsHubViewModel` آن را expose می‌کند. (rate/pitch از قبل بود.)
  - [x] **FV3 درک فارسی + code-switch صوتی** (v34، 2026-06-16): علاوه بر قاعده‌ی نوشتنِ `SCRIPT_RULE`، حالا **خروجیِ صوتی هم code-switch-aware است** — `VoiceSegmenter` (خالص، ۹ تست) متن را به runهای فارسی/لاتین تکه می‌کند و `AndroidVoiceController` هر تکه را **به‌ترتیب با صدای زبانِ خودش** می‌گوید (به‌جای خواندنِ کلِ جمله با یک صدا). پس «یک playlist از Shakira بساز» دیگر گیج نمی‌زند. + انتخابِ **بهترین صدای نصب‌شده** per-locale (کیفیت بالاتر، ترجیحِ offline). [باقی‌مانده‌ی FV3 قبلی: STT چندلوکیلِ خودکار.]
  - [ ] **FV4 Wake-word واقعی + رفع toggle میکروفون**: «صدا زدنش فعالش نمی‌کند، باید کلیک کنی»؛ میکروفون مدام خاموش/روشن می‌شود. → wake-word همیشه‌روشن (Porcupine/Vosk hotword) «Hey Vision»؛ رفعِ چرخه‌ی restartِ `SpeechRecognizer` (lifecycle/پایداری). (آیتمِ بازِ Phase 7.)
  - [~] **FV5 صدای نورونیِ فارسی (Edge Neural TTS، رایگان)** (v41، 2026-06-17 — خواستِ صریحِ کاربر «موتور صوتیِ روان فارسی»): موتورِ Microsoft Edge read-aloud (همان صداهای neuralِ Azure، **بدونِ کلید**) پیاده شد — `EdgeTtsProtocol` (خالص، ۱۱ تست: امضای `Sec-MS-GEC` با SHA256، ساختِ **SSMLِ code-switch** که با `VoiceSegmenter` هر بخشِ فارسی را با `fa-IR-DilaraNeural` و انگلیسی را با `en-US-AriaNeural` در **یک** درخواست می‌گذارد، و parseِ فریمِ صوتیِ باینری) + `EdgeTtsClient` (Ktor WebSocket، جمع‌آوریِ MP3، timeout). در `AndroidVoiceController`: مسیرِ neural **opt-in (پیش‌فرض خاموش)** و **network-gated**، با **fallback کاملِ امن** به TTSِ on-device روی هر شکست (پس هرگز رگرسیون/سکوت). toggle «Neural voice (online)» در Settings + مجوزِ `ACCESS_NETWORK_STATE`. الگوریتمِ GEC با مرجعِ edge-tts مو به مو تطبیق داده شد. **اتصالِ زنده در این sandbox قابلِ تأیید نبود (پراکسیِ MITM محیط هم edge-tts و هم درخواست را 403/cert-error می‌کند) → نیازِ تأییدِ روی دستگاهِ واقعی.** [follow-up: pickerِ صدای neural per-voice + streaming chunk-by-chunk.]
    - **🐞 FIX (v78، 2026-06-19 — بازخوردِ دستگاهِ کاربر روی v67 «فارسی صحبت نمی‌کند، می‌خواست از موتورِ سامسونگ/گوگل استفاده کند که فارسی ندارند»):** ریشه = `VoiceRouting.useNeural` در حالتِ AUTO فقط وقتی neural می‌داد که **تنظیمِ زبان** `LANG_FA` بود؛ اما پیش‌فرضِ زبان `LANG_AUTO` است، پس پاسخِ فارسی به on-device می‌رفت و سامسونگ/گوگل صدای `fa-IR` ندارند → سکوت/خراب. فیکس: AUTO حالا **اسکریپتِ واقعیِ متنِ پاسخ** را هم می‌بیند (`hasPersian` via `VoiceSegmenter`) → هر پاسخِ حاویِ فارسی، آنلاین، به صدای neural (که فارسی دارد) می‌رود، فارغ از تنظیمِ زبان. **۳ تستِ جدیدِ `VoiceRoutingTest`** (۴→۷)، build+test سبز **۴۰۵**. [خودِ تولیدِ صدای neural همچنان نیازِ تأییدِ شبکه/دستگاه؛ این فیکسِ routing بود که شکسته بود.]
  - [x] **FIX-ACTIONS2 پیام/تماس «متوجه نمی‌شود»** (v79، 2026-06-19 — بازخوردِ دومِ دستگاهِ کاربر روی v67): دو شکافِ پارس کشف و رفع شد. (۱) **عبارت‌های طبیعی‌تر**: `CallTool` و `SmsTool` فقط چند الگوی محدود را می‌گرفتند، پس «زنگ بزن مامان»، «تماس با علی بگیر»، «شماره علی رو بگیر»، «give mom a call»، و مهم‌تر **«به X بگو …»** (همان شکلی که `TOOL_PROTOCOL` به مدل می‌گوید بدهد) بی‌تطبیق می‌ماندند → «متوجه نشدم». الگوها گسترش یافت (ترتیب: خاص→عام تا فرمِ مقیدِ موجود بلعیده نشود) + گاردِ `بگو`-به‌خود/دستیار (تا «به من بگو ساعت چنده» اشتباهاً SMS نشود و به AI برود). (۲) **نگاشتِ روابط**: `ContactResolver` تنها یک `DISPLAY_NAME LIKE '%مامان%'` می‌زد، پس وقتی مخاطب با برچسبِ دیگری ذخیره بود («مادر»، نامِ واقعی) پیدا نمی‌شد؛ `ContactRelations` (خالص) واژه‌ی نسبت را به **لیستِ مرتبِ نام‌های کاندید** بسط می‌دهد (مامان↔مادر، بابا↔پدر، mom→مامان، …) و `find()` همه را به‌ترتیب امتحان می‌کند. **۱۰ تستِ جدید** (`ContactRelationsTest` ۶ + `ActionToolParsingTest` ۴)، build+test سبز **۴۱۵**. [اجرای واقعیِ تماس/SMS نیازِ تأییدِ دستگاه؛ این فیکسِ منطقِ پارس/تطبیق بود که شکسته بود.]

## 🧠⚙️ اولویت ۳ — FRAMEWORK FOUNDATION (موتورِ مرکزی؛ پایه‌ی همه‌ی قابلیت‌ها)
> این‌ها backbone اند: تا ساخته نشوند، PROACTIVE/AGT/MON فقط حرف‌اند. هر کدام pure-core + seamِ تزریقی → unit-testable (مثل router). جزئیات: `docs/superpowers/specs/2026-06-15-vision-brain-framework-review.md`.

- [~] **CF1 AgentEngine** (`core/agent/`) (v26، 2026-06-15 auto-rp): `AgentEngine.run(goal, executor, maxSteps, stopOnFailure)` → پلنِ CF3 را گام‌به‌گام اجرا و `AgentRun` (results/completed/success/finalText/transcript) می‌سازد؛ کران‌دار (maxSteps=8)، cooperatively cancellable (`ensureActive`)، stop-on-failure. اجرای هر گام پشتِ `StepExecutor` (fun interface) ایزوله است — موتور هرگز خودش ابزارِ دستگاه را اجرا نمی‌کند. ۶ تست. **باقی‌مانده (نیازِ دستگاه/Trust):** نوشتنِ executorِ واقعی (tools+router) + wire به chat. [streaming/reflect = follow-up].
- [x] **CF2 ToolCaller / function-calling** (`core/agent/`) (v29، 2026-06-15 auto-rp): `ToolCaller.parse(modelText)` (خالص، ۶ تست) JSONِ `{"tool":"action","args":"<command>"}` را از پاسخِ مدل (داخلِ code-fence یا inline، با حفظِ فارسی و unescape) استخراج می‌کند. پرامپتِ `TOOL_PROTOCOL` در `CloudChatRouter` به مدل می‌گوید برای هر اکشنِ دستگاهی فقط همین JSON را بدهد (نه ادعای متنی). `HudViewModel.deliver()` اگر tool-call دید، `inv.args` را به `CommandInterpreter.tryHandle` (لایه‌ی ابزارِ واقعی: Call/SMS/open-app/flashlight/settings/nav/battery/time) می‌دهد و **نتیجه‌ی واقعی** را می‌گوید؛ اگر اجرا نشد، صادقانه اعلام می‌کند (نه echo کردنِ JSON). «هرگز ادعای انجام بدون اجرا» اکنون برای اکشن‌های مدل‌محور هم برقرار است. [باقی‌مانده: argsِ ساختاریافته per-tool + Trust gate برای Criticalها.]
- [x] **CF3 TaskPlanner** (`core/planner/`) (v25، 2026-06-15 auto-rp): `TaskPlanner.plan(goal)` → `ActionPlan(steps)`؛ split روی connectorهای محافظه‌کارِ ترتیبی (then/and then/؛،;/سپس)، هر clause با `IntentClassifier` کلاسیفای و TOOL (ACTION) یا MODEL تگ می‌شود. منطقِ خالص، بدونِ اجرا/side-effect (CF1 بعداً اجرا می‌کند). ۸ تست (EN/FA/code، تک‌گام، تشخیصِ over-split نکردنِ «and»). [مصرف‌کننده‌ی نهایی = CF1 AgentEngine].
- [ ] **CF4 MemoryStore** (`core/memory/`): episodic (turns/events، Room) + semantic (embeddingِ MiniLM موجود) + بازیابی؛ تزریق به context ارکستراتور.
- [ ] **CF5 Scheduler / Automation** (`core/automation/`): WorkManager/AlarmManager — triggerهای زمانی/رویدادی → اجرای plan (پایه‌ی AGT-SCHED و Phase 9.5).
- [~] **CF6 FeedbackLog + Eval harness** (`core/eval/`) (v24، 2026-06-15 auto-rp): `FeedbackLog` (in-memory ring-buffer @Singleton — record `{prompt,intent,model,backend,latency,success,rating}` per turn + `rate()` 👍/👎 + `stats()` successRate؛ StateFlow `recent`) + `EvalHarness`/`GoldenPrompts`/`EvalReport` — ۱۴ promptِ طلایی (EN/FA/code-switch روی همه‌ی intentها + image/voice signal) از `IntentClassifier` واقعی عبور می‌کنند؛ تستِ CI `EvalHarnessTest` رفتارِ زبان/کدسوییچ را به‌صورت regression قفل می‌کند. ۷ تستِ جدید. **باقی‌مانده (نیازِ دستگاه/UI):** wire کردنِ `record()` در chat path + پنلِ 👍/👎 در HUD.

## 🤝 اولویت ۴ — PROACTIVE / AMBIENT AGENT + نظارت + دسترسی‌ها
> کاربر: «ویژن باید همیشه کاربر را ببیند، اقداماتش را ثبت کند، حالت پاپ‌اپ داشته باشد، همیشه پیشنهاد بدهد و تعامل کند و عملاً همه‌کار بکند.» این‌ها کدنویسیِ واقعیِ نحوه‌ی کار و تعاملِ فریم‌ورک‌اند، نه تنظیمات ساده. **هسته‌ی اجرای واقعی = ToolCaller/AgentEngine (CF1/CF2) + Accessibility (تعامل با المان‌ها) + OverlayService.** همه تحت Trust gate + سوییچ شفاف.

### PA — Proactive / Ambient
- [ ] **PAO — Proactive Overlay (پاپ‌اپ همیشه‌حاضر)**: `OverlayService` + `FloatingOrb` (Phase 7.5) — حبابِ شناورِ روی همه‌ی اپ‌ها؛ تپ → پنلِ سریع؛ ویژن از همین‌جا پیشنهاد می‌دهد/تعامل می‌کند. مجوز `SYSTEM_ALERT_WINDOW`.
- [ ] **PAW — Proactive Writing Assist (نمونه‌ی کاربر: در چت دوستش)**: وقتی کاربر در یک فیلدِ متن تایپ می‌کند، ویژن (از طریق Accessibility) متن را می‌خواند؛ اگر غلط/قابل‌بهبود بود، در overlay پیشنهاد می‌دهد «اینجوری بنویس» و با تأیید، متن را **جایگزین/تایپ می‌کند** (`ACTION_SET_TEXT` / `ACTION_PASTE`). شامل: تشخیصِ فیلدِ فعال، خواندنِ محتوا، بهبود با مدل، و نوشتنِ واقعی.
- [ ] **PAB — Browser/YouTube automation (نمونه: «آهنگ جدید شکیرا را در یوتیوب پیدا کن و پخش کن»)**: ابزارِ `WebTool`/`MediaTool` — برای جستجوی YouTube: `Intent` به اپ یوتیوب (`vnd.youtube:` یا `https://youtube.com/results?search_query=`) و در صورت امکان، پخشِ نتیجه‌ی اول؛ برای وب عمومی: باز کردن URL/جستجو. اجرای واقعی + گزارشِ صادقانه (نه ادعای دروغ).
- [ ] **PAE — Email access & assist**: خواندنِ ایمیل‌ها (Gmail API با OAuth یا قراردادِ READ از اپ ایمیل)، **یادآوریِ پیشنهادی** («به X ایمیل بزن؟»)، و کمک در نوشتنِ متن؛ ارسال با `ACTION_SEND`/`ACTION_SENDTO` یا API. تحت مجوزِ صریح.
- [ ] **PAS — Suggestion Engine**: موتورِ پیشنهادِ پیش‌دستانه که از Timeline/نظارت (MON) سیگنال می‌گیرد و در زمانِ مناسب پیشنهاد می‌دهد (نوشتنِ بهتر، یادآوری، اکشن). بدون مزاحمت: نرخ/حساسیت قابل‌تنظیم.
- [ ] **PAA — Action framework (ToolCaller/AgentEngine) = پایه‌ی همه‌ی این‌ها**: مدل خروجیِ function-calling می‌دهد → ToolCaller ابزارِ واقعی را اجرا می‌کند → فقط نتیجه‌ی واقعی گزارش می‌شود. «هرگز ادعای انجامِ بدونِ اجرا» (همان FA3/CF2؛ Call/SMS اولین ابزارهای واقعیِ اجراشده‌اند — کامیت `0c017db`).
- [ ] **PAU — Universal app automation (دستور: «به همه برنامه‌ها و همه‌چیزِ کاربر دسترسی داشته باشد و کارهایش را انجام دهد»)**: کنترلِ عمومیِ هر اپ از طریقِ Accessibility (یافتن المان‌ها، کلیک، اسکرول، تایپ) + کاتالوگِ Intentها (باز کردن/اشتراک/جستجو در هر اپ) + Deep-link؛ به‌علاوه‌ی **App Automation/Workflow** (Phase 9.5: Trigger→Action). هدف: «اتوماسیونِ کاملِ کارهای کاربر». مجوزها: کاملِ بلوک PERM.

### AG — Agents, Skills, Delegated & Scheduled tasks
- [ ] **AGSK — Agents & Skills (دستور: «از ایجنت‌ها و اسکیل‌ها هم استفاده کند»)**: ویژن وظایف را با **ایجنت‌های تخصصی** (روی `AgentRegistry` موجود — لایه ۲، ۴ ایجنت) و **اسکیل‌های ترکیب‌پذیر** (روی plugin registry موجود P12) انجام دهد. AgentEngine یک هدف را به ایجنت/اسکیلِ مناسب مسیریابی می‌کند (مثل routerِ مدل، اما برای قابلیت‌ها)؛ هر skill = یک واحدِ کارِ قابلِ‌فراخوان با ورودی/خروجیِ ساختاریافته. Trust per-agent (Read/Suggest/Auto/Critical) که قبلاً هست.
- [ ] **AGT-DELEGATE — وظیفه‌ی واگذارشده + گزارش (نمونه: «تمام پیام‌های مادر در همه‌ی اپ‌ها را بررسی کن و گزارش کامل بده»)**: AgentEngine هدف را به ایجنت(ها) می‌سپارد → جمع‌آوریِ داده از چند اپ (NotificationListener history + خواندنِ اپ‌های پیام‌رسان via Accessibility + Timeline/MON) → جمع‌بندی با مدل → **تحویلِ گزارشِ ساختاریافته** به کاربر. (long-running task + progress در overlay.)
- [ ] **AGT-SCHED — اکشنِ زمان‌بندی‌شده/خودمختار (نمونه: «امروز ساعت ۱۶ با آقای X تماس بگیر و بگو قرار ۲۲ را فراموش نکند»)**: پارسِ زمان + هدف + پیام → ثبت در `Scheduler`/Automation Engine (Phase 9.5، WorkManager/AlarmManager) → در زمانِ مقرر، اجرای واقعیِ CallTool + رساندنِ پیام (TTS در تماس یا یادآوری). نیازِ تأییدِ کاربر برای اکشن‌های Critical طبق Trust gate. «هرگز ادعای انجام بدون اجرا».

### MON — نظارتِ همیشگی + Timeline (دستور کاربر 2026-06-15)
> «خیلی مهم است ویژن همیشه همه‌چیز را ثبت کند و به صفحه، تماس‌ها، پیام‌ها، فایل‌ها و همه‌چیزِ کاربر نظارت داشته باشد و همیشه در حال بررسیِ کاربر و اقداماتش باشد.» (Always-On Ambient Awareness — C0/Phase 6 Timeline/Phase 7.5.)
- [ ] **MON1 Activity log/Timeline**: ثبتِ رویدادها (اپِ باز، تماس، پیام، نوتیف) در Room + جستجو (Phase 6 Vision Timeline).
- [ ] **MON2 نظارت**: NotificationListener (هست) + UsageStats (هست) + CallLog/SMS observers + Accessibility صفحه + (اختیاری) MediaProjection+OCR برای صفحه.
- [ ] **MON3 حریم خصوصی**: همه‌چیز on-device/رمزنگاری‌شده؛ تحت Trust gate؛ سوییچِ شفافِ روشن/خاموش (کاربر کنترل کامل دارد) — چون این داده‌ها بسیار حساس‌اند.

### PERM — تکمیل کاملِ دسترسی‌ها (دستور کاربر 2026-06-15: «به همه‌چی دسترسی داشته باشه»)
- [~] **PERM (در جریان)**: `CALL_PHONE`/`SEND_SMS`/`READ_CONTACTS` در v21 افزوده شد. باقی: همه‌ی مجوزهای لازم در Manifest + یک صفحه‌ی onboarding/permission-center که تک‌تک را با توضیح درخواست می‌کند (runtime + special access). شامل: `READ_SMS`/`RECEIVE_SMS`, `WRITE_CONTACTS`, `READ_CALL_LOG`/`WRITE_CALL_LOG`, `READ_MEDIA_*`/`MANAGE_EXTERNAL_STORAGE` (فایل‌ها), `RECORD_AUDIO`(هست), `CAMERA`(هست), `POST_NOTIFICATIONS`(هست), `PACKAGE_USAGE_STATS` (special), Notification Listener (special), Accessibility (special), `SYSTEM_ALERT_WINDOW` (overlay), `FOREGROUND_SERVICE*`(هست), MediaProjection (runtime prompt). همه تحت Trust gate + شفاف. (پایه‌ی PAU و MON2.)

### FRAMEWORK DEPTH (کارِ انقلابی — جهت‌گیریِ کلی)
- [ ] عمیق‌ترکردن فریم‌ورک VISION BRAIN فراتر از v1 — اجرای واقعیِ VB9.1 (سلامتِ per-provider)، LM4 hybrid routing، AgentEngine (Goal→Plan→Tool→Execute)، planner چندمرحله‌ای، و حافظه/خودآموزی. (جزئیات در بلوک‌های VB/CF/Phase 5/Phase 10.)

---

# بخش ۲ — 🧠🔀 VISION BRAIN — Cognitive Router + Local & Mesh AI (کاربر 2026-06-14)
> هدف کاربر: ویژن از منابع پرووایدرها استفاده کند **ولی خودش فکر کند و تصمیم بگیرد** (صوت/متن/تصویر/اقدام)؛ از منابع و **مدل‌های لوکالِ دستگاه‌های دیگرِ مش** هم استفاده کند؛ و یک **مدل لوکال رایگانِ خیلی سبک** روی خودِ گوشی نصب شود و در کنار توکن‌پول و مغز کار کند.
> ریشه‌ی فعلی: `CloudChatRouter` فقط enum پرووایدرها را به ترتیب ثابت می‌چرخاند (بدون فکر/قابلیت/هزینه/سلامت)؛ هیچ مدل تولیدیِ on-device نیست (فقط ONNX MiniLM برای embedding)؛ مش فقط «مغز» راه‌دور است، نه «مدلِ لوکالِ نود دیگر».
> 🎯 این کامل‌کننده‌ی **Phase 2 (Multi-Token Router)** + **Phase 8/8.5** + **WAN-Mesh W5** است. نگاشت ماژول‌های مستر اسپک (1–8, 17). طرح کامل: `repo/docs/superpowers/specs/2026-06-14-vision-brain-cognitive-router.md`.
> ✅ **هسته‌ی VB1–VB9 کامل و منتشر شد (v13–v20).** باقی‌مانده: LM1/LM3/LM4/LM5/LM6 (مدل لوکال) + MX (مش) + VB9.1.

### فاز VB — Cognitive Multi-Provider Router (مغزِ مسیریابی؛ ماژول 1–8) ✅ کامل
- [x] **VB1 Model Registry** (M3): `router/registry/` — `ModelSpec`+`CapabilityScores`+`ModelBackend`(CLOUD/LOCAL/MESH)+`Capability`؛ `ModelSeed` (۶ پرووایدر + Qwen2.5-0.5B لوکال)؛ `ModelRegistry` (@Singleton: `rankedFor`/`upsert`/`removeNode`/`setEnabled`/`applyRemote` JSON راه‌دور). ۸ تست سبز — v13 (versionCode 15)
- [x] **VB2 Capability Router** (M2): `router/capability/` — `CapabilityRequest` + `CapabilityRouter` (@Singleton): رتبه‌بندی Registry بر اساس قابلیت + فیلتر به مدل‌های reachable (CLOUD=توکن configured، LOCAL همیشه، MESH اگر حاضر)؛ `localOnly` (Privacy/Offline) + `best()`. ۶ تست سبز — `4210f2e`
- [x] **VB3 VisionOrchestrator** (M1): `router/orchestrator/` — `Intent`(۷تایی→Capability)+`Modality`+`DecisionObject`؛ `IntentClassifier` (heuristic on-device EN+FA + سیگنال‌های image/voice)؛ `VisionOrchestrator` (@Singleton): classify→CapabilityRequest→candidates→`DecisionObject` با reason برای VB9. خالص (توکن مصرف نمی‌کند؛ اجرا با VB8). مدل لوکال حتی با صفر توکن answerer می‌ماند. ۸ تست سبز — `b88d2f6`
- [x] **VB4 Availability Graph** (M8): `router/health/` — `AvailabilityGraph` (@Singleton، clock تزریقی): latency EWMA + error-rate EWMA + circuit-breaker (CLOSED→OPEN→HALF_OPEN، probe تک‌بار پس از cooldown) + cooldown با backoff نمایی و سقف، و احترام دقیق به `Retry-After` (429) via `RateLimited`. `BackendRouter` حالا قبل از dispatch `isAvailable` را چک و خروجی (latency/Retry-After) را record می‌کند → مدلِ ناسالم skip و substitution ادامه می‌یابد (`ALL_COOLING_DOWN` وقتی همه باز). ۱۲ تست graph + ۳ تست router سبز (build+test سبز). — v15 (versionCode 17)
- [x] **VB5 Smart Substitution Engine** (M4): `router/substitution/` — `SubstitutionEngine` (@Singleton) + `SubstitutionPolicy` (maxCloudAttempts/alwaysAppendLocal/keepMesh؛ DEFAULT و LOCAL_ONLY). از کاندیداهای رتبه‌بندی‌شده زنجیره‌ی `Primary→F1→F2→F3→Local→graceful` می‌سازد: dedup با حفظ ترتیب + سقفِ تلاش‌های cloud (MESH/LOCAL بی‌سقف) + drop مش در Privacy + **همیشه مدل on-device را به‌عنوان آخرین حلقه append می‌کند** (ضمانتِ «همیشه جواب می‌دهد»). `BackendRouter` حالا قبل از پیمایش، chain را از engine می‌سازد (localOnly→LOCAL_ONLY). ۹ تست سبز، build+test سبز. — v16 (versionCode 18)
- [x] **VB6 Token Pool upgrade** (M6): `data/ai/TokenPool` (@Singleton، clock تزریقی) — سلامت per-key (failures/cooldown/quota/lastUsed)؛ `order()` کلیدِ سالم‌ترین+کم‌مصرف‌ترین (LRU) را اول می‌دهد، کلیدِ cooling (401/429) را تا auto-recovery رد می‌کند (اگر همه cooling → soonest-first، هرگز گیر نمی‌کند). backoff نمایی؛ 401 خیلی طولانی‌تر از 429؛ `classify(message)` خطاها را تفکیک می‌کند. **secret-safe**: state با hashِ غیرقابل‌برگشتِ توکن کلید می‌خورد (توکن ذخیره/لاگ نمی‌شود). `CloudChatRouter.chatWith` به‌جای offset round-robin از `pool.order` + record استفاده می‌کند. ۱۱ تست سبز، build+test سبز. — v17 (versionCode 19)
- [x] **VB7 Adaptive Cost Controller** (M7): `router/cost/CostController` (@Singleton) + `CostMode` (Economy/Balanced/Premium/Unlimited با سقفِ unit per-request) + `CostDecision`. `estimate(spec,prompt,completion)` بر پایه‌ی `scores.cost` معکوس × حجم توکن (local همیشه رایگان)؛ `check()` گیتِ مود + سقف بودجه (Unlimited بای‌پس)؛ `record/budgetWarning()` هشدار ۸۰٪؛ `resetPeriod`. پیش‌فرض UNLIMITED → رفتار فعلی دست‌نخورده؛ seam برای router/UI (RD6 mode selector). ۱۱ تست سبز، build+test سبز. — v18 (versionCode 20)
- [x] **VB8 ProviderAdapter refactor** (M5): `router/backend/` — `Backend` interface + `BackendReply`؛ `CloudBackend` (→ `CloudChatRouter.chatWith` = توکن‌های کاربر از AiProviderStore)؛ `LocalBackend`/`MeshBackend` (stub با seam برای LM/MX)؛ `BackendRouter` (پیمایش کاندیداها، اولین موفقیت، substitution = proto-VB5؛ ctor مپ داخلی برای تست + @Inject برای Hilt). `CloudChatRouter.chatWith` استخراج شد. **HudViewModel.sendChat بازنویسی: gateِ brain-first حذف → orchestrator+backendRouter اول، brain فقط fallback.** ۶ تست + Hilt سبز — v14 (`0ceb410`)
- [x] **VB9 Decision telemetry در HUD** (M19): چیپِ «با کدام مدل/چرا» — `DecisionTelemetry` در HudScreen زیر خروجی؛ `HudUiState.decisionModel/decisionReason` از `orchestrator.decide` و سپس مدلی که واقعاً جواب داد (`r.model`) آپدیت می‌شود. طبق ui-ux-pro-max: status dot همیشه با لیبل (نه color-only)، reason دیم/تک‌خط truncate، توکن‌های Vision/JarvisColors، مونواسپیس برای id، بدون موشن اضافه (conditional render → سازگار با reduced-motion). build+test سبز. — v19 (versionCode 21)
  - [ ] **VB9.1 (follow-up)** dot سلامتِ per-provider از AvailabilityGraph/TokenPool (همراه ردیفِ پرووایدرها در RD6).

### فاز LM — Local AI Engine (مدل لوکالِ روی گوشی؛ ماژول 17) 🟠
- [ ] **LM1 Inference engine**: `LocalModelEngine` با llama.cpp (JNI، streaming + cancellation) یا MediaPipe LLM Inference API؛ پیاده‌سازی `ModelBackend`؛ thermal/thread guard. (native/NDK — روی این سرور build نمی‌شود؛ روی محیطِ دارای Android NDK.)
- [x] **LM2 Model packaging & download**: `router/local/` — `LocalModelCatalog` (Qwen2.5-0.5B-Instruct Q4 primary ~0.4GB Apache-2.0؛ Gemma 3 1B Q4 گزینه‌ی کیفیت) + `LocalModelManager` (@Singleton): دانلودِ on-demand **از داخل خود ویژن** (نه داخل APK) با **resume** (ادامه از `.part` با offset/Range)، **budget storage**، **progress (StateFlow)**، و **pin SHA256 fail-closed** (هش غلط → فایل پاک، اجرا نمی‌شود). seam شبکه (`ModelSource`) و dir فایل تزریق‌پذیر → کاملاً JVM-testable. id مدل = `ModelSeed.LOCAL_DEFAULT_ID` تا مدلِ seedـشده‌ی registry واقعی شود. ۸ تست سبز (download/verify/resume/budget/idempotent/delete/progress). build+test سبز. — v20 (versionCode 22)
  - [ ] **LM2.1 (follow-up)**: `ModelSource` واقعی با OkHttp Range + استپ «Offline Model Download» در Setup Wizard (B.5، UI) + ثبت readiness در registry.
- [ ] **LM3 Device-tier gating**: انتخاب واریانت بر اساس RAM/CPU (tierهای Brain-Nano/Lite/Full)؛ روی RAM کم هشدار/مدل کوچک‌تر. load on-demand + unload پس از idle.
- [x] **LM4 Hybrid routing** (v23، 2026-06-15 auto-rp): `SubstitutionPolicy.PREFER_LOCAL` + بازچینشِ local-firstِ پایدار در `SubstitutionEngine`؛ `BackendRouter` در مودِ Economy این پالیسی را برمی‌گزیند (تزریقِ `CostController`)، Privacy/Offline همچنان `LOCAL_ONLY`. مدلِ لوکال در registry با `local=true` هست (seed) و وقتی هیچ ابری reachable نیست خودبه‌خود candidate می‌شود. ۵ تست (۳ engine + economy/balanced router). [بقیه‌ی LM: LM1/LM3 نیازِ NDK/دستگاه].
- [ ] **LM5 Privacy Mode**: اجبارِ local-only (بدون خروجِ ابری)، نمایش در Trust gate.
- [ ] **LM6 Graceful no-model**: اگر نه توکن ابری و نه مدل لوکال هست → CommandInterpreter جواب می‌دهد + پیشنهاد دانلود مدل لوکال (جلوگیری از رگرسیونِ «صحبت نمی‌کند»). (هسته در `HudViewModel.sendChat` پیاده است.)

### فاز MX — Mesh Model Exchange (استفاده از مدل‌های لوکالِ دستگاه‌های دیگر؛ M17 + Phase 8) 🟡
- [ ] **MX1 Node model advertisement**: هر نودِ مش مدل‌های لوکالش (مثلاً tagهای Ollama) را کنار CPU/RAM/GPU در heartbeat/registry اعلام کند (توسعه‌ی `NodeMetrics`).
- [ ] **MX2 Mesh model discovery**: `agent.py`/Brain مدل‌های Ollama/llama.cpp روی سرور را شمارش و گزارش کند؛ گوشی آن‌ها را با `backend=mesh` وارد Registry کند.
- [ ] **MX3 MeshModelClient** (`ModelBackend`): ارسال inference به `/chat` نودِ منتخب با هدف مدل لوکالِ آن؛ انتخاب نود با **BrainScore × AvailabilityGraph**.
- [ ] **MX4 Orchestrator mesh routing**: taskهای سنگین/خصوصی به مدلِ لوکالِ قوی‌ترین نودِ مش قبل/به‌جای ابری (تکمیل WAN-Mesh **W5**).

> 📌 **یادداشتِ اولویتِ تاریخی (کاربر 2026-06-14):** «هیچ قابلیتی بدون Brain v1 کار نمی‌کند» — ترتیبِ آن زمان: VB1→VB2→VB3→VB8→VB4 → VB5–VB7 → LM1→LM2 → RD/FNT/VAL → MX. **اکنون VB کامل است؛ پس طراحیِ بصری (RD/FNT) اولویتِ بازِ بعدی است (بخش ۱).**

### 🗺️ MODULE MAP — VISION BRAIN V1 (M1–M20) → تسک/فاز (پوشش کامل، ممیزی 2026-06-14)
> تأیید اینکه هر ۲۰ ماژولِ مستر اسپک جایی در PLAN/ROADMAP تسک دارد. **NEW** = این جلسه اضافه شد · **موجود** = قبلاً در roadmap بود.
| M | ماژول | نگاشت | وضعیت |
|---|-------|-------|-------|
| 1 | Orchestrator Brain | **VB3** | ✅ |
| 2 | Capability Router | **VB2** | ✅ |
| 3 | Model Registry | **VB1** | ✅ |
| 4 | Smart Substitution | **VB5** | ✅ |
| 5 | Provider Router | **VB8** (ModelBackend) | ✅ |
| 6 | Token Pool Manager | **VB6** (روی AiProviderStore) | ✅ |
| 7 | Adaptive Cost Controller | **VB7** | ✅ |
| 8 | Availability Graph | **VB4** | ✅ |
| 9 | Memory Engine | Agent-OS **A1** + ROADMAP **P9** (Short/Long/Episodic/Semantic + scoring) → **CF4** | موجود |
| 10 | Goal Engine | Agent-OS **A4** (Goal System + Autonomous Planner) → **CF3** | موجود |
| 11 | Agent Framework | `AgentRegistry` موجود + ROADMAP **P5 A.2** Agent Pool → **AGSK** | موجود (partial) |
| 12 | Multi-Agent Coordinator | Agent-OS **B1** + ROADMAP **P5 A.3** → **CF1/AGSK** | موجود |
| 13 | Android Agent Layer | toolهای موجود (Nav/Notif/DeviceSettings/Call/SMS) + ROADMAP **P7.5** + Agent-OS **C1** + **PAU** | موجود (partial) |
| 14 | Workflow Engine | Agent-OS **B2** + ROADMAP **P9.5** Automation → **CF5** | موجود |
| 15 | Voice OS | ROADMAP **P7** (wake-word/streaming هنوز باز → **FV4**) | موجود (partial) |
| 16 | Multimodal Engine | ROADMAP **P14** Vision System + P7.5 Capture/OCR | موجود |
| 17 | Local AI Engine | **LM1–6** + **MX1–4** (mesh) | NEW 🟠 |
| 18 | Trust & Security | ROADMAP **P4.5** Trust + **P13** Privacy Monitor (موجود) + **VB**-aware | موجود |
| 19 | HUD System | **RD1/RD2** orb + **VB9** decision telemetry؛ thinking-indicator/floating = **C0.1** ambient | موجود/✅ |
| 20 | Marketplace | ROADMAP **P17** + Agent-OS **B3** Plugin Marketplace | موجود (future) |
> جمع‌بندی: M1–8 = ✅ (شیپ شد)، M17 = NEW 🟠. M9–16, 18–20 قبلاً در Agent-OS roadmap + فازهای P5/P7/P9/P13/P14/P17 تسک داشتند. **هیچ ماژولی بدون تسک نمانده.**

---

# بخش ۳ — 🗺️ FULL PROGRAM — همه‌ی فازها تا لانچ نهایی
> تمام تسک‌هایی که تا انتهای پروژه باید انجام شوند. هر فاز که کامل شد → push + build + ثبت در report → فاز بعد.
> منبع کامل: `repo/ROADMAP.md`.

## ✅ Phase 0 / X — Foundation & Dev Tooling — DONE
- [x] CI (android ktlint + brain ruff/mypy/pytest) سبز · ADR-001..011 · structured logging · health probes
- [ ] Fix Caddy `/ws/*` upgrade headers (باگ معلق ۴۰۴ — فقط برای mesh راه‌دور)

## ✅ Phase 1 — Flexible Brain Core — DONE
- [x] BrainLiteService (ForegroundService :7799) · Ktor ۱۰ endpoint · Room (memories/nodes/tasks) · Election+failover
- [ ] pin واقعی MODEL_SHA256 + tokenizer WordPiece برای OnnxEmbedder (فعلاً fallback)
- [ ] `/chat` streaming SSE · WakeLock per-request

## ✅ Phase 2 — Multi-Token AI Router — DONE
- [x] CloudChatRouter (Anthropic/Gemini/OpenAI/xAI/Groq/OpenRouter) · multi-key rotation روی 401/429 · usage store · trust gate

## ✅ Phase 3 — Android Launcher MVP — DONE
- [x] HOME launcher · App Drawer (QUERY_ALL_PACKAGES) · HUD «Eye of Vision» · QuickPanel · SettingsHub · adaptive icon

## ✅ Phase 4 — Privacy/Trust Gate — DONE
- [x] Trust levels (Sovereign/Balanced/Open) · EncryptedSharedPreferences · token-echo handshake

## 🟢 Phase 7 — Adaptive Voice & Persona + Universal Language — DONE (v2..v4)
- [x] STT/TTS on-device · persona rate/pitch · main-thread safety
- [x] **چت قابل‌ارسال** (SEND button + IME) — v3
- [x] **Persona** (name/humor/formality/length) + dynamic system prompt — v3
- [x] **چندزبانه**: FA→FA / EN، language setting، Persian-voice TTS، STT locale — v4
- [x] **پاسخ به اسم** (wake-name strip، قابل تغییر) — v4
- [x] **Onboarding اولِ راه‌اندازی** (name/language/voice/trust) — v4
- [ ] Wake-word همیشه‌روشن واقعی (Porcupine/Vosk hotword) — «Hey Vision» بدون باز کردن اپ → **FV4**
- [ ] انتخاب صدا (voice picker) + پیش‌نمایش TEST VOICE → **FV2**
- [ ] تشخیص خودکار زبان ورودی صوت (multi-locale STT) → **FV3**

## 🟢 Phase 5 — Agentic Reasoning Core (Tool System + Planner) — v1 شروع شد (v5)
- [x] `data/tools/Tool.kt` + `ToolResult.kt` + `ToolRegistry.kt` (انتزاع ابزار) — v5
- [x] Tools: Flashlight (torch) · DeviceSettings (Wifi/Bluetooth/Brightness/Airplane/Data panels) — v5
- [x] wired در CommandInterpreter (قبل از AI) — v5
- [x] **CallTool/SmsTool واقعی + ContactResolver** (اجرا یا گزارشِ صادقانه) — v21 (`0c017db`)
- [ ] App tools: CloseApp/Recents/Back/Home (نیازمند Accessibility — Phase 7.5)
- [ ] `core/agent/AgentEngine.kt` — Goal→Plan→Tool→Execute→Respond → **CF1**
- [ ] `core/planner/{IntentClassifier,TaskPlanner,ActionPlan}.kt` — تسک چندمرحله‌ای → **CF3**
- [ ] LLM tool-calling: تبدیل پاسخ مدل به فراخوانی ابزار (function-calling JSON) → **CF2/FA3**
- [ ] تست‌های واحد برای registry/planner

## 🔵 Phase 6 — AnySearch + Timeline + Notes
- [x] جستجوی حافظه‌ی مکالمه در App Drawer (v1)
- [ ] AnySearch: یک‌کاسه‌کردن apps + contacts + files + memory + web
- [ ] Vision Timeline (تاریخچه‌ی فعالیت قابل‌جستجو، Room) → **MON1**
- [ ] Vision Notes (یادداشت سریع + embedding + بازیابی)

## 🔵 Phase 1.5 — Fast File Transfer / VISN Protocol
- [ ] انتقال فایل دستگاه‌به‌دستگاه روی LAN (chunked, resume)
- [ ] UI انتقال + پیشرفت + تأیید امنیتی

## 🟢 Phase 7.5 — Vision Capture + Context Cards + Accessibility — v1 شروع شد (v6)
- [x] `VisionAccessibilityService` (Home/Back/Recents/Lock) + config xml + manifest + enable-row در Settings — v6
- [x] `NavigationTool` (home/back/recents/lock، EN/FA) wired در ToolRegistry — v6
- [x] `VisionNotificationService` (خواندن نوتیف‌ها) + `NotificationTool` + ردیف Notification Access — v7
- [ ] Click/Scroll/InputText (تعامل با المان‌ها) · پاسخ/حذف نوتیف · Overlay/FloatingOrb → **PAU/PAW/PAO**
- [ ] `JarvisNotificationService` (خواندن/پاسخ/حذف نوتیف)
- [ ] OverlayService + FloatingOrb (دستیار شناور همه‌جا) → **PAO**
- [ ] MediaProjection screenshot + ML Kit OCR → Context Cards
- [ ] runtime permission manager (storage/media/notif/overlay) → **PERM**

## 🔵 Phase 8 — Device Mesh & Node Network — partial
- [x] mDNS discovery · election · heartbeat · QR/URI pairing
- [ ] mesh چندنود واقعی (آدرس LAN واقعی به‌جای 127.0.0.1) · هماهنگی task بین نودها
- [ ] node garbage-collection (پاک‌سازی نودهای آفلاین قدیمی)

## 🔵 Phase 9.5 — Vision Tutor + Automation Builder
- [ ] Automation Engine: `Workflow/Trigger/Action` (مثل «باتری<۲۰٪ → Power Save») → **CF5**
- [ ] سازنده‌ی workflow بصری
- [ ] Vision Tutor (راهنمای تعاملی درون‌اپ)

## 🔵 Phase 10 — Digital Twin & Self-Learning + Smart Power
- [ ] حافظه‌ی بلندمدت کاربر (ترجیحات/الگوها) — Room: user_memory → **CF4**
- [ ] یادگیری عادت‌ها + پیشنهاد پیش‌دستانه → **PAS**
- [ ] Smart Power (مدیریت باتری/پس‌زمینه)

## 🔵 Phase 11 — OS-Level Integration + Conversational OS
- [ ] کنترل کامل دستگاه با زبان طبیعی (روی Accessibility) → **PAU**
- [ ] دستورات OS محاوره‌ای چندمرحله‌ای
- [ ] جایگزینی دستیار پیش‌فرض (Assistant role)

## 🔵 Phase 11.5 — Focus Mode + Broadcast
- [ ] Focus Mode (محدودسازی نوتیف/اپ‌ها)
- [ ] Broadcast (هم‌رسانی وضعیت بین دستگاه‌ها)

## 🔵 Phase 12 — MCP & Plugin Ecosystem + Widget API — partial
- [x] VisionPlugin interface + PluginRegistry + SystemInfoPlugin (v1)
- [ ] بارگذاری پلاگین خارجی + sandbox مجوزها → **AGSK**
- [ ] MCP client (اتصال به ابزارهای بیرونی)
- [ ] Widget API (ویجت‌های خانه)

## 🔵 Phase 13 — Zero-Trust Security + Privacy Monitor — partial
- [x] Privacy Monitor (شمارش cloud calls) · trust gate
- [ ] Behavioral baseline (تشخیص ناهنجاری)
- [ ] رمزنگاری end-to-end mesh · audit log

## 🔵 Phase 14 — Vision System (Screen Understanding)
- [ ] VisionManager + ScreenshotProvider + OCRManager + ImageAnalyzer
- [ ] Gemini/Claude Vision (تحلیل تصویر و صفحه)

## 🟣 Phase 18 — Marketing & Launch (مداوم از P3+)
- [ ] صفحه‌ی فرود + ویدیوی معرفی · انتشار Play/Direct APK · کانال آپدیت · onboarding عمومی ۱۰۰۰ کاربر اول (M4)

---

# بخش ۴ — 🧠 AGENT OS ROADMAP + 🌐 WAN MESH

## 🧠 AGENT OS ROADMAP — 2026-06-14 (جهت‌گیری راهبردی کاربر: نه کپی Omi، بلکه «Agent OS» اندروید)
> هدف: تبدیل Vision از «AI Launcher» به یک **Agent Operating System** کامل برای اندروید — حوزه‌ای که فعلاً رقیب مستقیم قوی ندارد. Omi روی «حافظه انسان» تمرکز دارد؛ Vision باید حافظه + گراف دانش + برنامه‌ریز خودمختار + چندایجنتی + نظارت پیش‌دستانه را با کنترل دستگاه/سرور ترکیب کند.
> 📄 تعریفِ کاملِ محصول: `repo/docs/VISION-CAPABILITIES.md` (۲۴ قابلیت + Ultimate Command).

### ⭐ Top-5 (بیشترین ارزش — اول این‌ها)
1. **Memory Engine** (Short/Long/Episodic) — حافظه بلندمدت واقعی
2. **Personal Knowledge Graph** — گراف افراد/پروژه‌ها/سرورها/سرویس‌ها
3. **Goal & Planning System** — Goal → Phases → Tasks → Subtasks → Dependencies (Autonomous Planner)
4. **Multi-Agent Architecture** — Brain / Memory / Device / Server / Automation agents
5. **Proactive Assistant** — هشدار خودکار (مثلاً «دیسک VPS ۹۵٪»، «کانتینر کرش کرد»)

### فاز A (بسیار مهم) — Memory & Reasoning Core
- [ ] **A1 Memory Engine**: لایه‌های Short-Term (مکالمات/وضعیت/تسک‌های فعال)، Long-Term (علایق/پروژه‌ها/افراد)، Episodic (رویدادهای تاریخ‌دار). API: `remember()/recall()/forget()`؛ ذخیره در brain (Room) + on-device. هدف: «آخرین بار روی پروژه VPN چه کردم؟» (→ **CF4**)
- [ ] **A2 Knowledge Graph**: گره‌ها (Person/Project/Server/Service/Token) + یال‌ها؛ استخراج خودکار از مکالمات؛ کوئری روابط.
- [ ] **A3 Universal Search**: جستجوی یکپارچه روی پیام‌ها/فایل‌ها/نوت‌ها/حافظه/اپ‌ها/سرورها (`find: …`). (نقشه‌برداری به Phase 6 موجود)
- [ ] **A4 Goal System + Autonomous Planner**: Goal را خودکار به فاز/تسک/زیرتسک با وابستگی بشکند و progress را دنبال کند. (→ **CF3**)
- [ ] **A5 Agent Timeline**: Time-Machine رویدادها (Today/Yesterday/Last Week/Month). (→ **MON1**)

### فاز B — Orchestration & Intelligence
- [ ] **B1 Multi-Agent Architecture**: ایجنت‌های Brain/Memory/Device/Server/Automation + router هماهنگ‌کننده. (هم‌راستا با KERNEL workspace) (→ **AGSK/CF1**)
- [ ] **B2 AI Workflow Builder**: When→Then بدون کد (مثلاً «پیام تلگرام → ذخیره در Notion → خلاصه → ارسال به Discord»).
- [ ] **B3 Plugin Marketplace**: قوی‌تر از Omi — GitHub/Docker/Telegram/Discord/Notion/Home-Assistant/OpenRouter/Ollama. (توسعه Phase 12)
- [ ] **B4 Semantic Notification System**: تحلیل نوتیف‌ها → Important/Medium/Ignore.
- [ ] **B5 Personal Dashboard**: Goals/Memory/Agents/Servers/Tasks/Automations/Plugins.

### ⭐ فاز C0 — Always-On Ambient Presence (اولویت بالا — کاربر 2026-06-14)
> Vision نباید فقط در صفحه‌ی اصلی فعال باشد. باید **همیشه و همه‌جا در دسترس** باشد، اپ‌های در حال استفاده را ببیند، و **پیش‌دستانه پیشنهاد بدهد و تعامل کند**. (→ بلوک PROACTIVE/AMBIENT بخش ۱)
- [ ] **C0.1 Persistent ambient service**: foreground service همیشه‌فعال + overlay/bubble سراسری (SYSTEM_ALERT_WINDOW یا Bubble API) تا Vision از داخل هر اپ یک‌لمسی در دسترس باشد، نه فقط در launcher. (→ **PAO**)
- [ ] **C0.2 Foreground-app awareness**: با AccessibilityService اپِ پیش‌زمینه و context را تشخیص بده (کدام اپ، چه صفحه‌ای).
- [ ] **C0.3 Proactive suggestions**: بر اساس اپ/زمان/رویداد پیشنهاد بده (مثلاً داخل مرورگر → «خلاصه کنم؟»، نوتیف مهم → اقدام). با C4 Proactive یکی می‌شود. (→ **PAS**)
- [ ] **C0.4 Always-listening wake** (اختیاری/قابل‌خاموش): فعال‌سازی صوتی سراسری با حریم‌خصوصی محلی. (→ **FV4**)

### 🐞 BUG (کاربر 2026-06-14) — منوی Recents/برنامه‌های باز داخل Vision نمی‌آید
- [x] وقتی Vision لانچرِ فعال است، منوی «recently apps / برنامه‌های باز» ظاهر نمی‌شود (محدودیت لانچر شخص‌ثالث + gesture nav). راهکار: پنل Recents داخل‌اپ با `UsageStatsManager` (آخرین اپ‌ها) + دکمه/ژست اختصاصی، یا فراخوانی `GLOBAL_ACTION_RECENTS` از AccessibilityService. → رفع در v10.

### فاز C — Action & Reach
- [ ] **C1 Android Automation Engine**: باز کردن اپ/کلیک/پر کردن فرم/Workflow با زبان طبیعی (مثل Tasker/AutoInput اما NL). روی AccessibilityService موجود. (→ **PAU**)
- [ ] **C2 Server Control Center**: SSH/Docker/K8s/VPS — «سرور فرانکفورت رو ری‌استارت کن».
- [ ] **C3 Shared/Org Brain**: Personal → Shared (تیمی) → Organization.
- [ ] **C4 Proactive Assistant**: نظارت پیش‌دستانه بر دستگاه/سرور/اپ‌ها و هشدار خودکار بدون دستور. (→ **PAS/MON**)
- [ ] **C5 AI App Launcher**: به‌جای آیکن‌ها، «با علی تماس بگیر» → Vision بهترین اپ را انتخاب و اجرا کند. (✅ Call/SMS در v21)

### فاز D — Ambient / Future
- [ ] **D1 Meeting & Voice Intelligence**: ضبط تماس/جلسه → خلاصه + استخراج Task/تصمیم.
- [ ] **D2 Wearable Integration**: Smart Glass / Watch / Earbuds.
- [ ] **D3 Offline Personal AI**: مدل on-device برای استقلال کامل. (→ **LM**)

> نگاشت به فازهای موجود: A3↔Phase6 · B3↔Phase12 · C1↔Phase9.5/7.5 · D1↔Phase6.

## 🌐 WAN MESH — Remote Server Nodes over Internet (BUG+CAPABILITY، کاربر 2026-06-14)
> کاربر: «چندین سرور دارم می‌خواهم منابعشان به Vision روی گوشی اضافه شود؛ مش فقط وقتی هر دو در یک Wi-Fi باشند کار می‌کند (آن هم خراب است)؛ کدِ add-device برای دستگاهی‌ست که Vision دارد نه سرور خالی.»

### 🐞 تشخیص ریشه‌ای (از خواندن کد)
1. **Brain روی `127.0.0.1:7799` bind می‌شود** (`BrainLiteService.kt:64 address="127.0.0.1:7799"`) → مغزِ گوشی از هیچ دستگاه دیگری قابل‌دسترس نیست، حتی در همان Wi-Fi. علتِ «حتی same-WiFi هم کار نمی‌کند».
2. **Discovery فقط mDNS/NSD است** (`NsdDiscovery`) → ذاتاً LAN-only؛ روی اینترنت کار نمی‌کند.
3. **گوشی پشت NAT اپراتور** است (بدون IP عمومی/پورت ورودی) → یک سرور راه‌دور هرگز نمی‌تواند به مغزِ گوشی POST کند. مدل فعلی «سرور به گوشی می‌پیوندد» برای اینترنت غیرممکن است.
4. **`node-agent/install.sh` کهنه/خراب است**: IP هاردکد `212.87.199.62`، برند «JARVIS»، `ws://…:8000/node/connect` و `pip install websockets psutil` — در حالی که `agent.py` به HTTP/stdlib روی `:7799` بازنویسی شده. install.sh با agent.py در تضاد است.
5. اسکریپتِ in-app (`PairDeviceSection`) درست است (فقط python3 لازم دارد، نه Vision) اما به `--host <LAN-ip-گوشی>:7799` اشاره می‌کند که به‌خاطر bind لوکال‌هاست و NAT کار نمی‌کند.

### ✅ قابلیتِ لازم — WAN Mesh / Remote Server Brain (معماری معکوس)
چون سرورها همیشه‌روشن و دارای IP عمومی‌اند و گوشی پشت NAT است، جهت اتصال باید **برعکس** شود:
- [ ] **W1 Bind انتخابی**: وقتی «Accept remote nodes» روشن است، Brain روی `0.0.0.0:7799` bind شود (نه فقط loopback) + هشدار امنیتی + توکن اجباری.
- [ ] **W2 Server-hosted node/brain**: یک نصبِ تک‌خطی واقعی برای سرورِ خالی لینوکس (بدون Vision/اندروید): `curl -fsSL <repo>/node-agent/install.sh | bash -s -- --host <addr> --token <t>` که `agent.py` را می‌گیرد، یک systemd unit می‌سازد، و منابع (CPU/RAM/GPU/disk) سرور را به Brain اضافه می‌کند. install.sh کهنه بازنویسی شود (HTTP/:7799/stdlib، بدون pip، بدون IP هاردکد، برند Vision).
- [ ] **W3 اتصال خروجی/Relay برای عبور از NAT**: یا (الف) گوشی به‌صورت خروجی به مغزِ سرورِ عمومی وصل شود (BrainSelectionStore الان host پویا را پشتیبانی می‌کند → افزودن سرور با host:port+token)، یا (ب) یک relay/rendezvous سبک (یا تونل مثل Cloudflare Tunnel/WireGuard از پروژه 01-kian-v2ray) تا مغزِ گوشی از بیرون قابل‌دسترس شود.
- [ ] **W4 افزودن سرور دستی در UI**: به‌جای فقط QR/mDNS، فرم «Add remote server» با host/port/token (اینترنت) + تست اتصال + نمایش منابع سرور.
- [ ] **W5 استفاده از منابع سرور**: مسیریابی کارهای سنگین (LLM/compute) به نودِ سرور با بهترین BrainScore؛ سرور به‌عنوان provider محاسبات. (هم‌راستا با Agent-OS «C2 Server Control Center» و «C3 Shared/Org Brain».)
- [ ] **W6 امنیت**: توکن چرخشی، TLS برای اتصال WAN، allowlist، و عدم افشای منابع بدون pairing.

> نگاشت: این کامل‌کننده‌ی **Phase 8 (Device Mesh — partial)** است و پایه‌ی **C2/C3** در Agent-OS roadmap. اولویت: بالا (کاربر فعال می‌خواهد منابع سرورها را استفاده کند).

---

# بخش ۵ — مرجع و تاریخچه (انجام‌شده / DONE)

## 🎯 Milestoneها
| M | هدف | معیار |
|---|-----|-------|
| M0 | Foundation Ready | WebSocket رفع، اسکلت push، n8n حذف، CI سبز |
| M1 | Brain-Lite MVP | Vision روی گوشی بدون VPS |
| M2 | Multi-Provider AI | افزودن چند key، Router هوشمند |
| M3 | Agentic Alpha | agentها وظایف ساده انجام می‌دهند |
| M4 | Beta Launch | نسخه عمومی ۱۰۰۰ کاربر اول (پایان فاز ۷) |

## ✅ USER DIRECTIVES — 2026-06-12 (انجام‌شده)
- [x] حذف کامل برند Jarvis از UI (لیبل اپ، TopBar «JARVIS v4.1.0» → «VISION v16.0.0») — `b8166c6`
- [x] BrainRepository: حذف سرور hardcode مرده → آدرس داینامیک از BrainSelectionStore، حذف WebSocket — `b8166c6`
- [x] ثبت به‌عنوان لانچر (HOME intent) از ابتدا — `b8166c6`
- [x] status bar: edge-to-edge + systemBarsPadding تا تداخل کادرها رفع شود — `b8166c6`
- [x] **Design overhaul** (Eye of Vision HUD + dock + icon — 2e75c5e/f6f72fa)
- [x] **Vision QuickPanel اختصاصی** (overlay روی HUD — 40ed110)
- [x] **Standalone از v1** (CloudChatRouter + trust gate)
- [x] **AI provider tokens از v1** (multi-token + rotation + xAI — c6677d7)
- [x] **Voice از v1** (STT/TTS + persona rate/pitch — 009925f)

## ✅ Sprint Phase 0 + PX — انجام‌شده
- [ ] Fix Caddy `handle /ws/*` + Upgrade/Connection headers — رفع 404
- [x] Push skeleton: `android/ brain/ node-agent/ sdk/ docs/{architecture,api,adr,user-guide}`
- [x] Remove n8n → Temporal Workflow Engine (compose: temporal + temporal-ui)
- [x] GitHub Actions: workflow android (ktlint) + brain (Ruff/mypy/pytest) — سبز `0ef77d4`
- [x] detect-secrets pre-commit (baseline تمیز) — `7117175`
- [x] Branch protection روی `main` — ruleset «protect-main» توسط کاربر فعال شد (2026-06-11)
- [x] ADR-001..011 در `docs/adr/` — `91deda0`
- [x] Structured logging (JSON) + `/health/live` `/health/ready` — probeها + JSON logging ✅ — `13230ad`

## ✅ Phase 1 (هفته ۲–۴) — انجام‌شده
- [x] BrainLiteService.kt (ForegroundService, پورت ۷۷۹۹, START_STICKY) — `ead4b70`
- [x] Ktor server — ۱۰ endpoint — ۱۶ تست سبز — `0d40eb1`
- [x] Room DB سازگار با schema برین‌فول (memories/nodes/tasks) — `8037d95`
- [x] Brain Score Calculator (الگوریتم election + failover) — ۷ تست سبز — `b5a51ea`
- [x] Gradle wrapper استاندارد بازسازی شد (jar + اجرای محلی سالم) — `b5a51ea`
- [x] Brain Election UI (Compose، تم HUD، override دستی) — `c7037ee`
- [x] Setup Wizard ۴ مرحله‌ای (Welcome/Discovery/Connect/Done) — `c7037ee`
- [x] اتصال Election UI به node registry واقعی (Room Flow + NodeMetricsCodec + freshness 90s) — `0276893`
- [x] navigation: اتصال SetupWizard و BrainElection به MainActivity — `049fe4e`
- [x] Discovery transport format: JoinPayload (`vision://join?host&port&token`) + parse در SetupWizard — `eb122f7`
- [x] NSD/mDNS: NsdDiscovery (advertise `_visionbrain._tcp` + scan→BrainCandidate) + advertise در BrainLiteService — `94fb20c`
- [x] scan در SetupWizard (DiscoveryScanner→candidates در state) + handshake واقعی `/health` (FAILED روی برینِ در دسترس‌نبودن) — `81c7073`
- [x] UI لیست candidates در SetupWizardScreen (HUD، scanning pulse، tap-to-select) + CONNECT=retry روی FAILED — `1867580`
- [x] QR pairing screen: بخش PAIR NEW DEVICE در Election (QR render + URI متن) — `a16f7fa`
- [ ] QR scanner (CameraX/ML Kit) — برای لانچ آزمایشی لازم نیست (paste URI کار می‌کند)
- [x] ذخیره brain منتخب (BrainSelectionStore/SharedPreferences) + HeartbeatSender → brain ذخیره‌شده — `820459b`
- [x] QR generator سمت برین (QrPairing + PairDeviceSection + LocalPairingInfoProvider) — انجام شده
- [x] سمت سرور heartbeat: id پایدار در POST /nodes → REPLACE همان ردیف، last_seen تازه می‌شود — `a9705b9`
- [x] heartbeat sender سمت کلاینت (HeartbeatSender، ۳۰s، id پایدار در SharedPreferences، wired در BrainLiteService) — `f6f1307`
- [x] HeartbeatSender → آدرس برینِ elected (wired در BrainLiteService:65) — انجام شده
- [x] networkMbps واقعی (WifiManager link speed) — این کامیت

## 🟡 Follow-upهای Brain-Lite (از اجرای plan)
- [ ] pin واقعی MODEL_SHA256 برای MiniLM ONNX (فعلاً fail-closed)
- [ ] tokenizer واقعی WordPiece برای OnnxEmbedder (فعلاً byte-level fallback)
- [ ] WakeLock per-request + اندازه‌گیری باتری
- [ ] /chat streaming SSE
- [x] CI Android: job unit-tests فعال است (gw :app:testDebugUnitTest)
- [~] طراحی UI: v8 — بازطراحی لانچر (TopBar پرتره تمیز + ساعت بزرگ، اپ‌های پرتکرار روی هوم دور چشم + کاشی ALL)
- [ ] طراحی بیشتر: ویجت‌ها/نوتیف دور چشم به‌صورت مداری، پوشه‌ی اپ کنار چشم

## 🧱 اصول کد (PX)
```
Android : Kotlin 2.0 · Compose · Clean Arch + MVI · Hilt · Coroutines/Flow · Detekt/ktlint · JUnit5+Turbine+Mockk
Brain   : Python 3.12 · FastAPI · Hexagonal · Pydantic v2 · SQLAlchemy 2 async · Alembic · Ruff/mypy · pytest (cov ≥۷۰٪)
```

## 🔐 Security follow-ups (از code review — shift#12) — انجام‌شده
- [x] EncryptedSharedPreferences (AES256 Keystore) + allowBackup=false — `40e1fda`
- [x] صف resolve در NsdDiscovery — `aacf583`
- [x] lazy scan فقط در step discovery — `aacf583`
- [x] token-echo handshake (X-Pair-Ack=sha256) — `4680710`
- [x] رفع‌های فوری review: commit() node_id، CancellationException rethrow، callback خارج از lock، first-occurrence params، state capture — `deca3c0`

## ✅ BUG-AI (رفع شد در v14 — `0ceb410`) — «از توکن‌ها استفاده نمی‌کند»
- [x] **ریشه (از کد):** در `HudViewModel.sendChat` وقتی `brainOnline` → اول `brain.chat()` (Brain-Lite فقط Groq با کلید جدا) صدا زده می‌شد و توکن‌های `AiProviderStore` کاربر دور زده می‌شدند.
- [x] **رفع اصولی (انجام شد):** **VB3 (Orchestrator) + VB8 (ModelBackend)** پیاده شد — `HudViewModel.sendChat` حالا: command لوکال → `orchestrator.decide` (privacy=SOVEREIGN) → `backendRouter.execute` (توکن‌های کاربر/لوکال/مش، رتبه‌بندی‌شده بر اساس قابلیت) → brain فقط fallback (در SOVEREIGN رد می‌شود). توکن‌های کاربر حالا از طریق `CloudBackend→CloudChatRouter.chatWith` واقعاً استفاده می‌شوند. ✅ لازم به تست روی دستگاه با کلید واقعی.

## ✅ VAL — تست زنده‌ی توکن هنگام افزودن — انجام‌شده
- [x] **VAL1** (در `c85eabc`) — `AiTokensViewModel.validate` هنگام افزودن توکن `CloudChatRouter.test(p, token)` را می‌زند؛ `KeyStatusBadge` نشانِ ✓ ACTIVE / ⟳ TESTING / ✗ FAILED کنار همان توکن. (تأییدشد 2026-06-15)
- [x] **VAL2** — وضعیت per-token (TESTING/ACTIVE/FAILED) + دلیلِ خطا (پیام exception، شامل 401/429/شبکه، تا ۷۰ کاراکتر) + دکمه‌ی TEST (retest)؛ در `_status` ماندگار تا تست بعدی. (تأییدشد 2026-06-15)

## ❌ SK1/SK2 — SEED-KEYS (CANCELLED توسط کاربر 2026-06-15)
- [~] کاربر گفت «بیخیال کلید فعال در نسخه منتشرشده شدم، اون جریان رو کنسل کن». ضمناً safety classifier هم baking کلید OpenRouter در APK پابلیک را hard-block می‌کرد (حتی با اجازه‌ی کاربر). دیگر دنبال نمی‌شود. (اگر روزی لازم شد: راهنمای دستی در report shift32؛ بهتر است از proxy استفاده شود نه baked key.)

## ✅ FIX-ACTIONS — رفع باگ بحرانی v20 (v21) — انجام‌شده
- [x] **FA1 CallTool واقعی** + **FA2 SmsTool واقعی** + `ContactResolver` — اجرا یا گزارشِ صادقانه، نه ادعای دروغ؛ مجوزهای `CALL_PHONE`/`SEND_SMS`/`READ_CONTACTS`؛ ۶ تست EN/FA parsing سبز؛ ToolRegistry: call/sms اول از LLM. — v21 (`0c017db`, release `c2e7454`)
- [ ] **FA3 ToolCaller** (هسته‌ای) → بخش ۱ / CF2.

## 📦 Release Ledger (version-driven tags)
- [x] v2 — design overhaul + multi-token + voice base
- [x] v3 — send button + offline commands + fast cloud fallback + persona + dynamic prompt
- [x] v4 — onboarding + multilingual + responds-to-name
- [x] v5 — Tool System v1 (flashlight/settings) + **zero-config chat** (BuildConfig.GROQ_KEYS seed + CI secret + clear no-key message) + **eye-blink rectangle bugfix**
- [x] v6 — Phase 7.5 Accessibility v1: device control (Home/Back/Recents/Lock) + NavigationTool + enable-row
- [x] v7 — Notification access (read notifications when asked) + NotificationTool
- [x] v8 — launcher redesign: clean portrait top bar + big clock, frequent apps on home around the eye + ALL tile
- [x] v9 — **fix(ai): structural "won't talk" bug** — OpenRouter `"auto"`→`"openrouter/auto"` (was HTTP 400) + **per-provider model selection** (AiProviderStore.model/setModel + AiTokensScreen model field) + OpenRouter HTTP-Referer/X-Title headers — commit `5db4293`
- [x] v10 — **feat(launcher): in-app Recents/open-apps panel** — UsageStatsManager-backed RecentsScreen + RECENTS dock button + VisionRoute.RECENTS + PACKAGE_USAGE_STATS + grant-access prompt (fixes "recents menu doesn't appear inside Vision") — commit `f81c2e5`
- [x] v11 — **feat(appearance): Theme & Appearance Engine** — 3 live themes (Deep Space/Aurora Dark/Light Future) + accent picker (8 presets + AUTO + #hex) + wallpaper picker + global animation toggle + distributed brain badge + show/hide + reset appearance; `VisionColors` snapshot-state backed (all screens re-theme live) + `ThemeStore` persistence; new APPEARANCE settings section. build+test green.
- [x] v11.1 hotfix — gear صریح در top bar (بازخورد کاربر: تنظیمات پیدا نمی‌شد) — `0a91b4e`
- [x] v12 — HOME REDESIGN to orb-launcher reference (versionCode 14) — جزئیات پایین.
- [x] v13 — VB1 Model Registry (versionCode 15)
- [x] v14 — VB8 ModelBackend layer + BUG-AI fix (`0ceb410`)
- [x] v15 — VB4 Availability Graph (`a4f8746`)
- [x] v16 — VB5 Smart Substitution Engine (`4bea741`)
- [x] v17 — VB6 Token Pool Manager (`4a7d578`)
- [x] v18 — VB7 Adaptive Cost Controller (`93d55f5`)
- [x] v19 — VB9 HUD Decision Telemetry (`d30e6ac`)
- [x] v20 — LM2 on-demand local-model download (`eb0e98e`)
- [x] v21 — real Call/SMS actions (fixes v20 "claimed but didn't act"; `c2e7454`)

## ✅ v12 — HOME REDESIGN to orb-launcher reference — DONE (versionCode 14)
> منبع طراحی: دو عکس مرجع کاربر + پرامپت ۴-لایه (`repo/docs/design/references/`).
- [x] **L1 AI Core**: `VisionOrb` (gradient accent→violet→magenta، pulse، animation-gated) + command bar «Ask Vision…» + status dot
- [x] **Greeting top bar**: greeting زمان‌محور + name + weather chip + ساعت زنده + brain dot + gear → Settings
- [x] **Stats row**: Agents (AgentRegistry) · Tasks (TaskRepository) · Devices (mesh nodes) — زنده
- [x] **Quick-actions grid** (Apps/Files/Browser/Tasks/Automation/Agents) — `QuickActionsStore` (ذخیره ترتیب) + edit-mode reorder + Reset
- [x] **L2 Agents panel** (glass card): Research/Automation/Developer/Device + dot وضعیت از registry
- [x] **Widgets card**: Recent Task + Device (battery/RAM via BatteryManager/ActivityManager) + Memory count + suggestion
- [x] **Bottom nav ۵-تایی**: Home / Agents / Apps / Memory / Settings
- [x] **Apps screen**: grid + search + کارت‌های ویژه «Vision Settings» و «Vision Hub» (+`VisionHubScreen` placeholder)
- [x] **L2 `AgentRegistry`** واقعی: ۴ ایجنت، trust ماندگار (Read/Suggest/Auto/Critical)، status مشتق از قابلیت‌های واقعی + Agents management screen + history
- [x] **L3 Memory screen**: memory count + لیست + semantic search
- [x] **Responsive**: WindowSizeClass — phone (bottom nav) / tablet+desktop (NavigationRail + content با عرض محدود)

### v13+ follow-ups (orb launcher) — توجه: کاربر v12 را رد کرد (RD بخش ۱ جایگزین است)
- [ ] drag-to-reorder واقعی quick actions (فعلاً arrows در edit mode)
- [ ] desktop سه‌ستونه با پنل راست اختصاصی (agents+widgets)
- [ ] weather واقعی · ویجت‌های خانه واقعی · Files/Servers (Layer 3) از Vision Hub

## 🐞 علتِ «صحبت نمی‌کند» (رفع‌شده در v5)
- ریشه: APK منتشرشده هیچ provider نداشت — `BuildConfig.GROQ_KEYS` خوانده نمی‌شد و CI هم کلید تزریق نمی‌کرد.
- رفع: `AiProviderStore` حالا Groq را از BuildConfig seed می‌کند؛ `build.yml` کلید `secrets.GROQ_KEYS` را به build می‌دهد؛ پیام خالی‌بودن provider واضح و دوزبانه شد.
- **اقدام لازم کاربر/مالک**: یا در اپ → AI Providers یک کلید رایگان Groq اضافه شود، یا secret به‌نام `GROQ_KEYS` در ریپو ست شود تا هر نسخه از جعبه صحبت کند.

---
← [[kian-workspace/01-projects/05-vision/vision|vision]]
