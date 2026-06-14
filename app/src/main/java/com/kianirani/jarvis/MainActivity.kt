package com.kianirani.jarvis

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.kianirani.jarvis.ui.screen.election.BrainElectionScreen
import com.kianirani.jarvis.ui.screen.hud.HudScreen
import com.kianirani.jarvis.ui.screen.hud.HudViewModel
import com.kianirani.jarvis.ui.screen.drawer.AppDrawerScreen
import com.kianirani.jarvis.ui.screen.recents.RecentsScreen
import com.kianirani.jarvis.ui.screen.onboarding.OnboardingScreen
import com.kianirani.jarvis.ui.screen.settings.AiTokensScreen
import com.kianirani.jarvis.ui.screen.settings.SettingsHubScreen
import com.kianirani.jarvis.ui.screen.setup.SetupWizardScreen
import com.kianirani.jarvis.ui.theme.JarvisTheme
import dagger.hilt.android.AndroidEntryPoint

private const val PREFS = "vision_prefs"
private const val KEY_SETUP_COMPLETE = "setup_complete"
private const val KEY_ONBOARDED = "persona_onboarded"

enum class VisionRoute { ONBOARDING, SETUP, HUD, ELECTION, AI_SETTINGS, APPS, RECENTS, SETTINGS }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* features degrade per-permission if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Vision needs mic (voice), camera (QR pairing) and notifications (brain
        // service) — ask for everything missing on first launch (user directive).
        val wanted = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (wanted.isNotEmpty()) permissions.launch(wanted.toTypedArray())
        // Standalone brain: the on-device Brain-Lite server must be alive for
        // BRAIN=ONLINE without any second device.
        androidx.core.content.ContextCompat.startForegroundService(
            this, android.content.Intent(this, com.kianirani.jarvis.brain.BrainLiteService::class.java),
        )
        // Load persisted appearance (theme / accent / wallpaper / animations /
        // brain badge) before first composition so the HUD paints correctly.
        com.kianirani.jarvis.ui.theme.ThemeStore.init(this)
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        setContent {
            JarvisTheme {
                var route by rememberSaveable {
                    mutableStateOf(
                        when {
                            !prefs.getBoolean(KEY_ONBOARDED, false) -> VisionRoute.ONBOARDING
                            !prefs.getBoolean(KEY_SETUP_COMPLETE, false) -> VisionRoute.HUD
                            else -> VisionRoute.HUD
                        }
                    )
                }
                BackHandler(enabled = route != VisionRoute.HUD && route != VisionRoute.SETUP && route != VisionRoute.ONBOARDING) { route = VisionRoute.HUD }
                when (route) {
                    VisionRoute.ONBOARDING -> OnboardingScreen(onFinished = {
                        prefs.edit().putBoolean(KEY_ONBOARDED, true).putBoolean(KEY_SETUP_COMPLETE, true).apply()
                        route = VisionRoute.HUD
                    })
                    VisionRoute.SETUP -> SetupWizardScreen(onFinished = {
                        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
                        route = VisionRoute.HUD
                    })
                    VisionRoute.HUD -> {
                        val vm: HudViewModel = hiltViewModel()
                        // P14: text shared from any app lands in the command bar.
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            if (intent?.action == android.content.Intent.ACTION_SEND) {
                                intent.getStringExtra(android.content.Intent.EXTRA_TEXT)?.let(vm::onInputChange)
                                intent.action = null
                            }
                        }
                        HudScreen(
                            viewModel = vm,
                            onOpenElection = { route = VisionRoute.ELECTION },
                            onOpenAiSettings = { route = VisionRoute.AI_SETTINGS },
                            onOpenApps = { route = VisionRoute.APPS },
                            onOpenRecents = { route = VisionRoute.RECENTS },
                            onOpenSettings = { route = VisionRoute.SETTINGS },
                        )
                    }
                    VisionRoute.ELECTION -> BrainElectionScreen()
                    VisionRoute.AI_SETTINGS -> AiTokensScreen(onBack = { route = VisionRoute.HUD })
                    VisionRoute.APPS -> AppDrawerScreen(onBack = { route = VisionRoute.HUD })
                    VisionRoute.RECENTS -> RecentsScreen(onBack = { route = VisionRoute.HUD })
                    VisionRoute.SETTINGS -> SettingsHubScreen(
                        onBack = { route = VisionRoute.HUD },
                        onOpenAiTokens = { route = VisionRoute.AI_SETTINGS },
                        onOpenElection = { route = VisionRoute.ELECTION },
                    )
                }
            }
        }
    }
}
