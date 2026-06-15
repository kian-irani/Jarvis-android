# Vision Brain — Framework Architecture Review (2026-06-15)

> How the code should be structured so Vision's brain is an *excellent, advanced*
> agentic framework — not a thin wrapper over an LLM call. Written after shipping the
> VB1–VB9 cognitive router + LM2. Audience: future auto-rp sessions doing FRAMEWORK DEPTH.

## 1. Where we are (honest assessment)

**Strong foundation already shipped (v13–v20):**
- `router/registry` (VB1) — capability-scored model catalogue, remote-patchable.
- `router/capability` (VB2) — ranks by *capability*, not provider; filters to reachable.
- `router/orchestrator` (VB3) — classify intent/modality → `DecisionObject` ("think before act").
- `router/backend` (VB8) — uniform Cloud/Local/Mesh backend behind one interface.
- `router/health` (VB4) — availability graph: EWMA latency/error + circuit breaker + Retry-After.
- `router/substitution` (VB5) — bounded, never-silent fallback chain, always ends at local.
- `data/ai/TokenPool` (VB6) — health-aware key rotation; `router/cost` (VB7) — spend modes.
- HUD telemetry (VB9); `router/local` (LM2) — in-app model download.

**What this is:** a very good *routing + reliability* layer. **What it is not yet:** an
*agent*. Today Vision = classify → route → single LLM turn → reply. It does not yet
**plan multi-step work, call tools in a loop, remember across sessions, or improve from
feedback.** That gap is the difference between "a chat app" and the "revolutionary brain."

## 2. Target architecture — the cognitive loop

Add an **AgentEngine** above the router implementing a ReAct-style loop. The router
becomes the engine's "call a model" primitive; the engine adds reasoning, acting,
observing, and reflecting:

```
Perceive ─▶ Orchestrate(VB3) ─▶ Plan ─▶ Act(tool|model) ─▶ Observe ─▶ Reflect ─▶ (loop|Answer)
   ▲ input (text/voice/image)        │                          │            │
   │                                 └─ uses Router(VB1-8)       │            └─ self-eval, memory write
   └─────────────────── Memory (episodic + semantic retrieval) ─┘
```

**New components to build (each pure-core + injected seams, like the existing router):**

| Module | Package | Responsibility |
|---|---|---|
| `AgentEngine` | `core/agent/` | Goal→Plan→Act→Observe→Reflect loop; bounded steps; cancellation; streaming. |
| `TaskPlanner` | `core/planner/` | Decompose a goal into an `ActionPlan` (ordered steps, each a tool or model call). |
| `ToolCaller` | `core/agent/` | Turn a model reply into structured tool calls (function-calling JSON) and back. |
| `MemoryStore` | `core/memory/` | Episodic (turns/events) + semantic (embeddings via existing MiniLM) + retrieval. |
| `Reflector` | `core/agent/` | After acting, score the result vs the goal; decide continue/redo/answer. |
| `FeedbackLog` | `core/eval/` | Capture every `DecisionObject` + outcome + (optional) user thumbs → quality signal. |

The existing `ToolRegistry`/`Tool` (P5) and `NavigationTool`/`NotificationTool` already
exist — the planner/agent consume them; tool-calling JSON is the missing glue.

## 3. Coding principles (keep doing these — they're why the core is solid)

1. **Capabilities, never providers.** Every decision is expressed in `Capability`, resolved
   to a concrete model late. New code must not hardcode a provider.
2. **Pure core + injected seams.** Clock, network, file IO, and now the LLM call are all
   behind `fun interface` seams → everything is JVM-unit-testable with fakes (see
   `AvailabilityGraphTest`, `LocalModelManagerTest`). The AgentEngine MUST follow this:
   inject the router and tools, so the whole loop runs in tests with a scripted model.
3. **Never hard-fail, never go silent.** VB5 guarantees an answerer; the agent loop must
   too (LM6 graceful fallback). A failed tool/step degrades, it doesn't crash the turn.
4. **Structured decisions + telemetry-first.** Everything emits a typed object with a
   human reason (`DecisionObject.reason`). Extend this: `PlanTrace`, `StepResult`,
   `ReflectionResult` — all surfaceable in the HUD (VB9) and the FeedbackLog.
5. **Streaming + cancellation everywhere.** Replies, tool steps, and local inference must
   stream and be cancellable (`Flow` + structured concurrency). The UI cancels instantly.
6. **Deterministic where possible.** Heuristic intent classification on-device (already in
   `IntentClassifier`) keeps the common path fast and offline before any model is touched.

## 4. The on-device model's role (per Kian's directive: "very light & fast, only assists")

- **Cloud is the primary brain** (providers). The local model is an **assistant**, used for:
  intent classification, short drafts, offline/privacy mode, and as the final VB5 fallback.
- **Keep it tiny:** Qwen2.5-0.5B Q4 (~0.4 GB) primary. For phone speed, prefer **MediaPipe
  LLM Inference API** (GPU-accelerated, simpler than raw llama.cpp JNI) for LM1; fall back to
  llama.cpp only if a model MediaPipe can't host is needed. Thermal/thread guard + unload on idle.
- The framework must be **fully functional with the local model absent** — it only *upgrades*
  quality/latency/privacy when present. (This is already true structurally via VB5.)

## 5. How to get "the best feedback" — make quality measurable

A revolutionary brain needs a **feedback/eval loop**, not vibes:

1. **FeedbackLog** — persist `{prompt, decision, model, latency, success, userRating?}` per turn.
2. **Eval harness** — a golden set of prompts (English, **Persian, and Persian+English
   code-switch** — directly targeting the v20 complaint) run in CI against a mock/real model;
   assert intent classification + tool selection + no-silent-failure. Make it a unit test
   so regressions can't ship.
3. **In-product signal** — a tiny 👍/👎 on each reply feeds the FeedbackLog; over time it
   biases routing (a provider/model that gets 👎 on coding loses coding rank via the graph).
4. **Latency budget** — assert on-device path < ~300 ms to first action; cloud TTFB tracked
   by the AvailabilityGraph (already collected).

## 6. Concrete next-version plan (batched — one new version, per new cadence)

Bundle these into the next release (fixes v20 + adds framework depth):

- **FIX (v20 bugs):** make text replies speak (FV1), real voice picker from
  `TextToSpeech.voices` (FV2), multi-locale/auto STT + code-switch system prompt (FV3).
- **FEATURE:** `AgentEngine` v1 (ReAct loop, bounded, tested with a scripted model) +
  `ToolCaller` (function-calling JSON) wired to the existing `ToolRegistry`.
- **FEATURE:** `MemoryStore` v1 — episodic log + semantic retrieval (reuse MiniLM embedder)
  injected into the orchestrator context.
- **FEATURE:** `FeedbackLog` + a code-switch eval test (locks in the Persian fix).

Each lands as its own commit (build+test green), but they ship together as **one new
version** that both fixes v20 and adds the agent loop — exactly the cadence Kian asked for.

## 7. What to avoid

- No provider-specific branching in business logic. No blocking the main thread with
  inference. No swallowing errors (use the never-silent pattern). No giant god-ViewModels —
  the agent loop lives in `core/`, the HUD just renders its `StateFlow`.
- Don't bundle the model in the APK; don't bake real keys into a public APK (use a proxy).
