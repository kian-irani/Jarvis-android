# 🔄 Prompt — sync my Vision workspace to build v48

> Paste the block below to **Claude Code (CLI)** in your local Vision workspace.
> آماده برای کپی به Claude Code در ورک‌اسپیسِ خودت.

---

```
Update my local Vision (Jarvis-android) workspace to the latest state on `main`.

WHAT HAPPENED (remote, by the web session):
- A large batch shipped on branch `claude/plu-tasks-releases-8x3rhs` via PR #7,
  now MERGED to `main` and released as build **v48** (versionName 48.0,
  versionCode 50; CI auto-publishes a GitHub Release on push to main).
- Two themes: (1) the REAL LAUNCHER rebuild (Neo/Nova-style, clean-room — NO GPL
  code copied), and (2) the VOICE engine (code-switch + free Edge neural Persian),
  plus a full docs refresh.

DO THIS:
1. Sync git:
   - `git checkout main && git fetch origin && git pull --ff-only origin main`
   - Confirm HEAD includes the v48 work (`git log --oneline -20` should show the
     LR/NEO + voice + docs commits up to the docs refresh).
2. Read the source of truth FIRST:
   - `plu/CLAUDE.md` (operating manual) and `plu/PLAN.md` — especially the new
     **NEO** track (priority 0.5) and its "Design North-Star" block.
   - `plu/reports/2026-06-17-neo-launcher-session.md` — the full shift report
     (version table v33–v48, tests, decisions).
3. Review the new/changed code so your local context matches:
   - Launcher: `app/src/main/java/com/kianirani/jarvis/data/launcher/`
     (`LauncherModel`, `LauncherOps`, `LauncherStore`, `LauncherGeometry`) and
     `app/src/main/java/com/kianirani/jarvis/ui/screen/workspace/`
     (`WorkspaceScreen.kt`, `LauncherViewModel.kt`); plus `MainActivity.kt`
     (HOME route wires `WorkspaceHomePager`), `ui/screen/drawer/AppDrawerScreen.kt`
     (A–Z index + add-to-home), `ui/screen/onboarding/OnboardingScreen.kt`
     (user name vs assistant name).
   - Voice: `app/src/main/java/com/kianirani/jarvis/voice/`
     (`VoiceSegmenter.kt`, `VoiceController.kt`, `EdgeTtsProtocol.kt`,
     `EdgeTtsClient.kt`) and `data/settings/VisionSettings.kt` (userName,
     neuralVoice, per-language voice prefs).
   - Tests: `app/src/test/java/com/kianirani/jarvis/voice/` and
     `.../data/launcher/` (VoiceSegmenter 9, EdgeTtsProtocol 11, LauncherGeometry 6,
     LauncherOps reflow 2).
   - Docs: `README.md`, `ROADMAP.md`, `CHANGELOG.md`, `CONTRIBUTING.md`,
     `docs/ARCHITECTURE.md` (new "As-built (v48)" section),
     `docs/VISION-CAPABILITIES.md`.
4. Verify the build locally (no emulator needed):
   `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`  → must be green.
   (Needs an Android SDK with platform 35 + build-tools 35.0.0; AGP 8.7.3,
   Gradle 8.11.1, JDK 17+.)
5. Note what still NEEDS ON-DEVICE confirmation (no emulator on CI): all launcher
   UI behaviour and the Edge neural voice path (it falls back to on-device TTS).
   Open items are in `plu/PLAN.md` → NEO7–NEO14 (UI polish, dock decision LR6,
   widgets LR8, gestures, At-a-Glance, icon pack, neural-voice device check).

Then give me a short summary of the diff vs my last local state and what to test
on a real device first.
```

---

### چکیده برای خودت (نه برای کلودِ لوکال)
- برنچ: `claude/plu-tasks-releases-8x3rhs` → مرج به `main` → ریلیزِ **v48**.
- نسخه‌ها: v33 (LR2) … v41 (Edge neural) … v48 (curated home) + داک‌ها.
- نقطه‌ی شروعِ همیشگی: `plu/PLAN.md`.
