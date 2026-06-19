package com.kianirani.jarvis.core.orb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ORB acceptance (PRD §7): the signal → state mapping is pure and prioritises a live turn
 * over passive cues, with a distinct label per state. No device.
 */
class OrbStateMachineTest {

    @Test fun `no signals is IDLE`() {
        assertEquals(OrbState.IDLE, OrbStateMachine.resolve(OrbSignals()))
    }

    @Test fun `executing outranks every other signal`() {
        val s = OrbSignals(listening = true, speaking = true, thinking = true, executing = true, error = true)
        assertEquals(OrbState.EXECUTING, OrbStateMachine.resolve(s))
    }

    @Test fun `listening outranks speaking and thinking`() {
        assertEquals(
            OrbState.LISTENING,
            OrbStateMachine.resolve(OrbSignals(listening = true, speaking = true, thinking = true)),
        )
    }

    @Test fun `speaking outranks thinking`() {
        assertEquals(OrbState.SPEAKING, OrbStateMachine.resolve(OrbSignals(speaking = true, thinking = true)))
    }

    @Test fun `thinking shows while a request is in flight`() {
        assertEquals(OrbState.THINKING, OrbStateMachine.resolve(OrbSignals(thinking = true)))
    }

    @Test fun `error surfaces only when idle, never hiding a live turn`() {
        assertEquals(OrbState.ERROR, OrbStateMachine.resolve(OrbSignals(error = true)))
        // a live turn hides the error
        assertEquals(OrbState.THINKING, OrbStateMachine.resolve(OrbSignals(thinking = true, error = true)))
    }

    @Test fun `notification shows when otherwise idle but yields to error`() {
        assertEquals(OrbState.NOTIFICATION, OrbStateMachine.resolve(OrbSignals(hasNotification = true)))
        assertEquals(OrbState.ERROR, OrbStateMachine.resolve(OrbSignals(hasNotification = true, error = true)))
    }

    @Test fun `long idle sleeps, recent idle does not`() {
        assertEquals(OrbState.SLEEPING, OrbStateMachine.resolve(OrbSignals(idleMillis = OrbStateMachine.sleepAfterMillis)))
        assertEquals(OrbState.IDLE, OrbStateMachine.resolve(OrbSignals(idleMillis = 1_000L)))
        // a notification wins over sleep
        assertEquals(
            OrbState.NOTIFICATION,
            OrbStateMachine.resolve(OrbSignals(hasNotification = true, idleMillis = OrbStateMachine.sleepAfterMillis)),
        )
    }

    @Test fun `every state has a non-blank label`() {
        OrbState.entries.forEach { state ->
            assertTrue(state.name, OrbStateMachine.label(state).isNotBlank())
        }
    }
}
