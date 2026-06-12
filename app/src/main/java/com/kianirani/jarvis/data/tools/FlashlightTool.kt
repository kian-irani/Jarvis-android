package com.kianirani.jarvis.data.tools

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Toggles the camera torch (no preview needed) on EN/FA commands. */
@Singleton
class FlashlightTool @Inject constructor(
    @ApplicationContext private val context: Context,
) : Tool {
    override val id = "flashlight"

    override fun matches(message: String): Boolean =
        message.startsWith("flashlight") || message.startsWith("torch") ||
            message.contains("چراغ قوه") || message.contains("چراغ‌قوه") ||
            message == "فلش" || message == "flash"

    override fun run(message: String): ToolResult {
        val off = message.contains("off") || message.contains("خاموش")
        return ToolResult(toggle(enable = !off))
    }

    private fun toggle(enable: Boolean): String = runCatching {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return "This device has no flashlight."
        cm.setTorchMode(id, enable)
        if (enable) "Flashlight on." else "Flashlight off."
    }.getOrElse { "Couldn't control the flashlight: ${it.message}" }
}
