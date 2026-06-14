package com.kianirani.jarvis

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kianirani.jarvis.ui.screen.agents.AgentsScreen
import com.kianirani.jarvis.ui.screen.drawer.AppDrawerScreen
import com.kianirani.jarvis.ui.screen.election.BrainElectionScreen
import com.kianirani.jarvis.ui.screen.home.HomeScreen
import com.kianirani.jarvis.ui.screen.home.HomeViewModel
import com.kianirani.jarvis.ui.screen.hub.VisionHubScreen
import com.kianirani.jarvis.ui.screen.hud.HudViewModel
import com.kianirani.jarvis.ui.screen.memory.MemoryScreen
import com.kianirani.jarvis.ui.screen.onboarding.OnboardingScreen
import com.kianirani.jarvis.ui.screen.recents.RecentsScreen
import com.kianirani.jarvis.ui.screen.settings.AiTokensScreen
import com.kianirani.jarvis.ui.screen.settings.SettingsHubScreen
import com.kianirani.jarvis.ui.screen.setup.SetupWizardScreen
import com.kianirani.jarvis.data.settings.QuickAction
import com.kianirani.jarvis.ui.theme.JarvisTheme
import com.kianirani.jarvis.ui.theme.VisionColors
import dagger.hilt.android.AndroidEntryPoint

private const val PREFS = "vision_prefs"
private const val KEY_SETUP_COMPLETE = "setup_complete"
private const val KEY_ONBOARDED = "persona_onboarded"

enum class VisionRoute { ONBOARDING, SETUP, HOME, AGENTS, APPS, MEMORY, SETTINGS, ELECTION, AI_SETTINGS, RECENTS, HUB }

/** The five tabs that appear in the bottom navigation / nav rail. */
private val TOP_LEVEL = setOf(VisionRoute.HOME, VisionRoute.AGENTS, VisionRoute.APPS, VisionRoute.MEMORY, VisionRoute.SETTINGS)
private data class NavItem(val route: VisionRoute, val glyph: String, val label: String)
private val NAV_ITEMS = listOf(
    NavItem(VisionRoute.HOME, "⌂", "Home"),
    NavItem(VisionRoute.AGENTS, "🤖", "Agents"),
    NavItem(VisionRoute.APPS, "▦", "Apps"),
    NavItem(VisionRoute.MEMORY, "◈", "Memory"),
    NavItem(VisionRoute.SETTINGS, "⚙", "Settings"),
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* features degrade per-permission if denied */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val wanted = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (wanted.isNotEmpty()) permissions.launch(wanted.toTypedArray())
        androidx.core.content.ContextCompat.startForegroundService(
            this, Intent(this, com.kianirani.jarvis.brain.BrainLiteService::class.java),
        )
        // Load persisted appearance before first composition.
        com.kianirani.jarvis.ui.theme.ThemeStore.init(this)
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        setContent {
            JarvisTheme {
                val widthClass = calculateWindowSizeClass(this).widthSizeClass
                val compact = widthClass == WindowWidthSizeClass.Compact
                var route by rememberSaveable {
                    mutableStateOf(
                        if (!prefs.getBoolean(KEY_ONBOARDED, false)) VisionRoute.ONBOARDING else VisionRoute.HOME,
                    )
                }
                // Back returns to HOME from any non-root screen.
                BackHandler(enabled = route != VisionRoute.HOME && route != VisionRoute.SETUP && route != VisionRoute.ONBOARDING) {
                    route = VisionRoute.HOME
                }
                when (route) {
                    VisionRoute.ONBOARDING -> OnboardingScreen(onFinished = {
                        prefs.edit().putBoolean(KEY_ONBOARDED, true).putBoolean(KEY_SETUP_COMPLETE, true).apply()
                        route = VisionRoute.HOME
                    })
                    VisionRoute.SETUP -> SetupWizardScreen(onFinished = {
                        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
                        route = VisionRoute.HOME
                    })
                    else -> VisionShell(route, compact, onNavigate = { route = it })
                }
            }
        }
    }
}

/**
 * Shell for all in-app routes: wraps the five top-level destinations in a bottom
 * NavigationBar (compact phones) or a NavigationRail (tablet/desktop). Secondary
 * screens (Election, AI tokens, Recents, Hub) render full-screen without the bar.
 */
