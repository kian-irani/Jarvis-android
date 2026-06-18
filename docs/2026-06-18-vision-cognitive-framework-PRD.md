# PRD — Vision Cognitive Framework (VCF) v1.0

> Status: **draft / planning** · Date: 2026-06-18 · Owner: Kian · Author: Claude (research + design pass)
> Companion: **[research/2026-06-18-agent-frameworks-study.md](research/2026-06-18-agent-frameworks-study.md)** (what we learned from AutoGen, CrewAI, LangGraph, openclaw — with code-level findings).
> Task list: the **VCF** block in **[`plu/PLAN.md`](../plu/PLAN.md)**. Every task references a section here by its `§` number.

---

## 0. How to use this PRD

This is the **reference manual** for building Vision's brain. When you pick a `VCF-*` task from `plu/PLAN.md`:
1. Open the matching `§` here. It has: **goal**, **design**, a **Kotlin code sample** (illustrative, not necessarily compiling), the **reference repo** the pattern comes from, and **acceptance criteria**.
2. Follow the repo rules in [`plu/CLAUDE.md`](../plu/CLAUDE.md): pure logic is JVM-testable (TDD), UI via `ui-ux-pro-max`, no mockups, build green before claiming done.
3. The code samples define **contracts and shapes**, not final code. Match the surrounding style of `core/` and `router/`.

**Non-negotiable principle (the whole point):**
> **VCF owns the control flow on-device; the cloud model is one node it calls through the existing VB router.**
> Vision *decides* (plan, route, gate, reflect) in its own Kotlin code. Providers are rented muscle, chosen by `VisionOrchestrator` on capability/health/cost, swappable at any time. Nothing about Vision's intelligence is locked to one vendor — this is the "sovereign" promise made real.

### Glossary
- **VB** — the existing VISION BRAIN router (`router/*`): orchestrator, capability, availability graph, substitution, cost, token pool. **Stays. VCF consumes it.**
- **VCF** — Vision Cognitive Framework: the new agentic runtime layered on VB.
- **Node / Graph / State / Reducer / Checkpoint / Interrupt** — LangGraph vocabulary (see §3, §4).
- **Tool / ToolSpec / Trust gate** — function-calling + safety (see §7).
- **Crew / Role / Process / Delegation** — multi-agent (CrewAI/AutoGen, see §9).
- **Gateway / Channel / EventBus** — runtime control plane (openclaw, see §10).

---

## 1. Background — where Vision is, and the gap

Vision's **router brain (VB1–VB9) is strong**; its **agent brain (CF1–CF4) is thin**.

| Concern | Today | Problem |
|--------|-------|---------|
| Model selection | `VisionOrchestrator` → `CapabilityRouter` → `AvailabilityGraph` → `SubstitutionEngine` → `CostController` → `TokenPool` | **Good — keep.** |
| Reasoning loop | `AgentEngine.run()` walks a **static linear** `ActionPlan` | No observe→re-plan, no reflection, no cycles. |
| Planning | `TaskPlanner` splits a string on `then`/`سپس` | No model-backed decomposition, no DAG, no deps. |
| Tool calling | `ToolCaller` regex-parses **one** `{"tool","args"}` | No JSON-schema tools, no native function-calling, no parallel calls, no trust gate. |
| Multimodal | `Intent.IMAGE/AUDIO` types exist; **no pipeline** | Can't actually see a screenshot or reason over audio. |
| Multi-agent | `AgentRegistry` = a UI list | No roles driving behaviour, no delegation. |
| Durability | none | An Android task dies when the process is killed. |
| Memory | `MemoryEngine` (CF4) typed vector recall | Good base; no summarization / working-memory in state. |

**VCF fills every "Problem" row** by importing the right pattern from the four references (§2).

---

## 2. What each reference contributes (one-line map)

| Reference | License | Vision imports |
|-----------|---------|----------------|
| **LangGraph** | MIT | the **execution substrate**: `StateGraph` (nodes+conditional edges+**cycles**), typed state + **reducers**, **checkpointing** (durable resume + time-travel), **interrupts** (HIL), streaming. → §3, §4, §5 |
| **AutoGen** | MIT/CC-BY | **layered design** (core/agents/ext), **actor runtime** (async messages, `AgentId`), **group chat** (round-robin / selector / swarm-handoff), **agent-as-tool**, the **multimodal `Image`** contract. → §6, §8, §9 |
| **CrewAI** | MIT | **roles** (role/goal/backstory), **Process** (sequential / hierarchical-manager), **structured task outputs**, **native FC with text fallback**, production guardrails (RPM, context-window, max-iter, HITL, delegation). → §6, §7, §9 |
| **openclaw** | (verify) | the **gateway-as-broker** + **event model**, **agent-loop hooks** (`beforeToolCall` veto = trust gate, `afterToolCall.terminate`, `prepareNextTurn` model-swap), **steering** (interrupt a running agent), **skills injected into the prompt**, **sandbox/allowlist trust**, **voice acquisition/processing split**. → §7, §8, §10 |

> We **reimplement patterns in our own Kotlin/Python**. We do not vendor or copy code (especially anything GPL/AGPL). Confirm openclaw's licence before reusing literal text.

---

## 3. Architecture overview

Adopt **AutoGen's three-layer split** so UI and brain never entangle (and it lines up with the planned `DS-F vision-brain` extraction):

