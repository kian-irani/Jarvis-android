"""VCF-B1 conformance — the Python contract decodes the JSON the Kotlin side produces and
re-encodes it identically. The fixtures below are exactly what kotlinx (encodeDefaults=true)
emits for the corresponding `core/protocol` DTOs.
"""

from brain.contract import (
    Attachment,
    AttachmentKind,
    Channel,
    DeviceContext,
    Intent,
    MemoryDto,
    ToolSpec,
    VisionRequest,
    VisionResponse,
)


def test_vision_request_round_trips_full_envelope():
    wire = {
        "text": "what's on my screen?",
        "sessionId": "s1",
        "channel": "REMOTE",
        "locale": "fa-IR",
        "intent": {"name": "see_screen", "confidence": 0.9, "slots": {}},
        "deviceContext": {
            "foregroundApp": "com.android.chrome",
            "batteryPercent": 42,
            "charging": False,
            "network": "wifi",
            "locale": "fa-IR",
            "timeOfDay": "evening",
            "unreadNotifications": 3,
            "extras": {},
        },
        "attachments": [{"kind": "IMAGE", "mime": "image/png", "base64": "AAAA", "uri": None}],
    }
    req = VisionRequest.model_validate(wire)
    assert req.channel is Channel.REMOTE
    assert req.deviceContext.batteryPercent == 42
    assert req.attachments[0].kind is AttachmentKind.IMAGE
    assert req.model_dump(mode="json") == wire


def test_minimal_request_applies_kotlin_defaults():
    req = VisionRequest.model_validate({"text": "hi"})
    assert req.sessionId == "main"
    assert req.channel is Channel.MAIN
    assert req.intent is None
    assert req.attachments == []


def test_vision_response_variants():
    finished = VisionResponse.model_validate({"sessionId": "s1", "text": "done"})
    assert finished.finished is True and finished.error is None

    interrupted = VisionResponse.model_validate(
        {"sessionId": "s1", "text": "", "finished": False, "awaitingConfirmation": True}
    )
    assert interrupted.awaitingConfirmation is True

    failed = VisionResponse.model_validate({"sessionId": "s1", "error": "boom"})
    assert failed.error == "boom"


def test_intent_default_confidence():
    assert Intent.chat().name == "chat"
    assert Intent.model_validate({"name": "x"}).confidence == 1.0


def test_memory_dto_round_trip():
    wire = {"id": "m1", "content": "likes dark mode", "type": "SEMANTIC", "importance": 0.8, "createdAt": 123}
    dto = MemoryDto.model_validate(wire)
    assert dto.model_dump(mode="json") == wire


def test_tool_spec_mirror():
    wire = {
        "name": "open_app",
        "description": "open an app",
        "parameters": {"type": "object", "properties": {"pkg": {"type": "string"}}},
        "trust": "AUTO",
        "readOnly": False,
    }
    spec = ToolSpec.model_validate(wire)
    assert spec.name == "open_app"
    assert spec.model_dump(mode="json") == wire


def test_device_context_defaults_are_all_optional():
    ctx = DeviceContext.model_validate({})
    assert ctx.foregroundApp is None
    assert ctx.extras == {}
