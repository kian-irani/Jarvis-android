"""VCF-B1 — pydantic models mirroring the Kotlin `core/protocol` DTOs (DS-F2).

Field names, defaults, and enum spellings match the kotlinx `@Serializable` data classes exactly
so a JSON produced by either side decodes on the other (conformance). kotlinx omits null/default
fields only when `encodeDefaults=false`; the Vision protocol uses `encodeDefaults=true`, so these
models also serialise their defaults. Use `model_dump(mode="json")` to get the wire JSON.
"""

from __future__ import annotations

from enum import Enum

from pydantic import BaseModel, Field


class Channel(str, Enum):
    MAIN = "MAIN"
    GROUP = "GROUP"
    WIDGET = "WIDGET"
    REMOTE = "REMOTE"


class AttachmentKind(str, Enum):
    IMAGE = "IMAGE"
    AUDIO = "AUDIO"
    FILE = "FILE"


class Intent(BaseModel):
    name: str
    confidence: float = 1.0
    slots: dict[str, str] = Field(default_factory=dict)

    @staticmethod
    def chat() -> "Intent":
        return Intent(name="chat")


class DeviceContext(BaseModel):
    foregroundApp: str | None = None
    batteryPercent: int | None = None
    charging: bool | None = None
    network: str | None = None
    locale: str | None = None
    timeOfDay: str | None = None
    unreadNotifications: int | None = None
    extras: dict[str, str] = Field(default_factory=dict)


class Attachment(BaseModel):
    kind: AttachmentKind
    mime: str
    base64: str = ""
    uri: str | None = None


class VisionRequest(BaseModel):
    text: str
    sessionId: str = "main"
    channel: Channel = Channel.MAIN
    locale: str | None = None
    intent: Intent | None = None
    deviceContext: DeviceContext | None = None
    attachments: list[Attachment] = Field(default_factory=list)


class VisionResponse(BaseModel):
    sessionId: str
    text: str = ""
    intent: Intent | None = None
    finished: bool = True
    awaitingConfirmation: bool = False
    error: str | None = None


class MemoryDto(BaseModel):
    """Mirror of the CF4/DS-B3 memory wire shape used by DS-C3 sync."""

    id: str
    content: str
    type: str
    importance: float = 0.5
    createdAt: int = 0


class ToolSpec(BaseModel):
    """Mirror of `core/tools/ToolSpec` — the JSON the agent advertises to a model.

    `parameters` is a JSON-Schema object; `trust` mirrors the `ActionRisk` enum name.
    """

    name: str
    description: str
    parameters: dict = Field(default_factory=dict)
    trust: str = "AUTO"
    readOnly: bool = False
