package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlannerNodeTest {

    private val planner = PlannerNode(ModelClient { _, _ -> ModelResponse(VisionMessage.text(Role.ASSISTANT, "")) })

    @Test fun `parses a json plan into ordered steps`() {
        val plan = planner.parsePlan(
            "goal",
            """{"steps":[{"id":"a","instruction":"do A"},{"id":"b","instruction":"do B"}]}""",
        )
        assertEquals(listOf("do A", "do B"), plan.steps.map { it.instruction })
        assertEquals(listOf("a", "b"), plan.steps.map { it.id })
    }

    @Test fun `extracts json from surrounding prose and generates missing ids`() {
        val plan = planner.parsePlan("g", "Sure! Here:\n{\"steps\":[{\"instruction\":\"only\"}]}\nHope that helps")
        assertEquals(listOf("only"), plan.steps.map { it.instruction })
        assertEquals("s1", plan.steps.single().id)
    }

    @Test fun `garbage falls back to a single-step plan for the goal`() {
        val plan = planner.parsePlan("my goal", "not json at all")
        assertEquals(1, plan.steps.size)
        assertEquals("my goal", plan.steps.single().instruction)
    }

    @Test fun `node writes the model's plan into state`() {
        val node = PlannerNode(
            ModelClient { _, _ -> ModelResponse(VisionMessage.text(Role.ASSISTANT, """{"steps":[{"instruction":"step one"}]}""")) },
        )
        val state = GraphState(messages = listOf(VisionMessage.text(Role.USER, "do the thing")))
        val update = (runBlocking { node.run(state, NodeContext()) } as NodeResult.Continue).update
        assertEquals("step one", update.plan!!.steps.single().instruction)
    }

    @Test fun `a blank goal is a no-op`() {
        val update = (runBlocking { planner.run(GraphState(), NodeContext()) } as NodeResult.Continue).update
        assertNull(update.plan)
    }
}
