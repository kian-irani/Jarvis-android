---
title: "Shift report — Real launcher (LR2–LR11) + voice engine + Neo-style parity"
date: 2026-06-17
project: 05-vision
branch: claude/plu-tasks-releases-8x3rhs
pr: kian-irani/Jarvis-android#7
status: open (draft) · CI green
---

# 📋 گزارشِ نشست — 2026-06-16 → 2026-06-17

> همه‌ی کار روی برنچِ `claude/plu-tasks-releases-8x3rhs` و **PR #7** انجام شد.
> هر فاز: build سبز (`compileDebugKotlin` + `testDebugUnitTest` روی Android SDKِ
> نصب‌شده در محیط) → commit → bump نسخه → push. انتشارِ واقعیِ نسخه‌ها هنگامِ
> merge به `main` انجام می‌شود (طبق `.github/workflows/build.yml`).

## نسخه‌ها (به‌ترتیب)
| نسخه | فاز | خلاصه |
|---|---|---|
| v33 | LR2 | هومِ لانچرِ واقعی: `HorizontalPager` (صفحه ۰ = orb، بقیه = گریدِ workspace از `LauncherStore`) + `LauncherViewModel` + page dots |
| v34 | FV2/FV3 | صدای code-switch on-device: `VoiceSegmenter` (۹ تست) + تلفظِ هر بخش با صدای زبانِ خودش + voice picker در Settings |
| v35 | LR3 | drag & drop: long-press→برداشتن، drop روی خالی=move/اپ=folder/فولدر=add. `LauncherGeometry` (۶ تست) |
| v36 | LR4-lite | افزودن به هوم (long-press در drawer) + حذف (کشیدن به نوارِ Remove) |
| v37 | LR11 | Backup/Restore/Reset layout (clipboard) |
| v38 | LR5 | فولدر: rename + open dialog |
| v39 | اصلاح | (بعداً در v48 جایگزین شد) |
| v40 | LR5 | کشیدنِ اپ به بیرونِ فولدر (`pullFromFolder`) |
| v41 | FV5 | **صدای نورونیِ فارسیِ رایگان (Edge neural)**: `EdgeTtsProtocol` (۱۱ تست) + `EdgeTtsClient` (Ktor WS) — opt-in، network-gated، fallback کامل |
| v42 | NEO1 | **فیکسِ باگِ اسم**: خوش‌آمد با اسمِ کاربر، نه اسمِ ویژن؛ onboarding دو نامِ جدا |
| v43 | NEO2 | منوی long-pressِ آیکن (App info / Remove) |
| v44 | NEO3 | شیتِ Edit Home (Wallpaper / Add / Remove page) |
| v45 | NEO4 | اندازه‌ی گرید + `reflowWorkspace` (۲ تست) |
| v46 | NEO3 | تکمیلِ Edit Home (Grid size اینلاین + Launcher settings) |
| v47 | NEO5 | ایندکسِ A–Z fast-scroll در App Drawer |
| v48 | NEO6 | هومِ curated (یک صفحه، سبکِ Neo/Pixel — نه ده‌ها صفحه) |

## تست‌ها (همه سبز)
`VoiceSegmenter` (9) · `EdgeTtsProtocol` (11) · `LauncherGeometry` (6) · `LauncherOps` (13، شاملِ ۲ تستِ reflow) و بقیه‌ی سوییت.

## تصمیم‌های مهم
- **Neo-Launcher = GPL-3.0** → کدش کپی نشد (اپ را مجبور به full open-source می‌کرد).
  کاربر **clean-room** را تأیید کرد: تجربه/منوها/امکاناتِ Neo با کدِ خودمان، گویِ ویژن حفظ.
- **Edge neural TTS** در sandbox قابلِ تأیید نبود (پراکسیِ MITM آن را 403 می‌کند؛ کتابخانه‌ی
  مرجعِ edge-tts هم همین‌جا fail شد) → الگوریتمِ GEC مو‌به‌مو با مرجع تطبیق داده شد؛
  **نیازِ تأییدِ روی دستگاهِ واقعی.**

## باز و نیازمندِ کاربر
- **NEO7**: بازخوردِ مشخصِ UI روی نسخه‌ی نصب‌شده (تا حدس‌نزنیم).
- **NEO8 (LR6)**: تصمیمِ ادغامِ داکِ نَو با hotseat.
- **NEO9 (LR8)**: ویجت‌های `AppWidgetHost` (تستِ دستگاه).
- جزئیاتِ بقیه در `plu/PLAN.md` بخشِ NEO.

## 📚 به‌روزرسانیِ داک‌ها + انتشار (2026-06-17، بعد از v48)
- **README.md**: بَج‌ها v2/v16 → v48؛ بخشِ Status از ۴ جدولِ مفصل به یک خلاصه‌ی فشرده + لینک به PLAN/ROADMAP/CHANGELOG **تمیز شد** (به‌خواستِ کاربر، چون با ROADMAP تکراری بود)؛ «NOT a launcher» → «AI-native launcher روی لایه‌ی هوش»؛ Tech Stack به as-built بازنویسی شد؛ نیازمندی‌های همکاری (QAِ دستگاه #۱).
- **ROADMAP.md**: Progress Snapshotِ ۲۰۲۶-۰۶-۱۷ + اصلاحِ North Star.
- **CHANGELOG.md**: ورودی‌های v20/v21/v48.
- **CONTRIBUTING.md**: جدولِ نیازمندی‌ها هماهنگ شد.
- **docs/ARCHITECTURE.md**: بخشِ «As-built (v48)» با نگاشتِ واقعیِ ماژول‌ها؛ بقیه = target.
- **docs/VISION-CAPABILITIES.md**: مقدمه اصلاح + یادداشتِ وضعیتِ v48.
- **انتشار:** PR #7 به `main` مرج شد → `build.yml` نسخه‌ی **v48** را به‌صورت GitHub Release منتشر می‌کند.

## فایلِ سینکِ ورک‌اسپیس
- پرامپتِ آماده برای Claude Code (CLI) جهتِ به‌روزرسانیِ کپیِ پروژه در ورک‌اسپیسِ کاربر: `plu/reports/2026-06-17-workspace-sync-prompt.md`.
