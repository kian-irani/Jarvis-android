"""Tests for structured JSON logging (M0)."""
import json
import logging

from brain.logging_config import JsonFormatter, setup_logging


def _format(record_msg: str, **extra) -> dict:
    logger = logging.getLogger("vision.test")
    record = logger.makeRecord("vision.test", logging.INFO, __file__, 1, record_msg, (), None, extra=extra)
    return json.loads(JsonFormatter().format(record))


def test_format_is_json_with_core_fields():
    out = _format("hello")
    assert out["msg"] == "hello"
    assert out["level"] == "INFO"
    assert out["logger"] == "vision.test"
    assert isinstance(out["ts"], float)


def test_extra_fields_are_included():
    out = _format("node registered", node="pixel-7", device_type="phone")
    assert out["node"] == "pixel-7"
    assert out["device_type"] == "phone"


def test_setup_logging_idempotent():
    setup_logging()
    setup_logging()
    root = logging.getLogger()
    json_handlers = [h for h in root.handlers if isinstance(h.formatter, JsonFormatter)]
    assert len(json_handlers) == 1
