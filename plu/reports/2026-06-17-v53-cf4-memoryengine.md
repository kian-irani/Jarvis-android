---
title: "Report — v53 CF4.1 MemoryEngine foundation"
date: 2026-06-17
project: 05-vision
build: v53 (versionCode 55 / versionName 53.0)
status: shipped · build green · CI release
prd: VISION OS — PRD v2.0 Part 4
---

# 📋 گزارش — v53 CF4.1 MemoryEngine (foundation)

> شروعِ CF4 (PRD Part 4). build سبز: `gw :app:compileDebugKotlin :app:testDebugUnitTest`
> → **۲۳۸ تست (۱۵ جدید) · ۰ failure**. TDD برای منطقِ خالص (تست اول → red → impl → green).

## کشفِ مهم (recon)
زیرساختِ memory + embedding **از قبل وجود داشت** (مثلِ یافته‌ی BUG-3): `brain/data/MemoryRepository`
(MiniLM-L12 embedding + cosine search روی Room `memories`)، `EmbeddingRepository`/`OnnxEmbedder`
(۳۸۴-بُعدی)، `MemoryEntity`. پس CF4 **دوباره نساخت** — رویِ این زیرساخت لایه زد.

## چه ساخته شد (`core/memory/`)
- **`MemoryType`** — ۷ نوع (FACT/PREFERENCE/PERSON/PROJECT/EPISODIC/HABIT/WORKING) + `fromName` امنِ parse. (۲ تست)
- **`MemoryScoring`** (خالص، TDD، ۷ تست) — رتبه = `cosine × importanceWeight × recencyDecay`؛ نیم‌عمرِ ۱۴ روز، age منفی=fresh، importance به ۰٫۵..۱٫۰ map می‌شود.
- **`PreferenceLearner`** (خالص، TDD، ۵ تست) — ۳-strike blacklist (آستانه قابلِ‌تنظیم) + reset.
- **`MemoryEngine`** (@Singleton، روی `MemoryRepository`) — `remember(type, importance, metadata)`، `recall` (importance/recency-ranked)، `buildContextWindow` (بلوکِ system prompt)، `learnDislike`. **graceful**: اگر مدلِ embedding دانلود نشده باشد، store/search خطا می‌دهد → no-op/خالی، هرگز crash.
- **`MemoryRepository.searchDetailed`** — مثلِ `search` ولی type/metadata/createdAt را برمی‌گرداند تا engine رتبه‌بندیِ importance/recency کند. (۱ تست characterization)

## تصمیم‌های طراحی
- **importance در `metadata` JSON** نگه داشته شد → بدونِ Room migration (ستونِ جدید لازم نشد).
- **foundation-only**: عمداً به مسیرِ زنده‌ی چت وصل نشد (CF4.2) — چون (۱) چیزی هنوز خاطره ثبت نمی‌کند پس سودِ فعلی صفر است، (۲) دست‌زدن به hot pathِ `CloudChatRouter` بدونِ سود = ریسکِ بی‌مورد. مثلِ الگوی LR1→LR2.
- منطقِ خالص (scoring/preference) TDD شد؛ engine/repo (Room/ONNX-bound) با compile + تستِ harnessِ موجود تأیید شد.

## بعدی
- **CF4.2**: تزریقِ `buildContextWindow` به promptِ `CloudChatRouter` (guarded) + populateِ باکیفیت (نه هر پیام). نیازِ مدلِ embedding (LM2) روی دستگاه.
- **CF4.3**: صفحه‌ی Memory + اتصالِ PreferenceLearner.

## انتشار
bump: `versionCode 54→55`، `versionName "52.0"→"53.0"`. push → ریلیزِ **v53**.
