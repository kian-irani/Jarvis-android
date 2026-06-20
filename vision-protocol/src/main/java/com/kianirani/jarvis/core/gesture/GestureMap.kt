package com.kianirani.jarvis.core.gesture

/**
 * DS-W3 — the floating-widget gesture map (PRD §, "tap→quick prompt · long-press→voice ·
 * swipe-up→پنلِ گسترده …"). A pure binding of [Gesture] to [GestureAction] with sensible
 * [DEFAULTS] that the user can override per gesture. The widget/launcher reads [actionFor] when
 * a gesture is detected. Pure & immutable → JVM-tested; detecting the gestures (drag threshold,
 * collapse-to-dot) and running the actions are the on-device half.
 */
enum class Gesture { TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT, TWO_FINGER, PINCH }

enum class GestureAction {
    QUICK_PROMPT, VOICE, EXPAND_PANEL, QUICK_ACTIONS, REPEAT_LAST,
    OPEN_DRAWER, NOTIFICATIONS, OPEN_VISION, LOCK, NONE,
}

class GestureMap(custom: Map<Gesture, GestureAction> = emptyMap()) {

    // custom overrides default; an explicit NONE binding disables a default gesture.
    private val bindings: Map<Gesture, GestureAction> = DEFAULTS + custom

    /** The action bound to [gesture], or [GestureAction.NONE] if unmapped/disabled. */
    fun actionFor(gesture: Gesture): GestureAction = bindings[gesture] ?: GestureAction.NONE

    /** A new map with [gesture] rebound to [action] (immutable). */
    fun bind(gesture: Gesture, action: GestureAction): GestureMap {
        val custom = bindings.toMutableMap().apply { this[gesture] = action }
        return GestureMap(custom)
    }

    /** All effective bindings (defaults merged with overrides). */
    fun bindings(): Map<Gesture, GestureAction> = bindings

    companion object {
        val DEFAULTS: Map<Gesture, GestureAction> = mapOf(
            Gesture.TAP to GestureAction.QUICK_PROMPT,
            Gesture.LONG_PRESS to GestureAction.VOICE,
            Gesture.SWIPE_UP to GestureAction.EXPAND_PANEL,
            Gesture.SWIPE_LEFT to GestureAction.QUICK_ACTIONS,
            Gesture.SWIPE_RIGHT to GestureAction.QUICK_ACTIONS,
            Gesture.DOUBLE_TAP to GestureAction.REPEAT_LAST,
        )
    }
}
