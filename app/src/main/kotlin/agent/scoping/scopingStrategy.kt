package agent.scoping

import agent.transformMessagesIntoResearchTopicPrompt
import agent.utils.foldPromptMessages
import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.prompt.dsl.prompt
import java.util.*

@Tool
@LLMDescription("Get current date in a human-readable format.")
fun getTodayStr(): String {
    return Date().toLocaleString()
}

fun scopingStrategy(askUser: suspend (String) -> String) = strategy<String, String>("scope-strategy") {
    val agentState = createStorageKey<AgentState>("agent-state")

    val clarifyWithUser: AIAgentNodeBase<String, ClarifyWithUser> by clarification()

    val askUser by node<String, String>("ask_user") { question ->
        val userAnswer = askUser(question)
        llm.writeSession {
            /**
             *  We need to handle agent state manually when using LangGraph, storing, updating messages, etc.
             *  With Koog it is more straightforward
             */
            updatePrompt {
                assistant(question)
                user(userAnswer)
            }
        }
        userAnswer
    }

    /**
     *     Transform the conversation history into a comprehensive research brief.
     *
     *     Uses structured output to ensure the brief follows the required format
     *     and contains all necessary details for effective research.
     */
    val writeResearchBrief by node<String, ResearchQuestion>("write_research_brief") {
        llm.writeSession {
            val initialPrompt = prompt.copy()
            prompt = prompt("write_research_brief") {
                system(
                    transformMessagesIntoResearchTopicPrompt(
                        messages = initialPrompt.messages.foldPromptMessages(),
                        date = getTodayStr()
                    )
                )
            }
            val result: ResearchQuestion = requestLLMStructured<ResearchQuestion>().getOrThrow().structure
            storage.set(
                agentState,
                // FIXME: make sure this is the desired way of storing the state
                AgentState(
                    researchBrief = result.researchBrief,
                    supervisorMessages = listOf(result.researchBrief),
                    rawNotes = emptyList(),
                    notes = emptyList(),
                    finalReport = ""
                )
            )
            result
        }
    }

    edge(nodeStart forwardTo clarifyWithUser)

    edge(
        clarifyWithUser forwardTo askUser onCondition { it.needClarification } transformed { it.question }
    )

    edge(
        clarifyWithUser forwardTo writeResearchBrief onCondition { !it.needClarification } transformed { it.verification }
    )

    edge(
        writeResearchBrief forwardTo nodeFinish transformed { it.researchBrief }
    )

    edge(
        askUser forwardTo clarifyWithUser
    )
}