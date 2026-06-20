package com.kianirani.jarvis.core.server

/**
 * C2 — Server Control Center (PRD §, "SSH/Docker/K8s/VPS — سرور فرانکفورت رو ری‌استارت کن"). The
 * pure model + safety classifier for remote server actions: parse a natural request into a
 * structured [ServerCommand], and rate its risk so destructive ops (reboot, rm, down) demand
 * confirmation (the SAFE trust gate, server edition). Executing over SSH/Docker API is the
 * network half; this keeps intent + risk deterministic and JVM-tested.
 */
enum class ServerOp { STATUS, RESTART, START, STOP, LOGS, DEPLOY, REMOVE, UNKNOWN }

enum class ServerRisk { SAFE, CONFIRM, CRITICAL }

data class ServerCommand(val op: ServerOp, val target: String, val risk: ServerRisk)

object ServerControl {

    private val DESTRUCTIVE = setOf(ServerOp.REMOVE)
    private val MUTATING = setOf(ServerOp.RESTART, ServerOp.START, ServerOp.STOP, ServerOp.DEPLOY)

    /** Map a natural request to an op (status/restart/stop/deploy/logs/remove). */
    fun opFor(text: String): ServerOp {
        val s = text.lowercase()
        return when {
            "remove" in s || "delete" in s || " rm " in s || "destroy" in s -> ServerOp.REMOVE
            "restart" in s || "reboot" in s || "ری‌استارت" in s || "ریستارت" in s -> ServerOp.RESTART
            "deploy" in s || "release" in s -> ServerOp.DEPLOY
            "stop" in s || "down" in s || "kill" in s || "خاموش" in s -> ServerOp.STOP
            "start" in s || "up" in s || "boot" in s || "روشن" in s -> ServerOp.START
            "log" in s -> ServerOp.LOGS
            "status" in s || "health" in s || "وضعیت" in s -> ServerOp.STATUS
            else -> ServerOp.UNKNOWN
        }
    }

    fun riskOf(op: ServerOp): ServerRisk = when (op) {
        in DESTRUCTIVE -> ServerRisk.CRITICAL
        in MUTATING -> ServerRisk.CONFIRM
        else -> ServerRisk.SAFE
    }

    /** Parse [text] for [target] into a risk-rated command. */
    fun parse(text: String, target: String): ServerCommand {
        val op = opFor(text)
        return ServerCommand(op, target, riskOf(op))
    }

    /** Does this command need explicit user confirmation before running? */
    fun requiresConfirmation(cmd: ServerCommand): Boolean = cmd.risk != ServerRisk.SAFE
}
