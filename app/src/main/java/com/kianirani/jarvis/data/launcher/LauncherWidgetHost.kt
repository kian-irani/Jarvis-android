package com.kianirani.jarvis.data.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LR8 — the launcher's widget host. Wraps Android's [AppWidgetHost]/[AppWidgetManager] so the
 * workspace can embed real home-screen widgets: list available providers, allocate an id, bind
 * it (or hand back the bind/configure intent the user must confirm), and inflate the live
 * [AppWidgetHostView] onto the grid. The host must be listening while the launcher is visible.
 *
 * Real Android plumbing (compile-verified); placing the inflated view on the grid + persisting
 * the widget id onto [LauncherItem] (already modelled: `widgetId`/`widgetProvider`) is the UI
 * half done by the workspace.
 */
@Singleton
class LauncherWidgetHost @Inject constructor(@ApplicationContext private val context: Context) {

    private val host = AppWidgetHost(context, HOST_ID)
    private val manager = AppWidgetManager.getInstance(context)

    /** Start receiving widget updates — call when the launcher becomes visible. */
    fun startListening() = runCatching { host.startListening() }

    /** Stop receiving updates — call when the launcher is hidden. */
    fun stopListening() = runCatching { host.stopListening() }

    /** All installed widget providers the user can add. */
    fun availableProviders(): List<AppWidgetProviderInfo> =
        runCatching { manager.installedProviders }.getOrDefault(emptyList())

    /** Reserve a fresh widget id (release it with [deleteWidget] if the user cancels). */
    fun allocateWidgetId(): Int = host.allocateAppWidgetId()

    /**
     * Try to bind [widgetId] to [provider] silently. Returns true if the system allowed it; if
     * false, the caller must launch [bindPermissionIntent] so the user grants the bind.
     */
    fun bindWidget(widgetId: Int, provider: ComponentName): Boolean =
        runCatching { manager.bindAppWidgetIdIfAllowed(widgetId, provider) }.getOrDefault(false)

    /** Intent to ask the user to allow binding [widgetId] to [provider]. */
    fun bindPermissionIntent(widgetId: Int, provider: ComponentName): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /** The configure intent if the provider needs setup, else null. */
    fun configureIntent(widgetId: Int): Intent? {
        val info = manager.getAppWidgetInfo(widgetId) ?: return null
        val configure = info.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Inflate the live widget view for [widgetId], or null on failure. */
    fun createView(widgetId: Int): AppWidgetHostView? = runCatching {
        val info = manager.getAppWidgetInfo(widgetId) ?: return null
        host.createView(context, widgetId, info)
    }.getOrNull()

    /** Release a widget id (user removed the widget or cancelled binding). */
    fun deleteWidget(widgetId: Int) = runCatching { host.deleteAppWidgetId(widgetId) }

    private companion object {
        const val HOST_ID = 0x5701
    }
}
