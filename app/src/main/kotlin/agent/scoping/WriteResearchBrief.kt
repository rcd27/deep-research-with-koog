package agent.scoping

import agent.transformMessagesIntoResearchTopicPrompt
import agent.utils.foldPromptMessages
import ai.koog.agents.core.agent.entity.AIAgentStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.prompt.dsl.prompt


/**
 *     Transform the conversation history into a comprehensive research brief.
 *
 *     Uses structured output to ensure the brief follows the required format
 *     and contains all necessary details for effective research.
 */
fun AIAgentSubgraphBuilderBase<*, *>.writeResearchBrief(
    agentStateKey: AIAgentStorageKey<AgentState>
): AIAgentNodeDelegate<String, ResearchQuestion> = node<String, ResearchQuestion>("write_research_brief") {
    llm.writeSession {
        val initialPrompt = prompt.copy()
        prompt = prompt("write_research_brief_prompt") {
            system(
                transformMessagesIntoResearchTopicPrompt(
                    messages = initialPrompt.messages.foldPromptMessages(),
                    date = getTodayStr()
                )
            )
        }
        val result: ResearchQuestion = requestLLMStructured<ResearchQuestion>().getOrThrow().structure
        storage.set(
            agentStateKey,
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