package com.kianirani.jarvis.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kianirani.jarvis.core.gesture.GestureAction
import com.kianirani.jarvis.core.gesture.GestureMap
import com.kianirani.jarvis.core.orb.OrbState
import com.kianirani.jarvis.ui.widget.FloatingOrb
import com.kianirani.jarvis.ui.widget.PanelAction

/**
 * DS-W1 — the Floating Widget service (PRD §8.1 / DS-W). A foreground service that hosts the
 * [FloatingOrb] in a `WindowManager` overlay (`TYPE_APPLICATION_OVERLAY`) so Vision floats over
 * every app. Drag repositions it; the user's [GestureMap] maps taps to actions; the mini-panel
 * fires quick actions. Compose lives off-Activity via [OverlayLifecycleOwner].
 *
 * Build-verified (`compileDebugKotlin`); the overlay permission flow and runtime behaviour need
 * on-device confirmation. Start/stop and the permission check go through [FloatingWidget].
 */
class FloatingWidgetService : Service() {

    private val lifecycleOwner = OverlayLifecycleOwner()
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 24
        y = 240
    }

    // Compose state the overlay reads — updates recompose the orb.
    private val orbState = mutableStateOf(OrbState.IDLE)
    private val expanded = mutableStateOf(false)
    private val gestures = GestureMap()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()
        lifecycleOwner.onResume()
        startInForeground()
        addOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun addOverlay() {
        if (overlayView != null || !Settings.canDrawOverlays(this)) return
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                FloatingOrb(
                    state = orbState.value,
                    expanded = expanded.value,
                    gestures = gestures,
                    onGestureAction = ::handleGesture,
                    onDrag = ::moveBy,
                    onPanelAction = ::handlePanel,
                )
            }
        }
        overlayView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun moveBy(dx: Float, dy: Float) {
        params.x += dx.toInt()
        params.y += dy.toInt()
        overlayView?.let { runCatching { windowManager.updateViewLayout(it, params) } }
    }

    private fun handleGesture(action: GestureAction) {
        when (action) {
            GestureAction.EXPAND_PANEL, GestureAction.QUICK_ACTIONS -> expanded.value = !expanded.value
            GestureAction.VOICE, GestureAction.QUICK_PROMPT, GestureAction.OPEN_VISION,
            GestureAction.OPEN_DRAWER, GestureAction.REPEAT_LAST -> launchApp()
            GestureAction.NOTIFICATIONS, GestureAction.LOCK, GestureAction.NONE -> Unit
        }
    }

    private fun handlePanel(action: PanelAction) {
        when (action) {
            PanelAction.CLOSE -> expanded.value = false
            PanelAction.ASK, PanelAction.VOICE, PanelAction.NOTE, PanelAction.OPEN_VISION -> {
                expanded.value = false
                launchApp()
            }
        }
    }

    private fun launchApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { intent?.let { startActivity(it) } }
    }

    private fun startInForeground() {
        val channelId = "vision_widget"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "Vision Widget", NotificationManager.IMPORTANCE_MIN),
                )
            }
        }
        val notification: Notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle("Vision")
            .setContentText("Floating assistant active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    override fun onDestroy() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        lifecycleOwner.onDestroy()
        super.onDestroy()
    }

    private companion object {
        const val NOTIF_ID = 4201
    }
}

/**
 * DS-W1 — start/stop the floating widget and check the overlay permission. The settings screen
 * uses [canDraw] to gate a toggle and [permissionIntent] to send the user to grant it.
 */
object FloatingWidget {

    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Intent that opens the "Display over other apps" permission screen for this app. */
    fun permissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun start(context: Context) {
        if (!canDraw(context)) return
        val intent = Intent(context, FloatingWidgetService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, FloatingWidgetService::class.java))
    }
}
