"""MX2 — mesh model discovery tests."""

from brain.compute.models import LocalModel, NodeModels, merge_node_models, parse_ollama_tags


def test_parse_ollama_tags():
    payload = {
        "models": [
            {"name": "qwen2.5:0.5b", "size": 397, "details": {"family": "qwen2", "quantization_level": "Q4_0"}},
            {"name": "gemma:2b", "size": 1500},
        ]
    }
    models = parse_ollama_tags(payload)
    assert len(models) == 2
    assert models[0] == LocalModel("qwen2.5:0.5b", 397, "qwen2", "Q4_0")
    assert models[1].name == "gemma:2b"
    assert models[1].family == ""  # missing details default


def test_parse_skips_nameless_and_handles_empty():
    assert parse_ollama_tags({}) == []
    assert parse_ollama_tags({"models": [{"size": 1}]}) == []  # no name → skipped


def test_merge_node_models_indexes_by_model():
    nodes = [
        NodeModels("desktop", [LocalModel("qwen2.5:0.5b"), LocalModel("llama3:8b")]),
        NodeModels("vps", [LocalModel("qwen2.5:0.5b")]),
    ]
    index = merge_node_models(nodes)
    assert index["qwen2.5:0.5b"] == ["desktop", "vps"]
    assert index["llama3:8b"] == ["desktop"]


def test_node_models_names():
    n = NodeModels("x", [LocalModel("a"), LocalModel("b")])
    assert n.names() == ["a", "b"]
