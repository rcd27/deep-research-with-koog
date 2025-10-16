package agent.scoping

import agent.strategy.deepResearchStrategy
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.context.DetachedPromptExecutorAPI
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.testing.feature.withTesting
import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.agents.testing.tools.mockLLMAnswer
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TODO:
 *  - check AgentState in storage after completing the node
 */
class WriteResearchBriefTest {

    @OptIn(DetachedPromptExecutorAPI::class)
    @Test
    fun `Test AgentState is updated`() = runBlocking {
        // FIXME: this stuff brakes down cuz of requestLLMStructuredNodes
        val mockLLMApi = getMockExecutor {
            // Mock a simple text response
            mockLLMAnswer("Hello!") onRequestContains "Hello"

            // Mock a default response
            mockLLMAnswer(Json.encodeToString(ClarifyWithUser(needClarification = true, "blalba",""))).asDefaultResponse
        }

        val agent = AIAgent(
            mockLLMApi,
            OpenAIModels.CostOptimized.GPT4oMini,
            strategy = deepResearchStrategy(mockk()) { question ->
                "Hello"
            },
            toolRegistry = ToolRegistry.EMPTY
        ) {
            withTesting()
        }

        val executionResult = agent.run("Hello")
        assertEquals("Hello!", executionResult)

        /*
        val defaultResponse = agent.run("How are you doin")
        assertEquals("I don't know how to answer that.", defaultResponse)
         */
    }
}