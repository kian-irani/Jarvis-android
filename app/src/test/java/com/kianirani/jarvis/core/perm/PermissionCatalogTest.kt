package com.kianirani.jarvis.core.perm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PERM acceptance: missing-set, satisfied check, batched request, available list. Pure. */
class PermissionCatalogTest {

    @Test fun `missing returns ungranted permissions`() {
        val missing = PermissionCatalog.missing(VisionCapability.CALLS, granted = emptySet())
        assertEquals(listOf("android.permission.CALL_PHONE"), missing)
    }

    @Test fun `satisfied when all granted`() {
        assertTrue(PermissionCatalog.isSatisfied(VisionCapability.CALLS, setOf("android.permission.CALL_PHONE")))
        assertFalse(PermissionCatalog.isSatisfied(VisionCapability.CALLS, emptySet()))
    }

    @Test fun `missingFor batches and dedupes`() {
        val missing = PermissionCatalog.missingFor(
            listOf(VisionCapability.CALLS, VisionCapability.MESSAGING),
            granted = setOf("android.permission.CALL_PHONE"),
        )
        assertEquals(listOf("android.permission.SEND_SMS"), missing)
    }

    @Test fun `available lists satisfied capabilities`() {
        val available = PermissionCatalog.available(setOf("android.permission.RECORD_AUDIO"))
        assertTrue(available.contains(VisionCapability.MICROPHONE))
        assertFalse(available.contains(VisionCapability.CALLS))
    }
}
