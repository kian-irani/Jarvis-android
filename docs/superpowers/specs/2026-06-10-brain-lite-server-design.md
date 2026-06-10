---
title: Brain-Lite Server (10 endpoints) — Design Spec — 05-vision
tags: [project, vision, spec, design, active]
date: 2026-06-10
status: active
project: 05-vision
---

# Brain-Lite Server — Design Spec

**Slice:** Phase 1 / M1 — full 10-endpoint Ktor server inside the Android app.
**Decisions made:** cloud API for chat + on-device MiniLM embeddings; Room DB from day one; localhost-only binding; pragmatic layered architecture (Approach A).

## Goal

A ForegroundService-hosted HTTP server on `127.0.0.1:7799` exposing the 10 Brain-Lite endpoints, so the Vision app (and later, mesh peers) talk to one local API regardless of where intelligence actually runs.

## Architecture

All code under `android/app/src/main/java/com/jarvis/vision/brain/`:

```
brain/
├── BrainLiteService.kt        ForegroundService, START_STICKY, owns KtorServer lifecycle,
│                              persistent notification, WakeLock per in-flight request
├── server/
│   ├── KtorServer.kt          CIO engine, binds 127.0.0.1:7799, ContentNegotiation
│   │                          (kotlinx.serialization), StatusPages → JSON error envelope
│   └── routes/                HealthRoutes, ChatRoutes, EmbedRoutes, MemoryRoutes,
│                              NodeRoutes, TaskRoutes, FileRoutes, SearchRoutes,
│                              StatusRoutes, EventRoutes
├── data/
│   ├── db/                    Room: VisionDatabase + entities/DAOs (memories, nodes, tasks)
│   ├── ChatRepository.kt      Groq client, 3-key rotation (mirrors brain-full)
│   ├── EmbeddingRepository.kt ONNX Runtime + paraphrase-multilingual-MiniLM-L12-v2;
│   │                          lazy download with checksum verify; 2GB storage budget
│   ├── MemoryRepository.kt    Room-backed episodic/semantic memory + embeddings
│   └── FileRepository.kt      app-private dirs only; path traversal rejected
└── di/BrainModule.kt          Hilt bindings
```

Data flow: route handler → repository → (Room | Groq HTTP | ONNX session) → serialized response. Routes never touch Room/HTTP/ONNX directly; this boundary lets the Discovery/Election slice later swap `ChatRepository` for a "forward to elected Brain" implementation without touching routes. `BrainLiteService` is the only component aware of Android lifecycle.

## API Contracts

Envelope for all responses: `{"ok": true, "data": ...}` / `{"ok": false, "error": {"code", "message"}}`.

| Endpoint | Behavior |
|---|---|
| `GET /health` | uptime, version, capabilities (`chat: cloud`, `embed: local`), model-download state, storage usage |
| `POST /chat` | `{messages[], model?, stream?}` → Groq with 3-key rotation; SSE when `stream: true` |
| `POST /embed` | `{texts[]}` → 384-dim MiniLM vectors; `503 MODEL_NOT_READY` until model present |
| `GET/POST /memory` | POST `{type: episodic\|semantic, content, metadata}` stored with its embedding; GET lists with paging/filter |
| `GET/POST /nodes` | mesh node registry CRUD in Room; registration only — no liveness checks in this slice |
| `POST /task` | enqueue `{kind, payload}` → Room status `pending`; coroutine worker executes; `GET /task/{id}` for status |
| `GET/POST /files` | list/read/write within app-private storage only |
| `POST /search` | embed query → cosine similarity over memory embeddings → top-k |
| `GET /status` | token-pool state: per-key rate-limit status, request counts |
| `POST /events` + `GET /events/stream` | components POST UI events; SSE stream for UI subscription |

## Data Model (Room)

Mirrors Brain-Full PostgreSQL naming so future sync is a column mapping, not a redesign:

- `memories(id UUID, type, content, metadata JSON, embedding BLOB float32[384], created_at, updated_at)`
- `nodes(id, name, address, capabilities JSON, brain_score, last_seen)`
- `tasks(id, kind, payload JSON, status, result JSON, created_at, finished_at)`

Search loads embedding vectors into memory and scores by cosine similarity. **Known limit:** acceptable up to ~50k memories; ANN indexing is a later optimization.

## Error Handling

- `StatusPages` maps exceptions to typed codes: `MODEL_NOT_READY`, `ALL_KEYS_RATE_LIMITED`, `STORAGE_BUDGET_EXCEEDED`, `NOT_FOUND`, `VALIDATION`.
- Groq 429 → rotate key; all three limited → 503 with retry-after.
- No silent fallbacks: every degraded state surfaces in `/health`.

## Lifecycle

- Foreground notification shows server state (running / model downloading / error).
- START_STICKY; WakeLock acquired per request, released in `finally`.
- MiniLM model download runs as a `/task` (survives, reports progress, checksum-verified, counts against the 2GB budget).

## Testing

- **Routes:** Ktor `testApplication` + Mockk repositories — success and error paths per endpoint (JVM-only).
- **Repositories:** Room in-memory DB for DAOs; Groq client against a mock web server, key rotation covered explicitly.
- **Embedding:** instrumented test for ONNX output shape; JVM test for checksum/budget logic with a fake downloader.
- **CI:** tests join the existing `CI · Android` workflow.

## Out of Scope (future slices)

LAN exposure + auth, Discovery/Election/mDNS, on-device LLM, Brain-Full sync, Doze/JobScheduler handling.
