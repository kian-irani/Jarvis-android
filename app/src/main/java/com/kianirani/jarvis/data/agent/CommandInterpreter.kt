package com.kianirani.jarvis.data.agent

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P5 (Agentic Core) + P11 (Conversational OS) v1 — local tool layer.
 * Recognizes device commands and executes them instantly on-device, no brain
 * or cloud round-trip. Anything unrecognized returns null and flows to AI.
 */
@Singleton
class CommandInterpreter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val plugins: com.kianirani.jarvis.data.plugin.PluginRegistry,
) {

    /** Returns a reply if the message was a device command, else null. */
    fun tryHandle(message: String): String? {
        plugins.dispatch(message)?.let { return it }
        val m = message.trim().lowercase()
        openAppTarget(m)?.let { return launchApp(it) }
        if (m in setOf("time", "what time is it", "the time", "ساعت", "ساعت چنده", "ساعت چند است", "الان ساعت چنده")) {
            return "It is ${SimpleDateFormat("HH:mm", Locale.US).format(Date())}."
        }
        if (m in setOf("date", "today", "what day is it", "what is the date", "تاریخ", "امروز چندمه", "امروز چه روزیه", "تاریخ امروز")) {
            return "Today is ${SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())}."
        }
        if (m in setOf("battery", "battery status", "battery level", "باتری", "شارژ", "شارژ چقدره", "شارژ گوشی", "باتری چقدره")) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val charging = bm.isCharging
            return "Battery at $pct%${if (charging) " (charging)" else ""}."
        }
        // Offline arithmetic ("12 * 7", "100 / 4") — answered with no network.
        if (m.matches(Regex("""\d+(\.\d+)?\s*[+\-*/x×÷]\s*\d+(\.\d+)?"""))) {
            calculate(m)?.let { return it }
        }
        return null
    }

    /** Single binary operation on two numbers; null if it cannot be parsed. */
    private fun calculate(expr: String): String? = runCatching {
        val cleaned = expr.replace(" ", "").replace('x', '*').replace('×', '*').replace('÷', '/')
        val op = cleaned.firstOrNull { it in "+-*/" } ?: return null
        val parts = cleaned.split(op)
        if (parts.size != 2) return null
        val a = parts[0].toDouble(); val b = parts[1].toDouble()
        val r = when (op) {
            '+' -> a + b; '-' -> a - b; '*' -> a * b
            '/' -> if (b == 0.0) return "Cannot divide by zero." else a / b
            else -> return null
        }
        "= " + if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
    }.getOrNull()

    private fun openAppTarget(m: String): String? {
        for (prefix in listOf("open ", "launch ", "باز کن ", "اجرا کن ")) {
            if (m.startsWith(prefix)) return m.removePrefix(prefix).trim()
        }
        if (m.endsWith(" رو باز کن")) return m.removeSuffix(" رو باز کن").trim()
        if (m.endsWith(" را باز کن")) return m.removeSuffix(" را باز کن").trim()
        return null
    }

    private fun launchApp(name: String): String {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(intent, 0)
            .firstOrNull { it.loadLabel(pm).toString().contains(name, ignoreCase = true) }
            ?: return "No app matching \"$name\"."
        val pkg = match.activityInfo.packageName
        pm.getLaunchIntentForPackage(pkg)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
            return "Opening ${match.loadLabel(pm)}."
        }
        return "Cannot launch \"$name\"."
    }
}
