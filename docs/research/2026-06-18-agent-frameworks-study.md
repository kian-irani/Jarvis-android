# Agent-Framework Study — what Vision should learn from AutoGen, CrewAI, LangGraph & openclaw

> Date: 2026-06-18 · Author: Claude (research pass) · Status: input to the **Vision Cognitive Framework (VCF) PRD**
> Purpose: these four projects are **open-source references we study, not dependencies** — Vision stays
> source-available and ships its own Kotlin/Python code. We mine their *architecture* to fix the weakest part
> of Vision today: the **agentic loop** (think → act → observe → re-plan), multimodal perception, and the
> multi-agent runtime. We do **not** copy code (esp. anything GPL/AGPL); we reimplement the patterns.

---

## 0. Where Vision is today (honest baseline)

Vision's **router brain is strong**, its **agent brain is thin**.

| Layer | Module (as-built) | Verdict |
|------|-------------------|---------|
| Model selection | `router/orchestrator/VisionOrchestrator`, `capability/CapabilityRouter`, `health/AvailabilityGraph`, `substitution/SubstitutionEngine`, `cost/CostController`, `data/ai/TokenPool`, `registry/ModelRegistry` | **Good.** Capability → ranked candidates → circuit-breaker → substitution chain → cost gate → key rotation → telemetry. This is genuinely competitive. |
| Provider I/O | `data/ai/CloudChatRouter` (277 LOC) | **OK but flat.** Tries providers in order, rotates keys via `TokenPool`, injects memory + history, single text-in/text-out. No function-calling API, no streaming, no image/audio body. |
| Planning | `core/planner/TaskPlanner` | **Primitive.** Splits the goal on connectors (`then`/`سپس`/`؛`). No model-backed decomposition, no dependencies, no DAG. |
| Agent loop | `core/agent/AgentEngine` | **Primitive.** Walks a *pre-computed linear* plan, `maxSteps`-bounded, `stopOnFailure`. **No observation feedback, no re-planning, no reflection.** It is a sequential executor, not a reasoning loop. |
| Tool calling | `core/agent/ToolCaller` + `data/tools/*` | **Primitive.** Regex-extracts **one** `{"tool","args"}` JSON from prose. String args. No JSON-schema tools, no native function-calling, no parallel calls, no tool registry exposed to the model. |
| Memory | `core/memory/MemoryEngine` (CF4) | **Good foundation.** Typed + importance + recency-decay over a MiniLM vector store; graceful. Missing: summarization, working-memory state, long-term store API. |
| Perception | — | **Missing.** `Intent.IMAGE/AUDIO` + `Capability.VISION/AUDIO` exist in the *type system*, but there is **no actual image or audio pipeline** (no screenshot/camera→VLM, no OCR, no STT→reasoning). Voice today is only TTS/STT for the chat surface. |
| Multi-agent | `data/agent/AgentRegistry` (UI roster) | **Cosmetic.** A list of named agents for the UI; there is no delegation, no roles driving behaviour, no agent-to-agent messaging. |

**Conclusion:** Vision needs a real **cognitive runtime** layered on top of the router. The four references are exactly the four pieces of that runtime.

---

## 1. LangGraph — the execution substrate (state graph + durability)

**What it is:** a low-level orchestration framework for *stateful, long-running* agents. Inspired by **Pregel** (bulk-synchronous super-steps) and Apache Beam; public API inspired by NetworkX.

**Core concepts:**
- **`StateGraph`** — the agent is a **graph of nodes** (units of work) connected by **edges**. **Conditional edges** route on the current state → this is how you get branching and **cycles** (loop back to "call model" after a tool runs). The flat linear plan in Vision's `AgentEngine` is the thing this replaces.
- **Typed shared state + reducers** — every node reads/writes one typed state object; a **reducer** says how updates merge (e.g. `messages` is *append*, `scratchpad` is *replace*). This is the backbone that makes a loop coherent.
- **Checkpointing / durable execution** — state is persisted at each super-step, so a run can **fail and resume**, be **paused** for human input, and support **time-travel** (rewind to a prior checkpoint and branch). For Vision this means: an agent task survives the app being backgrounded/killed (Android reality).
- **Human-in-the-loop (interrupts)** — `interrupt` pauses the graph, surfaces state to the user, resumes with their answer. This is the *correct* substrate for Vision's **Trust Levels** (pause before a `CRITICAL` action).
- **Streaming** — stream tokens *and* intermediate node events, not just the final answer. Feeds the Orb state machine (THINKING/EXECUTING) and live telemetry.
- **Subgraphs / prebuilt ReAct agent** — composable graphs; a batteries-included `create_react_agent`.

