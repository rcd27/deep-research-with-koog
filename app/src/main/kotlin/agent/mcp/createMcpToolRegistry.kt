package agent.mcp

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.mcp.McpToolRegistryProvider
import io.modelcontextprotocol.kotlin.sdk.shared.Transport

suspend fun createMcpToolRegistry(transport: Transport, name: String): ToolRegistry {
    return McpToolRegistryProvider.fromTransport(
        transport = transport,
        name = name,
        version = "0.0.1"
    )
}