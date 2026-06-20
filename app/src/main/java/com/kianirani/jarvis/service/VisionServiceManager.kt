package com.kianirani.jarvis.service

import android.content.Context
import com.kianirani.jarvis.core.power.PowerState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DS-BG1 — the foreground-service manager. One place to start/stop and reason about Vision's
 * background surfaces (the floating widget, the wake-word listener; Brain-Lite owns its own
 * lifecycle). Centralizing this keeps service start/stop consistent and lets [PowerOptimizer]
 * (DS-BG4) gate which ones run as battery/memory change, instead of each caller deciding.
 */
@Singleton
class VisionServiceManager @Inject constructor(@ApplicationContext private val context: Context) {

    /** Start the floating widget overlay (no-op without the overlay permission). */
    fun startWidget() = FloatingWidget.start(context)
    fun stopWidget() = FloatingWidget.stop(context)
    fun canShowWidget(): Boolean = FloatingWidget.canDraw(context)

    /** Start/stop the always-listening wake word. */
    fun startWakeWord() = WakeWord.start(context)
    fun stopWakeWord() = WakeWord.stop(context)

    /**
     * DS-BG4 — apply a power-aware decision: keep the requested services running only while the
     * [PowerOptimizer] allows them for [state]. Wake-word is the first thing dropped under low
     * power (it's the most expensive ambient cost); the widget stays (it's cheap, draw-only).
     */
    fun applyPowerState(state: PowerState, widgetWanted: Boolean, wakeWordWanted: Boolean) {
        if (widgetWanted && PowerOptimizer.allowWidget(state)) startWidget() else stopWidget()
        if (wakeWordWanted && PowerOptimizer.allowWakeWord(state)) startWakeWord() else stopWakeWord()
    }
}
