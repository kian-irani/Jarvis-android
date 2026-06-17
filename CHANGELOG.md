# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Planned
- Real-time weather on the home greeting bar; drag-to-reorder quick actions
- Three-column desktop layout (dedicated right panel for agents + widgets)
- Smart home / IoT integrations (Home Assistant, Tasmota) — Phase 16
- Cross-platform expansion (iOS / Desktop) — Phase 19

---

## [48.0.0] — 2026-06-17 — Real launcher (NEO) + free neural Persian voice + name fix

### Added — Real launcher (Neo-style, clean-room; the orb stays the hero)
- **Workspace home** — `HorizontalPager` where page 1 is the AI-core orb home and the rest are real
  workspace grids rendered from a persisted `LauncherStore` (layout model + pure ops + JSON), with
  floating page dots. (LR2)
- **Drag & drop** — long-press to pick up a tile; drop on empty → move, on an app → folder, on a folder →
  add; pure `LauncherGeometry` cell-math. (LR3)
- **Add / remove** — long-press in the drawer to pin an app to home; drag to a red "Remove" strip to unpin. (LR4-lite)
- **Folders** — create, open, **rename**, 2×2 child preview, and pull an app back out. (LR5)
- **Long-press icon menu** (App info / Remove) and a **long-press Edit-Home sheet**
  (Wallpaper · Grid size · Add/Remove page · Launcher settings). (LR4)
- **Grid-density presets** (4×5…6×6) with a safe re-flow that never strands icons. (LR10)
- **App drawer A–Z fast-scroll index**; curated first-run home (one clean page, not endless pages). (LR7/seed)
- **Layout backup / restore / reset** via the clipboard. (LR11)

### Added — Voice engine
- **Code-switch TTS** (`VoiceSegmenter`) — mixed Persian/English replies are split and each run is spoken
  in its own voice instead of one voice garbling the other.
- **Per-language voice picker** + TEST + best-installed-voice auto-selection.
- **Free Edge neural Persian TTS** (`fa-IR-DilaraNeural`) — opt-in, network-gated, with a full fallback to
  on-device TTS. *Needs on-device confirmation* (the build sandbox MITM-proxies the endpoint).

### Fixed
- **Greeting used the assistant's name as the user's name.** Onboarding now asks "Your name" and
  "Assistant name" separately, and the home greets the user by their own name.

### Notes
- ~37 new unit tests (`VoiceSegmenter`, `EdgeTtsProtocol`, `LauncherGeometry`, `LauncherOps` reflow).
- Neo-Launcher is GPL-3.0; its experience/menus are re-implemented **clean-room** (no GPL code copied) to
  keep Vision source-available. Decision + design north-star recorded in [`plu/PLAN.md`](plu/PLAN.md).

## [21.0.0] — 2026-06-15 — VISION BRAIN: cognitive multi-model router

### Added
- **Cognitive router (VB1–VB9):** an Orchestrator that classifies intent → capability → ranked model
  candidates, an **Availability Graph** (latency/error EWMA, circuit-breaker, Retry-After), a **Smart
  Substitution** chain, an **Adaptive Cost Controller**, a secret-safe **Token Pool** (per-key health,
  hashed keys), pluggable **Backend** adapters (Cloud/Local/Mesh), and in-HUD **decision telemetry**.
- **Agent engine** — `TaskPlanner` + `AgentEngine` + `ToolCaller` (real function-calling: call/SMS/
  open-app/settings/nav). "Never claim done without executing."
- **On-device local model** — catalog + on-demand, resumable, SHA-pinned downloads; hybrid local-first
  routing in Economy/Privacy modes.

### Changed
- Command bar wired to the cognitive path (orchestrator + backend router) instead of brain-first only.

## [20.0.0] — 2026-06-15 — Vision OS visual redesign + typography

### Added
- Full visual redesign to the orb-launcher reference: rewritten `VisionOrb` (luminous portal ring),
  azure→violet palette on dark, unified vector icon family (no emoji), floating glass dock, redesigned
  Home/Drawer/Agents/Memory/Settings.
- **Typography system** — bundled OFL fonts + state-backed `VisionFonts`/`FontStore` + in-app font picker.

## [12.0.0] — 2026-06-14 — Orb-Launcher Home Redesign + Agents Layer

