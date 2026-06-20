package com.kianirani.jarvis.service

import com.kianirani.jarvis.core.power.PowerMode
import com.kianirani.jarvis.core.power.PowerPolicy
import com.kianirani.jarvis.core.power.PowerState

/**
 * DS-BG4 — resource/power optimizer. The pure decision of which background surfaces are allowed
 * to run for a given [PowerState], built on the [PowerPolicy] core (DS-W6). Consulted by
 * [VisionServiceManager] (DS-BG1) so power-saving is decided in one place: under MINIMAL power
 * the wake word (always-on mic) is dropped; the draw-only widget survives until critical.
 */
object PowerOptimizer {

    /** Allow the always-listening wake word unless we're in MINIMAL power. */
    fun allowWakeWord(state: PowerState): Boolean = PowerPolicy.wakeWordEnabled(state)

    /** Keep the floating widget unless power is critical (MINIMAL) AND unplugged. */
    fun allowWidget(state: PowerState): Boolean =
        PowerPolicy.mode(state) != PowerMode.MINIMAL || state.charging

    /** Whether background work should run reduced (no heavy polling) — true off FULL power. */
    fun shouldThrottleBackground(state: PowerState): Boolean = PowerPolicy.mode(state) != PowerMode.FULL
}
