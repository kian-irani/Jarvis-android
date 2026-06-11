package com.kianirani.jarvis.ui.screen

import com.kianirani.jarvis.brain.data.NodeRepository
import com.kianirani.jarvis.brain.discovery.BrainCandidate
import com.kianirani.jarvis.brain.discovery.BrainHandshake
import com.kianirani.jarvis.brain.discovery.DiscoveryScanner
import com.kianirani.jarvis.brain.data.db.NodeDao
import com.kianirani.jarvis.brain.data.db.NodeEntity
import com.kianirani.jarvis.brain.score.DeviceMetrics
import com.kianirani.jarvis.brain.score.NodeMetricsCodec
import com.kianirani.jarvis.ui.screen.election.BrainElectionViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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

    /** Fake scanner: captures the update callback so tests can push candidates. */
    private var pushCandidates: ((List<BrainCandidate>) -> Unit)? = null
    private val scanner = DiscoveryScanner { onUpdate ->
        pushCandidates = onUpdate
        AutoCloseable { pushCandidates = null }
    }
    private var handshakeResult = true
    private var handshakeTarget: Pair<String, Int>? = null
    private val handshake = BrainHandshake { host, port ->
        handshakeTarget = host to port
        handshakeResult
    }

    private fun SetupWizardViewModel() = SetupWizardViewModel(scanner, handshake)

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

    @Test fun `mdns candidates stream into state and selection survives updates`() {
        val vm = SetupWizardViewModel()
        val brainA = BrainCandidate("Vision-A", "10.0.0.2", 7799)
        pushCandidates!!(listOf(brainA))
        assertEquals(listOf(brainA), vm.state.value.candidates)
        vm.onCandidateSelected(brainA)
        pushCandidates!!(listOf(brainA, BrainCandidate("Vision-B", "10.0.0.3", 7799)))
        assertEquals(brainA, vm.state.value.selectedCandidate)
        pushCandidates!!(emptyList()) // A disappears → selection cleared
        assertNull(vm.state.value.selectedCandidate)
    }

    @Test fun `connect uses real handshake against join payload target`() = runTest(dispatcher) {
        val vm = SetupWizardViewModel()
        vm.onDeviceNameChanged("pixel"); vm.next()
        vm.onTokenChanged("vision://join?host=10.0.0.9&port=7799&token=abc"); vm.next()
        vm.next() // CONNECT
        advanceUntilIdle()
        assertEquals("10.0.0.9" to 7799, handshakeTarget)
        assertEquals(ConnectStatus.OK, vm.state.value.connectStatus)
    }

    @Test fun `failed handshake sets FAILED and stays on connect step`() = runTest(dispatcher) {
        handshakeResult = false
        val vm = SetupWizardViewModel()
        vm.onDeviceNameChanged("pixel"); vm.next(); vm.next()
        vm.onCandidateSelected(BrainCandidate("Vision-A", "10.0.0.2", 7799))
        vm.next() // CONNECT
        advanceUntilIdle()
        assertEquals(ConnectStatus.FAILED, vm.state.value.connectStatus)
        assertEquals(2, vm.state.value.step)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BrainElectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val now = 1_000_000L
    private val registry = MutableSharedFlow<List<NodeEntity>>(replay = 1)

    private fun fakeDao() = object : NodeDao {
        val rows = mutableListOf<NodeEntity>()
        override suspend fun upsert(n: NodeEntity) { rows.removeAll { it.id == n.id }; rows += n }
        override suspend fun list() = rows.sortedByDescending { it.brain_score }
        override fun observe(): Flow<List<NodeEntity>> = registry
    }

    private fun vm() = BrainElectionViewModel(
        nodeRepository = NodeRepository(fakeDao()),
        localMetrics = { DeviceMetrics(ramFreeGb = 2.0, cpuCores = 8, batteryPercent = 100) },
        clock = { now },
    )

    @Test fun `local node elected by default`() {
        val vm = vm()
        val s = vm.state.value
        assertEquals(1, s.nodes.size)
        assertTrue(s.nodes[0].isElected)
    }

    @Test fun `higher scoring vps node wins election`() {
        val vm = vm()
        vm.onNodeMetrics("vps1", "trvps1", DeviceMetrics(ramFreeGb = 8.0, cpuCores = 16, isVps = true))
        val elected = vm.state.value.nodes.first { it.isElected }
        assertEquals("vps1", elected.id)
    }

    @Test fun `tap non-elected node sets manual override, tap elected clears it`() {
        val vm = vm()
        vm.onNodeMetrics("vps1", "trvps1", DeviceMetrics(ramFreeGb = 8.0, cpuCores = 16, isVps = true))
        vm.onNodeTapped("local")
        assertEquals("local", vm.state.value.manualOverrideId)
        assertTrue(vm.state.value.nodes.first { it.id == "local" }.isElected)
        vm.onNodeTapped("local") // tapping elected → back to auto
        assertNull(vm.state.value.manualOverrideId)
        assertTrue(vm.state.value.nodes.first { it.id == "vps1" }.isElected)
    }

    @Test fun `offline node excluded from election`() {
        val vm = vm()
        vm.onNodeMetrics("vps1", "trvps1", DeviceMetrics(ramFreeGb = 8.0, cpuCores = 16, isVps = true))
        vm.onNodeMetrics("vps1", "trvps1", DeviceMetrics(ramFreeGb = 8.0, cpuCores = 16, isVps = true), isOnline = false)
        assertTrue(vm.state.value.nodes.first { it.id == "local" }.isElected)
        assertFalse(vm.state.value.nodes.first { it.id == "vps1" }.isElected)
    }

    @Test fun `registry rows stream into election with freshness check`() = runTest(dispatcher) {
        val vm = vm()
        registry.emit(
            listOf(
                NodeEntity(
                    id = "vps1", name = "trvps1", address = "127.0.0.1:7799",
                    capabilities = NodeMetricsCodec.encode(DeviceMetrics(ramFreeGb = 8.0, cpuCores = 16, isVps = true)),
                    brain_score = 0, last_seen = now - 10_000, // fresh
                ),
                NodeEntity(
                    id = "stale1", name = "old-phone", address = "10.0.0.9:7799",
                    capabilities = NodeMetricsCodec.encode(DeviceMetrics(ramFreeGb = 16.0, cpuCores = 32, isVps = true)),
                    brain_score = 0, last_seen = now - 500_000, // stale ⇒ offline
                ),
            )
        )
        advanceUntilIdle()
        val s = vm.state.value
        assertEquals(3, s.nodes.size)
        assertTrue(s.nodes.first { it.id == "vps1" }.isElected)
        assertFalse(s.nodes.first { it.id == "stale1" }.online)
        assertFalse(s.nodes.first { it.id == "stale1" }.isElected)
    }

    @Test fun `registry row without metrics falls back to zero score`() = runTest(dispatcher) {
        val vm = vm()
        registry.emit(
            listOf(
                NodeEntity("bare1", "bare", "10.0.0.2:7799", "{}", brain_score = 0, last_seen = now),
            )
        )
        advanceUntilIdle()
        val bare = vm.state.value.nodes.first { it.id == "bare1" }
        assertTrue(bare.online)
        assertFalse(bare.isElected) // local (score > 0) beats it
    }
}
