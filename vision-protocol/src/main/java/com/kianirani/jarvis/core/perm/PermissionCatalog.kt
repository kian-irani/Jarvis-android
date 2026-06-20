package com.kianirani.jarvis.core.perm

/**
 * PERM — runtime permission manager (PRD §, "storage/media/notif/overlay …"). The pure catalog
 * that maps each Vision capability to the Android permissions it needs, so the UI can show "to
 * use X, grant Y" and request only what's missing. Keeping the mapping + the missing-set logic
 * pure makes it deterministic and JVM-tested; the actual `requestPermissions` / settings-intent
 * launch is the device half.
 */
enum class VisionCapability(val title: String, val permissions: List<String>) {
    CALLS("Place calls", listOf("android.permission.CALL_PHONE")),
    MESSAGING("Send SMS", listOf("android.permission.SEND_SMS")),
    CONTACTS("Resolve contacts", listOf("android.permission.READ_CONTACTS")),
    MICROPHONE("Voice & wake word", listOf("android.permission.RECORD_AUDIO")),
    CAMERA("Camera & QR", listOf("android.permission.CAMERA")),
    NOTIFICATIONS("Post notifications", listOf("android.permission.POST_NOTIFICATIONS")),
    OVERLAY("Floating orb", listOf("android.permission.SYSTEM_ALERT_WINDOW")),
}

object PermissionCatalog {

    /** Permissions a capability needs that are NOT in [granted] — what to actually request. */
    fun missing(capability: VisionCapability, granted: Set<String>): List<String> =
        capability.permissions.filter { it !in granted }

    /** True if [capability] is fully usable under [granted]. */
    fun isSatisfied(capability: VisionCapability, granted: Set<String>): Boolean =
        missing(capability, granted).isEmpty()

    /** Every distinct permission missing across [capabilities] (one batched request). */
    fun missingFor(capabilities: List<VisionCapability>, granted: Set<String>): List<String> =
        capabilities.flatMap { missing(it, granted) }.distinct()

    /** Capabilities the user can use right now under [granted]. */
    fun available(granted: Set<String>): List<VisionCapability> =
        VisionCapability.entries.filter { isSatisfied(it, granted) }
}
