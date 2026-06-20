package com.kianirani.jarvis.data.perception

import com.kianirani.jarvis.data.ai.CloudChatRouter
import com.kianirani.jarvis.service.VisionAccessibilityService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VCF-M2 — visual perception pipeline (PRD §8.1). Lets Vision *see the screen*: captures the
 * current display via the Accessibility screenshot API and asks a vision-capable model about it
 * ([CloudChatRouter.chatWithImage], MM). If no screen can be captured (service off / pre-R) or
 * no vision model is configured, it degrades to a plain text answer so the user is never stuck.
 *
 * Real wiring (compile-verified); ML-Kit OCR fallback and camera/gallery sources are follow-ups.
 */
@Singleton
class VisualPerception @Inject constructor(
    private val router: CloudChatRouter,
) {
    /**
     * Answer [prompt] about the **current screen**. Captures a screenshot through the
     * Accessibility service and sends it to a vision model. Returns a graceful message when the
     * screen can't be seen.
     */
    suspend fun seeScreen(prompt: String): Result<String> {
        val service = VisionAccessibilityService.instance
            ?: return Result.success("I can't see the screen — enable Vision's Accessibility service in Settings.")
        val shot = service.captureScreenshot()
            ?: return Result.success("I couldn't capture the screen on this device.")
        return seeImage(prompt, shot)
    }

    /** Answer [prompt] about an explicit [imageBytes] (camera/gallery/screenshot). */
    suspend fun seeImage(prompt: String, imageBytes: ByteArray): Result<String> =
        router.chatWithImage(prompt.ifBlank { "Describe what is on the screen." }, imageBytes)
            .map { it.text }
}
