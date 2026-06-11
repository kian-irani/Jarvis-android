"""Structured JSON logging for Vision Brain (M0).

One JSON object per line on stdout — friendly to journald/Loki/CloudWatch.
"""

import json
import logging
import sys
import time

_RESERVED = set(
    logging.LogRecord("", 0, "", 0, "", (), None).__dict__.keys()
) | {"message", "asctime"}


class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload = {
            "ts": round(time.time(), 3),
            "level": record.levelname,
            "logger": record.name,
            "msg": record.getMessage(),
        }
        if record.exc_info:
            payload["exc"] = self.formatException(record.exc_info)
        for key, value in record.__dict__.items():
            if key not in _RESERVED and not key.startswith("_"):
                payload[key] = value
        return json.dumps(payload, ensure_ascii=False, default=str)


def setup_logging(level: int = logging.INFO) -> logging.Logger:
    """Configure root logger for JSON output; idempotent."""
    root = logging.getLogger()
    root.setLevel(level)
    if not any(isinstance(h.formatter, JsonFormatter) for h in root.handlers):
        handler = logging.StreamHandler(sys.stdout)
        handler.setFormatter(JsonFormatter())
        root.handlers = [handler]
    # keep uvicorn access noise at WARNING; app logs carry the signal
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    return logging.getLogger("vision.brain")
