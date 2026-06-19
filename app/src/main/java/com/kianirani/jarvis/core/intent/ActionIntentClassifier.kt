package com.kianirani.jarvis.core.intent

import com.kianirani.jarvis.core.protocol.Intent

/**
 * DS-B1 — a pure, on-device **action** intent classifier (heuristic; no model, no network).
 *
 * Maps free user text (EN + FA) to a protocol [Intent] — the *verb* the user wants
 * (`open_app`, `call`, `message`, `search`, `set_reminder`, `weather`, `time`) plus an
 * extracted slot — else the `chat` fallback. This is **distinct** from the routing-level
 * [com.kianirani.jarvis.router.orchestrator.IntentClassifier], which picks a *model
 * capability* (CODE/REASONING/QUICK…): this one picks a *user action* and is the producer
 * for `VisionRequest.intent` (DS-F2) / the launcher / the agent. A wrong guess is
 * non-fatal — the agent still reasons over the raw text; the slot is a hint, not a parse.
 */
object ActionIntentClassifier {

    private data class Rule(val name: String, val slot: String?, val confidence: Float, val keywords: List<String>)

    // Order matters: specific verbs (call/message/reminder/search) before the generic "open".
    private val rules = listOf(
        Rule("call", "target", 0.9f, listOf("call ", "phone ", "ring ", "dial ", "زنگ بزن", "تماس بگیر", "تماس با", "زنگ به")),
        Rule(
            "message", "target", 0.9f,
            listOf("send a message", "send message", "send sms", "text ", "message ", "sms ", "پیام بده", "پیام بفرست", "پیام به", "اس ام اس", "اس‌ام‌اس", "پیامک"),
        ),
        Rule("set_reminder", "task", 0.9f, listOf("remind me", "set a reminder", "reminder to", "remind ", "یادآوری کن", "یادم بنداز", "یاد آوری", "ریمایندر")),
        Rule(
            "search", "query", 0.9f,
            listOf("search for", "search ", "google ", "look up", "web search", "جستجو کن", "جست و جو", "جست‌وجو", "سرچ کن", "گوگل کن", "بگرد"),
        ),
        Rule("weather", null, 0.85f, listOf("weather", "forecast", "آب و هوا", "آب‌وهوا", "هواشناسی", "هوا چطوره", "هوا چطور")),
        Rule("time", null, 0.85f, listOf("what time", "what's the time", "current time", "time now", "ساعت چنده", "ساعت چند", "چه ساعتی")),
        Rule("open_app", "app", 0.8f, listOf("open ", "launch ", "باز کن", "اجرا کن")),
    )

    private val fillers = listOf("to ", "the ", "a ", "for ", "on ", "که ", "به ")

    /** Classify [text]; never throws, always returns an [Intent] (worst case `chat`). */
    fun classify(text: String): Intent {
        val raw = text.trim()
        if (raw.isEmpty()) return Intent("chat", 0.3f)
        val lower = raw.lowercase()
        for (rule in rules) {
            val kw = rule.keywords.firstOrNull { lower.contains(it) } ?: continue
            val slots = rule.slot?.let { slotName ->
                val value = extractSlot(raw, lower, kw)
                if (value.isNotEmpty()) mapOf(slotName to value) else emptyMap()
            } ?: emptyMap()
            return Intent(rule.name, rule.confidence, slots)
        }
        return Intent("chat", 0.3f)
    }

    /** The text after the matched keyword becomes the slot value (original case), lightly cleaned. */
    private fun extractSlot(raw: String, lower: String, keyword: String): String {
        val idx = lower.indexOf(keyword)
        if (idx < 0) return ""
        var rest = raw.substring(idx + keyword.length).trim()
        for (f in fillers) {
            if (rest.lowercase().startsWith(f)) {
                rest = rest.substring(f.length).trim()
                break
            }
        }
        return rest.trimEnd('.', '!', '?', '،', ',').trim()
    }
}
