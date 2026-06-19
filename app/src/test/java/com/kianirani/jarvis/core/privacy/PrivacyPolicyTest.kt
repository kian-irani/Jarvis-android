package com.kianirani.jarvis.core.privacy

import com.kianirani.jarvis.core.mesh.RouteDecision
import com.kianirani.jarvis.core.mesh.Sensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LM5 acceptance: local-only forces PRIVATE, forbids cloud, downgrades a cloud route to
 * Unavailable, and leaves everything else untouched when off. Pure.
 */
class PrivacyPolicyTest {

    @Test fun `local-only forces private sensitivity`() {
        assertEquals(Sensitivity.PRIVATE, PrivacyPolicy.effectiveSensitivity(Sensitivity.NORMAL, localOnly = true))
        assertEquals(Sensitivity.NORMAL, PrivacyPolicy.effectiveSensitivity(Sensitivity.NORMAL, localOnly = false))
    }

    @Test fun `cloud is forbidden only under local-only`() {
        assertFalse(PrivacyPolicy.allowsCloud(localOnly = true))
        assertTrue(PrivacyPolicy.allowsCloud(localOnly = false))
    }

    @Test fun `sanitize downgrades a cloud route under local-only`() {
        assertEquals(RouteDecision.Unavailable, PrivacyPolicy.sanitize(RouteDecision.Cloud, localOnly = true))
    }

    @Test fun `sanitize passes cloud through when not local-only`() {
        assertEquals(RouteDecision.Cloud, PrivacyPolicy.sanitize(RouteDecision.Cloud, localOnly = false))
    }

    @Test fun `sanitize never touches non-cloud decisions`() {
        val mesh = RouteDecision.Mesh("desktop")
        assertEquals(mesh, PrivacyPolicy.sanitize(mesh, localOnly = true))
        assertEquals(RouteDecision.LocalOnDevice, PrivacyPolicy.sanitize(RouteDecision.LocalOnDevice, localOnly = true))
    }

    @Test fun `label reflects the mode`() {
        assertTrue(PrivacyPolicy.label(true).contains("Private"))
        assertTrue(PrivacyPolicy.label(false).contains("cloud"))
    }
}