```
┌──────────────────────────────────────────────────────────────────────┐
│  SURFACES (thin channels)  — Launcher · Floating Widget · Voice · Auto │
└───────────────┬──────────────────────────────────────────────────────┘
                │  VisionRequest / Flow<GraphEvent>
┌───────────────▼──────────────────────────────────────────────────────┐
│  VCF-R  Runtime / Gateway / EventBus   (openclaw broker)               │
├───────────────────────────────────────────────────────────────────────┤
│  VCF-X  Team: Roles · Crew · Process · Delegation   (CrewAI/AutoGen)   │
├───────────────────────────────────────────────────────────────────────┤
│  VCF-A  Agents: ReAct loop · Planner · Reflect · Steering (LG/CrewAI)  │
├──────────────┬──────────────────────────┬─────────────────────────────┤
│  VCF-G Graph │  VCF-T Tools + Trust      │  VCF-M Perception (img/aud) │
│  runtime     │  (FC schema, gate,        │  (screenshot→VLM, OCR,      │
│  (LangGraph) │   sandbox)                │   STT, wake word)           │
├──────────────┴──────────────────────────┴─────────────────────────────┤
│  VCF-MEM Memory (CF4 + summarization + store)                          │
├───────────────────────────────────────────────────────────────────────┤
│  VB ROUTER (existing): orchestrator · capability · health · subst ·    │
│  cost · token pool   ←  the "which model & is it healthy/cheap" layer  │
└───────────────────────────────────────────────────────────────────────┘
                │
        Providers (Anthropic · Gemini · OpenAI · Groq · Grok · OpenRouter · Local)
```

**Where the code lives**
- Primary: **Kotlin `app/src/main/java/com/kianirani/jarvis/`** (on-device, local-first):
  - `core/graph/` (VCF-G) · `core/agent/` (VCF-A, extends existing) · `core/tools/` + `data/tools/` (VCF-T) · `core/perception/` (VCF-M) · `core/team/` (VCF-X) · `core/runtime/` (VCF-R) · `core/memory/` (VCF-MEM, extends CF4).
  - Reuses `router/*` (VB) unchanged via an injected facade.
- Mirror (optional heavy tier): **Python `brain/`** (FastAPI) for the Brain-Full node — same contracts, for VPS/PC (VCF-B, §13). On-device is the source of truth; brain/ is an accelerator.

---

## 4. Core contracts (data model)

These types are the spine — everything else depends on them. Pure Kotlin, kotlinx-serialization, JVM-testable.

### 4.1 Messages with multimodal content parts (`core/graph/VisionMessage.kt`)
The single change that unlocks image + audio: a message is a **list of typed content parts**, not a string. (AutoGen/`ChatCompletion` content-parts model.)

```kotlin
enum class Role { SYSTEM, USER, ASSISTANT, TOOL }

sealed interface ContentPart {
    @Serializable data class Text(val text: String) : ContentPart
    /** Raw bytes; base64/data-URI is built per-provider at send time (see §8.3). */
    @Serializable data class Image(val bytes: ByteArray, val mime: String = "image/png") : ContentPart
    @Serializable data class Audio(val bytes: ByteArray, val mime: String = "audio/ogg") : ContentPart
    /** A function call the model asked for. */
    @Serializable data class ToolCall(val id: String, val name: String, val argsJson: String) : ContentPart
    /** The result we send back for a ToolCall. */
    @Serializable data class ToolResult(
        val callId: String, val name: String,
        val content: List<ContentPart>, val isError: Boolean = false,
    ) : ContentPart
}

@Serializable
data class VisionMessage(val role: Role, val content: List<ContentPart>, val name: String? = null) {
    fun text(): String = content.filterIsInstance<ContentPart.Text>().joinToString(" ") { it.text }
    fun toolCalls(): List<ContentPart.ToolCall> = content.filterIsInstance<ContentPart.ToolCall>()
    fun hasImage(): Boolean = content.any { it is ContentPart.Image }
}
```

### 4.2 Graph state + reducers (`core/graph/GraphState.kt`)
LangGraph's idea: nodes return **partial updates**; the runner **reduces** them per channel (messages *append*, plan *replace*). This is what makes a cyclic loop coherent.

```kotlin
@Serializable
data class GraphState(
    val messages: List<VisionMessage> = emptyList(),   // reducer: APPEND  (LangGraph add_messages)
    val plan: ActionPlan? = null,                       // reducer: REPLACE
    val observations: List<Observation> = emptyList(),  // reducer: APPEND
    val remainingSteps: Int = 12,                        // bound (LangGraph remaining_steps)
    val scratch: Map<String, String> = emptyMap(),       // reducer: MERGE
    val sessionId: String = "",
)

/** A node never mutates state; it returns this. The runner applies it. */
data class StateUpdate(
    val appendMessages: List<VisionMessage> = emptyList(),
    val plan: ActionPlan? = null,
    val appendObservations: List<Observation> = emptyList(),
    val spendStep: Boolean = false,
    val scratch: Map<String, String> = emptyMap(),
)

fun GraphState.reduce(u: StateUpdate): GraphState = copy(
    messages = messages + u.appendMessages,
    plan = u.plan ?: plan,
    observations = observations + u.appendObservations,
    remainingSteps = remainingSteps - if (u.spendStep) 1 else 0,
    scratch = scratch + u.scratch,
)
```
✅ **Acceptance:** `reduce` is pure; unit tests cover append vs replace vs merge and the step bound.

