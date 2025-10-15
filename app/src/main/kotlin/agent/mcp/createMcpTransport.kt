package agent.mcp

import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.defaultStdioTransport
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport

fun createMcpTransport(process: Process): StdioClientTransport {
    val transport: StdioClientTransport = McpToolRegistryProvider.defaultStdioTransport(process)
    return transport
}