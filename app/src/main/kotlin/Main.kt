import agent.executor.openAISinglePromptExecutor
import agent.scope.scopeStrategy
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter
import ai.koog.prompt.executor.clients.openai.OpenAIModels

suspend fun main(): Unit = run {
    val agentConfig = AIAgentConfig.withSystemPrompt(
        prompt = "You are deep research agent",
        maxAgentIterations = 50,
        llm = OpenAIModels.CostOptimized.GPT4oMini
    )

    val agent = AIAgent(
        promptExecutor = openAISinglePromptExecutor,
        strategy = scopeStrategy,
        agentConfig = agentConfig,
        toolRegistry = ToolRegistry.EMPTY
    ) {
        install(OpenTelemetry) {
            setVerbose(true) // to see system/user prompts
            // see: https://docs.koog.ai/opentelemetry-langfuse-exporter/
            addLangfuseExporter(
                langfuseUrl = Config.LANGFUSE_HOST,
                langfusePublicKey = Config.LANGFUSE_PUBLIC_KEY,
                langfuseSecretKey = Config.LANGFUSE_SECRET_KEY
            )
        }
    }
    val executionResult = agent.run("Please make a deep research about my Honda")
    println(executionResult)
}