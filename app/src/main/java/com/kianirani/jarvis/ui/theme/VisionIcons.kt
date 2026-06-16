package com.kianirani.jarvis.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.kianirani.jarvis.data.agent.AgentId
import com.kianirani.jarvis.data.settings.QuickAction

/**
 * v28 (2026-06-15): the launcher dropped emoji glyphs for a single, consistent
 * rounded vector icon family (Material Symbols Rounded). They are crisp at any
 * size, recolour with the theme accent, and match the orb-launcher reference —
 * no more font-dependent emoji rendering differently per device.
 */
object VisionIcons {
    val Home: ImageVector = Icons.Rounded.Home
    val Agents: ImageVector = Icons.Rounded.SmartToy
    val Apps: ImageVector = Icons.Rounded.GridView
    val Memory: ImageVector = Icons.Rounded.Memory
    val Settings: ImageVector = Icons.Rounded.Settings
    val Send: ImageVector = Icons.Rounded.ArrowUpward
    val Mic: ImageVector = Icons.Rounded.GraphicEq
    val Weather: ImageVector = Icons.Rounded.WbSunny
    val Tasks: ImageVector = Icons.Rounded.CheckCircle
    val Devices: ImageVector = Icons.Rounded.Smartphone

    // Orb satellites + quick access + AI-status metrics (orb-launcher reference).
    val Spark: ImageVector = Icons.Rounded.AutoAwesome
    val Projects: ImageVector = Icons.Rounded.Dashboard
    val Automation: ImageVector = Icons.Rounded.Bolt
    val Files: ImageVector = Icons.Rounded.Folder
    val Browser: ImageVector = Icons.Rounded.Language
    val More: ImageVector = Icons.Rounded.MoreHoriz
    val Cpu: ImageVector = Icons.Rounded.DeveloperBoard
    val Ram: ImageVector = Icons.Rounded.Memory
    val Battery: ImageVector = Icons.Rounded.BatteryFull

    // Floating dock (orb-launcher reference): Phone · Messages · Vision · Camera · Apps.
    val Phone: ImageVector = Icons.Rounded.Call
    val Messages: ImageVector = Icons.Rounded.ChatBubble
    val Camera: ImageVector = Icons.Rounded.PhotoCamera

    // App drawer (RD3): search + back.
    val Search: ImageVector = Icons.Rounded.Search
    val Back: ImageVector = Icons.Rounded.ChevronLeft

    /** Icon for a home quick-action tile. */
    fun forAction(a: QuickAction): ImageVector = when (a) {
        QuickAction.APPS -> Icons.Rounded.GridView
        QuickAction.FILES -> Icons.Rounded.Folder
        QuickAction.BROWSER -> Icons.Rounded.Language
        QuickAction.TASKS -> Icons.Rounded.CheckCircle
        QuickAction.AUTOMATION -> Icons.Rounded.Bolt
        QuickAction.AGENTS -> Icons.Rounded.SmartToy
    }

    /** Icon for an agent row. */
    fun forAgent(id: AgentId): ImageVector = when (id) {
        AgentId.RESEARCH -> Icons.Rounded.Search
        AgentId.AUTOMATION -> Icons.Rounded.Bolt
        AgentId.DEVELOPER -> Icons.Rounded.Code
        AgentId.DEVICE -> Icons.Rounded.Smartphone
    }
}
