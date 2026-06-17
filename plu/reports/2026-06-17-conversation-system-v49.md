---
title: "Shift report — PRD v2.0 Conversation System (BUG-1…BUG-5) → v49"
date: 2026-06-17
project: 05-vision
build: v49 (versionCode 51 / versionName 49.0)
status: implemented · CI build green locally
prd: VISION OS — PRD v2.0
---

# 📋 گزارشِ نشست — سیستم مکالمه (PRD v2.0, Part 2) → v49

> همه روی `main`. build سبز: `gw :app:compileDebugKotlin :app:testDebugUnitTest`
> → **BUILD SUCCESSFUL · ۲۲۳ تست (۷ تستِ جدید) · ۰ failure**. منطقِ خالص با TDD،
> Android/UI دورِ آن (طبقِ قاعده‌ی «pure logic is JVM-testable»). UI طبقِ `ui-ux-pro-max`.

## کارهای انجام‌شده
| BUG | خلاصه | فایل‌ها |
|---|---|---|
| **BUG-1** | خروجیِ بازشونده، بدونِ truncate (حذفِ `maxLines=4`). collapsed تا ۱۳۲dp اسکرول؛ «Show more/less» برای پاسخ‌های بلند. | `ui/screen/home/HomeScreen.kt` (`VisionOutput`)، `ui/screen/hud/HudScreen.kt` (`HudUiState.isOutputExpanded`)، `ui/screen/hud/HudViewModel.kt` (`toggleOutputExpanded`) |
| **BUG-2** | دکمه‌ی Stop + قطعِ گفتار. `VoiceController.isSpeaking: StateFlow` + `stopSpeaking()`؛ دکمه‌ی command-bar سه‌حالته (Mic/Send/**Stop قرمز**)؛ تپ روی گوی هنگامِ صحبت = قطع؛ barge-in. | `voice/VoiceController.kt`، `ui/screen/home/CommandBarMode.kt` (خالص)، `HomeScreen.kt`، `HudViewModel.kt`، `ui/theme/VisionIcons.kt` |
| **BUG-3** | ⚠️ حافظه از قبل وصل بود (`CloudChatRouter.chatWith`→`history.recent(6)` + append، از مسیرِ `CloudBackend`). بخشِ مفقود = **Clear**: `clearConversation()` + کنترلِ Clear. | `HudViewModel.kt`، `HomeScreen.kt` |
| **BUG-4** | صدای نورونیِ فارسیِ پیش‌فرض: `VoiceRouting.useNeural(...)` خالص. فارسیِ online → نورونی حتی بدونِ توگل. | `voice/VoiceRouting.kt`، `voice/VoiceController.kt` |
| **BUG-5** | قانونِ مرزِ پیام در `TOOL_PROTOCOL` (فقط متنِ گفتنی در args، مثال FA/EN، شفاف‌سازی، روابطِ فارسی) — **بدونِ تغییرِ schema**. | `data/ai/CloudChatRouter.kt` |

## تست‌های جدید (۷)
- `voice/VoiceRoutingTest` (۴): offline→on-device · فارسیِ online→neural · توگل→neural هر زبان · غیرفارسیِ بدونِ توگل→on-device.
- `ui/screen/home/CommandBarModeTest` (۳): speaking→STOP (حتی با متن) · متن→SEND · idle→MIC.

## تصمیم‌ها / انحراف از PRD (صادقانه)
- **BUG-3 «وصل‌نبودنِ حافظه» نادرست بود** — مسیرِ واقعیِ چت از قبل حافظه را تزریق می‌کرد. به‌جای دوباره‌کاری، فقط بخشِ واقعاً مفقود (Clear) ساخته شد.
- **BUG-5 schema تغییر نکرد** — تغییرِ `args` از string به object، `ToolCaller`/`CommandInterpreter` را می‌شکست؛ پس فقط قوانینِ مرزِ پیام (prose) تقویت شد. سازگار و non-breaking.
- **markdown render (پیشنهادِ PRD برای BUG-1)** فعلاً اضافه نشد تا dependency/risk اضافه نشود؛ هسته‌ی BUG-1 (عدمِ truncate + expand) کامل است. → follow-up.

## نیازمندِ تأییدِ روی دستگاه (NEO7)
- ظاهرِ بلوکِ خروجیِ بازشونده (اسپیسینگ/اسکرول/کنتراست).
- رفتارِ دکمه‌ی سه‌حالته‌ی Stop + barge-in (قطعِ فوریِ TTS).
- کیفیتِ صدای نورونیِ فارسی (در sandbox قابلِ‌تأیید نیست؛ پراکسیِ MITM endpointِ Edge را 403 می‌کند).

## انتشار
نسخه bump شد (`versionCode 50→51`, `versionName "48.0"→"49.0"`). با push به `main`،
`.github/workflows/build.yml` نسخه‌ی **v49** را به‌صورتِ GitHub Release منتشر می‌کند.
