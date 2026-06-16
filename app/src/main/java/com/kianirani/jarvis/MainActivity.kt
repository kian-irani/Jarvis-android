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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import com.kianirani.jarvis.ui.theme.VisionIcons
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

enum class VisionRoute { ONBOARDING, SETUP, HOME, AGENTS, APPS, MEMORY, SETTINGS, ELECTION, AI_SETTINGS, HUB }

/** The five tabs that appear in the bottom navigation / nav rail. */
private val TOP_LEVEL = setOf(VisionRoute.HOME, VisionRoute.AGENTS, VisionRoute.APPS, VisionRoute.MEMORY, VisionRoute.SETTINGS)
private data class NavItem(val route: VisionRoute, val icon: ImageVector, val label: String)
private val NAV_ITEMS = listOf(
    NavItem(VisionRoute.HOME, VisionIcons.Home, "Home"),
    NavItem(VisionRoute.AGENTS, VisionIcons.Agents, "Agents"),
    NavItem(VisionRoute.APPS, VisionIcons.Apps, "Apps"),
    NavItem(VisionRoute.MEMORY, VisionIcons.Memory, "Memory"),
    NavItem(VisionRoute.SETTINGS, VisionIcons.Settings, "Settings"),
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
        com.kianirani.jarvis.ui.theme.FontStore.init(this)
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        setContent {
            JarvisTheme {
                val widthClass = calculateWindowSizeClass(this).widthSizeClass
                val compact = widthClass == WindowWidthSizeClass.Compact
                val expanded = widthClass == WindowWidthSizeClass.Expanded
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
                    else -> VisionShell(route, compact, expanded, onNavigate = { route = it })
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
private fun VisionShell(route: VisionRoute, compact: Boolean, expanded: Boolean, onNavigate: (VisionRoute) -> Unit) {
    val isTop = route in TOP_LEVEL
    // On expanded widths Home shows a persistent right-hand Agents panel, so it
    // must use the full pane (no readability cap fighting the side panel).
    val homeWide = expanded && route == VisionRoute.HOME
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
                    Box(Modifier.widthIn(max = if (compact || homeWide) Dp.Unspecified else 760.dp).fillMaxSize()) {
                        RouteContent(route, homeWide, onNavigate)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteContent(route: VisionRoute, homeWide: Boolean, onNavigate: (VisionRoute) -> Unit) {
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
            val homeContent = @Composable { sidePanels: Boolean ->
                HomeScreen(
                    hud = hud, home = home,
                    onOpenSettings = { onNavigate(VisionRoute.SETTINGS) },
                    onOpenAgents = { onNavigate(VisionRoute.AGENTS) },
                    onOpenMemory = { onNavigate(VisionRoute.MEMORY) },
                    onQuickAction = { handleQuickAction(it, ctx, onNavigate) },
                    showSidePanels = sidePanels,
                )
            }
            if (homeWide) {
                // Expanded layout: home pane + persistent Agents side panel (RD7).
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight()) { homeContent(true) }
                    Box(Modifier.width(360.dp).fillMaxHeight()) {
                        com.kianirani.jarvis.ui.screen.agents.AgentsScreen(showBack = false)
                    }
                }
            } else {
                homeContent(false)
            }
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

/**
 * Floating Vision dock (RD3, 2026-06-16) — rebuilt to the orb-launcher reference:
 * a glass pill that floats above the gesture bar with Phone · Messages · **Vision**
 * (a larger, glowing plasma orb, raised above the rail) · Camera · Apps. Phone /
 * Messages / Camera open the system default apps; Vision returns Home; Apps opens
 * the drawer. Glass + glow read the state-backed [VisionColors] so it recolours
 * with the theme. (Tablet keeps the [VisionNavRail] for the in-app hubs.)
 */
@Composable
private fun VisionBottomBar(route: VisionRoute, onNavigate: (VisionRoute) -> Unit) {
    val ctx = LocalContext.current
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 10.dp)
            // Swipe up on the dock opens the app drawer (RD10 launcher gesture).
            // A drag is distinct from a tap, so the dock buttons still work.
            .pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragEnd = { if (dy < -70f) onNavigate(VisionRoute.APPS); dy = 0f },
                    onVerticalDrag = { _, amount -> dy += amount },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .drawBehind {
                    // soft ambient halo under the floating pill
                    drawRoundRect(
                        color = VisionColors.CyanGlow,
                        topLeft = Offset(-6f, 2f),
                        size = androidx.compose.ui.geometry.Size(size.width + 12f, size.height + 8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(34.dp.toPx()),
                        alpha = 0.30f,
                    )
                }
                .clip(RoundedCornerShape(30.dp))
                .background(VisionColors.GlassPanel)
                .border(1.dp, VisionColors.Border, RoundedCornerShape(30.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DockItem(VisionIcons.Phone, "Phone", Modifier.weight(1f)) { launchDial(ctx) }
            DockItem(VisionIcons.Messages, "Messages", Modifier.weight(1f)) { launchMessaging(ctx) }
            DockVision(selected = route == VisionRoute.HOME, modifier = Modifier.weight(1f)) { onNavigate(VisionRoute.HOME) }
            DockItem(VisionIcons.Camera, "Camera", Modifier.weight(1f)) { launchCamera(ctx) }
            DockItem(VisionIcons.Apps, "Apps", Modifier.weight(1f), selected = route == VisionRoute.APPS) { onNavigate(VisionRoute.APPS) }
        }
    }
}

/** A standard dock slot — a 56dp tappable column with a single vector glyph. */
@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier.height(56.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, contentDescription = label,
            tint = if (selected) VisionColors.CyanPrimary else VisionColors.TextSecondary,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** The centre Vision slot — a larger plasma orb that pops above the dock rail. */
@Composable
private fun DockVision(selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.height(56.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.offset(y = (-14).dp).size(58.dp)
                .drawBehind {
                    drawCircle(
                        color = if (selected) VisionColors.CyanPrimary else VisionColors.Violet,
                        radius = size.minDimension * 0.72f,
                        alpha = 0.40f,
                    )
                }
                .clip(CircleShape)
                .background(VisionColors.PlasmaSweep)
                .border(1.5.dp, VisionColors.TextPrimary.copy(alpha = 0.35f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VisionIcons.Spark, "Vision", tint = VisionColors.Background, modifier = Modifier.size(28.dp))
        }
    }
}

private fun launchDial(ctx: Context) = runCatching {
    ctx.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}.onFailure { Toast.makeText(ctx, "No dialer app", Toast.LENGTH_SHORT).show() }

private fun launchMessaging(ctx: Context) = runCatching {
    ctx.startActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}.onFailure { Toast.makeText(ctx, "No messaging app", Toast.LENGTH_SHORT).show() }

private fun launchCamera(ctx: Context) = runCatching {
    ctx.startActivity(
        Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}.onFailure { Toast.makeText(ctx, "No camera app", Toast.LENGTH_SHORT).show() }

@Composable
private fun VisionNavRail(route: VisionRoute, onNavigate: (VisionRoute) -> Unit) {
    NavigationRail(containerColor = VisionColors.Surface.copy(alpha = 0.92f)) {
        NAV_ITEMS.forEach { item ->
            NavigationRailItem(
                selected = route == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}
