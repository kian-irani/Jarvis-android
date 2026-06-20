package com.kianirani.jarvis.service

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException

/**
 * DS-C4 — cross-process IPC seam (the separate-app option). A bound [Messenger] service so a
 * Vision **widget hosted in another process/app** can hand a command to the main Vision process
 * and get a reply, without sharing memory. Uses Android's [Messenger] (serialized [Message]s on
 * one handler thread) — simpler and safer than raw AIDL for this request/reply shape.
 *
 * Protocol: send [MSG_COMMAND] with `data.getString(KEY_TEXT)` and a `replyTo` Messenger; the
 * service answers with [MSG_REPLY] carrying `KEY_TEXT`. The actual command handling routes
 * through the gateway/agent as this is wired to `VisionBrain` (DS-C1).
 */
class VisionIpcService : Service() {

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_COMMAND -> respond(msg)
                else -> super.handleMessage(msg)
            }
        }
    }

    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    private fun respond(msg: Message) {
        val reply = msg.replyTo ?: return
        val text = msg.data?.getString(KEY_TEXT).orEmpty()
        // Echo-ack for now; the gateway/agent result is filled in once VisionBrain is bound here
        // (DS-C1). Kept crash-safe — a dead client just drops the reply.
        val out = Message.obtain(null, MSG_REPLY).apply {
            data = Bundle().apply { putString(KEY_TEXT, if (text.isBlank()) "" else "ack: $text") }
        }
        runCatching { reply.send(out) }.recoverCatching { if (it is RemoteException) Unit else throw it }
    }

    companion object {
        const val MSG_COMMAND = 1
        const val MSG_REPLY = 2
        const val KEY_TEXT = "text"
    }
}
