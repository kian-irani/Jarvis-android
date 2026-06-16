# CLAUDE.md — Vision OS (Jarvis-android) operating manual

> This is the **single source of truth** for working on this repo with Claude
> (CLI or the GitHub plugin / `@claude` on issues & PRs). Read this file **and**
> [`plu/PLAN.md`](PLAN.md) before doing anything, then pick the nearest open task
> from the plan, implement it fully, prove it green, commit, and tick the plan.
> Nothing here is optional — these are the project rules.

---

## 1. What this project is

**Vision OS** is an **AI-native Android launcher + on-device "Brain"**. The AI
assistant ("Vision") is the centre of the experience: users talk to the AI first,
apps second. It must work as a **real Android launcher** (like Pixel / Nova /
Nothing) — editable home, drag & drop, folders, widgets, configurable dock,
app drawer — with an AI core (orb + command bar) wired to a multi-model router.

- **Design reference:** the `Example/` folder (reference image + brief) and
  `docs/design/`. Match it. The look is Apple Vision Pro + Nothing OS + Arc.
- **Package:** `com.kianirani.jarvis`  ·  **App label:** VISION.

## 2. Repository layout (critical)

```
app/          ← CANONICAL Android module (Jetpack Compose, Hilt, Room) — BUILD THIS
android/      ← README-only skeleton; do NOT add code here
brain/        ← Python FastAPI brain (ruff + mypy + pytest, cov ≥70%)
node-agent/ sdk/ docs/
gradle/libs.versions.toml  ← single version catalog (Kotlin 2.1, Compose, Hilt 2.54,
                              Room 2.6, Ktor 3.1, kotlinx-serialization, DataStore)
plu/          ← THIS manual + PLAN.md (work driver)
.github/workflows/  ← ci-android.yml (ktlint+tests), build.yml (APK + release), ci-brain.yml
```

In-app Brain-Lite server code lives at `app/src/main/java/com/kianirani/jarvis/brain/`
(layered: `server/routes/*` → `data/*` repositories → Room `data/db/VisionDatabase.kt`;
routes never touch Room/HTTP directly).

Key Android subsystems already built:
- `ui/screen/home/` — Home (orb, command bar, content cards) + `VisionOrb`.
- `ui/screen/drawer/AppDrawerScreen.kt` — app drawer (real `QUERY_ALL_PACKAGES`).
- `ui/theme/` — `VisionColors`/`ThemeStore` (state-backed palette, 3 themes),
  `VisionFonts`/`FontStore`, `VisionIcons` (vector icon family), `VisionMotion`
  (`visionEnter`, `glassPanel`).
- `data/launcher/` — `LauncherModel`/`LauncherOps`/`LauncherStore` (LR1: the real
  launcher layout model + pure ops + JSON persistence). **The launcher rebuild
  (LR1–LR12 in PLAN) binds everything to `LauncherStore`.**
- `router/` — VISION BRAIN: `orchestrator/VisionOrchestrator`, `backend/BackendRouter`,
  substitution/cost/health, token pool, local-model catalog.
- `core/agent/` — `AgentEngine`, `TaskPlanner`, `ToolCaller` (function-calling).
- `service/` — `VisionAccessibilityService`, `VisionNotificationService`.

## 3. Build & test (always before claiming done)

**On the maintainer's build server** use the `gw` wrapper (`/usr/local/bin/gw`):
it wraps Gradle 8.11.1 + `ANDROID_HOME=/opt/android-sdk` + IPv6 flags
(`dl.google.com` returns 404 over IPv4 from that host, 200 over IPv6 — never
remove the IPv6 flags in `gradle.properties`).

```bash
gw :app:compileDebugKotlin --no-daemon        # compile (the UI gate)
gw :app:testDebugUnitTest   --no-daemon        # JVM unit tests
gw :app:assembleDebug       --no-daemon        # APK
```