### 4.3 Tool contract (`core/tools/ToolSpec.kt`) — §7
### 4.4 Events (`core/graph/GraphEvent.kt`) — streamed lifecycle (openclaw)
```kotlin
sealed interface GraphEvent {
    data class NodeStart(val node: String) : GraphEvent
    data class NodeEnd(val node: String, val update: StateUpdate) : GraphEvent
    data class Token(val delta: String) : GraphEvent                  // streaming text
    data class ToolStart(val call: ContentPart.ToolCall) : GraphEvent
    data class ToolEnd(val result: ContentPart.ToolResult) : GraphEvent
    data class Interrupted(val reason: String, val payload: JsonObject) : GraphEvent  // HIL pause
    data class Done(val state: GraphState) : GraphEvent
    data class Failed(val message: String, val aborted: Boolean) : GraphEvent
}
```

---

## 5. §VCF-G — The Graph Runtime (from LangGraph)

**Goal:** replace `AgentEngine`'s linear walk with a real **graph of nodes** connected by **conditional edges**, supporting **cycles**, **checkpointing** (durable resume on Android process death + HIL pause), and **streaming**.

### 5.1 Node + builder
```kotlin
fun interface Node { suspend fun run(state: GraphState, ctx: NodeContext): NodeResult }

sealed interface NodeResult {
    data class Continue(val update: StateUpdate, val goto: String? = null) : NodeResult
    data class Interrupt(val reason: String, val payload: JsonObject) : NodeResult
}

const val END = "__end__"

class VisionGraph private constructor(
    private val nodes: Map<String, Node>,
    private val edges: Map<String, String>,
    private val conditional: Map<String, (GraphState) -> String>,
    private val entry: String,
) {
    class Builder {
        private val nodes = mutableMapOf<String, Node>()
        private val edges = mutableMapOf<String, String>()
        private val conditional = mutableMapOf<String, (GraphState) -> String>()
        private var entry = ""
        fun addNode(name: String, node: Node) = apply { nodes[name] = node }
        fun addEdge(from: String, to: String) = apply { edges[from] = to }
        fun addConditionalEdge(from: String, route: (GraphState) -> String) = apply { conditional[from] = route }
        fun setEntry(name: String) = apply { entry = name }
        fun compile(checkpointer: Checkpointer? = null) =
            CompiledGraph(VisionGraph(nodes, edges, conditional, entry), checkpointer)
    }
}
```

### 5.2 Runner — Pregel-style step loop that streams + checkpoints
```kotlin
class CompiledGraph(private val g: VisionGraph, private val checkpointer: Checkpointer?) {

    /** Stream a run. Resumes from a saved checkpoint if [threadId] has one. */
    fun stream(input: GraphState, threadId: String, ctx: NodeContext): Flow<GraphEvent> = flow {
        var state = checkpointer?.load(threadId) ?: input
        var current = checkpointer?.loadCursor(threadId) ?: g.entry
        while (current != END) {
            coroutineContext.ensureActive()                       // cancellable (AgentEngine already does this)
            emit(GraphEvent.NodeStart(current))
            when (val r = g.node(current).run(state, ctx)) {
                is NodeResult.Continue -> {
                    state = state.reduce(r.update)
                    val next = r.goto ?: g.next(current, state)
                    checkpointer?.save(threadId, state, next)     // durable: survive process death
                    emit(GraphEvent.NodeEnd(current, r.update))
                    current = next
                }
                is NodeResult.Interrupt -> {                       // HIL: pause, persist, return
                    checkpointer?.save(threadId, state, current)  // resume re-enters the SAME node
                    emit(GraphEvent.Interrupted(r.reason, r.payload))
                    return@flow
                }
            }
        }
        emit(GraphEvent.Done(state))
    }.catch { e -> emit(GraphEvent.Failed(e.message ?: "graph failed", aborted = e is CancellationException)) }

    /** Resume after an interrupt (user answered a trust prompt / clarifying question). */
    fun resume(threadId: String, answer: VisionMessage, ctx: NodeContext): Flow<GraphEvent> {
        val saved = checkpointer!!.load(threadId)!!.reduce(StateUpdate(appendMessages = listOf(answer)))
        return stream(saved, threadId, ctx.copy(preApproved = ctx.preApproved + threadId))
    }
}
```

### 5.3 Checkpointer (`core/graph/Checkpointer.kt`) — Room-backed
```kotlin
interface Checkpointer {
    suspend fun save(threadId: String, state: GraphState, cursor: String)
    suspend fun load(threadId: String): GraphState?
    suspend fun loadCursor(threadId: String): String?
    suspend fun history(threadId: String): List<GraphState>   // time-travel (Vision Lab dry-run)
}
// Impl: a Room table {threadId, seq, cursor, stateJson, ts}; load() = latest seq. Pure JSON via kotlinx-serialization.
```

