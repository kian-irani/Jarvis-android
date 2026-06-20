"""MX2 — mesh model discovery. The Brain reports which local models a node can serve (Ollama /
llama.cpp) so the phone can register them with `backend=mesh` and route inference to them.

Pure parsing/aggregation (the HTTP call to Ollama's `/api/tags` is the I/O half) so it's testable.
"""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class LocalModel:
    """A model a mesh node can serve, advertised to the phone's ModelRegistry."""

    name: str
    size_bytes: int = 0
    family: str = ""
    quantization: str = ""


def parse_ollama_tags(payload: dict) -> list[LocalModel]:
    """Parse Ollama's ``GET /api/tags`` response into [LocalModel]s.

    Shape: ``{"models": [{"name": "qwen2.5:0.5b", "size": 397, "details": {"family": "qwen2",
    "quantization_level": "Q4_0"}}, ...]}``. Missing fields default; a malformed entry is skipped.
    """
    out: list[LocalModel] = []
    for m in payload.get("models", []) or []:
        name = (m.get("name") or "").strip()
        if not name:
            continue
        details = m.get("details") or {}
        out.append(
            LocalModel(
                name=name,
                size_bytes=int(m.get("size") or 0),
                family=str(details.get("family") or ""),
                quantization=str(details.get("quantization_level") or ""),
            )
        )
    return out


@dataclass
class NodeModels:
    """The models one mesh node advertises, keyed by node id."""

    node_id: str
    models: list[LocalModel] = field(default_factory=list)

    def names(self) -> list[str]:
        return [m.name for m in self.models]


def merge_node_models(nodes: list[NodeModels]) -> dict[str, list[str]]:
    """Map each model name → the node ids that can serve it (for mesh routing/MX3)."""
    index: dict[str, list[str]] = {}
    for node in nodes:
        for name in node.names():
            index.setdefault(name, [])
            if node.node_id not in index[name]:
                index[name].append(node.node_id)
    return index
