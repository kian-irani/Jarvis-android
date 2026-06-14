# Spec — Vision Brain: Cognitive Multi-Provider Router + Local & Mesh AI

**Date:** 2026-06-14 · **Status:** Design · **Owner:** Vision OS
**Maps to:** VISION BRAIN V1 master spec (Modules 1–8, 17) · ROADMAP Phase 2 (Multi-Token AI Router), Phase 8 (Device Mesh), Phase 8.5 (Offline), WAN-Mesh W5, Agent-OS C2/C3.

> Core principle (from master spec): **Vision never depends on a specific provider. Vision depends on capabilities. Providers are replaceable; capabilities are permanent.**

---

## 1. Problem / Gap

The current chat path is:

```
input → CommandInterpreter (local tools/offline) → CloudChatRouter
```

`CloudChatRouter.chat()` iterates `AiProvider.entries` in a **fixed enum order** and, within each provider, rotates keys on failure. There is **no thinking step**: no intent/modality detection, no capability matching, no model metadata, no health/latency awareness, no cost control, no on-device generation, and no way to use a model running on another mesh node.

This spec inserts a **cognitive routing layer** ("Vision Brain") between the command layer and the execution backends, and adds two new execution backends (on-device local model, mesh remote-local model).

## 2. Target Architecture

```
User input (text / voice / image / screen)
  │
  ├─▶ CommandInterpreter        [EXISTS] device tools + offline answers (no model)
  │
  └─▶ VisionOrchestrator        [NEW] "think before acting" → DecisionObject
        ├ IntentClassifier        intent + modality
        ├ CapabilityAnalyzer      required capabilities (reasoning/coding/vision/audio/long-ctx/tools/realtime/offline)
        ├ CapabilityRouter  [NEW] ranks candidate models against requirements
        │     ↑ ModelRegistry [NEW]      metadata + capability scores + local flag (cloud + local + mesh)
        │     ↑ AvailabilityGraph [NEW]  latency EWMA, error rate, quota, cooldown, circuit-breaker
        │     ↑ CostController [NEW]      Economy / Balanced / Premium / Unlimited + budget ceiling
        └ SubstitutionEngine [NEW] ordered chain: Primary → F1 → F2 → F3 → Local → graceful fallback
              │
              ▼  ModelBackend (unified interface) [NEW]
        ┌────────────────┬──────────────────────┬───────────────────────────┐
        │ CloudProvider  │ LocalModelEngine      │ MeshModelClient            │
        │ Adapter [EXISTS│ [NEW] on-device GGUF  │ [NEW] inference on another │
        │ → CloudChat    │ via llama.cpp/MediaPipe│ node's local model (Ollama)│
        │ Router refactor│                       │ chosen by BrainScore       │
        └────────────────┴──────────────────────┴───────────────────────────┘
```

### DecisionObject (orchestrator output)
```
{ intent, modality, capability_requirements[], cost_mode,
  candidates[ {model_id, backend, score, est_cost, est_latency} ],
  selected, fallback_chain[], reasoning_trace }
```

## 3. Components

### 3.1 ModelRegistry (Module 3)
- Per-model metadata: `model_id, backend(cloud|local|mesh), provider, ctx_window, reasoning/coding/vision/audio/speed/cost scores (0–10), supports_tools/vision/audio, local: bool`.
- Seeded for current cloud providers; local model added with `local=true` after download; mesh models injected from node advertisements.
- **Remote-updatable**: capability table shipped as JSON (repo / GitHub release) so new models are added without an app update — keeps "providers replaceable."

### 3.2 CapabilityRouter (Module 2)
- Input: capability requirements + cost_mode + availability. Output: ranked candidate list (not a fixed enum).
- Ranking = capability match × availability health × cost-mode weight.

### 3.3 AvailabilityGraph (Module 8) — replaces bare `AiUsageStore`
- Per model/provider/key: latency EWMA, error rate, quota remaining, **cooldown** (honor `Retry-After` on 429), **circuit-breaker** (closed/open/half-open).
- Router skips OPEN circuits; surfaces health dots to HUD (Module 19).

### 3.4 Token Pool Manager upgrade (Module 6)
- Existing key rotation kept; add cooldown, load-balancing across keys, quota tracking, auto-recovery, per-key health. Builds on `AiProviderStore` + `AiUsageStore`.