**Reference:** LangGraph `graph/state.py` (`add_messages` reducer, `remaining_steps`), `pregel/_loop.py` (super-step loop), `checkpoint/*` (durability), `types.py` (`interrupt`).
✅ **Acceptance:** a graph with a cycle (`a→b→a`) terminates on `remainingSteps`; an `Interrupt` persists and `resume` continues from the same node; `stream` emits ordered events; killing mid-run and re-calling `stream` resumes. All JVM-tested with fake nodes (no model/network).

---

## 6. §VCF-A — The Agent layer (ReAct loop, planner, reflection, steering)

**Goal:** assemble the canonical **observe→think→act→re-plan** loop *as a graph*, calling the cloud through VB, with a model-backed planner, optional self-reflection, and **steering** (user can interrupt/redirect a running agent — openclaw).

### 6.1 The ReAct graph (replaces the old `AgentEngine` walk)
```kotlin
@Singleton
class ReActAgentFactory @Inject constructor(
    private val model: ModelNode, private val tools: ToolNode, private val reflect: ReflectNode,
    private val checkpointer: Checkpointer,
) {
    fun build(): CompiledGraph = VisionGraph.Builder()
        .addNode("model", model)            // §6.2 — calls provider via VB with tool schema
        .addNode("tools", tools)            // §7.3 — executes tool calls, trust-gated
        .addNode("reflect", reflect)        // §6.4 — optional self-critique
        .setEntry("model")
        .addConditionalEdge("model") { s ->
            val last = s.messages.last()
            when {
                last.toolCalls().isNotEmpty() && s.remainingSteps > 0 -> "tools"
                s.scratch["needsReflection"] == "1"                  -> "reflect"
                else                                                  -> END
            }
        }
        .addEdge("tools", "model")          // cycle: feed observations back to the model
        .addEdge("reflect", "model")
        .compile(checkpointer)
}
```

### 6.2 `ModelNode` — uses VB router + native function-calling + mid-run escalation
```kotlin
class ModelNode @Inject constructor(
    private val orchestrator: VisionOrchestrator,   // EXISTING VB brain — decides provider/model
    private val cloud: CloudChatRouter,             // EXISTING provider I/O (extended for FC + multimodal, §8.3)
    private val registry: ToolRegistry,
) : Node {
    override suspend fun run(state: GraphState, ctx: NodeContext): NodeResult {
        // Vision decides WHICH model — capability/health/cost — and may ESCALATE on hard steps (openclaw prepareNextTurn).
        val decision = orchestrator.decide(
            message = state.lastUserText(),
            hasImage = state.messages.any { it.hasImage() },
            difficulty = ctx.difficultyHint(state),         // escalate cheap→strong when stuck
        )
        val reply: VisionMessage = cloud.complete(
            chosen = decision.chosen,
            messages = state.messages,
            toolSchema = registry.functionSchema(decision.chosen),  // native FC (§7.2); null → text fallback
        ).getOrElse { return NodeResult.Continue(StateUpdate(
            appendMessages = listOf(assistantError(it.message)), spendStep = true)) }
        return NodeResult.Continue(StateUpdate(appendMessages = listOf(reply), spendStep = true))
    }
}
```

### 6.3 `PlannerNode` — model-backed decomposition (replaces string-split `TaskPlanner`)
Keep the cheap on-device `TaskPlanner` as a **fast path**; add a model-backed planner for real multi-step goals, producing a **typed plan with dependencies** (CrewAI structured tasks).
```kotlin
@Serializable data class PlanStepV2(
    val id: String, val instruction: String, val kind: StepKind,
    val dependsOn: List<String> = emptyList(), val toolHint: String? = null,
)
@Serializable data class ActionPlan(val goal: String, val steps: List<PlanStepV2>)

class PlannerNode(private val cloud: CloudChatRouter, private val orchestrator: VisionOrchestrator) : Node {
    override suspend fun run(state: GraphState, ctx: NodeContext): NodeResult {
        val plan = cloud.completeStructured<ActionPlan>(            // ask the model for JSON matching the schema
            chosen = orchestrator.decide(state.lastUserText()).chosen,
            messages = state.messages + planningInstruction(),
        ).getOrElse { return NodeResult.Continue(StateUpdate(plan = TaskPlanner.fallback(state.lastUserText()))) }
        return NodeResult.Continue(StateUpdate(plan = plan))
    }
}
```

### 6.4 `ReflectNode` — self-correction (CrewAI/AutoGen reflection)
```kotlin
class ReflectNode(private val cloud: CloudChatRouter, private val orchestrator: VisionOrchestrator) : Node {
    override suspend fun run(state: GraphState, ctx: NodeContext): NodeResult {
        val critique = cloud.complete(orchestrator.decide("critique").chosen,
            state.messages + critiqueInstruction("Is the last answer correct, complete, honest? If not, fix it."))
            .getOrNull() ?: return NodeResult.Continue(StateUpdate(scratch = mapOf("needsReflection" to "0")))
        return NodeResult.Continue(StateUpdate(
            appendMessages = listOf(critique), scratch = mapOf("needsReflection" to "0")))
    }
}
```

### 6.5 Steering — interrupt/redirect a running agent (openclaw)
```kotlin
interface SteeringSource { suspend fun drain(): List<VisionMessage> }   // user-typed/spoken messages queued mid-run
// In the runner, before each model turn: inject drained steering messages into state (QueueMode = all | one-at-a-time).
// Surfaced in the UI as the Orb's "stop / redirect" affordance while THINKING/EXECUTING.
```

