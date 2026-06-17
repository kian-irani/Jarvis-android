---
title: "Report — v51 audit fixes (HudScreen truncation + neural 3-state)"
date: 2026-06-17
project: 05-vision
build: v51 (versionCode 53 / versionName 51.0)
status: shipped · build green · CI release
---

# 📋 گزارش — v51 رفعِ ممیزی

> پس از auditِ کارِ v49، دو یافته رفع شد. build سبز: `gw :app:compileDebugKotlin :app:testDebugUnitTest`
> → BUILD SUCCESSFUL · **۲۲۳ تست · ۰ failure**.

## Fix-1 — truncationِ خروجی در HudScreen (هم‌کلاسِ BUG-1)
- یافته‌ی audit: `HudScreen.TypewriterPanel` خروجیِ AI را با `maxLines=3, overflow=Ellipsis` می‌برید — همان باگِ BUG-1 روی صفحه‌ی دیگر.
- شدت: کم — `HudScreen` در nav صدا زده نمی‌شود (`grep "HudScreen("` خالی؛ مسیرِ زنده = `HomeScreen`/`WorkspaceHomePager`). کدِ legacy.
- رفع: `maxLines/overflow` حذف؛ متن با `Modifier.weight(1f, fill=false)` در Row wrap می‌شود (cursor جا نمی‌مانَد). جلوگیری از regression اگر HudScreen دوباره wire شود.
- فایل: `ui/screen/hud/HudScreen.kt`.

## Fix-2 — BUG-4 به ۳-حالته (رفعِ override کردنِ خواستِ کاربر)
- یافته‌ی audit: BUG-4 (v49) برای فارسیِ online نورونی را روشن می‌کرد **حتی اگر** کاربر عمداً توگلِ نورونی را خاموش کرده بود (boolean تک‌حالته → بدونِ تمایزِ «پیش‌فرض خاموش» از «کاربر خاموش کرد»). کاربرِ data-conscious فارسی نمی‌توانست on-device بماند مگر با تغییرِ زبان.
- رفع: `NeuralVoiceMode {AUTO, ON, OFF}` جایگزینِ `neuralVoice: Boolean`:
  - **AUTO** (پیش‌فرض): نورونی فقط برای فارسیِ online (= رفتارِ v49) · **ON**: همیشه نورونیِ online · **OFF**: همیشه on-device حتی فارسی.
  - **migration** از کلیدِ قدیمیِ boolean: اگر کاربر قبلاً ست کرده بود → ON/OFF، وگرنه AUTO.
  - `VoiceRouting.useNeural(language, mode, online)` خالص + ۴ تستِ به‌روز (offline→false هر مود · AUTO فارسی→neural/بقیه→on-device · ON→همیشه · OFF→هرگز).
  - UI: `NeuralVoiceRow` (سلکتورِ Auto/On/Off، مطابقِ الگوی `LanguageRow`) جایگزینِ `ToggleRow`.
- فایل‌ها: `data/settings/VisionSettings.kt`، `voice/VoiceRouting.kt`، `voice/VoiceController.kt`، `ui/screen/settings/SettingsHubScreen.kt`، `voice/VoiceRoutingTest.kt`.

## انتشار
bump: `versionCode 52→53`، `versionName "50.0"→"51.0"`. push به `main` → `build.yml` ریلیزِ **v51**.
سایرِ یافته‌های audit (isSpeaking edge-case هنگامِ !ttsReady، نبودِ confirmation برای Clear) کم‌اهمیت و باز ماند.