**Lessons for Vision (highest-value):**
1. Replace `AgentEngine`'s linear walk with a **`VisionGraph`** (nodes + conditional edges + cycles).
2. Introduce a **typed `GraphState`** with reducers (messages append, plan replace, observations append).
3. Add a **Room-backed `Checkpointer`** → durable resume on Android + HIL pause for Trust gating + time-travel for Vision Lab dry-runs.
4. Make the run **streamed** (`Flow<GraphEvent>`) → Orb/telemetry update live.

> ⚠️ License note: LangGraph is MIT — patterns are safe to reimplement. We still write our own Kotlin; we don't vendor it.

---

## 2. AutoGen — the runtime model (event-driven actors + agent-as-tool)

**What it is (v0.4 architecture):** a **three-tier layered** framework — exactly the layering Vision should adopt:
1. **Core API** — message passing, **event-driven agents (actor model)**, local *and* distributed runtime, cross-language (Python/.NET).
2. **AgentChat API** — opinionated high-level API for common patterns (two-agent, group chat).
3. **Extensions API** — pluggable LLM clients + capabilities (e.g. code execution).

**Core concepts:**
- **Asynchronous messaging** — agents are actors that react to messages; no shared call stack. This decouples "who is thinking" from "who is acting" and scales to many agents/devices (maps to Vision's Mesh).
- **Agent-as-tool** (`AgentTool`) — wrap a specialised agent as a callable tool so a coordinator can route to it (math expert, code expert). A clean, composable alternative to a hard-coded planner.
- **Group chat / handoffs** — multiple agents converse to solve a task; a manager decides who speaks next.
- **Code execution agent** — a first-class agent that writes and *runs* code in a sandbox.
- **Configurable tool-iteration limits** — bound how many tool calls before concluding (Vision already bounds `maxSteps` — same instinct).
- **Human-in-the-loop** — a user can be a participant in the conversation.

**Lessons for Vision:**
1. **Adopt the 3-layer split** explicitly: `vcf-core` (graph/runtime), `vcf-agents` (opinionated agent presets), `vcf-ext` (model clients = the existing router; tools; code-exec). This is the cleanest way to grow without entangling UI and brain (and it lines up with the DS-F `vision-brain` extraction already planned).
2. **Event-driven core** — an internal message bus between agents/nodes (ties into the planned `DS-B5 EventBus`), so the Mesh ("every device a Brain") can later run agents on different nodes by routing messages.
3. **Agent-as-tool** — the simplest multi-agent primitive; ship it before a full Crew.

> License note: AutoGen is MIT/CC-BY — safe to learn from.

---

## 3. CrewAI — the team model (roles + process + structured tasks)

**What it is:** a **standalone** (no LangChain) multi-agent framework optimised for *teams of role-playing agents*. Two complementary paradigms:
- **Crews** = autonomy. Agents with **Role / Goal / Backstory** collaborate under a **Process**:
  - **Sequential** — tasks run in order.
  - **Hierarchical** — a **manager agent** plans, delegates, and validates the others' work.
- **Flows** = control. Event-driven pipelines with decorators **`@start` / `@listen` / `@router`**, conditional branching (`and_`, `or_`), and explicit **state**. Crews can be embedded inside Flows: autonomy where you want it, determinism where you need it.

**Core concepts worth stealing:**
- **Role/Goal/Backstory** as the unit of specialisation — cheap, prompt-level, and effective. Vision's `AgentRegistry` already *names* agents; this gives those names *behaviour*.
- **Tasks with structured outputs** — a Task declares its `expected_output` and can force **JSON / typed (Pydantic) output** + **dependencies** between tasks. This is how you get reliable hand-offs instead of parsing prose.
- **Hierarchical process / delegation** — a manager agent decomposes and routes. This is the principled version of Vision's planned `AGT Agent Society` + `AgentDelegate`.
- **Flows + state** — maps almost 1:1 onto Vision's planned `DS-B5 EventBus` + `CF5 Scheduler`: triggers (`app_opened`, `user_idle`, time) → a flow → a crew/agent.
- **Standalone, low-overhead** — CrewAI deliberately avoids a heavy dependency graph. Vision (on-device, battery-bound) should do the same: pure-Kotlin orchestration, model calls only when needed.

**Lessons for Vision:**
1. Give agents **Role/Goal/Backstory** structs that compile into system prompts (`core/team/AgentRole`).
2. Add a **`Crew` + `Process`** abstraction (sequential first, hierarchical/manager second) — this *is* the `AGT` track, now with a real design.
3. Make **Tasks carry a typed output contract** (kotlinx-serialization schema) so agent hand-offs are structured, not regex-scraped.
4. Wire **Flows-style triggers** to the EventBus/Scheduler (absorbs `DS-B5` + `CF5`).

> License note: CrewAI is MIT — safe to learn from. Standalone design is the model for an on-device brain.

---

## 4. openclaw — the product runtime (gateway, skills, voice, sandboxed trust)

**What it is:** a **local-first personal AI assistant** (TypeScript/Node) — a single-user **control plane** that brokers many messaging channels to isolated agents. This is the closest analogue to *Vision-as-a-product*, so it's the most directly applicable.

**Architecture patterns (high value for Vision):**
- **Gateway as a broker, not an orchestrator.** Thin channel adapters push everything to a central RPC gateway that owns *sessions, channels, tools, events*. → Vision's launcher / floating widget / voice / automation should all be **thin channels** into one **Vision Gateway**; the brain lives once, behind it. (Lines up with planned `DS-C` in-process + network planes and the Brain-Lite Ktor server.)
- **Turn-based agent loop with configurable thinking depth** (`/think <level>`) and **prompt injection files** (`AGENTS.md` = identity, `SOUL.md` = values, `TOOLS.md` = capabilities, `SKILL.md` per skill). → Vision should let *persona + values + tool catalogue* be **data injected into the system prompt**, not hard-coded — Vision's `systemPrompt()` already half does this with persona sliders; generalise it.
- **Skill/Tool registry surfaced to the model for capability discovery.** Skills live in a workspace folder and are *described to the model* so it can choose them dynamically — no retraining. → Vision needs a **`ToolRegistry` that emits machine-readable tool specs** to the model (native function-calling), plus a discoverable skill format. (Aligns with planned `MCP / Plugin SDK / DS-X1`.)
- **Provider failover + auth-profile rotation** (multiple keys per provider, graceful degradation). → **Vision already has this** (`SubstitutionEngine` + `TokenPool`). Validation that our router design is right; we should expose the same `provider/model-id` config simplicity.
- **Voice pipeline that separates acquisition from processing** — wake word + STT/TTS are **platform-local**, cascading to cloud only when needed; the agent layer just sees text. → Vision's `VoiceController` should formalise this split (FV4 wake word + FV6 provider abstraction already in the plan), keeping latency local-first.
- **Multimodal as optional layers over text** — image inputs flow through multimodal models when available; a **Canvas / Agent-to-UI** lets the agent render visual output. → Vision's `MM` (image understanding) and a future "agent draws a panel" map here; degrade gracefully when the chosen model isn't vision-capable.
- **Security via execution context** — `main` session = full trust on host; non-main sessions = **sandboxed** (Docker/SSH) with **per-sandbox tool allowlists**. → This is the blueprint for Vision's **Trust Levels (`SAFE`)** done right: trust is enforced **at tool-invocation time**, scoped per agent/session, not per channel.

**Lessons for Vision:**
1. Build a **Vision Gateway / control plane** that all surfaces talk to (`core/runtime`), with an **event model** for streaming.
2. Treat **persona, values, and the tool catalogue as injected context** (generalise `systemPrompt()`), enabling skills/plugins without code changes.
3. Make **trust/sandbox a property of the agent+session at tool-call time** — the right home for `SAFE`.
4. Formalise the **voice acquisition vs processing** split; keep wake-word/STT local-first.

> ⚠️ License: confirm openclaw's licence before reusing *anything* verbatim. We reimplement patterns only.

---

## 5. Synthesis — the four pieces become one runtime

| Reference | Gives Vision | Vision module it becomes |
|-----------|--------------|---------------------------|
| **LangGraph** | stateful graph, cycles, checkpoint/resume, HIL interrupts, streaming | `core/graph/` (VCF-G) |
| **AutoGen** | layered design, event-driven actors, agent-as-tool, code-exec, HIL | `vcf-core/agents/ext` split + `core/runtime` bus (VCF-R) |
| **CrewAI** | role/goal agents, Crew + (sequential/hierarchical) process, structured tasks, flows | `core/team/` (VCF-X) + structured tasks (VCF-A) |
| **openclaw** | gateway-as-broker, injected skill/tool catalogue, sandboxed trust, voice split, multimodal-over-text | `core/runtime` gateway (VCF-R), `core/tools` registry+trust (VCF-T), `core/perception` (VCF-M) |

**The thesis (answers "use tokens but decide for itself"):** the **VCF owns control flow** as Vision's own on-device logic (graph + planner + reflection + trust), and **calls the cloud model as a single node** through the *existing* router (which already picks provider/model by capability, health, and cost). Vision is the conductor; providers are interchangeable instruments. Nothing about Vision's intelligence is locked to one vendor — exactly the "sovereign" promise in the README.

**What does NOT change:** the router brain (VB1–VB9) stays — VCF *consumes* it. The launcher (LR/NEO) and Dual System (DS) tracks are unaffected; VCF is the brain those products call. CF4 memory stays and is extended.

---

## 6. Code-level findings (read from source, 2026-06-18)

Not a glance — these are patterns read directly from each repo's source. Each row is something Vision should adopt or deliberately reject.

### openclaw — `packages/agent-core/src/agent-loop.ts` + `types.ts` (the production agent loop)
The single most instructive file for Vision (it *is* a personal assistant loop). Concrete patterns:
- **Outer/inner loop with steering** — `runLoop` has an inner loop (`while hasMoreToolCalls || pendingMessages.length`) that processes tool calls, wrapped in an outer loop that resumes when **steering messages** (user typed mid-run) arrive. `getSteeringMessages()` is a config hook. → Vision must let the user **interrupt/redirect a running agent** (barge-in for agents, not just TTS).
- **Resumable** — separate `agentLoop` (new prompt) vs `agentLoopContinue` (resume from existing context; rejects continuing from an `assistant` message). → maps to a **Checkpointer** for Android process death.
- **Streamed lifecycle events** — every step emits `agent_start / turn_start / message_start / message_end / turn_end / agent_end`. → feeds the **Orb state machine** and telemetry for free.
- **Tool batch with early-termination** — `executeToolCalls` returns `{ messages, terminate }`; the loop only stops when **every** finalized tool result sets `terminate`. Mode is `sequential | parallel` (`ToolExecutionMode`).
- **Lifecycle hooks (the key insight)** — `beforeToolCall` can return `{ block: true, reason }` to **veto a tool before it runs** (this is *exactly* where Vision's Trust/SAFE gate belongs); `afterToolCall` can override the result; `prepareNextTurn` returns an `AgentLoopTurnUpdate` that can **swap the model / thinking level / context before the next provider call** (dynamic model escalation mid-run).
- **`convertToLlm`** — converts the agent's internal `AgentMessage[]` into LLM `Message[]` *per call*, filtering UI-only messages. → keep Vision's rich event log separate from the token-billed model payload.
- **Failure-as-data contract** — the stream fn "must not throw"; failures are encoded as a final assistant message with `stopReason: "error"|"aborted"` + `errorMessage`. → Vision already values honest reporting; make it a hard contract.

### AutoGen — `autogen-core/_image.py` + `autogen-agentchat/agents/_assistant_agent.py`, `teams/_group_chat/*`
- **Multimodal contract** — `Image` wraps PIL → `to_base64()` → `data_uri` → `to_openai_format()` = `{"type":"image_url","image_url":{"url": dataUri, "detail": ...}}`, with MIME sniffing from magic bytes. → Vision's `CloudChatRouter.ask()` must build **content-part messages** (image_url for OpenAI/Gemini, base64 `source` blocks for Anthropic) instead of text-only.
- **Layered actor runtime** — `_base_agent.py` + `_single_threaded_agent_runtime.py`: agents are actors addressed by `AgentId`, communicating by async messages; the runtime is the bus. `teams/_group_chat/` has `_round_robin`, `_selector` (a model picks who speaks next), `_swarm` (handoffs), `_base_group_chat_manager`. → blueprint for Vision's multi-agent + Mesh.
- **Handoff** (`base/_handoff.py`) and **agent-as-tool** (`tools/_agent.py`) — two clean multi-agent primitives; ship agent-as-tool first.

### LangGraph — `prebuilt/chat_agent_executor.py`, `graph/state.py`, `pregel/_loop.py`
- **State as typed dict + reducer** — `AgentState = {messages: Annotated[Sequence, add_messages], remaining_steps: RemainingSteps}`. The **reducer** (`add_messages` = append/merge) is *the* idea that makes a cyclic loop coherent; `remaining_steps` bounds it. → Vision's `GraphState` should carry `messages (append)`, `plan (replace)`, `observations (append)`, `remainingSteps`.
- **ReAct = compiled graph** — node `agent` (call model) → conditional edge → `ToolNode` → back to `agent`, else `END`. Plus `Checkpointer` (durable) and `BaseStore` (long-term memory) injected at compile. → Vision's `VisionGraph` mirrors this exactly, on-device in Kotlin.

### CrewAI — `agents/crew_agent_executor.py`, `crew.py`, `process.py`, `flow/flow.py`
- **Native function-calling with text fallback** — `convert_tools_to_openai_schema`, a dedicated `function_calling_llm`, and on `is_native_tool_calling_unsupported_error` it builds a `text_tool_calling_fallback_message` and parses `AgentAction/AgentFinish` (ReAct text). → Vision should **prefer native function-calling**, and keep the current regex/`ToolCaller` path *only* as the fallback for models without it.
- **Production guardrails** — `enforce_rpm_limit`, `respect_context_window` + `handle_context_length`, `has_reached_max_iterations` + `handle_max_iterations_exceeded`, before/after **LLM hooks** and before/after **tool hooks**, `ask_for_human_input` (HITL), `track_delegation_if_needed`. → these are the unglamorous things that make an agent survive the real world; Vision needs the same set.
- **Roles + Process + Flows** — `crew.py` runs agents under `process.py` (`sequential` / `hierarchical` with a manager), `flow/flow.py` is the event-driven `@start/@listen/@router` layer with its own state. → `VCF-X` (team) + the EventBus/Scheduler (`VCF-R`).

### Net new tasks these findings create (beyond the obvious graph/ReAct)
1. **Steering / interruptible agent runs** (openclaw) — the user can redirect a running agent. New: `VCF-A` steering hook + Orb "stop/redirect".
2. **Tool lifecycle hooks as the Trust seam** (openclaw `beforeToolCall.block`) — implement `SAFE` *as* a `beforeToolCall` veto, not a separate ad-hoc check.
3. **Mid-run model escalation** (`prepareNextTurn`) — start cheap/local, escalate to a stronger provider when a step is hard. Ties the router (VB) into the loop dynamically.
4. **`convertToLlm` seam** — separate the billed model payload from the UI event log (token-cost + privacy win).
5. **Native function-calling + text fallback** (CrewAI) — upgrade `ToolCaller` from "only path" to "fallback path".
6. **RPM / context-window guardrails** (CrewAI) — Vision has token-pool health but not request-rate or context-length handling in the loop.

---

See the full design + task breakdown in **[`docs/2026-06-18-vision-cognitive-framework-PRD.md`](../2026-06-18-vision-cognitive-framework-PRD.md)** and the live tasks under **VCF** in **[`plu/PLAN.md`](../../plu/PLAN.md)**.
