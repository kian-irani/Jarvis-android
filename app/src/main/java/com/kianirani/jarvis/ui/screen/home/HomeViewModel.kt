package com.kianirani.jarvis.ui.screen.home

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.data.TaskRepository
import com.kianirani.jarvis.data.agent.AgentRegistry
import com.kianirani.jarvis.data.settings.QuickActionsStore
import com.kianirani.jarvis.data.settings.VisionSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Live home stats fed into the stats row + widgets card. */
data class HomeStats(
    val tasks: Int = 0,
    val recentTask: String? = null,
    val memories: Int = 0,
    val battery: Int = -1,
    val freeRamMb: Int = -1,
)

/** A real installed app pinned on Home (RD2.b) — launched via its system intent. */
data class FavoriteApp(val label: String, val packageName: String, val icon: ImageBitmap)

/**
 * Home dashboard data (v12). Owns the Agents registry, quick-action order,
 * persona name (greeting) and derived device/brain stats. Command bar, clock,
 * voice and chat stay in [com.kianirani.jarvis.ui.screen.hud.HudViewModel] — the
 * home view reuses that VM rather than duplicating its logic.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val agents: AgentRegistry,
    val quickActions: QuickActionsStore,
    private val tasks: TaskRepository,
    private val memory: MemoryRepository,
    private val settings: VisionSettings,
) : ViewModel() {

    val agentStates = agents.states
    val personaName: StateFlow<String> = settings.personaName

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    private val _favorites = MutableStateFlow<List<FavoriteApp>>(emptyList())
    val favorites: StateFlow<List<FavoriteApp>> = _favorites.asStateFlow()

    // Shared with the app drawer so Home favourites learn from every launch.
    private val usage = context.getSharedPreferences("vision_app_usage", Context.MODE_PRIVATE)

    fun refresh() {
        agents.refresh()
        viewModelScope.launch { _stats.value = compute() }
        viewModelScope.launch { _favorites.value = withContext(Dispatchers.IO) { loadFavorites() } }
    }

    /** Launch a Home favourite by package and count it toward future ranking. */
    fun launchFavorite(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
            usage.edit().putInt(packageName, usage.getInt(packageName, 0) + 1).apply()
        }
    }

    /**
     * The Home favourites row (RD2.b): the user's most-launched real apps, or —
     * before any usage is recorded — the system defaults for phone, messaging,
     * browser and camera so the essentials are on Home from first boot.
     */
    private fun loadFavorites(): List<FavoriteApp> {
        val pm = context.packageManager
        val frequent = usage.all.entries
            .mapNotNull { (pkg, c) -> (c as? Int)?.takeIf { it > 0 }?.let { pkg to it } }
            .sortedByDescending { it.second }
            .mapNotNull { (pkg, _) -> favorite(pkg) }
            .take(4)
        if (frequent.isNotEmpty()) return frequent

        // First-run defaults — resolve the system default for each role.
        return listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
            Intent("android.media.action.STILL_IMAGE_CAMERA"),
        ).mapNotNull { intent ->
            runCatching { pm.resolveActivity(intent, 0)?.activityInfo?.packageName }.getOrNull()
                ?.takeIf { it != context.packageName }
                ?.let { favorite(it) }
        }.distinctBy { it.packageName }
    }

    /** Build a [FavoriteApp] for a launchable package, or null if it can't launch. */
    private fun favorite(pkg: String): FavoriteApp? {
        val pm = context.packageManager
        if (pm.getLaunchIntentForPackage(pkg) == null) return null
        return runCatching {
            val ai = pm.getApplicationInfo(pkg, 0)
            FavoriteApp(
                label = pm.getApplicationLabel(ai).toString(),
                packageName = pkg,
                icon = pm.getApplicationIcon(ai).toBitmap(96, 96).asImageBitmap(),
            )
        }.getOrNull()
    }

    private suspend fun compute(): HomeStats = withContext(Dispatchers.IO) {
        val taskCount = runCatching { tasks.pendingCount() }.getOrDefault(0)
        val recent = runCatching { tasks.nextPending()?.kind }.getOrNull()
        val mem = runCatching { memory.count() }.getOrDefault(0)
        val battery = runCatching {
            (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }.getOrDefault(-1)
        val freeRam = runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            (mi.availMem / (1024 * 1024)).toInt()
        }.getOrDefault(-1)
        HomeStats(taskCount, recent, mem, battery, freeRam)
    }
}
