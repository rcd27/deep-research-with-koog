package agent.scoping

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.AIAgentGraphContext
import ai.koog.agents.core.agent.context.AIAgentLLMContext
import ai.koog.agents.core.agent.context.DetachedPromptExecutorAPI
import ai.koog.agents.core.agent.entity.FinishNode
import ai.koog.agents.core.agent.entity.StartNode
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.environment.AIAgentEnvironment
import ai.koog.agents.core.feature.pipeline.AIAgentGraphPipeline
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.OllamaModels
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO: cover with more asserts
class ClarificationTest {
    private val testClock: Clock = object : Clock {
        override fun now(): Instant = Instant.parse("2023-01-01T00:00:00Z")
    }

    @OptIn(InternalAgentsApi::class, DetachedPromptExecutorAPI::class)
    @Test
    fun testChatStrategyDefaultName() = runTest {
        val initialPrompt = prompt("id") {
            system("System message")
            user("User question")
            assistant("Assistant question")
            user("User answer")
            tool {
                call(id = "tool-id-1", tool = "tool1", content = "{x=1}")
                result(id = "tool-id-1", tool = "tool1", content = "{result=2}")
            }
            tool {
                call(id = "tool-id-2", tool = "tool2", content = "{x=100}")
                result(id = "tool-id-2", tool = "tool2", content = "{result=-200}")
            }
        }

        val mockPromptExecutor = mockk<PromptExecutor>()

        val mockEnv = mockk<AIAgentEnvironment>()

        val initialModel = OllamaModels.Meta.LLAMA_3_2

        val mockLLM = AIAgentLLMContext(
            tools = emptyList(),
            toolRegistry = ToolRegistry {},
            prompt = initialPrompt,
            model = initialModel,
            promptExecutor = mockPromptExecutor,
            environment = mockEnv,
            config = AIAgentConfig(prompt = prompt("id") {}, model = OpenAIModels.Chat.GPT4o, maxAgentIterations = 10),
            clock = testClock
        )

        val context = AIAgentGraphContext(
            environment = mockEnv,
            agentId = "test-agent",
            agentInputType = typeOf<String>(),
            agentInput = "Hello",
            config = mockk(),
            llm = mockLLM,
            stateManager = mockk(),
            storage = mockk(),
            runId = "run-1",
            strategyName = "test-strategy",
            pipeline = AIAgentGraphPipeline(),
        )

        val subgraphContext = object : AIAgentSubgraphBuilderBase<String, String>() {
            override val nodeStart: StartNode<String> = mockk()
            override val nodeFinish: FinishNode<String> = mockk()
        }

        val clarificationNode by subgraphContext.clarification()

        coEvery { mockPromptExecutor.execute(any(), any(), any()) } returns listOf(
            Message.Assistant(
                content = Json.encodeToString(
                    ClarifyWithUser.serializer(),
                    ClarifyWithUser(
                        needClarification = true,
                        question = "What type of engine do you want to research?",
                        verification = ""
                    )
                ),
                metaInfo = ResponseMetaInfo.create(testClock),
            )
        )

        clarificationNode.execute(context, input = "-200")

        coVerify {
            mockPromptExecutor.execute(
                prompt = match {
                    // Check whether all message history is inlined into system_prompt
                    (it.messages.size == 1) && (it.id == "clarify_with_user_instructions")
                },
                model = match {
                    it == initialModel
                },
                tools = any()
            )
        }

        assertEquals(initialPrompt, context.llm.prompt)

        assertEquals(initialModel, context.llm.model)
    }
}
