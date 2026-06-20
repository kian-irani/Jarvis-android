package com.kianirani.jarvis.data.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kianirani.jarvis.core.automation.Clock
import com.kianirani.jarvis.core.automation.ScheduleEvaluator
import com.kianirani.jarvis.core.automation.ScheduledTask
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CF5 / DS-BG2 — wires the pure scheduler core ([ScheduleEvaluator]) to Android. Computes each
 * task's next fire time and arms an [AlarmManager] alarm for the soonest one; when it fires,
 * [AutomationReceiver] asks the core which tasks are due and runs them, then re-arms the next
 * alarm. AlarmManager (not WorkManager) keeps the dependency footprint zero and survives Doze
 * via `setExactAndAllowWhileIdle`. The pure decision (which/when) stays in core and is tested;
 * this is the device plumbing.
 */
@Singleton
class AutomationScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    /** Build a clock snapshot (now + start-of-local-day) the core needs for DailyAt math. */
    fun clock(): Clock {
        val now = System.currentTimeMillis()
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return Clock(now, dayStart)
    }

    /** Tasks due right now per the core (the receiver runs these). */
    fun due(tasks: List<ScheduledTask>): List<ScheduledTask> = ScheduleEvaluator.due(tasks, clock())

    /** Arm an alarm for the soonest upcoming task; cancels if there's nothing to schedule. */
    fun schedule(tasks: List<ScheduledTask>) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val clock = clock()
        val next = tasks.filter { it.enabled }
            .mapNotNull { ScheduleEvaluator.nextFire(it, clock) }
            .filter { it > clock.nowMillis }
            .minOrNull()
        val pi = pendingIntent()
        if (next == null) {
            am.cancel(pi)
            return
        }
        runCatching {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        }.onFailure {
            // Falls back to an inexact alarm if exact alarms aren't permitted (Android 12+).
            am.set(AlarmManager.RTC_WAKEUP, next, pi)
        }
    }

    fun cancel() {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, AutomationReceiver::class.java).setAction(ACTION_FIRE)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private companion object {
        const val ACTION_FIRE = "com.kianirani.jarvis.AUTOMATION_FIRE"
        const val REQUEST_CODE = 7701
    }
}

/**
 * CF5 — fired by the [AutomationScheduler]'s alarm. A Hilt receiver so it can reach the agent
 * later; for now it re-arms the next alarm (the task store/runner is wired as automations gain a
 * persisted source). Kept tiny and crash-safe.
 */
@AndroidEntryPoint
class AutomationReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: AutomationScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        // With a persisted task store this loads tasks, runs `scheduler.due(...)` through the
        // AgentEngine, marks them run, and re-arms. Until then it simply re-arms an empty set
        // (no-op) so the alarm chain never throws.
        runCatching { scheduler.schedule(emptyList()) }
    }
}
