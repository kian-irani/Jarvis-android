package com.kianirani.jarvis.core.eval

import com.kianirani.jarvis.core.agent.ActionRisk
import com.kianirani.jarvis.core.agent.ModelClient
import com.kianirani.jarvis.core.agent.ModelResponse
import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.tools.ToolContext
import com.kianirani.jarvis.core.tools.ToolRegistry
import com.kianirani.jarvis.core.tools.ToolSpec
import com.kianirani.jarvis.core.tools.VisionTool
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentEvalHarnessTest {

    private fun getTimeTool() = object : VisionTool {
        override val spec = ToolSpec("get_time", "time", JsonObject(emptyMap()), ActionRisk.AUTO)
        override suspend fun execute(args: JsonObject, ctx: ToolContext) =
            ContentPart.ToolResult("?", "get_time", listOf(ContentPart.Text("12:00")))
    }

    /** A model that calls get_time once, then answers after seeing the result. */
    private val toolUsingModel = ModelClient { messages, _ ->
        val sawResult = messages.any { m -> m.content.any { it is ContentPart.ToolResult } }
        val reply = if (sawResult) {
            VisionMessage.text(Role.ASSISTANT, "it is 12:00")
        } else {
            VisionMessage(Role.ASSISTANT, listOf(ContentPart.ToolCall("c1", "get_time", "{}")))
        }
        ModelResponse(reply)
    }

    private val plainModel = ModelClient { _, _ -> ModelResponse(VisionMessage.text(Role.ASSISTANT, "hello")) }

    @Test fun `passes when the agent completes and calls the expected tool`() {
        val case = AgentEvalCase("time", "what time?", toolUsingModel, ToolRegistry(listOf(getTimeTool())), expectedTool = "get_time")
        val report = runBlocking { AgentEvalHarness.run(listOf(case)) }
        assertEquals(1, report.passed)
        assertEquals(1.0, report.passRate, 0.0)
    }

    @Test fun `fails when the expected tool is never called`() {
        val case = AgentEvalCase("time", "hi", plainModel, ToolRegistry(emptyList()), expectedTool = "get_time")
        val report = runBlocking { AgentEvalHarness.run(listOf(case)) }
        assertEquals(0, report.passed)
        assertTrue(report.describeFailures().contains("get_time"))
    }

    @Test fun `report aggregates the pass rate over multiple cases`() {
        val pass = AgentEvalCase("p", "what time?", toolUsingModel, ToolRegistry(listOf(getTimeTool())), expectedTool = "get_time")
        val fail = AgentEvalCase("f", "hi", plainModel, ToolRegistry(emptyList()), expectedTool = "get_time")
        val report = runBlocking { AgentEvalHarness.run(listOf(pass, fail)) }
        assertEquals(2, report.total)
        assertEquals(1, report.passed)
        assertEquals(0.5, report.passRate, 0.0)
    }

    @Test fun `a plain answer with no expected tool passes`() {
        val case = AgentEvalCase("greet", "hi", plainModel, ToolRegistry(emptyList()))
        assertEquals(1, runBlocking { AgentEvalHarness.run(listOf(case)) }.passed)
    }
}
