---
title: "Report — v49 shipped + PRD v2.0 backlog written → v50"
date: 2026-06-17
project: 05-vision
build: v50 (versionCode 52 / versionName 50.0)
status: planning release · roadmap from PRD v2.0
prd: VISION OS — PRD v2.0
---

# 📋 گزارش — v49 منتشر شد + backlogِ PRD v2.0 نوشته شد (v50)

## ۱) چه چیزی در v49 انجام شد (PRD Part 2 — Conversation System)
BUG-1…BUG-5 پیاده و سبز شدند (۲۲۳ تست، ۷ تستِ جدید، ۰ failure). جزئیات در
`plu/reports/2026-06-17-conversation-system-v49.md`. خلاصه:
- **BUG-1** خروجیِ بازشونده (بدونِ truncate) · **BUG-2** Stop/interrupt + barge-in + دکمه‌ی سه‌حالته ·
  **BUG-3** حافظه از قبل وصل بود → فقط **Clear** اضافه شد · **BUG-4** نورونیِ فارسیِ پیش‌فرض (`VoiceRouting`) ·
  **BUG-5** قانونِ مرزِ پیام در `TOOL_PROTOCOL` (بدونِ تغییرِ schema).

## ۲) تسک‌های جدید (از PRD v2.0، Parts 3–14 + 16)
کلِ backlogِ بعد از v49 به `plu/PLAN.md` افزوده شد (بخشِ «🆕 PRD v2.0»). سه تیر:

**🟠 HIGH** — `CF4 MemoryEngine` (Room + ONNX embed + recall/preference-learning، خالصِ TDD) ·
`LR6` داکِ پیکربندی‌شدنی · `LR12` دکمه‌ی دستیارِ داک · `FV4` wake word («ویژن»/Hey Vision) ·
`FV6` انتزاعِ provider صدا · `NEO7` UI pass (دستگاه) · `BUG-1b` رندرِ markdown.

**🟡 MEDIUM** — `ORB` ماشینِ حالتِ گوی (۸ حالت) · `SAFE` trust-levelها + `SafetyLayer` + ضدِ توهم ·
`CTX` ContextEngine · `PAO` overlayِ شناور · `CF5` Automation/WorkManager · `SRCH` جستجوی معنایی ·
`AGT` Agent Society + delegation · `LR8/LR9/VB9.1`.

**🟢 FUTURE** — `MM` multimodal (تصویر) · `MX1–4` mesh + handoff + clipboard · `LM` on-device ·
`TWIN` Digital Twin · `MCP/A2A`.

> ترتیبِ پیشنهادی برای نشستِ بعد: **CF4 MemoryEngine** (پایه‌ی شخصی‌سازی و خیلی از قابلیت‌های PRD به آن وابسته‌اند) → سپس `LR6` و `FV4`.

## ۳) انتشار v50
این نسخه یک **ریلیزِ roadmap/planning** است: بعد از v49 تغییرِ کدِ اپ ندارد — فقط
`plu/PLAN.md` (تسک‌های جدید) + همین گزارش + bumpِ نسخه. هدف: ثبتِ رسمیِ backlogِ PRD v2.0
روی main و کشیدنِ یک نقطه‌ی نسخه‌ایِ تمیز (طبقِ `build.yml` که روی هر push ریلیز می‌سازد).
- bump: `versionCode 51→52`، `versionName "49.0"→"50.0"`.
- build سبز از v49 دست‌نخورده (۲۲۳ تست). کدِ اپ تغییر نکرد، پس رفتارِ اجراییِ APK = v49.
