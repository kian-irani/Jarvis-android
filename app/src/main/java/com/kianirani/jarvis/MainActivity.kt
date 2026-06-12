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
import com.kianirani.jarvis.ui.screen.settings.AiTokensScreen
import com.kianirani.jarvis.ui.screen.settings.SettingsHubScreen
import com.kianirani.jarvis.ui.screen.setup.SetupWizardScreen
import com.kianirani.jarvis.ui.theme.JarvisTheme
import dagger.hilt.android.AndroidEntryPoint

private const val PREFS = "vision_prefs"
private const val KEY_SETUP_COMPLETE = "setup_complete"

enum class VisionRoute { SETUP, HUD, ELECTION, AI_SETTINGS, APPS, SETTINGS }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* voice stays off if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        setContent {
            JarvisTheme {
                var route by rememberSaveable {
                    mutableStateOf(
                        if (prefs.getBoolean(KEY_SETUP_COMPLETE, false)) VisionRoute.HUD else VisionRoute.SETUP
                    )
                }
                BackHandler(enabled = route != VisionRoute.HUD && route != VisionRoute.SETUP) { route = VisionRoute.HUD }
                when (route) {
                    VisionRoute.SETUP -> SetupWizardScreen(onFinished = {
                        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
                        route = VisionRoute.HUD
                    })
                    VisionRoute.HUD -> {
                        val vm: HudViewModel = hiltViewModel()
                        HudScreen(
                            viewModel = vm,
                            onOpenElection = { route = VisionRoute.ELECTION },
                            onOpenAiSettings = { route = VisionRoute.AI_SETTINGS },
                            onOpenApps = { route = VisionRoute.APPS },
                            onOpenSettings = { route = VisionRoute.SETTINGS },
                        )
                    }
                    VisionRoute.ELECTION -> BrainElectionScreen()
                    VisionRoute.AI_SETTINGS -> AiTokensScreen(onBack = { route = VisionRoute.HUD })
                    VisionRoute.APPS -> AppDrawerScreen(onBack = { route = VisionRoute.HUD })
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
