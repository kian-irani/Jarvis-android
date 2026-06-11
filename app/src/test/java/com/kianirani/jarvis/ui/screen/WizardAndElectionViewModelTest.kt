package com.kianirani.jarvis.ui.screen

import com.kianirani.jarvis.brain.score.DeviceMetrics
import com.kianirani.jarvis.ui.screen.election.BrainElectionViewModel
import com.kianirani.jarvis.ui.screen.setup.ConnectStatus
import com.kianirani.jarvis.ui.screen.setup.DiscoveryMethod
import com.kianirani.jarvis.ui.screen.setup.SetupWizardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SetupWizardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `step0 blocked until device name entered`() {
        val vm = SetupWizardViewModel()
        assertFalse(vm.state.value.canAdvance)
        vm.next()
        assertEquals(0, vm.state.value.step)
        vm.onDeviceNameChanged("pixel")
        assertTrue(vm.state.value.canAdvance)
        vm.next()
        assertEquals(1, vm.state.value.step)
    }

    @Test fun `token method requires token`() {
        val vm = SetupWizardViewModel()
        vm.onDeviceNameChanged("pixel"); vm.next()
        vm.onDiscoveryMethodSelected(DiscoveryMethod.TOKEN)
        assertFalse(vm.state.value.canAdvance)
        vm.onTokenChanged("  abc123  ")
        assertEquals("abc123", vm.state.value.token)
        assertTrue(vm.state.value.canAdvance)
    }

    @Test fun `connect advances to done on success`() = runTest(dispatcher) {
        val vm = SetupWizardViewModel()
        vm.onDeviceNameChanged("pixel"); vm.next(); vm.next()
        assertEquals(2, vm.state.value.step)
        vm.next() // CONNECT
        assertEquals(ConnectStatus.CONNECTING, vm.state.value.connectStatus)
        advanceUntilIdle()
        assertEquals(ConnectStatus.OK, vm.state.value.connectStatus)
        assertEquals(3, vm.state.value.step)
    }

    @Test fun `back resets connect status`() {
        val vm = SetupWizardViewModel()
        vm.onDeviceNameChanged("pixel"); vm.next(); vm.next()
        vm.back()
        assertEquals(1, vm.state.value.step)
        assertEquals(ConnectStatus.IDLE, vm.state.value.connectStatus)
    }
}

class BrainElectionViewModelTest {

    @Test fun `local node elected by default`() {
        val vm = BrainElectionViewModel()
        val s = vm.state.value
        assertEquals(1, s.nodes.size)
        assertTrue(s.nodes[0].isElected)
    }

    @Test fun `higher scoring vps node wins election`() {
        val vm = BrainElectionViewModel()
        vm.onNodeMetrics("vps1", "trvps1", DeviceMetrics(ramFreeGb = 8.0, cpuCores = 16, isVps = true))
        val elected = vm.state.value.nodes.first { it.isElected }
        assertEquals("vps1", elected.id)
    }

    @Test fun `tap non-elected node sets manual override, tap elected clears it`() {
        val vm = BrainElectionViewModel()
        vm.onNodeMetrics("vps1", "trvps1", DeviceMetrics(ramFreeGb = 8.0, cpuCores = 16, isVps = true))
        vm.onNodeTapped("local")
        assertEquals("local", vm.state.value.manualOverrideId)
        assertTrue(vm.state.value.nodes.first { it.id == "local" }.isElected)
        vm.onNodeTapped("local") // tapping elected → back to auto
        assertNull(vm.state.value.manualOverrideId)
        assertTrue(vm.state.value.nodes.first { it.id == "vps1" }.isElected)
    }

    @Test fun `offline node excluded from election`() {
        val vm = BrainElectionViewModel()
        vm.onNodeMetrics("vps1", "trvps1", DeviceMetrics(ramFreeGb = 8.0, cpuCores = 16, isVps = true))
        vm.onNodeMetrics("vps1", "trvps1", DeviceMetrics(ramFreeGb = 8.0, cpuCores = 16, isVps = true), isOnline = false)
        assertTrue(vm.state.value.nodes.first { it.id == "local" }.isElected)
        assertFalse(vm.state.value.nodes.first { it.id == "vps1" }.isElected)
    }
}
