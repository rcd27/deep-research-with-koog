package agent.scope

import agent.clarifyWithUserInstructions
import agent.transformMessagesIntoResearchTopicPrompt
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.message.Message
import ai.koog.prompt.structure.StructureFixingParser
import ai.koog.prompt.xml.xml
import java.util.*

@Tool
@LLMDescription("Get current date in a human-readable format.")
fun getTodayStr(): String {
    return Date().toLocaleString()
}

fun List<Message>.foldPromptMessages(): String = xml {
    tag("previous_conversation") {
        this@foldPromptMessages.forEach { message ->
            when (message) {
                is Message.System -> tag("system") { +message.content }
                is Message.Tool.Result -> tag(
                    name = "tool_result",
                    attributes = linkedMapOf(
                        "tool" to message.tool
                    )
                ) { +message.content }

                is Message.User -> tag(name = "user") { +message.content }
                is Message.Assistant -> tag("assistant") { +message.content }
                is Message.Tool.Call -> tag(
                    name = "tool_call",
                    attributes = linkedMapOf(
                        "tool" to message.tool
                    )
                ) { +message.content }
            }
        }
    }
}

// TODO: write test, see:https://github.com/JetBrains/koog/blob/726de33ceeec65d3ef784c8630bbd522ba6f18c5/agents/agents-ext/src/jvmTest/kotlin/ai/koog/agents/ext/agent/LLMAsJudgeNodeTest.kt

fun scopingStrategy(askUser: suspend (String) -> String) = strategy<String, String>("scope-strategy") {
    val agentState = createStorageKey<AgentState>("agent-state")

    /**
     *     Determine if the user's request contains sufficient information to proceed with research.
     *
     *     Uses structured output to make deterministic decisions and avoid hallucination.
     *     Routes to either research brief generation or ends with a clarification question.
     */
    val clarifyWithUser by node<String, ClarifyWithUser>("clarify_with_user") { newInput ->
        llm.writeSession {
            val initialPrompt = prompt.copy()
            prompt = prompt("clarify_with_user_instructions") {
                system(
                    clarifyWithUserInstructions(
                        messages = initialPrompt.messages.foldPromptMessages(),
                        date = getTodayStr()
                    )
                )
                user(newInput)
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