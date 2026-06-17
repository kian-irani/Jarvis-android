package com.kianirani.jarvis.ui.screen.home

/**
 * BUG-2 — the single command-bar action button is multi-state. Its mode is pure
 * logic (no Compose), so the selection rule is unit-tested on the JVM and the UI
 * just renders icon/colour/handler for the chosen [CommandBarMode].
 *
 * Priority: STOP (Vision is speaking — let the user interrupt) ▸ SEND (there is
 * text to send) ▸ MIC (idle or listening → start/stop voice). Each mode maps to a
 * distinct icon AND colour so meaning never rides on colour alone.
 */
enum class CommandBarMode { MIC, SEND, STOP }

fun commandBarMode(isSpeaking: Boolean, hasText: Boolean): CommandBarMode = when {
    isSpeaking -> CommandBarMode.STOP
    hasText -> CommandBarMode.SEND
    else -> CommandBarMode.MIC
}