### Added
- **New home** matching the design reference: time-based greeting bar (name · weather · live clock · brain dot · gear), a soft glowing **Vision Orb** (radial accent→violet→magenta sphere, animation-gated), an **"Ask Vision…"** command bar, a **stats row** (Agents / Tasks / Devices, live), a **quick-actions grid** (Apps/Files/Browser/Tasks/Automation/Agents) with persisted order + edit-mode reorder + reset, an **Active Agents** card, and a **device/widgets** card (recent task · battery · free RAM · memory count).
- **5-tab bottom navigation** (Home / Agents / Apps / Memory / Settings) — settings now reachable from an obvious tab *and* the top-bar gear.
- **Agents layer (`AgentRegistry`)** — 4 agents (Research/Automation/Developer/Device) with **persisted trust levels** (Read/Suggest/Auto/Critical) and status **derived from real capabilities** (AI keys, tools, brain connection, accessibility). New **Agents** management screen + on-home panel.
- **Memory** workspace screen — on-device memory count, recent memories, semantic search.
- **Apps** drawer special cards — "Vision Settings" and "Vision Hub" (placeholder lab/notes/files/servers).
- **Responsive** layout via WindowSizeClass — bottom nav on phones, **NavigationRail** + width-capped content on tablet/desktop.

### Changed
- **Full reskin** to the soft reference look: sans-serif typography for prose/headings (monospace retained for data labels), rounded glass cards, spherical orb replacing the angular reactor eye on home.

## [11.0.0] — 2026-06-14 — Theme & Appearance Engine

### Added
- **Theme switcher** — three full themes applied live: **Deep Space** (default void), **Aurora Dark** (purple nebula), **Light Future** (daylight HUD, AA-contrast dark ink on light surfaces).
- **Custom accent picker** — 8 preset swatches + AUTO (theme default) + `#RRGGBB` hex input; recolours the orb glow, buttons, borders and selected states everywhere.
- **Wallpaper picker** — theme gradient or solid-colour presets, applied to every screen backdrop.
- **Global animation toggle** — one switch stops all entrance/pulse/motion (reduced-motion friendly).
- **Distributed brain badge** on the home top bar (active node + live status) with a show/hide toggle.
- **Reset appearance** — one tap restores theme/accent/wallpaper/animations/badge defaults.
- New **APPEARANCE** section in SYSTEM CONFIG hosting all of the above, applied immediately.

### Changed
- `VisionColors` is now snapshot-state backed, so every existing screen re-themes instantly without per-screen edits. Persisted via `ThemeStore` (SharedPreferences).

---

## [16.0.0] — 2026-06-10 — Vision OS · Sovereign Intelligence Edition

### Changed (BREAKING)
- **Vision is no longer "just an Agent OS launcher."** Repositioned as a **Personal Intelligence Operating Layer** — sovereign, local-first, distributed.
- **ROADMAP fully rewritten to v16.0** (20+ phases, Milestones M0–M4, ADR-001…011). Replaces the v5.1 CyberDeck roadmap.
- **README upgraded to v16** while preserving the Vision banner and bilingual structure.
- **Workflow engine:** n8n → **Temporal Workflow Engine**.

### Added
- **Distributed Brain** — every device can host a Brain (Nano / Lite / Full) with auto-election via **Brain Score**; full offline operation on phone without a VPS.
- **VISN Protocol** — fast file transfer between nodes (LZ4/zstd, chunked, resumable, XXH3).
- **Trust Level System** (Read / Suggest / Auto / Critical) with tamper-evident Audit Trail.
- **Vision Lab** (dry-run sandbox), **Vision Notes**, **Vision Timeline**, **AnySearch**.
- **Universal Language Engine** — true all-language support (3-tier).
- **Privacy Threat Monitor** + **Behavioral Baseline** security layer; **Session Handoff** across devices.
- Code standards: Detekt/ktlint, Ruff/mypy, pytest, GitHub Actions CI/CD, OpenTelemetry.

---

## [5.0.0] — 2026-06-06 — Vision Agent OS

### Changed (BREAKING)
- **Rebrand:** Jarvis-android → **Vision Agent OS**.
- **License:** MIT → **Vision Agent OS Source-Available License (VAOS-SAL) v1.0** — no longer open source. Commercial use, redistribution, and derivatives prohibited.

### Added
- **Activation model:** end-user activation via the **kiancdn** Telegram bot (token-based). See `docs/ACTIVATION.md`.
- **CLA.md** + rewritten `CONTRIBUTING.md` — contributors welcome under CLA.
- **NOTICE** file; activation/token layer in `docs/ARCHITECTURE.md` and new `Phase A` in `ROADMAP.md`.

### Removed
- All previous public **releases, tags, and APK assets** (v4.1.77, v4.1.79) purged.
- CI no longer publishes public GitHub Releases (build artifacts only, private to Actions).
- Duplicate `RoadMap.md` removed.

---

## [4.0.0] — 2026-05-21

### Added
- **Bilingual README** (English + Persian)
- **LICENSE** (MIT)
- **DONATE.md** with Tron TRC20 wallet
- **CODE_OF_CONDUCT.md**
- **.github/FUNDING.yml**
- **Social preview** image
- **10 topics** for searchability

### Changed
- Repository description set in English

---

## [3.x] — Previous versions

For older history, see Git log or releases page.
