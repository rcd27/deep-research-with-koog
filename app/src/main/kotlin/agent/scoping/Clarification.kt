package agent.scoping

import agent.clarifyWithUserInstructions
import agent.utils.foldPromptMessages
import ai.koog.agents.core.annotation.InternalAgentsApi
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.structure.StructureFixingParser
import kotlinx.serialization.Serializable


@Serializable
@LLMDescription("Schema for user clarification decision and questions.")
data class ClarifyWithUser(
    @property:LLMDescription("Whether the user needs to be asked a clarifying question.")
    val needClarification: Boolean, // FIXME: split into sealed class without flags
    @property:LLMDescription("A question to ask the user to clarify the report scope.")
    val question: String,
    @property:LLMDescription("Verify message that we will start research after the user has provided the necessary information.")
    val verification: String
)

/**
 *     Determine if the user's request contains sufficient information to proceed with research.
 *
 *     Uses structured output to make deterministic decisions and avoid hallucination.
 *     Routes to either research brief generation or ends with a clarification question.
 */
@OptIn(InternalAgentsApi::class)
public fun AIAgentSubgraphBuilderBase<*, *>.clarification(
): AIAgentNodeDelegate<String, ClarifyWithUser> = node<String, ClarifyWithUser> { nodeInput ->
    llm.writeSession {
        val initialPrompt = prompt.copy()
        prompt = prompt("clarify_with_user_instructions") {// FIXME: here we got not only user instructions but whole
            // chat history
            system(
                clarifyWithUserInstructions(
                    messages = initialPrompt.messages.foldPromptMessages(),
                    date = getTodayStr()
                )
            )
        }

        val result: ClarifyWithUser = requestLLMStructured<ClarifyWithUser>(
            examples = listOf(
                ClarifyWithUser(
                    needClarification = true,
                    question = "What is the model of a car you want to buy?",
                    verification = ""
                ),
                ClarifyWithUser(
                    needClarification = false,
                    question = "",
                    verification = "The chosen car is Honda Civic FD8, 1.8L"
                ),
            ),
            // optional field -- recommented for reliability of the format
            fixingParser = StructureFixingParser(
                fixingModel = OpenAIModels.CostOptimized.GPT4oMini,
                retries = 3,
            )
        ).getOrThrow().structure

        prompt = initialPrompt

        result
    }
}