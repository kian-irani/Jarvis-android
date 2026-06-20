package com.kianirani.jarvis.core.automation

/**
 * CF5 / DS-BG2 — the pure scheduler core (PRD §10). Decides **which time-triggered tasks are
 * due now** and **when each next fires**, with no Android/WorkManager dependency so it is
 * deterministic and JVM-tested. The on-device half (DS-BG) wires these decisions to
 * `WorkManager`/`AlarmManager` and runs each task's action through the `AgentEngine`.
 *
 * The evaluator never touches a real clock: the caller supplies a [Clock] snapshot (epoch
 * "now" plus the start-of-local-day, which encodes the timezone) so day-anchored schedules
 * resolve correctly without pulling in `java.time`/Android calendars.
 */
sealed interface Schedule {
    /** Fire once at an absolute epoch time. */
    data class Once(val atMillis: Long) : Schedule

    /** Fire every [everyMillis], counting from [anchorMillis] (or the last run). */
    data class Interval(val everyMillis: Long, val anchorMillis: Long = 0L) : Schedule

    /** Fire at [minuteOfDay] (0..1439, local) every day. */
    data class DailyAt(val minuteOfDay: Int) : Schedule
}

/** A scheduled unit of work: its [schedule], whether it's [enabled], and when it last ran. */
data class ScheduledTask(
    val id: String,
    val schedule: Schedule,
    val enabled: Boolean = true,
    val lastRunMillis: Long? = null,
)

/** A clock snapshot: epoch [nowMillis] and the epoch of the current local day's 00:00. */
data class Clock(val nowMillis: Long, val dayStartMillis: Long)

object ScheduleEvaluator {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Absolute epoch a [DailyAt] targets today, given the day start. */
    private fun dailyTargetToday(s: Schedule.DailyAt, clock: Clock): Long =
        clock.dayStartMillis + s.minuteOfDay.toLong() * 60_000

    /**
     * The next epoch time [task] should fire at, or null if it never will again (a spent
     * [Schedule.Once]). For a disabled task this still reports the schedule's natural next
     * time; [isDue]/[due] are what gate execution.
     */
    fun nextFire(task: ScheduledTask, clock: Clock): Long? = when (val s = task.schedule) {
        is Schedule.Once ->
            if (task.lastRunMillis != null && task.lastRunMillis >= s.atMillis) null else s.atMillis

        is Schedule.Interval -> {
            val base = task.lastRunMillis ?: s.anchorMillis
            var next = base + s.everyMillis
            if (next <= clock.nowMillis) {
                // jump straight to the first slot strictly after now (no catch-up storm)
                val missed = (clock.nowMillis - next) / s.everyMillis + 1
                next += missed * s.everyMillis
            }
            next
        }

        is Schedule.DailyAt -> {
            val target = dailyTargetToday(s, clock)
            val ranToday = task.lastRunMillis != null && task.lastRunMillis >= target
            if (clock.nowMillis < target && !ranToday) target else target + DAY_MS
        }
    }

    /** True if [task] is enabled and its scheduled time has arrived but it hasn't run yet. */
    fun isDue(task: ScheduledTask, clock: Clock): Boolean {
        if (!task.enabled) return false
        return when (val s = task.schedule) {
            is Schedule.Once ->
                clock.nowMillis >= s.atMillis &&
                    (task.lastRunMillis == null || task.lastRunMillis < s.atMillis)

            is Schedule.Interval -> {
                val base = task.lastRunMillis ?: s.anchorMillis
                clock.nowMillis >= base + s.everyMillis
            }

            is Schedule.DailyAt -> {
                val target = dailyTargetToday(s, clock)
                clock.nowMillis >= target && (task.lastRunMillis == null || task.lastRunMillis < target)
            }
        }
    }

    /** All due tasks, in input order — the set the runner should fire this tick. */
    fun due(tasks: List<ScheduledTask>, clock: Clock): List<ScheduledTask> =
        tasks.filter { isDue(it, clock) }
}
