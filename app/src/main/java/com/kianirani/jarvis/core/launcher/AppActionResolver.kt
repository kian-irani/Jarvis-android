package com.kianirani.jarvis.core.launcher

/**
 * C5 — AI App Launcher (PRD §, "به‌جای آیکن‌ها، «با علی تماس بگیر» → Vision بهترین اپ را انتخاب
 * و اجرا کند"). Maps a natural request to the **action category** (call/message/navigate/browse/
 * camera/email/music…) so the device layer fires the right `Intent` (or picks the user's default
 * app for that category). Pure intent classification → JVM-tested; resolving the concrete app +
 * launching is the device half.
 */
enum class AppAction { CALL, MESSAGE, EMAIL, NAVIGATE, BROWSE, CAMERA, MUSIC, ALARM, CALENDAR, SEARCH, OPEN_APP, UNKNOWN }

object AppActionResolver {

    /** Classify [request] into an [AppAction] (EN + FA keywords). */
    fun classify(request: String): AppAction {
        val s = request.lowercase()
        return when {
            hasAny(s, "call", "phone", "dial", "تماس", "زنگ") -> AppAction.CALL
            hasAny(s, "message", "text", "sms", "whatsapp", "telegram", "پیام", "اس ام اس") -> AppAction.MESSAGE
            hasAny(s, "email", "mail", "ایمیل") -> AppAction.EMAIL
            hasAny(s, "navigate", "directions", "drive to", "route", "مسیر", "ناوبری") -> AppAction.NAVIGATE
            hasAny(s, "play ", "music", "song", "spotify", "آهنگ", "موسیقی") -> AppAction.MUSIC
            hasAny(s, "photo", "camera", "selfie", "دوربین", "عکس") -> AppAction.CAMERA
            hasAny(s, "alarm", "wake me", "timer", "آلارم", "زنگ ساعت") -> AppAction.ALARM
            hasAny(s, "calendar", "schedule", "meeting", "تقویم", "جلسه") -> AppAction.CALENDAR
            hasAny(s, "browse", "website", "google", "search the web", "مرورگر") -> AppAction.BROWSE
            hasAny(s, "search", "find", "جستجو", "پیدا") -> AppAction.SEARCH
            hasAny(s, "open ", "launch ", "باز کن", "اجرا کن") -> AppAction.OPEN_APP
            else -> AppAction.UNKNOWN
        }
    }

    /** The free-text target of the action (the bit after the verb), best-effort. */
    fun target(request: String): String {
        val verbs = Regex("""(?i)\b(call|message|text|email|navigate to|play|open|launch|search for|find)\b\s*""")
        return request.replace(verbs, "").trim().trim('.', '!', '?')
    }

    private fun hasAny(s: String, vararg keys: String) = keys.any { s.contains(it) }
}