**Reference:** LangGraph `prebuilt/chat_agent_executor.py` (ReAct-as-graph); CrewAI `crew_agent_executor.py` (max-iter, native-FC-with-text-fallback, reflection, HITL); openclaw `agent-loop.ts` (steering, `prepareNextTurn` escalation).
✅ **Acceptance:** a 2-tool task loops model→tools→model→answer; the planner returns a typed `ActionPlan` (deps respected); reflection can revise an answer once; steering messages injected mid-run change the next turn; everything bounded by `remainingSteps`. JVM-tested with a fake `CloudChatRouter`.

---

## 7. §VCF-T — Tools, function-calling & Trust (CrewAI + openclaw)

**Goal:** turn the single-regex `ToolCaller` into a real **tool layer**: JSON-schema tools, **native function-calling** with text fallback, parallel execution, and a **Trust gate implemented as a `beforeToolCall` veto** (openclaw `block:true`).

### 7.1 Tool contract + registry
```kotlin
enum class TrustLevel { AUTO, SUGGEST, CONFIRM, CRITICAL }   // §SAFE

@Serializable data class ToolSpec(
    val name: String, val description: String,
    val parameters: JsonObject,              // JSON Schema (draft-07 subset)
    val trust: TrustLevel = TrustLevel.AUTO,
)
interface VisionTool {
    val spec: ToolSpec
    suspend fun execute(args: JsonObject, ctx: ToolContext): ContentPart.ToolResult
}

@Singleton
class ToolRegistry @Inject constructor(
    private val tools: Set<@JvmSuppressWildcards VisionTool>,
    private val allowlist: ToolAllowlist,    // openclaw per-session sandbox allowlist
) {
    fun byName(n: String) = tools.firstOrNull { it.spec.name == n }
    /** OpenAI/Gemini/Anthropic-shaped function schema for the model. Null when the model lacks FC → text fallback. */
    fun functionSchema(model: ModelSpec): JsonArray? =
        if (!model.supportsFunctionCalling) null
        else buildJsonArray { tools.filter { allowlist.permits(it.spec.name) }.forEach { add(it.spec.toOpenAiFunction()) } }
}
```

### 7.2 Native FC + text fallback (CrewAI pattern)
```kotlin
// In CloudChatRouter.complete(): if toolSchema != null, send "tools"/"tool_choice" (OpenAI), "tools" (Anthropic),
// "functionDeclarations" (Gemini), and parse tool_calls into ContentPart.ToolCall.
// If the model/provider does NOT support FC (or returns is_native_tool_calling_unsupported):
//   fall back to the EXISTING TOOL_PROTOCOL prompt + ToolCaller regex (keep it — now the fallback path, not the only path).
fun parseToolCalls(raw: JsonObject, provider: AiProvider): List<ContentPart.ToolCall> { /* per-provider */ }
```

### 7.3 `ToolNode` — trust gate + parallel exec
```kotlin
class ToolNode @Inject constructor(
    private val registry: ToolRegistry, private val safety: SafetyLayer,
) : Node {
    override suspend fun run(state: GraphState, ctx: NodeContext): NodeResult {
        val calls = state.messages.last().toolCalls()

        // TRUST GATE (openclaw beforeToolCall → block). CRITICAL/CONFIRM tools pause for the user.
        val needsOk = calls.firstOrNull { safety.requiresConfirmation(registry.byName(it.name)?.spec, it) }
        if (needsOk != null && !ctx.preApproved(needsOk.id))
            return NodeResult.Interrupt("confirm_tool:${needsOk.name}", needsOk.toJson())

        // Parallel for read-only tools; sequential when a tool can mutate device state.
        val results = calls.map { call ->
            val tool = registry.byName(call.name)
                ?: return@map errorResult(call, "unknown tool '${call.name}'")
            runCatching { tool.execute(call.argsObject(), ctx.toolContext) }
                .getOrElse { errorResult(call, it.message ?: "tool failed") }   // failure-as-data
        }
        return NodeResult.Continue(StateUpdate(
            appendMessages = results.map { VisionMessage(Role.TOOL, listOf(it)) },
            appendObservations = results.map { Observation(it.name, it.isError) },
        ))
    }
}
```

### 7.4 `SafetyLayer` (`core/agent/SafetyLayer.kt`) — pure, TDD (= the planned `SAFE`)
```kotlin
object SafetyLayer {
    private val alwaysCritical = setOf("send_sms", "call", "send_email", "delete", "purchase", "transfer")
    private val alwaysAuto = setOf("get_time", "get_battery", "open_app", "flashlight")
    fun requiresConfirmation(spec: ToolSpec?, call: ContentPart.ToolCall): Boolean = when {
        spec == null -> true                                  // unknown → confirm
        spec.trust == TrustLevel.CRITICAL -> true
        spec.name in alwaysCritical -> true
        spec.name in alwaysAuto -> false
        else -> spec.trust == TrustLevel.CONFIRM
    }
}
```

**Reference:** CrewAI `convert_tools_to_openai_schema`, `function_calling_llm`, text-fallback parser; openclaw `types.ts` (`beforeToolCall.block`, `ToolExecutionMode`, `afterToolCall.terminate`), sandbox allowlists.
✅ **Acceptance:** a FC-capable model produces structured `ToolCall`s; a non-FC model still works via the regex fallback; a `CRITICAL` tool triggers an `Interrupt` and only runs after `resume` with approval; unknown tool → graceful error observation; `SafetyLayer` fully unit-tested.

