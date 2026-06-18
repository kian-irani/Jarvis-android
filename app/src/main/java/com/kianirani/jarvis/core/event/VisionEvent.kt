package com.kianirani.jarvis.core.event

/** Coarse category of a [VisionEvent], used to register triggers by type. */
enum class EventKind { WAKE_WORD, APP_OPENED, USER_IDLE, SCHEDULED, CUSTOM }

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

    data class Custom(val name: String, val data: String = "") : VisionEvent {
        override val kind get() = EventKind.CUSTOM
    }
}
