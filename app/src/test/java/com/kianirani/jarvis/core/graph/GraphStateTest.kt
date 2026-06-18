package com.kianirani.jarvis.core.graph

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * VCF-G1 acceptance (PRD §4.2): the reducer is pure and applies the right channel
 * semantics — messages APPEND, plan REPLACE, observations APPEND, scratch MERGE,
 * steps BOUND — plus message helpers and serializable round-tripping.
 */
class GraphStateTest {

    private val user = VisionMessage.text(Role.USER, "hello")
    private val assistant = VisionMessage.text(Role.ASSISTANT, "hi there")

    // --- reduce: per-channel semantics ---

    @Test fun `messages channel appends`() {
        val state = GraphState(messages = listOf(user))
        val next = state.reduce(StateUpdate(appendMessages = listOf(assistant)))
        assertEquals(listOf(user, assistant), next.messages)
    }

    @Test fun `plan channel replaces when update carries a plan`() {
        val old = ActionPlan("old goal")
        val new = ActionPlan("new goal", listOf(PlanStep("s1", "do it")))
        val next = GraphState(plan = old).reduce(StateUpdate(plan = new))
        assertEquals(new, next.plan)
    }

    @Test fun `plan channel keeps existing plan when update has none`() {
        val old = ActionPlan("keep me")
        val next = GraphState(plan = old).reduce(StateUpdate(appendMessages = listOf(user)))
        assertEquals(old, next.plan)
    }

    @Test fun `observations channel appends`() {
        val first = Observation("toolA", "result A")
        val second = Observation("toolB", "boom", isError = true)
        val next = GraphState(observations = listOf(first)).reduce(
            StateUpdate(appendObservations = listOf(second)),
        )
        assertEquals(listOf(first, second), next.observations)
    }

    @Test fun `scratch channel merges with new keys winning`() {
        val next = GraphState(scratch = mapOf("a" to "1", "b" to "2")).reduce(
            StateUpdate(scratch = mapOf("b" to "9", "c" to "3")),
        )
        assertEquals(mapOf("a" to "1", "b" to "9", "c" to "3"), next.scratch)
    }

    @Test fun `remaining steps decrements only when spendStep is set`() {
        assertEquals(2, GraphState(remainingSteps = 3).reduce(StateUpdate(spendStep = true)).remainingSteps)
        assertEquals(3, GraphState(remainingSteps = 3).reduce(StateUpdate(spendStep = false)).remainingSteps)
    }

    @Test fun `isExhausted flips when the budget runs out`() {
        assertFalse(GraphState(remainingSteps = 1).isExhausted)
        assertTrue(GraphState(remainingSteps = 0).isExhausted)
        assertTrue(GraphState(remainingSteps = -1).isExhausted)
    }

    @Test fun `reduce is pure and does not mutate the receiver`() {
        val state = GraphState(messages = listOf(user), remainingSteps = 5)
        state.reduce(StateUpdate(appendMessages = listOf(assistant), spendStep = true))
        assertEquals(listOf(user), state.messages)
        assertEquals(5, state.remainingSteps)
    }

    @Test fun `a single update reduces all channels at once`() {
        val plan = ActionPlan("multi", listOf(PlanStep("s1", "step one")))
        val next = GraphState(remainingSteps = 4).reduce(
            StateUpdate(
                appendMessages = listOf(user),
                plan = plan,
                appendObservations = listOf(Observation("n", "obs")),
                spendStep = true,
                scratch = mapOf("k" to "v"),
            ),
        )
        assertEquals(listOf(user), next.messages)
        assertEquals(plan, next.plan)
        assertEquals(1, next.observations.size)
        assertEquals(3, next.remainingSteps)
        assertEquals("v", next.scratch["k"])
    }

    // --- VisionMessage helpers ---

    @Test fun `text joins all text parts`() {
        val msg = VisionMessage(Role.USER, listOf(ContentPart.Text("hello"), ContentPart.Text("world")))
        assertEquals("hello world", msg.text())
    }

    @Test fun `toolCalls returns only tool-call parts`() {
        val call = ContentPart.ToolCall("c1", "open_app", """{"pkg":"x"}""")
        val msg = VisionMessage(Role.ASSISTANT, listOf(ContentPart.Text("ok"), call))
        assertEquals(listOf(call), msg.toolCalls())
    }

    @Test fun `hasImage reflects presence of an image part`() {
        assertFalse(VisionMessage.text(Role.USER, "no image").hasImage())
        val withImage = VisionMessage(Role.USER, listOf(ContentPart.Image(byteArrayOf(1, 2, 3))))
        assertTrue(withImage.hasImage())
    }

    @Test fun `text factory builds a single text part`() {
        assertEquals(VisionMessage(Role.USER, listOf(ContentPart.Text("hi"))), VisionMessage.text(Role.USER, "hi"))
    }

    // --- ByteArray content equality ---

    @Test fun `image equality compares bytes by content`() {
        assertEquals(ContentPart.Image(byteArrayOf(1, 2, 3)), ContentPart.Image(byteArrayOf(1, 2, 3)))
        assertEquals(
            ContentPart.Image(byteArrayOf(1, 2, 3)).hashCode(),
            ContentPart.Image(byteArrayOf(1, 2, 3)).hashCode(),
        )
        assertNotEquals(ContentPart.Image(byteArrayOf(1, 2, 3)), ContentPart.Image(byteArrayOf(1, 2, 4)))
    }

    @Test fun `audio equality compares bytes by content`() {
        assertEquals(ContentPart.Audio(byteArrayOf(9, 9)), ContentPart.Audio(byteArrayOf(9, 9)))
        assertNotEquals(ContentPart.Audio(byteArrayOf(9, 9)), ContentPart.Audio(byteArrayOf(9, 8)))
    }

    // --- serialization round-trip (VCF-G3 checkpoint prerequisite) ---

    @Test fun `graph state round-trips through json including multimodal parts`() {
        val json = Json
        val state = GraphState(
            messages = listOf(
                VisionMessage(
                    Role.USER,
                    listOf(
                        ContentPart.Text("look at this"),
                        ContentPart.Image(byteArrayOf(10, 20, 30), mime = "image/jpeg"),
                        ContentPart.ToolCall("c1", "describe", "{}"),
                    ),
                ),
            ),
            plan = ActionPlan("describe image", listOf(PlanStep("s1", "see", kind = StepKind.TOOL))),
            observations = listOf(Observation("vision", "a cat")),
            remainingSteps = 7,
            scratch = mapOf("mode" to "vision"),
            sessionId = "sess-1",
        )
        val restored = json.decodeFromString<GraphState>(json.encodeToString(state))
        assertEquals(state, restored)
    }
}
