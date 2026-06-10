# Vision Brain — Module Map (v16)

Hexagonal (Ports & Adapters). Domain is pure Python; FastAPI/DB/Redis are adapters.

| Module | Responsibility | Phase |
|--------|----------------|-------|
| `api/` | FastAPI routers, health endpoints (`/health/live`, `/health/ready`) | P0/P1 |
| `router/` | Multi-token AI router, smart model selector, cost estimator | P2 |
| `memory/` | Episodic + semantic memory, ChromaDB RAG | P9 |
| `agents/` | Agent pool + ReAct via LangGraph, Vision Scheduler | P5 |
| `transfer/` | VISN protocol (LZ4/zstd, chunked, resume) | P1.5 |
| `compute/` | Distributed compute mesh | P17.5 |
| `language/` | Universal Language Engine (3-tier) — NEW v16 | P7 |
| `security/` | Behavioral Baseline, anomaly detection, vault — NEW v16 | P13 |
| `migrations/` | Alembic DB migrations | PX |
| `tests/` | pytest + pytest-asyncio + coverage | PX |

Stack: Python 3.12 · FastAPI 0.115+ · Pydantic v2 · SQLAlchemy 2.x async · LangGraph 0.2.x · structlog.
