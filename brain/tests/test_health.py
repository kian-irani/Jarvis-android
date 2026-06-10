"""Smoke/characterization tests for Vision Brain health surface (M0).

Pins current behavior of the root and health endpoints so the foundation
milestone has a green, meaningful CI gate before deeper refactors.
"""
from fastapi.testclient import TestClient

from brain.main import app

client = TestClient(app)


def test_root_reports_online():
    r = client.get("/")
    assert r.status_code == 200
    body = r.json()
    assert body["nodes_online"] == 0
    assert body["nodes_total"] == 0


def test_health_liveness_is_static_and_cheap():
    r = client.get("/health/live")
    assert r.status_code == 200
    assert r.json() == {"status": "alive"}


def test_health_readiness_reports_brain_state():
    r = client.get("/health/ready")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "ready"
    assert "groq_keys" in body
    assert body["nodes_total"] == 0


def test_health_full_exposes_metrics():
    r = client.get("/health")
    assert r.status_code == 200
    body = r.json()
    for key in ("status", "cpu", "ram_free_gb", "nodes_online", "groq_keys", "active_key"):
        assert key in body
