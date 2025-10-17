package agent.strategy

import agent.compressResearchSystemPrompt
import agent.scoping.*
import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.agent.entity.AIAgentStorageKey
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.prompt.message.Message
import java.util.*

@Tool
@LLMDescription("Get current date in a human-readable format.")
fun getTodayStr(): String {
    return Date().toLocaleString()
}

fun deepResearchStrategy(
    tavilySearchTool: ai.koog.agents.core.tools.Tool<*, *>,
    askUser: suspend (String) -> String
) = strategy<String, String>("scope-strategy") {
    val clarifyWithUser: AIAgentNodeBase<String, ClarifyWithUser> by clarifyWithUser()

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

    val writeResearchBrief: AIAgentNodeBase<String, ResearchQuestion> by writeResearchBrief()

    val searchWeb by node<ResearchQuestion, Message.Response>("search_web") { researchQuestion ->
        llm.writeSession {
            updatePrompt {
                system(researchQuestion.researchBrief)
            }
            val response = requestLLMForceOneTool(tavilySearchTool)
            response
        }
    }

    val compressPrompt = compressResearchSystemPrompt()

    val nodeExecuteTool by nodeExecuteTool("execute_tool") // multiple tool_calls possible in tutorial
    val nodeSendToolResult by nodeLLMSendToolResult("send_tool_result") // TODO: should continue?


    // FIXME: <<<< COMPRESS RESEARCH WITH PROMPT

    edge(nodeStart forwardTo clarifyWithUser)

    // Scoping
    edge(clarifyWithUser forwardTo askUser onCondition { it.needClarification } transformed { it.question })
    edge(askUser forwardTo clarifyWithUser)
    edge(clarifyWithUser forwardTo writeResearchBrief onCondition { !it.needClarification } transformed { it.verification })
    edge(writeResearchBrief forwardTo searchWeb)

    // Research
    // TODO: can be grouped in subgraph
    edge(searchWeb forwardTo nodeExecuteTool onToolCall { true })
    // FIXME: here we have system prompt "write_research_brief_prompt, is it correct, or should LLM summarize? No,
    //  there should be compress_research
    edge(nodeExecuteTool forwardTo nodeSendToolResult)
    edge(nodeSendToolResult forwardTo nodeFinish transformed { it.content })
    // FIXME: should force tool call in 146%
    edge(searchWeb forwardTo nodeFinish onToolCall { false } transformed { it.content })

}