package com.kianirani.jarvis.core.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * MCP — Model Context Protocol client core (PRD §, "اکوسیستمِ پلاگین + پروتکلِ agent-to-agent").
 * MCP is JSON-RPC 2.0: the client lists an external server's tools and calls them. These are the
 * pure wire types + the [McpTransport] seam; the concrete stdio/HTTP transport and bridging an
 * [McpTool] into Vision's own tool layer (so the agent can call external tools) are the
 * on-device half. Pure & serializable → portable and testable.
 */
@Serializable
data class McpRequest(
    val jsonrpc: String = "2.0",
    val id: Int,
    val method: String,
    val params: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class McpError(val code: Int, val message: String)

@Serializable
data class McpResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: JsonElement? = null,
    val error: McpError? = null,
) {
    val ok: Boolean get() = error == null
}

/** A tool advertised by an MCP server. [inputSchema] is JSON-Schema (maps to Vision's ToolSpec). */
@Serializable
data class McpTool(
    val name: String,
    val description: String = "",
    val inputSchema: JsonObject = JsonObject(emptyMap()),
)

/** Transport-agnostic send: hand the server a request, get its response (stdio/HTTP/WebSocket). */
fun interface McpTransport {
    suspend fun send(request: McpRequest): McpResponse
}

/**
 * MCP — standard method names + request builders (so call sites don't hand-roll JSON-RPC).
 * Monotonic ids keep request/response correlation simple.
 */
class McpClient(private val transport: McpTransport) {
    private var nextId = 1
    private fun id() = nextId++

    suspend fun initialize(): McpResponse = transport.send(McpRequest(id = id(), method = "initialize"))

    suspend fun listTools(): McpResponse = transport.send(McpRequest(id = id(), method = "tools/list"))

    suspend fun callTool(name: String, arguments: JsonObject): McpResponse =
        transport.send(
            McpRequest(
                id = id(),
                method = "tools/call",
                params = JsonObject(mapOf("name" to kotlinx.serialization.json.JsonPrimitive(name), "arguments" to arguments)),
            ),
        )
}
