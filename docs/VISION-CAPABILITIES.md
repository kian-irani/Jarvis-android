# Vision OS — Capability Catalog v1.0

> **Vision is NOT an AI launcher. Vision is an Agent Operating System.**
> The user states a *goal*; Vision **understands → plans → executes → monitors → learns → reports.**
> This is the canonical capability spec (from the user, 2026-06-15). Every release is measured against it.
> Build foundation: see `2026-06-15-vision-brain-framework-review.md` (CF1–CF6). Tasks: `PLAN.md`.

---

## The 24 capabilities

1. **Memory Engine** — recall past actions/conversations ("what did I last do on the VPN project?" → "3 days ago you edited xray-config.json then restarted the service on the Germany VPS").
2. **Knowledge Graph** — entities + relations ("the Frankfurt server belongs to which project?" → Frankfurt-01 → Jarvis, OpenRouter Proxy, Telegram Bot).
3. **Universal Search** — one query across files, memory, notes, servers, GitHub, chat history.
4. **Vision Timeline** — searchable activity log ("what did I do yesterday?" → 08:10 Telegram · 09:15 GitHub commit · 11:40 VPS login · …).
5. **Goal System** — a stated goal becomes phases ("publish Vision OS" → Infra → Agent Engine → Beta → Launch).
6. **Autonomous Planner** — goal → concrete steps ("install OpenRouter proxy" → SSH → pull → configure ENV → docker up → health check → report).
7. **Multi-Agent System** — specialized agents chained for a task (Memory → Messaging → Analysis → Summary → report).
8. **Android Automation** — drive any app to complete a task ("book a cinema ticket for 8am tomorrow" → open Chrome → fill form → book → report; "send my last download to Telegram").
9. **Notification Intelligence** — triage 200 notifications → 5 critical / 12 important / 183 ignored; "you have 3 important messages from your mother".
10. **Proactive Assistant** — acts before asked ("Germany server is at 94% disk — auto-clean?"; "you have a 16:00 meeting, travel time is 45 min").
11. **Server Agent** — check/operate all VPS ("check all VPS" → health table; "restart the OpenRouter container" → restarted + health check passed).
12. **Smart Messaging** — "tell my mother I'll be late" → find contact → send → confirm delivery.
13. **Smart Calling** — "call Ali" → contact found → calling → call started.
14. **Email Agent** — summarize/triage ("summarize today's important emails") + compose/send (draft → review → send → confirm).
15. **Writing Assistant** — watches typing in any app and offers a better version in the overlay, then writes it.
16. **Browser Agent** — "play Shakira's latest song" → YouTube → found → playing; "check the Bitcoin price" → search → analyze → result.
17. **Context Awareness** — knows the current app (Maps → ETA suggestion; banking app → Privacy mode on).
18. **Vision Overlay** — a floating Orb in every app: Chat · Search · Summarize · Translate · Execute.
19. **Workflow Builder** — When (Telegram message) → Then (summarize) → Then (save to Notion) → Then (send to Discord).
20. **Vision Tutor** — step-by-step guidance with automatic execution ("how do I install Docker?").
21. **OS-Level Control** — real device control ("turn off Wi-Fi", "enable battery saver") with real reported results.
22. **Digital Twin** — learns routines (wakes ~9, codes ~11, checks servers in the evening) and suggests accordingly.
23. **Personal Second Brain** — one unified view of everything about a project: docs, chats, commits, servers, tasks, goals, timeline, memory, knowledge graph.
24. **Future Wearables** — earbuds ("Vision, server status?") and glasses ("what is this building?" → image analysis).

### Vision Ultimate Command (the north star)
> "Vision — review the OpenRouter project, report server status, fix the problems, write me a summary, and send it on Telegram."
> → Goal understood → planning → executing → monitoring → reporting → **task completed.**

---

## How the framework delivers this (capability → component)

Every capability above is **the same engine + a tool/skill/agent** on top — not bespoke code:

| Capability | Delivered by (PLAN / foundation) |
|---|---|
| 1 Memory · 23 Second Brain | **CF4 MemoryStore** (episodic + semantic via MiniLM) + Knowledge Graph store |
| 2 Knowledge Graph | Knowledge Graph store (entities/relations) over MemoryStore |
| 3 Universal Search | AnySearch (Phase 6) over files/memory/notes/servers/GitHub |
| 4 Timeline · 9 Notifications · 17 Context · 22 Digital Twin | **MON** (monitoring + Timeline) + Context detector + pattern learner |
| 5 Goals · 6 Planner | **CF3 TaskPlanner** + Goal System |
| 7 Multi-Agent · in-task delegation | **AGSK** agents/skills + **AGT-DELEGATE** (on AgentRegistry) |
| 8 Android Automation · 21 OS Control · 16 Browser · 19 Workflow | **CF2 ToolCaller** + Accessibility action layer + **PAU/PAB** + Workflow engine (Phase 9.5) |
| 10 Proactive · 15 Writing · 18 Overlay | **PAO** overlay + **PAW** writing-assist + **PAS** suggestion engine |
| 11 Server Agent | Server skill (SSH/REST) under AGSK + the existing mesh/brain |
| 12 Messaging · 13 Calling | **SmsTool / CallTool** — already real & committed (`0c017db`) |
| 14 Email | **PAE** email agent |
| 20 Tutor | Vision Tutor (Phase 9.5) + step executor |
| Scheduled ("at 16:00 …") | **AGT-SCHED** + **CF5 Scheduler** |
| The Ultimate Command | **CF1 AgentEngine** ReAct loop orchestrating all of the above |
| Quality / "best feedback" | **CF6 FeedbackLog + eval harness** (EN/FA/code-switch) |

**Invariant across all of it:** *never claim an action without executing it* (CF2). Everything runs
under the **Trust gate**, on-device/encrypted where sensitive, with the user in control.
