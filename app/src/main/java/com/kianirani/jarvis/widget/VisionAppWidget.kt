package com.kianirani.jarvis.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kianirani.jarvis.R

/**
 * Widget API (PRD §, "Widget API — ویجت‌های خانه"). Vision's own home-screen app widget so the
 * user can summon the assistant from *any* launcher (not just Vision's). A glass pill with the
 * VISION wordmark + a "Search / Ask" tap that opens the app. Real `AppWidgetProvider` — registered
 * in the manifest with `vision_appwidget_info`.
 */
class VisionAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        widgetIds.forEach { id -> manager.updateAppWidget(id, build(context)) }
    }

    private fun build(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_vision)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: Intent()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getActivity(context, 0, launch, flags)
        views.setOnClickPendingIntent(R.id.widget_root, pi)
        return views
    }

    companion object {
        /** Push a refresh to every placed Vision widget (e.g. theme/state changed). */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VisionAppWidget::class.java))
            if (ids.isNotEmpty()) VisionAppWidget().onUpdate(context, manager, ids)
        }
    }
}
