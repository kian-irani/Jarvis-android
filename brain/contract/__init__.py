"""VCF-B1 — Python mirror of Vision's wire contract.

Mirrors the Kotlin `core/protocol` DTOs (DS-F2) — the flat, network-plane envelope that the
phone and the heavy Brain tier (VPS/PC) exchange for delegation + streaming. We deliberately
mirror the *protocol* DTOs (flat `@Serializable` data classes), not the internal kotlinx
*sealed* runtime types (`ContentPart`/`GraphEvent`), whose FQN class discriminator is awkward to
mirror — the flat envelope is what actually crosses the wire and round-trips byte-for-byte.
"""

from .models import (
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

__all__ = [
    "Attachment",
    "AttachmentKind",
    "Channel",
    "DeviceContext",
    "Intent",
    "MemoryDto",
    "ToolSpec",
    "VisionRequest",
    "VisionResponse",
]
