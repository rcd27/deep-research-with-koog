package agent

import agent.scoping.ResearchQuestion
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.prompt.dsl.prompt

private val researchSystemPrompt = """
    You are a research assistant conducting research on the user's input topic. For context, today's date is {date}.
""".trimIndent()

fun AIAgentSubgraphBuilderBase<*, *>.researcher(
): AIAgentNodeDelegate<String, ResearchQuestion> = node<String, ResearchQuestion>("researcher") {
    llm.writeSession {
        val initial = prompt.copy()
        val researcherPrompt = prompt("researcher_prompt") {
            system(
                researchAgentPrompt()
            )
        }
    }
    TODO("implement")
}