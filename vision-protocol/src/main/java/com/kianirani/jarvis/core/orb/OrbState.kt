package com.kianirani.jarvis.core.orb

/**
 * ORB State Machine (PRD §7) — the orb's mood, derived purely from runtime signals.
 *
 * Replaces the home orb's single `listening: Boolean` with an explicit state so each
 * phase of a turn reads distinctly (and animation/colour can key off it). The mapping is
 * **pure** ([OrbSignals] → [OrbState]) so it's unit-tested on the JVM; the `VisionOrb`
 * composable just renders the per-state look (breathing / wave / pulse / glow), honouring
 * reduced-motion. Meaning never rides on colour alone — each state also drives a label.
 */
enum class OrbState {
    /** Nothing happening — gentle breathing. */
    IDLE,

    /** Capturing speech — listening wave. */
    LISTENING,

    /** A request is in flight to the model/agent — thinking pulse. */
    THINKING,

    /** Speaking a reply aloud — responding glow. */
    SPEAKING,

    /** Running a tool/device action — executing spinner. */
    EXECUTING,

    /** A pending notification wants attention. */
    NOTIFICATION,

    /** The last turn failed — error state. */
    ERROR,

    /** Long-idle / power-save — dimmed, minimal motion. */
    SLEEPING,
}

/**
 * The runtime signals that decide the orb's state. Hard activity (listening/speaking/
 * executing) wins over passive cues (notification/sleep); [error] surfaces only when the
 * orb is otherwise idle so it never hides a live turn.
 */
data class OrbSignals(
    val listening: Boolean = false,
    val speaking: Boolean = false,
    val thinking: Boolean = false,
    val executing: Boolean = false,
    val hasNotification: Boolean = false,
    val error: Boolean = false,
    /** Millis since the last user interaction; past [sleepAfterMillis] → SLEEPING when idle. */
    val idleMillis: Long = 0L,
)

object OrbStateMachine {

    const val sleepAfterMillis: Long = 5 * 60 * 1000L // 5 minutes

    /**
     * Resolve the orb state. Priority (highest first): a live turn always wins —
     * EXECUTING ▸ LISTENING ▸ SPEAKING ▸ THINKING — then ERROR, then NOTIFICATION,
     * then SLEEPING (long-idle), else IDLE.
     */
    fun resolve(s: OrbSignals): OrbState = when {
        s.executing -> OrbState.EXECUTING
        s.listening -> OrbState.LISTENING
        s.speaking -> OrbState.SPEAKING
        s.thinking -> OrbState.THINKING
        s.error -> OrbState.ERROR
        s.hasNotification -> OrbState.NOTIFICATION
        s.idleMillis >= sleepAfterMillis -> OrbState.SLEEPING
        else -> OrbState.IDLE
    }

    /** Short uppercase label for the state (drives the wordmark; never colour-only meaning). */
    fun label(state: OrbState): String = when (state) {
        OrbState.IDLE -> "AI CORE ONLINE"
        OrbState.LISTENING -> "LISTENING…"
        OrbState.THINKING -> "THINKING…"
        OrbState.SPEAKING -> "SPEAKING…"
        OrbState.EXECUTING -> "WORKING…"
        OrbState.NOTIFICATION -> "NEW ACTIVITY"
        OrbState.ERROR -> "TAP TO RETRY"
        OrbState.SLEEPING -> "SLEEPING"
    }
}