---

## 8. §VCF-M — Perception: image & audio (the user's explicit ask)

**Goal:** make Vision actually **see and hear**. Two pipelines, both **graceful-degrading** (openclaw "multimodal as optional layers over text").

### 8.1 Image pipeline (`core/perception/`)
```kotlin
/** Capture what's on screen (or camera/gallery) and reason over it with a VISION-capable model. */
class VisualPerception @Inject constructor(
    private val orchestrator: VisionOrchestrator, private val agent: ReActAgentFactory,
    private val ocr: OnDeviceOcr,                  // ML Kit — fallback when no vision model reachable
) {
    suspend fun seeScreen(prompt: String, screenshot: ByteArray): Flow<GraphEvent> {
        val canVisionModel = orchestrator.decide(prompt, hasImage = true).chosen?.supportsVision == true
        val userMsg = if (canVisionModel)
            VisionMessage(Role.USER, listOf(ContentPart.Text(prompt), ContentPart.Image(screenshot)))
        else                                          // degrade: OCR → text-only
            VisionMessage(Role.USER, listOf(ContentPart.Text(prompt + "\n[screen text]\n" + ocr.read(screenshot))))
        return agent.build().stream(GraphState(messages = listOf(userMsg)), threadId = "vision", ctx())
    }
}
```
Sources of images: `MediaProjection` / `AccessibilityService.takeScreenshot()` (screen understanding → drives device actions), camera, gallery picker. Screen understanding + the Accessibility tool layer = "**look at the screen and tap the right thing**".

### 8.2 Audio pipeline — acquisition (local) vs processing (agent) (openclaw split)
```kotlin
class AudioPerception @Inject constructor(
    private val stt: SpeechToText,                  // FV6 abstraction: Android SpeechRecognizer → Vosk/Whisper → cloud
    private val agent: ReActAgentFactory,
) {
    /** Speak → on-device STT → reason. Audio bytes only go to the cloud if no local STT and the model is audio-capable. */
    suspend fun listen(): Flow<GraphEvent> {
        val text = stt.transcribeOnce()             // local first (latency + privacy)
        return agent.build().stream(GraphState(messages = listOf(userText(text))), "voice", ctx())
    }
}
// Wake word (FV4): Porcupine / openWakeWord on the existing Brain-Lite foreground service → emits VisionEvent.WakeWord
//   to the gateway (§10). Budget < 1%/hr battery; Doze-safe.
```

### 8.3 Provider multimodal encoding (extend `CloudChatRouter.ask`) — AutoGen `Image.to_openai_format`
This is the concrete "image processing at the I/O boundary": build the right content-part shape per provider.
```kotlin
private fun ContentPart.Image.encode(provider: AiProvider): JsonObject = when (provider) {
    AiProvider.OPENAI, AiProvider.XAI, AiProvider.GROQ, AiProvider.OPENROUTER -> buildJsonObject {
        put("type", "image_url")
        putJsonObject("image_url") { put("url", "data:$mime;base64,${base64(bytes)}") }
    }
    AiProvider.ANTHROPIC -> buildJsonObject {
        put("type", "image")
        putJsonObject("source") { put("type", "base64"); put("media_type", mime); put("data", base64(bytes)) }
    }
    AiProvider.GEMINI -> buildJsonObject {           // inline_data inside parts
        putJsonObject("inline_data") { put("mime_type", mime); put("data", base64(bytes)) }
    }
}
// MIME sniff from magic bytes (AutoGen pattern) when unknown: ÿØÿ=jpeg, \x89PNG=png, RIFF…WEBP=webp.
```

**Reference:** AutoGen `_image.py` (data-URI + `to_openai_format` + MIME sniff); openclaw `media-core/inline-image-data-url.ts`, `speech-core/*` (voice models, tts), voice acquisition/processing split.
✅ **Acceptance:** sending a screenshot to a vision-capable model returns a grounded answer; with no vision model, OCR degrade still answers; audio path transcribes locally then reasons; per-provider image encoding unit-tested against fixture JSON. (Device-dependent bits marked "needs on-device confirmation".)

---

## 9. §VCF-X — Multi-agent team (CrewAI + AutoGen)

**Goal:** give the named agents in `AgentRegistry` real behaviour: **roles**, a **Process** (sequential / hierarchical-manager), **delegation**, and **agent-as-tool**. (= the planned `AGT Agent Society`, now designed.)