@Composable
private fun VisionShell(route: VisionRoute, compact: Boolean, onNavigate: (VisionRoute) -> Unit) {
    val isTop = route in TOP_LEVEL
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        // Each screen manages its own status-bar inset via systemBarsPadding;
        // the Scaffold only reserves room for the bottom bar (avoids double top inset).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { if (isTop && compact) VisionBottomBar(route, onNavigate) },
    ) { pad ->
        Box(Modifier.fillMaxSize().background(VisionColors.ScreenBackdrop)) {
            Row(Modifier.fillMaxSize().padding(pad)) {
                if (isTop && !compact) VisionNavRail(route, onNavigate)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    // Constrain content width on large screens for readability.
                    Box(Modifier.widthIn(max = if (compact) Dp.Unspecified else 760.dp).fillMaxSize()) {
                        RouteContent(route, onNavigate)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteContent(route: VisionRoute, onNavigate: (VisionRoute) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    when (route) {
        VisionRoute.HOME -> {
            val hud: HudViewModel = hiltViewModel()
            val home: HomeViewModel = hiltViewModel()
            val activity = ctx as? android.app.Activity
            androidx.compose.runtime.LaunchedEffect(Unit) {
                val intent = activity?.intent
                if (intent?.action == Intent.ACTION_SEND) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let(hud::onInputChange)
                    intent.action = null
                }
            }
            HomeScreen(
                hud = hud, home = home,
                onOpenSettings = { onNavigate(VisionRoute.SETTINGS) },
                onOpenAgents = { onNavigate(VisionRoute.AGENTS) },
                onOpenMemory = { onNavigate(VisionRoute.MEMORY) },
                onQuickAction = { handleQuickAction(it, ctx, onNavigate) },
            )
        }
        VisionRoute.AGENTS -> AgentsScreen(showBack = false)
        VisionRoute.APPS -> AppDrawerScreen(
            showBack = false,
            onOpenSettings = { onNavigate(VisionRoute.SETTINGS) },
            onOpenHub = { onNavigate(VisionRoute.HUB) },
        )
        VisionRoute.MEMORY -> MemoryScreen(showBack = false)
        VisionRoute.SETTINGS -> SettingsHubScreen(
            onBack = { onNavigate(VisionRoute.HOME) },
            onOpenAiTokens = { onNavigate(VisionRoute.AI_SETTINGS) },
            onOpenElection = { onNavigate(VisionRoute.ELECTION) },
        )
        VisionRoute.ELECTION -> BrainElectionScreen()
        VisionRoute.AI_SETTINGS -> AiTokensScreen(onBack = { onNavigate(VisionRoute.SETTINGS) })
        VisionRoute.RECENTS -> RecentsScreen(onBack = { onNavigate(VisionRoute.HOME) })
        VisionRoute.HUB -> VisionHubScreen(onBack = { onNavigate(VisionRoute.APPS) })
        VisionRoute.ONBOARDING, VisionRoute.SETUP -> Unit // handled upstream
    }
}

private fun handleQuickAction(qa: QuickAction, ctx: Context, onNavigate: (VisionRoute) -> Unit) {
    when (qa) {
        QuickAction.APPS -> onNavigate(VisionRoute.APPS)
        QuickAction.AGENTS -> onNavigate(VisionRoute.AGENTS)
        QuickAction.TASKS -> onNavigate(VisionRoute.MEMORY)
        QuickAction.BROWSER -> runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        QuickAction.FILES, QuickAction.AUTOMATION ->
            Toast.makeText(ctx, "${qa.label} — coming soon", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun VisionBottomBar(route: VisionRoute, onNavigate: (VisionRoute) -> Unit) {
    NavigationBar(containerColor = VisionColors.Surface.copy(alpha = 0.92f), tonalElevation = 0.dp) {
        NAV_ITEMS.forEach { item ->
            NavigationBarItem(
                selected = route == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Text(item.glyph, style = MaterialTheme.typography.titleMedium) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = VisionColors.CyanPrimary,
                    selectedTextColor = VisionColors.CyanPrimary,
                    unselectedIconColor = VisionColors.TextDim,
                    unselectedTextColor = VisionColors.TextDim,
                    indicatorColor = VisionColors.CyanFaint,
                ),
            )
        }
    }
}

@Composable
private fun VisionNavRail(route: VisionRoute, onNavigate: (VisionRoute) -> Unit) {
    NavigationRail(containerColor = VisionColors.Surface.copy(alpha = 0.92f)) {
        NAV_ITEMS.forEach { item ->
            NavigationRailItem(
                selected = route == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Text(item.glyph, style = MaterialTheme.typography.titleMedium) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}
