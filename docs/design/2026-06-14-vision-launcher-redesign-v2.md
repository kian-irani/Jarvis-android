# Design Spec — Vision OS Launcher Redesign v2.2

**Date:** 2026-06-14 · **Status:** Design (awaiting build command) · **Supersedes:** v12 orb-launcher + Eye-of-Vision HUD (both rejected by user)
**References (version-controlled):** `docs/design/references/vision-multidevice-allplatforms.png` · `vision-phone-home-v2.png` · `vision-phone-home-clean.jpg` · `vision-android-windows-macos.png` · `vision-eye-variant.png`
**Design intelligence:** ui-ux-pro-max (Spatial/glass + Tech-startup typography).

> User directive 2026-06-14: "Don't use the old design at all — no trace of it, all new, use the images." Both the previous (HUD) and current (v12) designs are rejected. Rebuild the launcher to match the uploaded multi-device reference set, add a **font picker**, and ship with a **better default font**.

---

## 1. Design Language (from the reference images)

- **Theme:** deep-space aurora. Background `#0A0E1A`→`#0B1022` with a faint nebula/mountain horizon; glass surfaces over it.
- **Hero:** central glowing **AI Core orb** — radial gradient **cyan `#22D3EE` → blue `#3B82F6` → violet `#8B5CF6` → magenta `#D946EF`**, soft bloom, orbital ring, neural particles (Canvas), reflection ripples below. "VISION / AI CORE ONLINE" inside. Pulse animation (animation-toggle gated).
- **Surfaces:** glass cards (blurred translucent, 1px hairline border `#FFFFFF14`, radius 20–24dp, soft elevation). Consistent elevation scale.
- **Accent:** user-set accent (default violet `#8B5CF6`) drives orb glow, selected nav icon, buttons, selection rings.
- **Icons:** one vector set (Lucide-style), consistent stroke, NO emoji as structural icons.
- **Motion:** spring-based, 150–300ms, enter-from-below; honor reduced-motion + the global animation toggle.

## 2. Typography (the "better font" + switchable fonts)

| Role | Font | Why |
|------|------|-----|
| Display / wordmark / greeting | **Space Grotesk** | geometric, techy, distinctive wide-tracked "VISION" — clearly better than the current default |
| Body / UI / labels | **Inter** | proven for spatial/glass UIs, highly legible |
| Numerals / stats / brain score | **Space Mono** (tabular) | prevents layout shift on live numbers |
| **Persian (FA)** | **Vazirmatn** | Inter has poor Farsi coverage; app is bilingual FA/EN — Vazirmatn covers both cleanly (OFL) |

- **Bundle fonts in the APK** (`res/font/`) — do not rely on Google Fonts at runtime (offline-first). Preload, reserve space.
- **Font picker (Settings):** user switches the active UI font family among a curated set (Space Grotesk, Inter, Vazirmatn, DM Sans, Exo 2). Persisted in DataStore/VisionSettings; applied live via a `VisionTypography` snapshot-state (same pattern as the live `VisionColors`).
- Default in the released build = **Space Grotesk (display) + Inter (body)**, auto-switch to **Vazirmatn** when language = FA.

## 3. Three Responsive Layouts (WindowSizeClass)

- **Phone (<600dp):** vertical — greeting bar → orb → command bar → 3 stat pills → quick-actions row → Active Agents card → Widgets card → 5-item bottom nav. (= `vision-phone-home-v2.png` / `vision-phone-home-clean.jpg`)
- **Tablet (600–840dp):** NavigationRail + content (two-column).
- **Desktop (≥840dp):** NavigationRail + center content + **right panel** (Agents + Widgets). (= desktop frames in `vision-android-windows-macos.png`)

## 4. Four Architecture Layers → UI mapping (from the build prompt)

1. **AI Core** — orb + command bar + status dot (wires to BrainLite/CloudChatRouter via the new cognitive router).
2. **Agents** — home panel (live dots) + full Agents management screen (trust levels Read/Suggest/Auto/Critical).
3. **Workspaces** — Memory tab + Vision Hub (Projects/Files/Memory/Servers/Messages — placeholders first).
4. **Apps** — app drawer grid (`QUERY_ALL_PACKAGES`) + special "Vision Settings" & "Vision Hub" cards + search.

## 5. Screen inventory (output of the build prompt)

`HomeScreen`, `AppsScreen`, `AgentsScreen` (+management), `MemoryScreen`, `SettingsScreen` (ModalBottomSheet/full-screen), `VisionHubScreen`. Composables: `AICoreOrb`, `StatsIndicators`, `AgentsPanel`, `WidgetsCard`, `AppGrid`, `CommandBar`, `QuickActionsRow`. ViewModels: `QuickActionsViewModel`, `SettingsViewModel`. `MainActivity` NavHost + 5-item bottom nav. Reuse (do NOT rewrite): BrainLiteService, AgentRegistry, TaskRepository, mesh NodeRepository, MemoryRepository, CloudChatRouter, VisionSettings/stores.

## 6. Settings panel (complete)

Theme switcher (Deep Space/Aurora Dark/Light Future) · accent picker (wheel+hex) · **font picker (NEW)** · animation toggle · reset layout · language (EN/FA) · Vision shortcuts (AI Providers, Brain Election, Agent Trust, Privacy Monitor) · distributed-brain badge toggle · wallpaper picker · close/back. Access from: home gear, bottom-nav Settings, and the "Vision Settings" card in Apps. All applied live (recomposition) + DataStore-persisted.

## 7. Quality gates (ui-ux-pro-max)

Touch targets ≥48dp · contrast ≥4.5:1 in both themes · reduced-motion honored · safe-area insets · 8dp spacing rhythm · vector icons only · home loads <300ms · `collectAsStateWithLifecycle` + `remember`/`derivedStateOf`. ktlint/Detekt clean. JVM tests for stateless logic; instrumented UI guarded.

## 8. Release seed keys

Release must talk out-of-the-box: bake **OpenRouter** keys (from `config.txt`, user-provided) the same way Groq keys are baked — `BuildConfig.OPENROUTER_KEYS` consumed by `PrefsAiProviderStore.bakedKeys(OPENROUTER)`, injected at build via CI secret / local gradle property. **Keys must NOT be committed to source** (detect-secrets gate); they live in the CI secret + the gitignored `config.txt`.

## 9. Relationship to the AI bug

The redesign's command bar must route through the **Vision Brain cognitive router** (see `docs/superpowers/specs/2026-06-14-vision-brain-cognitive-router.md`). The current "doesn't use my tokens" bug (brain-first gate bypasses `AiProviderStore`; Brain-Lite is Groq-only with a separate key list) is tracked as a **HIGH-priority bug** and should be fixed before/with this redesign so the new command bar actually talks.
