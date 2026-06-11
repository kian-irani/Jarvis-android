package com.kianirani.jarvis

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.kianirani.jarvis.ui.screen.election.BrainElectionScreen
import com.kianirani.jarvis.ui.screen.hud.HudScreen
import com.kianirani.jarvis.ui.screen.hud.HudViewModel
import com.kianirani.jarvis.ui.screen.setup.SetupWizardScreen
import com.kianirani.jarvis.ui.theme.JarvisTheme
import dagger.hilt.android.AndroidEntryPoint

private const val PREFS = "vision_prefs"
private const val KEY_SETUP_COMPLETE = "setup_complete"

enum class VisionRoute { SETUP, HUD, ELECTION }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        setContent {
            JarvisTheme {
                var route by rememberSaveable {
                    mutableStateOf(
                        if (prefs.getBoolean(KEY_SETUP_COMPLETE, false)) VisionRoute.HUD else VisionRoute.SETUP
                    )
                }
                BackHandler(enabled = route == VisionRoute.ELECTION) { route = VisionRoute.HUD }
                when (route) {
                    VisionRoute.SETUP -> SetupWizardScreen(onFinished = {
                        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
                        route = VisionRoute.HUD
                    })
                    VisionRoute.HUD -> {
                        val vm: HudViewModel = hiltViewModel()
                        HudScreen(viewModel = vm, onOpenElection = { route = VisionRoute.ELECTION })
                    }
                    VisionRoute.ELECTION -> BrainElectionScreen()
                }
            }
        }
    }
}
