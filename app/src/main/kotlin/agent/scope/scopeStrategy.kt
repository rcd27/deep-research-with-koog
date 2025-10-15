package agent.scope

import agent.clarifyWithUserInstructions
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.message.Message
import ai.koog.prompt.structure.StructureFixingParser
import ai.koog.prompt.structure.StructuredResponse
import java.util.*

@Tool
@LLMDescription("Get current date in a human-readable format.")
fun getTodayStr(): String {
    return Date().toLocaleString()
}

val scopeStrategy = strategy<String, String>("scope-strategy") {
    /**
     *     Determine if the user's request contains sufficient information to proceed with research.
     *
     *     Uses structured output to make deterministic decisions and avoid hallucination.
     *     Routes to either research brief generation or ends with a clarification question.
     */
    val clarifyWithUser by node<String, ClarifyWithUser>("clarify_with_user") { initialInput ->
        llm.writeSession {
            val initialPrompt = prompt.copy()
            prompt = prompt("clarify_with_user_instructions") {
                val combinedMessage = buildString {
                    append("<previous_conversation>\n")
                    initialPrompt.messages.forEach { message ->
                        when (message) {
                            is Message.System -> append("<user>\n${message.content}\n</user>\n")
                            is Message.User -> append("<user>\n${message.content}\n</user>\n")
                            is Message.Assistant -> append("<assistant>\n${message.content}\n</assistant>\n")
                            is Message.Tool.Call -> append(
                                "<tool_call tool=${message.tool}>\n${message.content}\n</tool_call>\n"
                            )

                            is Message.Tool.Result -> append(
                                "<tool_result tool=${message.tool}>\n${message.content}\n</tool_result>\n"
                            )
                        }
                    }
                    append("</previous_conversation>\n")
                }
                system(
                    clarifyWithUserInstructions(
                        messages = combinedMessage,
                        date = getTodayStr()
                    )
                )
                user(initialInput) // TODO: not sure if needed
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

    /**
     *     Transform the conversation history into a comprehensive research brief.
     *
     *     Uses structured output to ensure the brief follows the required format
     *     and contains all necessary details for effective research.
     */
    val writeResearchBrief by node<String, Result<StructuredResponse<ResearchQuestion>>>("write_research_brief") {
        TODO("Implement")
    }

    edge(
        nodeStart forwardTo clarifyWithUser
    )

    edge(
        clarifyWithUser forwardTo nodeFinish onCondition { it.needClarification } transformed { it.question }
    )

    edge(
        clarifyWithUser forwardTo writeResearchBrief onCondition { !it.needClarification } transformed { it.verification }
    )


}