```kotlin
@Serializable data class AgentRole(
    val id: String, val role: String, val goal: String, val backstory: String,
    val tools: Set<String> = emptySet(),       // allowlist into ToolRegistry
) { fun systemPrompt() = "You are a $role. Your goal: $goal.\n$backstory" }

enum class Process { SEQUENTIAL, HIERARCHICAL }

class Crew @Inject constructor(private val agentFactory: ReActAgentFactory) {
    suspend fun run(agents: List<AgentRole>, process: Process, manager: AgentRole?, task: String): CrewResult =
        when (process) {
            Process.SEQUENTIAL ->                 // CrewAI sequential: each agent's output feeds the next
                agents.fold(CrewResult.start(task)) { acc, role -> acc + runAgent(role, acc.context) }
            Process.HIERARCHICAL ->               // manager plans → delegates → validates (AutoGen selector/swarm)
                managerLoop(manager!!, agents, task)
        }
}

/** AutoGen "agent-as-tool": expose a sub-agent as a callable tool the manager can invoke. */
class AgentAsTool(private val sub: CompiledGraph, override val spec: ToolSpec) : VisionTool {
    override suspend fun execute(args: JsonObject, ctx: ToolContext): ContentPart.ToolResult {
        val out = sub.stream(GraphState(messages = listOf(userText(args.string("task")))), ctx.threadChild(), ctx.node)
            .lastFinalText()
        return ContentPart.ToolResult(ctx.callId, spec.name, listOf(ContentPart.Text(out)))
    }
}
```

**Reference:** CrewAI `crew.py`/`process.py` (sequential/hierarchical, delegation), AutoGen `teams/_group_chat/_selector_group_chat.py` + `_swarm_group_chat.py` (who-speaks-next, handoffs), `tools/_agent.py` (agent-as-tool).
✅ **Acceptance:** a 2-agent sequential crew passes output forward; a hierarchical crew has the manager delegate to a worker and validate; agent-as-tool runs a sub-graph and returns text; all bounded + cancellable.

---

## 10. §VCF-R — Runtime / Gateway / Events (openclaw)

**Goal:** one **control plane** that every surface (launcher, floating widget, voice, automation) talks to; it owns **sessions/workspaces**, an **EventBus**, and **streams** `GraphEvent`s back. Surfaces stay thin (openclaw "gateway as broker"). (= absorbs `DS-B5 EventBus`, `DS-C` planes, ties `CF5 Scheduler`.)

```kotlin
@Serializable data class VisionRequest(
    val text: String? = null, val image: ByteArray? = null, val audio: ByteArray? = null,
    val channel: Channel, val sessionId: String = "main",
)
enum class Channel { LAUNCHER, WIDGET, VOICE, AUTOMATION, MESH }

sealed interface VisionEvent {                      // CrewAI Flow @listen triggers
    data class WakeWord(val phrase: String) : VisionEvent
    data class AppOpened(val pkg: String) : VisionEvent
    data class UserIdle(val ms: Long) : VisionEvent
    data class Scheduled(val workflowId: String) : VisionEvent
}

@Singleton
class VisionGateway @Inject constructor(
    private val sessions: SessionStore,             // per-session isolated graph + state (openclaw workspace isolation)
    private val agentFactory: ReActAgentFactory,
    private val bus: EventBus,
) {
    fun submit(req: VisionRequest): Flow<GraphEvent> {
        val msg = req.toVisionMessage()             // text/image/audio → ContentParts (§8)
        val graph = sessions.graphFor(req.sessionId)
        return graph.stream(GraphState(messages = listOf(msg), sessionId = req.sessionId), req.sessionId, node())
    }
}

@Singleton
class EventBus @Inject constructor() {              // = DS-B5
    private val _events = MutableSharedFlow<VisionEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<VisionEvent> = _events
    suspend fun emit(e: VisionEvent) = _events.emit(e)
}
```
Sessions: `main` = full trust on the user's device; group/automation sessions = restricted tool allowlist (openclaw main-vs-non-main). The existing **Brain-Lite Ktor server** becomes the *network plane* of this gateway (`DS-C2`: add `/v1/stream` WebSocket token streaming).

**Reference:** openclaw gateway/control-plane, `gateway-protocol/schema*`, session/workspace isolation, event-driven sessions; CrewAI Flow `@start/@listen/@router`.
✅ **Acceptance:** all four channels submit through one gateway and receive a `Flow<GraphEvent>`; an `EventBus` event (e.g. `WakeWord`) can trigger a run; group session can't call a `CRITICAL` tool.

---

## 11. §VCF-MEM — Memory deepening (extends CF4)

**Goal:** add **conversation summarization** (compress old turns into a SEMANTIC memory — the planned `DS-B3`), keep **working memory in `GraphState`**, and expose a **long-term store** (LangGraph `BaseStore` analog) the agent can read/write as a tool.
```kotlin
class ConversationSummarizer(private val cloud: CloudChatRouter, private val memory: MemoryEngine) {
    /** When history > N turns, compress the oldest into one SEMANTIC summary memory; drop them from the window. */
    suspend fun maybeSummarize(history: List<VisionMessage>): List<VisionMessage> {
        if (history.size <= KEEP) return history
        val old = history.dropLast(KEEP)
        val summary = cloud.complete(/* cheap model */, summariseInstruction(old)).getOrNull() ?: return history
        memory.remember(summary.text(), MemoryType.SEMANTIC, importance = 0.6f)
        return history.takeLast(KEEP)
    }
    private companion object { const val KEEP = 8 }
}
// "remember"/"recall" also exposed AS TOOLS so the agent can deliberately store/retrieve (LangGraph store-as-tool).
```
**Reference:** LangGraph `store/base.py` (long-term store), CrewAI memory; builds on CF4 `MemoryEngine`.
✅ **Acceptance:** long conversations stay within budget via summarization; recalled summaries appear in later answers; memory read/write tools work; all graceful when the embedding model isn't downloaded.

---

## 12. §VCF-E — Observability & Eval

