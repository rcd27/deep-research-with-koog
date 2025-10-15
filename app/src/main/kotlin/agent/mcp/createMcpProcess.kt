package agent.mcp

data class NodeMcpProcessConfig(
    val scriptPath: String,
    val apiKey: String
)

fun createMcpProcess(input: NodeMcpProcessConfig): Process {
    val processBuilder = ProcessBuilder(
        "node",
        input.scriptPath,
        "--apiKey=${input.apiKey}"
    )
    val process: Process = processBuilder.start()
    Thread.sleep(2000)
    return process
}