package com.kianirani.jarvis.core.event

/** Coarse category of a [VisionEvent], used to register triggers by type. */
enum class EventKind { WAKE_WORD, APP_OPENED, USER_IDLE, SCHEDULED, COMMAND_RECEIVED, CONTEXT_CHANGED, CUSTOM }

/**
 * VCF-R2 — something that happened that Vision may react to. Surfaces (accessibility,
 * wake-word service, scheduler) publish these to the [VisionEventBus]; [Trigger]s decide
 * which ones run the agent. (openclaw events / CrewAI Flow `@listen`.)
 */
sealed interface VisionEvent {
    val kind: EventKind

    data object WakeWord : VisionEvent {
        override val kind get() = EventKind.WAKE_WORD
    }

    data class AppOpened(val packageName: String) : VisionEvent {
        override val kind get() = EventKind.APP_OPENED
    }

    data class UserIdle(val millis: Long) : VisionEvent {
        override val kind get() = EventKind.USER_IDLE
    }

    data class Scheduled(val id: String) : VisionEvent {
        override val kind get() = EventKind.SCHEDULED
    }

    /** A command arrived from a surface (HUD/widget/voice/remote); [source] tags its origin. */
    data class CommandReceived(val text: String, val source: String = "main") : VisionEvent {
        override val kind get() = EventKind.COMMAND_RECEIVED
    }

    /** A piece of device context changed (e.g. foreground app), so subscribers can re-ground. */
    data class ContextChanged(val key: String, val value: String = "") : VisionEvent {
        override val kind get() = EventKind.CONTEXT_CHANGED
    }

    data class Custom(val name: String, val data: String = "") : VisionEvent {
        override val kind get() = EventKind.CUSTOM
    }
}