**Goal:** every run is a **trace** (the `GraphEvent` stream persisted) feeding telemetry (VB9), and a **golden-eval harness** (extends `EvalHarness`/`FeedbackLog`) gates regressions: plan quality, tool-call correctness, refusal rate, FA/EN/code-switch, latency.
```kotlin
data class RunTrace(val threadId: String, val events: List<GraphEvent>, val tokens: Int, val cost: Double, val ms: Long)
// EvalCase{input, expectTool?, expectNoRefusal, lang} → run graph with fake provider → assert. Wire into CI like existing tests.
```
✅ **Acceptance:** a run produces a persisted trace surfaced in the HUD; the eval suite runs in CI and fails on a regression.

---

## 13. §VCF-B — Brain-Full (Python) parity (optional heavy tier)

Mirror the **same contracts** in `brain/` (FastAPI) so a VPS/PC node can run heavier graphs (bigger models, longer tasks) and the phone offloads to it via the Mesh when elected. Keep on-device as source of truth; `brain/` is an accelerator. Map: `core/graph` → `brain/router` + `brain/agents`; reuse LangGraph-style nodes in Python where a dependency is acceptable on a server (it is — `brain/` already uses FastAPI). Contracts (`VisionMessage`, `ToolSpec`, `GraphEvent`) are serialized identically (kotlinx ↔ pydantic) so phone and brain interoperate.
✅ **Acceptance:** a graph run can be delegated to `brain/` and streamed back; identical request/response JSON on both sides (conformance test).

---

## 14. How VCF uses providers ("decide itself, but use tokens")

1. The **graph** (Vision's own code) decides *what to do*: plan, which node next, whether to use a tool, whether to gate, whether to reflect, whether to ask the user.
2. When it needs inference, the **`ModelNode` asks `VisionOrchestrator`** (VB) *which provider/model* — by capability, health (`AvailabilityGraph`), cost (`CostController`), with key rotation (`TokenPool`) and substitution fallback.
3. `CloudChatRouter` performs the call with the user's tokens; on failure VB substitutes the next candidate or the local model.
4. The model's output is **just data** flowing back into `GraphState`; the graph keeps control.

→ Swapping Anthropic for Gemini for a local model changes **one node's backend**, never the reasoning. That is the sovereignty guarantee.

---

## 15. Phasing & sequencing (foundation-first)

| Phase | Tasks | Outcome |
|------|-------|---------|
| **VCF-0 Foundation** | §4 contracts, §5 graph runtime + checkpointer | A real, durable, streamed graph engine. Nothing user-visible yet. |
| **VCF-1 ReAct** | §6 ReAct graph + ModelNode + §7 tools + native FC + trust gate | The agent actually loops, calls tools natively, gates risky actions. **Replaces `AgentEngine`.** |
| **VCF-2 Perception** | §8 image + audio | Vision can see a screenshot and hear speech. |
| **VCF-3 Planner+Reflect+Memory** | §6.3, §6.4, §11 | Multi-step plans, self-correction, summarized memory. |
| **VCF-4 Team** | §9 roles + crew + delegation | Specialised agents collaborate. |
| **VCF-5 Runtime** | §10 gateway + EventBus + streaming + §12 eval | One control plane; proactive triggers; CI evals. |
| **VCF-6 Brain parity** | §13 | Heavy tier on VPS/PC via Mesh. |

Sequencing vs the launcher (LR/NEO) and Dual System (DS) tracks is **the maintainer's call** — VCF is the brain those products call, so it can proceed in parallel with UI work. Recommended: VCF-0 and VCF-1 next, since they unblock everything agentic.

---

## 16. Mapping to existing PLAN items (no duplication)

VCF **subsumes and sequences** several open items — do NOT build them separately:
- `CF5 Scheduler` → triggers via §10 EventBus.
- `AGT Agent Society` + `AgentDelegate` → §9.
- `MM Multimodal` → §8.
- `SAFE Safety & Trust` → §7.4 + §7.3 (implemented as the `beforeToolCall` gate).
- `DS-B1 planner` → §6.3 · `DS-B3 memory/summarization` → §11 · `DS-B4 task engine` → §5/§6 · `DS-B5 EventBus` → §10.
- `FV4 wake word` / `FV6 voice abstraction` → §8.2.
Existing **VB router** and **CF4 memory** are **kept and consumed**, not replaced.

---

## 17. Risks, non-goals, open decisions
- **Battery/latency** — graphs + extra model calls cost power. Mitigate: on-device fast paths (cheap planner, OCR), local-first STT, `CostController` gating, parallel read-only tools only.
- **Device QA** — perception/voice need real-device confirmation (no emulator on the build server). Mark such tasks accordingly.
- **Non-goals (v1):** distributed multi-device graph execution (Mesh comes after VCF-6), training/fine-tuning, a visual graph editor.
- **Open decisions:** (a) summarization model choice; (b) how aggressively to escalate models mid-run; (c) whether group sessions ever get write tools. Decide per task.

## 18. Testing
Per `plu/CLAUDE.md`: pure logic (reducers, `SafetyLayer`, planner parsing, provider encoders, graph routing) is **JVM-unit-tested with fakes** (no model/network). UI/device bits → "needs on-device confirmation". `gw :app:compileDebugKotlin :app:testDebugUnitTest` green before any task is ticked.
