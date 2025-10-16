package agent.scoping

import agent.transformMessagesIntoResearchTopicPrompt
import agent.utils.foldPromptMessages
import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.agent.entity.AIAgentStorageKey
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
    val agentState: AIAgentStorageKey<AgentState> = createStorageKey<AgentState>("agent-state")

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

    val writeResearchBrief by writeResearchBrief(agentState)

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