**Anywhere else (GitHub Actions / fresh checkout)** use the wrapper that ships
in the repo:

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
```

Rules:
- A UI-only change is "verified" when `compileDebugKotlin` is **green**; logic
  changes must also pass `testDebugUnitTest`. There is **no device/emulator** on
  the build server — instrumented tests are `assumeTrue`-guarded; anything
  needing a screen is marked **"needs on-device confirmation"** in the plan.
- Never claim success without the green build output in hand. Report failures
  with the actual error.

## 4. Workflow (how to make progress)

1. Read this file + `plu/PLAN.md`. Pick the **nearest open task** (top of the
   active-priority list; currently **LR — Real Launcher**).
2. For UI/UX work, apply the **`ui-ux-pro-max`** design skill first. For
   multi-step features, spec → plan → TDD per the superpowers skills.
3. Implement the task **fully** — real behavior, not a mockup. Wire UI to
   `LauncherStore` / view-models; persist state; no hardcoded demo content.
4. Keep the existing code working — **never break what passes**. Prefer reuse of
   existing stores, theme tokens, and icons.
5. `gw … compileDebugKotlin (+ testDebugUnitTest)` green.
6. **Tick the task in `plu/PLAN.md`** with a one-line outcome, and keep
   `PLAN.md` (project root) in sync if you have it. Add a short shift report in
   `_daily-reports/` when working the maintainer's workspace.
7. Commit per task (see §5). Push to `main` when a coherent batch is ready (§6).

## 5. Commit conventions

Conventional commits, English subjects (since v16):
`feat:` / `fix:` / `docs:` / `ci:` / `build:` / `perf:` / `release:`; scope for
subsystems, e.g. `feat(launcher): …`, `feat(drawer): …`. Milestone/version tags
appear in the subject for releases: `release: v32 — …`.

End every commit message with:

```
Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>
```

## 6. Release process (one published version per coherent batch)

CI does the release. `.github/workflows/build.yml` runs on **push to `main`**,
builds debug + release APKs, and **publishes a GitHub Release tagged `v<major>`**
(derived from `versionName` in `app/build.gradle.kts`). So to ship:

1. Bump `versionCode` (+1) and `versionName` (`"<N>.0"`) in `app/build.gradle.kts`.
2. Commit `release: v<N> — <summary>`.
3. **Push:** `git push origin main`  ← use the configured credential helper; do
   **not** embed a hand-fetched token in the URL.
4. Don't wait for CI — the release builds itself. The `v<N>` release with
   `VISION-v<N>-release.apk` / `-debug.apk` appears under GitHub Releases.

Batch several finished tasks/phases into **one** version (no per-task hotfix
releases). Branch protection / PRs are not used yet — work lands on `main`.

## 7. Design rules (non-negotiable for UI)

- **Use `ui-ux-pro-max`** for any UI/UX decision (styles, color, spacing, motion,
  a11y). Don't hand-roll generic Material.
- **Vector icons only** (`VisionIcons`, Material Symbols Rounded). **No emoji** as
  icons anywhere.
- **State-backed colors** via `VisionColors`/`ThemeStore` (never raw hex in
  composables) so theme + accent recolour live. Support all 3 themes; contrast
  ≥ 4.5:1 both light/dark.
- **Glass + motion vocabulary:** `Modifier.glassPanel(...)`, `Modifier.visionEnter(i)`
  (animation-gated → reduced-motion safe). Honour `ThemeStore.animations`.
- **Touch targets ≥ 48dp**, `systemBarsPadding`/`navigationBarsPadding` for safe
  areas, `collectAsStateWithLifecycle` for flows.
- The orb is the **hero**; home is spacious, AI-first. No wasted empty sections,
  no hardcoded demo cards.

## 8. Coding rules

- Kotlin + Jetpack Compose + Hilt (DI) + Room + kotlinx-serialization + DataStore.
- **Pure logic is JVM-testable** — keep mutation/algorithm code in pure
  functions/objects (e.g. `LauncherOps`, `ToolCaller`) and unit-test them; UI/VM
  wraps them. Test names use backticks: `` fun `does X`() ``.
- Brain-Lite endpoints: route in `server/routes/` (delegate to a repository/port,
  throw `BrainException.*`) → mount in `server/KtorServer.kt` → test with
  `testApplication` + MockK. Error envelope `{ok,data,error{code,message}}`.
- Groq 3-key rotation on 429; `USE_MSGPACK = False`. Never log/store raw tokens.
- Honest reporting: if tests fail say so with output; if a step needs a device,
  say "needs on-device confirmation".

## 9. The current mission — LR (Real Launcher)

The home was a static scrollable dashboard; the active priority is to make Vision
a **real Android launcher**. The full backlog is **LR1–LR12** in `plu/PLAN.md`,
foundation-first. **LR1 (layout model + persistence) is done.** Next:

- **LR2** Workspace grid + pages (render pinned apps from `LauncherStore`,
  `HorizontalPager` + indicators) — replaces the scroll dashboard.
- **LR3** DragController (real long-press drag & drop, move between cells/pages/dock).
- then LR4 edit-mode sheet, LR5 folders, LR6 configurable dock, LR7 app-drawer pro,
  LR8 `AppWidgetHost`, LR9 gestures, LR10 launcher settings, LR11 backup/restore,
  LR12 Vision-assistant dock button.

Everything mutates the single persisted `LauncherStore` layout. **No mockups.**

## 10. Secrets

Tokens are **never** committed. The maintainer keeps them in
`00-master-hub/TOKENS-LIVE.md` (outside this repo). CI injects `GROQ_KEYS` via a
repo secret. If a task needs a key, reference where it lives — never paste it
into code, the plan, or this repo.

---

_Keep this file current. When conventions, build commands, or the mission change,
update §3 / §6 / §9 here so the next Claude session (CLI or GitHub plugin) stays
correct._
