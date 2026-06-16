package com.kianirani.jarvis.ui.screen.workspace

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.data.launcher.AppRef
import com.kianirani.jarvis.data.launcher.LauncherStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** The icon + label resolved for one installed package, used to paint a cell. */
data class AppVisual(val label: String, val icon: ImageBitmap)

/**
 * LR2 — view-model for the real launcher workspace. Binds the persisted
 * [LauncherStore] layout to the UI and resolves the live icon/label for every
 * installed package so the [WorkspaceScreen] can render the pinned apps the model
 * holds. On first run (empty store) it seeds a sensible default layout from the
 * installed apps. Launching reuses the shared `vision_app_usage` counters so the
 * App Drawer's Frequent/Recent rows keep learning from home taps too.
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val store: LauncherStore,
) : ViewModel() {

    val layout = store.layout

    private val usage = context.getSharedPreferences("vision_app_usage", Context.MODE_PRIVATE)
    private val _visuals = MutableStateFlow<Map<String, AppVisual>>(emptyMap())

    /** packageName → resolved icon + label for painting cells. */
    val visuals = _visuals.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { loadApps() }
            _visuals.value = apps.associate { it.packageName to AppVisual(it.label, it.icon) }
            // First-run seed: fill the dock + page-0 grid from installed apps.
            if (store.isEmpty) {
                store.seedDefault(apps.map { AppRef(it.packageName, it.className, it.label) })
            }
        }
    }

    fun visualFor(packageName: String?): AppVisual? = packageName?.let { _visuals.value[it] }

    /** Launch an installed app and count the tap toward the usage model. */
    fun launch(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
            usage.edit().putInt(packageName, usage.getInt(packageName, 0) + 1).apply()
        }
    }

    private data class LoadedApp(val packageName: String, val className: String?, val label: String, val icon: ImageBitmap)

    private fun loadApps(): List<LoadedApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_ALL.toLong()))
        } else {
            pm.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_ALL)
        }
        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                LoadedApp(
                    packageName = it.activityInfo.packageName,
                    className = it.activityInfo.name,
                    label = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