### 3.5 Adaptive Cost Controller (Module 7)
- Modes: **Economy** (cheapest capable), **Balanced** (best quality/cost), **Premium** (best quality), **Unlimited** (ignore cost).
- Pre-flight cost estimate for heavy tasks + monthly budget ceiling with 80% warning.

### 3.6 Smart Substitution Engine (Module 4)
- Per-task explicit chain `Primary → F1 → F2 → F3 → Local → graceful-fallback`. User-configurable. Never hard-fail while a capable alternative exists.

### 3.7 LocalModelEngine (Module 17) — on-device generative model
- **Engine:** llama.cpp via JNI (streaming + cancellation) — or MediaPipe LLM Inference API as an alternative.
- **Default downloadable model (recommendation):**
  - **Primary: Qwen2.5-0.5B-Instruct (Q4_K_M GGUF, ~0.4 GB) — Apache-2.0.** Cleanest license for redistribution as in-app downloadable data; runs on low/mid devices.
  - **Quality option: Gemma 3 1B (Q4 GGUF, ~0.7 GB) — Gemma license** (needs license acceptance) for stronger devices.
  - **Ultra-light fallback:** Qwen2.5-0.5B at a smaller quant for ≤3 GB RAM (Brain-Nano tier).
- **Not bundled in the APK** — downloaded on demand (Setup Wizard "Offline Model Download" step, ROADMAP B.5) with **resume + SHA256 pin** (reuse the existing `MODEL_SHA256` fail-closed pattern from the ONNX embedder), stored in app files.
- **Device-tier gating:** pick variant by RAM/CPU (Brain-Nano/Lite/Full tiers already defined in ROADMAP P1); warn / recommend smaller on low-RAM. Load on demand, **unload after idle** to free RAM; thermal guard.
- Registered in ModelRegistry with `local=true` + capability scores; orchestrator prefers it in **Privacy / Offline / Economy** modes or when all cloud tokens fail.
- **Privacy Mode:** force local-only (no cloud egress), surfaced in the Trust gate (Module 18).
- Reuse the existing OnnxEmbedder for local embeddings; share lifecycle with the generative engine.

### 3.8 MeshModelClient (Module 17 + Phase 8) — use other devices' models
- Each mesh node advertises its **local models** (e.g. Ollama tags) alongside CPU/RAM/GPU in the node registry/heartbeat (extends `NodeMetrics`).
- Phone ingests advertised models into ModelRegistry as `backend=mesh` entries.
- `MeshModelClient` POSTs inference to the chosen node's brain `/chat` targeting its local model; node chosen by **BrainScore × AvailabilityGraph**.
- Heavy/private tasks route to the strongest mesh node's local model before/instead of cloud (completes WAN-Mesh **W5**).

### 3.9 ProviderAdapter refactor (Module 5)
- Extract `ModelBackend` interface: `suspend fun generate(req): Flow<Token>` + capabilities. `CloudChatRouter` providers, `LocalModelEngine`, `MeshModelClient` all implement it. Orchestrator routes uniformly; circuit-breaker/retry live in the Provider Router around the adapter.

## 4. Extra suggestions (added by Claude)
1. **Streaming SSE for `/chat`** (already a ROADMAP follow-up) — essential for local-model UX and the "thinking" HUD; tokens appear gradually.
2. **Reasoning/decision trace** surfaced to the user ("answered with X because: capability+cost+health") — transparency + debugging; reuses A.1 Reasoning Trace.
3. **Graceful no-model degradation** — if no cloud token AND no local model yet, CommandInterpreter answers what it can and Vision offers to download the local model. Prevents "won't talk" regressions (the v5/v9 class of bug).
4. **First-run on-device benchmark** to seed a realistic latency score per device in the AvailabilityGraph.
5. **Capability scores remote-updatable** (JSON in repo / GitHub release) — new models without an app update.

## 5. Test strategy
- ModelRegistry: serialization + remote-merge; CapabilityRouter: ranking determinism per cost-mode; AvailabilityGraph: cooldown/circuit-breaker transitions (429 + Retry-After); SubstitutionEngine: chain exhaustion → graceful fallback; LocalModelEngine: SHA256 pin fail-closed, device-tier gating (mock RAM), cancellation; MeshModelClient: node selection by BrainScore, model advertisement parse. JVM-only (no device) per project convention; instrumented inference guarded with `assumeTrue`.

## 6. Non-goals (this slice)
Multimodal vision/image-gen routing (Phase 14), full multi-agent coordinator (Agent-OS B1), marketplace (Module 20). The registry/router are designed so these plug in later as new capabilities/backends.
